package com.server.smarttransferserver.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.domain.TransferHistory;
import com.server.smarttransferserver.mapper.TransferHistoryMapper;
import com.server.smarttransferserver.service.TransferHistoryService;
import com.server.smarttransferserver.util.UserContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 传输历史记录服务实现类
 */
@Slf4j
@Service
public class TransferHistoryServiceImpl extends ServiceImpl<TransferHistoryMapper, TransferHistory> 
        implements TransferHistoryService {
    
    /**
     * 查询传输历史记录
     *
     * @param id 主键ID
     * @return 传输历史记录
     */
    @Override
    public TransferHistory selectHistoryById(Long id) {
        return getById(id);
    }
    
    /**
     * 查询传输历史记录列表
     *
     * @param history 查询条件（可包含userId用于筛选）
     * @return 传输历史记录列表
     */
    @Override
    public List<TransferHistory> selectHistoryList(TransferHistory history) {
        // 如果history为null，创建新对象
        if (history == null) {
            history = new TransferHistory();
        }
        
        Long currentUserId = UserContextHolder.getUserId();
        String currentUserRole = UserContextHolder.getRole();
        LambdaQueryWrapper<TransferHistory> queryWrapper = new LambdaQueryWrapper<>();
        
        // 用户数据隔离逻辑：
        // 1. 如果是管理员，且history中指定了userId，则查询指定用户的数据
        // 2. 如果是管理员，且history中未指定userId，则查询所有用户的数据
        // 3. 如果是普通用户，只能查询自己的数据
        if ("ADMIN".equals(currentUserRole)) {
            // 管理员：如果指定了userId，查询指定用户；否则查询所有用户
            if (history.getUserId() != null) {
                queryWrapper.eq(TransferHistory::getUserId, history.getUserId());
            }
            // 如果未指定userId，不添加userId条件，查询所有用户
        } else {
            // 普通用户：只能查询自己的数据
            if (currentUserId != null) {
                queryWrapper.eq(TransferHistory::getUserId, currentUserId);
            }
        }
        
        // 根据文件名模糊查询
        if (history.getFileName() != null && !history.getFileName().isEmpty()) {
            queryWrapper.like(TransferHistory::getFileName, history.getFileName());
        }
        
        // 根据传输类型过滤
        if (history.getTransferType() != null && !history.getTransferType().isEmpty()) {
            queryWrapper.eq(TransferHistory::getTransferType, history.getTransferType());
        }
        
        // 根据传输状态过滤
        if (history.getTransferStatus() != null && !history.getTransferStatus().isEmpty()) {
            queryWrapper.eq(TransferHistory::getTransferStatus, history.getTransferStatus());
        }
        
        // 按完成时间降序排列
        queryWrapper.orderByDesc(TransferHistory::getCompletedTime);
        
        return list(queryWrapper);
    }
    
    @Override
    public List<TransferHistory> selectHistoryList(TransferHistory history, Long userId) {
        // 如果history为null，创建新对象
        if (history == null) {
            history = new TransferHistory();
        }
        // 如果传入了userId参数，设置到history对象中
        if (userId != null) {
            history.setUserId(userId);
        }
        return selectHistoryList(history);
    }
    
    /**
     * 新增传输历史记录
     *
     * @param history 传输历史记录
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertHistory(TransferHistory history) {
        // 设置用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            history.setUserId(userId);
        }
        return save(history) ? 1 : 0;
    }
    
    /**
     * 删除传输历史记录
     *
     * @param id 主键ID
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHistoryById(Long id) {
        return removeById(id) ? 1 : 0;
    }
    
    /**
     * 批量删除传输历史记录
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHistoryByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new RuntimeException("ID列表不能为空");
        }
        return removeBatchByIds(Arrays.asList(ids)) ? ids.length : 0;
    }
    
    /**
     * 清空所有传输历史记录（当前用户）
     *
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int clearAllHistory() {
        Long userId = UserContextHolder.getUserId();
        LambdaQueryWrapper<TransferHistory> queryWrapper = new LambdaQueryWrapper<>();
        
        // 用户隔离
        if (userId != null) {
            queryWrapper.eq(TransferHistory::getUserId, userId);
        }
        
        long count = count(queryWrapper);
        return remove(queryWrapper) ? (int) count : 0;
    }
    
    /**
     * 删除指定文件在最近若干秒内完成的传输历史（用于取消上传后移除误记的「已完成」）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRecentByFileId(Long fileId, String transferType, int withinSeconds) {
        if (fileId == null || transferType == null || withinSeconds <= 0) {
            return 0;
        }
        LocalDateTime since = LocalDateTime.now().minusSeconds(withinSeconds);
        LambdaQueryWrapper<TransferHistory> q = new LambdaQueryWrapper<>();
        q.eq(TransferHistory::getFileId, fileId)
         .eq(TransferHistory::getTransferType, transferType)
         .eq(TransferHistory::getTransferStatus, "COMPLETED")
         .ge(TransferHistory::getCompletedTime, since);
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            q.eq(TransferHistory::getUserId, userId);
        }
        int count = (int) count(q);
        remove(q);
        if (count > 0) {
            log.info("删除近期已完成历史 - fileId: {}, transferType: {}, 删除条数: {}", fileId, transferType, count);
        }
        return count;
    }
    
    /**
     * 获取传输统计（按日/周/月）
     */
    @Override
    public Map<String, Object> getTransferStats(String period, Long userId) {
        Long currentUserId = UserContextHolder.getUserId();
        String currentUserRole = UserContextHolder.getRole();
        
        // 确定查询的用户ID
        Long queryUserId = null;
        if ("ADMIN".equals(currentUserRole)) {
            // 管理员可以查询指定用户或所有用户
            queryUserId = userId;
        } else {
            // 普通用户只能查询自己
            queryUserId = currentUserId;
        }
        
        LambdaQueryWrapper<TransferHistory> queryWrapper = new LambdaQueryWrapper<>();
        if (queryUserId != null) {
            queryWrapper.eq(TransferHistory::getUserId, queryUserId);
        }
        
        // 根据周期设置时间范围
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = null;
        
        if ("day".equals(period)) {
            // 最近30天
            startTime = now.minusDays(30);
        } else if ("week".equals(period)) {
            // 最近12周
            startTime = now.minusWeeks(12);
        } else if ("month".equals(period)) {
            // 最近12个月
            startTime = now.minusMonths(12);
        } else {
            // 默认最近30天
            startTime = now.minusDays(30);
        }
        
        queryWrapper.ge(TransferHistory::getCompletedTime, startTime);
        queryWrapper.eq(TransferHistory::getTransferStatus, "COMPLETED");
        
        List<TransferHistory> histories = list(queryWrapper);
        
        // 按时间分组统计
        Map<String, Long> uploadData = new LinkedHashMap<>();
        Map<String, Long> downloadData = new LinkedHashMap<>();
        DateTimeFormatter formatter = null;
        
        if ("day".equals(period)) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (int i = 29; i >= 0; i--) {
                String date = now.minusDays(i).format(formatter);
                uploadData.put(date, 0L);
                downloadData.put(date, 0L);
            }
        } else if ("week".equals(period)) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (int i = 11; i >= 0; i--) {
                LocalDateTime weekStart = now.minusWeeks(i).minusDays(now.minusWeeks(i).getDayOfWeek().getValue() - 1);
                String week = weekStart.format(formatter);
                uploadData.put(week, 0L);
                downloadData.put(week, 0L);
            }
        } else if ("month".equals(period)) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (int i = 11; i >= 0; i--) {
                String month = now.minusMonths(i).format(formatter);
                uploadData.put(month, 0L);
                downloadData.put(month, 0L);
            }
        }
        
        // 统计数据
        for (TransferHistory history : histories) {
            String key = null;
            if (history.getCompletedTime() != null) {
                if ("day".equals(period)) {
                    key = history.getCompletedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else if ("week".equals(period)) {
                    LocalDateTime completed = history.getCompletedTime();
                    LocalDateTime weekStart = completed.minusDays(completed.getDayOfWeek().getValue() - 1);
                    key = weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else if ("month".equals(period)) {
                    key = history.getCompletedTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }
            }
            
            if (key != null && history.getFileSize() != null) {
                // 确保key在预初始化的Map中，如果不在则初始化（防止数据超出时间范围）
                if (!uploadData.containsKey(key)) {
                    uploadData.put(key, 0L);
                }
                if (!downloadData.containsKey(key)) {
                    downloadData.put(key, 0L);
                }
                
                if ("UPLOAD".equals(history.getTransferType())) {
                    uploadData.put(key, uploadData.get(key) + history.getFileSize());
                } else if ("DOWNLOAD".equals(history.getTransferType())) {
                    downloadData.put(key, downloadData.get(key) + history.getFileSize());
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("uploadData", uploadData);
        result.put("downloadData", downloadData);
        result.put("uploadLabels", new ArrayList<>(uploadData.keySet()));
        result.put("downloadLabels", new ArrayList<>(downloadData.keySet()));
        result.put("uploadValues", new ArrayList<>(uploadData.values()));
        result.put("downloadValues", new ArrayList<>(downloadData.values()));
        
        return result;
    }
    
    /**
     * 获取算法使用统计
     */
    @Override
    public Map<String, Object> getAlgorithmStats(Long userId) {
        Long currentUserId = UserContextHolder.getUserId();
        String currentUserRole = UserContextHolder.getRole();
        
        // 确定查询的用户ID
        Long queryUserId = null;
        if ("ADMIN".equals(currentUserRole)) {
            queryUserId = userId; // 管理员可以查询指定用户或所有用户（null表示所有）
        } else {
            queryUserId = currentUserId; // 普通用户只能查询自己
        }
        
        LambdaQueryWrapper<TransferHistory> queryWrapper = new LambdaQueryWrapper<>();
        if (queryUserId != null) {
            queryWrapper.eq(TransferHistory::getUserId, queryUserId);
        }
        queryWrapper.eq(TransferHistory::getTransferStatus, "COMPLETED");
        queryWrapper.isNotNull(TransferHistory::getAlgorithm);
        
        List<TransferHistory> histories = list(queryWrapper);
        
        // 统计算法使用次数和传输量
        Map<String, Long> algorithmCount = new HashMap<>();
        Map<String, Long> algorithmSize = new HashMap<>();
        
        for (TransferHistory history : histories) {
            String algorithm = history.getAlgorithm();
            if (algorithm != null) {
                algorithmCount.put(algorithm, algorithmCount.getOrDefault(algorithm, 0L) + 1);
                // 确保algorithmSize也有这个key，即使fileSize为null
                if (!algorithmSize.containsKey(algorithm)) {
                    algorithmSize.put(algorithm, 0L);
                }
                if (history.getFileSize() != null) {
                    algorithmSize.put(algorithm, algorithmSize.get(algorithm) + history.getFileSize());
                }
            }
        }
        
        // 确保两个Map的keySet一致，按algorithmCount的keySet顺序
        List<String> algorithmLabels = new ArrayList<>(algorithmCount.keySet());
        List<Long> countValues = new ArrayList<>();
        List<Long> sizeValues = new ArrayList<>();
        
        for (String algorithm : algorithmLabels) {
            countValues.add(algorithmCount.getOrDefault(algorithm, 0L));
            sizeValues.add(algorithmSize.getOrDefault(algorithm, 0L));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("algorithmCount", algorithmCount);
        result.put("algorithmSize", algorithmSize);
        result.put("algorithmLabels", algorithmLabels);
        result.put("countValues", countValues);
        result.put("sizeValues", sizeValues);
        
        return result;
    }
}

