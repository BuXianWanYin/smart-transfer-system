package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除用户DTO
 */
@Data
public class BatchDeleteUsersDTO {
    
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
}
