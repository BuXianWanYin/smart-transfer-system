package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.TransferTaskMapper;
import com.server.smarttransferserver.service.CongestionAlgorithmManager;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.service.FileDownloadService;
import com.server.smarttransferserver.service.IFileStorageService;
import com.server.smarttransferserver.service.RedisService;
import com.server.smarttransferserver.vo.FileDownloadInitVO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 文件下载服务实现
 * 支持分块下载和拥塞控制
 */
@Slf4j
@Service
public class FileDownloadServiceImpl implements FileDownloadService {
    
    private static final long DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024L; // 5MB默认分块大小
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private TransferTaskMapper transferTaskMapper;
    
    @Autowired
    private IFileStorageService storageService;
    
    @Autowired
    private CongestionAlgorithmManager algorithmManager;
    
    @Autowired
    private CongestionMetricsService metricsService;
    
    @Autowired
    private RedisService redisService;
    
    /**
     * Redis key前缀：存储已完成下载的分块集合
     */
    private static final String DOWNLOAD_COMPLETED_CHUNKS_KEY_PREFIX = "download:completed:chunks:";
    
    /**
     * Redis key前缀：存储下载任务的分块大小
     */
    private static final String DOWNLOAD_CHUNK_SIZE_KEY_PREFIX = "download:chunkSize:";
    
    /**
     * Redis key前缀：存储下载任务的初始文件大小
     */
    private static final String DOWNLOAD_FILE_SIZE_KEY_PREFIX = "download:fileSize:";
    
    /**
     * Redis key过期时间：24小时（下载任务通常在24小时内完成）
     */
    private static final long DOWNLOAD_CHUNKS_CACHE_EXPIRE_HOURS = 24;
    
    /**
     * 记录每个分块下载的开始时间，用于计算RTT
     */
    private final ConcurrentHashMap<String, Long> chunkStartTimes = new ConcurrentHashMap<>();
    
