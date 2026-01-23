package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 移动文件到文件夹DTO
 * 兼容前端传 targetFolderId 或 folderId
 */
@Data
public class MoveFileToFolderDTO {
    
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    
    private Long folderId = 0L;
    
    // 兼容前端可能传的 targetFolderId 字段
    public void setTargetFolderId(Long targetFolderId) {
        if (this.folderId == null || this.folderId == 0L) {
            this.folderId = targetFolderId;
        }
    }
}
