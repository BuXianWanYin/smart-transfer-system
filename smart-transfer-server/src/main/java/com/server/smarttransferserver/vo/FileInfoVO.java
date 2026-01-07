package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoVO {
    
    /**
     * 文件ID
     */
    private Long id;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件扩展名
     */
    private String extendName;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 文件哈希
     */
    private String fileHash;
    
    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 所属文件夹ID
     */
    private Long folderId;
    
    /**
     * 是否目录（0文件 1目录）
     */
    private Integer isDir;
    
    /**
     * 上传状态
     */
    private String uploadStatus;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

