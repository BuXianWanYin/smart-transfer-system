package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 重命名文件夹DTO
 */
@Data
public class RenameFolderDTO {
    
    @NotNull(message = "文件夹ID不能为空")
    private Long folderId;
    
    @NotBlank(message = "新名称不能为空")
    private String newName;
    
    // 兼容前端可能传的 id 字段（如果前端传 id，映射到 folderId）
    public void setId(Long id) {
        if (this.folderId == null) {
            this.folderId = id;
        }
    }
    
    // 兼容前端可能传的 folderName 字段（如果前端传 folderName，映射到 newName）
    public void setFolderName(String folderName) {
        if (this.newName == null) {
            this.newName = folderName;
        }
    }
}
