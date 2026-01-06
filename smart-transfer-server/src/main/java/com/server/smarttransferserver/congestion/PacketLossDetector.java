package com.server.smarttransferserver.congestion;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 丢包检测器
 * 使用超时和重复ACK检测丢包
 */
@Slf4j
@Component
public class PacketLossDetector {
    
    /**
     * 快速重传阈值（收到3个重复ACK触发）
     */
    private static final int FAST_RETRANSMIT_THRESHOLD = 3;
    
    /**
     * 已发送但未确认的数据包 <序列号, 发送信息>
     */
    private final ConcurrentHashMap<Long, PacketInfo> inflightPackets;
    
    /**
     * 已接收的ACK序列号集合
     */
    private final ConcurrentSkipListSet<Long> receivedAcks;
    
    /**
     * 重复ACK计数 <序列号, 重复次数>
     */
    private final ConcurrentHashMap<Long, Integer> duplicateAckCount;
    
    /**
     * 最后确认的序列号
     */
    private long lastAckedSeq;
    
    /**
     * 丢包统计
     */
    private long totalLostPackets;
    private long timeoutLostPackets;
    private long fastRetransmitLostPackets;
    
    /**
     * 构造方法
     */
    public PacketLossDetector() {
        this.inflightPackets = new ConcurrentHashMap<>();
        this.receivedAcks = new ConcurrentSkipListSet<>();
        this.duplicateAckCount = new ConcurrentHashMap<>();
        this.lastAckedSeq = 0;
        this.totalLostPackets = 0;
        this.timeoutLostPackets = 0;
        this.fastRetransmitLostPackets = 0;
    }
    
    /**
     * 记录数据包发送
     *
     * @param sequenceNumber 序列号
     * @param size           数据包大小
     * @param rto            超时时间
     */
    public void recordSent(long sequenceNumber, long size, long rto) {
        PacketInfo info = new PacketInfo();
        info.setSequenceNumber(sequenceNumber);
        info.setSize(size);
        info.setSendTime(System.currentTimeMillis());
        info.setRto(rto);
        info.setRetransmitCount(0);
        
        inflightPackets.put(sequenceNumber, info);
        
        log.debug("记录数据包发送 - 序列号: {}, 大小: {}字节, RTO: {}ms", 
                  sequenceNumber, size, rto);
    }
    
    /**
     * 记录ACK接收
     *
     * @param sequenceNumber 序列号
     * @return 是否检测到丢包
     */
    public boolean recordAck(long sequenceNumber) {
        receivedAcks.add(sequenceNumber);
        
        // 移除已确认的数据包
        inflightPackets.remove(sequenceNumber);
        duplicateAckCount.remove(sequenceNumber);
        
        boolean lossDetected = false;
        
        // 检测是否是重复ACK
        if (sequenceNumber <= lastAckedSeq) {
            // 重复ACK
            int count = duplicateAckCount.getOrDefault(sequenceNumber, 0) + 1;
            duplicateAckCount.put(sequenceNumber, count);
            
            // 达到快速重传阈值
            if (count >= FAST_RETRANSMIT_THRESHOLD) {
                lossDetected = handleFastRetransmit(sequenceNumber);
            }
            
            log.debug("收到重复ACK - 序列号: {}, 重复次数: {}", sequenceNumber, count);
        } else {
            // 新的ACK
            lastAckedSeq = sequenceNumber;
            
            log.debug("收到ACK - 序列号: {}", sequenceNumber);
        }
        
        return lossDetected;
    }
    
    /**
     * 检查超时丢包
     *
     * @return 丢失的数据包列表
     */
    public ConcurrentHashMap<Long, PacketInfo> checkTimeout() {
        long now = System.currentTimeMillis();
        ConcurrentHashMap<Long, PacketInfo> lostPackets = new ConcurrentHashMap<>();
        
        inflightPackets.forEach((seq, info) -> {
            long elapsed = now - info.getSendTime();
            if (elapsed > info.getRto()) {
                // 超时
                lostPackets.put(seq, info);
                totalLostPackets++;
                timeoutLostPackets++;
                
                log.warn("检测到超时丢包 - 序列号: {}, 大小: {}字节, 超时: {}ms", 
                         seq, info.getSize(), elapsed);
            }
        });
        
        return lostPackets;
    }
    
    /**
     * 处理快速重传
     *
     * @param ackSeq ACK序列号
     * @return 是否检测到丢包
     */
    private boolean handleFastRetransmit(long ackSeq) {
        // 查找丢失的数据包（ACK之后的未确认包）
        boolean lossDetected = false;
        
        for (long seq : inflightPackets.keySet()) {
            if (seq > ackSeq) {
                // 可能丢失
                totalLostPackets++;
                fastRetransmitLostPackets++;
                lossDetected = true;
                
                log.warn("快速重传检测到丢包 - 序列号: {}, ACK序列号: {}", seq, ackSeq);
            }
        }
        
        return lossDetected;
    }
    
    /**
     * 标记数据包为重传
     *
     * @param sequenceNumber 序列号
     */
    public void markRetransmit(long sequenceNumber) {
        PacketInfo info = inflightPackets.get(sequenceNumber);
        if (info != null) {
            info.setRetransmitCount(info.getRetransmitCount() + 1);
            info.setSendTime(System.currentTimeMillis());
            
            log.info("标记数据包重传 - 序列号: {}, 重传次数: {}", 
                     sequenceNumber, info.getRetransmitCount());
        }
    }
    
    /**
     * 获取丢包率
     *
     * @param totalPackets 总数据包数
     * @return 丢包率
     */
    public double getLossRate(long totalPackets) {
        if (totalPackets == 0) {
            return 0;
        }
        return (double) totalLostPackets / totalPackets;
    }
    
    /**
     * 获取在途数据包数量
     *
     * @return 在途数据包数
     */
    public int getInflightCount() {
        return inflightPackets.size();
    }
    
    /**
     * 获取在途数据总字节数
     *
     * @return 在途数据字节数
     */
    public long getInflightBytes() {
        return inflightPackets.values().stream()
                .mapToLong(PacketInfo::getSize)
                .sum();
    }
    
    /**
     * 重置统计
     */
    public void reset() {
        inflightPackets.clear();
        receivedAcks.clear();
        duplicateAckCount.clear();
        lastAckedSeq = 0;
        totalLostPackets = 0;
        timeoutLostPackets = 0;
        fastRetransmitLostPackets = 0;
    }
    
    /**
     * 获取丢包统计信息
     *
     * @return 丢包统计
     */
    public LossStats getStats() {
        LossStats stats = new LossStats();
        stats.setTotalLostPackets(totalLostPackets);
        stats.setTimeoutLostPackets(timeoutLostPackets);
        stats.setFastRetransmitLostPackets(fastRetransmitLostPackets);
        stats.setInflightCount(getInflightCount());
        stats.setInflightBytes(getInflightBytes());
        return stats;
    }
    
    /**
     * 数据包信息
     */
    @Data
    public static class PacketInfo {
        private long sequenceNumber;
        private long size;
        private long sendTime;
        private long rto;
        private int retransmitCount;
    }
    
    /**
     * 丢包统计信息
     */
    @Data
    public static class LossStats {
        private long totalLostPackets;
        private long timeoutLostPackets;
        private long fastRetransmitLostPackets;
        private int inflightCount;
        private long inflightBytes;
    }
}

