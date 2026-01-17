package com.server.smarttransferserver.congestion;

import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * TCP Reno拥塞控制算法实现
 * 经典AIMD（加性增乘性减）算法，传统TCP的基础实现
 */
@Slf4j
@Component
public class RenoAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * 最大段大小（MSS），用于拥塞避免阶段的线性增长
     * 默认1KB，实际应用中可根据网络MTU调整
     */
    private static final long MSS = 1024L;
    
    /**
     * 当前拥塞窗口大小（字节）
     */
    private long cwnd;
    
    /**
     * 慢启动阈值（字节）
     */
    private long ssthresh;
    
    /**
     * 当前RTT（毫秒）
     */
    private long currentRtt;
    
    /**
     * 上次拥塞避免阶段的窗口更新间隔（毫秒）
     */
    private long lastCwndUpdateTime;
    
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
     * 初始化TCP Reno算法
     */
    @PostConstruct
    @Override
    public void initialize() {
        // Reno算法从较小的初始窗口开始（传统TCP是1 MSS）
        // 这里使用配置的最小窗口，但为了体现Reno的保守特性，可以使用更小的值
        this.cwnd = Math.min(congestionConfig.getInitialCwnd(), MSS * 10); // 初始窗口不超过10 MSS
        this.ssthresh = congestionConfig.getSsthresh();
        this.currentRtt = 100; // 默认RTT 100ms
        this.lastCwndUpdateTime = System.currentTimeMillis();
        this.state = CongestionState.SLOW_START;
        
        log.info("TCP Reno算法初始化 - cwnd: {}字节, ssthresh: {}字节", cwnd, ssthresh);
    }
    
    /**
     * 处理ACK确认
     * Reno算法：慢启动阶段指数增长，拥塞避免阶段线性增长
     *
     * @param ackedBytes 确认的字节数
     * @param rtt        往返时延（毫秒）
     */
    @Override
    public void onAck(long ackedBytes, long rtt) {
        this.currentRtt = rtt;
        
        if (state == CongestionState.SLOW_START) {
            // 慢启动阶段：每收到一个ACK，cwnd增加1 MSS（指数增长）
            // 实际实现中，如果ackedBytes包含多个段，按段增加
            long segmentsAcked = Math.max(1, ackedBytes / MSS);
            cwnd += segmentsAcked * MSS;
            
            // 达到阈值，切换到拥塞避免
            if (cwnd >= ssthresh) {
                state = CongestionState.CONGESTION_AVOIDANCE;
                log.debug("TCP Reno切换到拥塞避免阶段 - cwnd: {}字节, ssthresh: {}字节", cwnd, ssthresh);
            }
        } else if (state == CongestionState.CONGESTION_AVOIDANCE) {
            // 拥塞避免阶段：每RTT增加1 MSS（线性增长）
            // 公式：每收到ACK，cwnd = cwnd + MSS^2 / cwnd
            // 这样平均每RTT增加1 MSS
            long now = System.currentTimeMillis();
            long timeDelta = now - lastCwndUpdateTime;
            
            if (timeDelta >= currentRtt) {
                // 经过一个RTT，增加1 MSS
                cwnd += MSS;
                lastCwndUpdateTime = now;
            } else {
                // 在一个RTT内，按比例增加
                // cwnd += (MSS * MSS * timeDelta) / (cwnd * currentRtt)
                double increment = ((double) (MSS * MSS * timeDelta)) / (cwnd * currentRtt);
                cwnd += (long) increment;
            }
        } else if (state == CongestionState.FAST_RECOVERY) {
            // 快速恢复阶段：每收到重复ACK，cwnd增加1 MSS
            cwnd += MSS;
            
            // 快速恢复完成，返回拥塞避免
            if (cwnd >= ssthresh) {
                state = CongestionState.CONGESTION_AVOIDANCE;
                log.debug("TCP Reno快速恢复完成，返回拥塞避免 - cwnd: {}字节", cwnd);
            }
        }
        
        // 限制窗口范围（从配置获取）
        cwnd = Math.max(congestionConfig.getMinCwnd(), Math.min(cwnd, congestionConfig.getMaxCwnd()));
        
        log.debug("TCP Reno onAck - cwnd: {}字节, rtt: {}ms, state: {}", cwnd, rtt, state);
    }
    
    /**
     * 处理丢包事件
     * Reno算法：快速重传和快速恢复
     *
     * @param lostBytes 丢失的字节数
     */
    @Override
    public void onLoss(long lostBytes) {
        if (state == CongestionState.SLOW_START || state == CongestionState.CONGESTION_AVOIDANCE) {
            // 慢启动或拥塞避免阶段检测到丢包：快速重传和快速恢复
            // 更新慢启动阈值：ssthresh = cwnd / 2
            ssthresh = Math.max(cwnd / 2, congestionConfig.getMinCwnd());
            
            // 设置新的拥塞窗口：cwnd = ssthresh + 3 MSS（快速恢复）
            cwnd = ssthresh + 3 * MSS;
            
            // 进入快速恢复阶段
            state = CongestionState.FAST_RECOVERY;
            
            log.warn("TCP Reno检测到丢包，进入快速恢复 - 丢失{}字节, 新cwnd: {}字节, ssthresh: {}字节", 
                     lostBytes, cwnd, ssthresh);
        } else if (state == CongestionState.FAST_RECOVERY) {
            // 快速恢复阶段再次丢包：超时重传
            // 回到慢启动，cwnd = 1 MSS（或最小窗口）
            ssthresh = Math.max(cwnd / 2, congestionConfig.getMinCwnd());
            cwnd = Math.max(MSS, congestionConfig.getMinCwnd());
            state = CongestionState.SLOW_START;
            
            log.warn("TCP Reno快速恢复阶段超时，回到慢启动 - 丢失{}字节, 新cwnd: {}字节, ssthresh: {}字节", 
                     lostBytes, cwnd, ssthresh);
        }
        
        // 确保窗口不小于最小值
        cwnd = Math.max(cwnd, congestionConfig.getMinCwnd());
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
        return "Reno";
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