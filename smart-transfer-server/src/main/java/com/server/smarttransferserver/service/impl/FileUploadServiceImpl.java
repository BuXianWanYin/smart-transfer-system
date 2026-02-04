package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.BandwidthEstimator;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.dto.ChunkUploadDTO;
import com.server.smarttransferserver.dto.FileUploadInitDTO;
import com.server.smarttransferserver.entity.FileChunk;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.mapper.FileChunkMapper;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.TransferTaskMapper;
import com.server.smarttransferserver.service.CongestionAlgorithmManager;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.service.FileUploadCacheService;
import com.server.smarttransferserver.service.ProbeRttStore;
import com.server.smarttransferserver.service.IFileStorageService;
import com.server.smarttransferserver.service.FileUploadService;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.vo.ChunkUploadVO;
import com.server.smarttransferserver.vo.FileUploadInitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.server.smarttransferserver.util.CongestionClientMetricsConstants;
import com.server.smarttransferserver.util.UserContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private FileUploadCacheService uploadCacheService;

    @Autowired
    private BandwidthEstimator bandwidthEstimator;
    
    @Autowired
    private TransferTaskService transferTaskService;
    
    @Autowired
    private TransferTaskMapper transferTaskMapper;
    
    @Autowired
    private CongestionAlgorithmManager algorithmManager;
    
    @Autowired
    private CongestionMetricsService metricsService;
    
    @Autowired
    private ProbeRttStore probeRttStore;
    
    /**
     * 记录每个分片上传的开始时间，用于计算RTT
     */
    private final ConcurrentHashMap<String, Long> chunkStartTimes = new ConcurrentHashMap<>();
    
    /**
     * 每个文件（fileId）对应的总分片数，在 initUpload 创建新任务时写入，用于丢包率 = 总丢包数/总分片数。
     */
    private final ConcurrentHashMap<Long, Integer> fileIdToTotalChunks = new ConcurrentHashMap<>();
    
    /** 分片日志采样间隔：每 N 个分片或首/尾分片才打印进度类日志，减少大量分片时的刷屏 */
    private static final int CHUNK_LOG_SAMPLE_INTERVAL = 5;
    
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
            
            // **修复：获取或创建任务ID（用于监控数据匹配）**
            String taskId = getOrCreateTaskId(existingFile.getId());
            
            log.info("断点续传 - 文件ID: {}, 已上传分片: {}, 任务ID: {}", existingFile.getId(), chunkNumbers.size(), taskId);
            return FileUploadInitVO.builder()
                    .fileId(existingFile.getId())
                    .taskId(taskId)
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
                    .chunkSize(dto.getChunkSize())  // **修复ISSUE-3: 直接使用前端提供的chunkSize，确保前后端一致**
                    .uploadStatus("PENDING")
                    .createTime(LocalDateTime.now())
                    .build();
            fileChunkMapper.insert(chunk);
        }
        
        // **修复：创建传输任务并获取taskId（用于监控数据匹配）**
        String taskId = transferTaskService.createTask(fileInfo.getId(), "UPLOAD");
        // 立即更新为PROCESSING状态
        TransferTask newTask = transferTaskMapper.selectByTaskId(taskId);
        if (newTask != null) {
            newTask.setTransferStatus("PROCESSING");
            transferTaskMapper.updateById(newTask);
        }
        
        fileIdToTotalChunks.put(fileInfo.getId(), dto.getTotalChunks());
        log.info("创建新上传任务 - 文件ID: {}, 总分片数: {}, 任务ID: {}", fileInfo.getId(), dto.getTotalChunks(), taskId);
        return FileUploadInitVO.builder()
                .fileId(fileInfo.getId())
                .taskId(taskId)
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
            
            if (totalChunks != null && totalChunks > 0) {
                fileIdToTotalChunks.put(existingFile.getId(), totalChunks);
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
    public ChunkUploadVO uploadChunk(Long fileId, Integer chunkNumber, String chunkHash, MultipartFile file, Long clientRttMs, Integer clientRetryCount) {
        ChunkUploadDTO dto = new ChunkUploadDTO();
        dto.setFileId(fileId);
        dto.setChunkNumber(chunkNumber);
        dto.setChunkHash(chunkHash);
        dto.setFile(file);
        dto.setClientRttMs(clientRttMs);
        dto.setClientRetryCount(clientRetryCount);
        return uploadChunkInternal(dto);
    }
    
    /**
     * 上传分片（内部方法）
     * 集成拥塞控制算法，在上传成功/失败时触发算法响应
     *
     * @param dto 分片上传DTO
     * @return 上传结果
     */
    private ChunkUploadVO uploadChunkInternal(ChunkUploadDTO dto) {
        // 获取任务ID和拥塞控制算法实例
        String taskId = getOrCreateTaskId(dto.getFileId());
        CongestionControlAlgorithm algorithm = algorithmManager.getOrCreateAlgorithm(taskId);
        handleRetryLossStatistics(dto, algorithm);
        // 记录分片开始时间
        String chunkKey = dto.getFileId() + "-" + dto.getChunkNumber();
        long startTime = System.currentTimeMillis();
        chunkStartTimes.put(chunkKey, startTime);
        try {
            long chunkSize = dto.getFile().getSize();
            storageService.saveChunk(dto.getFileId(), dto.getChunkNumber(), dto.getFile());
            long serverProcessingMs = System.currentTimeMillis() - startTime;
            chunkStartTimes.remove(chunkKey);
            long rtt = calculateRtt(dto, serverProcessingMs);
            Long propagationRttMs = updateCongestionControlOnAck(
                    taskId, dto, algorithm, chunkSize, rtt);
            updateChunkDatabaseRecord(dto, chunkSize);
            updateChunkCache(dto);
            ChunkProgressInfo progressInfo = calculateChunkProgress(dto.getFileId(), algorithm);
            logChunkUploadSuccess(taskId, dto, rtt, algorithm, progressInfo);
            return buildSuccessResponse(dto, rtt, propagationRttMs, algorithm, progressInfo);
        } catch (IOException e) {
            return handleUploadFailure(taskId, dto, algorithm, chunkKey, e);
        }
    }
    
    /**
     * 处理客户端重试导致的丢包统计
     * 将重试次数转换为丢包事件，通知拥塞控制算法
     */
    private void handleRetryLossStatistics(ChunkUploadDTO dto, CongestionControlAlgorithm algorithm) {
        Integer retryCount = dto.getClientRetryCount();
        if (retryCount == null || retryCount <= 0 || algorithm == null) {
            return;
        }
        
        int cappedRetry = Math.min(retryCount, CongestionClientMetricsConstants.RETRY_COUNT_CAP);
        long chunkSize = dto.getFile().getSize();
        
        for (int i = 0; i < cappedRetry; i++) {
            algorithm.onLoss(chunkSize);
        }
        
        if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
            log.debug("应用层丢包统计 - 分片{}重试{}次，计入{}次丢包", 
                     dto.getChunkNumber(), retryCount, cappedRetry);
        }
    }
    
    /**
     * 计算RTT（往返时延）
     * 优先使用客户端测量的RTT，否则使用服务器处理时间
     */
    private long calculateRtt(ChunkUploadDTO dto, long serverProcessingMs) {
        if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
            log.info("RTT原始值 - 分片{}: clientRttMs={}, serverProcessingMs={}ms", 
                    dto.getChunkNumber(),
                    dto.getClientRttMs() != null ? dto.getClientRttMs() + "ms" : "null",
                    serverProcessingMs);
        }
        
        if (dto.getClientRttMs() != null && dto.getClientRttMs() > 0) {
            // 使用客户端RTT，并限制在合理范围内
            return Math.max(CongestionClientMetricsConstants.RTT_MS_MIN,
                    Math.min(CongestionClientMetricsConstants.RTT_MS_MAX, dto.getClientRttMs()));
        }
        return serverProcessingMs;
    }
    
    /**
     * 更新拥塞控制算法状态（收到ACK时）
     * 
     * @return 传播延迟RTT（单向）
     */
    private Long updateCongestionControlOnAck(String taskId, ChunkUploadDTO dto,
            CongestionControlAlgorithm algorithm, long chunkSize, long rtt) {
        bandwidthEstimator.recordSent(chunkSize);
        
        if (algorithm == null) {
            return null;
        }
        
        // 获取传播延迟
        Long propagationRttMs = getPropagationRtt();
        
        // 设置自适应算法的总分片数
        if (algorithm instanceof AdaptiveAlgorithm) {
            int totalChunks = fileIdToTotalChunks.getOrDefault(dto.getFileId(), 0);
            ((AdaptiveAlgorithm) algorithm).setTotalChunks(totalChunks);
        }
        
        // 通知算法收到ACK
        algorithm.onAck(chunkSize, rtt, propagationRttMs);
        
        if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
            log.debug("拥塞控制ACK - 任务{}, 分片{}, 算法{}, RTT{}ms, cwnd{}字节",
                     taskId, dto.getChunkNumber(), algorithm.getAlgorithmName(), 
                     rtt, algorithm.getCwnd());
        }
        
        // 记录指标
        if (metricsService instanceof CongestionMetricsServiceImpl) {
            ((CongestionMetricsServiceImpl) metricsService).recordMetrics(taskId, algorithm);
        }
        
        return propagationRttMs;
    }
    
    /**
     * 获取传播延迟RTT
     */
    private Long getPropagationRtt() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return null;
        }
        Long probeRtt = probeRttStore.get(userId);
        return (probeRtt != null && probeRtt > 0) ? probeRtt : null;
    }
    
    /**
     * 更新分片数据库记录
     */
    private void updateChunkDatabaseRecord(ChunkUploadDTO dto, long chunkSize) {
        FileChunk chunk = fileChunkMapper.selectByFileIdAndChunkNumber(
                dto.getFileId(), dto.getChunkNumber());
        
        if (chunk != null) {
            chunk.setChunkHash(dto.getChunkHash());
            chunk.setUploadStatus("COMPLETED");
            fileChunkMapper.updateById(chunk);
        } else {
            log.warn("分片记录不存在，创建新记录 - 文件ID: {}, 分片: {}", 
                    dto.getFileId(), dto.getChunkNumber());
            chunk = FileChunk.builder()
                    .fileId(dto.getFileId())
                    .chunkNumber(dto.getChunkNumber())
                    .chunkSize(chunkSize)
                    .chunkHash(dto.getChunkHash())
                    .uploadStatus("COMPLETED")
                    .createTime(LocalDateTime.now())
                    .build();
            fileChunkMapper.insert(chunk);
        }
    }
    
    /**
     * 更新Redis分片缓存
     */
    private void updateChunkCache(ChunkUploadDTO dto) {
        try {
            uploadCacheService.markChunkUploaded(dto.getFileId(), dto.getChunkNumber());
            if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
                log.debug("更新Redis缓存 - 文件{}, 分片{}", dto.getFileId(), dto.getChunkNumber());
            }
        } catch (Exception e) {
            log.warn("更新Redis缓存失败 - 文件{}, 分片{}, 错误: {}",
                    dto.getFileId(), dto.getChunkNumber(), e.getMessage());
        }
    }
    
    /**
     * 分片进度信息
     */
    private static class ChunkProgressInfo {
        int totalChunks;
        long completedChunks;
        double progress;
        long cwnd;
    }
    
    /**
     * 计算分片上传进度
     */
    private ChunkProgressInfo calculateChunkProgress(Long fileId, CongestionControlAlgorithm algorithm) {
        QueryWrapper<FileChunk> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id", fileId);
        List<FileChunk> allChunks = fileChunkMapper.selectList(queryWrapper);
        
        ChunkProgressInfo info = new ChunkProgressInfo();
        info.totalChunks = allChunks.size();
        info.completedChunks = allChunks.stream()
                .filter(c -> "COMPLETED".equals(c.getUploadStatus()))
                .count();
        info.progress = info.totalChunks > 0 
                ? (double) info.completedChunks / info.totalChunks * 100 : 0;
        info.cwnd = algorithm != null ? algorithm.getCwnd() : 5 * 1024 * 1024;
        
        // 更新自适应算法的总分片数
        if (algorithm instanceof AdaptiveAlgorithm && info.totalChunks > 0) {
            ((AdaptiveAlgorithm) algorithm).setTotalChunks(info.totalChunks);
        }
        
        return info;
    }
    
    /**
     * 记录分片上传成功日志
     */
    private void logChunkUploadSuccess(String taskId, ChunkUploadDTO dto, long rtt,
            CongestionControlAlgorithm algorithm, ChunkProgressInfo progressInfo) {
        if (shouldLogChunk(dto.getChunkNumber(), progressInfo.totalChunks, (int) progressInfo.completedChunks)) {
            log.info("分片上传成功 - 任务{}, 文件{}, 分片{}, 进度{:.2f}%, RTT{}ms, 算法{}, cwnd{}字节",
                    taskId, dto.getFileId(), dto.getChunkNumber(), progressInfo.progress, rtt,
                    algorithm != null ? algorithm.getAlgorithmName() : "NONE", progressInfo.cwnd);
        }
    }
    
    /**
     * 构建上传成功响应
     */
    private ChunkUploadVO buildSuccessResponse(ChunkUploadDTO dto, long rtt, Long propagationRttMs,
            CongestionControlAlgorithm algorithm, ChunkProgressInfo progressInfo) {
        long rateBps = algorithm != null ? algorithm.getRate() : 0L;
        Long propagationRttOneWay = propagationRttMs != null ? propagationRttMs / 2 : null;
        
        return ChunkUploadVO.builder()
                .fileId(dto.getFileId())
                .chunkNumber(dto.getChunkNumber())
                .success(true)
                .completedChunks((int) progressInfo.completedChunks)
                .totalChunks(progressInfo.totalChunks)
                .progress(progressInfo.progress)
                .cwnd(progressInfo.cwnd)
                .rtt(rtt)
                .rate(rateBps > 0 ? rateBps : null)
                .propagationRtt(propagationRttOneWay)
                .message("分片上传成功")
                .build();
    }
    
    /**
     * 处理分片上传失败
     * 通知拥塞控制算法发生丢包，并返回失败响应
     */
    private ChunkUploadVO handleUploadFailure(String taskId, ChunkUploadDTO dto,
            CongestionControlAlgorithm algorithm, String chunkKey, IOException e) {
        log.error("分片上传失败 - 任务{}, 文件{}, 分片{}, 错误: {}",
                taskId, dto.getFileId(), dto.getChunkNumber(), e.getMessage());
        
        long chunkSize = dto.getFile().getSize();
        chunkStartTimes.remove(chunkKey);
        
        // 通知拥塞控制算法发生丢包
        if (algorithm != null) {
            algorithm.onLoss(chunkSize);
            log.warn("拥塞控制丢包 - 任务{}, 算法{}, 分片大小{}字节, cwnd{}字节",
                    taskId, algorithm.getAlgorithmName(), chunkSize, algorithm.getCwnd());
            
            if (metricsService instanceof CongestionMetricsServiceImpl) {
                ((CongestionMetricsServiceImpl) metricsService).recordMetrics(taskId, algorithm);
            }
        }
        
        long currentCwnd = algorithm != null ? algorithm.getCwnd() : 5 * 1024 * 1024;
        return ChunkUploadVO.builder()
                .fileId(dto.getFileId())
                .chunkNumber(dto.getChunkNumber())
                .success(false)
                .cwnd(currentCwnd)
                .message("分片上传失败: " + e.getMessage())
                .build();
    }
    
    /**
     * 获取或创建任务ID（同一 fileId 只复用一条未完成任务，避免重试时出现两条记录）
     * 若该文件已有未完成任务（PENDING/PROCESSING/PAUSED/FAILED），复用并置为 PROCESSING；否则创建新任务。
     *
     * @param fileId 文件ID
     * @return 任务ID
     */
    private String getOrCreateTaskId(Long fileId) {
        List<TransferTask> existingTasks = transferTaskMapper.selectByFileId(fileId);
        if (existingTasks != null && !existingTasks.isEmpty()) {
            // 复用任意未完成任务（优先 FAILED/PAUSED 便于重试场景），同一文件只保留一条任务
            TransferTask reuse = existingTasks.stream()
                    .filter(t -> {
                        String s = t.getTransferStatus();
                        return "PENDING".equals(s) || "PROCESSING".equals(s) || "PAUSED".equals(s) || "FAILED".equals(s);
                    })
                    .findFirst()
                    .orElse(null);
            if (reuse != null) {
                String originalStatus = reuse.getTransferStatus();
                reuse.setTransferStatus("PROCESSING");
                transferTaskMapper.updateById(reuse);
                log.debug("复用未完成任务 - fileId: {}, taskId: {}, 原状态: {}", fileId, reuse.getTaskId(), originalStatus);
                return reuse.getTaskId();
            }
        }
        // 无未完成任务时才创建新任务
        String taskId = transferTaskService.createTask(fileId, "UPLOAD");
        TransferTask newTask = transferTaskMapper.selectByTaskId(taskId);
        if (newTask != null) {
            newTask.setTransferStatus("PROCESSING");
            transferTaskMapper.updateById(newTask);
        }
        return taskId;
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
    
    /**
     * 是否应打印分片相关日志（采样：首片、每 N 片、末片），减少大量分片时的刷屏。
     *
     * @param chunkNumber    当前分片号（从 1 开始）
     * @param totalChunks    总分片数，<= 0 表示未知（仅按 chunkNumber 采样）
     * @param completedChunks 已完成分片数，< 0 表示未知
     * @return true 则打印日志
     */
    private boolean shouldLogChunk(int chunkNumber, int totalChunks, int completedChunks) {
        if (chunkNumber <= 0) {
            return false;
        }
        boolean first = (chunkNumber == 1);
        boolean interval = (chunkNumber % CHUNK_LOG_SAMPLE_INTERVAL == 1);
        if (totalChunks <= 0 || completedChunks < 0) {
            return first || interval;
        }
        boolean last = (completedChunks == totalChunks);
        return first || interval || last;
    }
}

