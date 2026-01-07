package com.server.smarttransferserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.vo.FileInfoVO;

import java.util.List;

/**
 * 文件信息服务接口
 * 提供文件信息的查询、更新等功能
 */
public interface FileInfoService extends IService<FileInfo> {
    
    
    /**
     * 根据文件哈希查询文件信息
     *
     * @param fileHash 文件哈希值
     * @return 文件信息，不存在返回null
     */
    FileInfo getByFileHash(String fileHash);
    
    /**
     * 根据ID获取文件信息（返回VO）
     *
     * @param id 文件ID
     * @return 文件信息VO
     */
    FileInfoVO getFileById(Long id);
    
    /**
     * 分页查询文件列表（返回VO）
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param status 文件状态（可选）
     * @return 分页结果
     */
    IPage<FileInfoVO> getFileList(Integer pageNum, Integer pageSize, String status);
    
    /**
     * 更新文件上传状态
     *
     * @param fileId 文件ID
     * @param status 上传状态
     * @return 是否更新成功
     */
    boolean updateUploadStatus(Long fileId, String status);
    
    /**
     * 删除文件
     *
     * @param id 文件ID
     */
    void deleteFile(Long id);
    
    /**
     * 搜索文件
     *
     * @param fileName 文件名关键词
     * @return 文件列表
     */
    List<FileInfoVO> searchByFileName(String fileName);
    
    /**
     * 重命名文件
     *
     * @param id 文件ID
     * @param newFileName 新文件名
     */
    void renameFile(Long id, String newFileName);
    
    /**
     * 移动文件到指定文件夹
     *
     * @param id 文件ID
     * @param targetFolderId 目标文件夹ID
     */
    void moveFile(Long id, Long targetFolderId);
    
    /**
     * 批量移动文件
     *
     * @param fileIds 文件ID列表
     * @param targetFolderId 目标文件夹ID
     */
    void batchMoveFiles(List<Long> fileIds, Long targetFolderId);
    
    /**
     * 批量删除文件（移动到回收站）
     *
     * @param ids 文件ID列表
     */
    void batchDeleteFiles(List<Long> ids);
    
    /**
     * 复制文件
     *
     * @param fileId 文件ID
     * @param targetFolderId 目标文件夹ID
     */
    void copyFile(Long fileId, Long targetFolderId);
    
    /**
     * 批量复制文件
     *
     * @param fileIds 文件ID列表
     * @param targetFolderId 目标文件夹ID
     */
    void batchCopyFiles(List<Long> fileIds, Long targetFolderId);
    
    /**
     * 解压文件
     *
     * @param fileId 文件ID
     * @param unzipMode 解压模式：1-当前文件夹，2-新建文件夹，3-指定路径
     * @param folderName 新建的文件夹名称（模式2使用）
     * @param targetFolderId 目标文件夹ID（模式3使用）
     */
    void unzipFile(Long fileId, Integer unzipMode, String folderName, Long targetFolderId);
}

