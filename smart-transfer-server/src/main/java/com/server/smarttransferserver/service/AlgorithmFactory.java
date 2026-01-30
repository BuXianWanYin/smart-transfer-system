package com.server.smarttransferserver.service;

import com.server.smarttransferserver.config.CongestionConfig;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.congestion.BBRAlgorithm;
import com.server.smarttransferserver.congestion.CubicAlgorithm;
import com.server.smarttransferserver.congestion.RenoAlgorithm;
import com.server.smarttransferserver.congestion.VegasAlgorithm;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 算法工厂
 * 为每个任务创建独立的算法实例，避免任务之间共享算法状态
 */
@Slf4j
@Component
public class AlgorithmFactory {
    
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 根据配置创建算法实例
     * 根据管理员配置的算法类型，为每个任务创建独立的算法实例
     *
     * @return 新的算法实例
     */
    public CongestionControlAlgorithm createAlgorithm() {
        String algorithmType = congestionConfig.getAlgorithm();
        if (algorithmType == null || algorithmType.isEmpty()) {
            algorithmType = "ADAPTIVE"; // 默认使用自适应算法
        }
        
        log.debug("根据配置创建算法实例 - 算法类型: {}", algorithmType);
        
        switch (algorithmType.toUpperCase()) {
            case "RENO":
                return createRenoAlgorithm();
            case "VEGAS":
                return createVegasAlgorithm();
            case "CUBIC":
                return createCubicAlgorithm();
            case "BBR":
                return createBBRAlgorithm();
            case "ADAPTIVE":
            default:
                return createAdaptiveAlgorithm();
        }
    }
    
    /**
     * 创建新的AdaptiveAlgorithm实例
     * 为每个任务创建独立的底层算法实例，确保任务之间互不干扰
     *
     * @return 新的AdaptiveAlgorithm实例
     */
    public AdaptiveAlgorithm createAdaptiveAlgorithm() {
        // **关键修复：为每个任务创建独立的底层算法实例**
        RenoAlgorithm newReno = createRenoAlgorithm();
        VegasAlgorithm newVegas = createVegasAlgorithm();
        CubicAlgorithm newCubic = createCubicAlgorithm();
        BBRAlgorithm newBBR = createBBRAlgorithm();
        
        // 创建新的AdaptiveAlgorithm实例
        AdaptiveAlgorithm newAdaptive = new AdaptiveAlgorithm(
            newReno, newVegas, newCubic, newBBR
        );
        
        // **关键修复：手动设置CongestionConfig（因为不是通过Spring管理的实例）**
        setCongestionConfig(newAdaptive, congestionConfig);
        
        // 初始化
        newAdaptive.initialize();
        
        log.debug("创建新的AdaptiveAlgorithm实例 - 包含独立的底层算法实例");
        return newAdaptive;
    }
    
    /**
     * 创建新的RenoAlgorithm实例
     */
    private RenoAlgorithm createRenoAlgorithm() {
        RenoAlgorithm reno = new RenoAlgorithm();
        // 手动设置依赖（因为不是通过Spring管理的）
        setCongestionConfig(reno, congestionConfig);
        reno.initialize();
        return reno;
    }
    
    /**
     * 创建新的VegasAlgorithm实例
     */
    private VegasAlgorithm createVegasAlgorithm() {
        VegasAlgorithm vegas = new VegasAlgorithm();
        setCongestionConfig(vegas, congestionConfig);
        vegas.initialize();
        return vegas;
    }
    
    /**
     * 创建新的CubicAlgorithm实例
     */
    private CubicAlgorithm createCubicAlgorithm() {
        CubicAlgorithm cubic = new CubicAlgorithm();
        setCongestionConfig(cubic, congestionConfig);
        cubic.initialize();
        return cubic;
    }
    
    /**
     * 创建新的BBRAlgorithm实例
     */
    private BBRAlgorithm createBBRAlgorithm() {
        BBRAlgorithm bbr = new BBRAlgorithm();
        setCongestionConfig(bbr, congestionConfig);
        bbr.initialize();
        return bbr;
    }
    
    /**
     * 通过反射设置CongestionConfig
     * 由于算法类使用@Autowired注入，我们需要手动设置
     */
    private void setCongestionConfig(Object algorithm, CongestionConfig config) {
        try {
            java.lang.reflect.Field field = algorithm.getClass().getDeclaredField("congestionConfig");
            field.setAccessible(true);
            field.set(algorithm, config);
        } catch (Exception e) {
            log.warn("设置CongestionConfig失败 - 算法: {}", algorithm.getClass().getSimpleName(), e);
        }
    }
}
