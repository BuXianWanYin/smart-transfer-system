package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.BBRAlgorithm;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.congestion.CubicAlgorithm;
import com.server.smarttransferserver.congestion.RenoAlgorithm;
import com.server.smarttransferserver.congestion.VegasAlgorithm;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithmMetrics;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

/**
 * 拥塞控制Controller
 */
@Slf4j
@RestController
@RequestMapping("/congestion")
public class CongestionController {
    
    @Autowired
    private CongestionMetricsService metricsService;
    
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
     * 当前使用的算法
     */
    private CongestionControlAlgorithm currentAlgorithm;
    
    /**
     * 初始化：设置默认算法为 ADAPTIVE
     */
    @PostConstruct
    public void init() {
        // 默认使用 ADAPTIVE 算法
        if (adaptiveAlgorithm != null) {
            currentAlgorithm = adaptiveAlgorithm;
            log.info("拥塞控制初始化完成 - 默认算法: ADAPTIVE");
        } else if (cubicAlgorithm != null) {
            currentAlgorithm = cubicAlgorithm;
            log.info("拥塞控制初始化完成 - 默认算法: CUBIC");
        } else if (bbrAlgorithm != null) {
            currentAlgorithm = bbrAlgorithm;
            log.info("拥塞控制初始化完成 - 默认算法: BBR");
        } else if (vegasAlgorithm != null) {
            currentAlgorithm = vegasAlgorithm;
            log.info("拥塞控制初始化完成 - 默认算法: Vegas");
        } else if (renoAlgorithm != null) {
            currentAlgorithm = renoAlgorithm;
            log.info("拥塞控制初始化完成 - 默认算法: Reno");
        } else {
            log.warn("未找到任何拥塞控制算法实现");
        }
    }
    
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
     * @param algorithm 算法名称（RENO, VEGAS, CUBIC, BBR, ADAPTIVE）
     * @return 切换结果
     */
    @PostMapping("/algorithm")
    public Result<String> switchAlgorithm(@RequestParam String algorithm) {
        log.info("切换拥塞控制算法 - 算法: {}", algorithm);
        try {
            switch (algorithm.toUpperCase()) {
                case "RENO":
                    if (renoAlgorithm != null) {
                        renoAlgorithm.initialize();
                        currentAlgorithm = renoAlgorithm;
                    } else {
                        return Result.error("Reno算法未初始化");
                    }
                    break;
                    
                case "VEGAS":
                    if (vegasAlgorithm != null) {
                        vegasAlgorithm.initialize();
                        currentAlgorithm = vegasAlgorithm;
                    } else {
                        return Result.error("Vegas算法未初始化");
                    }
                    break;
                    
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
            if (currentAlgorithm == null || !(currentAlgorithm instanceof AdaptiveAlgorithm)) {
                return Result.error("当前算法不是自适应算法");
            }
            
            AdaptiveAlgorithm adaptiveAlg = (AdaptiveAlgorithm) currentAlgorithm;
            AdaptiveAlgorithmMetrics metrics = adaptiveAlg.getMetrics();
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取自适应算法指标失败", e);
            return Result.error("获取指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前系统算法实例（供算法管理器使用）
     * 
     * @return 当前算法实例
     */
    public CongestionControlAlgorithm getCurrentAlgorithmInstance() {
        return currentAlgorithm;
    }
    
    /**
     * 获取当前系统算法类型名称
     * 
     * @return 算法类型名称（RENO, VEGAS, CUBIC, BBR, ADAPTIVE）
     */
    public String getCurrentAlgorithmType() {
        if (currentAlgorithm == null) {
            return "NONE";
        }
        String name = currentAlgorithm.getAlgorithmName();
        if (name.startsWith("Adaptive")) {
            return "ADAPTIVE";
        }
        return name.toUpperCase();
    }
    
    /**
     * 获取算法bean（供算法管理器创建新实例使用）
     */
    public RenoAlgorithm getRenoAlgorithm() {
        return renoAlgorithm;
    }
    
    public VegasAlgorithm getVegasAlgorithm() {
        return vegasAlgorithm;
    }
    
    public CubicAlgorithm getCubicAlgorithm() {
        return cubicAlgorithm;
    }
    
    public BBRAlgorithm getBbrAlgorithm() {
        return bbrAlgorithm;
    }
    
    public AdaptiveAlgorithm getAdaptiveAlgorithm() {
        return adaptiveAlgorithm;
    }
}

