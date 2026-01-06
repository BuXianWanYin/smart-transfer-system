package com.server.smarttransferserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.dto.CongestionConfigDTO;
import com.server.smarttransferserver.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 * 提供系统配置的查询、更新、保存等功能
 */
public interface SystemConfigService extends IService<SystemConfig> {
    
    /**
     * 获取拥塞控制配置
     *
     * @return 配置映射
     */
    Map<String, String> getCongestionConfig();
    
    /**
     * 更新拥塞控制配置
     * 包括更新数据库配置和刷新内存配置
     *
     * @param dto 配置DTO
     */
    void updateCongestionConfig(CongestionConfigDTO dto);
    
    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值，不存在返回null
     */
    String getConfigValue(String configKey);
    
    /**
     * 根据配置键获取配置值（带默认值）
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值，不存在返回默认值
     */
    String getConfigValue(String configKey, String defaultValue);
    
    /**
     * 设置配置值
     * 如果配置不存在则创建，存在则更新
     *
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否设置成功
     */
    boolean setConfigValue(String configKey, String configValue);
    
    /**
     * 批量设置配置
     *
     * @param configs 配置Map，key为配置键，value为配置值
     * @return 是否设置成功
     */
    boolean setConfigs(Map<String, String> configs);
    
    /**
     * 获取所有配置
     *
     * @return 配置列表
     */
    List<SystemConfig> getAllConfigs();
    
    /**
     * 删除配置
     *
     * @param configKey 配置键
     * @return 是否删除成功
     */
    boolean deleteConfig(String configKey);
    
    /**
     * 刷新配置（从数据库重新加载）
     */
    void refreshConfig();
}

