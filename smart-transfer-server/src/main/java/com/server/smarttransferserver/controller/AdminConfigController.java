package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.CongestionConfigDTO;
import com.server.smarttransferserver.entity.SystemConfig;
import com.server.smarttransferserver.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 管理员系统配置控制器
 * 包含所有需要管理员权限的系统配置接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/config")
@RequireAdmin
public class AdminConfigController {
    
    @Autowired
    private SystemConfigService configService;
    
    /**
     * 获取拥塞控制配置
     */
    @GetMapping("/congestion")
    public Result<Map<String, String>> getCongestionConfig() {
        log.info("获取拥塞控制配置");
        try {
            Map<String, String> config = configService.getCongestionConfig();
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取拥塞控制配置失败", e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新拥塞控制配置
     */
    @PostMapping("/congestion")
    public Result<String> updateCongestionConfig(@Valid @RequestBody CongestionConfigDTO dto) {
        log.info("更新拥塞控制配置 - 算法: {}", dto.getAlgorithm());
        try {
            configService.updateCongestionConfig(dto);
            return Result.success("配置更新成功");
        } catch (Exception e) {
            log.error("更新拥塞控制配置失败", e);
            return Result.error("配置更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新配置（从数据库重新加载）
     */
    @PostMapping("/refresh")
    public Result<String> refreshConfig() {
        log.info("刷新系统配置");
        try {
            configService.refreshConfig();
            return Result.success("配置刷新成功");
        } catch (Exception e) {
            log.error("刷新配置失败", e);
            return Result.error("刷新配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有配置
     */
    @GetMapping("/list")
    public Result<List<SystemConfig>> getAllConfigs() {
        log.info("获取所有配置");
        try {
            List<SystemConfig> configs = configService.getAllConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取所有配置失败", e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据键获取配置值
     */
    @GetMapping("/value")
    public Result<String> getConfigValue(@RequestParam String key) {
        log.info("获取配置值 - key: {}", key);
        try {
            String value = configService.getConfigValue(key);
            return Result.success(value);
        } catch (Exception e) {
            log.error("获取配置值失败 - key: {}", key, e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }
}
