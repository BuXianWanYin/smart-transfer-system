package com.server.smarttransferserver.congestion;

/**
 * 拥塞控制算法接口
 * 定义所有拥塞控制算法必须实现的方法
 */
public interface CongestionControlAlgorithm {
    
    /**
     * 初始化算法
     * 设置初始拥塞窗口、慢启动阈值等参数
     */
    void initialize();
    
    /**
     * 处理ACK确认（双 RTT：带宽估计用 fullRtt，延迟相关逻辑用 propagationRtt）
     *
     * @param ackedBytes      确认的字节数
     * @param fullRttMs       分片往返总时延（含传输时间，用于带宽估计）
     * @param propagationRttMs 传播 RTT（不含分片传输时间，用于延迟相关逻辑）；null 时用 fullRttMs
     */
    void onAck(long ackedBytes, long fullRttMs, Long propagationRttMs);

    /**
     * 兼容旧调用：仅传一个 RTT 时视为 fullRtt，propagation 用 null（算法内部用 fullRtt）
     */
    default void onAck(long ackedBytes, long rtt) {
        onAck(ackedBytes, rtt, null);
    }
    
    /**
     * 处理丢包事件
     * 检测到丢包时调整拥塞窗口和阈值
     *
     * @param lostBytes 丢失的字节数
     */
    void onLoss(long lostBytes);
    
    /**
     * 获取当前拥塞窗口大小
     *
     * @return 拥塞窗口大小（字节）
     */
    long getCwnd();
    
    /**
     * 获取当前传输速率
     *
     * @return 传输速率（字节/秒）
     */
    long getRate();
    
    /**
     * 获取当前算法状态
     *
     * @return 拥塞状态
     */
    CongestionState getState();
    
    /**
     * 获取算法名称
     *
     * @return 算法名称
     */
    String getAlgorithmName();
    
    /**
     * 获取慢启动阈值
     *
     * @return 慢启动阈值（字节）
     */
    long getSsthresh();
    
    /**
     * 获取当前RTT
     *
     * @return RTT（毫秒）
     */
    long getRtt();
    
    /**
     * 重置算法状态
     * 重新初始化所有参数
     */
    void reset();
}

