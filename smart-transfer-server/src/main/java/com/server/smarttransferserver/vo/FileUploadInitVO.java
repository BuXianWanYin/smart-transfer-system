package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件上传初始化VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadInitVO {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 是否秒传（文件已存在）
     */
    private Boolean quickUpload;
    
    /**
     * 已上传的分片列表（断点续传）
     */
    private List<Integer> uploadedChunks;
    
    /**
     * 文件路径（秒传时返回）
     */
    private String filePath;
    
    /**
     * 提示信息
     */
    private String message;
}

