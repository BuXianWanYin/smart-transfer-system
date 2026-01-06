package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 传输任务VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTaskVO {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 传输状态
     */
    private String transferStatus;
    
    /**
     * 传输进度（0-100）
     */
    private Double progress;
    
    /**
     * 传输速率（字节/秒）
     */
    private Long transferSpeed;
    
    /**
     * 使用的拥塞控制算法
     */
    private String algorithm;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}