    @Override
    @Transactional
    public FileDownloadInitVO initDownload(Long fileId, Long chunkSize) {
        log.info("初始化文件下载 - 文件ID: {}, 分块大小: {}字节", fileId, chunkSize);
        
        // 1. 检查文件是否存在
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        
        if ("DELETED".equals(fileInfo.getUploadStatus())) {
            throw new RuntimeException("文件已被删除");
        }
        
        // 2. 计算分块信息
        long actualChunkSize = chunkSize != null && chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
        long fileSize = fileInfo.getFileSize();
        int totalChunks = (int) Math.ceil((double) fileSize / actualChunkSize);
        
        // 3. 获取或创建传输任务ID
        String taskId = getOrCreateTaskId(fileId, "DOWNLOAD");
        
        log.info("初始化下载任务 - 文件ID: {}, 任务ID: {}, 总分块数: {}, 文件大小: {}字节",
                 fileId, taskId, totalChunks, fileSize);
        
        // **修复C1: 将chunkSize和文件大小存储到Redis（用于downloadChunk验证一致性）**
        try {
            String chunkSizeKey = DOWNLOAD_CHUNK_SIZE_KEY_PREFIX + taskId;
            String fileSizeKey = DOWNLOAD_FILE_SIZE_KEY_PREFIX + taskId;
            redisService.set(chunkSizeKey, String.valueOf(actualChunkSize));
            redisService.set(fileSizeKey, String.valueOf(fileSize));
            redisService.expire(chunkSizeKey, DOWNLOAD_CHUNKS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            redisService.expire(fileSizeKey, DOWNLOAD_CHUNKS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("存储下载任务配置到Redis - 任务ID: {}, chunkSize: {}字节, fileSize: {}字节", taskId, actualChunkSize, fileSize);
        } catch (Exception e) {
            log.warn("存储下载任务配置到Redis失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
            // 即使Redis失败也不影响下载初始化
        }
        
        // 4. **修复P0-2：从Redis获取已下载的分块列表（实现断点续传）**
        List<Integer> downloaded = new ArrayList<>();
        String completedChunksKey = DOWNLOAD_COMPLETED_CHUNKS_KEY_PREFIX + taskId;
        try {
            Set<Object> completedChunksObj = redisService.sMembers(completedChunksKey);
            if (completedChunksObj != null && !completedChunksObj.isEmpty()) {
                for (Object chunkObj : completedChunksObj) {
                    try {
                        Integer chunkNum = Integer.parseInt(chunkObj.toString());
                        downloaded.add(chunkNum);
                    } catch (NumberFormatException e) {
                        log.warn("无效的分块编号格式 - taskId: {}, chunk: {}", taskId, chunkObj);
                    }
                }
                log.info("从Redis获取已下载分块 - 任务ID: {}, 已下载分块数: {}", taskId, downloaded.size());
            }
        } catch (Exception e) {
            log.warn("从Redis获取已下载分块失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
            // 如果Redis获取失败，返回空列表（重新下载）
        }
        
        return FileDownloadInitVO.builder()
                .fileId(fileId)
                .fileName(fileInfo.getFileName())
                .fileSize(fileSize)
                .totalChunks(totalChunks)
                .chunkSize(actualChunkSize)
                .downloaded(downloaded)
                .taskId(taskId)
                .message("开始下载")
                .build();
    }
    
    @Override
    @Transactional
    public ResponseEntity<byte[]> downloadChunk(Long fileId, Integer chunkNumber, Long startByte, Long endByte) {
        log.info("下载分块 - 文件ID: {}, 分块: {}, 范围: {}-{}", fileId, chunkNumber, startByte, endByte);
        
        // 获取任务ID
        String taskId = getTaskIdByFileId(fileId, "DOWNLOAD");
        if (taskId == null) {
            log.error("无法获取任务ID，分块下载失败 - 文件ID: {}", fileId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("无法获取任务ID".getBytes());
        }
        
        // 获取任务对应的算法实例
        CongestionControlAlgorithm algorithm = algorithmManager.getOrCreateAlgorithm(taskId);
        // **修复P0-3：提前检查algorithm是否为null，避免后续NPE**
        // 注意：getOrCreateAlgorithm通常不会返回null，但防御性编程更安全
        if (algorithm == null) {
            log.error("无法获取拥塞控制算法实例，分块下载失败 - 任务ID: {}", taskId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("无法获取拥塞控制算法实例".getBytes());
        }
        
        // 记录分块下载开始时间（用于计算RTT）
        String chunkKey = fileId + "-" + chunkNumber;
        long startTime = System.currentTimeMillis();
        
        // **修复P1-2：在finally块中确保清理chunkStartTimes**
        try {
            chunkStartTimes.put(chunkKey, startTime);
            
            // **修复m2: 验证分块编号是否在有效范围内**
            // 1. 获取文件信息
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                throw new RuntimeException("文件不存在");
            }
            
            // 2. **修复C1: 从Redis获取chunkSize（与initDownload保持一致）**
            // **修复P1: 增强Redis返回值格式验证，防止NumberFormatException**
            long chunkSize = DEFAULT_CHUNK_SIZE;
            try {
                String chunkSizeKey = DOWNLOAD_CHUNK_SIZE_KEY_PREFIX + taskId;
                Object chunkSizeObj = redisService.get(chunkSizeKey);
                if (chunkSizeObj != null) {
                    String chunkSizeStr = chunkSizeObj.toString().trim();
                    if (!chunkSizeStr.isEmpty()) {
                        try {
                            chunkSize = Long.parseLong(chunkSizeStr);
                            if (chunkSize <= 0) {
                                log.warn("Redis中的chunkSize无效 - 任务ID: {}, 值: {}, 使用默认值", taskId, chunkSizeStr);
                                chunkSize = DEFAULT_CHUNK_SIZE;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Redis中的chunkSize格式错误 - 任务ID: {}, 值: {}, 使用默认值, 错误: {}", taskId, chunkSizeStr, e.getMessage());
                            chunkSize = DEFAULT_CHUNK_SIZE;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("从Redis获取chunkSize失败 - 任务ID: {}, 使用默认值: {}字节, 错误: {}", taskId, DEFAULT_CHUNK_SIZE, e.getMessage());
            }
            
            // 3. **修复C2: 验证文件大小是否与initDownload时一致**
            Path filePath = storageService.getAbsoluteFilePath(fileInfo.getFilePath());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("文件不存在或已被删除");
            }
            
            long currentFileSize = Files.size(filePath);
            long initialFileSize = fileInfo.getFileSize();
            
            // 从Redis获取初始文件大小（更准确）
            // **修复P1: 增强Redis返回值格式验证，防止NumberFormatException**
            try {
                String fileSizeKey = DOWNLOAD_FILE_SIZE_KEY_PREFIX + taskId;
                Object fileSizeObj = redisService.get(fileSizeKey);
                if (fileSizeObj != null) {
                    String fileSizeStr = fileSizeObj.toString().trim();
                    if (!fileSizeStr.isEmpty()) {
                        try {
                            long redisFileSize = Long.parseLong(fileSizeStr);
                            if (redisFileSize > 0) {
                                initialFileSize = redisFileSize;
                            } else {
                                log.warn("Redis中的文件大小无效 - 任务ID: {}, 值: {}, 使用数据库值", taskId, fileSizeStr);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Redis中的文件大小格式错误 - 任务ID: {}, 值: {}, 使用数据库值, 错误: {}", taskId, fileSizeStr, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("从Redis获取初始文件大小失败 - 任务ID: {}, 使用数据库值, 错误: {}", taskId, e.getMessage());
            }
            
            // 验证文件大小是否变化（允许小范围差异，可能是由于文件系统或时间戳问题）
            if (Math.abs(currentFileSize - initialFileSize) > 1024) { // 允许1KB的差异
                log.error("文件大小已变化 - 任务ID: {}, 初始大小: {}字节, 当前大小: {}字节, 差异: {}字节", 
                         taskId, initialFileSize, currentFileSize, Math.abs(currentFileSize - initialFileSize));
                throw new RuntimeException("文件大小已变化，请重新初始化下载");
            }
            
            // **修复m2: 验证分块编号是否在有效范围内**
            // **修复C1: 计算totalChunks（用于验证和进度计算）**
            int totalChunks = (int) Math.ceil((double) initialFileSize / chunkSize);
            if (chunkNumber < 0 || chunkNumber >= totalChunks) {
                log.error("分块编号无效 - 任务ID: {}, 分块编号: {}, 有效范围: [0, {}]", taskId, chunkNumber, totalChunks - 1);
                throw new RuntimeException("分块编号无效: " + chunkNumber + ", 有效范围: [0, " + (totalChunks - 1) + "]");
            }
            
            // 4. 读取文件分块（使用一致的chunkSize）
            long actualStart = Math.max(0, startByte != null ? startByte : chunkNumber * chunkSize);
            long actualEnd = Math.min(currentFileSize - 1, endByte != null ? endByte : Math.min(actualStart + chunkSize - 1, currentFileSize - 1));
            long actualChunkSize = actualEnd - actualStart + 1;
            
            // 读取文件分块
            // **修复P4: 验证文件读取完整性，确保读取的字节数与预期一致**
            byte[] chunkData;
            try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath.toFile())) {
                // 跳过到起始位置（验证实际跳过的字节数）
                long skipped = fis.skip(actualStart);
                if (skipped != actualStart) {
                    log.warn("文件跳过字节数不完整 - 任务ID: {}, 预期: {}字节, 实际: {}字节", taskId, actualStart, skipped);
                    // 如果跳过不完整，尝试继续跳过
                    long remainingSkip = actualStart - skipped;
                    while (remainingSkip > 0) {
                        long skippedMore = fis.skip(remainingSkip);
                        if (skippedMore <= 0) {
                            throw new RuntimeException("无法跳过到文件起始位置: " + actualStart);
                        }
                        remainingSkip -= skippedMore;
                    }
                }
                
                // 使用缓冲区读取，避免一次性分配大内存
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream((int) actualChunkSize);
                byte[] buffer = new byte[8192]; // 8KB缓冲区
                long remaining = actualChunkSize;
                int read;
                while (remaining > 0 && (read = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    baos.write(buffer, 0, read);
                    remaining -= read;
                }
                
                // **修复P4: 验证读取的字节数是否与预期一致**
                if (remaining > 0) {
                    log.error("文件读取不完整 - 任务ID: {}, 分块: {}, 预期: {}字节, 实际: {}字节, 缺失: {}字节", 
                             taskId, chunkNumber, actualChunkSize, (actualChunkSize - remaining), remaining);
                    throw new RuntimeException("文件读取不完整 - 预期: " + actualChunkSize + "字节, 实际: " + (actualChunkSize - remaining) + "字节, 缺失: " + remaining + "字节");
                }
                
                chunkData = baos.toByteArray();
                
                // 双重验证：确保数组长度与预期一致
                if (chunkData.length != actualChunkSize) {
                    log.error("分块数据大小不匹配 - 任务ID: {}, 分块: {}, 预期: {}字节, 实际: {}字节", 
                             taskId, chunkNumber, actualChunkSize, chunkData.length);
                    throw new RuntimeException("分块数据大小不匹配 - 预期: " + actualChunkSize + "字节, 实际: " + chunkData.length + "字节");
                }
            }
            
            // 3. 计算本次传输的RTT
            long endTime = System.currentTimeMillis();
            long rtt = endTime - startTime;
            
            // 4. **关键：触发拥塞控制算法的ACK响应**
            // 注意：algorithm已经在方法开始时检查过，这里可以直接使用
            algorithm.onAck(actualChunkSize, rtt);
            log.debug("拥塞控制响应ACK - 任务ID: {}, 算法: {}, 分块大小: {}字节, RTT: {}ms, 当前cwnd: {}字节",
                     taskId, algorithm.getAlgorithmName(), actualChunkSize, rtt, algorithm.getCwnd());
            
            // 5. 记录拥塞指标到数据库
            if (metricsService instanceof CongestionMetricsServiceImpl) {
                ((CongestionMetricsServiceImpl) metricsService).recordMetrics(taskId, algorithm);
            }
            
            // 6. **修复P0-1：使用Redis记录真实的已完成分块数（修复进度计算）**
            String completedChunksKey = DOWNLOAD_COMPLETED_CHUNKS_KEY_PREFIX + taskId;
            // **修复C1: 使用一致的chunkSize计算totalChunks（使用之前计算的totalChunks变量）**
            int completedChunks;
            double progress;
            
            try {
                // 将当前分块添加到Redis集合（原子操作，如果已存在不会重复添加）
                redisService.sAdd(completedChunksKey, chunkNumber.toString());
                // 设置过期时间（24小时）
                redisService.expire(completedChunksKey, DOWNLOAD_CHUNKS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                
                // 从Redis获取真实的已完成分块数
                Long completedChunksCount = redisService.sSize(completedChunksKey);
                completedChunks = completedChunksCount != null ? completedChunksCount.intValue() : 1;
                
                // 计算真实的进度（使用之前计算的totalChunks变量）
                progress = totalChunks > 0 ? (double) completedChunks / totalChunks * 100 : 0;
                
                log.debug("下载进度更新 - 任务ID: {}, 已完成: {}/{}, 进度: {}%", 
                         taskId, completedChunks, totalChunks, String.format("%.2f", progress));
            } catch (Exception e) {
                log.warn("记录已完成分块到Redis失败 - 任务ID: {}, 分块: {}, 错误: {}", taskId, chunkNumber, e.getMessage());
                // **修复M2: Redis失败降级处理 - 使用保守估计（由于并发下载，不能简单使用chunkNumber + 1）**
                // 实际进度可能略低于真实值，但不会导致功能错误
                completedChunks = Math.max(1, chunkNumber + 1); // 至少为1（当前分块）
                progress = totalChunks > 0 ? (double) completedChunks / totalChunks * 100 : 0;
                log.warn("Redis失败，使用降级进度计算 - 任务ID: {}, 进度: {}%", taskId, String.format("%.2f", progress));
            }
            
            // 7. 获取当前拥塞窗口大小
            long currentCwnd = algorithm.getCwnd();
            
            // 8. **优化：直接返回二进制数据，元数据通过响应头传输（标准做法）**
            // 不再使用Base64编码，减少33%的数据传输量
            log.info("分块下载成功 - 文件ID: {}, 分块: {}, 进度: {}%, RTT: {}ms, cwnd: {}字节",
                     fileId, chunkNumber, String.format("%.2f", progress), rtt, currentCwnd);
            
            // 构建响应头，包含元数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(chunkData.length);
            // 元数据通过自定义响应头传输
            headers.set("X-File-Id", String.valueOf(fileId));
            headers.set("X-Chunk-Number", String.valueOf(chunkNumber));
            headers.set("X-Success", "true");
            headers.set("X-Completed-Chunks", String.valueOf(completedChunks));
            headers.set("X-Total-Chunks", String.valueOf(totalChunks));
            headers.set("X-Progress", String.valueOf(progress));
            headers.set("X-Cwnd", String.valueOf(currentCwnd));
            headers.set("X-Rtt", String.valueOf(rtt));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(chunkData);  // 直接返回二进制数据
                    
        } catch (IOException | RuntimeException e) {
            log.error("分块下载失败 - 文件ID: {}, 分块: {}, 错误: {}", fileId, chunkNumber, e.getMessage());
            
            // **修复P0-3：下载失败视为丢包，触发拥塞控制算法的丢包响应**
            // 注意：algorithm已经在方法开始时检查过，这里可以直接使用
            long failedChunkSize = DEFAULT_CHUNK_SIZE; // 使用默认分块大小
            
            algorithm.onLoss(failedChunkSize);
            long currentCwnd = algorithm.getCwnd();
            log.warn("拥塞控制响应丢包 - 任务ID: {}, 算法: {}, 分块大小: {}字节, 当前cwnd: {}字节",
                    taskId, algorithm.getAlgorithmName(), failedChunkSize, currentCwnd);
            
            // 记录拥塞指标到数据库
            if (metricsService instanceof CongestionMetricsServiceImpl) {
                ((CongestionMetricsServiceImpl) metricsService).recordMetrics(taskId, algorithm);
            }
            
            // 返回错误响应（二进制格式）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("X-File-Id", String.valueOf(fileId));
            headers.set("X-Chunk-Number", String.valueOf(chunkNumber));
            headers.set("X-Success", "false");
            headers.set("X-Cwnd", String.valueOf(currentCwnd));
            headers.set("X-Error-Message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .headers(headers)
                    .body(("分块下载失败: " + e.getMessage()).getBytes());
        } finally {
            // **修复P1-2：在finally块中确保清理chunkStartTimes（防止内存泄漏）**
            chunkStartTimes.remove(chunkKey);
        }
    }
    
    /**
     * 获取或创建传输任务ID
     */
    private String getOrCreateTaskId(Long fileId, String taskType) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TransferTask> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("file_id", fileId)
                    .eq("task_type", taskType)
                    .in("transfer_status", "PENDING", "PROCESSING", "PAUSED")
                    .orderByDesc("start_time")
                    .last("LIMIT 1");
        TransferTask existingTask = transferTaskMapper.selectOne(queryWrapper);
        
        if (existingTask != null) {
            return existingTask.getTaskId();
        } else {
            // 创建新任务
            String taskId = UUID.randomUUID().toString();
            TransferTask newTask = TransferTask.builder()
                    .taskId(taskId)
                    .fileId(fileId)
                    .taskType(taskType)
                    .transferStatus("PENDING")
                    .progress(java.math.BigDecimal.ZERO)
                    .startTime(java.time.LocalDateTime.now())
                    .build();
            transferTaskMapper.insert(newTask);
            log.info("为文件创建新下载任务 - 文件ID: {}, 任务ID: {}", fileId, taskId);
            return taskId;
        }
    }
    
    /**
     * 根据文件ID获取任务ID
     */
    private String getTaskIdByFileId(Long fileId, String taskType) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TransferTask> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("file_id", fileId)
                    .eq("task_type", taskType)
                    .in("transfer_status", "PENDING", "PROCESSING", "PAUSED")
                    .orderByDesc("start_time")
                    .last("LIMIT 1");
        TransferTask task = transferTaskMapper.selectOne(queryWrapper);
        return task != null ? task.getTaskId() : null;
    }
}
