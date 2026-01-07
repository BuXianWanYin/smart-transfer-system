package com.server.smarttransferserver.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.BBRAlgorithm;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.congestion.CubicAlgorithm;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 监控数据推送定时任务
 */
@Slf4j
@Component
public class MonitorPushTask {

    /**
     * WebSocket会话集合（由WebSocketHandler注册）
     */
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Autowired
    private CongestionMetricsService metricsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private AdaptiveAlgorithm adaptiveAlgorithm;

    @Autowired(required = false)
    private CubicAlgorithm cubicAlgorithm;

    @Autowired(required = false)
    private BBRAlgorithm bbrAlgorithm;

    private CongestionControlAlgorithm currentAlgorithm;

    @PostConstruct
    public void init() {
        if (adaptiveAlgorithm != null) {
            currentAlgorithm = adaptiveAlgorithm;
        } else if (cubicAlgorithm != null) {
            currentAlgorithm = cubicAlgorithm;
        } else if (bbrAlgorithm != null) {
            currentAlgorithm = bbrAlgorithm;
        }
        log.info("监控推送任务初始化完成");
    }

    /**
     * 定时推送监控数据（每500ms）
     */
    @Scheduled(fixedRate = 500)
    public void pushMetrics() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            CongestionMetricsVO metrics = metricsService.getCurrentMetrics(currentAlgorithm);
            String json = objectMapper.writeValueAsString(metrics);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("发送消息失败，移除会话 - SessionId: {}", session.getId());
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("推送监控数据失败", e);
        }
    }

    /**
     * 注册WebSocket会话
     */
    public static void registerSession(WebSocketSession session) {
        sessions.add(session);
        log.info("注册WebSocket会话 - SessionId: {}, 当前连接数: {}", session.getId(), sessions.size());
    }

    /**
     * 移除WebSocket会话
     */
    public static void removeSession(WebSocketSession session) {
        sessions.remove(session);
        log.info("移除WebSocket会话 - SessionId: {}, 剩余连接数: {}", session.getId(), sessions.size());
    }

    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return sessions.size();
    }
}

