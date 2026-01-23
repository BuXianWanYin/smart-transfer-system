package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量更新用户状态DTO
 */
@Data
public class BatchUpdateUserStatusDTO {
    
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
    
    @NotNull(message = "状态不能为空")
    private Integer status;
}
