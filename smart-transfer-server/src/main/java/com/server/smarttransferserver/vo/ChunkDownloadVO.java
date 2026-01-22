package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块下载结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDownloadVO {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 分块编号
     */
    private Integer chunkNumber;
    
    /**
     * 下载是否成功
     */
    private Boolean success;
    
    /**
     * 已完成的分块数
     */
    private Integer completedChunks;
    
    /**
     * 总分块数
     */
    private Integer totalChunks;
    
    /**
     * 下载进度（0-100）
     */
    private Double progress;
    
    /**
     * 当前拥塞窗口大小（字节）
     */
    private Long cwnd;
    
    /**
     * 往返时延RTT（毫秒）
     */
    private Long rtt;
    
    /**
     * 分块数据（Base64编码）
     */
    private String chunkData;
    
    /**
     * 消息
     */
    private String message;
}
