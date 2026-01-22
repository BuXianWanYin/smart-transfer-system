package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.server.smarttransferserver.dto.FileMergeDTO;
import com.server.smarttransferserver.entity.FileChunk;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.mapper.FileChunkMapper;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.TransferTaskMapper;
import com.server.smarttransferserver.service.CongestionAlgorithmManager;
import com.server.smarttransferserver.service.IFileChecksumService;
import com.server.smarttransferserver.service.FileMergeService;
import com.server.smarttransferserver.service.IFileStorageService;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.vo.FileMergeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件合并服务实现
 */
@Slf4j
@Service
public class FileMergeServiceImpl implements FileMergeService {
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private FileChunkMapper fileChunkMapper;
    
    @Autowired
    private TransferTaskMapper transferTaskMapper;
    
    @Autowired
    private TransferTaskService transferTaskService;
    
    @Autowired
    private IFileStorageService storageService;
    
    @Autowired
    private IFileChecksumService checksumService;
    
    @Autowired
    private CongestionAlgorithmManager algorithmManager;
    
    /**
     * 合并文件
     *
     * @param dto 合并请求DTO
     * @return 合并结果
     */
    @Override
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
            
            // 1.5. 如果文件已经上传完成（秒传情况），直接返回成功
            if ("COMPLETED".equals(fileInfo.getUploadStatus())) {
                log.info("文件已上传完成，跳过合并 - 文件ID: {}, 文件名: {}", dto.getFileId(), fileInfo.getFileName());
                // 查询是否已有传输任务记录
                QueryWrapper<TransferTask> taskWrapper = new QueryWrapper<>();
                taskWrapper.eq("file_id", dto.getFileId())
                           .eq("task_type", "UPLOAD")
                           .eq("transfer_status", "COMPLETED")
                           .orderByDesc("end_time")
                           .last("LIMIT 1");
                TransferTask existingTask = transferTaskMapper.selectList(taskWrapper).stream().findFirst().orElse(null);
                
                String taskId = existingTask != null ? existingTask.getTaskId() : UUID.randomUUID().toString();
                
                return FileMergeVO.builder()
                        .fileId(dto.getFileId())
                        .fileName(fileInfo.getFileName())
                        .filePath(fileInfo.getFilePath())
                        .fileSize(fileInfo.getFileSize())
                        .success(true)
                        .verified(true)
                        .taskId(taskId)
                        .message("文件已存在，秒传成功")
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
            
            // 4. 校验文件完整性（需要绝对路径）
            // mergeChunks返回的是相对路径，需要转换为绝对路径才能用于文件操作
            java.nio.file.Path absolutePath = storageService.getAbsoluteFilePath(filePath);
            String absolutePathStr = absolutePath.toString();
            boolean verified = checksumService.verifyHash(absolutePathStr, dto.getFileHash(), "MD5");
            if (!verified) {
                log.error("文件校验失败 - 文件ID: {}", dto.getFileId());
                // 清理所有相关数据
                cleanupFailedUpload(dto.getFileId(), filePath);
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
            
            // 7. 查找并更新已有的活跃传输任务，如果没有则创建新任务
            String taskId = null;
            List<TransferTask> existingTasks = transferTaskMapper.selectByFileId(dto.getFileId());
            if (existingTasks != null && !existingTasks.isEmpty()) {
                // 查找活跃的任务（PENDING或PROCESSING状态）
                TransferTask activeTask = existingTasks.stream()
                        .filter(t -> "PENDING".equals(t.getTransferStatus()) || "PROCESSING".equals(t.getTransferStatus()))
                        .findFirst()
                        .orElse(null);
                
                if (activeTask != null) {
                    taskId = activeTask.getTaskId();
                    // 更新任务状态为COMPLETED（会自动从活跃用户集合移除）
                    transferTaskService.updateTaskStatus(taskId, "COMPLETED");
                    
                    // **关键修复：清理任务的算法实例**
                    if (algorithmManager != null) {
                        algorithmManager.removeAlgorithm(taskId);
                        log.debug("清理任务算法实例 - 任务ID: {}", taskId);
                    }
                }
            }
            
            // 如果没有找到活跃任务，创建新任务（兼容处理）
            if (taskId == null) {
                taskId = transferTaskService.createTask(dto.getFileId(), "UPLOAD");
                transferTaskService.updateTaskStatus(taskId, "COMPLETED");
            }
            
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
            // 清理所有相关数据
            cleanupFailedUpload(dto.getFileId(), null);
            return FileMergeVO.builder()
                    .fileId(dto.getFileId())
                    .success(false)
                    .message("文件合并失败: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 取消上传
     * 清理未完成的上传数据
     * **修复MODULE-5: 取消上传时清理算法实例**
     *
     * @param fileId 文件ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean cancelUpload(Long fileId) {
        log.info("取消上传 - 文件ID: {}", fileId);
        try {
            // **修复MODULE-5: 查找并清理关联的算法实例**
            QueryWrapper<TransferTask> taskWrapper = new QueryWrapper<>();
            taskWrapper.eq("file_id", fileId)
                       .eq("task_type", "UPLOAD")
                       .in("transfer_status", Arrays.asList("PENDING", "PROCESSING"));
            List<TransferTask> activeTasks = transferTaskMapper.selectList(taskWrapper);
            
            for (TransferTask task : activeTasks) {
                if (algorithmManager != null && task.getTaskId() != null) {
                    algorithmManager.removeAlgorithm(task.getTaskId());
                    log.debug("取消上传时清理算法实例 - 任务ID: {}", task.getTaskId());
                }
            }
            
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                log.warn("文件不存在 - ID: {}", fileId);
                return true; // 文件不存在也视为成功
            }
            
            // 只能取消未完成的上传
            if ("COMPLETED".equals(fileInfo.getUploadStatus())) {
                log.warn("文件已上传完成，无法取消 - ID: {}", fileId);
                return false;
            }
            
            cleanupFailedUpload(fileId, null);
            return true;
        } catch (Exception e) {
            log.error("取消上传失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 清理上传失败的文件及相关数据
     *
     * @param fileId 文件ID
     * @param filePath 合并后的文件路径（可为null）
     */
    private void cleanupFailedUpload(Long fileId, String filePath) {
        try {
            // 1. 删除合并后的文件（如果存在）
            if (filePath != null) {
                storageService.deleteFile(filePath);
            }
            
            // 2. 删除临时分片文件
            storageService.deleteTempChunks(fileId);
            
            // 3. 删除分片记录
            QueryWrapper<FileChunk> chunkWrapper = new QueryWrapper<>();
            chunkWrapper.eq("file_id", fileId);
            fileChunkMapper.delete(chunkWrapper);
            
            // 4. 删除关联的传输任务记录
            transferTaskMapper.deleteByFileId(fileId);
            
            // 5. 物理删除文件记录（因为文件从未成功上传）
            fileInfoMapper.deletePhysically(fileId);
            
            log.info("清理上传失败的文件数据完成 - 文件ID: {}", fileId);
        } catch (Exception e) {
            log.error("清理上传失败的文件数据时出错 - 文件ID: {}, 错误: {}", fileId, e.getMessage());
        }
    }
}

