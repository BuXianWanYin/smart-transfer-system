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
     * 是否跳过上传（文件已存在，秒传）
     * 兼容 vue-simple-uploader
     */
    private Boolean skipUpload;
    
    /**
     * 是否秒传（文件已存在）
     */
    private Boolean quickUpload;
    
    /**
     * 已上传的分片列表（断点续传）
     */
    private List<Integer> uploaded;
    
    /**
     * 文件路径（秒传时返回）
     */
    private String filePath;
    
    /**
     * 提示信息
     */
    private String message;
    
    /**
     * 任务ID（用于监控数据匹配）
     */
    private String taskId;
}

