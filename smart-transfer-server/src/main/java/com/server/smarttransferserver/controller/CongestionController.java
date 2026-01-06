package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.BBRAlgorithm;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.congestion.CubicAlgorithm;
import com.server.smarttransferserver.service.impl.CongestionMetricsServiceImpl;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 拥塞控制Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/congestion")
public class CongestionController {
    
    @Autowired
    private CongestionMetricsServiceImpl metricsService;
    
    @Autowired(required = false)
    private CubicAlgorithm cubicAlgorithm;
    
    @Autowired(required = false)
    private BBRAlgorithm bbrAlgorithm;
    
    @Autowired(required = false)
    private AdaptiveAlgorithm adaptiveAlgorithm;
    
    /**
     * 当前使用的算法
     */
    private CongestionControlAlgorithm currentAlgorithm;
    
    /**
     * 获取当前拥塞控制指标
     *
     * @return 指标信息
     */
    @GetMapping("/metrics")
    public Result<CongestionMetricsVO> getCurrentMetrics() {
        log.info("获取当前拥塞控制指标");
        try {
            CongestionMetricsVO metrics = metricsService.getCurrentMetrics(currentAlgorithm);
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
            String algorithm = currentAlgorithm != null ? 
                    currentAlgorithm.getAlgorithmName() : "NONE";
            return Result.success(algorithm);
        } catch (Exception e) {
            log.error("获取当前算法失败", e);
            return Result.error("获取算法失败: " + e.getMessage());
        }
    }
    
    /**
     * 切换拥塞控制算法
     *
     * @param algorithm 算法名称（CUBIC, BBR, ADAPTIVE）
     * @return 切换结果
     */
    @PostMapping("/algorithm")
    public Result<String> switchAlgorithm(@RequestParam String algorithm) {
        log.info("切换拥塞控制算法 - 算法: {}", algorithm);
        try {
            switch (algorithm.toUpperCase()) {
                case "CUBIC":
                    if (cubicAlgorithm != null) {
                        cubicAlgorithm.initialize();
                        currentAlgorithm = cubicAlgorithm;
                    } else {
                        return Result.error("CUBIC算法未初始化");
                    }
                    break;
                    
                case "BBR":
                    if (bbrAlgorithm != null) {
                        bbrAlgorithm.initialize();
                        currentAlgorithm = bbrAlgorithm;
                    } else {
                        return Result.error("BBR算法未初始化");
                    }
                    break;
                    
                case "ADAPTIVE":
                    if (adaptiveAlgorithm != null) {
                        adaptiveAlgorithm.initialize();
                        currentAlgorithm = adaptiveAlgorithm;
                    } else {
                        return Result.error("Adaptive算法未初始化");
                    }
                    break;
                    
                default:
                    return Result.error("不支持的算法: " + algorithm);
            }
            
            log.info("算法切换成功 - 当前算法: {}", currentAlgorithm.getAlgorithmName());
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
            if (currentAlgorithm == null) {
                return Result.success("未初始化");
            }
            
            String status = String.format(
                    "算法: %s, 状态: %s, CWND: %d字节, 速率: %d字节/秒",
                    currentAlgorithm.getAlgorithmName(),
                    currentAlgorithm.getState().getDescription(),
                    currentAlgorithm.getCwnd(),
                    currentAlgorithm.getRate()
            );
            
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取算法状态失败", e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }
}

