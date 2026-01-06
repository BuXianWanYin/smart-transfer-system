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
     * 处理ACK确认
     * 根据确认的字节数和RTT调整拥塞窗口
     *
     * @param ackedBytes 确认的字节数
     * @param rtt        往返时延（毫秒）
     */
    void onAck(long ackedBytes, long rtt);
    
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

