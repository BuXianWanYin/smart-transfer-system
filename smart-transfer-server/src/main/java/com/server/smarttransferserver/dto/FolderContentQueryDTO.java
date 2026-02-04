package com.server.smarttransferserver.dto;

import lombok.Data;

/**
 * 文件夹内容查询DTO
 */
@Data
public class FolderContentQueryDTO {
    
    /**
     * 父文件夹ID
     */
    private Long parentId = 0L;
    
    /**
     * 文件类型筛选：0-全部, 1-图片, 2-文档, 3-视频, 4-音乐, 5-其他
     */
    private Integer fileType = 0;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
    
    /**
     * 用户ID（可选，仅管理员可用）
     */
    private Long userId;
}
