package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.dto.*;
import com.server.smarttransferserver.service.FolderService;
import com.server.smarttransferserver.vo.FolderContentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
    public Result<Folder> createFolder(@Valid @RequestBody CreateFolderDTO dto) {
        log.info("创建文件夹 - 名称: {}, 父ID: {}", dto.getFolderName(), dto.getParentId());
        try {
            Folder folder = folderService.createFolder(dto.getFolderName(), dto.getParentId());
            return Result.success(folder);
        } catch (Exception e) {
            log.error("创建文件夹失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取文件夹内容（文件夹+文件列表）
     * @param query 查询参数
     */
    @GetMapping("/content")
    public Result<FolderContentVO> getFolderContent(FolderContentQueryDTO query) {
        // 改进日志：只在管理员指定筛选用户时打印该参数
        if (query.getUserId() != null) {
            log.info("获取文件夹内容 - 文件夹ID: {}, 文件类型: {}, 筛选用户ID: {}", 
                    query.getParentId(), query.getFileType(), query.getUserId());
        } else {
            log.info("获取文件夹内容 - 文件夹ID: {}, 文件类型: {}", 
                    query.getParentId(), query.getFileType());
        }
        try {
            FolderContentVO content = folderService.getFolderContent(
                    query.getParentId(), query.getFileType(), query.getPageNum(), 
                    query.getPageSize(), query.getUserId());
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
            List<Folder> folders = folderService.getFoldersByParentId(parentId, null);
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
    public Result<Void> renameFolder(@Valid @RequestBody RenameFolderDTO dto) {
        log.info("重命名文件夹 - ID: {}, 新名称: {}", dto.getFolderId(), dto.getNewName());
        try {
            folderService.renameFolder(dto.getFolderId(), dto.getNewName());
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
    public Result<Void> moveFileToFolder(@Valid @RequestBody MoveFileToFolderDTO dto) {
        log.info("移动文件 - 文件ID: {}, 目标文件夹: {}", dto.getFileId(), dto.getFolderId());
        try {
            folderService.moveFileToFolder(dto.getFileId(), dto.getFolderId());
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
    public Result<Void> moveFolderTo(@Valid @RequestBody MoveFolderDTO dto) {
        log.info("移动文件夹 - ID: {}, 目标: {}", dto.getFolderId(), dto.getTargetFolderId());
        try {
            folderService.moveFolderTo(dto.getFolderId(), dto.getTargetFolderId());
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

