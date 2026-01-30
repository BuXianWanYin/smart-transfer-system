package com.server.smarttransferserver.congestion;

import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * 自适应拥塞控制算法
 * 根据网络质量动态选择Reno、Vegas、CUBIC或BBR
 */
@Slf4j
@Component
public class AdaptiveAlgorithm implements CongestionControlAlgorithm {
    
    /**
     * 评估窗口大小（固定值）
     */
    private static final int EVALUATION_WINDOW = 100;
    
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
     * 算法性能统计（吞吐量、延迟等）
     * 键：算法名称，值：性能评分（0-100，越高越好）
     */
    private final Map<String, Double> algorithmScores;
    
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
     * 切换前的算法得分（用于回滚判断）
     */
    private double previousAlgorithmScore;
    
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
        this.algorithmScores = new java.util.HashMap<>();
        this.lastSwitchTime = 0;
        this.currentAlgorithmStartTime = System.currentTimeMillis();
        this.lossRateHistory = new LinkedList<>();
        this.preservedRttSamples = new LinkedList<>();
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
        metrics.setAlgorithmScores(new java.util.HashMap<>(algorithmScores));
        
        log.info("自适应算法初始化 - 默认使用: {}, 趋势窗口: {}, 异常值过滤: {}", 
                currentAlgorithm.getAlgorithmName(), 
                congestionConfig.getTrendWindowSize(),
                congestionConfig.isOutlierFilterEnabled());
    }
    
    @Override
    public void onAck(long ackedBytes, long rtt) {
        // 记录RTT样本（过滤无效值）
        if (rtt > 0 && rtt < 10000) {  // RTT应在1ms-10秒范围内
            rttSamples.offer(rtt);
            if (rttSamples.size() > EVALUATION_WINDOW) {
                rttSamples.poll();
            }
        }
        
        // 统计数据包
        totalPackets++;
        
        // 调用当前算法
        currentAlgorithm.onAck(ackedBytes, rtt);
        
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
        
        // 如果处于预热状态，不进行评估
        if (isWarmingUp) {
            warmupRttCount--;
            if (warmupRttCount <= 0) {
                isWarmingUp = false;
                log.info("算法预热完成 - 当前算法: {}", currentAlgorithm.getAlgorithmName());
            } else {
                return;
            }
        }
        
        // 计算网络指标（**修复：过滤null值**）
        double lossRate = (double) lostPackets / totalPackets;
        long rttJitter = calculateRttJitter();
        double avgRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(100);
        long minRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .min()
                .orElse(100);
        long maxRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .max()
                .orElse(100);
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
        
        // 评估当前算法性能（包含吞吐量）
        evaluateCurrentAlgorithmPerformance(lossRate, rttJitter, avgRtt, bandwidth);
        
        // 检查回滚机制：如果切换后性能下降超过阈值，回滚
        if (previousAlgorithm != null && previousAlgorithmScore > 0) {
            double currentScore = algorithmScores.getOrDefault(currentAlgorithm.getAlgorithmName(), 50.0);
            double performanceDrop = (previousAlgorithmScore - currentScore) / previousAlgorithmScore;
            if (performanceDrop > congestionConfig.getRollbackThreshold()) {
                log.warn("算法性能下降超过阈值（{}%），回滚到上一个算法: {}", 
                        String.format("%.2f", performanceDrop * 100), previousAlgorithm.getAlgorithmName());
                currentAlgorithm = previousAlgorithm;
                lastSwitchTime = System.currentTimeMillis();
                currentAlgorithmStartTime = System.currentTimeMillis();
                previousAlgorithm = null;
                previousAlgorithmScore = 0;
                resetStatistics(ResetType.INCREMENTAL_RESET);
                return;
            }
        }
        
        // 根据网络特征和趋势选择最优算法
        String previousAlgorithmName = currentAlgorithm.getAlgorithmName();
        CongestionControlAlgorithm selectedAlgorithm = selectOptimalAlgorithm(
                lossRate, rttJitter, avgRtt, rttVariation, bandwidth);
        
        if (selectedAlgorithm != null && selectedAlgorithm != currentAlgorithm) {
            long now = System.currentTimeMillis();
            
            // 检查切换间隔
            if (now - lastSwitchTime < congestionConfig.getMinSwitchInterval()) {
                return;
            }
            
            // 置信度阈值检查
            double currentScore = algorithmScores.getOrDefault(currentAlgorithm.getAlgorithmName(), 50.0);
            double selectedScore = algorithmScores.getOrDefault(selectedAlgorithm.getAlgorithmName(), 50.0);
            double scoreDiff = (selectedScore - currentScore) / Math.max(currentScore, 1.0);
            if (scoreDiff < congestionConfig.getConfidenceThreshold()) {
                log.debug("算法得分差异不足置信度阈值（{}%），保持当前算法", 
                         String.format("%.2f", scoreDiff * 100));
                return;
            }
            
            // 执行切换
            previousAlgorithm = currentAlgorithm;
            previousAlgorithmScore = currentScore;
            currentAlgorithm = selectedAlgorithm;
            lastSwitchTime = now;
            currentAlgorithmStartTime = now;
            
            // 算法预热：设置预热状态和初始cwnd
            isWarmingUp = true;
            warmupRttCount = congestionConfig.getWarmupRttCount();
            preWarmupCwnd = previousAlgorithm.getCwnd();
            long newCwnd = (long) (preWarmupCwnd * 0.5);
            // 注意：这里需要根据具体算法实现来设置cwnd，暂时记录日志
            log.info("算法预热开始 - 新算法: {}, 预热前cwnd: {}, 预热初始cwnd: {}", 
                    selectedAlgorithm.getAlgorithmName(), preWarmupCwnd, newCwnd);
            
            // 记录切换历史
            AdaptiveAlgorithmMetrics.AlgorithmSwitchRecord record = 
                    new AdaptiveAlgorithmMetrics.AlgorithmSwitchRecord(
                            previousAlgorithmName,
                            selectedAlgorithm.getAlgorithmName(),
                            "网络条件变化",
                            currentScore,
                            selectedScore,
                            lossRate,
                            rttJitter,
                            avgRtt
                    );
            metrics.addSwitchRecord(record);
            metrics.setCurrentAlgorithm(selectedAlgorithm.getAlgorithmName());
            
            log.info("自适应算法切换 - {} -> {} (丢包率: {}%, RTT抖动: {}ms, 平均RTT: {}ms, 得分提升: {}%)", 
                     previousAlgorithmName, selectedAlgorithm.getAlgorithmName(), 
                     String.format("%.2f", lossRate * 100), rttJitter, 
                     String.format("%.2f", avgRtt), String.format("%.2f", scoreDiff * 100));
            
            // 增量重置统计
            resetStatistics(ResetType.INCREMENTAL_RESET);
        }
    }
    
    /**
     * 评估当前算法的性能
     * 基于丢包率、RTT抖动、延迟、吞吐量等指标计算性能评分
     * 使用动态权重和相对评分
     *
     * @param lossRate 丢包率
     * @param rttJitter RTT抖动
     * @param avgRtt 平均RTT
     * @param bandwidth 带宽（字节/秒）
     */
    private void evaluateCurrentAlgorithmPerformance(double lossRate, long rttJitter, double avgRtt, long bandwidth) {
        String algorithmName = currentAlgorithm.getAlgorithmName();
        
        // 判断网络场景，获取动态权重
        NetworkScenario scenario = NetworkScenario.fromMetrics(lossRate, rttJitter);
        
        // 计算各项得分
        double lossScore = Math.max(0, 100 - lossRate * 10000);
        double jitterScore = Math.max(0, 100 - rttJitter * 2);
        double rttScore = Math.max(0, 100 - (avgRtt - 50) / 10);
        
        // 吞吐量得分（仅优秀网络使用）
        double throughputScore = 0;
        if (scenario == NetworkScenario.EXCELLENT && bandwidth > 0) {
            // 假设理想吞吐量为100MB/s，计算得分
            double idealThroughput = 100 * 1024 * 1024.0;
            throughputScore = Math.min(100, (bandwidth / idealThroughput) * 100);
        }
        
        // 使用动态权重计算综合评分
        double performanceScore = lossScore * scenario.getLossRateWeight() +
                                 jitterScore * scenario.getRttJitterWeight() +
                                 rttScore * scenario.getRttWeight() +
                                 throughputScore * scenario.getThroughputWeight();
        
        // 平滑更新评分（移动平均）
        double previousScore = algorithmScores.getOrDefault(algorithmName, 50.0);
        double smoothedScore = previousScore * 0.7 + performanceScore * 0.3;
        
        // 相对评分：以基准算法为基准
        String baselineAlg = congestionConfig.getBaselineAlgorithm();
        if (!baselineAlg.equals(algorithmName)) {
            double baselineScore = algorithmScores.getOrDefault(baselineAlg, 50.0);
            if (baselineScore > 0) {
                smoothedScore = (smoothedScore / baselineScore) * 100;
            }
        }
        
        algorithmScores.put(algorithmName, smoothedScore);
        
        log.debug("算法性能评估 - {}: 评分={}, 场景={}, 丢包率={}%, RTT抖动={}ms, 平均RTT={}ms, 吞吐量={}MB/s", 
                  algorithmName, String.format("%.2f", smoothedScore), scenario.getDescription(),
                  String.format("%.2f", lossRate * 100), rttJitter, String.format("%.2f", avgRtt),
                  String.format("%.2f", bandwidth / (1024.0 * 1024.0)));
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
        
        // 智能算法选择策略（多维度决策）：
        // 1. 优秀网络（低丢包、低抖动）：优先BBR，但考虑历史性能
        // 2. 良好网络：根据延迟和RTT变化选择Vegas或CUBIC
        // 3. 一般网络：优先CUBIC（稳定可靠）
        // 4. 差网络：使用保守的Reno算法
        // 5. 高延迟、低变化网络：Vegas更适合
        // 6. 考虑算法历史性能评分（如果当前算法表现良好，不轻易切换）
        
        // 获取当前算法性能评分
        double currentScore = algorithmScores.getOrDefault(currentAlgorithm.getAlgorithmName(), 50.0);
        String currentAlgName = currentAlgorithm.getAlgorithmName();
        
        // **优化：基于网络条件的强制切换逻辑（降低评分比较的阻碍）**
        // 目标：在明确的网络条件下，优先根据网络特征选择最适合的算法
        
        // === 场景1：优秀网络 → 强烈推荐BBR ===
        // 条件：丢包率<0.5%，RTT抖动<25ms
        if (isExcellentNetwork && bbrAlgorithm != null) {
            if (!currentAlgName.equals("BBR")) {
                double bdp = bandwidth * avgRtt / 1000.0;  // 带宽时延积（字节）
                double bbrScore = algorithmScores.getOrDefault("BBR", 50.0);
                
                // 降低切换门槛：只要BBR评分不是特别差（>35），就切换
                if (bbrScore > 35.0) {
                    log.info("优秀网络条件（丢包: {}%, 抖动: {}ms），切换到BBR算法 - 当前算法: {}, BBR评分: {}", 
                            String.format("%.2f", lossRate * 100), rttJitter, 
                            currentAlgName, String.format("%.2f", bbrScore));
                    return bbrAlgorithm;
                }
            }
            // 如果当前已经是BBR，继续使用
            return bbrAlgorithm;
        }
        
        // === 场景4：高丢包网络 → 强制使用Reno ===
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
        
        // === 场景3：高延迟网络 → 强烈推荐Vegas ===
        // 条件：良好网络（丢包<1%），RTT>100ms，RTT变化不大
        if (isGoodNetwork && avgRtt > 100 && vegasAlgorithm != null) {
            if (!currentAlgName.equals("Vegas")) {
                double vegasScore = algorithmScores.getOrDefault("Vegas", 50.0);
                
                // 高延迟场景：降低切换门槛，只要Vegas评分>40或当前算法评分<55就切换
                if (vegasScore > 40.0 || currentScore < 55.0) {
                    log.info("高延迟网络（RTT: {}ms，丢包: {}%），切换到Vegas算法 - 当前算法: {}, Vegas评分: {}", 
                            String.format("%.2f", avgRtt), String.format("%.2f", lossRate * 100),
                            currentAlgName, String.format("%.2f", vegasScore));
                    return vegasAlgorithm;
                }
            }
            // 如果当前已经是Vegas，继续使用
            return vegasAlgorithm;
        }
        
        // === 场景2：良好网络（中等延迟）→ 推荐CUBIC ===
        // 条件：良好网络（丢包<1%），RTT<=100ms
        if (isGoodNetwork && avgRtt <= 100 && cubicAlgorithm != null) {
            if (!currentAlgName.equals("CUBIC")) {
                double cubicScore = algorithmScores.getOrDefault("CUBIC", 50.0);
                
                // 降低切换门槛：只要CUBIC评分>40或当前算法评分<55就切换
                if (cubicScore > 40.0 || currentScore < 55.0) {
                    log.info("良好网络（RTT: {}ms，丢包: {}%），切换到CUBIC算法 - 当前算法: {}, CUBIC评分: {}", 
                            String.format("%.2f", avgRtt), String.format("%.2f", lossRate * 100),
                            currentAlgName, String.format("%.2f", cubicScore));
                    return cubicAlgorithm;
                }
            }
            // 如果当前已经是CUBIC，继续使用
            return cubicAlgorithm;
        }
        
        // === 一般网络：根据延迟选择CUBIC或Vegas ===
        // 如果不是明确的优秀/良好/差网络，根据延迟特征选择
        if (avgRtt > 120 && vegasAlgorithm != null) {
            // 较高延迟：倾向Vegas
            double vegasScore = algorithmScores.getOrDefault("Vegas", 50.0);
            if (vegasScore > 45.0 && !currentAlgName.equals("Vegas")) {
                log.info("一般网络（高延迟: {}ms），切换到Vegas算法 - 当前算法: {}", 
                        String.format("%.2f", avgRtt), currentAlgName);
                return vegasAlgorithm;
            }
        }
        
        // 默认：使用CUBIC（稳定可靠的通用算法）
        if (cubicAlgorithm != null && !currentAlgName.equals("CUBIC")) {
            double cubicScore = algorithmScores.getOrDefault("CUBIC", 50.0);
            // 只有在CUBIC评分不太差的情况下才切换
            if (cubicScore > 45.0) {
                log.info("一般网络条件，切换到CUBIC算法 - 当前算法: {}, CUBIC评分: {}", 
                        currentAlgName, String.format("%.2f", cubicScore));
                return cubicAlgorithm;
            }
        }
        
        // 最终兜底：如果当前算法表现还可以（>40分），就保持不变
        if (currentScore > 40.0) {
            log.debug("当前算法性能可接受（评分: {}），暂时保持 - 算法: {}", 
                    String.format("%.2f", currentScore), currentAlgName);
            return currentAlgorithm;
        }
        
        // 如果当前算法评分很差，返回CUBIC作为安全的默认选择
        if (cubicAlgorithm != null) {
            log.info("当前算法性能差（评分: {}），切换到CUBIC作为默认算法", String.format("%.2f", currentScore));
            return cubicAlgorithm;
        }
        
        // 最终兜底：返回当前算法
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
            lastEvaluationTime = System.currentTimeMillis();
            lastSwitchTime = 0;
            currentAlgorithmStartTime = System.currentTimeMillis();
            algorithmScores.clear();
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
        metrics.setAlgorithmScores(new java.util.HashMap<>(algorithmScores));
        
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
                .orElse(100);
        long minRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .min()
                .orElse(100);
        long maxRtt = rttSamples.stream()
                .filter(rtt -> rtt != null && rtt > 0)
                .mapToLong(Long::longValue)
                .max()
                .orElse(100);
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

