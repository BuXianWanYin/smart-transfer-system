package com.server.smarttransferserver.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 分片上传DTO
 */
@Data
public class ChunkUploadDTO {
    
    /**
     * 文件ID
     */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    
    /**
     * 分片序号（从0开始）
     */
    @NotNull(message = "分片序号不能为空")
    @Min(value = 0, message = "分片序号不能小于0")
    private Integer chunkNumber;
    
    /**
     * 分片哈希值
     */
    @NotBlank(message = "分片哈希值不能为空")
    private String chunkHash;
    
    /**
     * 分片文件
     */
    @NotNull(message = "分片文件不能为空")
    private MultipartFile file;
}

