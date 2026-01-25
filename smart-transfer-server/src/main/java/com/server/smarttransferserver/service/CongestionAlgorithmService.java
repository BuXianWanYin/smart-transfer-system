package com.server.smarttransferserver.service;

import com.server.smarttransferserver.congestion.AdaptiveAlgorithm;
import com.server.smarttransferserver.congestion.AdaptiveAlgorithmMetrics;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;

import java.util.Map;

/**
 * 拥塞控制算法服务接口
 * 处理算法切换、状态查询等业务逻辑
 */
public interface CongestionAlgorithmService {
    
    /**
     * 切换拥塞控制算法
     * @param algorithm 算法名称（RENO, VEGAS, CUBIC, BBR, ADAPTIVE）
     * @return 切换后的算法实例
     */
    CongestionControlAlgorithm switchAlgorithm(String algorithm);
    
    /**
     * 获取当前使用的算法
     * @return 算法实例，如果未初始化返回null
     */
    CongestionControlAlgorithm getCurrentAlgorithm();
    
    /**
     * 获取当前算法名称
     * @return 算法名称，如果未初始化返回"NONE"
     */
    String getCurrentAlgorithmName();
    
    /**
     * 获取算法状态详情
     * @return 状态详情字符串
     */
    String getAlgorithmStatus();
    
    /**
     * 获取自适应算法详细指标
     * @return 自适应算法指标
     */
    AdaptiveAlgorithmMetrics getAdaptiveMetrics();
}
