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
        // 用于评估/显示的 RTT 用传播 RTT（与 Clumsy 配置一致）
        long rttForSample = propagationRttMs != null ? propagationRttMs : fullRttMs;
        if (rttForSample > 0 && rttForSample < 10000) {
            rttSamples.offer(rttForSample);
            if (rttSamples.size() > EVALUATION_WINDOW) {
                rttSamples.poll();
            }
        }
        
        totalPackets++;
        // Clumsy 在 TCP 层丢包时应用层无重试，retryCount 恒为 0；用 RTT 突增推断丢包/重传，保证差网络时能切出 BBR 到 Reno
        boolean inferredLoss = false;
        if (rttSamples.size() >= 3 && rttForSample > 0) {
            double avg = rttSamples.stream().mapToLong(Long::longValue).average().orElse(0);
            if (avg > 0 && rttForSample >= 1.8 * avg) {
                inferredLoss = true;
                if (log.isDebugEnabled()) {
                    log.debug("RTT突增推断丢包 - 当前RTT: {}ms, 近期平均: {}ms", rttForSample, String.format("%.0f", avg));
                }
            }
        }
        recentPackets.offer(inferredLoss ? false : true);
        if (recentPackets.size() > RECENT_WINDOW_SIZE) {
            recentPackets.poll();
        }
        
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
            // 快速计算当前丢包率
            double quickLossRate = recentPackets.size() >= 5 
                ? (double) recentPackets.stream().filter(p -> !p).count() / recentPackets.size()
                : (totalPackets > 0 ? (double) lostPackets / totalPackets : 0);
            long quickJitter = calculateRttJitter();
            
            // 判断当前网络质量
            double lossThreshold = congestionConfig.getLossRateThreshold();
            long jitterThreshold = congestionConfig.getRttJitterThreshold();
            boolean nowPoor = quickLossRate > lossThreshold * 2 || quickJitter > jitterThreshold * 2;
            boolean nowExcellent = quickLossRate < lossThreshold * 0.5 && quickJitter < jitterThreshold * 0.5;
            
            // 如果网络状况与当前算法严重不匹配，提前结束预热
            String currentAlgName = currentAlgorithm.getAlgorithmName();
            boolean mismatch = (nowPoor && !"Reno".equals(currentAlgName)) 
                            || (nowExcellent && !"BBR".equals(currentAlgName));
            
            if (mismatch) {
                log.info("网络状况与当前算法不匹配，提前结束预热 - 算法: {}, 丢包: {}%, 抖动: {}ms", 
                        currentAlgName, String.format("%.2f", quickLossRate * 100), quickJitter);
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
        
        // **修复：使用滑动窗口计算丢包率（快速响应网络变化）**
        double lossRate;
        if (recentPackets.size() >= 5) {
            // 滑动窗口有 5 个样本即可开始使用（约 25MB）
            long recentLost = recentPackets.stream().filter(p -> !p).count();
            lossRate = (double) recentLost / recentPackets.size();
        } else {
            // 样本不足，使用累积数据
            lossRate = totalPackets > 0 ? (double) lostPackets / totalPackets : 0;
        }
        long rttJitter = calculateRttJitter();
        
        // **更新成员变量，供算法切换决策使用**
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
            
            // **优化：算法状态继承机制（统一处理所有算法）**
            // 1. 保存上一个算法的关键状态
            isWarmingUp = true;
            preWarmupCwnd = previousAlgorithm.getCwnd();
            long previousRate = previousAlgorithm.getRate();
            
            // 2. 根据目标算法类型，设置合适的预热期
            int warmupPeriod = congestionConfig.getWarmupRttCount();
            String targetAlgName = selectedAlgorithm.getAlgorithmName();
            
            // BBR需要更长的预热期（至少5个RTT来测量带宽）
            if ("BBR".equals(targetAlgName)) {
                warmupPeriod = Math.max(warmupPeriod, 5);
            }
            
            // **修复：为所有算法设置cwnd（使用反射或类型判断）**
            try {
                // 尝试使用setCwnd方法（所有算法现在都有这个方法）
                if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.BBRAlgorithm) {
                    ((com.server.smarttransferserver.congestion.BBRAlgorithm) selectedAlgorithm).setCwnd(preWarmupCwnd);
                } else if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.CubicAlgorithm) {
                    ((com.server.smarttransferserver.congestion.CubicAlgorithm) selectedAlgorithm).setCwnd(preWarmupCwnd);
                } else if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.VegasAlgorithm) {
                    ((com.server.smarttransferserver.congestion.VegasAlgorithm) selectedAlgorithm).setCwnd(preWarmupCwnd);
                } else if (selectedAlgorithm instanceof com.server.smarttransferserver.congestion.RenoAlgorithm) {
                    ((com.server.smarttransferserver.congestion.RenoAlgorithm) selectedAlgorithm).setCwnd(preWarmupCwnd);
                }
                
                log.info("算法预热开始 - 新算法: {}, 继承cwnd: {}字节 ({}MB), 预热期: {}个RTT", 
                        targetAlgName, preWarmupCwnd, String.format("%.2f", preWarmupCwnd / 1024.0 / 1024.0), warmupPeriod);
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
        
        // 判断网络质量等级
        // **修复：调整网络质量判断标准，使其更容易识别优秀网络**
        // 优秀网络：丢包率<0.5%，RTT抖动<25ms（更宽松的条件）
        boolean isExcellentNetwork = lossRate < lossThreshold * 0.5 && rttJitter < jitterThreshold * 0.5;
        // 良好网络：丢包率<1%，RTT抖动<50ms
        boolean isGoodNetwork = lossRate < lossThreshold && rttJitter < jitterThreshold;
        // 差网络：丢包率>2%或RTT抖动>100ms
        boolean isPoorNetwork = lossRate > lossThreshold * 2 || rttJitter > jitterThreshold * 2;
        
        // **修复：增加日志输出，方便调试网络质量判断**
        log.debug("网络质量评估 - 丢包率: {}%, RTT抖动: {}ms, 平均RTT: {}ms, 优秀: {}, 良好: {}, 差: {}", 
                 String.format("%.2f", lossRate * 100), rttJitter, String.format("%.2f", avgRtt),
                 isExcellentNetwork, isGoodNetwork, isPoorNetwork);
        
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
        // 1. 优秀网络（低丢包、低抖动）：优先BBR，但考虑历史性能
        // 2. 良好网络：根据延迟和RTT变化选择Vegas或CUBIC
        // 3. 一般网络：优先CUBIC（稳定可靠）
        // 4. 差网络：使用保守的Reno算法
        // 5. 高延迟、低变化网络：Vegas更适合
        String currentAlgName = currentAlgorithm.getAlgorithmName();
        
        // 选算法完全按网络条件，不看评分（评分仅用于回滚与观测）
        
        // === 场景1：优秀网络 → BBR ===
        if (isExcellentNetwork && bbrAlgorithm != null) {
            if (!currentAlgName.equals("BBR")) {
                log.info("优秀网络条件（丢包: {}%, 抖动: {}ms），切换到BBR - 当前: {}", 
                        String.format("%.2f", lossRate * 100), rttJitter, currentAlgName);
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
            // 增量重置：保留20%的RTT历史样本
            int preserveCount = (int) (rttSamples.size() * 0.2);
            preservedRttSamples.clear();
            List<Long> sorted = new LinkedList<>(rttSamples);
            sorted.sort(Long::compareTo);
            for (int i = sorted.size() - preserveCount; i < sorted.size(); i++) {
                if (i >= 0) {
                    preservedRttSamples.offer(sorted.get(i));
                }
            }
            
            // 清空当前统计，但保留历史
            totalPackets = 0;
            lostPackets = 0;
            rttSamples.clear();
            recentPackets.clear(); // **新增：重置滑动窗口**
            
            // 将保留的样本重新加入
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
     * 用于界面显示的丢包率：使用滑动窗口丢包率（与算法决策一致），能快速反映网络状况变化。
     * 不再使用累积丢包率（分母太大，无法体现短期网络质量变化）。
     *
     * @return 丢包率 [0, 1]，无数据时返回 0
     */
    public double getDisplayLossRate() {
        // **修复：使用滑动窗口丢包率，与算法决策逻辑一致**
        if (recentPackets.size() >= 5) {
            // 滑动窗口有 5 个样本即可开始使用（约 25MB）
            long recentLost = recentPackets.stream().filter(p -> !p).count();
            double rate = (double) recentLost / recentPackets.size();
            return Math.min(1.0, Math.max(0.0, rate));
        } else {
            // 样本不足，使用累积数据
            long total = totalPackets + lostPackets;
            if (total <= 0) {
                return 0;
            }
            double rate = (double) lostPackets / total;
            return Math.min(1.0, Math.max(0.0, rate));
        }
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
        
        // 更新网络质量
        NetworkScenario scenario = NetworkScenario.fromMetrics(
                totalPackets > 0 ? (double) lostPackets / totalPackets : 0,
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

