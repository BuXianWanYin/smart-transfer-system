package com.server.smarttransferserver.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储服务接口
 * 负责文件和分片的物理存储
 */
public interface IFileStorageService {
    
    /**
     * 保存分片文件
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @param file        分片文件
     * @return 分片文件路径
     * @throws IOException IO异常
     */
    String saveChunk(Long fileId, Integer chunkNumber, MultipartFile file) throws IOException;
    
    /**
     * 合并分片文件
     *
     * @param fileId      文件ID
     * @param fileName    文件名
     * @param totalChunks 总分片数
     * @return 合并后的文件路径
     * @throws IOException IO异常
     */
    String mergeChunks(Long fileId, String fileName, Integer totalChunks) throws IOException;
    
    /**
     * 删除临时分片文件
     *
     * @param fileId 文件ID
     */
    void deleteTempChunks(Long fileId);
    
    /**
     * 检查分片是否存在
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @return 是否存在
     */
    boolean chunkExists(Long fileId, Integer chunkNumber);
    
    /**
     * 获取分片文件大小
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @return 文件大小
     * @throws IOException IO异常
     */
    long getChunkSize(Long fileId, Integer chunkNumber) throws IOException;
    
    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    boolean fileExists(String filePath);
    
    /**
     * 删除文件
     *
     * @param filePath 文件路径
     */
    void deleteFile(String filePath);
    
    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小
     * @throws IOException IO异常
     */
    long getFileSize(String filePath) throws IOException;
    
    /**
     * 获取文件的绝对路径
     * 兼容相对路径和绝对路径（旧数据可能是绝对路径）
     *
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return 绝对路径
     */
    java.nio.file.Path getAbsoluteFilePath(String filePath);
    
    /**
     * 保存文件（用于复制/解压等场景）
     * 将源文件保存到用户存储目录，返回相对路径
     *
     * @param sourceFile 源文件
     * @param fileName 目标文件名
     * @param userId 用户ID
     * @return 相对路径（相对于storagePath，格式：userId/fileName）
     * @throws IOException IO异常
     */
    String saveFile(java.io.File sourceFile, String fileName, Long userId) throws IOException;
}

