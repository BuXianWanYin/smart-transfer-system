package com.server.smarttransferserver.service;

import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.vo.FolderContentVO;

import java.util.List;

/**
 * 文件夹服务接口
 */
public interface FolderService {

    /**
     * 创建文件夹
     */
    Folder createFolder(String folderName, Long parentId);

    /**
     * 获取文件夹列表
     */
    List<Folder> getFoldersByParentId(Long parentId);

    /**
     * 获取文件夹内容（文件夹+文件）
     */
    FolderContentVO getFolderContent(Long folderId, Integer pageNum, Integer pageSize);

    /**
     * 重命名文件夹
     */
    void renameFolder(Long folderId, String newName);

    /**
     * 删除文件夹
     */
    void deleteFolder(Long folderId);

    /**
     * 获取面包屑路径
     */
    List<Folder> getBreadcrumb(Long folderId);

    /**
     * 移动文件到文件夹
     */
    void moveFileToFolder(Long fileId, Long folderId);

    /**
     * 移动文件夹
     */
    void moveFolderTo(Long folderId, Long targetFolderId);
}

