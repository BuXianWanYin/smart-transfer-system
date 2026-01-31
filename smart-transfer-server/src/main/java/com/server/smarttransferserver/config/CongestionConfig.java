package com.server.smarttransferserver.config;

import com.server.smarttransferserver.entity.SystemConfig;
import com.server.smarttransferserver.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 拥塞控制配置管理
 * 从数据库读取配置，支持动态更新
 */
@Slf4j
@Component
public class CongestionConfig {
    
    @Autowired
    private SystemConfigMapper configMapper;
    
    // 算法选择配置
    private String algorithm = "ADAPTIVE";     // 默认使用自适应算法 (RENO/VEGAS/CUBIC/BBR/ADAPTIVE)
    
    // CUBIC算法配置
    private long initialCwnd = 10485760L;      // 10MB
    private long ssthresh = 52428800L;         // 50MB
    private long maxCwnd = 104857600L;         // 100MB
    private long minCwnd = 1048576L;           // 1MB
    
    // 自适应算法配置
    private double lossRateThreshold = 0.01;   // 1%
    private long rttJitterThreshold = 50L;     // 50ms
    private long evaluationInterval = 5000L;   // 5秒
    
    // 网络趋势分析配置
    private int trendWindowSize = 5;           // 趋势窗口大小
    private double trendThreshold = 0.1;       // 趋势变化率阈值（10%）
    
    // 置信度阈值配置
    private double confidenceThreshold = 0.1;   // 算法切换置信度阈值（10%）
    
    // 基准算法配置
    private String baselineAlgorithm = "CUBIC"; // 基准算法（用于相对评分）
    
    // 算法预热配置
    private int warmupRttCount = 2;            // 算法预热RTT周期数
    
    // 异常值过滤配置
    private boolean outlierFilterEnabled = true; // 是否启用异常值过滤
    
    // 算法回滚配置
    private double rollbackThreshold = 0.2;     // 算法回滚阈值（20%性能下降）
    
    // 最小切换间隔配置
    private long minSwitchInterval = 10000L;    // 最小切换间隔（毫秒）
    
    /** 传播 RTT 上限（往返，毫秒）。0 表示不封顶；若使用 Clumsy 等工具，可设为 2×单向延迟（如 20ms 填 40，150ms 填 300），超出部分视为排队/上传时间。 */
    private long maxPropagationRttMs = 0L;
    
    /**
     * 初始化时从数据库加载配置
     */
    @PostConstruct
    public void init() {
        loadFromDatabase();
        log.info("拥塞控制配置已初始化");
    }
    
    /**
     * 从数据库加载配置
     */
    public void loadFromDatabase() {
        try {
            // 算法选择配置
            algorithm = getStringConfig("congestion.algorithm", algorithm);
            
            // CUBIC配置
            initialCwnd = getLongConfig("congestion.initial-cwnd", initialCwnd);
            ssthresh = getLongConfig("congestion.ssthresh", ssthresh);
            maxCwnd = getLongConfig("congestion.max-cwnd", maxCwnd);
            minCwnd = getLongConfig("congestion.min-cwnd", minCwnd);
            
            // 自适应算法配置
            lossRateThreshold = getDoubleConfig("congestion.loss-rate-threshold", lossRateThreshold);
            rttJitterThreshold = getLongConfig("congestion.rtt-jitter-threshold", rttJitterThreshold);
            evaluationInterval = getLongConfig("congestion.evaluation-interval", evaluationInterval);
            
            // 网络趋势分析配置
            trendWindowSize = (int) getLongConfig("congestion.trend-window-size", trendWindowSize);
            trendThreshold = getDoubleConfig("congestion.trend-threshold", trendThreshold);
            
            // 置信度阈值配置
            confidenceThreshold = getDoubleConfig("congestion.confidence-threshold", confidenceThreshold);
            
            // 基准算法配置
            baselineAlgorithm = getStringConfig("congestion.baseline-algorithm", baselineAlgorithm);
            
            // 算法预热配置
            warmupRttCount = (int) getLongConfig("congestion.warmup-rtt-count", warmupRttCount);
            
            // 异常值过滤配置
            outlierFilterEnabled = getBooleanConfig("congestion.outlier-filter-enabled", outlierFilterEnabled);
            
            // 算法回滚配置
            rollbackThreshold = getDoubleConfig("congestion.rollback-threshold", rollbackThreshold);
            
            // 最小切换间隔配置
            minSwitchInterval = getLongConfig("congestion.min-switch-interval", minSwitchInterval);
            
            // 传播 RTT 上限（0=不封顶；Clumsy 单向 20ms 可填 40，150ms 可填 300）
            maxPropagationRttMs = getLongConfig("congestion.max-propagation-rtt-ms", maxPropagationRttMs);
            
            log.info("从数据库加载拥塞控制配置 - algorithm: {}, initialCwnd: {}, maxCwnd: {}, trendWindowSize: {}, confidenceThreshold: {}", 
                    algorithm, initialCwnd, maxCwnd, trendWindowSize, confidenceThreshold);
        } catch (Exception e) {
            log.error("加载配置失败，使用默认值", e);
        }
    }
    
    /**
     * 刷新配置（API更新后调用）
     */
    public void refresh() {
        loadFromDatabase();
        log.info("拥塞控制配置已刷新");
    }
    
    /**
     * 获取Long类型配置
     */
    private long getLongConfig(String key, long defaultValue) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
            try {
                return Long.parseLong(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("配置值格式错误 - key: {}, value: {}", key, config.getConfigValue());
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取Double类型配置
     */
    private double getDoubleConfig(String key, double defaultValue) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
            try {
                return Double.parseDouble(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("配置值格式错误 - key: {}, value: {}", key, config.getConfigValue());
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取String类型配置
     */
    private String getStringConfig(String key, String defaultValue) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
            return config.getConfigValue();
        }
        return defaultValue;
    }
    
    /**
     * 获取Boolean类型配置
     */
    private boolean getBooleanConfig(String key, boolean defaultValue) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
            return Boolean.parseBoolean(config.getConfigValue());
        }
        return defaultValue;
    }
    
    // Getter方法
    public String getAlgorithm() {
        return algorithm;
    }
    
    public long getInitialCwnd() {
        return initialCwnd;
    }
    
    public long getSsthresh() {
        return ssthresh;
    }
    
    public long getMaxCwnd() {
        return maxCwnd;
    }
    
    public long getMinCwnd() {
        return minCwnd;
    }
    
    public double getLossRateThreshold() {
        return lossRateThreshold;
    }
    
    public long getRttJitterThreshold() {
        return rttJitterThreshold;
    }
    
    public long getEvaluationInterval() {
        return evaluationInterval;
    }
    
    public int getTrendWindowSize() {
        return trendWindowSize;
    }
    
    public double getTrendThreshold() {
        return trendThreshold;
    }
    
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    public String getBaselineAlgorithm() {
        return baselineAlgorithm;
    }
    
    public int getWarmupRttCount() {
        return warmupRttCount;
    }
    
    public boolean isOutlierFilterEnabled() {
        return outlierFilterEnabled;
    }
    
    public double getRollbackThreshold() {
        return rollbackThreshold;
    }
    
    public long getMinSwitchInterval() {
        return minSwitchInterval;
    }
    
    /** 传播 RTT 上限（往返 ms）。0 表示不封顶。 */
    public long getMaxPropagationRttMs() {
        return maxPropagationRttMs;
    }
}

