package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.congestion.*;
import com.server.smarttransferserver.entity.CongestionMetrics;
import com.server.smarttransferserver.mapper.CongestionMetricsMapper;
import com.server.smarttransferserver.service.CongestionMetricsService;
import com.server.smarttransferserver.service.INetworkMonitorService;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 拥塞指标服务实现
 */
@Slf4j
@Service
public class CongestionMetricsServiceImpl extends ServiceImpl<CongestionMetricsMapper, CongestionMetrics> implements CongestionMetricsService {
    
    @Autowired
    private CongestionMetricsMapper metricsMapper;
    
    @Autowired(required = false)
    private INetworkMonitorService networkMonitor;
    
    /**
     * 获取当前拥塞控制指标
     *
     * @param algorithm 当前使用的算法
     * @return 指标VO
     */
    @Override
    public CongestionMetricsVO getCurrentMetrics(CongestionControlAlgorithm algorithm) {
        if (algorithm == null || networkMonitor == null) {
            return buildEmptyMetrics();
        }
        
        // 评估网络质量，如果没有数据则返回null（前端显示"-"）
        NetworkMonitorServiceImpl.NetworkQuality quality = networkMonitor.evaluateNetworkQuality();
        String qualityDesc = quality != null ? quality.getDescription() : "-";
        
        CongestionMetricsVO vo = CongestionMetricsVO.builder()
                .algorithm(algorithm.getAlgorithmName())
                .cwnd(algorithm.getCwnd())
                .ssthresh(algorithm.getSsthresh())
                .rate(algorithm.getRate())
                .state(algorithm.getState().getDescription())
                .rtt(algorithm.getRtt())
                .minRtt(networkMonitor.getMinRtt())
                .lossRate(networkMonitor.getLossRate())
                .bandwidth(networkMonitor.getEstimatedBandwidth())
                .networkQuality(qualityDesc)
                .inflightCount(networkMonitor.getInflightCount())
                .inflightBytes(networkMonitor.getInflightBytes())
                .build();
        
        // 高频调用，使用trace级别避免日志过多
        log.trace("获取当前拥塞指标 - 算法: {}, cwnd: {}, rate: {}", 
                  vo.getAlgorithm(), vo.getCwnd(), vo.getRate());
        
        return vo;
    }
    
    /**
     * 记录拥塞指标
     *
     * @param metrics 拥塞指标实体
     * @return 是否记录成功
     */
    @Override
    public boolean recordMetrics(CongestionMetrics metrics) {
        return save(metrics);
    }
    
    /**
     * 根据任务ID查询最新的拥塞指标
     *
     * @param taskId 任务ID（UUID字符串）
     * @param limit 查询数量
     * @return 拥塞指标列表
     */
    @Override
    public List<CongestionMetricsVO> getLatestMetrics(String taskId, Integer limit) {
        List<CongestionMetrics> metricsList = metricsMapper.selectByTaskIdOrderByRecordTimeDesc(taskId);
        
        return metricsList.stream()
                .limit(limit != null ? limit : 100)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据任务ID查询所有拥塞指标
     *
     * @param taskId 任务ID
     * @return 拥塞指标列表
     */
    @Override
    public List<CongestionMetricsVO> getMetricsByTaskId(String taskId) {
        return getLatestMetrics(taskId, null);
    }
    
    /**
     * 记录拥塞指标到数据库（兼容旧方法）
     *
     * @param taskId    任务ID（UUID字符串）
     * @param algorithm 算法
     */
    public void recordMetrics(String taskId, CongestionControlAlgorithm algorithm) {
        if (algorithm == null || networkMonitor == null) {
            return;
        }
        
        CongestionMetrics metrics = CongestionMetrics.builder()
                .taskId(taskId)
                .algorithm(algorithm.getAlgorithmName())
                .cwnd(algorithm.getCwnd())
                .ssthresh(algorithm.getSsthresh())
                .rtt(algorithm.getRtt())
                .bandwidth(networkMonitor.getEstimatedBandwidth())
                .lossRate(BigDecimal.valueOf(networkMonitor.getLossRate()))
                .recordTime(LocalDateTime.now())
                .build();
        
        recordMetrics(metrics);
        
        log.debug("记录拥塞指标 - 任务ID: {}, 算法: {}", taskId, algorithm.getAlgorithmName());
    }
    
    /**
     * 查询任务的历史指标
     *
     * @param taskId 任务ID（UUID字符串）
     * @return 指标列表
     */
    public List<CongestionMetrics> getTaskMetrics(String taskId) {
        return metricsMapper.selectByTaskIdOrderByRecordTimeDesc(taskId);
    }
    
    /**
     * 聚合多个任务的监控数据
     *
     * @param taskIds 任务ID列表
     * @return 聚合后的监控指标
     */
    @Override
    public CongestionMetricsVO aggregateMetricsByTaskIds(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return buildEmptyMetrics();
        }
        
        List<CongestionMetricsVO> allMetrics = new ArrayList<>();
        
        // 获取每个任务的最新指标
        for (String taskId : taskIds) {
            List<CongestionMetricsVO> taskMetrics = getLatestMetrics(taskId, 1);
            if (!taskMetrics.isEmpty()) {
                allMetrics.add(taskMetrics.get(0)); // 取最新的一条
            }
        }
        
        if (allMetrics.isEmpty()) {
            return buildEmptyMetrics();
        }
        
        // 聚合指标：计算平均值
        return aggregateMetrics(allMetrics);
    }
    
