package com.server.smarttransferserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 拥塞控制配置DTO
 */
@Data
public class CongestionConfigDTO {
    
    /**
     * 算法名称：CUBIC, BBR, ADAPTIVE
     * 注意：允许为null，支持部分更新配置
     */
    private String algorithm;
    
    /**
     * 初始拥塞窗口（字节）
     */
    @Min(value = 1024, message = "初始拥塞窗口不能小于1KB")
    @JsonProperty("initial-cwnd")
    private Long initialCwnd;
    
    /**
     * 慢启动阈值（字节）
     */
    @Min(value = 1024, message = "慢启动阈值不能小于1KB")
    private Long ssthresh;
    
    /**
     * 最大拥塞窗口（字节）
     */
    @Min(value = 1024, message = "最大拥塞窗口不能小于1KB")
    @JsonProperty("max-cwnd")
    private Long maxCwnd;
    
    /**
     * 最小拥塞窗口（字节）
     */
    @Min(value = 1024, message = "最小拥塞窗口不能小于1KB")
    @JsonProperty("min-cwnd")
    private Long minCwnd;
    
    // ========== 自适应算法配置 ==========
    
    /**
     * 丢包率阈值
     */
    @JsonProperty("loss-rate-threshold")
    private Double lossRateThreshold;
    
    /**
     * RTT抖动阈值（毫秒）
     */
    @JsonProperty("rtt-jitter-threshold")
    private Long rttJitterThreshold;
    
    /**
     * 评估间隔（毫秒）
     */
    @JsonProperty("evaluation-interval")
    private Long evaluationInterval;
    
    /**
     * 趋势窗口大小
     */
    @JsonProperty("trend-window-size")
    private Integer trendWindowSize;
    
    /**
     * 趋势变化率阈值
     */
    @JsonProperty("trend-threshold")
    private Double trendThreshold;
    
    /**
     * 置信度阈值
     */
    @JsonProperty("confidence-threshold")
    private Double confidenceThreshold;
    
    /**
     * 基准算法
     */
    @JsonProperty("baseline-algorithm")
    private String baselineAlgorithm;
    
    /**
     * 预热RTT周期数
     */
    @JsonProperty("warmup-rtt-count")
    private Integer warmupRttCount;
    
    /**
     * 是否启用异常值过滤
     */
    @JsonProperty("outlier-filter-enabled")
    private Boolean outlierFilterEnabled;
    
    /**
     * 回滚阈值
     */
    @JsonProperty("rollback-threshold")
    private Double rollbackThreshold;
    
    /**
     * 最小切换间隔（毫秒）
     */
    @JsonProperty("min-switch-interval")
    private Long minSwitchInterval;
}

