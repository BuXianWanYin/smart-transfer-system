package com.server.smarttransferserver.congestion;

import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CUBIC拥塞控制算法实现
 * Linux内核默认的TCP拥塞控制算法
 */
@Slf4j
@Component
public class CubicAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * CUBIC常数C = 0.4
     */
    private static final double C = 0.4;
    
    /**
     * 乘性减少因子β = 0.7
     */
    private static final double BETA = 0.7;
    
    /**
     * 当前拥塞窗口大小（字节）
     */
    private long cwnd;
    
    /**
     * 慢启动阈值（字节）
     */
    private long ssthresh;
    
    /**
     * 上次拥塞前的最大窗口（字节）
     */
    private long wMax;
    
    /**
     * 上次拥塞事件时间（毫秒）
     */
    private long lastCongestionTime;
    
    /**
     * 当前RTT（毫秒）
     */
    private long currentRtt;
    
    /**
     * 当前状态
     */
    private CongestionState state;
    
    /**
     * 拥塞控制配置
     */
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 初始化CUBIC算法
     */
    @Override
    public void initialize() {
        // 从配置中心获取初始值
        this.cwnd = congestionConfig.getInitialCwnd();
        this.ssthresh = congestionConfig.getSsthresh();
        this.wMax = 0;
        this.lastCongestionTime = System.currentTimeMillis();
        this.currentRtt = 100; // 默认RTT 100ms
        this.state = CongestionState.SLOW_START;
        
        log.info("CUBIC算法初始化 - cwnd: {}字节, ssthresh: {}字节", cwnd, ssthresh);
    }
    
    /**
     * 处理ACK确认
     *
     * @param ackedBytes 确认的字节数
     * @param rtt        往返时延（毫秒）
     */
    @Override
    public void onAck(long ackedBytes, long rtt) {
        this.currentRtt = rtt;
        
        if (state == CongestionState.SLOW_START) {
            // 慢启动阶段：指数增长
            cwnd += ackedBytes;
            
            // 达到阈值，切换到拥塞避免
            if (cwnd >= ssthresh) {
                state = CongestionState.CONGESTION_AVOIDANCE;
                log.debug("CUBIC切换到拥塞避免阶段 - cwnd: {}字节", cwnd);
            }
        } else {
            // 拥塞避免阶段：使用CUBIC函数
            long t = System.currentTimeMillis() - lastCongestionTime;
            long newCwnd = calculateCubicWindow(t);
            
            // 更新拥塞窗口
            cwnd = Math.min(newCwnd, congestionConfig.getMaxCwnd());
        }
        
        // 限制窗口范围（从配置获取）
        cwnd = Math.max(congestionConfig.getMinCwnd(), Math.min(cwnd, congestionConfig.getMaxCwnd()));
        
        log.debug("CUBIC onAck - cwnd: {}字节, rtt: {}ms, state: {}", cwnd, rtt, state);
    }
    
    /**
     * 处理丢包事件
     *
     * @param lostBytes 丢失的字节数
     */
    @Override
    public void onLoss(long lostBytes) {
        // 记录丢包前的最大窗口
        wMax = cwnd;
        
        // 乘性减少：cwnd = cwnd * β
        cwnd = (long) (cwnd * BETA);
        cwnd = Math.max(cwnd, congestionConfig.getMinCwnd());
        
        // 更新慢启动阈值
        ssthresh = cwnd;
        
        // 记录拥塞时间
        lastCongestionTime = System.currentTimeMillis();
        
        // 进入快速恢复
        state = CongestionState.FAST_RECOVERY;
        
        log.warn("CUBIC检测到丢包 - 丢失{}字节, 新cwnd: {}字节, ssthresh: {}字节", 
                 lostBytes, cwnd, ssthresh);
    }
    
    /**
     * 计算CUBIC窗口大小
     * W(t) = C * (t - K)^3 + Wmax
     * K = ∛((Wmax - cwnd) / C)
     *
     * @param t 距离上次拥塞的时间（毫秒）
     * @return 新的拥塞窗口大小
     */
    private long calculateCubicWindow(long t) {
        if (wMax == 0) {
            // 没有拥塞历史，线性增长
            return cwnd + 1000; // 每次增加1KB
        }
        
        // 转换为秒
        double tSec = t / 1000.0;
        
        // 计算K = ∛((Wmax - cwnd) / C)
        double k = Math.cbrt((wMax - cwnd) / C);
        
        // 计算W(t) = C * (t - K)^3 + Wmax
        double delta = tSec - k;
        long newCwnd = (long) (C * delta * delta * delta + wMax);
        
        return Math.max(newCwnd, cwnd);
    }
    
    @Override
    public long getCwnd() {
        return cwnd;
    }
    
    @Override
    public long getRate() {
        // 速率 = 拥塞窗口 / RTT
        return (cwnd * 1000) / Math.max(currentRtt, 1);
    }
    
    @Override
    public CongestionState getState() {
        return state;
    }
    
    @Override
    public String getAlgorithmName() {
        return "CUBIC";
    }
    
    @Override
    public long getSsthresh() {
        return ssthresh;
    }
    
    @Override
    public long getRtt() {
        return currentRtt;
    }
    
    @Override
    public void reset() {
        initialize();
    }
}

