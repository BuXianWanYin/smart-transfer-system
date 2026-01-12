package com.server.smarttransferserver.task;

import com.server.smarttransferserver.mapper.CongestionMetricsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 拥塞指标数据清理定时任务
 * 定期清理过期的历史指标数据，避免数据量过大
 */
@Slf4j
@Component
public class CongestionMetricsCleanupTask {

    @Autowired
    private CongestionMetricsMapper metricsMapper;

    /**
     * 定期清理过期的拥塞指标数据
     * 每天凌晨3点执行，删除30天前的历史数据
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void cleanupOldMetrics() {
        log.info("开始清理过期的拥塞指标数据...");
        try {
            // 删除30天前的历史数据
            int deletedCount = metricsMapper.deleteOldMetrics(30);
            log.info("拥塞指标数据清理完成，删除了 {} 条记录", deletedCount);
        } catch (Exception e) {
            log.error("清理拥塞指标数据失败", e);
        }
    }
}
