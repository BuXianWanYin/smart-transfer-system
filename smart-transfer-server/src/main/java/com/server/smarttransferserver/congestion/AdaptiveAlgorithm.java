package com.server.smarttransferserver.congestion;

import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 自适应拥塞控制算法
 * 根据网络质量动态选择Reno、Vegas、CUBIC或BBR
 */
@Slf4j
@Component
public class AdaptiveAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * 评估窗口大小（固定值）
     */
    private static final int EVALUATION_WINDOW = 100;
    
    /**
     * Reno算法实例
     */
    private final RenoAlgorithm renoAlgorithm;
    
    /**
     * Vegas算法实例
     */
    private final VegasAlgorithm vegasAlgorithm;
    
    /**
     * CUBIC算法实例
     */
    private final CubicAlgorithm cubicAlgorithm;
    
    /**
     * BBR算法实例
     */
    private final BBRAlgorithm bbrAlgorithm;
    
    /**
     * 当前使用的算法
     */
    private CongestionControlAlgorithm currentAlgorithm;
    
    /**
     * 丢包统计
     */
    private long totalPackets;
    private long lostPackets;
    
    /**
     * RTT样本队列
     */
    private final Queue<Long> rttSamples;
    
    /**
     * 上次评估时间
     */
    private long lastEvaluationTime;
    
    /**
     * 上次切换时间（避免频繁切换）
     */
    private long lastSwitchTime;
    
    /**
     * 最小切换间隔（毫秒），默认10秒
     */
    private static final long MIN_SWITCH_INTERVAL = 10000L;
    
    /**
     * 算法性能统计（吞吐量、延迟等）
     * 键：算法名称，值：性能评分（0-100，越高越好）
     */
    private final java.util.Map<String, Double> algorithmScores;
    
    /**
     * 当前算法的连续使用时间（毫秒）
     */
    private long currentAlgorithmStartTime;
    
    /**
     * 拥塞控制配置
     */
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 构造方法
     */
    @Autowired
    public AdaptiveAlgorithm(RenoAlgorithm renoAlgorithm, VegasAlgorithm vegasAlgorithm,
                             CubicAlgorithm cubicAlgorithm, BBRAlgorithm bbrAlgorithm) {
        this.renoAlgorithm = renoAlgorithm;
        this.vegasAlgorithm = vegasAlgorithm;
        this.cubicAlgorithm = cubicAlgorithm;
        this.bbrAlgorithm = bbrAlgorithm;
        this.rttSamples = new LinkedList<>();
        this.algorithmScores = new java.util.HashMap<>();
        this.lastSwitchTime = 0;
        this.currentAlgorithmStartTime = System.currentTimeMillis();
    }
    
    @PostConstruct
    @Override
    public void initialize() {
        // 初始化所有算法
        if (renoAlgorithm != null) {
            renoAlgorithm.initialize();
        }
        if (vegasAlgorithm != null) {
            vegasAlgorithm.initialize();
        }
        if (cubicAlgorithm != null) {
            cubicAlgorithm.initialize();
        }
        if (bbrAlgorithm != null) {
            bbrAlgorithm.initialize();
        }
        
        // 默认使用CUBIC（平衡性能与稳定性）
        currentAlgorithm = cubicAlgorithm != null ? cubicAlgorithm : bbrAlgorithm;
        
        // 重置统计
        totalPackets = 0;
        lostPackets = 0;
        rttSamples.clear();
        lastEvaluationTime = System.currentTimeMillis();
        lastSwitchTime = 0;
        currentAlgorithmStartTime = System.currentTimeMillis();
        algorithmScores.clear();
        
        log.info("自适应算法初始化 - 默认使用: {}", currentAlgorithm.getAlgorithmName());
    }
    
    @Override
    public void onAck(long ackedBytes, long rtt) {
        // 记录RTT样本
        rttSamples.offer(rtt);
        if (rttSamples.size() > EVALUATION_WINDOW) {
            rttSamples.poll();
        }
        
        // 统计数据包
        totalPackets++;
        
        // 调用当前算法
        currentAlgorithm.onAck(ackedBytes, rtt);
        
        // 定期评估是否需要切换算法
        long now = System.currentTimeMillis();
        if (now - lastEvaluationTime > congestionConfig.getEvaluationInterval()) {
            evaluateAndSwitch();
            lastEvaluationTime = now;
        }
        
        log.debug("自适应算法 onAck - 当前使用: {}, cwnd: {}字节", 
                  currentAlgorithm.getAlgorithmName(), currentAlgorithm.getCwnd());
    }
    
    @Override
    public void onLoss(long lostBytes) {
        // 统计丢包
        lostPackets++;
        
        // 调用当前算法
        currentAlgorithm.onLoss(lostBytes);
        
        log.debug("自适应算法检测到丢包 - 当前使用: {}, 丢失{}字节", 
                  currentAlgorithm.getAlgorithmName(), lostBytes);
    }
    
    /**
     * 评估网络质量并决定是否切换算法
     * 根据丢包率、RTT抖动、平均RTT等指标选择最优算法
     */
    private void evaluateAndSwitch() {
        if (totalPackets < 10) {
            // 样本太少，不做评估
            return;
        }
        
        // 计算丢包率
        double lossRate = (double) lostPackets / totalPackets;
        
        // 计算RTT抖动（标准差）
        long rttJitter = calculateRttJitter();
        
        // 计算平均RTT
        double avgRtt = rttSamples.stream().mapToLong(Long::longValue).average().orElse(100);
        
        // 计算RTT变化率
        long minRtt = rttSamples.stream().mapToLong(Long::longValue).min().orElse(100);
        long maxRtt = rttSamples.stream().mapToLong(Long::longValue).max().orElse(100);
        double rttVariation = maxRtt > 0 ? (double) (maxRtt - minRtt) / maxRtt : 0;
        
        // 评估当前算法性能
        evaluateCurrentAlgorithmPerformance(lossRate, rttJitter, avgRtt);
        
        // 根据网络特征选择最优算法
        String previousAlgorithm = currentAlgorithm.getAlgorithmName();
        CongestionControlAlgorithm selectedAlgorithm = selectOptimalAlgorithm(lossRate, rttJitter, avgRtt, rttVariation);
        
        if (selectedAlgorithm != null && selectedAlgorithm != currentAlgorithm) {
            long now = System.currentTimeMillis();
            
            // 避免频繁切换：检查切换间隔
            if (now - lastSwitchTime < MIN_SWITCH_INTERVAL) {
                log.debug("切换间隔过短，暂不切换 - 距离上次切换: {}ms", now - lastSwitchTime);
                return;
            }
            
            // 避免频繁切换：当前算法使用时间过短时不切换（除非性能很差）
            long algorithmUseTime = now - currentAlgorithmStartTime;
            boolean isPerformancePoor = lossRate > congestionConfig.getLossRateThreshold() * 3 
                                     || rttJitter > congestionConfig.getRttJitterThreshold() * 3;
            if (algorithmUseTime < MIN_SWITCH_INTERVAL && !isPerformancePoor) {
                log.debug("算法使用时间过短且性能尚可，暂不切换 - 使用时间: {}ms", algorithmUseTime);
                return;
            }
            
            // 执行切换
            currentAlgorithm = selectedAlgorithm;
            lastSwitchTime = now;
            currentAlgorithmStartTime = now;
            
            log.info("自适应算法切换 - {} → {} (丢包率: {}%, RTT抖动: {}ms, 平均RTT: {}ms, RTT变化率: {})", 
                     previousAlgorithm, currentAlgorithm.getAlgorithmName(), 
                     String.format("%.2f", lossRate * 100), rttJitter, 
                     String.format("%.2f", avgRtt), String.format("%.2f", rttVariation * 100));
            
            // 重置统计
            totalPackets = 0;
            lostPackets = 0;
        }
    }
    
    /**
     * 评估当前算法的性能
     * 基于丢包率、RTT抖动、延迟等指标计算性能评分
     *
     * @param lossRate 丢包率
     * @param rttJitter RTT抖动
     * @param avgRtt 平均RTT
     */
    private void evaluateCurrentAlgorithmPerformance(double lossRate, long rttJitter, double avgRtt) {
        String algorithmName = currentAlgorithm.getAlgorithmName();
        
        // 性能评分计算（0-100分）
        // 丢包率越低越好（权重40%）
        double lossScore = Math.max(0, 100 - lossRate * 10000);
        
        // RTT抖动越低越好（权重30%）
        double jitterScore = Math.max(0, 100 - rttJitter * 2);
        
        // RTT越低越好（权重30%），假设理想RTT为50ms
        double rttScore = Math.max(0, 100 - (avgRtt - 50) / 10);
        
        // 综合评分
        double performanceScore = lossScore * 0.4 + jitterScore * 0.3 + rttScore * 0.3;
        
        // 平滑更新评分（移动平均）
        double previousScore = algorithmScores.getOrDefault(algorithmName, 50.0);
        double smoothedScore = previousScore * 0.7 + performanceScore * 0.3;
        algorithmScores.put(algorithmName, smoothedScore);
        
        log.debug("算法性能评估 - {}: 评分={}, 丢包率={}%, RTT抖动={}ms, 平均RTT={}ms", 
                  algorithmName, String.format("%.2f", smoothedScore),
                  String.format("%.2f", lossRate * 100), rttJitter, String.format("%.2f", avgRtt));
    }
    
    /**
     * 根据网络特征选择最优算法
     * 综合考虑网络质量、算法历史性能和算法特性
     *
     * @param lossRate 丢包率
     * @param rttJitter RTT抖动
     * @param avgRtt 平均RTT
     * @param rttVariation RTT变化率
     * @return 最优算法
     */
    private CongestionControlAlgorithm selectOptimalAlgorithm(double lossRate, long rttJitter, 
                                                              double avgRtt, double rttVariation) {
        // 获取配置阈值
        double lossThreshold = congestionConfig.getLossRateThreshold();
        long jitterThreshold = congestionConfig.getRttJitterThreshold();
        
        // 判断网络质量等级
        boolean isGoodNetwork = lossRate < lossThreshold && rttJitter < jitterThreshold;
        boolean isExcellentNetwork = lossRate < lossThreshold * 0.5 && rttJitter < jitterThreshold * 0.5;
        boolean isPoorNetwork = lossRate > lossThreshold * 2 || rttJitter > jitterThreshold * 2;
        
        // 智能算法选择策略（多维度决策）：
        // 1. 优秀网络（低丢包、低抖动）：优先BBR，但考虑历史性能
        // 2. 良好网络：根据延迟和RTT变化选择Vegas或CUBIC
        // 3. 一般网络：优先CUBIC（稳定可靠）
        // 4. 差网络：使用保守的Reno算法
        // 5. 高延迟、低变化网络：Vegas更适合
        // 6. 考虑算法历史性能评分（如果当前算法表现良好，不轻易切换）
        
        // 获取当前算法性能评分
        double currentScore = algorithmScores.getOrDefault(currentAlgorithm.getAlgorithmName(), 50.0);
        boolean isCurrentPerformingWell = currentScore > 60.0;
        
        // 如果当前算法表现良好，且网络质量没有急剧恶化，保持当前算法
        if (isCurrentPerformingWell && !isPoorNetwork) {
            log.debug("当前算法性能良好（评分: {}），保持使用", String.format("%.2f", currentScore));
            return currentAlgorithm;
        }
        
        // 优秀网络：优先BBR，但考虑历史性能
        if (isExcellentNetwork) {
            if (bbrAlgorithm != null) {
                double bbrScore = algorithmScores.getOrDefault("BBR", 60.0);
                if (bbrScore > 50.0 || !isCurrentPerformingWell) {
                    return bbrAlgorithm;
                }
            }
        }
        
        // 良好网络：根据RTT特征和变化率选择
        if (isGoodNetwork) {
            // 高延迟且RTT变化小的网络：Vegas更适合
            if (avgRtt > 100 && rttVariation < 0.2 && vegasAlgorithm != null) {
                double vegasScore = algorithmScores.getOrDefault("Vegas", 50.0);
                if (vegasScore > currentScore * 0.9) {
                    return vegasAlgorithm;
                }
            }
            // 低延迟或RTT变化大的网络：CUBIC
            if (cubicAlgorithm != null) {
                double cubicScore = algorithmScores.getOrDefault("CUBIC", 50.0);
                if (cubicScore > currentScore * 0.8) {
                    return cubicAlgorithm;
                }
            }
        }
        
        // 差网络：使用保守的Reno算法
        if (isPoorNetwork) {
            if (renoAlgorithm != null) {
                log.info("网络质量差，切换到保守的Reno算法");
                return renoAlgorithm;
            }
        }
        
        // 一般网络：使用CUBIC或Vegas（根据RTT特征）
        if (avgRtt > 100 && vegasAlgorithm != null) {
            double vegasScore = algorithmScores.getOrDefault("Vegas", 50.0);
            if (vegasScore > currentScore * 0.9) {
                return vegasAlgorithm;
            }
        }
        
        if (cubicAlgorithm != null) {
            double cubicScore = algorithmScores.getOrDefault("CUBIC", 50.0);
            if (cubicScore > currentScore * 0.8) {
                return cubicAlgorithm;
            }
        }
        
        // 默认返回当前算法（如果没有找到明显更优的）
        return currentAlgorithm;
    }
    
    /**
     * 计算RTT抖动（标准差）
     *
     * @return RTT抖动值
     */
    private long calculateRttJitter() {
        if (rttSamples.isEmpty()) {
            return 0;
        }
        
        // 计算平均值
        double avg = rttSamples.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // 计算标准差
        double variance = rttSamples.stream()
                .mapToDouble(rtt -> Math.pow(rtt - avg, 2))
                .average()
                .orElse(0);
        
        return (long) Math.sqrt(variance);
    }
    
    @Override
    public long getCwnd() {
        return currentAlgorithm.getCwnd();
    }
    
    @Override
    public long getRate() {
        return currentAlgorithm.getRate();
    }
    
    @Override
    public CongestionState getState() {
        return currentAlgorithm.getState();
    }
    
    @Override
    public String getAlgorithmName() {
        return "Adaptive(" + currentAlgorithm.getAlgorithmName() + ")";
    }
    
    @Override
    public long getSsthresh() {
        return currentAlgorithm.getSsthresh();
    }
    
    @Override
    public long getRtt() {
        return currentAlgorithm.getRtt();
    }
    
    @Override
    public void reset() {
        if (renoAlgorithm != null) {
            renoAlgorithm.reset();
        }
        if (vegasAlgorithm != null) {
            vegasAlgorithm.reset();
        }
        if (cubicAlgorithm != null) {
            cubicAlgorithm.reset();
        }
        if (bbrAlgorithm != null) {
            bbrAlgorithm.reset();
        }
        initialize();
    }
    
    /**
     * 获取当前丢包率
     *
     * @return 丢包率
     */
    public double getLossRate() {
        return totalPackets > 0 ? (double) lostPackets / totalPackets : 0;
    }
    
    /**
     * 获取当前使用的算法名称
     *
     * @return 算法名称
     */
    public String getCurrentAlgorithmName() {
        return currentAlgorithm.getAlgorithmName();
    }
}

