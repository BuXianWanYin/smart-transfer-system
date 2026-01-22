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
        try {
            recoveryFileService.restoreFile(id);
            return Result.success();
        } catch (Exception e) {
            log.error("还原文件失败", e);
            return Result.error("还原失败: " + e.getMessage());
        }
    }

    /**
     * 批量还原文件
     */
    @PostMapping("/restore/batch")
    public Result<Void> batchRestore(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        try {
            recoveryFileService.batchRestoreFiles(ids);
            return Result.success();
        } catch (Exception e) {
            log.error("批量还原文件失败", e);
            return Result.error("批量还原失败: " + e.getMessage());
        }
    }

    /**
     * 彻底删除文件
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            recoveryFileService.deleteFilePermanently(id);
            return Result.success();
        } catch (Exception e) {
            log.error("彻底删除文件失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量彻底删除文件
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        try {
            recoveryFileService.batchDeleteFilesPermanently(ids);
            return Result.success();
        } catch (Exception e) {
            log.error("批量彻底删除文件失败", e);
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        try {
            recoveryFileService.clearRecoveryBin();
            return Result.success();
        } catch (Exception e) {
            log.error("清空回收站失败", e);
            return Result.error("清空失败: " + e.getMessage());
        }
    }
}

