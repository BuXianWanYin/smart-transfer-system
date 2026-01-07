package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.domain.RecoveryFile;
import com.server.smarttransferserver.service.RecoveryFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 回收站控制器
 */
@Slf4j
@RestController
@RequestMapping("/recovery")
public class RecoveryFileController {

    @Autowired
    private RecoveryFileService recoveryFileService;

    /**
     * 获取回收站文件列表
     */
    @GetMapping("/list")
    public Result<List<RecoveryFile>> list() {
        List<RecoveryFile> list = recoveryFileService.getRecoveryFileList();
        return Result.success(list);
    }

    /**
     * 还原文件
     */
    @PostMapping("/restore/{id}")
    public Result<Void> restore(@PathVariable Long id) {
        recoveryFileService.restoreFile(id);
        return Result.success();
    }

    /**
     * 批量还原文件
     */
    @PostMapping("/restore/batch")
    public Result<Void> batchRestore(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        if (ids != null) {
            for (Long id : ids) {
                recoveryFileService.restoreFile(id);
            }
        }
        return Result.success();
    }

    /**
     * 彻底删除文件
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recoveryFileService.deleteFilePermanently(id);
        return Result.success();
    }

    /**
     * 批量彻底删除文件
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        if (ids != null) {
            for (Long id : ids) {
                recoveryFileService.deleteFilePermanently(id);
            }
        }
        return Result.success();
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        recoveryFileService.clearRecoveryBin();
        return Result.success();
    }
}

