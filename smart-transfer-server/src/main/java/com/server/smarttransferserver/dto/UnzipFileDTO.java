package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 解压文件DTO
 */
@Data
public class UnzipFileDTO {
    
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    
    @NotNull(message = "解压模式不能为空")
    private Integer unzipMode;
    
    private String folderName;
    
    private Long targetFolderId;
}
