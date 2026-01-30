package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.BBRAlgorithm;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.congestion.CubicAlgorithm;
import com.server.smarttransferserver.congestion.RenoAlgorithm;
import com.server.smarttransferserver.congestion.VegasAlgorithm;
import com.server.smarttransferserver.service.AlgorithmFactory;
import com.server.smarttransferserver.service.CongestionAlgorithmManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拥塞控制算法管理器实现
 * 为每个传输任务维护独立的算法实例，确保不同任务的网络状态互不干扰
 */
@Slf4j
@Service
public class CongestionAlgorithmManagerImpl implements CongestionAlgorithmManager {
    
    /**
     * 任务ID到算法实例的映射
     * Key: 任务ID, Value: 算法实例
     */
    private final Map<String, CongestionControlAlgorithm> algorithmMap = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private RenoAlgorithm renoAlgorithm;
    
    @Autowired(required = false)
    private VegasAlgorithm vegasAlgorithm;
    
    @Autowired(required = false)
    private CubicAlgorithm cubicAlgorithm;
    
    @Autowired(required = false)
    private BBRAlgorithm bbrAlgorithm;
    
    @Autowired
    private AlgorithmFactory algorithmFactory;
    
    /**
     * 为任务创建或获取算法实例
     * 根据系统配置的当前算法，为每个任务创建独立的算法实例
     *
     * @param taskId 任务ID
     * @return 算法实例
     */
    @Override
    public CongestionControlAlgorithm getOrCreateAlgorithm(String taskId) {
        return algorithmMap.computeIfAbsent(taskId, k -> {
            // 创建新的算法实例（根据当前系统配置的算法类型）
            CongestionControlAlgorithm newAlgorithm = createNewAlgorithmInstance();
            log.info("为任务创建算法实例 - 任务ID: {}, 算法: {}", taskId, newAlgorithm.getAlgorithmName());
            return newAlgorithm;
        });
    }
    
    /**
     * 获取任务对应的算法实例
     *
     * @param taskId 任务ID
     * @return 算法实例，如果不存在则返回null
     */
    @Override
    public CongestionControlAlgorithm getAlgorithm(String taskId) {
        return algorithmMap.get(taskId);
    }
    
    /**
     * 移除任务的算法实例（任务完成或失败时调用）
     *
     * @param taskId 任务ID
     */
    @Override
    public void removeAlgorithm(String taskId) {
        CongestionControlAlgorithm removed = algorithmMap.remove(taskId);
        if (removed != null) {
            log.info("移除任务算法实例 - 任务ID: {}, 算法: {}", taskId, removed.getAlgorithmName());
        }
    }
    
    /**
     * 移除所有算法实例（清理资源）
     */
    @Override
    public void clearAll() {
        int count = algorithmMap.size();
        algorithmMap.clear();
        log.info("清理所有算法实例 - 清理数量: {}", count);
    }
    
    /**
     * 根据当前系统配置的算法类型创建新的算法实例
     * **关键修复：使用AlgorithmFactory为每个任务创建完全独立的算法实例**
     * 
     * 这样每个任务有独立的：
     * 1. 算法实例（根据管理员配置：RENO/VEGAS/CUBIC/BBR/ADAPTIVE）
     * 2. 对于AdaptiveAlgorithm：独立的rttSamples, totalPackets等和底层算法实例
     * 3. 算法状态（cwnd, state, ssthresh等）- 完全独立
     * 
     * 结果：任务之间完全隔离，互不干扰
     *
     * @return 新的算法实例
     */
    private CongestionControlAlgorithm createNewAlgorithmInstance() {
        // **关键修复：使用AlgorithmFactory根据配置创建完全独立的算法实例**
        if (algorithmFactory != null) {
            CongestionControlAlgorithm newAlgorithm = algorithmFactory.createAlgorithm();
            log.debug("通过AlgorithmFactory创建新算法实例 - 算法: {}", newAlgorithm.getAlgorithmName());
            return newAlgorithm;
        }
        
        // Fallback：如果工厂不可用，使用默认方式（会有共享问题）
        log.warn("AlgorithmFactory不可用，使用默认方式创建算法实例（底层算法可能共享状态）");
        if (renoAlgorithm != null && vegasAlgorithm != null 
            && cubicAlgorithm != null && bbrAlgorithm != null) {
            AdaptiveAlgorithm newAdaptive = new AdaptiveAlgorithm(
                renoAlgorithm, vegasAlgorithm, cubicAlgorithm, bbrAlgorithm
            );
            newAdaptive.initialize();
            log.warn("创建AdaptiveAlgorithm实例 - 警告：底层算法是共享的单例，任务之间可能互相干扰");
            return newAdaptive;
        }
        
        // 最后的fallback：使用CUBIC算法单例（不推荐）
        log.error("无法创建新的算法实例，使用默认CUBIC算法单例（所有任务共享状态）");
        if (cubicAlgorithm != null) {
            return cubicAlgorithm;
        }
        
        // 如果连CUBIC都没有，抛出异常
        throw new IllegalStateException("没有可用的拥塞控制算法实例");
    }
}
