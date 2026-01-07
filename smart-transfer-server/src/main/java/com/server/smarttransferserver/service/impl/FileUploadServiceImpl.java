package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.server.smarttransferserver.congestion.BandwidthEstimator;
import com.server.smarttransferserver.dto.ChunkUploadDTO;
import com.server.smarttransferserver.dto.FileUploadInitDTO;
import com.server.smarttransferserver.entity.FileChunk;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.mapper.FileChunkMapper;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.service.FileUploadCacheService;
import com.server.smarttransferserver.service.IFileChecksumService;
import com.server.smarttransferserver.service.IFileStorageService;
import com.server.smarttransferserver.service.FileUploadService;
import com.server.smarttransferserver.vo.ChunkUploadVO;
import com.server.smarttransferserver.vo.FileUploadInitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.server.smarttransferserver.util.UserContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件上传服务实现
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private FileChunkMapper fileChunkMapper;
    
    @Autowired
    private IFileStorageService storageService;
    
    @Autowired
    private IFileChecksumService checksumService;
    
    @Autowired
    private FileUploadCacheService uploadCacheService;

    @Autowired
    private BandwidthEstimator bandwidthEstimator;
    
    /**
     * 初始化文件上传
     * 检查是否秒传或断点续传
     *
     * @param dto 上传初始化DTO
     * @return 初始化结果
     */
    @Override
    @Transactional
    public FileUploadInitVO initUpload(FileUploadInitDTO dto) {
        log.info("初始化文件上传 - 文件名: {}, 大小: {}字节, 哈希: {}", 
                 dto.getFileName(), dto.getFileSize(), dto.getFileHash());
        
        // 1. 检查文件是否已存在（秒传）
        FileInfo existingFile = fileInfoMapper.selectByFileHash(dto.getFileHash());
        if (existingFile != null && "COMPLETED".equals(existingFile.getUploadStatus())) {
            // 秒传
            log.info("文件已存在，秒传 - 文件ID: {}", existingFile.getId());
            return FileUploadInitVO.builder()
                    .fileId(existingFile.getId())
                    .skipUpload(true)
                    .quickUpload(true)
                    .filePath(existingFile.getFilePath())
                    .message("文件已存在，秒传成功")
                    .build();
        }
        
        // 3. 检查是否有未完成的上传（断点续传）
        if (existingFile != null && "UPLOADING".equals(existingFile.getUploadStatus())) {
            // 优先从 Redis 获取已上传分片
            Set<Integer> cachedChunks = uploadCacheService.getUploadedChunks(existingFile.getId());
            List<Integer> chunkNumbers;
            
            if (!cachedChunks.isEmpty()) {
                // Redis 有缓存
                chunkNumbers = new ArrayList<>(cachedChunks);
                log.info("从Redis获取已上传分片 - 文件ID: {}, 数量: {}", existingFile.getId(), chunkNumbers.size());
            } else {
                // Redis 无缓存，从数据库查询
                List<FileChunk> uploadedChunks = fileChunkMapper.selectByFileIdAndUploadStatus(
                        existingFile.getId(), "COMPLETED");
                chunkNumbers = uploadedChunks.stream()
                        .map(FileChunk::getChunkNumber)
                        .collect(Collectors.toList());
                
                // 同步到 Redis
                chunkNumbers.forEach(num -> uploadCacheService.markChunkUploaded(existingFile.getId(), num));
            }
            
            log.info("断点续传 - 文件ID: {}, 已上传分片: {}", existingFile.getId(), chunkNumbers.size());
            return FileUploadInitVO.builder()
                    .fileId(existingFile.getId())
                    .skipUpload(false)
                    .quickUpload(false)
                    .uploaded(chunkNumbers)
                    .message("继续上传，已完成 " + chunkNumbers.size() + " 个分片")
                    .build();
        }
        
        // 4. 创建新的文件记录
        // 临时文件路径（上传完成后会更新为实际路径）
        String tempFilePath = "uploading/" + dto.getFileHash();
        
        // 提取文件扩展名
        String extendName = extractExtendName(dto.getFileName());
        
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        
        FileInfo fileInfo = FileInfo.builder()
                .userId(userId)  // 设置用户ID
                .fileName(dto.getFileName())
                .extendName(extendName)  // 设置文件扩展名
                .fileSize(dto.getFileSize())
                .fileHash(dto.getFileHash())
                .filePath(tempFilePath)  // 设置临时文件路径，合并完成后会更新
                .isDir(0)  // 上传的是文件，不是目录
                .folderId(dto.getFolderId() != null ? dto.getFolderId() : 0L)  // 默认放在根目录
                .uploadStatus("UPLOADING")
                .delFlag(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        
        fileInfoMapper.insert(fileInfo);
        
        log.info("创建文件记录 - 文件ID: {}, 临时路径: {}", fileInfo.getId(), tempFilePath);
        
        // 5. 创建分片记录
        for (int i = 0; i < dto.getTotalChunks(); i++) {
            FileChunk chunk = FileChunk.builder()
                    .fileId(fileInfo.getId())
                    .chunkNumber(i)
                    .chunkSize(calculateChunkSize(dto.getFileSize(), dto.getChunkSize(), i, dto.getTotalChunks()))
                    .uploadStatus("PENDING")
                    .createTime(LocalDateTime.now())
                    .build();
            fileChunkMapper.insert(chunk);
        }
        
        log.info("创建新上传任务 - 文件ID: {}, 总分片数: {}", fileInfo.getId(), dto.getTotalChunks());
        return FileUploadInitVO.builder()
                .fileId(fileInfo.getId())
                .skipUpload(false)
                .quickUpload(false)
                .uploaded(Collections.emptyList())
                .message("开始上传")
                .build();
    }
    
    /**
     * 检查分片是否已上传（秒传/断点续传）
     * 兼容 vue-simple-uploader
     */
    @Override
    public FileUploadInitVO checkChunk(String identifier, Long totalSize, String filename, Integer totalChunks) {
        log.info("检查分片 - identifier: {}, filename: {}", identifier, filename);
        
        // 1. 根据文件哈希查找文件
        FileInfo existingFile = fileInfoMapper.selectByFileHash(identifier);
        
        // 2. 文件已存在且上传完成 - 秒传
        if (existingFile != null && "COMPLETED".equals(existingFile.getUploadStatus())) {
            log.info("文件已存在，支持秒传 - 文件ID: {}", existingFile.getId());
            return FileUploadInitVO.builder()
                    .fileId(existingFile.getId())
                    .skipUpload(true)
                    .quickUpload(true)
                    .uploaded(null)
                    .filePath(existingFile.getFilePath())
                    .message("文件已存在，秒传成功")
                    .build();
        }
        
        // 3. 文件正在上传中 - 断点续传
        if (existingFile != null && "UPLOADING".equals(existingFile.getUploadStatus())) {
            // 获取已上传的分片
            Set<Integer> cachedChunks = uploadCacheService.getUploadedChunks(existingFile.getId());
            List<Integer> uploadedList;
            
            if (!cachedChunks.isEmpty()) {
                uploadedList = new ArrayList<>(cachedChunks);
            } else {
                List<FileChunk> uploadedChunks = fileChunkMapper.selectByFileIdAndUploadStatus(
                        existingFile.getId(), "COMPLETED");
                uploadedList = uploadedChunks.stream()
                        .map(FileChunk::getChunkNumber)
                        .collect(Collectors.toList());
            }
            
            log.info("断点续传 - 文件ID: {}, 已上传分片数: {}", existingFile.getId(), uploadedList.size());
            return FileUploadInitVO.builder()
                    .fileId(existingFile.getId())
                    .skipUpload(false)
                    .quickUpload(false)
                    .uploaded(uploadedList)
                    .message("继续上传")
                    .build();
        }
        
        // 4. 文件不存在 - 新上传
        log.info("文件不存在，需要新上传 - identifier: {}", identifier);
        return FileUploadInitVO.builder()
                .fileId(null)
                .skipUpload(false)
                .quickUpload(false)
                .uploaded(Collections.emptyList())
                .message("开始上传")
                .build();
    }
    
    /**
     * 上传文件分片
     *
     * @param fileId 文件ID
     * @param chunkNumber 分片编号
     * @param chunkHash 分片哈希
     * @param file 分片文件
     * @return 分片上传结果
     */
    @Override
    @Transactional
    public ChunkUploadVO uploadChunk(Long fileId, Integer chunkNumber, String chunkHash, MultipartFile file) {
        ChunkUploadDTO dto = new ChunkUploadDTO();
        dto.setFileId(fileId);
        dto.setChunkNumber(chunkNumber);
        dto.setChunkHash(chunkHash);
        dto.setFile(file);
        return uploadChunkInternal(dto);
    }
    
    /**
     * 上传分片（内部方法）
     *
     * @param dto 分片上传DTO
     * @return 上传结果
     */
    private ChunkUploadVO uploadChunkInternal(ChunkUploadDTO dto) {
        log.info("上传分片 - 文件ID: {}, 分片: {}", dto.getFileId(), dto.getChunkNumber());
        
        try {
            // 注：分片哈希验证已跳过，文件完整性在合并时通过整体哈希验证
            
            // 1. 保存分片文件
            long chunkSize = dto.getFile().getSize();
            storageService.saveChunk(dto.getFileId(), dto.getChunkNumber(), dto.getFile());
            
            // 记录传输字节数用于带宽估计
            bandwidthEstimator.recordSent(chunkSize);
            
            // 2. 更新分片记录
            FileChunk chunk = fileChunkMapper.selectByFileIdAndChunkNumber(dto.getFileId(), dto.getChunkNumber());
            if (chunk != null) {
                chunk.setChunkHash(dto.getChunkHash());
                chunk.setUploadStatus("COMPLETED");
                fileChunkMapper.updateById(chunk);
            }
            
            // 3. 统计上传进度
            QueryWrapper<FileChunk> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id", dto.getFileId());
            List<FileChunk> allChunks = fileChunkMapper.selectList(queryWrapper);
            int totalChunks = allChunks.size();
            long completedChunks = allChunks.stream()
                    .filter(c -> "COMPLETED".equals(c.getUploadStatus()))
                    .count();
            double progress = (double) completedChunks / totalChunks * 100;
            
            log.info("分片上传成功 - 文件ID: {}, 分片: {}, 进度: {:.2f}%", 
                     dto.getFileId(), dto.getChunkNumber(), progress);
            
            return ChunkUploadVO.builder()
                    .fileId(dto.getFileId())
                    .chunkNumber(dto.getChunkNumber())
                    .success(true)
                    .completedChunks((int) completedChunks)
                    .totalChunks(totalChunks)
                    .progress(progress)
                    .message("分片上传成功")
                    .build();
            
        } catch (IOException e) {
            log.error("分片上传失败 - 文件ID: {}, 分片: {}, 错误: {}", 
                      dto.getFileId(), dto.getChunkNumber(), e.getMessage());
            return ChunkUploadVO.builder()
                    .fileId(dto.getFileId())
                    .chunkNumber(dto.getChunkNumber())
                    .success(false)
                    .message("分片上传失败: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 计算分片大小
     *
     * @param fileSize    文件总大小
     * @param chunkSize   标准分片大小
     * @param chunkNumber 分片序号
     * @param totalChunks 总分片数
     * @return 实际分片大小
     */
    private long calculateChunkSize(long fileSize, long chunkSize, int chunkNumber, int totalChunks) {
        if (chunkNumber < totalChunks - 1) {
            return chunkSize;
        } else {
            // 最后一个分片
            return fileSize - chunkSize * (totalChunks - 1);
        }
    }
    
    /**
     * 从文件名中提取扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（小写），如果没有扩展名返回空字符串
     */
    private String extractExtendName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}

