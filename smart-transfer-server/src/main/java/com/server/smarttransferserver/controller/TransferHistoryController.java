package com.server.smarttransferserver.controller;

import java.util.List;
import java.util.Map;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.domain.TransferHistory;
import com.server.smarttransferserver.dto.DeleteRecentQueryDTO;
import com.server.smarttransferserver.service.TransferHistoryService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 传输历史记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/transfer/history")
public class TransferHistoryController {
    
    @Autowired
    private TransferHistoryService historyService;
    
    /**
     * 查询传输历史记录列表
     * @param history 查询条件
     * @param userId 用户ID（可选，仅管理员可用，用于筛选指定用户的数据）
     */
    @GetMapping("/list")
    public Result<List<TransferHistory>> list(TransferHistory history, 
                                               @RequestParam(required = false) Long userId) {
        try {
            List<TransferHistory> list = historyService.selectHistoryList(history, userId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询传输历史记录列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取传输历史记录详细信息
     */
    @GetMapping("/{id}")
    public Result<TransferHistory> getInfo(@PathVariable("id") Long id) {
        try {
            TransferHistory history = historyService.selectHistoryById(id);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取传输历史记录详细信息失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 新增传输历史记录
     */
    @PostMapping
    public Result<Void> add(@RequestBody TransferHistory history) {
        try {
            historyService.insertHistory(history);
            return Result.success();
        } catch (Exception e) {
            log.error("新增传输历史记录失败", e);
            return Result.error("新增失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除传输历史记录
     */
    @DeleteMapping
    public Result<Void> remove(@RequestBody List<Long> ids) {
        try {
            Long[] idsArray = ids.toArray(new Long[0]);
            historyService.deleteHistoryByIds(idsArray);
            return Result.success();
        } catch (Exception e) {
            log.error("删除传输历史记录失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除指定文件在最近若干秒内完成的传输历史（取消上传后移除误记的「已完成」）
     */
    @DeleteMapping("/recent-by-file")
    public Result<Integer> deleteRecentByFile(DeleteRecentQueryDTO query) {
        try {
            int count = historyService.deleteRecentByFileId(
                    query.getFileId(), query.getTransferType(), query.getWithinSeconds());
            return Result.success(count);
        } catch (Exception e) {
            log.error("删除近期历史失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空所有传输历史记录
     */
    @DeleteMapping("/clear")
    public Result<Void> clearAll() {
        try {
            int result = historyService.clearAllHistory();
            return Result.success("已清空 " + result + " 条记录");
        } catch (Exception e) {
            log.error("清空传输历史记录失败", e);
            return Result.error("清空失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取传输统计（按日/周/月）
     * @param period 统计周期：day-日, week-周, month-月
     * @param userId 用户ID（可选，仅管理员可用）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getTransferStats(
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) Long userId) {
        try {
            Map<String, Object> stats = historyService.getTransferStats(period, userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取传输统计失败", e);
            return Result.error("获取统计失败: " + e.getMessage());
        }
    }
    
}

