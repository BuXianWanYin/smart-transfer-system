package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadVO {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 分片序号
     */
    private Integer chunkNumber;
    
    /**
     * 是否上传成功
     */
    private Boolean success;
    
    /**
     * 已完成的分片数
     */
    private Integer completedChunks;
    
    /**
     * 总分片数
     */
    private Integer totalChunks;
    
    /**
     * 上传进度（百分比）
     */
    private Double progress;
    
    /**
     * 当前拥塞窗口大小（字节）
     * 前端可根据此值调整并发上传数
     */
    private Long cwnd;
    
    /**
     * 本次传输的RTT（毫秒）
     */
    private Long rtt;
    
    /**
     * 当前算法速率（字节/秒）
     */
    private Long rate;
    
    /**
     * 单向传播时延（毫秒）：与 Clumsy 的「延迟」一致（配 50ms 即返回 50ms）
     * 后端计算：(clientRtt - serverProcessingMs) / 2
     */
    private Long propagationRtt;
    
    /**
     * 提示信息
     */
    private String message;
}

