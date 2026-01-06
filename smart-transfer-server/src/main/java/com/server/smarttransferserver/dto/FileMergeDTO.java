package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 文件合并DTO
 */
@Data
public class FileMergeDTO {
    
    /**
     * 文件ID
     */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    
    /**
     * 文件哈希值（用于校验）
     */
    @NotBlank(message = "文件哈希值不能为空")
    private String fileHash;
    
    /**
     * 拥塞控制算法（可选）
     * CUBIC, BBR, ADAPTIVE
     */
    private String algorithm;
}

