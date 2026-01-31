package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
     * **关键修复：为每个任务使用独立的算法实例**
     *
     * @param dto 分片上传DTO
     * @return 上传结果
     */
    private ChunkUploadVO uploadChunkInternal(ChunkUploadDTO dto) {
        log.info("上传分片 - 文件ID: {}, 分片: {}", dto.getFileId(), dto.getChunkNumber());
        
        // **关键修复：获取或创建任务，确保每个任务有独立的算法实例**
        String taskId = getOrCreateTaskId(dto.getFileId());
        CongestionControlAlgorithm algorithm = algorithmManager.getOrCreateAlgorithm(taskId);
        
        // 丢包率优化：上一分片在客户端的重试次数视为应用层“丢包”，计入滑动窗口（上限防恶意/异常值，与前端 RETRY_COUNT_MAX 一致）
        Integer retryCount = dto.getClientRetryCount();
        if (retryCount != null && retryCount > 0 && algorithm != null) {
            int capped = Math.min(retryCount, CongestionClientMetricsConstants.RETRY_COUNT_CAP);
            long chunkSizeForLoss = dto.getFile().getSize();
            for (int i = 0; i < capped; i++) {
                algorithm.onLoss(chunkSizeForLoss);
            }
            if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
                log.debug("应用层丢包统计 - 上一分片重试{}次，计入{}次丢包", retryCount, capped);
            }
        }
        
        // 记录分片上传开始时间（用于计算RTT）
        String chunkKey = dto.getFileId() + "-" + dto.getChunkNumber();
        long startTime = System.currentTimeMillis();
        chunkStartTimes.put(chunkKey, startTime);
        
        try {
            // 注：分片哈希验证已跳过，文件完整性在合并时通过整体哈希验证
            
            // 1. 保存分片文件
            long chunkSize = dto.getFile().getSize();
            storageService.saveChunk(dto.getFileId(), dto.getChunkNumber(), dto.getFile());
            
            // 2. RTT：优先使用客户端测量的上一分片网络往返时延，否则用服务端处理时间
            long endTime = System.currentTimeMillis();
            long serverProcessingMs = endTime - startTime;
            chunkStartTimes.remove(chunkKey);
            
            // **DEBUG: 记录原始值（移到外面，确保每个采样分片都打印）**
            if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
                log.info("RTT原始值 - 分片{}: clientRttMs={}, serverProcessingMs={}ms", 
                        dto.getChunkNumber(), 
                        (dto.getClientRttMs() != null ? dto.getClientRttMs() + "ms" : "null"), 
                        serverProcessingMs);
            }
            
            long rtt;
            if (dto.getClientRttMs() != null && dto.getClientRttMs() > 0) {
                rtt = Math.max(CongestionClientMetricsConstants.RTT_MS_MIN, Math.min(CongestionClientMetricsConstants.RTT_MS_MAX, dto.getClientRttMs()));
            } else {
                rtt = serverProcessingMs;
            }
            
            // 3. 记录传输字节数用于带宽估计
            bandwidthEstimator.recordSent(chunkSize);
            
            // 4. 传播时延：仅使用前端独立探测 RTT（与 Clumsy 一致），不再用分片推算
            Long propagationRttMs = null;
            if (algorithm != null) {
                Long userId = UserContextHolder.getUserId();
                Long probeRtt = userId != null ? probeRttStore.get(userId) : null;
                if (probeRtt != null && probeRtt > 0) {
                    propagationRttMs = probeRtt;
                }
                if (algorithm instanceof com.server.smarttransferserver.congestion.AdaptiveAlgorithm) {
                    int totalChunks = fileIdToTotalChunks.getOrDefault(dto.getFileId(), 0);
                    ((com.server.smarttransferserver.congestion.AdaptiveAlgorithm) algorithm).setTotalChunks(totalChunks);
                }
                algorithm.onAck(chunkSize, rtt, propagationRttMs);
                if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
                    log.debug("拥塞控制响应ACK - 任务ID: {}, 文件ID: {}, 分片: {}, 算法: {}, fullRtt: {}ms, propRtt: {}ms, cwnd: {}字节", 
                             taskId, dto.getFileId(), dto.getChunkNumber(), algorithm.getAlgorithmName(), rtt, propagationRttMs, algorithm.getCwnd());
                }
                // **关键修复：记录指标到数据库（关联taskId）**
                // 使用CongestionMetricsServiceImpl的recordMetrics方法（不是接口方法，需要强制转换）
                if (metricsService instanceof CongestionMetricsServiceImpl) {
                    ((CongestionMetricsServiceImpl) metricsService).recordMetrics(taskId, algorithm);
                }
            }
            
            // 5. 更新分片记录（如果分片记录不存在，则创建）
            FileChunk chunk = fileChunkMapper.selectByFileIdAndChunkNumber(dto.getFileId(), dto.getChunkNumber());
            if (chunk != null) {
                chunk.setChunkHash(dto.getChunkHash());
                chunk.setUploadStatus("COMPLETED");
                fileChunkMapper.updateById(chunk);
            } else {
                // 如果分片记录不存在（可能是在初始化时未创建或编号不匹配），创建新记录
                log.warn("分片记录不存在，创建新记录 - 文件ID: {}, 分片: {}", dto.getFileId(), dto.getChunkNumber());
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
            
            // **修复CRITICAL-2: 上传成功后更新Redis缓存（用于断点续传）**
            try {
                uploadCacheService.markChunkUploaded(dto.getFileId(), dto.getChunkNumber());
                if (shouldLogChunk(dto.getChunkNumber(), -1, -1)) {
                    log.debug("更新Redis分片缓存 - 文件ID: {}, 分片: {}", dto.getFileId(), dto.getChunkNumber());
                }
            } catch (Exception e) {
                log.warn("更新Redis分片缓存失败 - 文件ID: {}, 分片: {}, 错误: {}", 
                        dto.getFileId(), dto.getChunkNumber(), e.getMessage());
                // Redis缓存失败不影响上传流程，但可能影响断点续传性能
            }
            
            // 6. 统计上传进度
            QueryWrapper<FileChunk> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id", dto.getFileId());
            List<FileChunk> allChunks = fileChunkMapper.selectList(queryWrapper);
            int totalChunks = allChunks.size();
            // 兜底：若 fileIdToTotalChunks 未设置（如断点续传未走 checkChunk），用 DB 分片数保证丢包率分母正确
            if (algorithm instanceof com.server.smarttransferserver.congestion.AdaptiveAlgorithm && totalChunks > 0) {
                ((com.server.smarttransferserver.congestion.AdaptiveAlgorithm) algorithm).setTotalChunks(totalChunks);
            }
            long completedChunks = allChunks.stream()
                    .filter(c -> "COMPLETED".equals(c.getUploadStatus()))
                    .count();
            // 修复：防止除零，如果totalChunks为0，进度设为0
            double progress = totalChunks > 0 ? (double) completedChunks / totalChunks * 100 : 0;
            
            // 7. 获取当前拥塞窗口大小（用于前端调整并发数）
            long currentCwnd = algorithm != null ? algorithm.getCwnd() : 5 * 1024 * 1024;
            
            // 仅首片、每 N 片、末片时打印进度日志，避免大量分片刷屏
            if (shouldLogChunk(dto.getChunkNumber(), totalChunks, (int) completedChunks)) {
                String progressStr = String.format("%.2f", progress);
                log.info("分片上传成功 - 任务ID: {}, 文件ID: {}, 分片: {}, 进度: {}%, RTT: {}ms, 算法: {}, cwnd: {}字节", 
                         taskId, dto.getFileId(), dto.getChunkNumber(), progressStr, rtt, 
                         algorithm != null ? algorithm.getAlgorithmName() : "NONE", currentCwnd);
            }
            
            long rateBps = algorithm != null ? algorithm.getRate() : 0L;
            // 单向传播时延 = propagationRttMs / 2，与 Clumsy 的「延迟」一致（配 50ms 即返回 50ms）
            Long propagationRttOneWay = propagationRttMs != null ? propagationRttMs / 2 : null;
            return ChunkUploadVO.builder()
                    .fileId(dto.getFileId())
                    .chunkNumber(dto.getChunkNumber())
                    .success(true)
                    .completedChunks((int) completedChunks)
                    .totalChunks(totalChunks)
                    .progress(progress)
                    .cwnd(currentCwnd)  // 返回当前拥塞窗口
                    .rtt(rtt)           // 返回本次RTT（fullRtt，含传输时间）
                    .rate(rateBps > 0 ? rateBps : null)
                    .propagationRtt(propagationRttOneWay)  // 单向传播时延，与 Clumsy「延迟」一致
                    .message("分片上传成功")
                    .build();
            
        } catch (IOException e) {
            log.error("分片上传失败 - 任务ID: {}, 文件ID: {}, 分片: {}, 错误: {}", 
                      taskId, dto.getFileId(), dto.getChunkNumber(), e.getMessage());
            
            // **关键修复：上传失败视为丢包，使用任务独立的算法实例触发丢包响应**
            long chunkSize = dto.getFile().getSize();
            chunkStartTimes.remove(chunkKey);
            
            if (algorithm != null) {
                algorithm.onLoss(chunkSize);
                log.warn("拥塞控制响应丢包 - 任务ID: {}, 算法: {}, 分片大小: {}字节, 当前cwnd: {}字节", 
                        taskId, algorithm.getAlgorithmName(), chunkSize, algorithm.getCwnd());
                
                // **关键修复：记录丢包后的指标到数据库**
                if (metricsService instanceof CongestionMetricsServiceImpl) {
                    ((CongestionMetricsServiceImpl) metricsService).recordMetrics(taskId, algorithm);
                }
            }
            
            long currentCwnd = algorithm != null ? algorithm.getCwnd() : 5 * 1024 * 1024;
            
            return ChunkUploadVO.builder()
                    .fileId(dto.getFileId())
                    .chunkNumber(dto.getChunkNumber())
                    .success(false)
                    .cwnd(currentCwnd)  // 即使失败也返回cwnd，让前端降低并发
                    .message("分片上传失败: " + e.getMessage())
                    .build();
        }
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

