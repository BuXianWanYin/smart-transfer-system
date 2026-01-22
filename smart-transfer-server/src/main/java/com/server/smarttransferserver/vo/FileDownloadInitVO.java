package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件下载初始化结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadInitVO {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 总分块数
     */
    private Integer totalChunks;
    
    /**
     * 分块大小（字节）
     */
    private Long chunkSize;
    
    /**
     * 已下载的分块列表
     */
    private List<Integer> downloaded;
    
    /**
     * 任务ID（用于拥塞控制）
     */
    private String taskId;
    
    /**
     * 消息
     */
    private String message;
}
