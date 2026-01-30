package com.server.smarttransferserver.congestion;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RTT测量工具
 * 用于测量往返时延
 */
@Slf4j
@Component
public class RttMeasurement {
    
    /**
     * 样本窗口大小
     */
    private static final int SAMPLE_WINDOW = 100;
    
    /**
     * RTT样本队列
     */
    private final Queue<Long> rttSamples;
    
    /**
     * 数据包发送时间映射 <序列号, 发送时间>
     */
    private final ConcurrentHashMap<Long, Long> sendTimeMap;
    
    /**
     * 平滑RTT（SRTT）
     */
    private long smoothedRtt;
    
    /**
     * RTT变化量（RTTVAR）
     */
    private long rttVar;
    
    /**
     * 最小RTT
     */
    private long minRtt;
    
    /**
     * 最大RTT
     */
    private long maxRtt;
    
    /**
     * 构造方法
     */
    public RttMeasurement() {
        this.rttSamples = new LinkedList<>();
        this.sendTimeMap = new ConcurrentHashMap<>();
        this.smoothedRtt = 0;
        this.rttVar = 0;
        this.minRtt = Long.MAX_VALUE;
        this.maxRtt = 0;
    }
    
    /**
     * 记录数据包发送时间
     *
     * @param sequenceNumber 序列号
     */
    public void recordSendTime(long sequenceNumber) {
        sendTimeMap.put(sequenceNumber, System.currentTimeMillis());
    }
    
    /**
     * 记录ACK接收，计算RTT
     *
     * @param sequenceNumber 序列号
     * @return RTT值，如果找不到对应的发送时间则返回-1
     */
    public long recordAck(long sequenceNumber) {
        Long sendTime = sendTimeMap.remove(sequenceNumber);
        if (sendTime == null) {
            return -1;
        }
        
        long rtt = System.currentTimeMillis() - sendTime;
        addSample(rtt);
        return rtt;
    }
    
    /**
     * 添加RTT样本
     *
     * @param rtt RTT值
     */
    public void addSample(long rtt) {
        // 添加到样本队列
        rttSamples.offer(rtt);
        if (rttSamples.size() > SAMPLE_WINDOW) {
            rttSamples.poll();
        }
        
        // 更新最小、最大RTT
        minRtt = Math.min(minRtt, rtt);
        maxRtt = Math.max(maxRtt, rtt);
        
        // 更新平滑RTT（使用Karn算法）
        if (smoothedRtt == 0) {
            // 第一个样本
            smoothedRtt = rtt;
            rttVar = rtt / 2;
        } else {
            // SRTT = (1 - α) * SRTT + α * RTT
            // RTTVAR = (1 - β) * RTTVAR + β * |SRTT - RTT|
            // α = 1/8, β = 1/4
            long delta = Math.abs(smoothedRtt - rtt);
            rttVar = (3 * rttVar + delta) / 4;
            smoothedRtt = (7 * smoothedRtt + rtt) / 8;
        }
        
        log.debug("RTT测量 - 当前RTT: {}ms, 平滑RTT: {}ms, RTT变化: {}ms", 
                  rtt, smoothedRtt, rttVar);
    }
    
    /**
     * 获取平滑RTT
     *
     * @return 平滑RTT
     */
    public long getSmoothedRtt() {
        return smoothedRtt > 0 ? smoothedRtt : 0;
    }
    
    /**
     * 获取RTT变化量
     *
     * @return RTT变化量
     */
    public long getRttVar() {
        return rttVar;
    }
    
    /**
     * 获取最小RTT
     *
     * @return 最小RTT
     */
    public long getMinRtt() {
        return minRtt != Long.MAX_VALUE ? minRtt : 0;
    }
    
    /**
     * 获取最大RTT
     *
     * @return 最大RTT
     */
    public long getMaxRtt() {
        return maxRtt > 0 ? maxRtt : 0;
    }
    
    /**
     * 获取平均RTT
     *
     * @return 平均RTT
     */
    public long getAverageRtt() {
        if (rttSamples.isEmpty()) {
            return 0;
        }
        return (long) rttSamples.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
    }
    
    /**
     * 计算超时时间（RTO）
     * RTO = SRTT + 4 * RTTVAR
     *
     * @return 超时时间
     */
    public long calculateRto() {
        return smoothedRtt + 4 * rttVar;
    }
    
    /**
     * 重置统计
     */
    public void reset() {
        rttSamples.clear();
        sendTimeMap.clear();
        smoothedRtt = 0;
        rttVar = 0;
        minRtt = Long.MAX_VALUE;
        maxRtt = 0;
    }
    
    /**
     * 获取RTT统计信息
     *
     * @return RTT统计
     */
    public RttStats getStats() {
        RttStats stats = new RttStats();
        stats.setSmoothedRtt(getSmoothedRtt());
        stats.setMinRtt(getMinRtt());
        stats.setMaxRtt(getMaxRtt());
        stats.setAverageRtt(getAverageRtt());
        stats.setRttVar(getRttVar());
        stats.setRto(calculateRto());
        stats.setSampleCount(rttSamples.size());
        return stats;
    }
    
    /**
     * RTT统计信息
     */
    @Data
    public static class RttStats {
        private long smoothedRtt;
        private long minRtt;
        private long maxRtt;
        private long averageRtt;
        private long rttVar;
        private long rto;
        private int sampleCount;
    }
}

