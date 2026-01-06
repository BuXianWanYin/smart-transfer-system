package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.congestion.*;
import com.server.smarttransferserver.entity.CongestionMetrics;
import com.server.smarttransferserver.mapper.CongestionMetricsMapper;
import com.server.smarttransferserver.vo.CongestionMetricsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拥塞指标服务实现
 */
@Slf4j
@Service
public class CongestionMetricsServiceImpl {
    
    @Autowired
    private CongestionMetricsMapper metricsMapper;
    
    @Autowired(required = false)
    private NetworkMonitorService networkMonitor;
    
    @Autowired(required = false)
    private TransferRateController rateController;
    
    /**
     * 获取当前拥塞控制指标
     *
     * @param algorithm 当前使用的算法
     * @return 指标VO
     */
    public CongestionMetricsVO getCurrentMetrics(CongestionControlAlgorithm algorithm) {
        if (algorithm == null || networkMonitor == null || rateController == null) {
            return buildEmptyMetrics();
        }
        
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
                .networkQuality(networkMonitor.evaluateNetworkQuality().getDescription())
                .inflightCount(networkMonitor.getInflightCount())
                .inflightBytes(networkMonitor.getInflightBytes())
                .build();
        
        log.debug("获取当前拥塞指标 - 算法: {}, cwnd: {}, rate: {}", 
                  vo.getAlgorithm(), vo.getCwnd(), vo.getRate());
        
        return vo;
    }
    
    /**
     * 记录拥塞指标到数据库
     *
     * @param taskId    任务ID
     * @param algorithm 算法
     */
    public void recordMetrics(Long taskId, CongestionControlAlgorithm algorithm) {
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
        
        metricsMapper.insert(metrics);
        
        log.debug("记录拥塞指标 - 任务ID: {}, 算法: {}", taskId, algorithm.getAlgorithmName());
    }
    
    /**
     * 查询任务的历史指标
     *
     * @param taskId 任务ID
     * @return 指标列表
     */
    public List<CongestionMetrics> getTaskMetrics(Long taskId) {
        return metricsMapper.selectByTaskIdOrderByRecordTimeDesc(taskId);
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
                .networkQuality("未知")
                .inflightCount(0)
                .inflightBytes(0L)
                .build();
    }
}

