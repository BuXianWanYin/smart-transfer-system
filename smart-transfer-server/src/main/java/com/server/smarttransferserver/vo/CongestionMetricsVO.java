package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拥塞控制指标VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CongestionMetricsVO {
    
    /**
     * 任务ID（用于按任务分别推送指标）
     */
    private String taskId;
    
    /**
     * 当前算法名称
     */
    private String algorithm;
    
    /**
     * 拥塞窗口大小（字节）
     */
    private Long cwnd;
    
    /**
     * 慢启动阈值（字节）
     */
    private Long ssthresh;
    
    /**
     * 传输速率（字节/秒）
     */
    private Long rate;
    
    /**
     * 算法状态
     */
    private String state;
    
    /**
     * 往返时延RTT（毫秒）
     */
    private Long rtt;
    
    /**
     * 最小RTT（毫秒）
     */
    private Long minRtt;
    
    /**
     * 丢包率（0-1）
     */
    private Double lossRate;
    
    /**
     * 估算带宽（字节/秒）
     */
    private Long bandwidth;
    
    /**
     * 网络质量
     */
    private String networkQuality;
    
    /**
     * 在途数据包数量
     */
    private Integer inflightCount;
    
    /**
     * 在途数据字节数
     */
    private Long inflightBytes;
    
    /**
     * RTT抖动（毫秒）
     */
    private Long rttJitter;
    
    /**
     * 带宽时延积BDP（字节）
     */
    private Long bdp;
    
    /**
     * 网络趋势（上升/下降/平稳）
     */
    private String networkTrend;
    
    /**
     * 算法预热状态（true表示正在预热）
     */
    private Boolean isWarmingUp;
}

