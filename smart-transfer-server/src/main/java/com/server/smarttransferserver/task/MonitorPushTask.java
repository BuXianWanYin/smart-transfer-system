package com.server.smarttransferserver.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.service.TransferTaskService;
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
    private ObjectMapper objectMapper;

    /**
     * 定时推送监控数据（每500ms）
     * 按用户推送，只在用户有活跃任务时推送
     */
    @Scheduled(fixedRate = 500)
    public void pushMetrics() {
        if (userSessions.isEmpty()) {
            return;
        }

        // 遍历所有用户的会话
        for (Map.Entry<Long, Set<WebSocketSession>> entry : userSessions.entrySet()) {
            Long userId = entry.getKey();
            Set<WebSocketSession> sessions = entry.getValue();

            if (sessions == null || sessions.isEmpty()) {
                continue;
            }

            try {
                // 查询用户所有活跃的传输任务
                List<TransferTask> activeTasks = transferTaskService.getActiveTasksByUserId(userId);

                // 如果没有活跃任务，不推送
                if (activeTasks == null || activeTasks.isEmpty()) {
                    continue;
                }

                // 提取任务ID列表
                List<String> taskIds = activeTasks.stream()
                        .map(TransferTask::getTaskId)
                        .collect(Collectors.toList());

                // 聚合所有任务的监控数据
                CongestionMetricsVO aggregatedMetrics = metricsService.aggregateMetricsByTaskIds(taskIds);

                // 序列化为JSON
                String json = objectMapper.writeValueAsString(aggregatedMetrics);
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
