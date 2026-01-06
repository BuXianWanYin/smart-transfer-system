package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 文件上传初始化DTO
 */
@Data
public class FileUploadInitDTO {
    
    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;
    
    /**
     * 文件哈希值（MD5或SHA256）
     */
    @NotBlank(message = "文件哈希值不能为空")
    private String fileHash;
    
    /**
     * 分片大小（字节）
     */
    @NotNull(message = "分片大小不能为空")
    @Min(value = 1024, message = "分片大小不能小于1KB")
    private Long chunkSize;
    
    /**
     * 总分片数
     */
    @NotNull(message = "总分片数不能为空")
    @Min(value = 1, message = "总分片数必须大于0")
    private Integer totalChunks;
}

