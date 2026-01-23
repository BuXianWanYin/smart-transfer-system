package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 移动文件DTO
 */
@Data
public class MoveFileDTO {
    
    @NotNull(message = "文件ID不能为空")
    private Long id;
    
    @NotNull(message = "目标文件夹ID不能为空")
    private Long targetFolderId;
}
