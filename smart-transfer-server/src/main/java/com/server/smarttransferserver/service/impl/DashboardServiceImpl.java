package com.server.smarttransferserver.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import com.server.smarttransferserver.domain.SystemActivity;
import com.server.smarttransferserver.service.DashboardService;
import com.server.smarttransferserver.service.SystemActivityService;
import com.server.smarttransferserver.service.TransferHistoryService;
import com.server.smarttransferserver.service.UserService;
import com.server.smarttransferserver.vo.DashboardVO;
import com.server.smarttransferserver.vo.UserInfoVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 仪表盘服务实现类
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TransferHistoryService transferHistoryService;
    
    @Autowired
    private SystemActivityService systemActivityService;
    
    /**
     * 获取仪表盘完整数据
     *
     * @return 仪表盘数据
     */
    @Override
    public DashboardVO getDashboardData() {
        try {
            // 获取KPI数据
            DashboardVO.KpiData kpiData = getKpiData();
            
            // 获取用户统计数据
            DashboardVO.UserStats userStats = getUserStats();
            
            // 获取存储统计数据
            DashboardVO.StorageStats storageStats = getStorageStats();
            
            // 获取传输趋势数据（月度）
            DashboardVO.TransferTrend transferTrend = getTransferTrend();
            
            // 获取最近动态
            List<SystemActivity> recentActivities = systemActivityService.getRecentActivities(10);
            
            return DashboardVO.builder()
                    .kpiData(kpiData)
                    .userStats(userStats)
                    .storageStats(storageStats)
                    .transferTrend(transferTrend)
                    .recentActivities(recentActivities)
                    .build();
        } catch (Exception e) {
            log.error("获取仪表盘数据失败", e);
            throw new RuntimeException("获取仪表盘数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取KPI数据
     */
    private DashboardVO.KpiData getKpiData() {
        // 获取用户列表
        List<UserInfoVO> userList = userService.getUserList();
        int totalUsers = userList.size();
        
        // 获取系统存储统计
        Map<String, Object> storageStatsMap = userService.getSystemStorageStats();
        Long totalStorage = ((Number) storageStatsMap.getOrDefault("totalSize", 0L)).longValue();
        Long totalFiles = ((Number) storageStatsMap.getOrDefault("fileCount", 0L)).longValue();
        
        // 获取今日传输量（使用日统计）
        Map<String, Object> todayStats = transferHistoryService.getTransferStats("day", null);
        List<Long> uploadValues = (List<Long>) todayStats.get("uploadValues");
        List<Long> downloadValues = (List<Long>) todayStats.get("downloadValues");
        
        // 计算今日传输量（今天的上传+下载）
        Long todayTransfer = 0L;
        if (uploadValues != null && !uploadValues.isEmpty()) {
            todayTransfer += uploadValues.get(uploadValues.size() - 1);
        }
        if (downloadValues != null && !downloadValues.isEmpty()) {
            todayTransfer += downloadValues.get(downloadValues.size() - 1);
        }
        
        // TODO: 计算变化百分比（需要历史数据对比）
        // 这里先使用模拟数据，后续可以实现真实的对比逻辑
        
        return DashboardVO.KpiData.builder()
                .totalUsers(totalUsers)
                .totalStorage(totalStorage)
                .totalFiles(totalFiles)
                .todayTransfer(todayTransfer)
                .usersChangePercent("+12%")
                .storageChangePercent("+8%")
                .filesChangePercent("-2%")
                .transferChangePercent("+20%")
                .build();
    }
    
    /**
     * 获取用户统计数据
     */
    private DashboardVO.UserStats getUserStats() {
        List<UserInfoVO> userList = userService.getUserList();
        
        // 统计用户数据
        int adminCount = 0;
        int userCount = 0;
        int enabledUsers = 0;
        int disabledUsers = 0;
        int activeUsers = 0;
        int monthlyNewUsers = 0;
        
        LocalDateTime oneMonthAgo = LocalDateTime.now().minus(1, ChronoUnit.MONTHS);
        
        for (UserInfoVO user : userList) {
            // 角色统计
            if ("ADMIN".equals(user.getRole())) {
                adminCount++;
            } else {
                userCount++;
            }
            
            // 状态统计
            if (user.getStatus() != null && user.getStatus() == 1) {
                enabledUsers++;
            } else {
                disabledUsers++;
            }
            
            // 活跃用户统计（有最后登录时间）
            if (user.getLastLoginTime() != null) {
                activeUsers++;
            }
            
            // 本月新增用户统计
            if (user.getCreateTime() != null) {
                LocalDateTime createTime = LocalDateTime.ofInstant(
                    user.getCreateTime().toInstant(), 
                    java.time.ZoneId.systemDefault()
                );
                if (createTime.isAfter(oneMonthAgo)) {
                    monthlyNewUsers++;
                }
            }
        }
        
        return DashboardVO.UserStats.builder()
                .adminCount(adminCount)
                .userCount(userCount)
                .enabledUsers(enabledUsers)
                .disabledUsers(disabledUsers)
                .totalUsers(userList.size())
                .activeUsers(activeUsers)
                .monthlyNewUsers(monthlyNewUsers)
                .changePercent("+23%")
                .newUsersChangePercent("+30%")
                .build();
    }
    
    /**
     * 获取存储统计数据
     */
    private DashboardVO.StorageStats getStorageStats() {
        Map<String, Object> storageStatsMap = userService.getSystemStorageStats();
        
        return DashboardVO.StorageStats.builder()
                .totalSize(((Number) storageStatsMap.getOrDefault("totalSize", 0L)).longValue())
                .fileCount(((Number) storageStatsMap.getOrDefault("fileCount", 0L)).longValue())
                .imageSize(((Number) storageStatsMap.getOrDefault("imageSize", 0L)).longValue())
                .imageCount(((Number) storageStatsMap.getOrDefault("imageCount", 0L)).longValue())
                .videoSize(((Number) storageStatsMap.getOrDefault("videoSize", 0L)).longValue())
                .videoCount(((Number) storageStatsMap.getOrDefault("videoCount", 0L)).longValue())
                .audioSize(((Number) storageStatsMap.getOrDefault("audioSize", 0L)).longValue())
                .audioCount(((Number) storageStatsMap.getOrDefault("audioCount", 0L)).longValue())
                .docSize(((Number) storageStatsMap.getOrDefault("docSize", 0L)).longValue())
                .docCount(((Number) storageStatsMap.getOrDefault("docCount", 0L)).longValue())
                .archiveSize(((Number) storageStatsMap.getOrDefault("archiveSize", 0L)).longValue())
                .archiveCount(((Number) storageStatsMap.getOrDefault("archiveCount", 0L)).longValue())
                .codeSize(((Number) storageStatsMap.getOrDefault("codeSize", 0L)).longValue())
                .codeCount(((Number) storageStatsMap.getOrDefault("codeCount", 0L)).longValue())
                .otherSize(((Number) storageStatsMap.getOrDefault("otherSize", 0L)).longValue())
                .otherCount(((Number) storageStatsMap.getOrDefault("otherCount", 0L)).longValue())
                .build();
    }
    
    /**
     * 获取传输趋势数据
     */
    private DashboardVO.TransferTrend getTransferTrend() {
        // 获取月度传输统计
        Map<String, Object> monthStats = transferHistoryService.getTransferStats("month", null);
        
        @SuppressWarnings("unchecked")
        List<String> uploadLabels = (List<String>) monthStats.get("uploadLabels");
        @SuppressWarnings("unchecked")
        List<Long> uploadValues = (List<Long>) monthStats.get("uploadValues");
        @SuppressWarnings("unchecked")
        List<String> downloadLabels = (List<String>) monthStats.get("downloadLabels");
        @SuppressWarnings("unchecked")
        List<Long> downloadValues = (List<Long>) monthStats.get("downloadValues");
        
        return DashboardVO.TransferTrend.builder()
                .uploadLabels(uploadLabels)
                .uploadValues(uploadValues)
                .downloadLabels(downloadLabels)
                .downloadValues(downloadValues)
                .build();
    }
}
