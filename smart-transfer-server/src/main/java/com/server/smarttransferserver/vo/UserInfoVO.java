package com.server.smarttransferserver.vo;

import lombok.Data;

import java.util.Date;

/**
 * 用户信息 VO
 */
@Data
public class UserInfoVO {
    
    private Long id;
    
    private String username;
    
    private String nickname;
    
    private String avatar;
    
    private String email;
    
    private String phone;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    private Date lastLoginTime;
    
    private Date createTime;
    
    /**
     * 用户角色：ADMIN-管理员，USER-普通用户
     */
    private String role;
}

