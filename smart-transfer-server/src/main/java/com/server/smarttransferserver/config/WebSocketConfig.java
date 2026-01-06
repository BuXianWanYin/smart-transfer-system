package com.server.smarttransferserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.server.smarttransferserver.websocket.MonitorWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MonitorWebSocketHandler monitorHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(monitorHandler, "/ws/monitor")
                .setAllowedOrigins("*");
    }
}

