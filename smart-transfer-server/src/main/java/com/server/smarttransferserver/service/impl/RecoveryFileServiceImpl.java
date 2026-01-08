package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.domain.RecoveryFile;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.FolderMapper;
import com.server.smarttransferserver.mapper.RecoveryFileMapper;
import com.server.smarttransferserver.mapper.TransferTaskMapper;
import com.server.smarttransferserver.service.IFileStorageService;
import com.server.smarttransferserver.service.RecoveryFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.smarttransferserver.util.UserContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 回收站服务实现类
 */
@Slf4j
@Service
public class RecoveryFileServiceImpl extends ServiceImpl<RecoveryFileMapper, RecoveryFile> implements RecoveryFileService {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private FolderMapper folderMapper;
    
    @Autowired
    private TransferTaskMapper transferTaskMapper;
    
    @Autowired
    private IFileStorageService fileStorageService;

    @Override
    public List<RecoveryFile> getRecoveryFileList() {
        Long userId = UserContextHolder.getUserId();
        LambdaQueryWrapper<RecoveryFile> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(RecoveryFile::getUserId, userId);
        }
        queryWrapper.orderByDesc(RecoveryFile::getDeleteTime);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileToRecovery(Long fileId) {
        Long userId = UserContextHolder.getUserId();
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            log.warn("文件不存在，fileId: {}", fileId);
            return;
        }

        // 创建回收站记录
        RecoveryFile recoveryFile = RecoveryFile.builder()
                .userId(userId)
                .fileId(fileInfo.getId())
                .fileName(fileInfo.getFileName())
                .extendName(fileInfo.getExtendName())
                .filePath(fileInfo.getFilePath())
                .folderId(fileInfo.getFolderId())
                .isDir(fileInfo.getIsDir())
                .fileSize(fileInfo.getFileSize())
                .deleteTime(LocalDateTime.now())
                .deleteBatchNum(UUID.randomUUID().toString().replace("-", ""))
                .build();
        save(recoveryFile);

        // 标记文件为已删除（使用原生SQL绕过@TableLogic）
        fileInfoMapper.updateDelFlag(fileId, 1);

