package com.server.smarttransferserver.congestion;

import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.Queue;

/**
 * BBR拥塞控制算法实现
 * Google开发的基于带宽和RTT的拥塞控制算法
 */
@Slf4j
@Component
public class BBRAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * STARTUP阶段的pacing_gain = 2.0
     */
    private static final double STARTUP_PACING_GAIN = 2.0;
    
    /**
     * DRAIN阶段的pacing_gain = 0.5
     */
    private static final double DRAIN_PACING_GAIN = 0.5;
    
    /**
     * PROBE_BW阶段的pacing_gain循环值
     */
    private static final double[] PROBE_BW_PACING_GAINS = {1.25, 0.75, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
    
    /**
     * 瓶颈带宽（字节/秒）
     */
    private long bottleneckBandwidth;
    
    /**
     * 最小RTT（毫秒）- 传播RTT，用于延迟检测
     */
    private long minRtt;
    
    /**
     * 最近的fullRTT（毫秒）- 用于BDP计算，与bottleneckBandwidth计算保持一致
     */
    private long lastFullRtt;
    
    /**
     * 当前状态
     */
    private CongestionState state;
    
    /**
     * 当前pacing_gain
     */
    private double pacingGain;
    
    /**
     * PROBE_BW循环索引
     */
    private int probeBwCycleIndex;
    
    /**
     * 带宽样本队列（最近10个）
     */
    private final Queue<Long> bandwidthSamples;
    
    /**
     * RTT样本队列（最近10个）
     */
    private final Queue<Long> rttSamples;
    
    /**
     * 上次进入PROBE_RTT的时间
     */
    private long lastProbeRttTime;
    
    /**
     * 当前拥塞窗口
     */
    private long cwnd;
    
    /**
     * 拥塞控制配置
     */
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 构造方法
     */
    public BBRAlgorithm() {
        this.bandwidthSamples = new LinkedList<>();
        this.rttSamples = new LinkedList<>();
    }
    
    @PostConstruct
    @Override
    public void initialize() {
        this.bottleneckBandwidth = 0;
        this.minRtt = Long.MAX_VALUE;
        this.lastFullRtt = 100;  // 初始假设100ms
        this.state = CongestionState.BBR_STARTUP;
        this.pacingGain = STARTUP_PACING_GAIN;
        this.probeBwCycleIndex = 0;
        this.lastProbeRttTime = System.currentTimeMillis();
        this.cwnd = congestionConfig.getMinCwnd();
        
        bandwidthSamples.clear();
        rttSamples.clear();
        
        log.info("BBR算法初始化 - state: {}, cwnd: {}字节", state, cwnd);
    }
    
    @Override
    public void onAck(long ackedBytes, long fullRttMs, Long propagationRttMs) {
        // 带宽估计用 fullRtt（含传输时间），得到真实吞吐
        long rttForBandwidth = Math.max(fullRttMs, 1);
        long currentBandwidth = (ackedBytes * 1000) / rttForBandwidth;
        updateBandwidthSample(currentBandwidth);
        
        // 保存最近的fullRTT，用于BDP计算
        this.lastFullRtt = rttForBandwidth;
        
        // 延迟检测用传播 RTT（与 Clumsy 配置的延迟一致）
        long rttForDelay = propagationRttMs != null ? propagationRttMs : fullRttMs;
        updateRttSample(rttForDelay);
        
        // 根据当前状态处理
        switch (state) {
            case BBR_STARTUP:
                handleStartup();
                break;
            case BBR_DRAIN:
                handleDrain();
                break;
            case BBR_PROBE_BW:
                handleProbeBw();
                break;
            case BBR_PROBE_RTT:
                handleProbeRtt();
                break;
            default:
                break;
        }
        
        // **关键修复：BBR的BDP应该基于链路容量，而不是当前吞吐量**
        // 问题：bottleneckBandwidth是用fullRTT计算的吞吐量，受cwnd限制
        // 如果cwnd小，fullRTT长，bottleneckBandwidth就小，形成恶性循环
        // 解决：使用propagationRTT计算理论BDP，但要保证cwnd足够大以维持吞吐
        
        // 使用传播RTT计算理论BDP
        long theoreticalBdp = (bottleneckBandwidth * minRtt) / 1000;
        
        // **核心修复**：在HTTP大文件传输场景，fullRTT包含数据传输时间
        // 实际需要的cwnd应该 >= 当前传输的数据量，才能维持满速
        // 使用 fullRTT 作为参考，确保 cwnd 足够大
        long minCwndForThroughput = (bottleneckBandwidth * lastFullRtt) / 1000;
        
        // 取两者的最大值，确保既考虑网络延迟，又保证吞吐量
        long bdp = Math.max(theoreticalBdp, minCwndForThroughput);
        cwnd = (long) (bdp * pacingGain);
        cwnd = Math.max(congestionConfig.getMinCwnd(), Math.min(cwnd, congestionConfig.getMaxCwnd()));
        
        log.debug("BBR onAck - state: {}, cwnd: {}字节, bandwidth: {}字节/秒, fullRtt: {}ms, propRtt: {}ms, theoreticalBDP: {}字节, minCwndForThroughput: {}字节", 
                  state, cwnd, bottleneckBandwidth, fullRttMs, rttForDelay, theoreticalBdp, minCwndForThroughput);
    }
    
    @Override
    public void onLoss(long lostBytes) {
        // BBR不依赖丢包进行拥塞控制
        // 只记录日志
        log.debug("BBR检测到丢包 - 丢失{}字节（BBR不依赖丢包调整）", lostBytes);
    }
    
    /**
     * 处理STARTUP阶段
     * 快速探测带宽，pacing_gain = 2.0
     */
    private void handleStartup() {
        // 如果带宽不再增长，切换到DRAIN
        if (bandwidthSamples.size() >= 3) {
            Long[] samples = bandwidthSamples.toArray(new Long[0]);
            int len = samples.length;
            
            // 检查最近3个样本是否增长
            if (len >= 3 && samples[len - 1] <= samples[len - 2]) {
                state = CongestionState.BBR_DRAIN;
                pacingGain = DRAIN_PACING_GAIN;
                log.info("BBR切换到DRAIN阶段");
            }
        }
    }
    
    /**
     * 处理DRAIN阶段
     * 排空多余数据，pacing_gain = 0.5
     */
    private void handleDrain() {
        // 当队列排空后，切换到PROBE_BW
        long bdp = (bottleneckBandwidth * minRtt) / 1000;
        if (cwnd <= bdp) {
            state = CongestionState.BBR_PROBE_BW;
            probeBwCycleIndex = 0;
            pacingGain = PROBE_BW_PACING_GAINS[probeBwCycleIndex];
            log.info("BBR切换到PROBE_BW阶段");
        }
    }
    
    /**
     * 处理PROBE_BW阶段
     * 循环探测带宽变化
     */
    private void handleProbeBw() {
        // 每隔一段时间切换pacing_gain
        probeBwCycleIndex = (probeBwCycleIndex + 1) % PROBE_BW_PACING_GAINS.length;
        pacingGain = PROBE_BW_PACING_GAINS[probeBwCycleIndex];
        
        // 每10秒进入PROBE_RTT
        long now = System.currentTimeMillis();
        if (now - lastProbeRttTime > 10000) {
            state = CongestionState.BBR_PROBE_RTT;
            lastProbeRttTime = now;
            log.info("BBR切换到PROBE_RTT阶段");
        }
    }
    
    /**
     * 处理PROBE_RTT阶段
     * 定期探测最小RTT
     */
    private void handleProbeRtt() {
        // 维持200ms后返回PROBE_BW
        long now = System.currentTimeMillis();
        if (now - lastProbeRttTime > 200) {
            state = CongestionState.BBR_PROBE_BW;
            probeBwCycleIndex = 0;
            pacingGain = PROBE_BW_PACING_GAINS[probeBwCycleIndex];
            log.info("BBR返回PROBE_BW阶段");
        }
    }
    
    /**
     * 更新RTT样本
     *
     * @param rtt RTT值
     */
    private void updateRttSample(long rtt) {
        // **修复：过滤无效RTT值**
        if (rtt > 0 && rtt < 10000) {
            rttSamples.offer(rtt);
            if (rttSamples.size() > 10) {
                rttSamples.poll();
            }
            
            // 更新最小RTT（过滤null值）
            minRtt = rttSamples.stream()
                    .filter(r -> r != null && r > 0)
                    .min(Long::compare)
                    .orElse(rtt);
        }
    }
    
    /**
     * 更新带宽样本
     *
     * @param bandwidth 带宽值
     */
    private void updateBandwidthSample(long bandwidth) {
        bandwidthSamples.offer(bandwidth);
        if (bandwidthSamples.size() > 10) {
            bandwidthSamples.poll();
        }
        
        // 更新瓶颈带宽（取最大值）
        bottleneckBandwidth = bandwidthSamples.stream().max(Long::compare).orElse(bandwidth);
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
        
        // **修复：不要反推带宽，让BBR通过实际测量恢复**
        // 如果有历史带宽样本，保留它们；否则清空，让BBR重新测量
        // 原因：cwnd来自其他算法（如Reno/CUBIC），用错误的RTT反推会得到错误的带宽
        // BBR会在接下来的ACK中通过 (ackedBytes * 1000) / fullRTT 来准确测量带宽
        
        log.info("BBR算法设置cwnd: {}字节 ({}MB), 保留历史带宽样本: {} 个, 当前估算带宽: {}字节/秒 ({}MB/s)", 
                cwnd, String.format("%.2f", cwnd / 1024.0 / 1024.0),
                bandwidthSamples.size(),
                bottleneckBandwidth, String.format("%.2f", bottleneckBandwidth / 1024.0 / 1024.0));
    }
    
    @Override
    public long getRate() {
        return (long) (bottleneckBandwidth * pacingGain);
    }
    
    @Override
    public CongestionState getState() {
        return state;
    }
    
    @Override
    public String getAlgorithmName() {
        return "BBR";
    }
    
    @Override
    public long getSsthresh() {
        // BBR不使用ssthresh
        return 0;
    }
    
    @Override
    public long getRtt() {
        return minRtt != Long.MAX_VALUE ? minRtt : 0;
    }
    
    @Override
    public void reset() {
        initialize();
    }
}

