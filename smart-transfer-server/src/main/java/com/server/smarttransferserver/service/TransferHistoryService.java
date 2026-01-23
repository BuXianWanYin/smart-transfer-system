package com.server.smarttransferserver.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.domain.TransferHistory;

/**
 * 传输历史记录服务接口
 */
public interface TransferHistoryService extends IService<TransferHistory> {
    
    /**
     * 查询传输历史记录
     *
     * @param id 主键ID
     * @return 传输历史记录
     */
    TransferHistory selectHistoryById(Long id);
    
    /**
     * 查询传输历史记录列表
     *
     * @param history 查询条件
     * @return 传输历史记录列表
     */
    List<TransferHistory> selectHistoryList(TransferHistory history);
    
    /**
     * 查询传输历史记录列表（支持userId筛选）
     *
     * @param history 查询条件
     * @param userId 用户ID（可选，仅管理员可用，用于筛选指定用户的数据）
     * @return 传输历史记录列表
     */
    List<TransferHistory> selectHistoryList(TransferHistory history, Long userId);
    
    /**
     * 新增传输历史记录
     *
     * @param history 传输历史记录
     * @return 影响行数
     */
    int insertHistory(TransferHistory history);
    
    /**
     * 删除传输历史记录
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteHistoryById(Long id);
    
    /**
     * 批量删除传输历史记录
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int deleteHistoryByIds(Long[] ids);
    
    /**
     * 清空所有传输历史记录
     *
     * @return 影响行数
     */
    int clearAllHistory();
    
    /**
     * 获取传输统计（按日/周/月）
     * @param period 统计周期：day-日, week-周, month-月
     * @param userId 用户ID（可选，管理员可查询指定用户，普通用户只能查询自己）
     * @return 传输统计数据
     */
    Map<String, Object> getTransferStats(String period, Long userId);
    
    /**
     * 获取算法使用统计
     * @param userId 用户ID（可选，管理员可查询指定用户，普通用户只能查询自己）
     * @return 算法使用统计数据
     */
    Map<String, Object> getAlgorithmStats(Long userId);
}

