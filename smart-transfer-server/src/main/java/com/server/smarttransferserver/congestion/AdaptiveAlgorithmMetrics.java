package com.server.smarttransferserver.congestion;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自适应算法运行指标
 * 用于可观测性和监控
 */
@Data
public class AdaptiveAlgorithmMetrics {
    
    /**
     * 当前算法名称
     */
    private String currentAlgorithm;
    
    /**
     * 各算法得分
     */
    private Map<String, Double> algorithmScores;
    
    /**
     * 网络质量等级
     */
    private String networkQuality;
    
    /**
     * 算法切换历史
     */
    private List<AlgorithmSwitchRecord> switchHistory;
    
    /**
     * 当前网络指标
     */
    private NetworkMetrics currentMetrics;
    
    /**
     * 算法切换记录
     */
    @Data
    public static class AlgorithmSwitchRecord {
        private long timestamp;
        private String fromAlgorithm;
        private String toAlgorithm;
        private String reason;
        private double beforeScore;
        private double afterScore;
        private double lossRate;
        private long rttJitter;
        private double avgRtt;
        
        public AlgorithmSwitchRecord(String fromAlgorithm, String toAlgorithm, String reason,
                                    double beforeScore, double afterScore, double lossRate,
                                    long rttJitter, double avgRtt) {
            this.timestamp = System.currentTimeMillis();
            this.fromAlgorithm = fromAlgorithm;
            this.toAlgorithm = toAlgorithm;
            this.reason = reason;
            this.beforeScore = beforeScore;
            this.afterScore = afterScore;
            this.lossRate = lossRate;
            this.rttJitter = rttJitter;
            this.avgRtt = avgRtt;
        }
    }
    
    /**
     * 网络指标
     */
    @Data
    public static class NetworkMetrics {
        private double lossRate;
        private long rttJitter;
        private double avgRtt;
        private double rttVariation;
        private long bandwidth;
        private long bdp;  // 带宽时延积
        
        public NetworkMetrics(double lossRate, long rttJitter, double avgRtt, 
                             double rttVariation, long bandwidth, long bdp) {
            this.lossRate = lossRate;
            this.rttJitter = rttJitter;
            this.avgRtt = avgRtt;
            this.rttVariation = rttVariation;
            this.bandwidth = bandwidth;
            this.bdp = bdp;
        }
    }
    
    /**
     * 构造方法
     */
    public AdaptiveAlgorithmMetrics() {
        this.algorithmScores = new HashMap<>();
        this.switchHistory = new ArrayList<>();
    }
    
    /**
     * 添加切换记录
     *
     * @param record 切换记录
     */
    public void addSwitchRecord(AlgorithmSwitchRecord record) {
        switchHistory.add(record);
        
        // 保持最近50条记录
        if (switchHistory.size() > 50) {
            switchHistory.remove(0);
        }
    }
    
    /**
     * 获取最近N条切换记录
     *
     * @param count 记录数量
     * @return 切换记录列表
     */
    public List<AlgorithmSwitchRecord> getRecentSwitchHistory(int count) {
        int size = switchHistory.size();
        if (size <= count) {
            return new ArrayList<>(switchHistory);
        }
        return new ArrayList<>(switchHistory.subList(size - count, size));
    }
}
