package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.congestion.*;
import com.server.smarttransferserver.service.INetworkMonitorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 网络监测服务实现
 * 集成RTT测量、带宽估算、丢包检测
 */
@Slf4j
@Service
public class NetworkMonitorServiceImpl implements INetworkMonitorService {
    
    /**
     * RTT测量器
     */
    private final RttMeasurement rttMeasurement;
    
    /**
     * 带宽估算器
     */
    private final BandwidthEstimator bandwidthEstimator;
    
    /**
     * 丢包检测器
     */
    private final PacketLossDetector lossDetector;
    
    /**
     * 总发送数据包数
     */
    private long totalSentPackets;
    
    /**
     * 总接收ACK数
     */
    private long totalReceivedAcks;
    
    /**
     * 构造方法
     */
    @Autowired
    public NetworkMonitorServiceImpl(RttMeasurement rttMeasurement,
                                      BandwidthEstimator bandwidthEstimator,
                                      PacketLossDetector lossDetector) {
        this.rttMeasurement = rttMeasurement;
        this.bandwidthEstimator = bandwidthEstimator;
        this.lossDetector = lossDetector;
        this.totalSentPackets = 0;
        this.totalReceivedAcks = 0;
    }
    
    /**
     * 记录数据包发送
     *
     * @param sequenceNumber 序列号
     * @param size           数据包大小
     */
    @Override
    public void recordPacketSent(long sequenceNumber, long size) {
        totalSentPackets++;
        
        // RTT测量
        rttMeasurement.recordSendTime(sequenceNumber);
        
        // 带宽估算
        bandwidthEstimator.recordSent(size);
        
        // 丢包检测
        long rto = rttMeasurement.calculateRto();
        lossDetector.recordSent(sequenceNumber, size, rto);
        
        log.debug("记录数据包发送 - 序列号: {}, 大小: {}字节", sequenceNumber, size);
    }
    
    /**
     * 记录ACK接收
     *
     * @param sequenceNumber 序列号
     * @return 测量的RTT值
     */
    @Override
    public long recordAckReceived(long sequenceNumber) {
        totalReceivedAcks++;
        
        // RTT测量
        long rtt = rttMeasurement.recordAck(sequenceNumber);
        
        // 丢包检测
        boolean lossDetected = lossDetector.recordAck(sequenceNumber);
        
        if (lossDetected) {
            log.warn("检测到丢包 - ACK序列号: {}", sequenceNumber);
        }
        
        log.debug("记录ACK接收 - 序列号: {}, RTT: {}ms", sequenceNumber, rtt);
        
        return rtt;
    }
    
    /**
     * 检查超时丢包
     */
    @Override
    public void checkTimeoutLoss() {
        java.util.concurrent.ConcurrentHashMap<Long, PacketLossDetector.PacketInfo> lostPackets = lossDetector.checkTimeout();
        if (!lostPackets.isEmpty()) {
            log.warn("检测到{}个超时丢包", lostPackets.size());
        }
    }
    
    /**
     * 获取当前RTT
     *
     * @return RTT（毫秒）
     */
    @Override
    public long getCurrentRtt() {
        return rttMeasurement.getSmoothedRtt();
    }
    
    /**
     * 获取最小RTT
     *
     * @return 最小RTT（毫秒）
     */
    @Override
    public long getMinRtt() {
        return rttMeasurement.getMinRtt();
    }
    
    /**
     * 获取估算带宽
     *
     * @return 带宽（字节/秒）
     */
    @Override
    public long getEstimatedBandwidth() {
        return bandwidthEstimator.getEstimatedBandwidth();
    }
    
    /**
     * 丢包率
     *
     * @return 丢包率
     */
    @Override
    public double getLossRate() {
        return lossDetector.getLossRate(totalSentPackets);
    }
    
    /**
     * 获取在途数据包数量
     *
     * @return 在途数据包数
     */
    @Override
    public int getInflightCount() {
        return lossDetector.getInflightCount();
    }
    
    /**
     * 获取在途数据总字节数
     *
     * @return 在途数据字节数
     */
    @Override
    public long getInflightBytes() {
        return lossDetector.getInflightBytes();
    }
    
    /**
     * 评估网络质量
     *
     * @return 网络质量等级
     */
    @Override
    public NetworkQuality evaluateNetworkQuality() {
        double lossRate = getLossRate();
        long rtt = getCurrentRtt();
        long bandwidth = getEstimatedBandwidth();
        
        // 优秀：丢包率<0.5%，RTT<50ms，带宽>10MB/s
        if (lossRate < 0.005 && rtt < 50 && bandwidth > 10 * 1024 * 1024) {
            return NetworkQuality.EXCELLENT;
        }
        
        // 良好：丢包率<1%，RTT<100ms，带宽>5MB/s
        if (lossRate < 0.01 && rtt < 100 && bandwidth > 5 * 1024 * 1024) {
            return NetworkQuality.GOOD;
        }
        
        // 一般：丢包率<3%，RTT<200ms，带宽>1MB/s
        if (lossRate < 0.03 && rtt < 200 && bandwidth > 1024 * 1024) {
            return NetworkQuality.FAIR;
        }
        
        // 差：其他情况
        return NetworkQuality.POOR;
    }
    
    /**
     * 重置所有监测器
     */
    @Override
    public void reset() {
        rttMeasurement.reset();
        bandwidthEstimator.reset();
        lossDetector.reset();
        totalSentPackets = 0;
        totalReceivedAcks = 0;
        
        log.info("网络监测器已重置");
    }
    
    /**
     * 获取网络监测统计信息
     *
     * @return 网络统计
     */
    @Override
    public NetworkStats getStats() {
        NetworkStats stats = new NetworkStats();
        
        // RTT统计
        stats.setRttStats(rttMeasurement.getStats());
        
        // 带宽统计
        stats.setBandwidthStats(bandwidthEstimator.getStats());
        
        // 丢包统计
        stats.setLossStats(lossDetector.getStats());
        
        // 总体统计
        stats.setTotalSentPackets(totalSentPackets);
        stats.setTotalReceivedAcks(totalReceivedAcks);
        stats.setNetworkQuality(evaluateNetworkQuality());
        
        return stats;
    }
    
    /**
     * 网络质量等级
     */
    public enum NetworkQuality {
        EXCELLENT("优秀"),
        GOOD("良好"),
        FAIR("一般"),
        POOR("差");
        
        private final String description;
        
        NetworkQuality(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 网络统计信息
     */
    @Data
    public static class NetworkStats {
        private RttMeasurement.RttStats rttStats;
        private BandwidthEstimator.BandwidthStats bandwidthStats;
        private PacketLossDetector.LossStats lossStats;
        private long totalSentPackets;
        private long totalReceivedAcks;
        private NetworkQuality networkQuality;
    }
}

