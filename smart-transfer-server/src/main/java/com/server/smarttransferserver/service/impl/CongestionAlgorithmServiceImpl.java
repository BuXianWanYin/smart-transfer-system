package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithmMetrics;
import com.server.smarttransferserver.congestion.BBRAlgorithm;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.congestion.CubicAlgorithm;
import com.server.smarttransferserver.congestion.RenoAlgorithm;
import com.server.smarttransferserver.congestion.VegasAlgorithm;
import com.server.smarttransferserver.service.CongestionAlgorithmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 拥塞控制算法服务实现
 */
@Slf4j
@Service
public class CongestionAlgorithmServiceImpl implements CongestionAlgorithmService {
    
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
    
    @Override
    public CongestionControlAlgorithm switchAlgorithm(String algorithm) {
        log.info("切换拥塞控制算法 - 算法: {}", algorithm);
        
        switch (algorithm.toUpperCase()) {
            case "RENO":
                if (renoAlgorithm != null) {
                    renoAlgorithm.initialize();
                    currentAlgorithm = renoAlgorithm;
                } else {
                    throw new RuntimeException("Reno算法未初始化");
                }
                break;
                
            case "VEGAS":
                if (vegasAlgorithm != null) {
                    vegasAlgorithm.initialize();
                    currentAlgorithm = vegasAlgorithm;
                } else {
                    throw new RuntimeException("Vegas算法未初始化");
                }
                break;
                
            case "CUBIC":
                if (cubicAlgorithm != null) {
                    cubicAlgorithm.initialize();
                    currentAlgorithm = cubicAlgorithm;
                } else {
                    throw new RuntimeException("CUBIC算法未初始化");
                }
                break;
                
            case "BBR":
                if (bbrAlgorithm != null) {
                    bbrAlgorithm.initialize();
                    currentAlgorithm = bbrAlgorithm;
                } else {
                    throw new RuntimeException("BBR算法未初始化");
                }
                break;
                
            case "ADAPTIVE":
                if (adaptiveAlgorithm != null) {
                    adaptiveAlgorithm.initialize();
                    currentAlgorithm = adaptiveAlgorithm;
                } else {
                    throw new RuntimeException("Adaptive算法未初始化");
                }
                break;
                
            default:
                throw new RuntimeException("不支持的算法: " + algorithm);
        }
        
        log.info("算法切换成功 - 当前算法: {}", currentAlgorithm.getAlgorithmName());
        return currentAlgorithm;
    }
    
    @Override
    public CongestionControlAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }
    
    @Override
    public String getCurrentAlgorithmName() {
        return currentAlgorithm != null ? currentAlgorithm.getAlgorithmName() : "NONE";
    }
    
    @Override
    public String getAlgorithmStatus() {
        if (currentAlgorithm == null) {
            return "未初始化";
        }
        
        return String.format(
                "算法: %s, 状态: %s, CWND: %d字节, 速率: %d字节/秒",
                currentAlgorithm.getAlgorithmName(),
                currentAlgorithm.getState().getDescription(),
                currentAlgorithm.getCwnd(),
                currentAlgorithm.getRate()
        );
    }
    
    @Override
    public AdaptiveAlgorithmMetrics getAdaptiveMetrics() {
        if (currentAlgorithm == null || !(currentAlgorithm instanceof AdaptiveAlgorithm)) {
            throw new RuntimeException("当前算法不是自适应算法");
        }
        
        AdaptiveAlgorithm adaptiveAlg = (AdaptiveAlgorithm) currentAlgorithm;
        return adaptiveAlg.getMetrics();
    }
}
