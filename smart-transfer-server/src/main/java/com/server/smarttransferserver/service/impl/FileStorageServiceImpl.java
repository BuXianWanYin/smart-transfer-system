package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.IFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储服务实现
 * 负责文件和分片的物理存储
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements IFileStorageService {
    
    /**
     * 文件存储根路径
     */
    @Value("${transfer.storage-path:./file-storage}")
    private String storagePath;
    
    /**
     * 临时文件路径
     */
    @Value("${transfer.temp-path:./file-storage/temp}")
    private String tempPath;

    /**
     * 初始化：将相对路径转为绝对路径，并创建目录
     */
    @PostConstruct
    public void init() {
        // 获取项目根目录（用户工作目录）
        String userDir = System.getProperty("user.dir");
        
        // 转换为绝对路径
        if (storagePath.startsWith("./") || storagePath.startsWith(".\\")) {
            storagePath = Paths.get(userDir, storagePath.substring(2)).toString();
        } else if (!Paths.get(storagePath).isAbsolute()) {
            storagePath = Paths.get(userDir, storagePath).toString();
        }
        
        if (tempPath.startsWith("./") || tempPath.startsWith(".\\")) {
            tempPath = Paths.get(userDir, tempPath.substring(2)).toString();
        } else if (!Paths.get(tempPath).isAbsolute()) {
            tempPath = Paths.get(userDir, tempPath).toString();
        }
        
        // 创建目录
        try {
            Files.createDirectories(Paths.get(storagePath));
            Files.createDirectories(Paths.get(tempPath));
            log.info("文件存储路径初始化完成 - 存储路径: {}, 临时路径: {}", storagePath, tempPath);
        } catch (IOException e) {
            log.error("创建存储目录失败", e);
        }
    }
    
    /**
     * 保存分片文件
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @param file        分片文件
     * @return 分片文件路径
     * @throws IOException IO异常
     */
    @Override
    public String saveChunk(Long fileId, Integer chunkNumber, MultipartFile file) throws IOException {
        // 创建临时目录
        Path chunkDir = Paths.get(tempPath, fileId.toString());
        Files.createDirectories(chunkDir);
        
        // 分片文件名：chunk_0, chunk_1, ...
        String chunkFileName = "chunk_" + chunkNumber;
        Path chunkPath = chunkDir.resolve(chunkFileName);
        
        // 保存分片
        file.transferTo(chunkPath.toFile());
        
        log.info("保存分片 - 文件ID: {}, 分片: {}, 大小: {}字节", 
                 fileId, chunkNumber, file.getSize());
        
        return chunkPath.toString();
    }
    
    /**
     * 合并分片文件
     *
     * @param fileId      文件ID
     * @param fileName    文件名
     * @param totalChunks 总分片数
     * @return 合并后的文件路径
     * @throws IOException IO异常
     */
    @Override
    public String mergeChunks(Long fileId, String fileName, Integer totalChunks) throws IOException {
        // 创建存储目录
        Path storageDir = Paths.get(storagePath);
        Files.createDirectories(storageDir);
        
        // 目标文件路径
        Path targetPath = storageDir.resolve(fileName);
        File targetFile = targetPath.toFile();
        
        // 使用FileChannel进行高效合并
        try (FileOutputStream fos = new FileOutputStream(targetFile);
             FileChannel targetChannel = fos.getChannel()) {
            
            long position = 0;
            
            // 按顺序合并分片
            for (int i = 0; i < totalChunks; i++) {
                Path chunkPath = Paths.get(tempPath, fileId.toString(), "chunk_" + i);
                File chunkFile = chunkPath.toFile();
                
                if (!chunkFile.exists()) {
                    throw new IOException("分片文件不存在: chunk_" + i);
                }
                
                // 使用FileChannel合并，手动维护位置
                try (FileInputStream fis = new FileInputStream(chunkFile);
                     FileChannel sourceChannel = fis.getChannel()) {
                    long chunkSize = sourceChannel.size();
                    // 从源通道传输到目标通道的正确位置
                    sourceChannel.transferTo(0, chunkSize, targetChannel);
                    position += chunkSize;
                }
                
                log.debug("合并分片 - 文件ID: {}, 分片: {}/{}, 当前位置: {}", fileId, i + 1, totalChunks, position);
            }
        }
        
        log.info("文件合并完成 - 文件ID: {}, 文件名: {}, 大小: {}字节", 
                 fileId, fileName, targetFile.length());
        
        return targetPath.toString();
    }
    
    /**
     * 删除临时分片文件
     *
     * @param fileId 文件ID
     */
    @Override
    public void deleteTempChunks(Long fileId) {
        try {
            Path chunkDir = Paths.get(tempPath, fileId.toString());
            if (Files.exists(chunkDir)) {
                FileUtils.deleteDirectory(chunkDir.toFile());
                log.info("删除临时分片 - 文件ID: {}", fileId);
            }
        } catch (IOException e) {
            log.error("删除临时分片失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage());
        }
    }
    
    /**
     * 检查分片是否存在
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @return 是否存在
     */
    @Override
    public boolean chunkExists(Long fileId, Integer chunkNumber) {
        Path chunkPath = Paths.get(tempPath, fileId.toString(), "chunk_" + chunkNumber);
        return Files.exists(chunkPath);
    }
    
    /**
     * 获取分片文件大小
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @return 文件大小
     * @throws IOException IO异常
     */
    @Override
    public long getChunkSize(Long fileId, Integer chunkNumber) throws IOException {
        Path chunkPath = Paths.get(tempPath, fileId.toString(), "chunk_" + chunkNumber);
        return Files.size(chunkPath);
    }
    
    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    @Override
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 删除文件
     *
     * @param filePath 文件路径
     */
    @Override
    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.info("删除文件: {}", filePath);
        } catch (IOException e) {
            log.error("删除文件失败: {}, 错误: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小
     * @throws IOException IO异常
     */
    @Override
    public long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }
}

