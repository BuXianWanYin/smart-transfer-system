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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    @Transactional
    public Folder createFolder(String folderName, Long parentId) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }
        
        // 检查同名文件夹（同一用户下）
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getUserId, userId)
               .eq(Folder::getParentId, parentId)
               .eq(Folder::getFolderName, folderName);
        if (folderMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("同名文件夹已存在");
        }

        // 构建路径
        String path = "/";
        if (parentId != null && parentId > 0) {
            Folder parent = folderMapper.selectById(parentId);
            if (parent != null) {
                path = parent.getPath() + parent.getFolderName() + "/";
            }
        }

        Folder folder = Folder.builder()
                .userId(userId)
                .folderName(folderName)
                .parentId(parentId == null ? 0L : parentId)
                .path(path)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        folderMapper.insert(folder);
        log.info("创建文件夹成功 - ID: {}, 名称: {}, 用户: {}", folder.getId(), folderName, userId);
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

        // 获取文件（分页）- 只查询未删除的、当前用户的文件
        Page<FileInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FileInfo> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(FileInfo::getDelFlag, 0); // 过滤已删除文件
        
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

        // 检查同名
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getParentId, folder.getParentId())
               .eq(Folder::getFolderName, newName)
               .ne(Folder::getId, folderId);
        if (folderMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("同名文件夹已存在");
        }

        folder.setFolderName(newName);
        folder.setUpdateTime(LocalDateTime.now());
        folderMapper.updateById(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long folderId) {
        // 递归删除子文件夹
        List<Folder> subFolders = getFoldersByParentId(folderId);
        for (Folder sub : subFolders) {
            deleteFolder(sub.getId());
        }

        // 删除文件夹内的文件记录
        LambdaQueryWrapper<FileInfo> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(FileInfo::getFolderId, folderId);
        fileInfoMapper.delete(fileWrapper);

        // 删除文件夹
        folderMapper.deleteById(folderId);
        log.info("删除文件夹 - ID: {}", folderId);
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
        file.setFolderId(folderId == null ? 0L : folderId);
        file.setUpdateTime(LocalDateTime.now());
        fileInfoMapper.updateById(file);
    }

    @Override
    @Transactional
    public void moveFolderTo(Long folderId, Long targetFolderId) {
        if (folderId.equals(targetFolderId)) {
            throw new RuntimeException("不能移动到自身");
        }

        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        // 检查是否移动到子文件夹（会造成循环）
        if (isSubFolder(folderId, targetFolderId)) {
            throw new RuntimeException("不能移动到子文件夹");
        }

        folder.setParentId(targetFolderId == null ? 0L : targetFolderId);
        folder.setUpdateTime(LocalDateTime.now());

        // 更新路径
        String newPath = "/";
        if (targetFolderId != null && targetFolderId > 0) {
            Folder parent = folderMapper.selectById(targetFolderId);
            if (parent != null) {
                newPath = parent.getPath() + parent.getFolderName() + "/";
            }
        }
        folder.setPath(newPath);

        folderMapper.updateById(folder);
    }

    private Integer countSubFolders(Long folderId, Long userId) {
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getParentId, folderId);
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
        
        // 获取当前用户的所有文件夹
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Folder::getUserId, userId);
        }
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
}

