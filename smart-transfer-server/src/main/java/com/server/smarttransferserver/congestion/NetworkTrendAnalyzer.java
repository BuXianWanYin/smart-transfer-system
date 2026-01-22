package com.server.smarttransferserver.congestion;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 网络趋势分析器
 * 维护最近N个评估窗口的指标历史，计算变化趋势
 */
@Slf4j
public class NetworkTrendAnalyzer {
    
    /**
     * 历史窗口数据队列
     */
    private final Queue<NetworkMetrics> historyWindows;
    
    /**
     * 窗口大小（默认5个评估窗口）
     */
    private final int windowSize;
    
    /**
     * 趋势变化率阈值（默认10%）
     */
    private final double trendThreshold;
    
    /**
     * 网络指标数据
     */
    @Data
    public static class NetworkMetrics {
        private double lossRate;
        private long rttJitter;
        private double avgRtt;
        private long timestamp;
        
        public NetworkMetrics(double lossRate, long rttJitter, double avgRtt) {
            this.lossRate = lossRate;
            this.rttJitter = rttJitter;
            this.avgRtt = avgRtt;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 趋势方向
     */
    public enum TrendDirection {
        RISING,      // 上升
        FALLING,     // 下降
        STABLE       // 平稳
    }
    
    /**
     * 构造方法
     *
     * @param windowSize 窗口大小
     * @param trendThreshold 趋势阈值
     */
    public NetworkTrendAnalyzer(int windowSize, double trendThreshold) {
        this.windowSize = windowSize;
        this.trendThreshold = trendThreshold;
        this.historyWindows = new LinkedList<>();
    }
    
    /**
     * 添加新的评估窗口数据
     *
     * @param lossRate 丢包率
     * @param rttJitter RTT抖动
     * @param avgRtt 平均RTT
     */
    public void addWindow(double lossRate, long rttJitter, double avgRtt) {
        NetworkMetrics metrics = new NetworkMetrics(lossRate, rttJitter, avgRtt);
        historyWindows.offer(metrics);
        
        // 保持窗口大小
        if (historyWindows.size() > windowSize) {
            historyWindows.poll();
        }
        
        log.debug("添加网络指标窗口 - 丢包率: {}%, RTT抖动: {}ms, 平均RTT: {}ms, 窗口数: {}", 
                 String.format("%.2f", lossRate * 100), rttJitter, String.format("%.2f", avgRtt), 
                 historyWindows.size());
    }
    
    /**
     * 分析丢包率趋势
     *
     * @return 趋势方向
     */
    public TrendDirection analyzeLossRateTrend() {
        if (historyWindows.size() < 2) {
            return TrendDirection.STABLE;
        }
        
        NetworkMetrics[] metrics = historyWindows.toArray(new NetworkMetrics[0]);
        double first = metrics[0].getLossRate();
        double last = metrics[metrics.length - 1].getLossRate();
        
        double changeRate = first > 0 ? (last - first) / first : 0;
        
        if (changeRate > trendThreshold) {
            return TrendDirection.RISING;
        } else if (changeRate < -trendThreshold) {
            return TrendDirection.FALLING;
        } else {
            return TrendDirection.STABLE;
        }
    }
    
    /**
     * 分析RTT趋势
     *
     * @return 趋势方向
     */
    public TrendDirection analyzeRttTrend() {
        if (historyWindows.size() < 2) {
            return TrendDirection.STABLE;
        }
        
        NetworkMetrics[] metrics = historyWindows.toArray(new NetworkMetrics[0]);
        double first = metrics[0].getAvgRtt();
        double last = metrics[metrics.length - 1].getAvgRtt();
        
        double changeRate = first > 0 ? (last - first) / first : 0;
        
        if (changeRate > trendThreshold) {
            return TrendDirection.RISING;
        } else if (changeRate < -trendThreshold) {
            return TrendDirection.FALLING;
        } else {
            return TrendDirection.STABLE;
        }
    }
    
    /**
     * 分析RTT抖动趋势
     *
     * @return 趋势方向
     */
    public TrendDirection analyzeRttJitterTrend() {
        if (historyWindows.size() < 2) {
            return TrendDirection.STABLE;
        }
        
        NetworkMetrics[] metrics = historyWindows.toArray(new NetworkMetrics[0]);
        long first = metrics[0].getRttJitter();
        long last = metrics[metrics.length - 1].getRttJitter();
        
        double changeRate = first > 0 ? (double) (last - first) / first : 0;
        
        if (changeRate > trendThreshold) {
            return TrendDirection.RISING;
        } else if (changeRate < -trendThreshold) {
            return TrendDirection.FALLING;
        } else {
            return TrendDirection.STABLE;
        }
    }
    
    /**
     * 重置历史数据
     */
    public void reset() {
        historyWindows.clear();
        log.debug("网络趋势分析器已重置");
    }
    
    /**
     * 获取历史窗口数量
     *
     * @return 窗口数量
     */
    public int getWindowCount() {
        return historyWindows.size();
    }
}
