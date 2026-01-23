package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 复制文件DTO
 */
@Data
public class CopyFileDTO {
    
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    
    @NotNull(message = "目标文件夹ID不能为空")
    private Long targetFolderId;
}
