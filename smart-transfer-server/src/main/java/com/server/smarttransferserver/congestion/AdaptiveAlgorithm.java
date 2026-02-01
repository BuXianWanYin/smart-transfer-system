package com.server.smarttransferserver.congestion;

import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 自适应拥塞控制算法
 * 根据网络质量动态选择Reno、Vegas、CUBIC或BBR
 */
@Slf4j
@Component
public class AdaptiveAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * RTT 样本窗口大小（平衡响应速度与稳定性）
     * 15 个分片约 75MB，能快速响应网络变化
     */
    private static final int EVALUATION_WINDOW = 15;
    
    /**
     * Reno算法实例
     */
    private final RenoAlgorithm renoAlgorithm;
    
    /**
     * Vegas算法实例
     */
    private final VegasAlgorithm vegasAlgorithm;
    
    /**
     * CUBIC算法实例
     */
    private final CubicAlgorithm cubicAlgorithm;
    
    /**
     * BBR算法实例
     */
    private final BBRAlgorithm bbrAlgorithm;
    
    /**
     * 当前使用的算法
     */
    private CongestionControlAlgorithm currentAlgorithm;
    
    /**
     * 丢包统计
     */
    private long totalPackets;
    private long lostPackets;
    
    /**
     * 当前网络指标（用于算法切换决策）
     */
    private double currentLossRate;
    private long currentRttJitter;
    
    /**
     * 上一次评估的网络质量等级（用于检测网络变化）
     * 0=未知, 1=优秀, 2=良好, 3=一般, 4=差
     */
    private int lastNetworkQualityLevel = 0;
    
    /**
     * RTT样本队列
     */
    private final Queue<Long> rttSamples;
    
    /**
     * 上次评估时间
     */
    private long lastEvaluationTime;
    
    /**
     * 上次切换时间（避免频繁切换）
     */
    private long lastSwitchTime;
    
    /**
     * 当前算法的连续使用时间（毫秒）
     */
    private long currentAlgorithmStartTime;
    
    /**
     * 拥塞控制配置
     */
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 网络趋势分析器
     */
    private NetworkTrendAnalyzer trendAnalyzer;
    
    /**
     * RTT异常值过滤器
     */
    private RttOutlierFilter outlierFilter;
    
    /**
     * 算法运行指标（可观测性）
     */
    private AdaptiveAlgorithmMetrics metrics;
    
    /**
     * 上一个算法（用于回滚）
     */
    private CongestionControlAlgorithm previousAlgorithm;
    
    /**
     * 本任务总分片数（init 时确定），用于丢包率 = 总丢包数/总分片数。0 表示未知，退回滑动窗口/累积计算。
     */
    private int totalChunks;
    
    /**
     * 算法预热状态：预热RTT计数
     */
    private int warmupRttCount;
    
    /**
     * 是否处于预热状态
     */
    private boolean isWarmingUp;
    
    /**
     * 预热前的cwnd（用于平滑过渡）
     */
    private long preWarmupCwnd;
    
    /**
     * 滑动窗口丢包统计（最近20个评估周期）
     */
    private final java.util.Queue<Double> lossRateHistory;
    
    /**
     * 保留的RTT历史样本（增量重置时保留20%）
     */
    private final java.util.Queue<Long> preservedRttSamples;
    
    /**
     * **新增：实时滑动窗口**
     * 用于快速响应网络变化（最近200个包的统计）
     */
    /**
     * 丢包滑动窗口（用于快速检测网络质量变化）
     * 20 个分片约 100MB，能较快反映丢包率变化
     */
    private final java.util.Queue<Boolean> recentPackets; // true=成功, false=丢包
    private static final int RECENT_WINDOW_SIZE = 20;
    
    /**
     * 构造方法
     */
    @Autowired
    public AdaptiveAlgorithm(RenoAlgorithm renoAlgorithm, VegasAlgorithm vegasAlgorithm,
                             CubicAlgorithm cubicAlgorithm, BBRAlgorithm bbrAlgorithm) {
        this.renoAlgorithm = renoAlgorithm;
        this.vegasAlgorithm = vegasAlgorithm;
        this.cubicAlgorithm = cubicAlgorithm;
        this.bbrAlgorithm = bbrAlgorithm;
        this.rttSamples = new LinkedList<>();
        this.lastSwitchTime = 0;
        this.currentAlgorithmStartTime = System.currentTimeMillis();
        this.lossRateHistory = new LinkedList<>();
        this.preservedRttSamples = new LinkedList<>();
        this.recentPackets = new LinkedList<>(); // **新增：初始化滑动窗口**
        this.metrics = new AdaptiveAlgorithmMetrics();
        this.isWarmingUp = false;
        this.warmupRttCount = 0;
    }
    
    @PostConstruct
    @Override
    public void initialize() {
        // 初始化所有算法
        if (renoAlgorithm != null) {
            renoAlgorithm.initialize();
        }
        if (vegasAlgorithm != null) {
            vegasAlgorithm.initialize();
        }
        if (cubicAlgorithm != null) {
            cubicAlgorithm.initialize();
        }
        if (bbrAlgorithm != null) {
            bbrAlgorithm.initialize();
        }
        
        // 默认使用CUBIC（平衡性能与稳定性）
        currentAlgorithm = cubicAlgorithm != null ? cubicAlgorithm : bbrAlgorithm;
        
        totalChunks = 0;
        
        // 初始化辅助组件
        trendAnalyzer = new NetworkTrendAnalyzer(
                congestionConfig.getTrendWindowSize(),
                congestionConfig.getTrendThreshold()
        );
        outlierFilter = new RttOutlierFilter(congestionConfig.isOutlierFilterEnabled());
        
        // 重置统计（全量重置）
        resetStatistics(ResetType.FULL_RESET);
        
        // 初始化指标
        metrics.setCurrentAlgorithm(currentAlgorithm.getAlgorithmName());
        
        log.info("自适应算法初始化 - 默认使用: {}, 趋势窗口: {}, 异常值过滤: {}", 
                currentAlgorithm.getAlgorithmName(), 
                congestionConfig.getTrendWindowSize(),
                congestionConfig.isOutlierFilterEnabled());
    }
    
    @Override
    public void onAck(long ackedBytes, long fullRttMs, Long propagationRttMs) {
        // RTT 仅使用独立探测值：只有 propagationRttMs（前端 probe）非空时才入样、参与 RTT 突增推断
        boolean inferredLoss = false;
        if (propagationRttMs != null && propagationRttMs > 0 && propagationRttMs < 10000) {
            rttSamples.offer(propagationRttMs);
            if (rttSamples.size() > EVALUATION_WINDOW) {
                rttSamples.poll();
            }
            // RTT 突增推断丢包（仅在有探测 RTT 时）
            if (rttSamples.size() >= 3) {
                double avg = rttSamples.stream().mapToLong(Long::longValue).average().orElse(0);
                if (avg > 0 && propagationRttMs >= 1.8 * avg) {
                    inferredLoss = true;
                    if (log.isDebugEnabled()) {
                        log.debug("RTT突增推断丢包 - 当前RTT: {}ms, 近期平均: {}ms", propagationRttMs, String.format("%.0f", avg));
                    }
                }
            }
        }
        
        // **修复：每个成功的 ACK 都记录到 recentPackets（不仅仅是有 RTT 探测时）**
        recentPackets.offer(inferredLoss ? false : true);
        if (recentPackets.size() > RECENT_WINDOW_SIZE) {
            recentPackets.poll();
        }
        
        totalPackets++;
        // 子算法使用双 RTT：带宽用 full，延迟用 propagation
        currentAlgorithm.onAck(ackedBytes, fullRttMs, propagationRttMs);
        
        // 定期评估是否需要切换算法
        long now = System.currentTimeMillis();
        if (now - lastEvaluationTime > congestionConfig.getEvaluationInterval()) {
            evaluateAndSwitch();
            lastEvaluationTime = now;
        } else {
            // **修复：在评估间隔内也记录当前使用的算法，方便调试（每50个包记录一次，避免刷屏）**
            if (totalPackets % 50 == 0) {
                log.debug("自适应算法运行中 - 当前使用: {}, cwnd: {}字节, 已处理包数: {}", 
                         currentAlgorithm.getAlgorithmName(), currentAlgorithm.getCwnd(), totalPackets);
            }
        }
    }
    
    @Override
    public void onLoss(long lostBytes) {
        // 统计丢包
        lostPackets++;
        
        // **新增：更新滑动窗口（记录丢包）**
        recentPackets.offer(false);
        if (recentPackets.size() > RECENT_WINDOW_SIZE) {
            recentPackets.poll();
        }
        
        // 调用当前算法
        currentAlgorithm.onLoss(lostBytes);
        
        log.debug("自适应算法检测到丢包 - 当前使用: {}, 丢失{}字节", 
                  currentAlgorithm.getAlgorithmName(), lostBytes);
    }
    
    /**
     * 评估网络质量并决定是否切换算法
     * 集成网络趋势分析、置信度阈值、算法预热、回滚机制等优化
     */
    private void evaluateAndSwitch() {
        if (totalPackets < 10) {
            return;
        }
        
        // 如果处于预热状态，检查是否需要提前结束
        if (isWarmingUp) {
            warmupRttCount--;
            
            // **优化：检测网络剧烈变化时提前结束预热**
            // 使用近期丢包率（更快响应网络变化）
            double cumulativeLossRate = getDisplayLossRate();
            long recentLossCount = recentPackets.stream().filter(b -> !b).count();
            double recentLossRate = recentPackets.isEmpty() ? cumulativeLossRate : (double) recentLossCount / recentPackets.size();
            long quickJitter = calculateRttJitter();
            
            // 判断当前网络质量（差网络用累计丢包率，好网络用近期丢包率）
            double lossThreshold = congestionConfig.getLossRateThreshold();
            long jitterThreshold = congestionConfig.getRttJitterThreshold();
            boolean nowPoor = cumulativeLossRate > lossThreshold * 2 || quickJitter > jitterThreshold * 2;
            boolean nowExcellent = recentLossRate < lossThreshold * 0.5 && quickJitter < jitterThreshold * 0.5;
            
            // 如果网络状况与当前算法严重不匹配，提前结束预热
            String currentAlgName = currentAlgorithm.getAlgorithmName();
            boolean mismatch = (nowPoor && !"Reno".equals(currentAlgName)) 
                            || (nowExcellent && !"BBR".equals(currentAlgName));
            
            if (mismatch) {
                log.info("网络状况与当前算法不匹配，提前结束预热 - 算法: {}, 累计丢包: {}%, 近期丢包: {}%, 抖动: {}ms", 
                        currentAlgName, String.format("%.2f", cumulativeLossRate * 100), 
                        String.format("%.2f", recentLossRate * 100), quickJitter);
                isWarmingUp = false;
                warmupRttCount = 0;
                // 继续执行评估
            } else if (warmupRttCount <= 0) {
                isWarmingUp = false;
                log.info("算法预热完成 - 当前算法: {}", currentAlgorithm.getAlgorithmName());
            } else {
                return;
            }
        }
        
        // **无 probe RTT 时不切换**：rttSamples 仅来自独立探测，为空时 avgRtt/minRtt 为 0，会误判为低延迟选 CUBIC；保持当前算法直至有 RTT 数据
        if (rttSamples.isEmpty()) {
            log.debug("无 RTT 样本（未上报 probe），跳过算法评估，保持当前: {}", currentAlgorithm.getAlgorithmName());
            return;
        }
        
        // **与界面一致：切换逻辑使用 getDisplayLossRate()（总丢包数/总分片数）**
        double lossRate = getDisplayLossRate();
        long rttJitter = calculateRttJitter();
        
        // **更新成员变量，供算法切换决策与 recordMetrics 使用**
        this.currentLossRate = lossRate;
        this.currentRttJitter = rttJitter;
        double avgRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        long minRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .min()
                .orElse(0);
        long maxRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
        double rttVariation = maxRtt > 0 ? (double) (maxRtt - minRtt) / maxRtt : 0;
        
        // 获取带宽（用于吞吐量评估）
        long bandwidth = currentAlgorithm.getRate();
        
        // **网络恢复检测**：如果近期连续无丢包，且 CWND 被压缩过，自动恢复到初始值
        long initialCwnd = congestionConfig.getInitialCwnd();
        long currentCwnd = currentAlgorithm.getCwnd();
        
        // 计算近期成功率（recentPackets 中 true 的比例）
        long recentSuccessCount = recentPackets.stream().filter(b -> b).count();
        double recentSuccessRate = recentPackets.isEmpty() ? 0 : (double) recentSuccessCount / recentPackets.size();
        
        // 条件：近期成功率 > 95%（几乎无丢包），RTT抖动 < 阈值，CWND 被压缩到初始值的 50% 以下
        boolean recentlyGood = recentSuccessRate > 0.95 && recentPackets.size() >= 10;
        boolean lowJitter = rttJitter < congestionConfig.getRttJitterThreshold();
        boolean cwndCompressed = currentCwnd < initialCwnd * 0.5;
        
        if (recentlyGood && lowJitter && cwndCompressed) {
            // 网络已恢复，提升 CWND 到初始值
            try {
                if (currentAlgorithm instanceof com.server.smarttransferserver.congestion.BBRAlgorithm) {
                    ((com.server.smarttransferserver.congestion.BBRAlgorithm) currentAlgorithm).setCwnd(initialCwnd);
                } else if (currentAlgorithm instanceof com.server.smarttransferserver.congestion.CubicAlgorithm) {
                    ((com.server.smarttransferserver.congestion.CubicAlgorithm) currentAlgorithm).setCwnd(initialCwnd);
                } else if (currentAlgorithm instanceof com.server.smarttransferserver.congestion.VegasAlgorithm) {
                    ((com.server.smarttransferserver.congestion.VegasAlgorithm) currentAlgorithm).setCwnd(initialCwnd);
                } else if (currentAlgorithm instanceof com.server.smarttransferserver.congestion.RenoAlgorithm) {
                    ((com.server.smarttransferserver.congestion.RenoAlgorithm) currentAlgorithm).setCwnd(initialCwnd);
                }
                log.info("网络恢复检测 - 近期成功率: {}%, RTT抖动: {}ms, CWND 从 {}MB 恢复至初始值 {}MB",
                        String.format("%.0f", recentSuccessRate * 100), 
                        rttJitter,
                        String.format("%.2f", currentCwnd / 1024.0 / 1024.0),
                        String.format("%.2f", initialCwnd / 1024.0 / 1024.0));
            } catch (Exception e) {
                log.warn("网络恢复时提升 CWND 失败: {}", e.getMessage());
            }
        }
        
        // 添加到趋势分析器
        trendAnalyzer.addWindow(lossRate, rttJitter, avgRtt);
        
        // 添加到滑动窗口丢包统计
        lossRateHistory.offer(lossRate);
        if (lossRateHistory.size() > 20) {
            lossRateHistory.poll();
        }
        
        String previousAlgorithmName = currentAlgorithm.getAlgorithmName();
        CongestionControlAlgorithm selectedAlgorithm = selectOptimalAlgorithm(
                lossRate, rttJitter, avgRtt, rttVariation, bandwidth);
        
        if (selectedAlgorithm != null && selectedAlgorithm != currentAlgorithm) {
            long now = System.currentTimeMillis();
            
            // **DEBUG：添加详细日志**
            log.info("尝试切换算法 - 当前: {}, 目标: {}", 
                    currentAlgorithm.getAlgorithmName(), selectedAlgorithm.getAlgorithmName());
            
            long timeSinceLastSwitch = now - lastSwitchTime;
            long minInterval = congestionConfig.getMinSwitchInterval();
            if (timeSinceLastSwitch < minInterval) {
                log.info("切换间隔不足（{}ms < {}ms），跳过切换", 
                        timeSinceLastSwitch, minInterval);
                return;
            }
            
            previousAlgorithm = currentAlgorithm;
            currentAlgorithm = selectedAlgorithm;
            lastSwitchTime = now;
            currentAlgorithmStartTime = now;
            
            // **优化：算法状态继承机制**
            // 1. 保存上一个算法的 cwnd
            isWarmingUp = true;
            preWarmupCwnd = previousAlgorithm.getCwnd();
            long previousRate = previousAlgorithm.getRate();
            
            // **修复：从差网络（Reno）切回好网络时，不继承已减半的 cwnd，否则速率卡在约一半（如 10MB/s -> 5MB/s -> 切回仍 5MB/s）**
            // 切到 BBR/CUBIC/Vegas 时至少用 initialCwnd，切到 Reno 时仍继承上一算法 cwnd
            long initialCwndConfig = congestionConfig.getInitialCwnd();
            long cwndToSet = preWarmupCwnd;
            String targetAlgName = selectedAlgorithm.getAlgorithmName();
            if ("BBR".equals(targetAlgName) || "CUBIC".equals(targetAlgName) || "Vegas".equals(targetAlgName)) {
                if (preWarmupCwnd < initialCwndConfig) {
                    cwndToSet = initialCwndConfig;
                    log.info("从保守算法切回激进算法，cwnd 提升至配置初始值: {}字节 ({}MB)，避免速率卡在半速",
                            cwndToSet, String.format("%.2f", cwndToSet / 1024.0 / 1024.0));
                }
            }
            
            // 2. 根据目标算法类型，设置合适的预热期
            int warmupPeriod = congestionConfig.getWarmupRttCount();
            if ("BBR".equals(targetAlgName)) {
                warmupPeriod = Math.max(warmupPeriod, 5);
            }
            
            // **为所有算法设置 cwnd**
            try {
                if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.BBRAlgorithm) {
                    ((com.server.smarttransferserver.congestion.BBRAlgorithm) selectedAlgorithm).setCwnd(cwndToSet);
                } else if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.CubicAlgorithm) {
                    ((com.server.smarttransferserver.congestion.CubicAlgorithm) selectedAlgorithm).setCwnd(cwndToSet);
                } else if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.VegasAlgorithm) {
                    ((com.server.smarttransferserver.congestion.VegasAlgorithm) selectedAlgorithm).setCwnd(cwndToSet);
                } else if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.RenoAlgorithm) {
                    ((com.server.smarttransferserver.congestion.RenoAlgorithm) selectedAlgorithm).setCwnd(preWarmupCwnd);
                }
                
                log.info("算法预热开始 - 新算法: {}, cwnd: {}字节 ({}MB), 预热期: {}个RTT", 
                        targetAlgName, cwndToSet, String.format("%.2f", cwndToSet / 1024.0 / 1024.0), warmupPeriod);
            } catch (Exception e) {
                log.warn("设置新算法cwnd失败: {}, 使用默认值", e.getMessage());
            }
            
            warmupRttCount = warmupPeriod;
            
            // 记录切换历史
            AdaptiveAlgorithmMetrics.AlgorithmSwitchRecord record = 
                    new AdaptiveAlgorithmMetrics.AlgorithmSwitchRecord(
                            previousAlgorithmName,
                            selectedAlgorithm.getAlgorithmName(),
                            "网络条件变化",
                            lossRate,
                            rttJitter,
                            avgRtt
                    );
            metrics.addSwitchRecord(record);
            metrics.setCurrentAlgorithm(selectedAlgorithm.getAlgorithmName());
            
            log.info("自适应算法切换 - {} -> {} (丢包率: {}%, RTT抖动: {}ms, 平均RTT: {}ms)", 
                     previousAlgorithmName, selectedAlgorithm.getAlgorithmName(), 
                     String.format("%.2f", lossRate * 100), rttJitter, 
                     String.format("%.2f", avgRtt));
            
            // 增量重置统计
            resetStatistics(ResetType.INCREMENTAL_RESET);
        }
    }
    
    /**
     * 根据网络特征选择最优算法
     * 综合考虑网络质量、算法历史性能、网络趋势和算法特性
     *
     * @param lossRate 丢包率
     * @param rttJitter RTT抖动
     * @param avgRtt 平均RTT
     * @param rttVariation RTT变化率
     * @param bandwidth 带宽
     * @return 最优算法
     */
    private CongestionControlAlgorithm selectOptimalAlgorithm(double lossRate, long rttJitter, 
                                                              double avgRtt, double rttVariation, long bandwidth) {
        // 网络趋势分析
        NetworkTrendAnalyzer.TrendDirection lossTrend = trendAnalyzer.analyzeLossRateTrend();
        NetworkTrendAnalyzer.TrendDirection rttTrend = trendAnalyzer.analyzeRttTrend();
        NetworkTrendAnalyzer.TrendDirection jitterTrend = trendAnalyzer.analyzeRttJitterTrend();
        
        // 根据趋势调整算法优先级
        // 丢包率持续下降时，优先选择激进型算法（BBR/CUBIC）
        // 丢包率持续上升时，提前切换到保守型算法（Reno）
        if (lossTrend == NetworkTrendAnalyzer.TrendDirection.RISING) {
            log.info("丢包率趋势上升，考虑切换到保守算法");
            if (renoAlgorithm != null && lossRate > congestionConfig.getLossRateThreshold()) {
                return renoAlgorithm;
            }
        } else if (lossTrend == NetworkTrendAnalyzer.TrendDirection.FALLING) {
            log.debug("丢包率趋势下降，优先选择激进算法");
        }
        
        // RTT或抖动持续上升时，也考虑切换到保守算法
        if ((rttTrend == NetworkTrendAnalyzer.TrendDirection.RISING || 
             jitterTrend == NetworkTrendAnalyzer.TrendDirection.RISING) &&
            rttJitter > congestionConfig.getRttJitterThreshold()) {
            log.debug("RTT或抖动趋势上升，网络质量可能恶化");
        }
        // 获取配置阈值
        double lossThreshold = congestionConfig.getLossRateThreshold();
        long jitterThreshold = congestionConfig.getRttJitterThreshold();
        
        // **计算近期丢包率**：用于快速响应网络恢复
        // recentPackets 中 false 表示丢包，统计丢包比例
        long recentLossCount = recentPackets.stream().filter(b -> !b).count();
        double recentLossRate = recentPackets.isEmpty() ? lossRate : (double) recentLossCount / recentPackets.size();
        // 综合丢包率：取累计丢包率和近期丢包率的较小值（更快响应网络恢复）
        double effectiveLossRate = Math.min(lossRate, recentLossRate);
        
        // 判断网络质量等级
        // **修复：使用 effectiveLossRate，让网络恢复时更快切回激进算法**
        // 优秀网络：丢包率<0.5%，RTT抖动<25ms
        boolean isExcellentNetwork = effectiveLossRate < lossThreshold * 0.5 && rttJitter < jitterThreshold * 0.5;
        // 良好网络：丢包率<1%，RTT抖动<50ms
        boolean isGoodNetwork = effectiveLossRate < lossThreshold && rttJitter < jitterThreshold;
        // 差网络：丢包率>2%或RTT抖动>100ms（这里仍用累计丢包率，避免误判）
        boolean isPoorNetwork = lossRate > lossThreshold * 2 || rttJitter > jitterThreshold * 2;
        
        // **修复：增加日志输出，方便调试网络质量判断**
        log.debug("网络质量评估 - 累计丢包: {}%, 近期丢包: {}%, 有效丢包: {}%, RTT抖动: {}ms, 优秀: {}, 良好: {}, 差: {}", 
                 String.format("%.2f", lossRate * 100), 
                 String.format("%.2f", recentLossRate * 100),
                 String.format("%.2f", effectiveLossRate * 100),
                 rttJitter, isExcellentNetwork, isGoodNetwork, isPoorNetwork);
        
        // **关键修复：检测网络质量等级变化，加速响应**
        int currentQualityLevel = isPoorNetwork ? 4 : (isExcellentNetwork ? 1 : (isGoodNetwork ? 2 : 3));
        if (lastNetworkQualityLevel != 0 && currentQualityLevel != lastNetworkQualityLevel) {
            int levelDiff = Math.abs(currentQualityLevel - lastNetworkQualityLevel);
            // 网络质量变化时清理旧数据加速响应
            // 变化 1 级：清理 1/3；变化 2+ 级：清理 2/3
            int clearRatio = levelDiff >= 2 ? 3 : 2; // 分母：2=清一半，3=清1/3
            int clearFraction = levelDiff >= 2 ? 2 : 1; // 分子
            log.info("检测到网络质量变化（{} → {}），清理 {}/{} 旧数据加速响应", 
                    lastNetworkQualityLevel, currentQualityLevel, clearFraction, clearRatio);
            int clearCount = rttSamples.size() * clearFraction / clearRatio;
            for (int i = 0; i < clearCount && !rttSamples.isEmpty(); i++) {
                rttSamples.poll();
            }
            clearCount = recentPackets.size() * clearFraction / clearRatio;
            for (int i = 0; i < clearCount && !recentPackets.isEmpty(); i++) {
                recentPackets.poll();
            }
        }
        lastNetworkQualityLevel = currentQualityLevel;
        
        // 智能算法选择策略（多维度决策）：
        // 1. 优秀网络（低丢包、低抖动）：优先BBR
        // 2. 良好网络：根据延迟和RTT变化选择Vegas或CUBIC
        // 3. 一般网络：优先CUBIC（稳定可靠）
        // 4. 差网络：使用保守的Reno算法
        // 5. 高延迟、低变化网络：Vegas更适合
        String currentAlgName = currentAlgorithm.getAlgorithmName();
        
        // **防御**：avgRtt 为 0 表示无有效 RTT（evaluateAndSwitch 已保证有样本，此处双重保险），不按延迟分支切换
        if (avgRtt <= 0) {
            return currentAlgorithm;
        }
        
        // === 场景1：优秀网络 → BBR ===
        if (isExcellentNetwork && bbrAlgorithm != null) {
            if (!currentAlgName.equals("BBR")) {
                log.info("优秀网络条件（有效丢包: {}%, 近期丢包: {}%, 抖动: {}ms），切换到BBR - 当前: {}", 
                        String.format("%.2f", effectiveLossRate * 100), 
                        String.format("%.2f", recentLossRate * 100),
                        rttJitter, currentAlgName);
                return bbrAlgorithm;
            }
            return bbrAlgorithm;
        }
        
        // === 场景4：差网络 → Reno ===
        // 条件：丢包率>2% 或 RTT抖动>100ms
        if (isPoorNetwork && renoAlgorithm != null) {
            if (!currentAlgName.equals("Reno")) {
                log.info("网络质量差（丢包: {}%, 抖动: {}ms），切换到保守的Reno算法 - 当前算法: {}", 
                        String.format("%.2f", lossRate * 100), rttJitter, currentAlgName);
                return renoAlgorithm;
            }
            // 如果当前已经是Reno，继续使用
            return renoAlgorithm;
        }
        
        // === 场景3：良好 + 高延迟 → Vegas ===
        if (isGoodNetwork && avgRtt > 100 && vegasAlgorithm != null) {
            if (!currentAlgName.equals("Vegas")) {
                log.info("高延迟网络（RTT: {}ms，丢包: {}%），切换到Vegas - 当前: {}", 
                        String.format("%.2f", avgRtt), String.format("%.2f", lossRate * 100), currentAlgName);
                return vegasAlgorithm;
            }
            return vegasAlgorithm;
        }
        
        // === 场景2：良好 + 中低延迟 → CUBIC ===
        if (isGoodNetwork && avgRtt <= 100 && cubicAlgorithm != null) {
            if (!currentAlgName.equals("CUBIC")) {
                log.info("良好网络（RTT: {}ms，丢包: {}%），切换到CUBIC - 当前: {}", 
                        String.format("%.2f", avgRtt), String.format("%.2f", lossRate * 100), currentAlgName);
                return cubicAlgorithm;
            }
            return cubicAlgorithm;
        }
        
        // === 一般网络：按延迟选 Vegas 或 CUBIC ===
        if (avgRtt > 120 && vegasAlgorithm != null && !currentAlgName.equals("Vegas")) {
            log.info("一般网络高延迟（RTT: {}ms），切换到Vegas - 当前: {}", String.format("%.2f", avgRtt), currentAlgName);
            return vegasAlgorithm;
        }
        if (cubicAlgorithm != null && !currentAlgName.equals("CUBIC")) {
            log.info("一般网络条件，切换到CUBIC - 当前: {}", currentAlgName);
            return cubicAlgorithm;
        }
        
        return currentAlgorithm;
    }
    
    /**
     * 重置统计信息
     *
     * @param type 重置类型
     */
    private void resetStatistics(ResetType type) {
        if (type == ResetType.FULL_RESET) {
            // 全量重置：清空所有数据
            totalPackets = 0;
            lostPackets = 0;
            rttSamples.clear();
            preservedRttSamples.clear();
            lossRateHistory.clear();
            recentPackets.clear(); // **新增：重置滑动窗口**
            lastEvaluationTime = System.currentTimeMillis();
            lastSwitchTime = 0;
            currentAlgorithmStartTime = System.currentTimeMillis();
            trendAnalyzer.reset();
        } else {
            // 增量重置（算法切换）：保留20%的RTT历史样本；不重置 lostPackets，保证界面丢包率 = 总丢包/总分片 不回落
            int preserveCount = (int) (rttSamples.size() * 0.2);
            preservedRttSamples.clear();
            List<Long> sorted = new LinkedList<>(rttSamples);
            sorted.sort(Long::compareTo);
            for (int i = sorted.size() - preserveCount; i < sorted.size(); i++) {
                if (i >= 0) {
                    preservedRttSamples.offer(sorted.get(i));
                }
            }
            
            totalPackets = 0;
            rttSamples.clear();
            recentPackets.clear();
            
            while (!preservedRttSamples.isEmpty()) {
                rttSamples.offer(preservedRttSamples.poll());
            }
        }
    }
    
    /**
     * 获取RTT抖动（供外部调用）
     *
     * @return RTT抖动值
     */
    public long getRttJitter() {
        return calculateRttJitter();
    }
    
    /**
     * 获取网络趋势（供外部调用）
     *
     * @return 网络趋势描述
     */
    public String getNetworkTrend() {
        if (trendAnalyzer == null || trendAnalyzer.getWindowCount() < 2) {
            return "平稳";
        }
        
        NetworkTrendAnalyzer.TrendDirection lossTrend = trendAnalyzer.analyzeLossRateTrend();
        NetworkTrendAnalyzer.TrendDirection rttTrend = trendAnalyzer.analyzeRttTrend();
        
        if (lossTrend == NetworkTrendAnalyzer.TrendDirection.RISING || 
            rttTrend == NetworkTrendAnalyzer.TrendDirection.RISING) {
            return "上升";
        } else if (lossTrend == NetworkTrendAnalyzer.TrendDirection.FALLING || 
                   rttTrend == NetworkTrendAnalyzer.TrendDirection.FALLING) {
            return "下降";
        } else {
            return "平稳";
        }
    }
    
    /**
     * 获取算法预热状态（供外部调用）
     *
     * @return 是否正在预热
     */
    public boolean isWarmingUp() {
        return isWarmingUp;
    }
    
    /**
     * 计算RTT抖动（标准差）
     * 使用异常值过滤后的数据
     *
     * @return RTT抖动值
     */
    private long calculateRttJitter() {
        if (rttSamples.isEmpty()) {
            return 0;
        }
        
        // 使用异常值过滤器
        List<Long> filtered = outlierFilter.filterOutliers(rttSamples);
        if (filtered.isEmpty()) {
            return 0;
        }
        
        // 计算平均值
        double avg = filtered.stream().mapToLong(Long::longValue).average().orElse(0);
        
        // 计算标准差
        double variance = filtered.stream()
                .mapToDouble(rtt -> Math.pow(rtt - avg, 2))
                .average()
                .orElse(0);
        
        return (long) Math.sqrt(variance);
    }
    
    @Override
    public long getCwnd() {
        return currentAlgorithm.getCwnd();
    }
    
    @Override
    public long getRate() {
        return currentAlgorithm.getRate();
    }
    
    @Override
    public CongestionState getState() {
        return currentAlgorithm.getState();
    }
    
    @Override
    public String getAlgorithmName() {
        // 直接返回当前使用的子算法名称（CUBIC、BBR、Vegas、Reno）
        // 而不是返回 "Adaptive(XXX)" 格式，保持前后端一致
        return currentAlgorithm.getAlgorithmName();
    }
    
    @Override
    public long getSsthresh() {
        return currentAlgorithm.getSsthresh();
    }
    
    @Override
    public long getRtt() {
        return currentAlgorithm.getRtt();
    }
    
    @Override
    public void reset() {
        if (renoAlgorithm != null) {
            renoAlgorithm.reset();
        }
        if (vegasAlgorithm != null) {
            vegasAlgorithm.reset();
        }
        if (cubicAlgorithm != null) {
            cubicAlgorithm.reset();
        }
        if (bbrAlgorithm != null) {
            bbrAlgorithm.reset();
        }
        initialize();
    }
    
    /**
     * 获取当前丢包率
     *
     * @return 丢包率
     */
    public double getLossRate() {
        return totalPackets > 0 ? (double) lostPackets / totalPackets : 0;
    }
    
    /**
     * 获取当前滑动窗口丢包率（用于算法决策）
     *
     * @return 最近窗口内的丢包率 [0, 1]
     */
    public double getCurrentLossRate() {
        return currentLossRate;
    }

    /** 用于估算「传播 RTT」时的典型分片大小（5MB），与前端 CHUNK_SIZE 一致 */
    /**
     * 用于界面显示的传播时延：只计算一次单向时延（ms），与 Clumsy 的「延迟」一致（配 50ms 即显示 50ms）。
     * rttSamples 存储的已是传播 RTT（往返，已减去传输时间），直接取平均后除以 2 得到单向。
     *
     * @return 单向传播时延（ms），无样本时返回 0
     */
    public long getDisplayRtt() {
        if (rttSamples == null || rttSamples.isEmpty()) {
            return 0;
        }
        double avg = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        long avgMs = (long) Math.round(avg);
        // rttSamples 已是传播 RTT（往返），除以 2 得到单向传播时延
        return avgMs / 2;
    }

    /**
     * 设置本任务总分片数（由上传服务在每次分片时注入，来自 init 时的 totalChunks）。
     * 用于界面丢包率 = 总丢包数/总分片数，避免前几个包丢 1 个就暴增到 20%。
     */
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    /**
     * 用于界面显示的丢包率：统一为「总丢包数/总分片数」，整次传输内累计、不随算法切换或近期无丢包而回落。
     * totalChunks 未知时返回 0。
     *
     * @return 丢包率 [0, 1]
     */
    public double getDisplayLossRate() {
        if (totalChunks <= 0) {
            return 0;
        }
        double rate = (double) lostPackets / totalChunks;
        return Math.min(1.0, Math.max(0.0, rate));
    }
    
    /**
     * 获取当前使用的算法名称
     *
     * @return 算法名称
     */
    public String getCurrentAlgorithmName() {
        return currentAlgorithm.getAlgorithmName();
    }
    
    /**
     * 获取算法运行指标（用于可观测性）
     *
     * @return 算法指标
     */
    public AdaptiveAlgorithmMetrics getMetrics() {
        // 更新当前指标
        metrics.setCurrentAlgorithm(currentAlgorithm.getAlgorithmName());
        
        // 更新网络质量（与前端显示、算法切换一致，均用 getDisplayLossRate()）
        NetworkScenario scenario = NetworkScenario.fromMetrics(
                getDisplayLossRate(),
                calculateRttJitter()
        );
        metrics.setNetworkQuality(scenario.getDescription());
        
        // 更新当前网络指标（**修复：过滤null值**）
        double lossRate = totalPackets > 0 ? (double) lostPackets / totalPackets : 0;
        long rttJitter = calculateRttJitter();
        double avgRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        long minRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .min()
                .orElse(0);
        long maxRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
        double rttVariation = maxRtt > 0 ? (double) (maxRtt - minRtt) / maxRtt : 0;
        long bandwidth = currentAlgorithm.getRate();
        long bdp = bandwidth * (long) avgRtt / 1000;
        
        AdaptiveAlgorithmMetrics.NetworkMetrics networkMetrics = 
                new AdaptiveAlgorithmMetrics.NetworkMetrics(
                        lossRate, rttJitter, avgRtt, rttVariation, bandwidth, bdp
                );
        metrics.setCurrentMetrics(networkMetrics);
        
        return metrics;
    }
}

