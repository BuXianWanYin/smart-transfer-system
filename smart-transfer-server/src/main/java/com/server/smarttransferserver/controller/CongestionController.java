package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithmMetrics;
import com.server.smarttransferserver.service.CongestionAlgorithmService;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 拥塞控制Controller
 * 需要管理员权限
 */
@Slf4j
@RestController
@RequestMapping("/congestion")
@RequireAdmin
public class CongestionController {
    
    @Autowired
    private CongestionMetricsService metricsService;
    
    @Autowired
    private CongestionAlgorithmService algorithmService;
    
    /**
     * 获取当前拥塞控制指标
     *
     * @return 指标信息
     */
    @GetMapping("/metrics")
    public Result<CongestionMetricsVO> getCurrentMetrics() {
        log.info("获取当前拥塞控制指标");
        try {
            CongestionMetricsVO metrics = metricsService.getCurrentMetrics();
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取拥塞控制指标失败", e);
            return Result.error("获取指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前使用的算法
     *
     * @return 算法名称
     */
    @GetMapping("/algorithm")
    public Result<String> getCurrentAlgorithm() {
        log.info("获取当前算法");
        try {
            String algorithm = algorithmService.getCurrentAlgorithmName();
            return Result.success(algorithm);
        } catch (Exception e) {
            log.error("获取当前算法失败", e);
            return Result.error("获取算法失败: " + e.getMessage());
        }
    }
    
    /**
     * 切换拥塞控制算法
     *
     * @param algorithm 算法名称（RENO, VEGAS, CUBIC, BBR, ADAPTIVE）
     * @return 切换结果
     */
    @PostMapping("/algorithm")
    public Result<String> switchAlgorithm(@RequestParam String algorithm) {
        log.info("切换拥塞控制算法 - 算法: {}", algorithm);
        try {
            algorithmService.switchAlgorithm(algorithm);
            return Result.success("算法切换成功");
        } catch (Exception e) {
            log.error("切换算法失败", e);
            return Result.error("切换算法失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取算法状态详情
     *
     * @return 状态详情
     */
    @GetMapping("/status")
    public Result<String> getAlgorithmStatus() {
        log.info("获取算法状态详情");
        try {
            String status = algorithmService.getAlgorithmStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取算法状态失败", e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取自适应算法详细指标（用于可观测性）
     * 包括算法评分、切换历史、网络趋势等
     *
     * @return 自适应算法指标
     */
    @GetMapping("/adaptive-metrics")
    public Result<AdaptiveAlgorithmMetrics> getAdaptiveMetrics() {
        log.info("获取自适应算法详细指标");
        try {
            AdaptiveAlgorithmMetrics metrics = algorithmService.getAdaptiveMetrics();
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取自适应算法指标失败", e);
            return Result.error("获取指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取网络质量统计
     * 从最近的拥塞指标中统计网络质量分布
     *
     * @return 网络质量统计数据
     */
    @GetMapping("/network-quality-stats")
    public Result<Map<String, Object>> getNetworkQualityStats() {
        log.info("获取网络质量统计");
        try {
            // 从metricsService获取网络质量统计，传入当前算法
            CongestionControlAlgorithm currentAlgorithm = algorithmService.getCurrentAlgorithm();
            Map<String, Object> stats = metricsService.getNetworkQualityStats(currentAlgorithm);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取网络质量统计失败", e);
            return Result.error("获取统计失败: " + e.getMessage());
        }
    }
}