        log.info("文件已移至回收站，fileId: {}, fileName: {}, userId: {}", fileId, fileInfo.getFileName(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteToRecovery(List<Long> fileIds) {
        Long userId = UserContextHolder.getUserId();
        String batchNum = UUID.randomUUID().toString().replace("-", "");
        for (Long fileId : fileIds) {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                continue;
            }

            // 创建回收站记录
            RecoveryFile recoveryFile = RecoveryFile.builder()
                    .userId(userId)
                    .fileId(fileInfo.getId())
                    .fileName(fileInfo.getFileName())
                    .extendName(fileInfo.getExtendName())
                    .filePath(fileInfo.getFilePath())
                    .folderId(fileInfo.getFolderId())
                    .isDir(fileInfo.getIsDir())
                    .fileSize(fileInfo.getFileSize())
                    .deleteTime(LocalDateTime.now())
                    .deleteBatchNum(batchNum)
                    .build();
            save(recoveryFile);

            // 标记文件为已删除（使用原生SQL绕过@TableLogic）
            fileInfoMapper.updateDelFlag(fileId, 1);
        }
        log.info("批量删除文件到回收站，数量: {}, batchNum: {}, userId: {}", fileIds.size(), batchNum, userId);
    }

    /**
     * 删除文件夹到回收站（包括子文件夹和子文件）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolderToRecovery(Long folderId) {
        Long userId = UserContextHolder.getUserId();
        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            log.warn("文件夹不存在，folderId: {}", folderId);
            return;
        }

        String batchNum = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        // 创建回收站记录（只记录顶层被删除的文件夹）
        RecoveryFile recoveryFile = RecoveryFile.builder()
                .userId(userId)
                .fileId(null)  // 文件夹没有 fileId
                .fileName(folder.getFolderName())
                .extendName(null)
                .filePath(folder.getPath())
                .folderId(folder.getParentId())
                .isDir(1)
                .fileSize(0L)
                .deleteTime(now)
                .deleteBatchNum(batchNum)
                .originalFolderId(folderId)  // 记录文件夹ID
                .build();
        save(recoveryFile);

        // 标记该文件夹及所有子文件夹和子文件为已删除
        markFolderAndChildrenDeleted(folderId, batchNum, now, userId);

        log.info("文件夹已移至回收站，folderId: {}, folderName: {}, userId: {}", folderId, folder.getFolderName(), userId);
    }

    /**
     * 递归标记文件夹及其子内容为已删除
     */
    private void markFolderAndChildrenDeleted(Long folderId, String batchNum, LocalDateTime deleteTime, Long userId) {
        // 标记当前文件夹为已删除
        LambdaUpdateWrapper<Folder> folderUpdate = new LambdaUpdateWrapper<>();
        folderUpdate.eq(Folder::getId, folderId)
                .set(Folder::getDelFlag, 1)
                .set(Folder::getDeleteBatchNum, batchNum)
                .set(Folder::getDeleteTime, deleteTime);
        folderMapper.update(null, folderUpdate);

        // 标记该文件夹下的文件为已删除
        LambdaQueryWrapper<FileInfo> fileQuery = new LambdaQueryWrapper<>();
        fileQuery.eq(FileInfo::getFolderId, folderId)
                .eq(FileInfo::getDelFlag, 0);
        if (userId != null) {
            fileQuery.eq(FileInfo::getUserId, userId);
        }
        List<FileInfo> files = fileInfoMapper.selectList(fileQuery);
        for (FileInfo file : files) {
            fileInfoMapper.updateDelFlagWithBatch(file.getId(), 1, batchNum);
        }

        // 递归处理子文件夹
        LambdaQueryWrapper<Folder> subFolderQuery = new LambdaQueryWrapper<>();
        subFolderQuery.eq(Folder::getParentId, folderId)
                .eq(Folder::getDelFlag, 0);
        if (userId != null) {
            subFolderQuery.eq(Folder::getUserId, userId);
        }
        List<Folder> subFolders = folderMapper.selectList(subFolderQuery);
        for (Folder subFolder : subFolders) {
            markFolderAndChildrenDeleted(subFolder.getId(), batchNum, deleteTime, userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(Long recoveryId) {
        RecoveryFile recoveryFile = getById(recoveryId);
        if (recoveryFile == null) {
            log.warn("回收站记录不存在，recoveryId: {}", recoveryId);
            return;
        }

        String batchNum = recoveryFile.getDeleteBatchNum();

        if (recoveryFile.getIsDir() == 1 && recoveryFile.getOriginalFolderId() != null) {
            // 还原文件夹及其所有内容
            restoreFolderAndChildren(batchNum);
            log.info("文件夹已还原，recoveryId: {}, folderName: {}", recoveryId, recoveryFile.getFileName());
        } else {
            // 还原单个文件（使用原生SQL绕过@TableLogic）
            fileInfoMapper.updateDelFlag(recoveryFile.getFileId(), 0);
            log.info("文件已还原，recoveryId: {}, fileName: {}", recoveryId, recoveryFile.getFileName());
        }

        // 删除回收站记录
        removeById(recoveryId);
    }

    /**
     * 还原文件夹及其所有内容
     */
    private void restoreFolderAndChildren(String batchNum) {
        // 还原所有标记为该批次号的文件夹
        LambdaUpdateWrapper<Folder> folderUpdate = new LambdaUpdateWrapper<>();
        folderUpdate.eq(Folder::getDeleteBatchNum, batchNum)
                .set(Folder::getDelFlag, 0)
                .set(Folder::getDeleteBatchNum, null)
                .set(Folder::getDeleteTime, null);
        folderMapper.update(null, folderUpdate);

        // 还原所有标记为该批次号的文件
        fileInfoMapper.restoreByBatchNum(batchNum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFilePermanently(Long recoveryId) {
        RecoveryFile recoveryFile = getById(recoveryId);
        if (recoveryFile == null) {
            log.warn("回收站记录不存在，recoveryId: {}", recoveryId);
            return;
        }

        String batchNum = recoveryFile.getDeleteBatchNum();

        if (recoveryFile.getIsDir() == 1 && recoveryFile.getOriginalFolderId() != null) {
            // 彻底删除文件夹及其所有内容
            deleteFolderAndChildrenPermanently(batchNum);
            log.info("文件夹已彻底删除，recoveryId: {}, folderName: {}", recoveryId, recoveryFile.getFileName());
        } else {
            // 1. 获取文件信息，删除物理文件
            FileInfo fileInfo = fileInfoMapper.selectById(recoveryFile.getFileId());
            if (fileInfo != null && fileInfo.getFilePath() != null) {
                fileStorageService.deleteFile(fileInfo.getFilePath());
            }
            // 2. 删除关联的传输任务记录
            transferTaskMapper.deleteByFileId(recoveryFile.getFileId());
            // 3. 彻底删除单个文件记录（物理删除，使用原生SQL）
            fileInfoMapper.deletePhysically(recoveryFile.getFileId());
            log.info("文件已彻底删除，recoveryId: {}, fileName: {}", recoveryId, recoveryFile.getFileName());
        }

        // 删除回收站记录
        removeById(recoveryId);
    }

    /**
     * 彻底删除文件夹及其所有内容
     */
    private void deleteFolderAndChildrenPermanently(String batchNum) {
        // 1. 查询所有标记为该批次号的文件，删除物理文件
        LambdaQueryWrapper<FileInfo> fileQuery = new LambdaQueryWrapper<>();
        fileQuery.eq(FileInfo::getDeleteBatchNum, batchNum);
        List<FileInfo> files = fileInfoMapper.selectList(fileQuery);
        for (FileInfo file : files) {
            if (file.getFilePath() != null && file.getIsDir() != 1) {
                fileStorageService.deleteFile(file.getFilePath());
            }
        }
        
        // 2. 删除关联的传输任务记录（通过批次号关联的文件）
        transferTaskMapper.deleteByBatchNum(batchNum);
        
        // 3. 彻底删除所有标记为该批次号的文件记录
        fileInfoMapper.deletePhysicallyByBatchNum(batchNum);

        // 4. 彻底删除所有标记为该批次号的文件夹记录
        LambdaQueryWrapper<Folder> folderQuery = new LambdaQueryWrapper<>();
        folderQuery.eq(Folder::getDeleteBatchNum, batchNum);
        folderMapper.delete(folderQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearRecoveryBin() {
        Long userId = UserContextHolder.getUserId();
        
        // 只清空当前用户的回收站
        LambdaQueryWrapper<RecoveryFile> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(RecoveryFile::getUserId, userId);
        }
        
        List<RecoveryFile> recoveryFiles = list(wrapper);
        for (RecoveryFile recoveryFile : recoveryFiles) {
            String batchNum = recoveryFile.getDeleteBatchNum();
            
            if (recoveryFile.getIsDir() == 1 && recoveryFile.getOriginalFolderId() != null) {
                // 彻底删除文件夹及其所有内容（包括物理文件）
                deleteFolderAndChildrenPermanently(batchNum);
            } else if (recoveryFile.getFileId() != null) {
                // 1. 获取文件信息，删除物理文件
                FileInfo fileInfo = fileInfoMapper.selectById(recoveryFile.getFileId());
                if (fileInfo != null && fileInfo.getFilePath() != null) {
                    fileStorageService.deleteFile(fileInfo.getFilePath());
                }
                // 2. 删除关联的传输任务记录
                transferTaskMapper.deleteByFileId(recoveryFile.getFileId());
                // 3. 彻底删除单个文件记录（物理删除）
                fileInfoMapper.deletePhysically(recoveryFile.getFileId());
            }
        }
        // 清空回收站
        remove(wrapper);
        log.info("回收站已清空，删除记录数量: {}, userId: {}", recoveryFiles.size(), userId);
    }
}

