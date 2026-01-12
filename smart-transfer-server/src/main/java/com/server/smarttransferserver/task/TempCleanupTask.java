package com.server.smarttransferserver.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 临时文件清理任务
 * 定期清理超过24小时的临时分片文件
 */
@Slf4j
@Component
public class TempCleanupTask {
    
    @Value("${transfer.temp-path:./file-storage/temp}")
    private String tempPath;
    
    @PostConstruct
    public void init() {
        // 转换为绝对路径
        String userDir = System.getProperty("user.dir");
        if (tempPath.startsWith("./") || tempPath.startsWith(".\\")) {
            tempPath = Paths.get(userDir, tempPath.substring(2)).toString();
        } else if (!Paths.get(tempPath).isAbsolute()) {
            tempPath = Paths.get(userDir, tempPath).toString();
        }
        log.info("临时文件清理任务初始化完成 - 临时路径: {}", tempPath);
    }
    
    /**
     * 每天凌晨2点执行清理任务
     * 删除超过24小时的临时分片文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldTempFiles() {
        log.info("开始清理临时文件...");
        
        try {
            Path tempDir = Paths.get(tempPath);
            if (!Files.exists(tempDir)) {
                log.debug("临时目录不存在，跳过清理");
                return;
            }
            
            File tempDirFile = tempDir.toFile();
            File[] userDirs = tempDirFile.listFiles(File::isDirectory);
            
            if (userDirs == null) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            long expiredTime = 24 * 60 * 60 * 1000; // 24小时
            int deletedCount = 0;
            
            // 遍历每个用户的临时目录
            for (File userDir : userDirs) {
                File[] fileDirs = userDir.listFiles(File::isDirectory);
                if (fileDirs == null) {
                    continue;
                }
                
                // 遍历每个文件的临时分片目录
                for (File fileDir : fileDirs) {
                    long lastModified = fileDir.lastModified();
                    long age = currentTime - lastModified;
                    
                    // 如果超过24小时未使用，删除
                    if (age > expiredTime) {
                        try {
                            FileUtils.deleteDirectory(fileDir);
                            deletedCount++;
                            log.debug("删除过期临时目录: {}", fileDir.getAbsolutePath());
                        } catch (Exception e) {
                            log.warn("删除临时目录失败: {}, 错误: {}", fileDir.getAbsolutePath(), e.getMessage());
                        }
                    }
                }
                
                // 如果用户目录为空，也删除
                if (userDir.listFiles() == null || userDir.listFiles().length == 0) {
                    try {
                        Files.deleteIfExists(userDir.toPath());
                        log.debug("删除空用户目录: {}", userDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.warn("删除空用户目录失败: {}, 错误: {}", userDir.getAbsolutePath(), e.getMessage());
                    }
                }
            }
            
            log.info("临时文件清理完成 - 删除目录数: {}", deletedCount);
            
        } catch (Exception e) {
            log.error("清理临时文件失败", e);
        }
    }
}
