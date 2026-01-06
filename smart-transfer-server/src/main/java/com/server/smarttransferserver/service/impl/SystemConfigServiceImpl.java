package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.server.smarttransferserver.dto.CongestionConfigDTO;
import com.server.smarttransferserver.entity.SystemConfig;
import com.server.smarttransferserver.mapper.SystemConfigMapper;
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
public class SystemConfigServiceImpl {
    
    @Autowired
    private SystemConfigMapper configMapper;
    
    /**
     * 获取拥塞控制配置
     *
     * @return 配置映射
     */
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
     *
     * @param dto 配置DTO
     */
    @Transactional
    public void updateCongestionConfig(CongestionConfigDTO dto) {
        log.info("更新拥塞控制配置 - 算法: {}", dto.getAlgorithm());
        
        // 更新各个配置项
        updateConfigValue("congestion.algorithm", dto.getAlgorithm(), "拥塞控制算法");
        
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
        
        if (dto.getMaxRate() != null) {
            updateConfigValue("congestion.max-rate", dto.getMaxRate().toString(), "最大速率");
        }
        
        if (dto.getMinRate() != null) {
            updateConfigValue("congestion.min-rate", dto.getMinRate().toString(), "最小速率");
        }
        
        log.info("拥塞控制配置更新完成");
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
    public String getConfigValue(String key) {
        SystemConfig config = configMapper.selectByConfigKey(key);
        return config != null ? config.getConfigValue() : null;
    }
    
    /**
     * 获取所有配置
     *
     * @return 配置列表
     */
    public List<SystemConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }
}

