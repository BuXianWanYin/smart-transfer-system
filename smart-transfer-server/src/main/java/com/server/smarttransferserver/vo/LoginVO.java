package com.server.smarttransferserver.vo;

import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO {
    
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 用户角色：ADMIN-管理员，USER-普通用户
     */
    private String role;
}

