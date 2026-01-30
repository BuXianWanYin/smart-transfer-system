package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.congestion.*;
import com.server.smarttransferserver.entity.CongestionMetrics;
import com.server.smarttransferserver.mapper.CongestionMetricsMapper;
import com.server.smarttransferserver.service.CongestionAlgorithmService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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
    
    @Autowired(required = false)
    private CongestionAlgorithmService algorithmService;
    
    /** 记录拥塞指标日志采样间隔：每 N 次记录打印一次，减少大量分片时的刷屏 */
    private static final int RECORD_LOG_SAMPLE_INTERVAL = 50;
    
    /** 按任务ID计数的记录次数，用于采样打印日志 */
    private final ConcurrentHashMap<String, AtomicLong> recordCountByTask = new ConcurrentHashMap<>();
    
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
        
        // 评估网络质量：优先用 NetworkMonitor；若为 null 且为自适应算法，则用算法侧的网络质量描述（优秀/良好/一般/差）
        NetworkMonitorServiceImpl.NetworkQuality quality = networkMonitor.evaluateNetworkQuality();
        String qualityDesc = quality != null ? quality.getDescription() : "-";
        if ("-".equals(qualityDesc) && algorithm instanceof AdaptiveAlgorithm) {
            String adaptiveQuality = ((AdaptiveAlgorithm) algorithm).getMetrics().getNetworkQuality();
            if (adaptiveQuality != null && !adaptiveQuality.isEmpty()) {
                qualityDesc = adaptiveQuality;
            }
        }
        
        // 计算RTT抖动
        long rttJitter = 0;
        if (networkMonitor.getStats() != null && networkMonitor.getStats().getRttStats() != null) {
            rttJitter = networkMonitor.getStats().getRttStats().getRttVar();
        }
        
        // 如果是自适应算法，使用其计算的RTT抖动（更准确，包含异常值过滤）
        if (algorithm instanceof AdaptiveAlgorithm) {
            AdaptiveAlgorithm adaptiveAlg = (AdaptiveAlgorithm) algorithm;
            rttJitter = adaptiveAlg.getRttJitter();
        }
        
        // 计算BDP（带宽时延积）
        long bandwidth = networkMonitor.getEstimatedBandwidth();
        long rtt = algorithm.getRtt();
        long bdp = bandwidth > 0 && rtt > 0 ? (bandwidth * rtt / 1000) : 0;
        
        // 获取网络趋势和预热状态（如果是自适应算法）
        String networkTrend = null;
        Boolean isWarmingUp = null;
        double lossRateForVo = networkMonitor.getLossRate();
        if (algorithm instanceof AdaptiveAlgorithm) {
            AdaptiveAlgorithm adaptiveAlg = (AdaptiveAlgorithm) algorithm;
            networkTrend = adaptiveAlg.getNetworkTrend();
            isWarmingUp = adaptiveAlg.isWarmingUp();
            // 丢包率优化：上传场景用自适应算法的滑动窗口丢包率（与算法决策一致，上传有数据）
            lossRateForVo = adaptiveAlg.getCurrentLossRate();
        }
        
        CongestionMetricsVO vo = CongestionMetricsVO.builder()
                .algorithm(algorithm.getAlgorithmName())
                .cwnd(algorithm.getCwnd())
                .ssthresh(algorithm.getSsthresh())
                .rate(algorithm.getRate())
                .state(algorithm.getState().getDescription())
                .rtt(algorithm.getRtt())
                .minRtt(networkMonitor.getMinRtt())
                .lossRate(lossRateForVo)
                .bandwidth(bandwidth)
                .networkQuality(qualityDesc)
                .inflightCount(networkMonitor.getInflightCount())
                .inflightBytes(networkMonitor.getInflightBytes())
                .rttJitter(rttJitter)
                .bdp(bdp)
                .networkTrend(networkTrend)
                .isWarmingUp(isWarmingUp)
                .build();
        
        // 高频调用，使用trace级别避免日志过多
        log.trace("获取当前拥塞指标 - 算法: {}, cwnd: {}, rate: {}", 
                  vo.getAlgorithm(), vo.getCwnd(), vo.getRate());
        
        return vo;
    }
    
    @Override
    public CongestionMetricsVO getCurrentMetrics() {
        if (algorithmService == null) {
            return buildEmptyMetrics();
        }
        CongestionControlAlgorithm currentAlgorithm = algorithmService.getCurrentAlgorithm();
        return getCurrentMetrics(currentAlgorithm);
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
        double lossRateForRecord = networkMonitor.getLossRate();
        if (algorithm instanceof AdaptiveAlgorithm) {
            lossRateForRecord = ((AdaptiveAlgorithm) algorithm).getCurrentLossRate();
        }
        CongestionMetrics metrics = CongestionMetrics.builder()
                .taskId(taskId)
                .algorithm(algorithm.getAlgorithmName())
                .cwnd(algorithm.getCwnd())
                .ssthresh(algorithm.getSsthresh())
                .rtt(algorithm.getRtt())
                .bandwidth(networkMonitor.getEstimatedBandwidth())
                .lossRate(BigDecimal.valueOf(lossRateForRecord))
                .recordTime(LocalDateTime.now())
                .build();
        
        recordMetrics(metrics);
        
        long n = recordCountByTask.computeIfAbsent(taskId, k -> new AtomicLong(0)).incrementAndGet();
        if (n == 1 || n % RECORD_LOG_SAMPLE_INTERVAL == 1) {
            log.debug("记录拥塞指标 - 任务ID: {}, 算法: {}, 第 {} 次", taskId, algorithm.getAlgorithmName(), n);
        }
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
        // 设置taskId
        vo.setTaskId(metrics.getTaskId());
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
                .rttJitter(0L)
                .bdp(0L)
                .networkTrend(null)
                .isWarmingUp(false)
                .build();
    }
    
    /**
     * 获取网络质量统计
     * 从最近的拥塞指标中统计网络质量分布
     *
     * @param algorithm 当前使用的算法（可选，如果为null则使用默认算法）
     * @return 网络质量统计数据
     */
    @Override
    public Map<String, Object> getNetworkQualityStats(CongestionControlAlgorithm algorithm) {
        // 如果未提供算法，尝试从algorithmService获取
        if (algorithm == null && algorithmService != null) {
            algorithm = algorithmService.getCurrentAlgorithm();
        }
        
        // 从最近的指标中统计网络质量
        CongestionMetricsVO metrics = getCurrentMetrics(algorithm);
        
        Map<String, Object> stats = new HashMap<>();
        Map<String, Integer> qualityCount = new HashMap<>();
        
        // 初始化质量计数
        qualityCount.put("EXCELLENT", 0);
        qualityCount.put("GOOD", 0);
        qualityCount.put("FAIR", 0);
        qualityCount.put("POOR", 0);
        qualityCount.put("UNKNOWN", 0);
        
        // 根据当前指标评估网络质量
        if (metrics != null && metrics.getNetworkQuality() != null) {
            String quality = metrics.getNetworkQuality();
            qualityCount.put(quality, qualityCount.getOrDefault(quality, 0) + 1);
        } else {
            qualityCount.put("UNKNOWN", qualityCount.get("UNKNOWN") + 1);
        }
        
        stats.put("qualityCount", qualityCount);
        stats.put("currentQuality", metrics != null ? metrics.getNetworkQuality() : "UNKNOWN");
        stats.put("totalSamples", 1); // 当前只有一个样本，实际应该从历史数据统计
        
        return stats;
    }
}

