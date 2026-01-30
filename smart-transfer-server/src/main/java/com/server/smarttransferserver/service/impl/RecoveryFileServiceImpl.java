package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.domain.RecoveryFile;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.mapper.CongestionMetricsMapper;
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
    private CongestionMetricsMapper congestionMetricsMapper;
    
    @Autowired
    private IFileStorageService fileStorageService;

    @Override
    public List<RecoveryFile> getRecoveryFileList(Long filterUserId) {
        Long userId = UserContextHolder.getUserId();
        String userRole = UserContextHolder.getRole();
        LambdaQueryWrapper<RecoveryFile> queryWrapper = new LambdaQueryWrapper<>();
        
        // 修复：管理员可以查看所有用户的回收站，普通用户只能查看自己的
        if ("ADMIN".equals(userRole)) {
            // 管理员：如果指定了filterUserId，查询指定用户的回收站；否则查询所有用户的回收站
            if (filterUserId != null) {
                queryWrapper.eq(RecoveryFile::getUserId, filterUserId);
            }
            // 如果未指定filterUserId，不添加userId条件，查询所有用户
        } else {
            // 普通用户：只能查看自己的回收站，忽略filterUserId参数
            if (userId != null) {
                queryWrapper.eq(RecoveryFile::getUserId, userId);
            }
        }
        
        queryWrapper.orderByDesc(RecoveryFile::getDeleteTime);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileToRecovery(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            log.warn("文件不存在，fileId: {}", fileId);
            return;
        }

        // 修复：使用文件所有者的ID作为回收站记录的userId，而不是当前登录用户的ID
        // 这样管理员删除其他用户文件时，回收站记录属于文件所有者，原用户可以在自己的回收站中看到
        Long fileOwnerId = fileInfo.getUserId();
        Long currentUserId = UserContextHolder.getUserId();
        String currentUserRole = UserContextHolder.getRole();

        // 创建回收站记录
        RecoveryFile recoveryFile = RecoveryFile.builder()
                .userId(fileOwnerId)  // 使用文件所有者的ID
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

        log.info("文件已移至回收站，fileId: {}, fileName: {}, 文件所有者: {}, 操作者: {}, 角色: {}", 
                fileId, fileInfo.getFileName(), fileOwnerId, currentUserId, currentUserRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteToRecovery(List<Long> fileIds) {
        String batchNum = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = UserContextHolder.getUserId();
        String currentUserRole = UserContextHolder.getRole();
        
        for (Long fileId : fileIds) {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                continue;
            }

            // 修复：使用文件所有者的ID作为回收站记录的userId
            Long fileOwnerId = fileInfo.getUserId();

            // 创建回收站记录
            RecoveryFile recoveryFile = RecoveryFile.builder()
                    .userId(fileOwnerId)  // 使用文件所有者的ID
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
        log.info("批量删除文件到回收站，数量: {}, batchNum: {}, 操作者: {}, 角色: {}", 
                fileIds.size(), batchNum, currentUserId, currentUserRole);
    }

    /**
     * 删除文件夹到回收站（包括子文件夹和子文件）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolderToRecovery(Long folderId) {
        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            log.warn("文件夹不存在，folderId: {}", folderId);
            return;
        }

        // 修复：使用文件夹所有者的ID作为回收站记录的userId
        Long folderOwnerId = folder.getUserId();
        Long currentUserId = UserContextHolder.getUserId();
        String currentUserRole = UserContextHolder.getRole();

        String batchNum = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        // 创建回收站记录（只记录顶层被删除的文件夹）
        RecoveryFile recoveryFile = RecoveryFile.builder()
                .userId(folderOwnerId)  // 使用文件夹所有者的ID
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
        // 注意：markFolderAndChildrenDeleted方法中的userId参数用于查询，应该使用文件夹所有者的ID
        markFolderAndChildrenDeleted(folderId, batchNum, now, folderOwnerId);

        log.info("文件夹已移至回收站，folderId: {}, folderName: {}, 文件夹所有者: {}, 操作者: {}, 角色: {}", 
                folderId, folder.getFolderName(), folderOwnerId, currentUserId, currentUserRole);
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
            
            // 2. 先删除拥塞指标（外键依赖 transfer_task），再删除传输任务
            List<TransferTask> tasksToDelete = transferTaskMapper.selectByFileId(recoveryFile.getFileId());
            if (tasksToDelete != null) {
                for (TransferTask t : tasksToDelete) {
                    congestionMetricsMapper.deleteByTaskId(t.getTaskId());
                }
            }
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
        
        // 2. 先删除拥塞指标（外键依赖 transfer_task），再删除传输任务
        for (FileInfo file : files) {
            List<TransferTask> tasks = transferTaskMapper.selectByFileId(file.getId());
            if (tasks != null) {
                for (TransferTask t : tasks) {
                    congestionMetricsMapper.deleteByTaskId(t.getTaskId());
                }
            }
        }
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
    public void batchRestoreFiles(List<Long> recoveryIds) {
        if (recoveryIds == null || recoveryIds.isEmpty()) {
            throw new RuntimeException("回收站记录ID列表不能为空");
        }
        for (Long recoveryId : recoveryIds) {
            if (recoveryId != null) {
                restoreFile(recoveryId);
            }
        }
        log.info("批量还原文件 - 数量: {}", recoveryIds.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteFilesPermanently(List<Long> recoveryIds) {
        if (recoveryIds == null || recoveryIds.isEmpty()) {
            throw new RuntimeException("回收站记录ID列表不能为空");
        }
        for (Long recoveryId : recoveryIds) {
            if (recoveryId != null) {
                deleteFilePermanently(recoveryId);
            }
        }
        log.info("批量彻底删除文件 - 数量: {}", recoveryIds.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearRecoveryBin() {
        Long userId = UserContextHolder.getUserId();
        String userRole = UserContextHolder.getRole();
        
        // 修复：管理员可以清空所有用户的回收站，普通用户只能清空自己的
        LambdaQueryWrapper<RecoveryFile> wrapper = new LambdaQueryWrapper<>();
        if ("ADMIN".equals(userRole)) {
            // 管理员：不添加userId条件，清空所有用户的回收站
        } else {
            // 普通用户：只清空自己的回收站
            if (userId != null) {
                wrapper.eq(RecoveryFile::getUserId, userId);
            }
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
                
                // 2. 先删除拥塞指标（外键依赖 transfer_task），再删除传输任务
                List<TransferTask> tasksToDelete = transferTaskMapper.selectByFileId(recoveryFile.getFileId());
                if (tasksToDelete != null) {
                    for (TransferTask t : tasksToDelete) {
                        congestionMetricsMapper.deleteByTaskId(t.getTaskId());
                    }
                }
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

