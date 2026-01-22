package com.server.smarttransferserver.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.service.ActiveUserService;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.service.RedisService;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.service.impl.ActiveUserServiceImpl;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 监控数据推送定时任务
 * 按用户推送，聚合所有活跃任务的监控数据
 * 使用Redis缓存活跃用户集合，避免无任务时的数据库查询
 */
@Slf4j
@Component
public class MonitorPushTask {

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

    /**
     * 定时推送监控数据（每500ms）
     * 按用户推送，只在用户有活跃任务时推送
     * 从Redis获取活跃用户列表，避免无任务时的数据库查询
     */
    @Scheduled(fixedRate = 500)
    public void pushMetrics() {
        if (userSessions.isEmpty()) {
            return;
        }

        // 从Redis获取活跃用户ID集合（有活跃任务的用户）
        // 使用ActiveUserService中定义的常量，避免硬编码
        Set<Object> activeUserIdsObj = redisService.sMembers(ActiveUserServiceImpl.ACTIVE_USERS_KEY);
        if (activeUserIdsObj == null || activeUserIdsObj.isEmpty()) {
            return; // 没有活跃用户，直接返回
        }

        // 转换为Long类型的用户ID集合
        Set<Long> activeUserIds = activeUserIdsObj.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(Collectors.toSet());

        // 只遍历有活跃任务的用户
        for (Long userId : activeUserIds) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }

            try {
                // 查询详细的任务列表
                List<TransferTask> activeTasks = transferTaskService.getActiveTasksByUserId(userId);
                if (activeTasks == null || activeTasks.isEmpty()) {
                    // 如果Redis中有但实际没有任务，清理Redis缓存（可能任务刚完成）
                    activeUserService.removeActiveUser(userId);
                    continue;
                }

                // **改进：按任务分别推送指标（而不是聚合）**
                // 为每个任务获取最新的指标
                Map<String, CongestionMetricsVO> taskMetricsMap = new java.util.HashMap<>();
                for (TransferTask task : activeTasks) {
                    List<CongestionMetricsVO> latestMetrics = metricsService.getLatestMetrics(task.getTaskId(), 1);
                    if (!latestMetrics.isEmpty()) {
                        CongestionMetricsVO metrics = latestMetrics.get(0);
                        // 设置taskId
                        metrics.setTaskId(task.getTaskId());
                        taskMetricsMap.put(task.getTaskId(), metrics);
                    }
                }
                
                // 如果没有任何任务的指标，使用空指标
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
                            .build();
                    // 如果有任务但没有指标，为第一个任务创建一个空指标
                    if (!activeTasks.isEmpty()) {
                        emptyMetrics.setTaskId(activeTasks.get(0).getTaskId());
                        taskMetricsMap.put(activeTasks.get(0).getTaskId(), emptyMetrics);
                    }
                }
                
                // **改进：构造包含所有任务指标的响应对象**
                // 使用Map结构，前端可以根据taskId获取对应任务的指标
                java.util.Map<String, Object> responseData = new java.util.HashMap<>();
                responseData.put("type", "metrics");
                responseData.put("tasks", taskMetricsMap);
                responseData.put("timestamp", System.currentTimeMillis());
                
                // 序列化为JSON
                String json = objectMapper.writeValueAsString(responseData);
                TextMessage message = new TextMessage(json);

                // 推送消息到该用户的所有会话
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

                // 移除已关闭的会话
                for (WebSocketSession session : sessionsToRemove) {
                    sessions.remove(session);
                }

                // 如果该用户的所有会话都关闭了，从Map中移除
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
     *
     * @param userId  用户ID
     * @param session WebSocket会话
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
     *
     * @param userId  用户ID（可为null，表示从所有用户中移除）
     * @param session WebSocket会话
     */
    public static void removeSession(Long userId, WebSocketSession session) {
        if (userId != null) {
            // 从指定用户的会话集合中移除
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                log.info("移除WebSocket会话 - 用户ID: {}, SessionId: {}, 剩余会话数: {}", 
                        userId, session.getId(), sessions.size());

                // 如果该用户的所有会话都关闭了，从Map中移除
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        } else {
            // 如果userId为null，遍历所有用户查找并移除（兼容处理）
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
     *
     * @return 连接数
     */
    public static int getConnectionCount() {
        return userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * 获取当前用户数
     *
     * @return 用户数
     */
    public static int getUserCount() {
        return userSessions.size();
    }

}
