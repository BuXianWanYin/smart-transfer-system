package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.domain.RecoveryFile;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.RecoveryFileMapper;
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
    private RecoveryFileMapper recoveryFileMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(Long recoveryId) {
        RecoveryFile recoveryFile = getById(recoveryId);
        if (recoveryFile == null) {
            log.warn("回收站记录不存在，recoveryId: {}", recoveryId);
            return;
        }

        // 还原文件（使用原生SQL绕过@TableLogic）
        fileInfoMapper.updateDelFlag(recoveryFile.getFileId(), 0);

        // 删除回收站记录
        removeById(recoveryId);
        log.info("文件已还原，recoveryId: {}, fileName: {}", recoveryId, recoveryFile.getFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFilePermanently(Long recoveryId) {
        RecoveryFile recoveryFile = getById(recoveryId);
        if (recoveryFile == null) {
            log.warn("回收站记录不存在，recoveryId: {}", recoveryId);
            return;
        }

        // 彻底删除文件记录（物理删除，使用原生SQL）
        fileInfoMapper.deletePhysically(recoveryFile.getFileId());

        // 删除回收站记录
        removeById(recoveryId);
        log.info("文件已彻底删除，recoveryId: {}, fileName: {}", recoveryId, recoveryFile.getFileName());
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
            // 彻底删除文件记录（物理删除）
            fileInfoMapper.deletePhysically(recoveryFile.getFileId());
        }
        // 清空回收站
        remove(wrapper);
        log.info("回收站已清空，删除文件数量: {}, userId: {}", recoveryFiles.size(), userId);
    }
}

