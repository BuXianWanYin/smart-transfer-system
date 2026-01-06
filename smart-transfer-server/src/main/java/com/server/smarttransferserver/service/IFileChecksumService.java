package com.server.smarttransferserver.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * 文件校验服务接口
 * 提供MD5、SHA256等哈希计算
 */
public interface IFileChecksumService {
    
    /**
     * 计算文件MD5
     *
     * @param filePath 文件路径
     * @return MD5值
     * @throws IOException IO异常
     */
    String calculateMD5(String filePath) throws IOException;
    
    /**
     * 计算文件SHA256
     *
     * @param filePath 文件路径
     * @return SHA256值
     * @throws IOException IO异常
     */
    String calculateSHA256(String filePath) throws IOException;
    
    /**
     * 计算MultipartFile的MD5
     *
     * @param file 上传文件
     * @return MD5值
     * @throws IOException IO异常
     */
    String calculateMD5(MultipartFile file) throws IOException;
    
    /**
     * 计算MultipartFile的SHA256
     *
     * @param file 上传文件
     * @return SHA256值
     * @throws IOException IO异常
     */
    String calculateSHA256(MultipartFile file) throws IOException;
    
    /**
     * 验证文件哈希值
     *
     * @param filePath     文件路径
     * @param expectedHash 期望的哈希值
     * @param algorithm    算法（MD5或SHA256）
     * @return 是否匹配
     */
    boolean verifyHash(String filePath, String expectedHash, String algorithm);
    
    /**
     * 快速计算文件哈希（只计算文件的部分内容，用于快速比对）
     *
     * @param filePath 文件路径
     * @return 快速哈希值
     * @throws IOException              IO异常
     * @throws NoSuchAlgorithmException 算法异常
     */
    String calculateQuickHash(String filePath) throws IOException, NoSuchAlgorithmException;
}

