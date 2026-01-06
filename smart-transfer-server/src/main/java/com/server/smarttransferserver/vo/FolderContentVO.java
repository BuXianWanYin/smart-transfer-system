package com.server.smarttransferserver.vo;

import com.server.smarttransferserver.domain.Folder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件夹内容VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderContentVO {

    /**
     * 当前文件夹ID
     */
    private Long currentFolderId;

    /**
     * 当前文件夹名称
     */
    private String currentFolderName;

    /**
     * 面包屑路径
     */
    private List<Folder> breadcrumb;

    /**
     * 子文件夹列表
     */
    private List<FolderVO> folders;

    /**
     * 文件列表
     */
    private List<FileInfoVO> files;

    /**
     * 文件总数
     */
    private Long fileTotal;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}

