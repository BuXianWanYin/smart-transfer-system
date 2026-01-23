package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 移动文件夹DTO
 */
@Data
public class MoveFolderDTO {
    
    @NotNull(message = "文件夹ID不能为空")
    private Long folderId;
    
    private Long targetFolderId = 0L;
}
