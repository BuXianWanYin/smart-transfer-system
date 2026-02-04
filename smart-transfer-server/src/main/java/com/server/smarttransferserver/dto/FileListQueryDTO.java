package com.server.smarttransferserver.dto;

import lombok.Data;

/**
 * 文件列表查询DTO
 */
@Data
public class FileListQueryDTO {
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
    
    /**
     * 文件状态（可选）
     */
    private String status;
    
    /**
     * 用户ID（可选，仅管理员可用）
     */
    private Long userId;
}
