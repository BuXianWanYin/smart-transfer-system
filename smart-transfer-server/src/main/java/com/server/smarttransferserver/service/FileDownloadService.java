package com.server.smarttransferserver.service;

import com.server.smarttransferserver.vo.FileDownloadInitVO;
import org.springframework.http.ResponseEntity;

/**
 * 文件下载服务接口
 * 支持分块下载和拥塞控制
 * **优化：使用二进制流传输（标准做法），而非Base64编码**
 */
public interface FileDownloadService {
    
    /**
     * 初始化文件下载
     * 检查文件是否存在，计算分块信息
     *
     * @param fileId 文件ID
     * @param chunkSize 分块大小（字节）
     * @return 下载初始化结果
     */
    FileDownloadInitVO initDownload(Long fileId, Long chunkSize);
    
    /**
     * 下载文件分块（二进制流传输）
     * 集成拥塞控制算法，在下载成功/失败时触发算法响应
     * **优化：直接返回二进制数据，元数据通过响应头传输**
     *
     * @param fileId 文件ID
     * @param chunkNumber 分块编号
     * @param startByte 起始字节位置
     * @param endByte 结束字节位置
     * @param clientRttMs 客户端测量的上一分片 RTT（毫秒），可选，用于拥塞算法使用真实网络往返时延
     * @param clientRetryCount 上一分片在客户端的重试次数，可选，用于应用层丢包率统计
     * @return ResponseEntity包含二进制数据和元数据响应头
     */
    ResponseEntity<byte[]> downloadChunk(Long fileId, Integer chunkNumber, Long startByte, Long endByte, Long clientRttMs, Integer clientRetryCount);
    
    /**
     * 处理下载分块错误
     * @param e 异常
     * @return 错误响应
     */
    ResponseEntity<byte[]> handleDownloadChunkError(Exception e);
}
