package com.server.smarttransferserver.congestion;

import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.Queue;

/**
 * TCP Vegas拥塞控制算法实现
 * 基于延迟变化而非丢包进行拥塞控制，具有低延迟、高公平性特点
 */
@Slf4j
@Component
public class VegasAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * Alpha参数：期望与实际吞吐量差异的下限阈值
     * 当差异小于Alpha时，增加窗口（网络空闲）
     */
    private static final long ALPHA = 1;
    
    /**
     * Beta参数：期望与实际吞吐量差异的上限阈值
     * 当差异大于Beta时，减少窗口（接近拥塞）
     */
    private static final long BETA = 3;
    
    /**
     * Gamma参数：用于慢启动阶段检测延迟增长
     */
    private static final long GAMMA = 1;
    
    /**
     * 当前拥塞窗口大小（字节）
     */
    private long cwnd;
    
    /**
     * 慢启动阈值（字节）
     */
    private long ssthresh;
    
    /**
     * 当前状态
     */
    private CongestionState state;
    
    /**
     * 基础RTT（BaseRTT）：当前已知的最小RTT
     */
    private long baseRtt;
    
    /**
     * 当前RTT
     */
    private long currentRtt;
    
    /**
     * RTT样本队列（用于计算基础RTT和检测延迟变化）
     */
    private final Queue<Long> rttSamples;
    
    /**
     * 窗口样本队列（用于计算期望吞吐量）
     */
    private final Queue<Long> windowSamples;
    
    /**
     * 上次窗口更新时的RTT
     */
    private long lastRtt;
    
    /**
     * 拥塞控制配置
     */
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 构造方法
     */
    public VegasAlgorithm() {
        this.rttSamples = new LinkedList<>();
        this.windowSamples = new LinkedList<>();
    }
    
    /**
     * 初始化TCP Vegas算法
     */
    @PostConstruct
    @Override
    public void initialize() {
        // Vegas算法从较小窗口开始，但可以使用配置的初始窗口
        this.cwnd = Math.min(congestionConfig.getInitialCwnd(), 1048576L * 5); // 初始不超过5MB
        this.ssthresh = congestionConfig.getSsthresh();
        this.baseRtt = Long.MAX_VALUE;
        this.currentRtt = 100; // 默认RTT 100ms
        this.lastRtt = 100;
        this.state = CongestionState.SLOW_START;
        
        rttSamples.clear();
        windowSamples.clear();
        
        log.info("TCP Vegas算法初始化 - cwnd: {}字节, ssthresh: {}字节", cwnd, ssthresh);
    }
    
    /**
     * 处理ACK确认
     * Vegas算法：基于RTT变化调整窗口
     *
     * @param ackedBytes 确认的字节数
     * @param rtt        往返时延（毫秒）
     */
    @Override
    public void onAck(long ackedBytes, long rtt) {
        // 更新RTT样本
        updateRttSample(rtt);
        this.currentRtt = rtt;
        
        // 更新基础RTT（取最小值）
        if (rtt < baseRtt) {
            baseRtt = rtt;
        }
        
        // 更新窗口样本
        windowSamples.offer(cwnd);
        if (windowSamples.size() > 10) {
            windowSamples.poll();
        }
        
        if (state == CongestionState.SLOW_START) {
            // 慢启动阶段：指数增长，但监测RTT变化
            cwnd += ackedBytes;
            
            // Vegas改进的慢启动：如果RTT开始增长，提前切换到拥塞避免
            if (rtt > lastRtt + GAMMA && baseRtt != Long.MAX_VALUE) {
                ssthresh = cwnd;
                state = CongestionState.CONGESTION_AVOIDANCE;
                log.debug("TCP Vegas检测到RTT增长，提前切换到拥塞避免 - cwnd: {}字节, rtt: {}ms", cwnd, rtt);
            } else if (cwnd >= ssthresh) {
                // 达到阈值，切换到拥塞避免
                state = CongestionState.CONGESTION_AVOIDANCE;
                log.debug("TCP Vegas切换到拥塞避免阶段 - cwnd: {}字节, ssthresh: {}字节", cwnd, ssthresh);
            }
        } else {
            // 拥塞避免阶段：基于延迟差异调整窗口
            adjustWindowByDelay();
        }
        
        lastRtt = rtt;
        
        // 限制窗口范围（从配置获取）
        cwnd = Math.max(congestionConfig.getMinCwnd(), Math.min(cwnd, congestionConfig.getMaxCwnd()));
        
        log.debug("TCP Vegas onAck - cwnd: {}字节, rtt: {}ms, baseRtt: {}ms, state: {}", 
                  cwnd, rtt, baseRtt != Long.MAX_VALUE ? baseRtt : 0, state);
    }
    
    /**
     * 基于延迟差异调整拥塞窗口
     * Vegas核心算法：Expected = cwnd / BaseRTT, Actual = cwnd / CurrentRTT
     * Diff = Expected - Actual
     */
    private void adjustWindowByDelay() {
        if (baseRtt == Long.MAX_VALUE || baseRtt <= 0) {
            // 基础RTT未建立，线性增长
            cwnd += 1024; // 每次增加1KB
            return;
        }
        
        // 计算期望吞吐量（字节/秒）
        long expectedThroughput = (cwnd * 1000) / baseRtt;
        
        // 计算实际吞吐量（字节/秒）
        long actualThroughput = (cwnd * 1000) / Math.max(currentRtt, 1);
        
        // 计算差异（期望 - 实际）
        long diff = expectedThroughput - actualThroughput;
        
        // 将差异转换为字节单位（简化计算）
        // 更准确的计算应该考虑RTT的差异
        long diffInBytes = (diff * baseRtt) / 1000;
        
        if (diffInBytes < ALPHA * 1024) {
            // 差异小于Alpha：网络空闲，增加窗口
            // 每RTT增加1个MSS
            cwnd += 1024; // 1 KB
        } else if (diffInBytes > BETA * 1024) {
            // 差异大于Beta：接近拥塞，减少窗口
            // 每RTT减少1个MSS
            cwnd = Math.max(cwnd - 1024, congestionConfig.getMinCwnd());
        }
        // 差异在[Alpha, Beta]之间：保持窗口不变（稳定状态）
    }
    
    /**
     * 处理丢包事件
     * Vegas算法：丢包较少发生，但发生时仍需要处理
     *
     * @param lostBytes 丢失的字节数
     */
    @Override
    public void onLoss(long lostBytes) {
        // Vegas算法很少发生丢包（因为它在拥塞发生前就减少窗口）
        // 如果发生丢包，说明网络拥塞严重，需要更激进地减少窗口
        
        // 更新慢启动阈值：ssthresh = cwnd / 2
        ssthresh = Math.max(cwnd / 2, congestionConfig.getMinCwnd());
        
        // 减少拥塞窗口：cwnd = ssthresh
        cwnd = ssthresh;
        
        // 回到慢启动
        state = CongestionState.SLOW_START;
        
        // 重置基础RTT，重新探测
        baseRtt = Long.MAX_VALUE;
        rttSamples.clear();
        
        log.warn("TCP Vegas检测到丢包 - 丢失{}字节, 新cwnd: {}字节, ssthresh: {}字节", 
                 lostBytes, cwnd, ssthresh);
    }
    
    /**
     * 更新RTT样本
     *
     * @param rtt RTT值（毫秒）
     */
    private void updateRttSample(long rtt) {
        // **修复：过滤无效RTT值**
        if (rtt > 0 && rtt < 10000) {
            rttSamples.offer(rtt);
            if (rttSamples.size() > 20) {
                rttSamples.poll();
            }
            
            // 更新基础RTT（取历史最小值，过滤null值）
            long minRtt = rttSamples.stream()
                    .filter(r -> r != null && r > 0)
                    .min(Long::compare)
                    .orElse(rtt);
            if (minRtt < baseRtt) {
                baseRtt = minRtt;
            }
        }
    }
    
    @Override
    public long getCwnd() {
        return cwnd;
    }
    
    /**
     * 设置拥塞窗口（用于算法切换时继承cwnd）
     * @param cwnd 新的拥塞窗口大小
     */
    public void setCwnd(long cwnd) {
        this.cwnd = Math.max(cwnd, congestionConfig.getMinCwnd());
        log.info("TCP Vegas算法设置cwnd: {}字节 ({:.2f}MB)", cwnd, cwnd / 1024.0 / 1024.0);
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
        return "Vegas";
    }
    
    @Override
    public long getSsthresh() {
        return ssthresh;
    }
    
    @Override
    public long getRtt() {
        return baseRtt != Long.MAX_VALUE ? baseRtt : currentRtt;
    }
    
    @Override
    public void reset() {
        initialize();
    }
}