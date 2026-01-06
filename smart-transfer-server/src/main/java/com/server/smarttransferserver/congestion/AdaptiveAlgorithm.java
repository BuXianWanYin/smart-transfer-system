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
 * 根据网络质量动态选择CUBIC或BBR
 */
@Slf4j
@Component
public class AdaptiveAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * 评估窗口大小（固定值）
     */
    private static final int EVALUATION_WINDOW = 100;
    
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
     * 拥塞控制配置
     */
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 构造方法
     */
    @Autowired
    public AdaptiveAlgorithm(CubicAlgorithm cubicAlgorithm, BBRAlgorithm bbrAlgorithm) {
        this.cubicAlgorithm = cubicAlgorithm;
        this.bbrAlgorithm = bbrAlgorithm;
        this.rttSamples = new LinkedList<>();
    }
    
    @PostConstruct
    @Override
    public void initialize() {
        // 初始化两个算法
        cubicAlgorithm.initialize();
        bbrAlgorithm.initialize();
        
        // 默认使用BBR（适合高质量网络）
        currentAlgorithm = bbrAlgorithm;
        
        // 重置统计
        totalPackets = 0;
        lostPackets = 0;
        rttSamples.clear();
        lastEvaluationTime = System.currentTimeMillis();
        
        log.info("自适应算法初始化 - 默认使用BBR");
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
        
        // 判断网络质量（从配置获取阈值）
        boolean isGoodNetwork = (lossRate < congestionConfig.getLossRateThreshold()) 
                && (rttJitter < congestionConfig.getRttJitterThreshold());
        
        // 根据网络质量选择算法
        String previousAlgorithm = currentAlgorithm.getAlgorithmName();
        
        if (isGoodNetwork && currentAlgorithm != bbrAlgorithm) {
            // 好网络，切换到BBR（更激进，更高吞吐量）
            currentAlgorithm = bbrAlgorithm;
            log.info("网络质量好 - 切换到BBR算法 (丢包率: {:.2f}%, RTT抖动: {}ms)", 
                     lossRate * 100, rttJitter);
        } else if (!isGoodNetwork && currentAlgorithm != cubicAlgorithm) {
            // 差网络，切换到CUBIC（更保守，更稳定）
            currentAlgorithm = cubicAlgorithm;
            log.info("网络质量差 - 切换到CUBIC算法 (丢包率: {:.2f}%, RTT抖动: {}ms)", 
                     lossRate * 100, rttJitter);
        }
        
        // 重置统计
        if (!previousAlgorithm.equals(currentAlgorithm.getAlgorithmName())) {
            totalPackets = 0;
            lostPackets = 0;
        }
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
        cubicAlgorithm.reset();
        bbrAlgorithm.reset();
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

