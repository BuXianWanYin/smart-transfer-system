package com.server.smarttransferserver.service;

import com.server.smarttransferserver.service.impl.NetworkMonitorServiceImpl.NetworkQuality;
import com.server.smarttransferserver.service.impl.NetworkMonitorServiceImpl.NetworkStats;

/**
 * 网络监测服务接口
 * 集成RTT测量、带宽估算、丢包检测
 */
public interface INetworkMonitorService {
    
    /**
     * 记录数据包发送
     *
     * @param sequenceNumber 序列号
     * @param size           数据包大小
     */
    void recordPacketSent(long sequenceNumber, long size);
    
    /**
     * 记录ACK接收
     *
     * @param sequenceNumber 序列号
     * @return 测量的RTT值
     */
    long recordAckReceived(long sequenceNumber);
    
    /**
     * 检查超时丢包
     */
    void checkTimeoutLoss();
    
    /**
     * 获取当前RTT
     *
     * @return RTT（毫秒）
     */
    long getCurrentRtt();
    
    /**
     * 获取最小RTT
     *
     * @return 最小RTT（毫秒）
     */
    long getMinRtt();
    
    /**
     * 获取估算带宽
     *
     * @return 带宽（字节/秒）
     */
    long getEstimatedBandwidth();
    
    /**
     * 获取丢包率
     *
     * @return 丢包率
     */
    double getLossRate();
    
    /**
     * 获取在途数据包数量
     *
     * @return 在途数据包数
     */
    int getInflightCount();
    
    /**
     * 获取在途数据总字节数
     *
     * @return 在途数据字节数
     */
    long getInflightBytes();
    
    /**
     * 评估网络质量
     *
     * @return 网络质量等级，如果数据不足可能返回null
     */
    NetworkQuality evaluateNetworkQuality();
    
    /**
     * 重置所有监测器
     */
    void reset();
    
    /**
     * 获取网络监测统计信息
     *
     * @return 网络统计
     */
    NetworkStats getStats();
}

