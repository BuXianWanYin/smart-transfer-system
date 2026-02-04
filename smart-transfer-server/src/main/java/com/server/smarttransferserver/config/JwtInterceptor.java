package com.server.smarttransferserver.config;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.entity.User;
import com.server.smarttransferserver.mapper.UserMapper;
import com.server.smarttransferserver.util.JwtUtil;
import com.server.smarttransferserver.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private UserMapper userMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // POST /user/avatar 需要认证（上传头像）
        // GET /user/avatar/avatars/** 不需要认证（访问头像静态资源，已在WebMvcConfig中排除）
        // 这里确保 POST /user/avatar 会经过认证检查
        
        // 优先从 Authorization 头获取 token
        String token = null;
        String tokenSource = null;
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            tokenSource = "Authorization头";
            log.debug("从Authorization头获取Token - 长度: {}", token.length());
        }
        
        // 如果 Authorization 头中没有 token，尝试从 URL 查询参数中获取（用于img/video/audio标签）
        if (token == null || token.trim().isEmpty()) {
            String tokenParam = request.getParameter("token");
            log.debug("URL参数token: {}", tokenParam != null ? "存在(长度:" + tokenParam.length() + ")" : "不存在");
            
            if (tokenParam != null && !tokenParam.trim().isEmpty()) {
                token = tokenParam;
                tokenSource = "URL参数";
                log.debug("从URL参数获取Token - 长度: {}", token.length());
            }
        }
        
        if (token != null && !token.trim().isEmpty()) {
            log.debug("尝试验证Token - 来源: {}", tokenSource);
            
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                
                log.info("Token验证成功 - 来源: {}, 用户ID: {}, 用户名: {}", tokenSource, userId, username);
                
                UserContextHolder.setUserId(userId);
                UserContextHolder.setUsername(username);
                
                // 查询用户角色并设置到上下文
                User user = userMapper.selectById(userId);
                if (user != null) {
                    String role = user.getRole() != null ? user.getRole() : "USER";
                    UserContextHolder.setRole(role);
                    log.debug("设置用户角色: {}", role);
                } else {
                    log.warn("用户不存在 - 用户ID: {}", userId);
                }
            } else {
                // Token 无效，记录日志
                log.warn("Token验证失败 - 来源: {}, URI: {}, Method: {}", tokenSource, requestURI, method);
            }
        } else {
            // 没有 token
            log.debug("未找到Token - URI: {}, Method: {}", requestURI, method);
            
            // 对于需要认证的接口（如 POST /user/avatar），记录警告
            if ("/user/avatar".equals(requestURI) && "POST".equals(method)) {
                log.warn("上传头像请求缺少Authorization头 - URI: {}, Method: {}", requestURI, method);
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

