package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadVO {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 分片序号
     */
    private Integer chunkNumber;
    
    /**
     * 是否上传成功
     */
    private Boolean success;
    
    /**
     * 已完成的分片数
     */
    private Integer completedChunks;
    
    /**
     * 总分片数
     */
    private Integer totalChunks;
    
    /**
     * 上传进度（百分比）
     */
    private Double progress;
    
    /**
     * 提示信息
     */
    private String message;
}

