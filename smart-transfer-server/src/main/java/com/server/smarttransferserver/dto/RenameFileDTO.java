package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 重命名文件DTO
 */
@Data
public class RenameFileDTO {
    
    @NotNull(message = "文件ID不能为空")
    private Long id;
    
    @NotBlank(message = "文件名不能为空")
    private String fileName;
}
