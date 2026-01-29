package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithmMetrics;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.service.CongestionAlgorithmService;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 拥塞控制Controller
 * 已废弃：监控功能已移除，管理员不再需要此功能
 * 原因：不同用户传输的算法不一致，无法统一显示全局监控数据
 * 
 * @deprecated 此Controller已废弃，所有接口已禁用，返回403禁止访问
 */
@Slf4j
@RestController
@RequestMapping("/congestion")
@Deprecated
public class CongestionController {
    
    @Autowired
    private CongestionMetricsService metricsService;
    
    @Autowired
    private CongestionAlgorithmService algorithmService;
    
    /**
     * 获取当前拥塞控制指标
     * 已禁用：监控功能已移除
     *
     * @return 403禁止访问
     */
    @GetMapping("/metrics")
    public Result<CongestionMetricsVO> getCurrentMetrics() {
        log.warn("尝试访问已禁用的监控接口: /congestion/metrics");
        return Result.error(403, "监控功能已移除，此接口不可用");
    }
    
    /**
     * 获取当前使用的算法
     * 已禁用：监控功能已移除
     *
     * @return 403禁止访问
     */
    @GetMapping("/algorithm")
    public Result<String> getCurrentAlgorithm() {
        log.warn("尝试访问已禁用的监控接口: /congestion/algorithm");
        return Result.error(403, "监控功能已移除，此接口不可用");
    }
    
    /**
     * 切换拥塞控制算法
     * 已禁用：监控功能已移除
     *
     * @param algorithm 算法名称
     * @return 403禁止访问
     */
    @PostMapping("/algorithm")
    public Result<String> switchAlgorithm(@RequestParam String algorithm) {
        log.warn("尝试访问已禁用的监控接口: /congestion/algorithm (POST)");
        return Result.error(403, "监控功能已移除，此接口不可用");
    }
    
    /**
     * 获取算法状态详情
     * 已禁用：监控功能已移除
     *
     * @return 403禁止访问
     */
    @GetMapping("/status")
    public Result<String> getAlgorithmStatus() {
        log.warn("尝试访问已禁用的监控接口: /congestion/status");
        return Result.error(403, "监控功能已移除，此接口不可用");
    }
    
    /**
     * 获取自适应算法详细指标
     * 已禁用：监控功能已移除
     *
     * @return 403禁止访问
     */
    @GetMapping("/adaptive-metrics")
    public Result<AdaptiveAlgorithmMetrics> getAdaptiveMetrics() {
        log.warn("尝试访问已禁用的监控接口: /congestion/adaptive-metrics");
        return Result.error(403, "监控功能已移除，此接口不可用");
    }
    
    /**
     * 获取网络质量统计
     * 已禁用：监控功能已移除
     *
     * @return 403禁止访问
     */
    @GetMapping("/network-quality-stats")
    public Result<Map<String, Object>> getNetworkQualityStats() {
        log.warn("尝试访问已禁用的监控接口: /congestion/network-quality-stats");
        return Result.error(403, "监控功能已移除，此接口不可用");
    }
}

