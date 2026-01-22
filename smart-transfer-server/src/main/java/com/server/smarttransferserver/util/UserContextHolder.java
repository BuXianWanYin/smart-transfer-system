package com.server.smarttransferserver.util;

/**
 * 用户上下文持有者
 * 使用 ThreadLocal 存储当前请求的用户信息
 */
public class UserContextHolder {
    
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }
    
    /**
     * 设置当前用户名
     */
    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }
    
    /**
     * 设置当前用户角色
     */
    public static void setRole(String role) {
        USER_ROLE_HOLDER.set(role);
    }
    
    /**
     * 获取当前用户角色
     */
    public static String getRole() {
        return USER_ROLE_HOLDER.get();
    }
    
    /**
     * 清除当前用户信息
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
        USER_ROLE_HOLDER.remove();
    }
}

