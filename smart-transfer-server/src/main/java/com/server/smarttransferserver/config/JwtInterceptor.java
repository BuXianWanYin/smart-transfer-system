package com.server.smarttransferserver.config;

import com.server.smarttransferserver.util.JwtUtil;
import com.server.smarttransferserver.util.UserContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器
 * 从请求头中解析 Token 并设置用户上下文
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                
                UserContextHolder.setUserId(userId);
                UserContextHolder.setUsername(username);
            }
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 请求结束后清除用户上下文
        UserContextHolder.clear();
    }
}

