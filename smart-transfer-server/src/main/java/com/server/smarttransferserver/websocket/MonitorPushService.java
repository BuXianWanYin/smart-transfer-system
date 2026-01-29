package com.server.smarttransferserver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.service.ActiveUserService;
import com.server.smarttransferserver.service.CongestionAlgorithmManager;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.service.RedisService;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.service.impl.ActiveUserServiceImpl;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 监控数据推送服务
 * 使用独立后台线程按间隔推送，仅在存在 WebSocket 连接且有活跃任务时工作
 * 不使用全局定时任务，避免占用调度线程且无连接时不再轮询
 */
@Slf4j
@Component
public class MonitorPushService {

    /** 有会话时推送间隔（毫秒），从配置 transfer.monitor.push-interval-ms 读取 */
    @Value("${transfer.monitor.push-interval-ms:500}")
    private long pushIntervalMs;
    /** 无会话时休眠间隔（毫秒），从配置 transfer.monitor.idle-sleep-ms 读取 */
    @Value("${transfer.monitor.idle-sleep-ms:2000}")
    private long idleSleepMs;

    /**
     * 按用户存储WebSocket会话
     * Key: 用户ID, Value: 该用户的WebSocket会话集合
     */
    private static final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private CongestionMetricsService metricsService;

    @Autowired
    private TransferTaskService transferTaskService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ActiveUserService activeUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CongestionAlgorithmManager algorithmManager;

    private volatile boolean running = true;
    private Thread pushThread;

    @PostConstruct
    public void startPushThread() {
        pushThread = new Thread(this::pushLoop, "monitor-push");
        pushThread.setDaemon(true);
        pushThread.start();
        log.info("监控推送线程已启动（有连接时每 {}ms 推送，无连接时休眠 {}ms）", pushIntervalMs, idleSleepMs);
    }

    @PreDestroy
    public void stopPushThread() {
        running = false;
        if (pushThread != null) {
            pushThread.interrupt();
        }
    }

    /**
     * 推送循环：无连接时长时间休眠，有连接时按间隔推送
     */
    private void pushLoop() {
        while (running) {
            try {
                if (userSessions.isEmpty()) {
                    Thread.sleep(idleSleepMs);
                    continue;
                }
                doPushMetrics();
                Thread.sleep(pushIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running) {
                    break;
                }
            }
        }
        log.info("监控推送线程已退出");
    }

    /**
     * 执行一次推送：按用户推送，只在用户有活跃任务时推送
     */
    private void doPushMetrics() {
        if (userSessions.isEmpty()) {
            return;
        }

        // 从Redis获取活跃用户ID集合（有活跃任务的用户）
        Set<Object> activeUserIdsObj = redisService.sMembers(ActiveUserServiceImpl.ACTIVE_USERS_KEY);
        if (activeUserIdsObj == null || activeUserIdsObj.isEmpty()) {
            return;
        }

        Set<Long> activeUserIds = activeUserIdsObj.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(Collectors.toSet());

        for (Long userId : activeUserIds) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }

            try {
                List<TransferTask> activeTasks = transferTaskService.getActiveTasksByUserId(userId);
                if (activeTasks == null || activeTasks.isEmpty()) {
                    activeUserService.removeActiveUser(userId);
                    continue;
                }

                Map<String, CongestionMetricsVO> taskMetricsMap = new java.util.HashMap<>();
                for (TransferTask task : activeTasks) {
                    CongestionControlAlgorithm algorithm = algorithmManager.getAlgorithm(task.getTaskId());
                    if (algorithm != null) {
                        CongestionMetricsVO metrics = metricsService.getCurrentMetrics(algorithm);
                        metrics.setTaskId(task.getTaskId());
                        taskMetricsMap.put(task.getTaskId(), metrics);
                    } else {
                        List<CongestionMetricsVO> latestMetrics = metricsService.getLatestMetrics(task.getTaskId(), 1);
                        if (!latestMetrics.isEmpty()) {
                            CongestionMetricsVO metrics = latestMetrics.get(0);
                            metrics.setTaskId(task.getTaskId());
                            taskMetricsMap.put(task.getTaskId(), metrics);
                        }
                    }
                }

                if (taskMetricsMap.isEmpty()) {
                    CongestionMetricsVO emptyMetrics = CongestionMetricsVO.builder()
                            .algorithm("NONE")
                            .cwnd(0L)
                            .ssthresh(0L)
                            .rate(0L)
                            .rtt(0L)
                            .lossRate(0.0)
                            .bandwidth(0L)
                            .networkQuality("-")
                            .rttJitter(0L)
                            .bdp(0L)
                            .networkTrend(null)
                            .isWarmingUp(false)
                            .build();
                    if (!activeTasks.isEmpty()) {
                        emptyMetrics.setTaskId(activeTasks.get(0).getTaskId());
                        taskMetricsMap.put(activeTasks.get(0).getTaskId(), emptyMetrics);
                    }
                }

                java.util.Map<String, Object> responseData = new java.util.HashMap<>();
                responseData.put("type", "metrics");
                responseData.put("tasks", taskMetricsMap);
                responseData.put("timestamp", System.currentTimeMillis());

                String json = objectMapper.writeValueAsString(responseData);
                TextMessage message = new TextMessage(json);

                List<WebSocketSession> sessionsToRemove = new ArrayList<>();
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(message);
                        } catch (IOException e) {
                            log.warn("发送消息失败，移除会话 - 用户ID: {}, SessionId: {}", userId, session.getId());
                            sessionsToRemove.add(session);
                        }
                    } else {
                        sessionsToRemove.add(session);
                    }
                }

                for (WebSocketSession session : sessionsToRemove) {
                    sessions.remove(session);
                }

                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }

            } catch (Exception e) {
                log.error("推送监控数据失败 - 用户ID: {}", userId, e);
            }
        }
    }

    /**
     * 注册WebSocket会话
     */
    public static void registerSession(Long userId, WebSocketSession session) {
        if (userId == null) {
            log.warn("注册WebSocket会话失败 - 用户ID为空, SessionId: {}", session.getId());
            return;
        }

        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        int sessionCount = userSessions.get(userId).size();
        int userCount = userSessions.size();
        log.info("注册WebSocket会话 - 用户ID: {}, SessionId: {}, 该用户会话数: {}, 总用户数: {}",
                userId, session.getId(), sessionCount, userCount);
    }

    /**
     * 移除WebSocket会话
     */
    public static void removeSession(Long userId, WebSocketSession session) {
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                log.info("移除WebSocket会话 - 用户ID: {}, SessionId: {}, 剩余会话数: {}",
                        userId, session.getId(), sessions.size());

                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        } else {
            for (Map.Entry<Long, Set<WebSocketSession>> entry : userSessions.entrySet()) {
                Set<WebSocketSession> sessions = entry.getValue();
                if (sessions != null && sessions.remove(session)) {
                    log.info("移除WebSocket会话 - 用户ID: {}, SessionId: {}", entry.getKey(), session.getId());
                    if (sessions.isEmpty()) {
                        userSessions.remove(entry.getKey());
                    }
                    break;
                }
            }
        }
    }

    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * 获取当前用户数
     */
    public static int getUserCount() {
        return userSessions.size();
    }

}
