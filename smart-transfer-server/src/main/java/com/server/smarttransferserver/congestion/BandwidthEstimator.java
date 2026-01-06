package com.server.smarttransferserver.congestion;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 带宽估算器
 * 用于估算网络可用带宽
 */
@Slf4j
@Component
public class BandwidthEstimator {
    
    /**
     * 样本窗口大小
     */
    private static final int SAMPLE_WINDOW = 50;
    
    /**
     * 带宽样本队列
     */
    private final Queue<BandwidthSample> bandwidthSamples;
    
    /**
     * 上次采样时间
     */
    private long lastSampleTime;
    
    /**
     * 上次采样的累计字节数
     */
    private long lastSampleBytes;
    
    /**
     * 累计发送字节数
     */
    private long totalSentBytes;
    
    /**
     * 当前估算带宽（字节/秒）
     */
    private long estimatedBandwidth;
    
    /**
     * 最大带宽
     */
    private long maxBandwidth;
    
    /**
     * 最小带宽
     */
    private long minBandwidth;
    
    /**
     * 构造方法
     */
    public BandwidthEstimator() {
        this.bandwidthSamples = new LinkedList<>();
        this.lastSampleTime = System.currentTimeMillis();
        this.lastSampleBytes = 0;
        this.totalSentBytes = 0;
        this.estimatedBandwidth = 0;
        this.maxBandwidth = 0;
        this.minBandwidth = Long.MAX_VALUE;
    }
    
    /**
     * 记录发送的字节数
     *
     * @param bytes 字节数
     */
    public void recordSent(long bytes) {
        totalSentBytes += bytes;
        
        long now = System.currentTimeMillis();
        long timeDelta = now - lastSampleTime;
        
        // 每100ms采样一次
        if (timeDelta >= 100) {
            long bytesDelta = totalSentBytes - lastSampleBytes;
            long bandwidth = (bytesDelta * 1000) / timeDelta; // 字节/秒
            
            addSample(bandwidth);
            
            lastSampleTime = now;
            lastSampleBytes = totalSentBytes;
        }
    }
    
    /**
     * 添加带宽样本
     *
     * @param bandwidth 带宽值
     */
    private void addSample(long bandwidth) {
        BandwidthSample sample = new BandwidthSample();
        sample.setBandwidth(bandwidth);
        sample.setTimestamp(System.currentTimeMillis());
        
        bandwidthSamples.offer(sample);
        if (bandwidthSamples.size() > SAMPLE_WINDOW) {
            bandwidthSamples.poll();
        }
        
        // 更新最大、最小带宽
        maxBandwidth = Math.max(maxBandwidth, bandwidth);
        if (bandwidth > 0) {
            minBandwidth = Math.min(minBandwidth, bandwidth);
        }
        
        // 计算估算带宽（使用指数加权移动平均）
        updateEstimatedBandwidth();
        
        log.debug("带宽估算 - 当前带宽: {}字节/秒, 估算带宽: {}字节/秒", 
                  bandwidth, estimatedBandwidth);
    }
    
    /**
     * 更新估算带宽（使用EWMA算法）
     * EWMA = α * 当前值 + (1 - α) * 上次EWMA
     * α = 0.2
     */
    private void updateEstimatedBandwidth() {
        if (bandwidthSamples.isEmpty()) {
            return;
        }
        
        // 计算最近的平均带宽
        long recentAvg = (long) bandwidthSamples.stream()
                .mapToLong(BandwidthSample::getBandwidth)
                .average()
                .orElse(0);
        
        if (estimatedBandwidth == 0) {
            estimatedBandwidth = recentAvg;
        } else {
            // EWMA
            estimatedBandwidth = (long) (0.2 * recentAvg + 0.8 * estimatedBandwidth);
        }
    }
    
    /**
     * 获取估算带宽
     *
     * @return 估算带宽（字节/秒）
     */
    public long getEstimatedBandwidth() {
        return estimatedBandwidth;
    }
    
    /**
     * 获取最大带宽
     *
     * @return 最大带宽
     */
    public long getMaxBandwidth() {
        return maxBandwidth;
    }
    
    /**
     * 获取最小带宽
     *
     * @return 最小带宽
     */
    public long getMinBandwidth() {
        return minBandwidth != Long.MAX_VALUE ? minBandwidth : 0;
    }
    
    /**
     * 获取平均带宽
     *
     * @return 平均带宽
     */
    public long getAverageBandwidth() {
        if (bandwidthSamples.isEmpty()) {
            return 0;
        }
        return (long) bandwidthSamples.stream()
                .mapToLong(BandwidthSample::getBandwidth)
                .average()
                .orElse(0);
    }
    
    /**
     * 获取带宽利用率
     * 利用率 = 当前带宽 / 最大带宽
     *
     * @return 利用率（0-1）
     */
    public double getUtilization() {
        if (maxBandwidth == 0) {
            return 0;
        }
        return (double) estimatedBandwidth / maxBandwidth;
    }
    
    /**
     * 重置统计
     */
    public void reset() {
        bandwidthSamples.clear();
        lastSampleTime = System.currentTimeMillis();
        lastSampleBytes = 0;
        totalSentBytes = 0;
        estimatedBandwidth = 0;
        maxBandwidth = 0;
        minBandwidth = Long.MAX_VALUE;
    }
    
    /**
     * 获取带宽统计信息
     *
     * @return 带宽统计
     */
    public BandwidthStats getStats() {
        BandwidthStats stats = new BandwidthStats();
        stats.setEstimatedBandwidth(getEstimatedBandwidth());
        stats.setMaxBandwidth(getMaxBandwidth());
        stats.setMinBandwidth(getMinBandwidth());
        stats.setAverageBandwidth(getAverageBandwidth());
        stats.setUtilization(getUtilization());
        stats.setTotalSentBytes(totalSentBytes);
        stats.setSampleCount(bandwidthSamples.size());
        return stats;
    }
    
    /**
     * 带宽样本
     */
    @Data
    private static class BandwidthSample {
        private long bandwidth;
        private long timestamp;
    }
    
    /**
     * 带宽统计信息
     */
    @Data
    public static class BandwidthStats {
        private long estimatedBandwidth;
        private long maxBandwidth;
        private long minBandwidth;
        private long averageBandwidth;
        private double utilization;
        private long totalSentBytes;
        private int sampleCount;
    }
}

