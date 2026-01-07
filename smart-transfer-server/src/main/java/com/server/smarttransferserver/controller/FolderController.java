package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.service.FolderService;
import com.server.smarttransferserver.vo.FolderContentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文件夹Controller
 */
@Slf4j
@RestController
@RequestMapping("/folder")
public class FolderController {

    @Autowired
    private FolderService folderService;

    /**
     * 创建文件夹
     */
    @PostMapping("/create")
    public Result<Folder> createFolder(@RequestBody Map<String, Object> params) {
        String folderName = (String) params.get("folderName");
        Long parentId = params.get("parentId") != null ? 
            Long.valueOf(params.get("parentId").toString()) : 0L;
        
        log.info("创建文件夹 - 名称: {}, 父ID: {}", folderName, parentId);
        try {
            Folder folder = folderService.createFolder(folderName, parentId);
            return Result.success(folder);
        } catch (Exception e) {
            log.error("创建文件夹失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取文件夹内容（文件夹+文件列表）
     * @param parentId 父文件夹ID
     * @param fileType 文件类型筛选：0-全部, 1-图片, 2-文档, 3-视频, 4-音乐, 5-其他
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GetMapping("/content")
    public Result<FolderContentVO> getFolderContent(
            @RequestParam(required = false, defaultValue = "0") Long parentId,
            @RequestParam(required = false, defaultValue = "0") Integer fileType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        log.info("获取文件夹内容 - 文件夹ID: {}, 文件类型: {}", parentId, fileType);
        try {
            FolderContentVO content = folderService.getFolderContent(parentId, fileType, pageNum, pageSize);
            return Result.success(content);
        } catch (Exception e) {
            log.error("获取文件夹内容失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取子文件夹列表
     */
    @GetMapping("/list")
    public Result<List<Folder>> getFolderList(
            @RequestParam(required = false, defaultValue = "0") Long parentId) {
        
        log.info("获取文件夹列表 - 父ID: {}", parentId);
        try {
            List<Folder> folders = folderService.getFoldersByParentId(parentId);
            return Result.success(folders);
        } catch (Exception e) {
            log.error("获取文件夹列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取面包屑路径
     */
    @GetMapping("/breadcrumb")
    public Result<List<Folder>> getBreadcrumb(
            @RequestParam(required = false, defaultValue = "0") Long folderId) {
        
        try {
            List<Folder> breadcrumb = folderService.getBreadcrumb(folderId);
            return Result.success(breadcrumb);
        } catch (Exception e) {
            log.error("获取面包屑失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重命名文件夹
     */
    @PutMapping("/rename")
    public Result<Void> renameFolder(@RequestBody Map<String, Object> params) {
        Long folderId = Long.valueOf(params.get("folderId").toString());
        String newName = (String) params.get("newName");
        
        log.info("重命名文件夹 - ID: {}, 新名称: {}", folderId, newName);
        try {
            folderService.renameFolder(folderId, newName);
            return Result.success();
        } catch (Exception e) {
            log.error("重命名文件夹失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId) {
        log.info("删除文件夹 - ID: {}", folderId);
        try {
            folderService.deleteFolder(folderId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除文件夹失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 移动文件到文件夹
     */
    @PostMapping("/move/file")
    public Result<Void> moveFileToFolder(@RequestBody Map<String, Object> params) {
        Long fileId = Long.valueOf(params.get("fileId").toString());
        Long folderId = params.get("folderId") != null ? 
            Long.valueOf(params.get("folderId").toString()) : 0L;
        
        log.info("移动文件 - 文件ID: {}, 目标文件夹: {}", fileId, folderId);
        try {
            folderService.moveFileToFolder(fileId, folderId);
            return Result.success();
        } catch (Exception e) {
            log.error("移动文件失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 移动文件夹
     */
    @PostMapping("/move/folder")
    public Result<Void> moveFolderTo(@RequestBody Map<String, Object> params) {
        Long folderId = Long.valueOf(params.get("folderId").toString());
        Long targetFolderId = params.get("targetFolderId") != null ? 
            Long.valueOf(params.get("targetFolderId").toString()) : 0L;
        
        log.info("移动文件夹 - ID: {}, 目标: {}", folderId, targetFolderId);
        try {
            folderService.moveFolderTo(folderId, targetFolderId);
            return Result.success();
        } catch (Exception e) {
            log.error("移动文件夹失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取文件夹树
     */
    @GetMapping("/tree")
    public Result<Object> getFolderTree() {
        log.info("获取文件夹树");
        try {
            Object tree = folderService.getFolderTree();
            return Result.success(tree);
        } catch (Exception e) {
            log.error("获取文件夹树失败", e);
            return Result.error(e.getMessage());
        }
    }
}

