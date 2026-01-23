package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 更新用户状态DTO
 */
@Data
public class UpdateUserStatusDTO {
    
    @NotNull(message = "状态不能为空")
    private Integer status;
}
