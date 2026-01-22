package com.server.smarttransferserver.service;

import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;

/**
 * 拥塞控制算法管理器
 * 为每个传输任务维护独立的算法实例
 */
public interface CongestionAlgorithmManager {
    
    /**
     * 为任务创建或获取算法实例
     * 如果任务已存在算法实例，返回现有实例；否则创建新实例
     *
     * @param taskId 任务ID
     * @return 算法实例
     */
    CongestionControlAlgorithm getOrCreateAlgorithm(String taskId);
    
    /**
     * 获取任务对应的算法实例
     *
     * @param taskId 任务ID
     * @return 算法实例，如果不存在则返回null
     */
    CongestionControlAlgorithm getAlgorithm(String taskId);
    
    /**
     * 移除任务的算法实例（任务完成或失败时调用）
     *
     * @param taskId 任务ID
     */
    void removeAlgorithm(String taskId);
    
    /**
     * 移除所有算法实例（清理资源）
     */
    void clearAll();
}
