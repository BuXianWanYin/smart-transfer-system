package com.server.smarttransferserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.congestion.CongestionControlAlgorithm;
import com.server.smarttransferserver.entity.CongestionMetrics;
import com.server.smarttransferserver.vo.CongestionMetricsVO;

import java.util.List;
import java.util.Map;

/**
 * 拥塞指标服务接口
 * 提供拥塞指标的记录、查询等功能
 */
public interface CongestionMetricsService extends IService<CongestionMetrics> {
    
    /**
     * 获取当前拥塞控制指标
     *
     * @param algorithm 当前使用的算法
     * @return 指标VO
     */
    CongestionMetricsVO getCurrentMetrics(CongestionControlAlgorithm algorithm);
    
    /**
     * 获取当前拥塞控制指标（自动获取当前算法）
     * 
     * @return 指标VO
     */
    CongestionMetricsVO getCurrentMetrics();
    
    /**
     * 记录拥塞指标
     *
     * @param metrics 拥塞指标实体
     * @return 是否记录成功
     */
    boolean recordMetrics(CongestionMetrics metrics);
    
    /**
     * 根据任务ID查询最新的拥塞指标
     *
     * @param taskId 任务ID
     * @param limit 查询数量
     * @return 拥塞指标列表
     */
    List<CongestionMetricsVO> getLatestMetrics(String taskId, Integer limit);
    
    /**
     * 根据任务ID查询所有拥塞指标
     *
     * @param taskId 任务ID
     * @return 拥塞指标列表
     */
    List<CongestionMetricsVO> getMetricsByTaskId(String taskId);
    
    /**
     * 聚合多个任务的监控数据
     * 对所有任务的指标进行平均或求和
     *
     * @param taskIds 任务ID列表
     * @return 聚合后的监控指标
     */
    CongestionMetricsVO aggregateMetricsByTaskIds(List<String> taskIds);
    
    /**
     * 获取网络质量统计
     * 从最近的拥塞指标中统计网络质量分布
     *
     * @param algorithm 当前使用的算法（可选，如果为null则使用默认算法）
     * @return 网络质量统计数据
     */
    Map<String, Object> getNetworkQualityStats(CongestionControlAlgorithm algorithm);
}

