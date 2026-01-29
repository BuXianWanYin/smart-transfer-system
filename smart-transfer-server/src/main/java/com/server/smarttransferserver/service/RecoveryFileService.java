package com.server.smarttransferserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.domain.RecoveryFile;

import java.util.List;

/**
 * 回收站服务接口
 */
public interface RecoveryFileService extends IService<RecoveryFile> {

    /**
     * 获取回收站文件列表
     *
     * @param filterUserId 用户ID（可选，仅管理员可用，用于筛选指定用户的回收站）
     * @return 回收站文件列表
     */
    List<RecoveryFile> getRecoveryFileList(Long filterUserId);

    /**
     * 删除文件到回收站
     *
     * @param fileId 文件ID
     */
    void deleteFileToRecovery(Long fileId);

    /**
     * 批量删除文件到回收站
     *
     * @param fileIds 文件ID列表
     */
    void batchDeleteToRecovery(List<Long> fileIds);

    /**
     * 删除文件夹到回收站（包括子文件夹和子文件）
     *
     * @param folderId 文件夹ID
     */
    void deleteFolderToRecovery(Long folderId);

    /**
     * 还原文件
     *
     * @param recoveryId 回收站记录ID
     */
    void restoreFile(Long recoveryId);

    /**
     * 彻底删除文件
     *
     * @param recoveryId 回收站记录ID
     */
    void deleteFilePermanently(Long recoveryId);

    /**
     * 清空回收站
     */
    void clearRecoveryBin();
    
    /**
     * 批量还原文件
     * @param recoveryIds 回收站记录ID列表
     */
    void batchRestoreFiles(List<Long> recoveryIds);
    
    /**
     * 批量彻底删除文件
     * @param recoveryIds 回收站记录ID列表
     */
    void batchDeleteFilesPermanently(List<Long> recoveryIds);
}

