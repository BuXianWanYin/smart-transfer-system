package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.IFileStorageService;
import com.server.smarttransferserver.util.UserContextHolder;
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
import java.nio.file.StandardCopyOption;

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
        // 获取用户ID，按用户隔离临时文件
        Long userId = UserContextHolder.getUserId();
        String userDir = userId != null ? userId.toString() : "default";
        
        // 创建临时目录：temp/{userId}/{fileId}/
        Path chunkDir = Paths.get(tempPath, userDir, fileId.toString());
        Files.createDirectories(chunkDir);
        
        // 分片文件名：chunk_0, chunk_1, ...
        String chunkFileName = "chunk_" + chunkNumber;
        Path chunkPath = chunkDir.resolve(chunkFileName);
        
        // 保存分片
        file.transferTo(chunkPath.toFile());
        
        log.info("保存分片 - 用户ID: {}, 文件ID: {}, 分片: {}, 大小: {}字节", 
                 userId, fileId, chunkNumber, file.getSize());
        
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
        // 获取用户ID，按用户隔离文件存储
        Long userId = UserContextHolder.getUserId();
        String userDir = userId != null ? userId.toString() : "default";
        
        // 创建用户存储目录：storage/{userId}/
        Path storageDir = Paths.get(storagePath, userDir);
        Files.createDirectories(storageDir);
        
        // 目标文件路径：storage/{userId}/{fileName}
        Path targetPath = storageDir.resolve(fileName);
        File targetFile = targetPath.toFile();
        
        // 使用FileChannel进行高效合并
        try (FileOutputStream fos = new FileOutputStream(targetFile);
             FileChannel targetChannel = fos.getChannel()) {
            
            long position = 0;
            
            // 按顺序合并分片（从用户临时目录读取）
            for (int i = 0; i < totalChunks; i++) {
                Path chunkPath = Paths.get(tempPath, userDir, fileId.toString(), "chunk_" + i);
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
        
        // 返回相对路径（相对于storagePath），兼容不同部署环境
        // 格式：{userId}/{fileName}
        Path relativePath = Paths.get(storagePath).relativize(targetPath);
        String relativePathStr = relativePath.toString().replace("\\", "/"); // 统一使用 / 分隔符
        
        log.debug("文件路径 - 绝对路径: {}, 相对路径: {}", targetPath, relativePathStr);
        return relativePathStr;
    }
    
    /**
     * 删除临时分片文件
     *
     * @param fileId 文件ID
     */
    @Override
    public void deleteTempChunks(Long fileId) {
        try {
            // 获取用户ID
            Long userId = UserContextHolder.getUserId();
            String userDir = userId != null ? userId.toString() : "default";
            
            Path chunkDir = Paths.get(tempPath, userDir, fileId.toString());
            if (Files.exists(chunkDir)) {
                FileUtils.deleteDirectory(chunkDir.toFile());
                log.info("删除临时分片 - 用户ID: {}, 文件ID: {}", userId, fileId);
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
        Long userId = UserContextHolder.getUserId();
        String userDir = userId != null ? userId.toString() : "default";
        Path chunkPath = Paths.get(tempPath, userDir, fileId.toString(), "chunk_" + chunkNumber);
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
        Long userId = UserContextHolder.getUserId();
        String userDir = userId != null ? userId.toString() : "default";
        Path chunkPath = Paths.get(tempPath, userDir, fileId.toString(), "chunk_" + chunkNumber);
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
        Path path = getAbsolutePath(filePath);
        return Files.exists(path);
    }
    
    /**
     * 删除文件
     *
     * @param filePath 文件路径（支持相对路径和绝对路径，兼容旧数据）
     */
    @Override
    public void deleteFile(String filePath) {
        try {
            Path path = getAbsolutePath(filePath);
            
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("物理文件删除成功: {}", path);
            } else {
                log.warn("物理文件不存在，无需删除: {}", path);
            }
        } catch (IOException e) {
            log.error("删除文件失败: {}, 错误: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * 获取文件的绝对路径
     * 兼容相对路径和绝对路径（旧数据可能是绝对路径）
     *
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return 绝对路径
     */
    private Path getAbsolutePath(String filePath) {
        Path path = Paths.get(filePath);
        // 如果是绝对路径（旧数据兼容），直接使用
        if (path.isAbsolute()) {
            return path;
        }
        // 如果是相对路径（新数据），拼接storagePath
        return Paths.get(storagePath).resolve(filePath).normalize();
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
        Path path = getAbsolutePath(filePath);
        return Files.size(path);
    }
    
    @Override
    public Path getAbsoluteFilePath(String filePath) {
        return getAbsolutePath(filePath);
    }
    
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
    @Override
    public String saveFile(File sourceFile, String fileName, Long userId) throws IOException {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        // 创建用户存储目录
        String userDir = userId.toString();
        Path userStorageDir = Paths.get(storagePath, userDir);
        Files.createDirectories(userStorageDir);
        
        // 目标文件路径
        Path targetPath = userStorageDir.resolve(fileName);
        
        // 复制文件
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // 返回相对路径（格式：userId/fileName）
        String relativePath = userDir + "/" + fileName;
        log.info("保存文件成功 - 用户ID: {}, 文件名: {}, 相对路径: {}", userId, fileName, relativePath);
        
        return relativePath;
    }
}

