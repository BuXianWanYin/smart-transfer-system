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
    
    // CUBIC算法配置
    private long initialCwnd = 10485760L;      // 10MB
    private long ssthresh = 52428800L;         // 50MB
    private long maxCwnd = 104857600L;         // 100MB
    private long minCwnd = 1048576L;           // 1MB
    
    // 速率控制配置
    private long maxRate = 104857600L;         // 100MB/s
    private long minRate = 1048576L;           // 1MB/s
    
    // 自适应算法配置
    private double lossRateThreshold = 0.01;   // 1%
    private long rttJitterThreshold = 50L;     // 50ms
    private long evaluationInterval = 5000L;   // 5秒
    
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
            // CUBIC配置
            initialCwnd = getLongConfig("congestion.initial-cwnd", initialCwnd);
            ssthresh = getLongConfig("congestion.ssthresh", ssthresh);
            maxCwnd = getLongConfig("congestion.max-cwnd", maxCwnd);
            minCwnd = getLongConfig("congestion.min-cwnd", minCwnd);
            
            // 速率配置
            maxRate = getLongConfig("congestion.max-rate", maxRate);
            minRate = getLongConfig("congestion.min-rate", minRate);
            
            // 自适应算法配置
            lossRateThreshold = getDoubleConfig("congestion.loss-rate-threshold", lossRateThreshold);
            rttJitterThreshold = getLongConfig("congestion.rtt-jitter-threshold", rttJitterThreshold);
            evaluationInterval = getLongConfig("congestion.evaluation-interval", evaluationInterval);
            
            log.info("从数据库加载拥塞控制配置 - initialCwnd: {}, maxRate: {}", initialCwnd, maxRate);
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
    
    // Getter方法
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
    
    public long getMaxRate() {
        return maxRate;
    }
    
    public long getMinRate() {
        return minRate;
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
}

