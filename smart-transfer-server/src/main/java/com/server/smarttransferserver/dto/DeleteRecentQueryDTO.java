package com.server.smarttransferserver.dto;

import lombok.Data;

/**
 * 删除近期传输历史查询DTO
 */
@Data
public class DeleteRecentQueryDTO {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 传输类型
     */
    private String transferType;
    
    /**
     * 时间范围（秒）
     */
    private Integer withinSeconds = 120;
}
