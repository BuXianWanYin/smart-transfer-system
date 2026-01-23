package com.server.smarttransferserver.dto;

import lombok.Data;

/**
 * 更新用户信息DTO
 */
@Data
public class UpdateUserInfoDTO {
    
    private String nickname;
    
    private String email;
    
    private String phone;
}
