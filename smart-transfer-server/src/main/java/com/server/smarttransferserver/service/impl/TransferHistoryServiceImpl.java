package com.server.smarttransferserver.service.impl;

import java.util.Arrays;
import java.util.List;

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
     * @param history 查询条件
     * @return 传输历史记录列表
     */
    @Override
    public List<TransferHistory> selectHistoryList(TransferHistory history) {
        Long userId = UserContextHolder.getUserId();
        LambdaQueryWrapper<TransferHistory> queryWrapper = new LambdaQueryWrapper<>();
        
        // 用户隔离
        if (userId != null) {
            queryWrapper.eq(TransferHistory::getUserId, userId);
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
}

