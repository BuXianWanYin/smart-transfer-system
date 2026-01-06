package com.server.smarttransferserver.service;

import java.util.List;

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
}

