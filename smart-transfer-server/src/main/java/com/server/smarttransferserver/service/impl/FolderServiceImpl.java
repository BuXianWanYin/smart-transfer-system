package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.FolderMapper;
import com.server.smarttransferserver.service.FolderService;
import com.server.smarttransferserver.vo.FileInfoVO;
import com.server.smarttransferserver.vo.FolderContentVO;
import com.server.smarttransferserver.vo.FolderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.smarttransferserver.util.FileTypeUtil;
import com.server.smarttransferserver.util.UserContextHolder;
import com.server.smarttransferserver.service.RecoveryFileService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文件夹服务实现
 */
@Slf4j
@Service
public class FolderServiceImpl implements FolderService {

    @Autowired
    private FolderMapper folderMapper;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private RecoveryFileService recoveryFileService;

    @Override
    @Transactional
    public Folder createFolder(String folderName, Long parentId) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }
        
        // 规范化 parentId（null 或 0 都视为根目录）
        Long normalizedParentId = (parentId == null || parentId == 0) ? 0L : parentId;
        
        // 检查同名文件夹并自动重命名（同一用户、同一父目录下）
        String finalFolderName = checkAndRenameDuplicateFolder(folderName, normalizedParentId, userId);

        // 构建路径
        String path = "/";
        if (normalizedParentId != null && normalizedParentId > 0) {
            Folder parent = folderMapper.selectById(normalizedParentId);
            if (parent != null) {
                path = parent.getPath() + parent.getFolderName() + "/";
            }
        }

        Folder folder = Folder.builder()
                .userId(userId)
                .folderName(finalFolderName)
                .parentId(normalizedParentId)
                .path(path)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        folderMapper.insert(folder);
        log.info("创建文件夹成功 - ID: {}, 名称: {}, 父目录ID: {}, 用户: {}", 
                 folder.getId(), finalFolderName, normalizedParentId, userId);
        return folder;
    }

    @Override
    public List<Folder> getFoldersByParentId(Long parentId) {
        Long userId = UserContextHolder.getUserId();
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Folder::getUserId, userId);
        }
        wrapper.eq(Folder::getParentId, parentId == null ? 0L : parentId)
               .eq(Folder::getDelFlag, 0)  // 只查询未删除的文件夹
               .orderByDesc(Folder::getCreateTime);
        return folderMapper.selectList(wrapper);
    }

    @Override
    public FolderContentVO getFolderContent(Long folderId, Integer fileType, Integer pageNum, Integer pageSize) {
        Long userId = UserContextHolder.getUserId();
        Long currentFolderId = folderId == null ? 0L : folderId;
        String currentFolderName = "全部文件";
        
        // 是否按类型筛选（非全部类型时，在所有目录中搜索该类型文件）
        boolean filterByType = fileType != null && fileType > 0 && fileType < 6;

        if (currentFolderId > 0) {
            Folder current = folderMapper.selectById(currentFolderId);
            if (current != null) {
                currentFolderName = current.getFolderName();
            }
        }

        // 获取子文件夹（仅在"全部"类型时显示文件夹）
        List<FolderVO> folderVOs = new ArrayList<>();
        if (!filterByType) {
            List<Folder> subFolders = getFoldersByParentId(currentFolderId);
            folderVOs = subFolders.stream().map(f -> {
                FolderVO vo = new FolderVO();
                BeanUtils.copyProperties(f, vo);
                vo.setType("folder");
                // 统计子文件夹和文件数量
                vo.setSubFolderCount(countSubFolders(f.getId(), userId));
                vo.setFileCount(countFiles(f.getId(), userId));
                return vo;
            }).collect(Collectors.toList());
        }

        // 获取文件（分页）- 只查询未删除的、上传完成的、当前用户的文件
        Page<FileInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FileInfo> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(FileInfo::getDelFlag, 0); // 过滤已删除文件
        fileWrapper.eq(FileInfo::getUploadStatus, "COMPLETED"); // 只显示上传完成的文件
        
        // 用户数据隔离
        if (userId != null) {
            fileWrapper.eq(FileInfo::getUserId, userId);
        }
        
        if (filterByType) {
            // 按类型筛选：从所有目录搜索该类型的文件
            Set<String> extensions = FileTypeUtil.getExtensionsByType(fileType);
            if (!extensions.isEmpty()) {
                fileWrapper.in(FileInfo::getExtendName, extensions);
            } else if (fileType == FileTypeUtil.TYPE_OTHER) {
                // "其他"类型：排除所有已知类型的扩展名
                Set<String> allKnownExt = new java.util.HashSet<>();
                allKnownExt.addAll(FileTypeUtil.getExtensionsByType(FileTypeUtil.TYPE_IMAGE));
                allKnownExt.addAll(FileTypeUtil.getExtensionsByType(FileTypeUtil.TYPE_DOCUMENT));
                allKnownExt.addAll(FileTypeUtil.getExtensionsByType(FileTypeUtil.TYPE_VIDEO));
                allKnownExt.addAll(FileTypeUtil.getExtensionsByType(FileTypeUtil.TYPE_AUDIO));
                fileWrapper.notIn(FileInfo::getExtendName, allKnownExt);
            }
        } else {
            // 全部类型：只查询当前目录
            fileWrapper.eq(FileInfo::getFolderId, currentFolderId);
        }
        
        fileWrapper.orderByDesc(FileInfo::getCreateTime);
        Page<FileInfo> filePage = fileInfoMapper.selectPage(page, fileWrapper);

        List<FileInfoVO> fileVOs = filePage.getRecords().stream().map(f -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(f, vo);
            return vo;
        }).collect(Collectors.toList());

        // 获取面包屑
        List<Folder> breadcrumb = filterByType ? new ArrayList<>() : getBreadcrumb(currentFolderId);

        return FolderContentVO.builder()
                .currentFolderId(currentFolderId)
                .currentFolderName(filterByType ? getFileTypeName(fileType) : currentFolderName)
                .breadcrumb(breadcrumb)
                .folders(folderVOs)
                .files(fileVOs)
                .fileTotal(filePage.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }
    
    /**
     * 获取文件类型名称
     */
    private String getFileTypeName(Integer fileType) {
        if (fileType == null) return "全部文件";
        switch (fileType) {
            case 1: return "图片";
            case 2: return "文档";
            case 3: return "视频";
            case 4: return "音乐";
            case 5: return "其他";
            default: return "全部文件";
        }
    }

    @Override
    @Transactional
    public void renameFolder(Long folderId, String newName) {
        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        Long userId = UserContextHolder.getUserId();
        Long parentId = folder.getParentId() != null ? folder.getParentId() : 0L;
        
        // 检查同名文件夹并自动重命名（排除当前文件夹本身）
        String finalFolderName = checkAndRenameDuplicateFolderForRename(newName, parentId, userId, folderId);

        folder.setFolderName(finalFolderName);
        folder.setUpdateTime(LocalDateTime.now());
        folderMapper.updateById(folder);
        
        log.info("重命名文件夹成功 - ID: {}, 新名称: {}", folderId, finalFolderName);
    }

    @Override
    @Transactional
    public void deleteFolder(Long folderId) {
        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        
        // 使用回收站服务进行软删除（会递归删除子文件夹和子文件）
        recoveryFileService.deleteFolderToRecovery(folderId);
        log.info("文件夹已移至回收站 - ID: {}", folderId);
    }

    @Override
    public List<Folder> getBreadcrumb(Long folderId) {
        List<Folder> breadcrumb = new ArrayList<>();

        if (folderId == null || folderId == 0) {
            return breadcrumb;
        }

        Long currentId = folderId;
        while (currentId != null && currentId > 0) {
            Folder folder = folderMapper.selectById(currentId);
            if (folder != null) {
                breadcrumb.add(folder);
                currentId = folder.getParentId();
            } else {
                break;
            }
        }

        Collections.reverse(breadcrumb);
        return breadcrumb;
    }

    @Override
    @Transactional
    public void moveFileToFolder(Long fileId, Long folderId) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 规范化目标文件夹ID
        Long normalizedTargetFolderId = (folderId == null || folderId == 0) ? 0L : folderId;
        
        // 验证目标文件夹是否存在（如果targetFolderId不为0）
        if (normalizedTargetFolderId > 0) {
            Folder targetFolder = folderMapper.selectById(normalizedTargetFolderId);
            if (targetFolder == null) {
                throw new RuntimeException("目标文件夹不存在");
            }
        }
        
        // 如果移动到不同文件夹，检查目标文件夹是否有同名文件
        Long currentFolderId = file.getFolderId() != null ? file.getFolderId() : 0L;
        if (!currentFolderId.equals(normalizedTargetFolderId)) {
            Long userId = UserContextHolder.getUserId();
            String finalFileName = fileInfoMapper.selectByFileNameAndFolder(
                    file.getFileName(), normalizedTargetFolderId, userId).isEmpty() 
                    ? file.getFileName() 
                    : generateUniqueFileNameForMove(file.getFileName(), normalizedTargetFolderId, userId);
            
            if (!finalFileName.equals(file.getFileName())) {
                log.info("移动文件时检测到同名文件，自动重命名 - 原文件名: {}, 新文件名: {}, 目标文件夹: {}", 
                         file.getFileName(), finalFileName, normalizedTargetFolderId);
                file.setFileName(finalFileName);
            }
        }
        
        file.setFolderId(normalizedTargetFolderId);
        file.setUpdateTime(LocalDateTime.now());
        fileInfoMapper.updateById(file);
    }
    
    /**
     * 生成唯一的文件名（用于移动文件时的同名处理）
     */
    private String generateUniqueFileNameForMove(String originalFileName, Long folderId, Long userId) {
        List<FileInfo> duplicateFiles = fileInfoMapper.selectByFileNameAndFolder(originalFileName, folderId, userId);
        
        if (duplicateFiles.isEmpty()) {
            return originalFileName;
        }
        
        // 提取文件名和扩展名
        int lastDotIndex = originalFileName.lastIndexOf('.');
        String baseName;
        String extension;
        
        if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
            baseName = originalFileName.substring(0, lastDotIndex);
            extension = originalFileName.substring(lastDotIndex);
        } else {
            baseName = originalFileName;
            extension = "";
        }
        
        // 查找已存在的编号
        Pattern pattern = Pattern.compile("^" + Pattern.quote(baseName) + "\\((\\d+)\\)" + Pattern.quote(extension) + "$");
        int maxNumber = 0;
        
        for (FileInfo duplicate : duplicateFiles) {
            String dupFileName = duplicate.getFileName();
            Matcher matcher = pattern.matcher(dupFileName);
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }
        
        // 生成新文件名
        int newNumber = maxNumber + 1;
        return baseName + "(" + newNumber + ")" + extension;
    }

    @Override
    @Transactional
    public void moveFolderTo(Long folderId, Long targetFolderId) {
        // **修复MODULE-2: 处理targetFolderId为null的情况，转换为0（根目录）**
        if (targetFolderId == null) {
            targetFolderId = 0L;
        }
        
        if (folderId.equals(targetFolderId)) {
            throw new RuntimeException("不能移动到自身");
        }

        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        // 规范化目标文件夹ID
        Long normalizedTargetFolderId = (targetFolderId == null || targetFolderId == 0) ? 0L : targetFolderId;
        
        // **修复MODULE-2: 只有targetFolderId不为0时才检查循环依赖（0是根目录，无需检查）**
        if (normalizedTargetFolderId != null && normalizedTargetFolderId > 0) {
            // 验证目标文件夹存在
            Folder targetFolder = folderMapper.selectById(normalizedTargetFolderId);
            if (targetFolder == null) {
                throw new RuntimeException("目标文件夹不存在");
            }
            
            // 检查是否移动到子文件夹（会造成循环）
            if (isSubFolder(folderId, normalizedTargetFolderId)) {
                throw new RuntimeException("不能移动到子文件夹");
            }
        }
        
        // 如果移动到不同父目录，检查目标父目录是否有同名文件夹
        if (!folder.getParentId().equals(normalizedTargetFolderId)) {
            Long userId = UserContextHolder.getUserId();
            String finalFolderName = checkAndRenameDuplicateFolderForRename(
                    folder.getFolderName(), normalizedTargetFolderId, userId, folderId);
            
            if (!finalFolderName.equals(folder.getFolderName())) {
                log.info("移动文件夹时检测到同名文件夹，自动重命名 - 原文件夹名: {}, 新文件夹名: {}, 目标父目录: {}", 
                         folder.getFolderName(), finalFolderName, normalizedTargetFolderId);
                folder.setFolderName(finalFolderName);
            }
        }

        folder.setParentId(normalizedTargetFolderId);
        folder.setUpdateTime(LocalDateTime.now());

        // 更新路径
        String newPath = "/";
        if (normalizedTargetFolderId != null && normalizedTargetFolderId > 0) {
            Folder parent = folderMapper.selectById(normalizedTargetFolderId);
            if (parent != null) {
                newPath = parent.getPath() + parent.getFolderName() + "/";
            }
        }
        folder.setPath(newPath);

        folderMapper.updateById(folder);
    }

    private Integer countSubFolders(Long folderId, Long userId) {
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getParentId, folderId)
               .eq(Folder::getDelFlag, 0);  // 只统计未删除的
        if (userId != null) {
            wrapper.eq(Folder::getUserId, userId);
        }
        return Math.toIntExact(folderMapper.selectCount(wrapper));
    }

    private Integer countFiles(Long folderId, Long userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFolderId, folderId)
               .eq(FileInfo::getDelFlag, 0);
        if (userId != null) {
            wrapper.eq(FileInfo::getUserId, userId);
        }
        return Math.toIntExact(fileInfoMapper.selectCount(wrapper));
    }

    private boolean isSubFolder(Long parentId, Long childId) {
        if (childId == null || childId == 0) {
            return false;
        }
        Long currentId = childId;
        while (currentId != null && currentId > 0) {
            Folder folder = folderMapper.selectById(currentId);
            if (folder == null) {
                return false;
            }
            if (folder.getParentId().equals(parentId)) {
                return true;
            }
            currentId = folder.getParentId();
        }
        return false;
    }
    
    @Override
    public Object getFolderTree() {
        Long userId = UserContextHolder.getUserId();
        
        // 获取当前用户的所有文件夹（只获取未删除的）
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Folder::getUserId, userId);
        }
        wrapper.eq(Folder::getDelFlag, 0);  // 只获取未删除的文件夹
        List<Folder> allFolders = folderMapper.selectList(wrapper);
        
        // 构建根节点
        Map<String, Object> root = new HashMap<>();
        root.put("id", 0L);
        root.put("label", "全部文件");
        root.put("path", "/");
        root.put("children", buildTreeChildren(allFolders, 0L, "/"));
        
        return root;
    }
    
    /**
     * 递归构建子节点
     */
    private List<Map<String, Object>> buildTreeChildren(List<Folder> allFolders, Long parentId, String parentPath) {
        List<Map<String, Object>> children = new ArrayList<>();
        
        for (Folder folder : allFolders) {
            if (folder.getParentId().equals(parentId)) {
                String currentPath = "/".equals(parentPath) 
                    ? "/" + folder.getFolderName() 
                    : parentPath + "/" + folder.getFolderName();
                
                Map<String, Object> node = new HashMap<>();
                node.put("id", folder.getId());
                node.put("label", folder.getFolderName());
                node.put("path", currentPath);
                
                List<Map<String, Object>> subChildren = buildTreeChildren(allFolders, folder.getId(), currentPath);
                if (!subChildren.isEmpty()) {
                    node.put("children", subChildren);
                }
                
                children.add(node);
            }
        }
        
        return children;
    }
    
    /**
     * 检查同名文件夹并自动重命名
     * 如果同一父目录下已存在同名文件夹，自动生成新文件夹名（如：图片(1)）
     *
     * @param folderName 原始文件夹名
     * @param parentId 父目录ID（0表示根目录）
     * @param userId 用户ID
     * @return 最终文件夹名（如果存在同名则重命名，否则返回原文件夹名）
     */
    private String checkAndRenameDuplicateFolder(String folderName, Long parentId, Long userId) {
        // 查询同名文件夹（同一用户、同一父目录下）
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getUserId, userId)
               .eq(Folder::getParentId, parentId)
               .eq(Folder::getFolderName, folderName)
               .eq(Folder::getDelFlag, 0);  // 只检查未删除的文件夹
        
        List<Folder> duplicateFolders = folderMapper.selectList(wrapper);
        
        // 如果没有同名文件夹，直接返回原文件夹名
        if (duplicateFolders.isEmpty()) {
            return folderName;
        }
        
        // 有同名文件夹，需要重命名
        // 查找已存在的编号（如：图片(1), 图片(2)）
        Pattern pattern = Pattern.compile("^" + Pattern.quote(folderName) + "\\((\\d+)\\)$");
        int maxNumber = 0;
        
        for (Folder duplicate : duplicateFolders) {
            String dupFolderName = duplicate.getFolderName();
            Matcher matcher = pattern.matcher(dupFolderName);
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }
        
        // 生成新文件夹名
        int newNumber = maxNumber + 1;
        String newFolderName = folderName + "(" + newNumber + ")";
        
        log.info("检测到同名文件夹，自动重命名 - 原文件夹名: {}, 新文件夹名: {}, 父目录ID: {}", 
                 folderName, newFolderName, parentId);
        
        return newFolderName;
    }
    
    /**
     * 检查同名文件夹并自动重命名（用于重命名操作）
     * 如果同一父目录下已存在同名文件夹，自动生成新文件夹名（如：图片(1)）
     * 排除当前正在重命名的文件夹本身
     *
     * @param folderName 原始文件夹名
     * @param parentId 父目录ID（0表示根目录）
     * @param userId 用户ID
     * @param excludeFolderId 要排除的文件夹ID（当前正在重命名的文件夹）
     * @return 最终文件夹名（如果存在同名则重命名，否则返回原文件夹名）
     */
    private String checkAndRenameDuplicateFolderForRename(String folderName, Long parentId, Long userId, Long excludeFolderId) {
        // 查询同名文件夹（同一用户、同一父目录下，排除当前文件夹）
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getUserId, userId)
               .eq(Folder::getParentId, parentId)
               .eq(Folder::getFolderName, folderName)
               .ne(Folder::getId, excludeFolderId)  // 排除当前文件夹
               .eq(Folder::getDelFlag, 0);  // 只检查未删除的文件夹
        
        List<Folder> duplicateFolders = folderMapper.selectList(wrapper);
        
        // 如果没有同名文件夹，直接返回原文件夹名
        if (duplicateFolders.isEmpty()) {
            return folderName;
        }
        
        // 有同名文件夹，需要重命名
        // 查找已存在的编号（如：图片(1), 图片(2)）
        Pattern pattern = Pattern.compile("^" + Pattern.quote(folderName) + "\\((\\d+)\\)$");
        int maxNumber = 0;
        
        for (Folder duplicate : duplicateFolders) {
            String dupFolderName = duplicate.getFolderName();
            Matcher matcher = pattern.matcher(dupFolderName);
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                maxNumber = Math.max(maxNumber, number);
            }
        }
        
        // 生成新文件夹名
        int newNumber = maxNumber + 1;
        String newFolderName = folderName + "(" + newNumber + ")";
        
        log.info("重命名时检测到同名文件夹，自动重命名 - 原文件夹名: {}, 新文件夹名: {}, 父目录ID: {}", 
                 folderName, newFolderName, parentId);
        
        return newFolderName;
    }
}

