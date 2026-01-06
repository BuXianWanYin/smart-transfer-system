package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.server.smarttransferserver.dto.FileMergeDTO;
import com.server.smarttransferserver.entity.FileChunk;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.mapper.FileChunkMapper;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.TransferTaskMapper;
import com.server.smarttransferserver.service.FileChecksumService;
import com.server.smarttransferserver.service.FileStorageService;
import com.server.smarttransferserver.vo.FileMergeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件合并服务实现
 */
@Slf4j
@Service
public class FileMergeServiceImpl {
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private FileChunkMapper fileChunkMapper;
    
    @Autowired
    private TransferTaskMapper transferTaskMapper;
    
    @Autowired
    private FileStorageService storageService;
    
    @Autowired
    private FileChecksumService checksumService;
    
    /**
     * 合并文件
     *
     * @param dto 合并请求DTO
     * @return 合并结果
     */
    @Transactional
    public FileMergeVO mergeFile(FileMergeDTO dto) {
        log.info("开始合并文件 - 文件ID: {}, 哈希: {}", dto.getFileId(), dto.getFileHash());
        
        try {
            // 1. 获取文件信息
            FileInfo fileInfo = fileInfoMapper.selectById(dto.getFileId());
            if (fileInfo == null) {
                return FileMergeVO.builder()
                        .fileId(dto.getFileId())
                        .success(false)
                        .message("文件不存在")
                        .build();
            }
            
            // 2. 检查所有分片是否上传完成
            QueryWrapper<FileChunk> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id", dto.getFileId());
            List<FileChunk> allChunks = fileChunkMapper.selectList(queryWrapper);
            
            long completedChunks = allChunks.stream()
                    .filter(c -> "COMPLETED".equals(c.getUploadStatus()))
                    .count();
            
            if (completedChunks < allChunks.size()) {
                log.warn("分片未全部上传 - 文件ID: {}, 已完成: {}/{}", 
                         dto.getFileId(), completedChunks, allChunks.size());
                return FileMergeVO.builder()
                        .fileId(dto.getFileId())
                        .success(false)
                        .message("分片未全部上传，已完成 " + completedChunks + "/" + allChunks.size())
                        .build();
            }
            
            // 3. 合并分片文件
            String filePath = storageService.mergeChunks(
                    dto.getFileId(), 
                    fileInfo.getFileName(), 
                    allChunks.size());
            
            // 4. 校验文件完整性
            boolean verified = checksumService.verifyHash(filePath, dto.getFileHash(), "MD5");
            if (!verified) {
                log.error("文件校验失败 - 文件ID: {}", dto.getFileId());
                storageService.deleteFile(filePath);
                return FileMergeVO.builder()
                        .fileId(dto.getFileId())
                        .success(false)
                        .verified(false)
                        .message("文件校验失败，请重新上传")
                        .build();
            }
            
            // 5. 更新文件记录
            fileInfo.setFilePath(filePath);
            fileInfo.setUploadStatus("COMPLETED");
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfoMapper.updateById(fileInfo);
            
            // 6. 删除临时分片
            storageService.deleteTempChunks(dto.getFileId());
            
            // 7. 创建传输任务记录
            String taskId = UUID.randomUUID().toString();
            TransferTask task = TransferTask.builder()
                    .taskId(taskId)
                    .fileId(dto.getFileId())
                    .taskType("UPLOAD")
                    .transferStatus("COMPLETED")
                    .progress(new java.math.BigDecimal("100.0"))
                    .startTime(fileInfo.getCreateTime())
                    .endTime(LocalDateTime.now())
                    .build();
            transferTaskMapper.insert(task);
            
            log.info("文件合并成功 - 文件ID: {}, 文件名: {}, 任务ID: {}", 
                     dto.getFileId(), fileInfo.getFileName(), taskId);
            
            return FileMergeVO.builder()
                    .fileId(dto.getFileId())
                    .fileName(fileInfo.getFileName())
                    .filePath(filePath)
                    .fileSize(fileInfo.getFileSize())
                    .success(true)
                    .verified(true)
                    .taskId(taskId)
                    .message("文件上传成功")
                    .build();
            
        } catch (Exception e) {
            log.error("文件合并失败 - 文件ID: {}, 错误: {}", dto.getFileId(), e.getMessage(), e);
            return FileMergeVO.builder()
                    .fileId(dto.getFileId())
                    .success(false)
                    .message("文件合并失败: " + e.getMessage())
                    .build();
        }
    }
}

