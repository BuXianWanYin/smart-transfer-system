package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.CongestionAlgorithmManager;
import com.server.smarttransferserver.service.DownloadCompleteService;
import com.server.smarttransferserver.service.RedisService;
import com.server.smarttransferserver.service.TransferTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 下载完成服务实现
 */
@Slf4j
@Service
public class DownloadCompleteServiceImpl implements DownloadCompleteService {
    
    @Autowired
    private TransferTaskService transferTaskService;
    
    @Autowired
    private CongestionAlgorithmManager algorithmManager;
    
    @Autowired
    private RedisService redisService;
    
    /**
     * Redis key前缀：存储已完成下载的分块集合
     */
    private static final String DOWNLOAD_COMPLETED_CHUNKS_KEY_PREFIX = "download:completed:chunks:";
    private static final String DOWNLOAD_CHUNK_SIZE_KEY_PREFIX = "download:chunkSize:";
    private static final String DOWNLOAD_FILE_SIZE_KEY_PREFIX = "download:fileSize:";
    
    @Override
    @Transactional
    public void completeDownload(String taskId) {
        log.info("标记下载任务完成 - 任务ID: {}", taskId);
        
        // 1. 更新任务状态为COMPLETED
        transferTaskService.updateTaskStatus(taskId, "COMPLETED");
        
        // 2. 清理算法实例
        algorithmManager.removeAlgorithm(taskId);
        
        // 3. **修复P1-1：清理Redis中的已完成分块记录和任务配置（释放内存）**
        try {
            redisService.delete(DOWNLOAD_COMPLETED_CHUNKS_KEY_PREFIX + taskId);
            redisService.delete(DOWNLOAD_CHUNK_SIZE_KEY_PREFIX + taskId);
            redisService.delete(DOWNLOAD_FILE_SIZE_KEY_PREFIX + taskId);
            log.debug("清理Redis下载任务数据 - 任务ID: {}", taskId);
        } catch (Exception e) {
            log.warn("清理Redis下载任务数据失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
            // 清理失败不影响任务完成
        }
        
        log.info("下载任务完成处理完成 - 任务ID: {}", taskId);
    }
    
    @Override
    @Transactional
    public void cancelDownload(String taskId) {
        log.info("取消下载任务 - 任务ID: {}", taskId);
        
        // 1. 更新任务状态为FAILED或PAUSED（如果任务存在）
        try {
            transferTaskService.updateTaskStatus(taskId, "FAILED");
        } catch (Exception e) {
            log.warn("更新任务状态失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
        }
        
        // 2. 清理算法实例
        try {
            algorithmManager.removeAlgorithm(taskId);
            log.debug("清理算法实例 - 任务ID: {}", taskId);
        } catch (Exception e) {
            log.warn("清理算法实例失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
        }
        
        // 3. **修复M1: 清理Redis中的已完成分块记录和任务配置**
        try {
            redisService.delete(DOWNLOAD_COMPLETED_CHUNKS_KEY_PREFIX + taskId);
            redisService.delete(DOWNLOAD_CHUNK_SIZE_KEY_PREFIX + taskId);
            redisService.delete(DOWNLOAD_FILE_SIZE_KEY_PREFIX + taskId);
            log.debug("清理Redis下载任务数据 - 任务ID: {}", taskId);
        } catch (Exception e) {
            log.warn("清理Redis下载任务数据失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
        }
        
        log.info("取消下载任务处理完成 - 任务ID: {}", taskId);
    }
}
