package com.server.smarttransferserver.service;

import java.util.Set;

/**
 * 文件上传缓存服务接口
 * 使用 Redis 缓存文件上传相关信息，提升性能
 */
public interface FileUploadCacheService {
    
    /**
     * 缓存文件哈希对应的文件ID（用于秒传）
     * 
     * @param fileHash 文件哈希值
     * @param fileId   文件ID
     */
    void cacheFileHash(String fileHash, Long fileId);
    
    /**
     * 根据文件哈希获取文件ID（秒传查询）
     * 
     * @param fileHash 文件哈希值
     * @return 文件ID，不存在返回null
     */
    Long getFileIdByHash(String fileHash);
    
    /**
     * 删除文件哈希缓存
     * 
     * @param fileHash 文件哈希值
     */
    void deleteFileHashCache(String fileHash);
    
    /**
     * 记录已上传的分片
     * 
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     */
    void markChunkUploaded(Long fileId, Integer chunkNumber);
    
    /**
     * 获取已上传的分片列表
     * 
     * @param fileId 文件ID
     * @return 已上传的分片序号集合
     */
    Set<Integer> getUploadedChunks(Long fileId);
    
    /**
     * 删除分片上传记录（合并完成后清理）
     * 
     * @param fileId 文件ID
     */
    void deleteChunkUploadRecord(Long fileId);
    
    /**
     * 尝试获取文件上传锁（防止并发上传同一文件）
     * 
     * @param fileHash 文件哈希值
     * @param lockId   锁ID（UUID）
     * @return 是否获取成功
     */
    Boolean tryLockFileUpload(String fileHash, String lockId);
    
    /**
     * 释放文件上传锁
     * 
     * @param fileHash 文件哈希值
     * @param lockId   锁ID
     * @return 是否释放成功
     */
    Boolean unlockFileUpload(String fileHash, String lockId);
    
    /**
     * 缓存文件上传进度
     * 
     * @param fileId   文件ID
     * @param progress 上传进度（0-100）
     */
    void cacheUploadProgress(Long fileId, Integer progress);
    
    /**
     * 获取文件上传进度
     * 
     * @param fileId 文件ID
     * @return 上传进度，不存在返回null
     */
    Integer getUploadProgress(Long fileId);
}

