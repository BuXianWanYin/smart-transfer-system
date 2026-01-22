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
     * @param folderId 文件夹ID
     * @param fileType 文件类型筛选（0-全部, 1-图片, 2-文档, 3-视频, 4-音乐, 5-其他）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param userId 用户ID（可选，仅管理员可用，用于筛选指定用户的文件）
     * @return 文件夹内容
     */
    FolderContentVO getFolderContent(Long folderId, Integer fileType, Integer pageNum, Integer pageSize, Long userId);

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
    
    /**
     * 获取文件夹树结构
     * @return 文件夹树
     */
    Object getFolderTree();
}

