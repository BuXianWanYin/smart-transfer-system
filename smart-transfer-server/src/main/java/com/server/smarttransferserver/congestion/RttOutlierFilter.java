package com.server.smarttransferserver.congestion;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * RTT异常值过滤器
 * 使用四分位法过滤异常值
 */
@Slf4j
public class RttOutlierFilter {
    
    /**
     * 是否启用过滤
     */
    private final boolean enabled;
    
    /**
     * 构造方法
     *
     * @param enabled 是否启用
     */
    public RttOutlierFilter(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 过滤异常值
     * 使用四分位法：过滤超出 [Q1-1.5IQR, Q3+1.5IQR] 范围的数据
     *
     * @param rttSamples RTT样本队列
     * @return 过滤后的RTT样本列表
     */
    public List<Long> filterOutliers(Queue<Long> rttSamples) {
        if (!enabled || rttSamples.isEmpty()) {
            // **修复：过滤null值后返回**
            List<Long> result = new ArrayList<>();
            for (Long rtt : rttSamples) {
                if (rtt != null && rtt > 0) {
                    result.add(rtt);
                }
            }
            return result;
        }
        
        // **修复：先过滤null值和无效值**
        List<Long> validSamples = new ArrayList<>();
        for (Long rtt : rttSamples) {
            if (rtt != null && rtt > 0) {
                validSamples.add(rtt);
            }
        }
        
        if (validSamples.size() < 4) {
            // 样本太少，不进行过滤
            return validSamples;
        }
        
        // 转换为列表并排序
        List<Long> sorted = new ArrayList<>(validSamples);
        Collections.sort(sorted);
        
        // 计算四分位数
        int n = sorted.size();
        double q1Index = (n + 1) * 0.25;
        double q3Index = (n + 1) * 0.75;
        
        long q1 = getPercentile(sorted, q1Index);
        long q3 = getPercentile(sorted, q3Index);
        
        // 计算IQR（四分位距）
        long iqr = q3 - q1;
        
        // 计算上下限
        long lowerBound = (long) (q1 - 1.5 * iqr);
        long upperBound = (long) (q3 + 1.5 * iqr);
        
        // 过滤异常值
        List<Long> filtered = new ArrayList<>();
        int outlierCount = 0;
        for (Long rtt : sorted) {
            if (rtt >= lowerBound && rtt <= upperBound) {
                filtered.add(rtt);
            } else {
                outlierCount++;
            }
        }
        
        if (outlierCount > 0) {
            log.debug("RTT异常值过滤 - 原始样本: {}, 过滤后: {}, 异常值: {}, Q1: {}, Q3: {}, IQR: {}", 
                     n, filtered.size(), outlierCount, q1, q3, iqr);
        }
        
        return filtered;
    }
    
    /**
     * 获取百分位数
     *
     * @param sorted 已排序的列表
     * @param index 索引（可以是小数）
     * @return 百分位数值
     */
    private long getPercentile(List<Long> sorted, double index) {
        if (index <= 0) {
            return sorted.get(0);
        }
        if (index >= sorted.size()) {
            return sorted.get(sorted.size() - 1);
        }
        
        int lower = (int) Math.floor(index) - 1;
        int upper = (int) Math.ceil(index) - 1;
        
        if (lower < 0) {
            lower = 0;
        }
        if (upper >= sorted.size()) {
            upper = sorted.size() - 1;
        }
        
        if (lower == upper) {
            return sorted.get(lower);
        }
        
        // 线性插值
        double fraction = index - Math.floor(index);
        return (long) (sorted.get(lower) * (1 - fraction) + sorted.get(upper) * fraction);
    }
}
