package com.server.smarttransferserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import com.server.smarttransferserver.domain.SystemActivity;

/**
 * 仪表盘数据视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {
    
    /**
     * KPI数据
     */
    private KpiData kpiData;
    
    /**
     * 用户统计数据
     */
    private UserStats userStats;
    
    /**
     * 存储统计数据
     */
    private StorageStats storageStats;
    
    /**
     * 传输趋势数据
     */
    private TransferTrend transferTrend;
    
    /**
     * 最近动态列表
     */
    private List<SystemActivity> recentActivities;
    
    /**
     * KPI数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiData {
        /**
         * 总用户数
         */
        private Integer totalUsers;
        
        /**
         * 总存储（字节）
         */
        private Long totalStorage;
        
        /**
         * 总文件数
         */
        private Long totalFiles;
        
        /**
         * 今日传输量（字节）
         */
        private Long todayTransfer;
        
        /**
         * 较上周变化百分比（用户数）
         */
        private String usersChangePercent;
        
        /**
         * 较上周变化百分比（存储）
         */
        private String storageChangePercent;
        
        /**
         * 较上周变化百分比（文件数）
         */
        private String filesChangePercent;
        
        /**
         * 较昨日变化百分比（传输量）
         */
        private String transferChangePercent;
    }
    
    /**
     * 用户统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        /**
         * 管理员数量
         */
        private Integer adminCount;
        
        /**
         * 普通用户数量
         */
        private Integer userCount;
        
        /**
         * 启用用户数量
         */
        private Integer enabledUsers;
        
        /**
         * 禁用用户数量
         */
        private Integer disabledUsers;
        
        /**
         * 总用户量
         */
        private Integer totalUsers;
        
        /**
         * 活跃用户数（有登录记录）
         */
        private Integer activeUsers;
        
        /**
         * 本月新增用户数
         */
        private Integer monthlyNewUsers;
        
        /**
         * 较上周变化百分比
         */
        private String changePercent;
        
        /**
         * 较上月新增变化百分比
         */
        private String newUsersChangePercent;
    }
    
    /**
     * 存储统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageStats {
        /**
         * 总存储（字节）
         */
        private Long totalSize;
        
        /**
         * 总文件数
         */
        private Long fileCount;
        
        /**
         * 图片大小（字节）
         */
        private Long imageSize;
        
        /**
         * 图片数量
         */
        private Long imageCount;
        
        /**
         * 视频大小（字节）
         */
        private Long videoSize;
        
        /**
         * 视频数量
         */
        private Long videoCount;
        
        /**
         * 音频大小（字节）
         */
        private Long audioSize;
        
        /**
         * 音频数量
         */
        private Long audioCount;
        
        /**
         * 文档大小（字节）
         */
        private Long docSize;
        
        /**
         * 文档数量
         */
        private Long docCount;
        
        /**
         * 压缩包大小（字节）
         */
        private Long archiveSize;
        
        /**
         * 压缩包数量
         */
        private Long archiveCount;
        
        /**
         * 代码文件大小（字节）
         */
        private Long codeSize;
        
        /**
         * 代码文件数量
         */
        private Long codeCount;
        
        /**
         * 其他文件大小（字节）
         */
        private Long otherSize;
        
        /**
         * 其他文件数量
         */
        private Long otherCount;
    }
    
    /**
     * 传输趋势数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferTrend {
        /**
         * 上传趋势标签
         */
        private List<String> uploadLabels;
        
        /**
         * 上传趋势值
         */
        private List<Long> uploadValues;
        
        /**
         * 下载趋势标签
         */
        private List<String> downloadLabels;
        
        /**
         * 下载趋势值
         */
        private List<Long> downloadValues;
    }
}
