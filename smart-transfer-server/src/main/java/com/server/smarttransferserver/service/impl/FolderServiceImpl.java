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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        // 检查同名文件夹
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getParentId, parentId)
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
                .folderName(folderName)
                .parentId(parentId == null ? 0L : parentId)
                .path(path)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        folderMapper.insert(folder);
        log.info("创建文件夹成功 - ID: {}, 名称: {}", folder.getId(), folderName);
        return folder;
    }

    @Override
    public List<Folder> getFoldersByParentId(Long parentId) {
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getParentId, parentId == null ? 0L : parentId)
               .orderByDesc(Folder::getCreateTime);
        return folderMapper.selectList(wrapper);
    }

    @Override
    public FolderContentVO getFolderContent(Long folderId, Integer pageNum, Integer pageSize) {
        Long currentFolderId = folderId == null ? 0L : folderId;
        String currentFolderName = "全部文件";

        if (currentFolderId > 0) {
            Folder current = folderMapper.selectById(currentFolderId);
            if (current != null) {
                currentFolderName = current.getFolderName();
            }
        }

        // 获取子文件夹
        List<Folder> subFolders = getFoldersByParentId(currentFolderId);
        List<FolderVO> folderVOs = subFolders.stream().map(f -> {
            FolderVO vo = new FolderVO();
            BeanUtils.copyProperties(f, vo);
            vo.setType("folder");
            // 统计子文件夹和文件数量
            vo.setSubFolderCount(countSubFolders(f.getId()));
            vo.setFileCount(countFiles(f.getId()));
            return vo;
        }).collect(Collectors.toList());

        // 获取文件（分页）
        Page<FileInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FileInfo> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(FileInfo::getFolderId, currentFolderId)
                   .orderByDesc(FileInfo::getCreateTime);
        Page<FileInfo> filePage = fileInfoMapper.selectPage(page, fileWrapper);

        List<FileInfoVO> fileVOs = filePage.getRecords().stream().map(f -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(f, vo);
            return vo;
        }).collect(Collectors.toList());

        // 获取面包屑
        List<Folder> breadcrumb = getBreadcrumb(currentFolderId);

        return FolderContentVO.builder()
                .currentFolderId(currentFolderId)
                .currentFolderName(currentFolderName)
                .breadcrumb(breadcrumb)
                .folders(folderVOs)
                .files(fileVOs)
                .fileTotal(filePage.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
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

    private Integer countSubFolders(Long folderId) {
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Folder::getParentId, folderId);
        return Math.toIntExact(folderMapper.selectCount(wrapper));
    }

    private Integer countFiles(Long folderId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFolderId, folderId);
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
}

