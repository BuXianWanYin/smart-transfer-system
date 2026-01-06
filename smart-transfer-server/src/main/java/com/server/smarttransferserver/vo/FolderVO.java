package com.server.smarttransferserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件夹VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderVO {

    private Long id;

    private String folderName;

    private Long parentId;

    private String path;

    /**
     * 类型标识：folder
     */
    private String type = "folder";

    /**
     * 子文件夹数量
     */
    private Integer subFolderCount;

    /**
     * 子文件数量
     */
    private Integer fileCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

