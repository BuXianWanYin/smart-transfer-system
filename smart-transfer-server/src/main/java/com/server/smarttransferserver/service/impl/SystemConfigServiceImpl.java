package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.config.CongestionConfig;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.BBRAlgorithm;
import com.server.smarttransferserver.congestion.CubicAlgorithm;
import com.server.smarttransferserver.congestion.RenoAlgorithm;
import com.server.smarttransferserver.congestion.VegasAlgorithm;
import com.server.smarttransferserver.dto.CongestionConfigDTO;
import com.server.smarttransferserver.entity.SystemConfig;
import com.server.smarttransferserver.mapper.SystemConfigMapper;
import com.server.smarttransferserver.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {
    
    @Autowired
    private SystemConfigMapper configMapper;
    
    @Autowired
    private CongestionConfig congestionConfig;
    
    @Autowired(required = false)
    private RenoAlgorithm renoAlgorithm;
    
    @Autowired(required = false)
    private VegasAlgorithm vegasAlgorithm;
    
    @Autowired(required = false)
    private CubicAlgorithm cubicAlgorithm;
    
    @Autowired(required = false)
    private BBRAlgorithm bbrAlgorithm;
    
    @Autowired(required = false)
    private AdaptiveAlgorithm adaptiveAlgorithm;
    
    /**
     * 获取拥塞控制配置
     *
     * @return 配置映射
     */
    @Override
    public Map<String, String> getCongestionConfig() {
        Map<String, String> config = new HashMap<>();
        
        // 查询所有拥塞控制相关配置
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("config_key", "congestion.");
        List<SystemConfig> configs = configMapper.selectList(queryWrapper);
        
        for (SystemConfig sc : configs) {
            String key = sc.getConfigKey().replace("congestion.", "");
            config.put(key, sc.getConfigValue());
        }
        
        log.info("获取拥塞控制配置 - 配置项数: {}", config.size());
        return config;
    }
    
    /**
     * 更新拥塞控制配置
     * 包括更新数据库配置和刷新内存配置
     *
     * @param dto 配置DTO
     */
    @Override
    @Transactional
    public void updateCongestionConfig(CongestionConfigDTO dto) {
        log.info("更新拥塞控制配置 - 算法: {}", dto.getAlgorithm());
        
        // 更新各个配置项到数据库
        if (dto.getAlgorithm() != null) {
            updateConfigValue("congestion.algorithm", dto.getAlgorithm(), "拥塞控制算法");
        }
        
        if (dto.getInitialCwnd() != null) {
            updateConfigValue("congestion.initial-cwnd", dto.getInitialCwnd().toString(), "初始拥塞窗口");
        }
        
        if (dto.getSsthresh() != null) {
            updateConfigValue("congestion.ssthresh", dto.getSsthresh().toString(), "慢启动阈值");
        }
        
        if (dto.getMaxCwnd() != null) {
            updateConfigValue("congestion.max-cwnd", dto.getMaxCwnd().toString(), "最大拥塞窗口");
        }
        
        if (dto.getMinCwnd() != null) {
            updateConfigValue("congestion.min-cwnd", dto.getMinCwnd().toString(), "最小拥塞窗口");
        }

        if (dto.getLossRateThreshold() != null) {
            updateConfigValue("congestion.loss-rate-threshold", dto.getLossRateThreshold().toString(), "丢包率阈值");
        }
        
        if (dto.getRttJitterThreshold() != null) {
            updateConfigValue("congestion.rtt-jitter-threshold", dto.getRttJitterThreshold().toString(), "RTT抖动阈值");
        }
        
        if (dto.getEvaluationInterval() != null) {
            updateConfigValue("congestion.evaluation-interval", dto.getEvaluationInterval().toString(), "评估间隔");
        }
        
        if (dto.getTrendWindowSize() != null) {
            updateConfigValue("congestion.trend-window-size", dto.getTrendWindowSize().toString(), "趋势窗口大小");
        }
        
        if (dto.getTrendThreshold() != null) {
            updateConfigValue("congestion.trend-threshold", dto.getTrendThreshold().toString(), "趋势变化率阈值");
        }
        
        if (dto.getConfidenceThreshold() != null) {
            updateConfigValue("congestion.confidence-threshold", dto.getConfidenceThreshold().toString(), "置信度阈值");
        }
        
        if (dto.getBaselineAlgorithm() != null) {
            updateConfigValue("congestion.baseline-algorithm", dto.getBaselineAlgorithm(), "基准算法");
        }
        
        if (dto.getWarmupRttCount() != null) {
            updateConfigValue("congestion.warmup-rtt-count", dto.getWarmupRttCount().toString(), "预热RTT周期数");
        }
        
        if (dto.getOutlierFilterEnabled() != null) {
            updateConfigValue("congestion.outlier-filter-enabled", dto.getOutlierFilterEnabled().toString(), "异常值过滤开关");
        }
        
        if (dto.getRollbackThreshold() != null) {
            updateConfigValue("congestion.rollback-threshold", dto.getRollbackThreshold().toString(), "回滚阈值");
        }
        
        if (dto.getMinSwitchInterval() != null) {
            updateConfigValue("congestion.min-switch-interval", dto.getMinSwitchInterval().toString(), "最小切换间隔");
        }
        
        // 刷新内存中的配置
        congestionConfig.refresh();

        reinitializeAlgorithms();
        
        log.info("拥塞控制配置更新完成，已刷新内存配置并重新初始化算法");
    }
    
    /**
     * 重新初始化所有拥塞控制算法
     * 使新配置生效
     */
    private void reinitializeAlgorithms() {
        if (renoAlgorithm != null) {
            renoAlgorithm.initialize();
            log.info("Reno算法已使用新配置重新初始化");
        }
        if (vegasAlgorithm != null) {
            vegasAlgorithm.initialize();
            log.info("Vegas算法已使用新配置重新初始化");
        }
        if (cubicAlgorithm != null) {
            cubicAlgorithm.initialize();
            log.info("CUBIC算法已使用新配置重新初始化");
        }
        if (bbrAlgorithm != null) {
            bbrAlgorithm.initialize();
            log.info("BBR算法已使用新配置重新初始化");
        }
        if (adaptiveAlgorithm != null) {
            adaptiveAlgorithm.initialize();
            log.info("Adaptive算法已使用新配置重新初始化");
        }
    }
    
    /**
     * 更新配置值
     *
     * @param key         配置键
     * @param value       配置值
     * @param description 描述
     */
    private void updateConfigValue(String key, String value, String description) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        
        if (config != null) {
            // 更新现有配置
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
        } else {
            // 创建新配置
            config = SystemConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .description(description)
                    .updateTime(LocalDateTime.now())
                    .build();
            configMapper.insert(config);
        }
        
        log.debug("更新配置 - key: {}, value: {}", key, value);
    }
    
    /**
     * 根据键获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    @Override
    public String getConfigValue(String key) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        return config != null ? config.getConfigValue() : null;
    }
    
    /**
     * 根据键获取配置值（带默认值）
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，不存在返回默认值
     */
    @Override
    public String getConfigValue(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 设置配置值
     *
     * @param key 配置键
     * @param value 配置值
     * @return 是否设置成功
     */
    @Override
    @Transactional
    public boolean setConfigValue(String key, String value) {
        updateConfigValue(key, value, "");
        return true;
    }
    
    /**
     * 批量设置配置
     *
     * @param configs 配置Map
     * @return 是否设置成功
     */
    @Override
    @Transactional
    public boolean setConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            updateConfigValue(entry.getKey(), entry.getValue(), "");
        }
        return true;
    }
    
    /**
     * 获取所有配置
     *
     * @return 配置列表
     */
    @Override
    public List<SystemConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }
    
    /**
     * 删除配置
     *
     * @param key 配置键
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteConfig(String key) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        if (config != null) {
            removeById(config.getId());
            return true;
        }
        return false;
    }
    
    /**
     * 刷新配置（从数据库重新加载）
     */
    @Override
    public void refreshConfig() {
        log.info("刷新系统配置");
        congestionConfig.refresh();
    }
}

