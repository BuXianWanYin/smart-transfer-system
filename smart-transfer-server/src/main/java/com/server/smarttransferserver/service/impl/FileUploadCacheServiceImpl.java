package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.FileUploadCacheService;
import com.server.smarttransferserver.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件上传缓存服务实现
 */
@Slf4j
@Service
public class FileUploadCacheServiceImpl implements FileUploadCacheService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 文件哈希缓存前缀
     */
    private static final String FILE_HASH_PREFIX = "file:hash:";
    
    /**
     * 分片上传记录前缀
     */
    private static final String CHUNK_UPLOAD_PREFIX = "file:chunks:";
    
    /**
     * 文件上传锁前缀
     */
    private static final String UPLOAD_LOCK_PREFIX = "lock:upload:";
    
    /**
     * 文件上传进度前缀
     */
    private static final String UPLOAD_PROGRESS_PREFIX = "file:progress:";
    
    /**
     * 缓存过期时间：24小时
     */
    private static final long CACHE_EXPIRE_HOURS = 24;
    
    /**
     * 锁过期时间：30秒
     */
    private static final long LOCK_EXPIRE_SECONDS = 30;
    
    @Override
    public void cacheFileHash(String fileHash, Long fileId) {
        String key = FILE_HASH_PREFIX + fileHash;
        redisService.set(key, fileId, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("缓存文件哈希 - hash: {}, fileId: {}", fileHash, fileId);
    }
    
    @Override
    public Long getFileIdByHash(String fileHash) {
        String key = FILE_HASH_PREFIX + fileHash;
        Object value = redisService.get(key);
        if (value != null) {
            log.info("Redis秒传命中 - hash: {}, fileId: {}", fileHash, value);
            return Long.valueOf(value.toString());
        }
        return null;
    }
    
    @Override
    public void deleteFileHashCache(String fileHash) {
        String key = FILE_HASH_PREFIX + fileHash;
        redisService.delete(key);
        log.debug("删除文件哈希缓存 - hash: {}", fileHash);
    }
    
    @Override
    public void markChunkUploaded(Long fileId, Integer chunkNumber) {
        String key = CHUNK_UPLOAD_PREFIX + fileId;
        redisService.sAdd(key, chunkNumber);
        // 设置过期时间
        redisService.expire(key, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("标记分片已上传 - fileId: {}, chunk: {}", fileId, chunkNumber);
    }
    
    @Override
    public Set<Integer> getUploadedChunks(Long fileId) {
        String key = CHUNK_UPLOAD_PREFIX + fileId;
        Set<Object> chunks = redisService.sMembers(key);
        if (chunks != null) {
            return chunks.stream()
                    .map(obj -> Integer.valueOf(obj.toString()))
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
    
    @Override
    public void deleteChunkUploadRecord(Long fileId) {
        String key = CHUNK_UPLOAD_PREFIX + fileId;
        redisService.delete(key);
        log.debug("删除分片上传记录 - fileId: {}", fileId);
    }
    
    @Override
    public Boolean tryLockFileUpload(String fileHash, String lockId) {
        String key = UPLOAD_LOCK_PREFIX + fileHash;
        Boolean locked = redisService.tryLock(key, lockId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("尝试获取上传锁 - hash: {}, lockId: {}, result: {}", fileHash, lockId, locked);
        return locked;
    }
    
    @Override
    public Boolean unlockFileUpload(String fileHash, String lockId) {
        String key = UPLOAD_LOCK_PREFIX + fileHash;
        Boolean unlocked = redisService.releaseLock(key, lockId);
        log.debug("释放上传锁 - hash: {}, lockId: {}, result: {}", fileHash, lockId, unlocked);
        return unlocked;
    }
    
    @Override
    public void cacheUploadProgress(Long fileId, Integer progress) {
        String key = UPLOAD_PROGRESS_PREFIX + fileId;
        redisService.set(key, progress, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("缓存上传进度 - fileId: {}, progress: {}%", fileId, progress);
    }
    
    @Override
    public Integer getUploadProgress(Long fileId) {
        String key = UPLOAD_PROGRESS_PREFIX + fileId;
        Object value = redisService.get(key);
        if (value != null) {
            return Integer.valueOf(value.toString());
        }
        return null;
    }
}

