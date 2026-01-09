package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.service.FileInfoService;
import com.server.smarttransferserver.service.RecoveryFileService;
import com.server.smarttransferserver.vo.FileInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件信息服务实现
 */
@Slf4j
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Lazy
    @Autowired
    private RecoveryFileService recoveryFileService;
    
    @Autowired
    private com.server.smarttransferserver.service.FolderService folderService;
    
    @org.springframework.beans.factory.annotation.Value("${file.upload.path:./uploads}")
    private String uploadPath;
    
    /**
     * 根据ID获取文件信息
     * 只返回上传完成的文件
     *
     * @param id 文件ID
     * @return 文件信息VO
     */
    @Override
    public FileInfoVO getFileById(Long id) {
        FileInfo fileInfo = fileInfoMapper.selectById(id);
        if (fileInfo == null) {
            return null;
        }
        
        // 只返回上传完成的文件，未完成的文件视为不存在
        if (!"COMPLETED".equals(fileInfo.getUploadStatus())) {
            log.warn("尝试访问未完成上传的文件 - ID: {}, 状态: {}", id, fileInfo.getUploadStatus());
            return null;
        }
        
        FileInfoVO vo = new FileInfoVO();
        BeanUtils.copyProperties(fileInfo, vo);
        return vo;
    }
    
    /**
     * 分页查询文件列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param status   上传状态（可选）
     * @return 文件列表
     */
    @Override
    public IPage<FileInfoVO> getFileList(Integer pageNum, Integer pageSize, String status) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("upload_status", status);
        } else {
            // 默认只查询上传完成的文件
            queryWrapper.eq("upload_status", "COMPLETED");
        }
        
        // 只查询未删除的文件
        queryWrapper.eq("del_flag", 0);
        
        // 按创建时间倒序
        queryWrapper.orderByDesc("create_time");
        
        // 分页查询
        Page<FileInfo> page = new Page<>(pageNum, pageSize);
        IPage<FileInfo> filePage = fileInfoMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<FileInfoVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(filePage.getTotal());
        voPage.setRecords(filePage.getRecords().stream().map(fileInfo -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(fileInfo, vo);
            return vo;
        }).collect(java.util.stream.Collectors.toList()));
        
        log.info("查询文件列表 - 状态: {}, 结果数: {}", status, voPage.getRecords().size());
        return voPage;
    }
    
    /**
     * 根据文件哈希查询文件信息
     *
     * @param fileHash 文件哈希值
     * @return 文件信息，不存在返回null
     */
    @Override
    public FileInfo getByFileHash(String fileHash) {
        return fileInfoMapper.selectByFileHash(fileHash);
    }
    
    /**
     * 更新文件上传状态
     *
     * @param fileId 文件ID
     * @param status 上传状态
     * @return 是否更新成功
     */
    @Override
    public boolean updateUploadStatus(Long fileId, String status) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(fileId);
        fileInfo.setUploadStatus(status);
        return updateById(fileInfo);
    }
    
    /**
     * 删除文件（移动到回收站）
     *
     * @param id 文件ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        // 将文件移至回收站（逻辑删除）
        recoveryFileService.deleteFileToRecovery(id);
        log.info("文件已移至回收站 - ID: {}", id);
    }
    
    /**
     * 搜索文件
     *
     * @param fileName 文件名关键词
     * @return 文件列表
     */
    @Override
    public List<FileInfoVO> searchByFileName(String fileName) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(FileInfo::getFileName, fileName)
                    .eq(FileInfo::getDelFlag, 0)
                    .orderByDesc(FileInfo::getCreateTime);
        
        List<FileInfo> fileList = list(queryWrapper);
        return fileList.stream().map(fileInfo -> {
            FileInfoVO vo = new FileInfoVO();
            BeanUtils.copyProperties(fileInfo, vo);
            return vo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 重命名文件
     *
     * @param id 文件ID
     * @param newFileName 新文件名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(Long id, String newFileName) {
        FileInfo fileInfo = getById(id);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        
        fileInfo.setFileName(newFileName);
        fileInfo.setUpdateTime(LocalDateTime.now());
        updateById(fileInfo);
        log.info("文件重命名 - ID: {}, 新文件名: {}", id, newFileName);
    }
    
    /**
     * 移动文件到指定文件夹
     *
     * @param id 文件ID
     * @param targetFolderId 目标文件夹ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveFile(Long id, Long targetFolderId) {
        FileInfo fileInfo = getById(id);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        
        fileInfo.setFolderId(targetFolderId);
        fileInfo.setUpdateTime(LocalDateTime.now());
        updateById(fileInfo);
        log.info("文件移动 - ID: {}, 目标文件夹: {}", id, targetFolderId);
    }
    
    /**
     * 批量移动文件
     *
     * @param fileIds 文件ID列表
     * @param targetFolderId 目标文件夹ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMoveFiles(List<Long> fileIds, Long targetFolderId) {
        for (Long fileId : fileIds) {
            moveFile(fileId, targetFolderId);
        }
        log.info("批量移动文件 - 数量: {}, 目标文件夹: {}", fileIds.size(), targetFolderId);
    }
    
    /**
     * 批量删除文件（移动到回收站）
     *
     * @param ids 文件ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteFiles(List<Long> ids) {
        recoveryFileService.batchDeleteToRecovery(ids);
        log.info("批量删除文件到回收站 - 数量: {}", ids.size());
    }
    
    /**
     * 复制文件
     *
     * @param fileId 文件ID
     * @param targetFolderId 目标文件夹ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyFile(Long fileId, Long targetFolderId) {
        FileInfo sourceFile = getById(fileId);
        if (sourceFile == null) {
            throw new RuntimeException("源文件不存在");
        }
        
        try {
            // 复制物理文件
            File source = new File(sourceFile.getFilePath());
            if (!source.exists()) {
                throw new RuntimeException("源文件物理路径不存在");
            }
            
            // 生成新的文件路径
            String newFileName = generateCopyFileName(sourceFile.getFileName());
            String newFilePath = uploadPath + File.separator + UUID.randomUUID() + 
                    "_" + newFileName;
            File dest = new File(newFilePath);
            dest.getParentFile().mkdirs();
            
            // 复制文件内容
            Files.copy(source.toPath(), dest.toPath());
            
            // 创建新的文件记录
            Long userId = com.server.smarttransferserver.util.UserContextHolder.getUserId();
            FileInfo newFile = new FileInfo();
            newFile.setUserId(userId);
            newFile.setFileName(newFileName);
            newFile.setExtendName(sourceFile.getExtendName());
            newFile.setFileSize(sourceFile.getFileSize());
            newFile.setFileHash(sourceFile.getFileHash());
            newFile.setFilePath(newFilePath);
            newFile.setIsDir(0);
            newFile.setFolderId(targetFolderId);
            newFile.setUploadStatus("COMPLETED");
            newFile.setDelFlag(0);
            newFile.setCreateTime(LocalDateTime.now());
            newFile.setUpdateTime(LocalDateTime.now());
            
            save(newFile);
            log.info("文件复制成功 - 源ID: {}, 新ID: {}, 目标文件夹: {}", 
                    fileId, newFile.getId(), targetFolderId);
            
        } catch (IOException e) {
            log.error("复制文件失败", e);
            throw new RuntimeException("复制文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成复制文件的新名称
     */
    private String generateCopyFileName(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            String baseName = originalName.substring(0, dotIndex);
            String extension = originalName.substring(dotIndex);
            return baseName + "_副本" + extension;
        } else {
            return originalName + "_副本";
        }
    }
    
    /**
     * 批量复制文件
     *
     * @param fileIds 文件ID列表
     * @param targetFolderId 目标文件夹ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCopyFiles(List<Long> fileIds, Long targetFolderId) {
        for (Long fileId : fileIds) {
            copyFile(fileId, targetFolderId);
        }
        log.info("批量复制文件成功 - 数量: {}, 目标文件夹: {}", fileIds.size(), targetFolderId);
    }
    
    /**
     * 解压文件
     *
     * @param fileId 文件ID
     * @param unzipMode 解压模式：1-当前文件夹，2-新建文件夹，3-指定路径
     * @param folderName 新建的文件夹名称（模式2使用）
     * @param targetFolderId 目标文件夹ID（模式3使用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unzipFile(Long fileId, Integer unzipMode, String folderName, Long targetFolderId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        
        String extendName = fileInfo.getExtendName();
        if (extendName == null || !extendName.equalsIgnoreCase("zip")) {
            throw new RuntimeException("仅支持解压 ZIP 格式文件");
        }
        
        File zipFile = new File(fileInfo.getFilePath());
        if (!zipFile.exists()) {
            throw new RuntimeException("压缩文件不存在");
        }
        
        // 确定目标文件夹ID
        Long destFolderId;
        switch (unzipMode) {
            case 1: // 当前文件夹
                destFolderId = fileInfo.getFolderId();
                break;
            case 2: // 新建文件夹
                com.server.smarttransferserver.domain.Folder newFolder = 
                        folderService.createFolder(folderName, fileInfo.getFolderId());
                destFolderId = newFolder.getId();
                break;
            case 3: // 指定路径
                destFolderId = targetFolderId;
                break;
            default:
                throw new RuntimeException("无效的解压模式");
        }
        
        // 解压文件
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    // 跳过目录，可以后续扩展支持
                    continue;
                }
                
                String entryName = entry.getName();
                // 处理嵌套目录中的文件名
                int lastSlash = entryName.lastIndexOf('/');
                if (lastSlash >= 0) {
                    entryName = entryName.substring(lastSlash + 1);
                }
                
                if (entryName.isEmpty()) {
                    continue;
                }
                
                // 创建解压后的文件
                String newFilePath = uploadPath + File.separator + UUID.randomUUID() + 
                        "_" + entryName;
                File newFile = new File(newFilePath);
                newFile.getParentFile().mkdirs();
                
                // 写入文件内容
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                
                // 计算解压文件的MD5
                String fileHash = org.apache.commons.codec.digest.DigestUtils.md5Hex(
                        new java.io.FileInputStream(newFile));
                
                // 创建文件记录
                Long userId = com.server.smarttransferserver.util.UserContextHolder.getUserId();
                FileInfo extractedFile = new FileInfo();
                extractedFile.setUserId(userId);
                extractedFile.setFileName(entryName);
                extractedFile.setExtendName(extractExtendName(entryName));
                extractedFile.setFileSize(newFile.length());
                extractedFile.setFileHash(fileHash);
                extractedFile.setFilePath(newFilePath);
                extractedFile.setIsDir(0);
                extractedFile.setFolderId(destFolderId);
                extractedFile.setUploadStatus("COMPLETED");
                extractedFile.setDelFlag(0);
                extractedFile.setCreateTime(LocalDateTime.now());
                extractedFile.setUpdateTime(LocalDateTime.now());
                
                save(extractedFile);
                log.info("解压文件 - 文件名: {}, ID: {}", entryName, extractedFile.getId());
                
                zis.closeEntry();
            }
            
            log.info("解压完成 - 源文件ID: {}, 目标文件夹: {}", fileId, destFolderId);
            
        } catch (IOException e) {
            log.error("解压文件失败", e);
            throw new RuntimeException("解压文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 从文件名中提取扩展名
     */
    private String extractExtendName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return null;
    }
}

