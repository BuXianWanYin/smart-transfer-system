package com.server.smarttransferserver.congestion;

/**
 * 拥塞控制状态枚举
 */
public enum CongestionState {
    
    /**
     * 慢启动阶段
     * cwnd < ssthresh，指数增长
     */
    SLOW_START("慢启动"),
    
    /**
     * 拥塞避免阶段
     * cwnd >= ssthresh，线性增长
     */
    CONGESTION_AVOIDANCE("拥塞避免"),
    
    /**
     * 快速恢复阶段
     * 检测到丢包后的恢复过程
     */
    FAST_RECOVERY("快速恢复"),
    
    /**
     * BBR - STARTUP阶段
     * 快速探测带宽
     */
    BBR_STARTUP("BBR启动"),
    
    /**
     * BBR - DRAIN阶段
     * 排空多余数据
     */
    BBR_DRAIN("BBR排空"),
    
    /**
     * BBR - PROBE_BW阶段
     * 循环探测带宽变化
     */
    BBR_PROBE_BW("BBR探测带宽"),
    
    /**
     * BBR - PROBE_RTT阶段
     * 定期探测最小RTT
     */
    BBR_PROBE_RTT("BBR探测RTT");
    
    /**
     * 状态描述
     */
    private final String description;
    
    /**
     * 构造方法
     *
     * @param description 状态描述
     */
    CongestionState(String description) {
        this.description = description;
    }
    
    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }
}

