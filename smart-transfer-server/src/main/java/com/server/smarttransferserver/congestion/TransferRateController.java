package com.server.smarttransferserver.congestion;

import com.google.common.util.concurrent.RateLimiter;
import com.server.smarttransferserver.config.CongestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 传输速率控制器
 * 使用Guava的RateLimiter实现速率限制
 */
@Slf4j
@Component
public class TransferRateController {
    
    /**
     * 速率限制器
     */
    private RateLimiter rateLimiter;
    
    /**
     * 当前速率（字节/秒）
     */
    private double currentRate;
    
    /**
     * 拥塞控制算法
     */
    private CongestionControlAlgorithm algorithm;
    
    /**
     * 网络监测服务
     */
    private final NetworkMonitorService networkMonitor;
    
    /**
     * 拥塞控制配置
     */
    @Autowired
    private CongestionConfig congestionConfig;
    
    /**
     * 构造方法
     */
    @Autowired
    public TransferRateController(NetworkMonitorService networkMonitor, CongestionConfig congestionConfig) {
        this.networkMonitor = networkMonitor;
        this.congestionConfig = congestionConfig;
        this.currentRate = congestionConfig.getMinRate();
        this.rateLimiter = RateLimiter.create(currentRate);
    }
    
    /**
     * 设置拥塞控制算法
     *
     * @param algorithm 算法实例
     */
    public void setAlgorithm(CongestionControlAlgorithm algorithm) {
        this.algorithm = algorithm;
        log.info("设置拥塞控制算法: {}", algorithm.getAlgorithmName());
    }
    
    /**
     * 申请发送许可
     * 发送数据前必须调用此方法获取许可
     *
     * @param bytes 要发送的字节数
     * @return 等待时间（秒）
     */
    public double acquire(long bytes) {
        if (rateLimiter == null) {
            return 0;
        }
        
        // 申请许可，会阻塞直到获得许可
        double waitTime = rateLimiter.acquire((int) bytes);
        
        log.debug("申请发送许可 - 字节数: {}, 等待时间: {}秒", bytes, waitTime);
        
        return waitTime;
    }
    
    /**
     * 尝试申请发送许可（非阻塞）
     *
     * @param bytes 要发送的字节数
     * @return 是否获得许可
     */
    public boolean tryAcquire(long bytes) {
        if (rateLimiter == null) {
            return true;
        }
        
        return rateLimiter.tryAcquire((int) bytes);
    }
    
    /**
     * 根据拥塞控制算法更新速率
     */
    public void updateRate() {
        if (algorithm == null) {
            return;
        }
        
        // 从算法获取新速率
        long newRate = algorithm.getRate();
        
        // 限制速率范围（从配置获取）
        newRate = Math.max(congestionConfig.getMinRate(), Math.min(newRate, congestionConfig.getMaxRate()));
        
        // 更新速率限制器
        if (newRate != currentRate) {
            currentRate = newRate;
            rateLimiter.setRate(currentRate);
            
            log.info("更新传输速率 - 新速率: {}字节/秒 ({}MB/s), 算法: {}, 状态: {}", 
                     currentRate, 
                     String.format("%.2f", currentRate / (1024.0 * 1024.0)),
                     algorithm.getAlgorithmName(),
                     algorithm.getState().getDescription());
        }
    }
    
    /**
     * 手动设置速率
     *
     * @param rate 速率（字节/秒）
     */
    public void setRate(double rate) {
        rate = Math.max(congestionConfig.getMinRate(), Math.min(rate, congestionConfig.getMaxRate()));
        this.currentRate = rate;
        
        if (rateLimiter == null) {
            rateLimiter = RateLimiter.create(rate);
        } else {
            rateLimiter.setRate(rate);
        }
        
        log.info("手动设置传输速率: {}字节/秒 ({}MB/s)", 
                 rate, 
                 String.format("%.2f", rate / (1024.0 * 1024.0)));
    }
    
    /**
     * 获取当前速率
     *
     * @return 当前速率（字节/秒）
     */
    public double getCurrentRate() {
        return currentRate;
    }
    
    /**
     * 获取当前速率（MB/s）
     *
     * @return 速率（MB/s）
     */
    public double getCurrentRateMBps() {
        return currentRate / (1024.0 * 1024.0);
    }
    
    /**
     * 暂停速率控制
     */
    public void pause() {
        if (rateLimiter != null) {
            rateLimiter.setRate(0.1); // 设置为极低速率
            log.info("传输速率控制已暂停");
        }
    }
    
    /**
     * 恢复速率控制
     */
    public void resume() {
        if (rateLimiter != null && currentRate > 0) {
            rateLimiter.setRate(currentRate);
            log.info("传输速率控制已恢复: {}字节/秒", currentRate);
        }
    }
    
    /**
     * 重置速率控制器
     */
    public void reset() {
        this.currentRate = congestionConfig.getMinRate();
        this.rateLimiter = RateLimiter.create(currentRate);
        log.info("速率控制器已重置");
    }
    
    /**
     * 获取速率利用率
     *
     * @return 利用率（0-1）
     */
    public double getUtilization() {
        long estimatedBandwidth = networkMonitor.getEstimatedBandwidth();
        if (estimatedBandwidth == 0) {
            return 0;
        }
        return currentRate / estimatedBandwidth;
    }
}

