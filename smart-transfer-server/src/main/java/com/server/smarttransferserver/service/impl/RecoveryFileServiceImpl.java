package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.domain.RecoveryFile;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
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

        // 使用MyBatis Plus的LambdaUpdateWrapper标记文件为已删除
        LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FileInfo::getId, fileId)
                .set(FileInfo::getDelFlag, 1)
                .set(FileInfo::getUpdateTime, LocalDateTime.now());
        fileInfoMapper.update(null, updateWrapper);

        log.info("文件已移至回收站，fileId: {}, fileName: {}, userId: {}", fileId, fileInfo.getFileName(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteToRecovery(List<Long> fileIds) {
        Long userId = UserContextHolder.getUserId();
        String batchNum = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        
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
                    .deleteTime(now)
                    .deleteBatchNum(batchNum)
                    .build();
            save(recoveryFile);

            // 使用MyBatis Plus的LambdaUpdateWrapper标记文件为已删除
            LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FileInfo::getId, fileId)
                    .set(FileInfo::getDelFlag, 1)
                    .set(FileInfo::getDeleteBatchNum, batchNum)
                    .set(FileInfo::getUpdateTime, now);
            fileInfoMapper.update(null, updateWrapper);
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
        // 使用MyBatis Plus的LambdaUpdateWrapper标记当前文件夹为已删除
        LambdaUpdateWrapper<Folder> folderUpdate = new LambdaUpdateWrapper<>();
        folderUpdate.eq(Folder::getId, folderId)
                .set(Folder::getDelFlag, 1)
                .set(Folder::getDeleteBatchNum, batchNum)
                .set(Folder::getDeleteTime, deleteTime)
                .set(Folder::getUpdateTime, deleteTime);
        folderMapper.update(null, folderUpdate);

        // 使用MyBatis Plus的LambdaUpdateWrapper标记该文件夹下的文件为已删除
        LambdaQueryWrapper<FileInfo> fileQuery = new LambdaQueryWrapper<>();
        fileQuery.eq(FileInfo::getFolderId, folderId)
                .eq(FileInfo::getDelFlag, 0);
        if (userId != null) {
            fileQuery.eq(FileInfo::getUserId, userId);
        }
        List<FileInfo> files = fileInfoMapper.selectList(fileQuery);
        for (FileInfo file : files) {
            LambdaUpdateWrapper<FileInfo> fileUpdate = new LambdaUpdateWrapper<>();
            fileUpdate.eq(FileInfo::getId, file.getId())
                    .set(FileInfo::getDelFlag, 1)
                    .set(FileInfo::getDeleteBatchNum, batchNum)
                    .set(FileInfo::getUpdateTime, deleteTime);
            fileInfoMapper.update(null, fileUpdate);
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
            // 使用MyBatis Plus的LambdaUpdateWrapper还原单个文件
            LambdaUpdateWrapper<FileInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FileInfo::getId, recoveryFile.getFileId())
                    .set(FileInfo::getDelFlag, 0)
                    .set(FileInfo::getDeleteBatchNum, null)
                    .set(FileInfo::getUpdateTime, LocalDateTime.now());
            fileInfoMapper.update(null, updateWrapper);
            log.info("文件已还原，recoveryId: {}, fileName: {}", recoveryId, recoveryFile.getFileName());
        }

        // 删除回收站记录
        removeById(recoveryId);
    }

    /**
     * 还原文件夹及其所有内容
     */
    private void restoreFolderAndChildren(String batchNum) {
        LocalDateTime now = LocalDateTime.now();
        
        // 使用MyBatis Plus的LambdaUpdateWrapper还原所有标记为该批次号的文件夹
        LambdaUpdateWrapper<Folder> folderUpdate = new LambdaUpdateWrapper<>();
        folderUpdate.eq(Folder::getDeleteBatchNum, batchNum)
                .set(Folder::getDelFlag, 0)
                .set(Folder::getDeleteBatchNum, null)
                .set(Folder::getDeleteTime, null)
                .set(Folder::getUpdateTime, now);
        folderMapper.update(null, folderUpdate);

        // 使用MyBatis Plus的LambdaUpdateWrapper还原所有标记为该批次号的文件
        LambdaUpdateWrapper<FileInfo> fileUpdate = new LambdaUpdateWrapper<>();
        fileUpdate.eq(FileInfo::getDeleteBatchNum, batchNum)
                .set(FileInfo::getDelFlag, 0)
                .set(FileInfo::getDeleteBatchNum, null)
                .set(FileInfo::getUpdateTime, now);
        fileInfoMapper.update(null, fileUpdate);
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
            // **修复MODULE-4: 检查是否有活跃的下载任务**
            if (recoveryFile.getFileId() != null) {
                List<TransferTask> activeTasks = transferTaskMapper.selectByFileId(recoveryFile.getFileId());
                if (activeTasks != null && !activeTasks.isEmpty()) {
                    boolean hasActiveDownload = activeTasks.stream()
                        .anyMatch(t -> "DOWNLOAD".equals(t.getTaskType()) && 
                                      ("PENDING".equals(t.getTransferStatus()) || 
                                       "PROCESSING".equals(t.getTransferStatus())));
                    if (hasActiveDownload) {
                        throw new RuntimeException("文件正在被下载，无法彻底删除。请先取消下载任务或等待下载完成");
                    }
                }
            }
            
            // 1. 优先从FileInfo获取文件路径，如果不存在则从RecoveryFile获取
            String filePath = null;
            FileInfo fileInfo = fileInfoMapper.selectById(recoveryFile.getFileId());
            if (fileInfo != null && fileInfo.getFilePath() != null) {
                filePath = fileInfo.getFilePath();
            } else if (recoveryFile.getFilePath() != null) {
                // 如果FileInfo已被删除，从RecoveryFile中获取保存的文件路径
                filePath = recoveryFile.getFilePath();
                log.warn("文件记录已不存在，使用RecoveryFile中保存的路径: {}", filePath);
            }
            
            // 删除物理文件
            if (filePath != null) {
                fileStorageService.deleteFile(filePath);
            } else {
                log.warn("无法获取文件路径，跳过物理文件删除 - recoveryId: {}, fileId: {}", 
                        recoveryId, recoveryFile.getFileId());
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
                // **改进：清空回收站时也检查是否有活跃的下载任务（防止误删）**
                List<TransferTask> activeTasks = transferTaskMapper.selectByFileId(recoveryFile.getFileId());
                if (activeTasks != null && !activeTasks.isEmpty()) {
                    boolean hasActiveDownload = activeTasks.stream()
                        .anyMatch(t -> "DOWNLOAD".equals(t.getTaskType()) && 
                                      ("PENDING".equals(t.getTransferStatus()) || 
                                       "PROCESSING".equals(t.getTransferStatus())));
                    if (hasActiveDownload) {
                        log.warn("跳过删除正在下载的文件 - recoveryId: {}, fileId: {}", 
                                recoveryFile.getId(), recoveryFile.getFileId());
                        continue; // 跳过此文件，继续处理下一个
                    }
                }
                
                // 1. 优先从FileInfo获取文件路径，如果不存在则从RecoveryFile获取
                String filePath = null;
                FileInfo fileInfo = fileInfoMapper.selectById(recoveryFile.getFileId());
                if (fileInfo != null && fileInfo.getFilePath() != null) {
                    filePath = fileInfo.getFilePath();
                } else if (recoveryFile.getFilePath() != null) {
                    // 如果FileInfo已被删除，从RecoveryFile中获取保存的文件路径
                    filePath = recoveryFile.getFilePath();
                    log.warn("文件记录已不存在，使用RecoveryFile中保存的路径: {}", filePath);
                }
                
                // 删除物理文件
                if (filePath != null) {
                    fileStorageService.deleteFile(filePath);
                } else {
                    log.warn("无法获取文件路径，跳过物理文件删除 - recoveryId: {}, fileId: {}", 
                            recoveryFile.getId(), recoveryFile.getFileId());
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

