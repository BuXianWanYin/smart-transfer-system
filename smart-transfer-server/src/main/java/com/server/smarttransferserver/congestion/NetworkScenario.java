package com.server.smarttransferserver.congestion;

import lombok.Getter;

/**
 * 网络场景枚举
 * 用于动态权重决策矩阵
 */
@Getter
public enum NetworkScenario {
    
    /**
     * 优秀网络
     * 丢包率<0.5%，RTT抖动<25ms
     */
    EXCELLENT("优秀", 0.3, 0.25, 0.25, 0.2),
    
    /**
     * 良好网络
     * 丢包率<1%，RTT抖动<50ms
     */
    GOOD("良好", 0.4, 0.3, 0.3, 0.0),
    
    /**
     * 一般网络
     * 丢包率<3%，RTT抖动<100ms
     */
    FAIR("一般", 0.45, 0.3, 0.25, 0.0),
    
    /**
     * 差网络
     * 丢包率>3%或RTT抖动>100ms
     */
    POOR("差", 0.5, 0.3, 0.2, 0.0);
    
    private final String description;
    
    /**
     * 丢包率权重
     */
    private final double lossRateWeight;
    
    /**
     * RTT抖动权重
     */
    private final double rttJitterWeight;
    
    /**
     * RTT权重
     */
    private final double rttWeight;
    
    /**
     * 吞吐量权重（仅优秀网络使用）
     */
    private final double throughputWeight;
    
    NetworkScenario(String description, double lossRateWeight, double rttJitterWeight, 
                    double rttWeight, double throughputWeight) {
        this.description = description;
        this.lossRateWeight = lossRateWeight;
        this.rttJitterWeight = rttJitterWeight;
        this.rttWeight = rttWeight;
        this.throughputWeight = throughputWeight;
    }
    
    /**
     * 根据网络指标判断场景
     *
     * @param lossRate 丢包率
     * @param rttJitter RTT抖动
     * @return 网络场景
     */
    public static NetworkScenario fromMetrics(double lossRate, long rttJitter) {
        // 优秀网络：丢包率<0.5%，RTT抖动<25ms
        if (lossRate < 0.005 && rttJitter < 25) {
            return EXCELLENT;
        }
        // 良好网络：丢包率<1%，RTT抖动<50ms
        if (lossRate < 0.01 && rttJitter < 50) {
            return GOOD;
        }
        // 一般网络：丢包率<3%，RTT抖动<100ms
        if (lossRate < 0.03 && rttJitter < 100) {
            return FAIR;
        }
        // 差网络：其他情况
        return POOR;
    }
}
