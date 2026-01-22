package com.server.smarttransferserver.config;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.entity.User;
import com.server.smarttransferserver.mapper.UserMapper;
import com.server.smarttransferserver.util.JwtUtil;
import com.server.smarttransferserver.util.UserContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 拦截器
 * 从请求头中解析 Token 并设置用户上下文
 * 同时检查管理员权限
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private UserMapper userMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                
                UserContextHolder.setUserId(userId);
                UserContextHolder.setUsername(username);
                
                // 查询用户角色并设置到上下文
                User user = userMapper.selectById(userId);
                if (user != null) {
                    String role = user.getRole() != null ? user.getRole() : "USER";
                    UserContextHolder.setRole(role);
                }
            }
        }
        
        // 检查是否需要管理员权限
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
            if (requireAdmin == null) {
                requireAdmin = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
            }
            
            if (requireAdmin != null) {
                String role = UserContextHolder.getRole();
                if (role == null || !"ADMIN".equals(role)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    try {
                        response.getWriter().write("{\"code\":403,\"message\":\"需要管理员权限\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
                    } catch (IOException e) {
                        // 忽略
                    }
                    return false;
                }
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

