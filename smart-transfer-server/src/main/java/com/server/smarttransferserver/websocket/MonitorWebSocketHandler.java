package com.server.smarttransferserver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.smarttransferserver.service.ProbeRttStore;
import com.server.smarttransferserver.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 监控WebSocket处理器
 * 负责连接管理、用户ID绑定、WebSocket Ping/Pong RTT测量
 */
@Slf4j
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ProbeRttStore probeRttStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从查询参数获取Token
        Long userId = extractUserIdFromSession(session);
        if (userId != null) {
            MonitorPushService.registerSession(userId, session);
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
            MonitorPushService.removeSession(userId, session);
            log.info("移除WebSocket会话 - 用户ID: {}, SessionId: {}", userId, session.getId());
        } else {
            // 如果没有userId，尝试移除所有会话（兼容处理）
            MonitorPushService.removeSession(null, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            
            // 解析客户端消息
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            
            if ("ping".equals(type)) {
                // 收到 Ping，立即返回 Pong，携带客户端发送的时间戳
                Long clientTimestamp = ((Number) data.get("ts")).longValue();
                long serverTimestamp = System.currentTimeMillis();
                
                Map<String, Object> pongData = new HashMap<>(4);
                pongData.put("type", "pong");
                pongData.put("clientTs", clientTimestamp);
                pongData.put("serverTs", serverTimestamp);
                
                String pongJson = objectMapper.writeValueAsString(pongData);
                session.sendMessage(new TextMessage(pongJson));
                
                log.debug("RTT Ping/Pong - 客户端时间戳: {}, 服务器时间戳: {}", clientTimestamp, serverTimestamp);
            } else if ("rtt-update".equals(type)) {
                // 收到 RTT 更新消息（客户端测量完 RTT 后立即发送）
                Long userId = extractUserIdFromSession(session);
                if (userId != null && data.containsKey("rtt")) {
                    Long rttMs = ((Number) data.get("rtt")).longValue();
                    probeRttStore.set(userId, rttMs);
                    log.debug("更新WebSocket Ping RTT - 用户ID: {}, RTT: {}ms", userId, rttMs);
                }
            } else {
                log.debug("收到客户端消息: {}", payload);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误 - SessionId: {}, 错误: {}", session.getId(), exception.getMessage());
        Long userId = extractUserIdFromSession(session);
        if (userId != null) {
            MonitorPushService.removeSession(userId, session);
        } else {
            MonitorPushService.removeSession(null, session);
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
