package com.server.smarttransferserver.dto;

import lombok.Data;

/**
 * 传输任务查询DTO
 */
@Data
public class TransferTaskQueryDTO {
    
    /**
     * 任务类型
     * UPLOAD, DOWNLOAD
     */
    private String taskType;
    
    /**
     * 传输状态
     * PENDING, PROCESSING, COMPLETED, FAILED, PAUSED
     */
    private String transferStatus;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}

