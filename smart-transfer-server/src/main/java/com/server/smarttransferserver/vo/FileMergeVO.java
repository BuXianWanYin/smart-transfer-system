package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件合并VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMergeVO {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 是否合并成功
     */
    private Boolean success;
    
    /**
     * 是否校验通过
     */
    private Boolean verified;
    
    /**
     * 传输任务ID
     */
    private String taskId;
    
    /**
     * 提示信息
     */
    private String message;
}

