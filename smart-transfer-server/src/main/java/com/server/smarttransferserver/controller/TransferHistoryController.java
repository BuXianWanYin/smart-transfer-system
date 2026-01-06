package com.server.smarttransferserver.controller;

import java.util.List;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.domain.TransferHistory;
import com.server.smarttransferserver.service.TransferHistoryService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
     */
    @GetMapping("/list")
    public Result<List<TransferHistory>> list(TransferHistory history) {
        try {
            List<TransferHistory> list = historyService.selectHistoryList(history);
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
            int result = historyService.insertHistory(history);
            return result > 0 ? Result.success() : Result.error("新增失败");
        } catch (Exception e) {
            log.error("新增传输历史记录失败", e);
            return Result.error("新增失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除传输历史记录
     */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable Long[] ids) {
        try {
            int result = historyService.deleteHistoryByIds(ids);
            return result > 0 ? Result.success() : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除传输历史记录失败", e);
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
}

