package com.server.smarttransferserver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 监控数据WebSocket处理器
 */
@Slf4j
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    @Autowired
    private CongestionMetricsService metricsService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket连接建立 - SessionId: {}, 当前连接数: {}", session.getId(), sessions.size());
        
        // 立即发送一次数据
        sendMetricsToSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket连接关闭 - SessionId: {}, 状态: {}, 剩余连接数: {}", 
                 session.getId(), status, sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 可以处理客户端发来的消息，比如手动请求刷新
        String payload = message.getPayload();
        if ("refresh".equals(payload)) {
            sendMetricsToSession(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误 - SessionId: {}, 错误: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
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
            CongestionMetricsVO metrics = metricsService.getCurrentMetrics();
            String json = objectMapper.writeValueAsString(metrics);
            TextMessage message = new TextMessage(json);
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.error("发送消息失败 - SessionId: {}", session.getId());
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            log.error("推送监控数据失败", e);
        }
    }

    /**
     * 发送数据到指定session
     */
    private void sendMetricsToSession(WebSocketSession session) {
        try {
            CongestionMetricsVO metrics = metricsService.getCurrentMetrics();
            String json = objectMapper.writeValueAsString(metrics);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送数据失败 - SessionId: {}", session.getId(), e);
        }
    }

    /**
     * 广播消息给所有连接
     */
    public void broadcast(Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage(json);
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("广播消息失败", e);
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }
}

