package com.server.smarttransferserver.websocket;

import com.server.smarttransferserver.task.MonitorPushTask;
import com.server.smarttransferserver.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * 监控WebSocket处理器
 * 负责连接管理和用户ID绑定
 */
@Slf4j
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从查询参数获取Token
        Long userId = extractUserIdFromSession(session);
        if (userId != null) {
            MonitorPushTask.registerSession(userId, session);
            log.info("注册WebSocket会话 - 用户ID: {}, SessionId: {}", userId, session.getId());
        } else {
            log.warn("WebSocket连接失败 - 无法获取用户ID, SessionId: {}", session.getId());
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未提供有效的Token"));
            } catch (Exception e) {
                log.error("关闭WebSocket会话失败", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserIdFromSession(session);
        if (userId != null) {
            MonitorPushTask.removeSession(userId, session);
            log.info("移除WebSocket会话 - 用户ID: {}, SessionId: {}", userId, session.getId());
        } else {
            // 如果没有userId，尝试移除所有会话（兼容处理）
            MonitorPushTask.removeSession(null, session);
        }
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
        Long userId = extractUserIdFromSession(session);
        if (userId != null) {
            MonitorPushTask.removeSession(userId, session);
        } else {
            MonitorPushTask.removeSession(null, session);
        }
    }

    /**
     * 从WebSocket会话中提取用户ID
     *
     * @param session WebSocket会话
     * @return 用户ID，如果无法获取返回null
     */
    private Long extractUserIdFromSession(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null || uri.getQuery() == null) {
                return null;
            }

            // 从查询参数获取Token (格式: ?token=xxx)
            String query = uri.getQuery();
            String[] params = query.split("&");
            String token = null;
            
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6); // "token="长度为6
                    break;
                }
            }

            if (token == null || token.isEmpty()) {
                return null;
            }

            // 验证Token并获取用户ID
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }

            return null;
        } catch (Exception e) {
            log.error("从WebSocket会话提取用户ID失败", e);
            return null;
        }
    }
}