    /**
     * 聚合多个指标数据
     *
     * @param metricsList 指标列表
     * @return 聚合后的指标
     */
    private CongestionMetricsVO aggregateMetrics(List<CongestionMetricsVO> metricsList) {
        if (metricsList.isEmpty()) {
            return buildEmptyMetrics();
        }
        
        // 计算平均值
        long avgCwnd = (long) metricsList.stream()
                .mapToLong(m -> m.getCwnd() != null ? m.getCwnd() : 0L)
                .average()
                .orElse(0.0);
        
        long avgSsthresh = (long) metricsList.stream()
                .mapToLong(m -> m.getSsthresh() != null ? m.getSsthresh() : 0L)
                .average()
                .orElse(0.0);
        
        long avgRate = (long) metricsList.stream()
                .mapToLong(m -> m.getRate() != null ? m.getRate() : 0L)
                .average()
                .orElse(0.0);
        
        long avgRtt = (long) metricsList.stream()
                .mapToLong(m -> m.getRtt() != null ? m.getRtt() : 0L)
                .average()
                .orElse(0.0);
        
        long avgMinRtt = (long) metricsList.stream()
                .mapToLong(m -> m.getMinRtt() != null ? m.getMinRtt() : 0L)
                .average()
                .orElse(0.0);
        
        double avgLossRate = metricsList.stream()
                .mapToDouble(m -> m.getLossRate() != null ? m.getLossRate() : 0.0)
                .average()
                .orElse(0.0);
        
        long avgBandwidth = (long) metricsList.stream()
                .mapToLong(m -> m.getBandwidth() != null ? m.getBandwidth() : 0L)
                .average()
                .orElse(0.0);
        
        // 使用第一个任务的算法名称
        String algorithm = metricsList.get(0).getAlgorithm();
        
        return CongestionMetricsVO.builder()
                .algorithm(algorithm)
                .cwnd(avgCwnd)
                .ssthresh(avgSsthresh)
                .rate(avgRate)
                .rtt(avgRtt)
                .minRtt(avgMinRtt)
                .lossRate(avgLossRate)
                .bandwidth(avgBandwidth)
                .networkQuality("NORMAL") // 聚合数据不评估网络质量
                .build();
    }
    
    /**
     * 转换为VO
     *
     * @param metrics 指标实体
     * @return 指标VO
     */
    private CongestionMetricsVO convertToVO(CongestionMetrics metrics) {
        CongestionMetricsVO vo = new CongestionMetricsVO();
        BeanUtils.copyProperties(metrics, vo);
        return vo;
    }
    
    /**
     * 构建空指标
     *
     * @return 空指标VO
     */
    private CongestionMetricsVO buildEmptyMetrics() {
        return CongestionMetricsVO.builder()
                .algorithm("NONE")
                .cwnd(0L)
                .ssthresh(0L)
                .rate(0L)
                .state("未初始化")
                .rtt(0L)
                .minRtt(0L)
                .lossRate(0.0)
                .bandwidth(0L)
                .networkQuality("-")
                .inflightCount(0)
                .inflightBytes(0L)
                .build();
    }
}

