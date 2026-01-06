package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.IFileChecksumService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件校验服务实现
 * 提供MD5、SHA256等哈希计算
 */
@Slf4j
@Service
public class FileChecksumServiceImpl implements IFileChecksumService {
    
    /**
     * 计算文件MD5
     *
     * @param filePath 文件路径
     * @return MD5值
     * @throws IOException IO异常
     */
    @Override
    public String calculateMD5(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            String md5 = DigestUtils.md5Hex(fis);
            log.debug("计算文件MD5 - 文件: {}, MD5: {}", filePath, md5);
            return md5;
        }
    }
    
    /**
     * 计算文件SHA256
     *
     * @param filePath 文件路径
     * @return SHA256值
     * @throws IOException IO异常
     */
    @Override
    public String calculateSHA256(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            String sha256 = DigestUtils.sha256Hex(fis);
            log.debug("计算文件SHA256 - 文件: {}, SHA256: {}", filePath, sha256);
            return sha256;
        }
    }
    
    /**
     * 计算MultipartFile的MD5
     *
     * @param file 上传文件
     * @return MD5值
     * @throws IOException IO异常
     */
    @Override
    public String calculateMD5(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            String md5 = DigestUtils.md5Hex(is);
            log.debug("计算上传文件MD5 - 文件名: {}, MD5: {}", file.getOriginalFilename(), md5);
            return md5;
        }
    }
    
    /**
     * 计算MultipartFile的SHA256
     *
     * @param file 上传文件
     * @return SHA256值
     * @throws IOException IO异常
     */
    @Override
    public String calculateSHA256(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            String sha256 = DigestUtils.sha256Hex(is);
            log.debug("计算上传文件SHA256 - 文件名: {}, SHA256: {}", file.getOriginalFilename(), sha256);
            return sha256;
        }
    }
    
    /**
     * 验证文件哈希值
     *
     * @param filePath     文件路径
     * @param expectedHash 期望的哈希值
     * @param algorithm    算法（MD5或SHA256）
     * @return 是否匹配
     */
    @Override
    public boolean verifyHash(String filePath, String expectedHash, String algorithm) {
        try {
            String actualHash;
            if ("MD5".equalsIgnoreCase(algorithm)) {
                actualHash = calculateMD5(filePath);
            } else if ("SHA256".equalsIgnoreCase(algorithm)) {
                actualHash = calculateSHA256(filePath);
            } else {
                log.error("不支持的哈希算法: {}", algorithm);
                return false;
            }
            
            boolean match = actualHash.equalsIgnoreCase(expectedHash);
            log.info("文件哈希校验 - 文件: {}, 算法: {}, 结果: {}", 
                     filePath, algorithm, match ? "通过" : "失败");
            return match;
            
        } catch (IOException e) {
            log.error("文件哈希校验失败 - 文件: {}, 错误: {}", filePath, e.getMessage());
            return false;
        }
    }
    
    /**
     * 快速计算文件哈希（只计算文件的部分内容，用于快速比对）
     *
     * @param filePath 文件路径
     * @return 快速哈希值
     * @throws IOException              IO异常
     * @throws NoSuchAlgorithmException 算法异常
     */
    @Override
    public String calculateQuickHash(String filePath) throws IOException, NoSuchAlgorithmException {
        Path path = Paths.get(filePath);
        long fileSize = Files.size(path);
        
        // 读取文件头1MB和尾1MB
        int sampleSize = 1024 * 1024; // 1MB
        
        MessageDigest digest = MessageDigest.getInstance("MD5");
        
        try (FileInputStream fis = new FileInputStream(filePath)) {
            // 读取文件头
            byte[] buffer = new byte[Math.min((int) fileSize, sampleSize)];
            int read = fis.read(buffer);
            if (read > 0) {
                digest.update(buffer, 0, read);
            }
            
            // 如果文件大于2MB，跳到文件尾读取
            if (fileSize > sampleSize * 2) {
                fis.skip(fileSize - sampleSize * 2);
                read = fis.read(buffer);
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        
        // 加入文件大小
        digest.update(String.valueOf(fileSize).getBytes());
        
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        
        String quickHash = sb.toString();
        log.debug("计算快速哈希 - 文件: {}, 快速哈希: {}", filePath, quickHash);
        return quickHash;
    }
}

