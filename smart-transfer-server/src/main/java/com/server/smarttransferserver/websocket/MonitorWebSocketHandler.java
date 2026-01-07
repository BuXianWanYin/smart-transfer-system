package com.server.smarttransferserver.websocket;

import com.server.smarttransferserver.task.MonitorPushTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 监控WebSocket处理器
 * 只负责连接管理，数据推送由MonitorPushTask定时任务处理
 */
@Slf4j
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        MonitorPushTask.registerSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        MonitorPushTask.removeSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端消息处理（如需要可扩展）
        String payload = message.getPayload();
        log.debug("收到客户端消息: {}", payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误 - SessionId: {}, 错误: {}", session.getId(), exception.getMessage());
        MonitorPushTask.removeSession(session);
    }
}
