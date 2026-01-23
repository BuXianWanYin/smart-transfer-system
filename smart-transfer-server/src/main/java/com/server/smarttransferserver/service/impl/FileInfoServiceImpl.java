package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.domain.Folder;
import com.server.smarttransferserver.mapper.FolderMapper;
import com.server.smarttransferserver.service.FileInfoService;
import com.server.smarttransferserver.service.FolderService;
import com.server.smarttransferserver.service.IFileStorageService;
import com.server.smarttransferserver.service.RecoveryFileService;
import com.server.smarttransferserver.util.UserContextHolder;
import com.server.smarttransferserver.vo.FileInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private FolderService folderService;
    
    @Autowired
    private FolderMapper folderMapper;
    
    @Autowired
    private IFileStorageService fileStorageService;
    
    @Value("${file.upload.path:./uploads}")
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
        Long userId = UserContextHolder.getUserId();
        FileInfo fileInfo = fileInfoMapper.selectById(id);
        if (fileInfo == null) {
            return null;
        }
        
        // 检查用户权限：只能访问自己的文件
        if (userId == null || !userId.equals(fileInfo.getUserId())) {
            log.warn("用户尝试访问其他用户的文件 - 用户ID: {}, 文件ID: {}, 文件所有者: {}", 
                     userId, id, fileInfo.getUserId());
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
     * @param filterUserId 用户ID（可选，仅管理员可用，用于筛选指定用户的文件）
     * @return 文件列表
     */
    @Override
    public IPage<FileInfoVO> getFileList(Integer pageNum, Integer pageSize, String status, Long filterUserId) {
        Long currentUserId = UserContextHolder.getUserId();
        String currentUserRole = UserContextHolder.getRole();
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        
        // 用户数据隔离逻辑：
        // 1. 如果是管理员，且指定了filterUserId，则查询指定用户的文件
        // 2. 如果是管理员，且未指定filterUserId，则查询所有用户的文件
        // 3. 如果是普通用户，只能查询自己的文件
        if ("ADMIN".equals(currentUserRole)) {
            // 管理员：如果指定了filterUserId，查询指定用户；否则查询所有用户
            if (filterUserId != null) {
                queryWrapper.eq("user_id", filterUserId);
            }
            // 如果未指定filterUserId，不添加userId条件，查询所有用户
        } else {
            // 普通用户：只能查询自己的文件
            if (currentUserId != null) {
                queryWrapper.eq("user_id", currentUserId);
            }
        }
        
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
        }).collect(Collectors.toList()));
        
        log.info("查询文件列表 - 当前用户ID: {}, 角色: {}, 筛选用户ID: {}, 状态: {}, 结果数: {}", 
                currentUserId, currentUserRole, filterUserId, status, voPage.getRecords().size());
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
        FileInfo fileInfo = getById(id);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 注意：上传中的文件不会出现在文件列表中（只显示uploadStatus=COMPLETED的文件）
        // 用户只能在传输中心暂停或取消上传，无法删除正在上传的文件
        
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
        Long userId = UserContextHolder.getUserId();
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        
        // 用户数据隔离：只搜索当前用户的文件
        if (userId != null) {
            queryWrapper.eq(FileInfo::getUserId, userId);
        }
        
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
        if (newFileName == null || newFileName.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        
        FileInfo fileInfo = getById(id);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 检查同名文件并自动重命名（同一文件夹下，排除当前文件）
        Long userId = UserContextHolder.getUserId();
        Long folderId = fileInfo.getFolderId() != null ? fileInfo.getFolderId() : 0L;
        String finalFileName = checkAndRenameDuplicateFileForRename(newFileName, folderId, userId, id);
        
        fileInfo.setFileName(finalFileName);
        fileInfo.setUpdateTime(LocalDateTime.now());
        updateById(fileInfo);
        log.info("文件重命名 - ID: {}, 新文件名: {}", id, finalFileName);
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
        
        // 规范化目标文件夹ID
        Long normalizedTargetFolderId = (targetFolderId == null || targetFolderId == 0) ? 0L : targetFolderId;
        
        // 如果目标文件夹不为0，验证目标文件夹是否存在
        if (normalizedTargetFolderId > 0) {
            Folder targetFolder = folderMapper.selectById(normalizedTargetFolderId);
            if (targetFolder == null) {
                throw new RuntimeException("目标文件夹不存在");
            }
        }
        
        // 如果移动到不同文件夹，检查目标文件夹是否有同名文件
        Long currentFolderId = fileInfo.getFolderId() != null ? fileInfo.getFolderId() : 0L;
        if (!currentFolderId.equals(normalizedTargetFolderId)) {
            Long userId = UserContextHolder.getUserId();
            String finalFileName = checkAndRenameDuplicateFileForRename(
                    fileInfo.getFileName(), normalizedTargetFolderId, userId, id);
            
            if (!finalFileName.equals(fileInfo.getFileName())) {
                log.info("移动文件时检测到同名文件，自动重命名 - 原文件名: {}, 新文件名: {}, 目标文件夹: {}", 
                         fileInfo.getFileName(), finalFileName, normalizedTargetFolderId);
                fileInfo.setFileName(finalFileName);
            }
        }
        
        fileInfo.setFolderId(normalizedTargetFolderId);
        fileInfo.setUpdateTime(LocalDateTime.now());
        updateById(fileInfo);
        log.info("文件移动 - ID: {}, 目标文件夹: {}", id, normalizedTargetFolderId);
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
        if (fileIds == null || fileIds.isEmpty()) {
            throw new RuntimeException("文件ID列表不能为空");
        }
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
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("文件ID列表不能为空");
        }
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
            // 复制物理文件（获取绝对路径，兼容相对路径和绝对路径）
            Path sourcePath = fileStorageService.getAbsoluteFilePath(sourceFile.getFilePath());
            File source = sourcePath.toFile();
            if (!source.exists()) {
                throw new RuntimeException("源文件物理路径不存在: " + sourcePath);
            }
            
            // 规范化目标文件夹ID
            Long normalizedTargetFolderId = (targetFolderId == null || targetFolderId == 0) ? 0L : targetFolderId;
            
            // 验证目标文件夹是否存在（如果targetFolderId不为0）
            if (normalizedTargetFolderId > 0) {
                Folder targetFolder = folderMapper.selectById(normalizedTargetFolderId);
                if (targetFolder == null) {
                    throw new RuntimeException("目标文件夹不存在");
                }
            }
            
            // 创建新的文件记录
            Long userId = UserContextHolder.getUserId();
            
            // 检查目标文件夹是否有同名文件，如果有则自动重命名
            String newFileName = checkAndRenameDuplicateFileForCopy(
                    sourceFile.getFileName(), normalizedTargetFolderId, userId);
            
            // 使用FileStorageService保存文件，返回相对路径
            String relativePath = fileStorageService.saveFile(source, newFileName, userId);
            
            FileInfo newFile = new FileInfo();
            newFile.setUserId(userId);
            newFile.setFileName(newFileName);
            newFile.setExtendName(sourceFile.getExtendName());
            newFile.setFileSize(sourceFile.getFileSize());
            newFile.setFileHash(sourceFile.getFileHash());
            newFile.setFilePath(relativePath);
            newFile.setIsDir(0);
            newFile.setFolderId(normalizedTargetFolderId);
            newFile.setUploadStatus("COMPLETED");
            newFile.setDelFlag(0);
            newFile.setCreateTime(LocalDateTime.now());
            newFile.setUpdateTime(LocalDateTime.now());
            
            save(newFile);
            log.info("文件复制成功 - 源ID: {}, 新ID: {}, 目标文件夹: {}, 文件名: {}", 
                    fileId, newFile.getId(), normalizedTargetFolderId, newFileName);
            
        } catch (IOException e) {
            log.error("复制文件失败", e);
            throw new RuntimeException("复制文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查同名文件并自动重命名（用于重命名操作）
     * 如果同一文件夹下已存在同名文件，自动生成新文件名（如：图片(1).jpg）
     * 排除当前正在重命名的文件本身
     *
     * @param fileName 原始文件名
     * @param folderId 文件夹ID（0表示根目录）
     * @param userId 用户ID
     * @param excludeFileId 要排除的文件ID（当前正在重命名的文件）
     * @return 最终文件名（如果存在同名则重命名，否则返回原文件名）
     */
    private String checkAndRenameDuplicateFileForRename(String fileName, Long folderId, Long userId, Long excludeFileId) {
        // 查询同名文件（同一用户、同一文件夹下，排除当前文件）
        List<FileInfo> duplicateFiles = fileInfoMapper.selectByFileNameAndFolder(fileName, folderId, userId);
        
        // 过滤掉当前文件本身
        duplicateFiles.removeIf(f -> f.getId().equals(excludeFileId));
        
        // 如果没有同名文件，直接返回原文件名
        if (duplicateFiles.isEmpty()) {
            return fileName;
        }
        
        // 有同名文件，需要重命名
        return generateUniqueFileName(fileName, duplicateFiles);
    }
    
    /**
     * 检查同名文件并自动重命名（用于复制操作）
     * 如果目标文件夹下已存在同名文件，自动生成新文件名
     *
     * @param fileName 原始文件名
     * @param folderId 文件夹ID（0表示根目录）
     * @param userId 用户ID
     * @return 最终文件名（如果存在同名则重命名，否则返回原文件名）
     */
    private String checkAndRenameDuplicateFileForCopy(String fileName, Long folderId, Long userId) {
        // 查询同名文件（同一用户、同一文件夹下）
        List<FileInfo> duplicateFiles = fileInfoMapper.selectByFileNameAndFolder(fileName, folderId, userId);
        
        // 如果没有同名文件，直接返回原文件名
        if (duplicateFiles.isEmpty()) {
            return fileName;
        }
        
        // 有同名文件，需要重命名
        return generateUniqueFileName(fileName, duplicateFiles);
    }
    
    /**
     * 检查同名文件并自动重命名（用于解压操作）
     * 如果目标文件夹下已存在同名文件，自动生成新文件名
     *
     * @param fileName 原始文件名
     * @param folderId 文件夹ID（0表示根目录）
     * @param userId 用户ID
     * @return 最终文件名（如果存在同名则重命名，否则返回原文件名）
     */
    private String checkAndRenameDuplicateFileForUnzip(String fileName, Long folderId, Long userId) {
        // 查询同名文件（同一用户、同一文件夹下）
        List<FileInfo> duplicateFiles = fileInfoMapper.selectByFileNameAndFolder(fileName, folderId, userId);
        
        // 如果没有同名文件，直接返回原文件名
        if (duplicateFiles.isEmpty()) {
            return fileName;
        }
        
        // 有同名文件，需要重命名
        return generateUniqueFileName(fileName, duplicateFiles);
    }
    
    /**
     * 生成唯一的文件名（自动编号）
     * 如果存在同名文件，自动生成新文件名（如：图片(1).jpg）
     *
     * @param originalFileName 原始文件名
     * @param duplicateFiles 同名文件列表
     * @return 唯一的文件名
     */
    private String generateUniqueFileName(String originalFileName, List<FileInfo> duplicateFiles) {
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
        
        // 查找已存在的编号（如：图片(1).jpg, 图片(2).jpg）
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
        String newFileName = baseName + "(" + newNumber + ")" + extension;
        
        log.info("检测到同名文件，自动重命名 - 原文件名: {}, 新文件名: {}", 
                 originalFileName, newFileName);
        
        return newFileName;
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
        if (fileIds == null || fileIds.isEmpty()) {
            throw new RuntimeException("文件ID列表不能为空");
        }
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
        
        // 获取绝对路径（兼容相对路径和绝对路径）
        Path zipFilePath = fileStorageService.getAbsoluteFilePath(fileInfo.getFilePath());
        File zipFile = zipFilePath.toFile();
        if (!zipFile.exists()) {
            throw new RuntimeException("压缩文件不存在: " + zipFilePath);
        }
        
        // 确定目标文件夹ID
        Long destFolderId;
        Long currentFolderId = fileInfo.getFolderId() != null ? fileInfo.getFolderId() : 0L;
        switch (unzipMode) {
            case 1: // 当前文件夹
                destFolderId = currentFolderId;
                break;
            case 2: // 新建文件夹
                Folder newFolder = folderService.createFolder(folderName, currentFolderId);
                destFolderId = newFolder.getId();
                break;
            case 3: // 指定路径
                Long normalizedTargetFolderId = (targetFolderId == null || targetFolderId == 0) ? 0L : targetFolderId;
                // 验证目标文件夹是否存在（如果targetFolderId不为0）
                if (normalizedTargetFolderId > 0) {
                    Folder targetFolder = folderMapper.selectById(normalizedTargetFolderId);
                    if (targetFolder == null) {
                        throw new RuntimeException("目标文件夹不存在");
                    }
                }
                destFolderId = normalizedTargetFolderId;
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
                
                // 创建临时文件用于解压
                File tempFile = File.createTempFile("unzip_", "_" + entryName);
                try {
                    // 写入文件内容
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    
                    // 计算解压文件的MD5
                    String fileHash;
                    try (FileInputStream hashFis = new FileInputStream(tempFile)) {
                        fileHash = DigestUtils.md5Hex(hashFis);
                    }
                    
                    // 创建文件记录
                    Long userId = UserContextHolder.getUserId();
                    
                    // 检查目标文件夹是否有同名文件，如果有则自动重命名
                    String finalEntryName = checkAndRenameDuplicateFileForUnzip(
                            entryName, destFolderId, userId);
                    
                    // 使用FileStorageService保存文件，返回相对路径
                    String relativePath = fileStorageService.saveFile(tempFile, finalEntryName, userId);
                    
                    FileInfo extractedFile = new FileInfo();
                    extractedFile.setUserId(userId);
                    extractedFile.setFileName(finalEntryName);
                    extractedFile.setExtendName(extractExtendName(entryName));
                    extractedFile.setFileSize(tempFile.length());
                    extractedFile.setFileHash(fileHash);
                    extractedFile.setFilePath(relativePath);
                    extractedFile.setIsDir(0);
                    extractedFile.setFolderId(destFolderId);
                    extractedFile.setUploadStatus("COMPLETED");
                    extractedFile.setDelFlag(0);
                    extractedFile.setCreateTime(LocalDateTime.now());
                    extractedFile.setUpdateTime(LocalDateTime.now());
                    
                    save(extractedFile);
                    log.info("解压文件 - 文件名: {}, ID: {}", finalEntryName, extractedFile.getId());
                } finally {
                    // 修复：清理临时文件，确保删除成功
                    if (tempFile.exists()) {
                        boolean deleted = tempFile.delete();
                        if (!deleted) {
                            log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                            tempFile.deleteOnExit(); // 尝试在JVM退出时删除
                        }
                    }
                }
                
                zis.closeEntry();
            }
            
            log.info("解压完成 - 源文件ID: {}, 目标文件夹: {}", fileId, destFolderId);
            
        } catch (IOException e) {
            log.error("解压文件失败", e);
            throw new RuntimeException("解压文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public void batchDownloadFiles(List<Long> fileIds, javax.servlet.http.HttpServletResponse response) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new RuntimeException("文件ID列表不能为空");
        }
        
        try {
            // 设置响应头
            String fileName = "files_" + System.currentTimeMillis() + ".zip";
            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            
            // 创建ZIP输出流
            try (java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
                for (Long id : fileIds) {
                    FileInfoVO fileInfo = getFileById(id);
                    if (fileInfo == null || fileInfo.getIsDir() == 1) {
                        continue;
                    }
                    
                    // 获取绝对路径（兼容相对路径和绝对路径）
                    java.nio.file.Path filePath = fileStorageService.getAbsoluteFilePath(fileInfo.getFilePath());
                    File file = filePath.toFile();
                    if (!file.exists()) {
                        log.warn("文件不存在 - ID: {}, 路径: {}", id, filePath);
                        continue;
                    }
                    
                    // 添加文件到ZIP
                    String entryName = fileInfo.getFileName();
                    if (fileInfo.getExtendName() != null && !fileInfo.getFileName().contains(".")) {
                        entryName = fileInfo.getFileName() + "." + fileInfo.getExtendName();
                    }
                    
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipOut.putNextEntry(zipEntry);
                    
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zipOut.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    zipOut.closeEntry();
                }
                
                zipOut.finish();
            }
        } catch (Exception e) {
            log.error("批量下载失败", e);
            throw new RuntimeException("批量下载失败: " + e.getMessage());
        }
    }
    
    @Override
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadFile(Long fileId, String rangeHeader) {
        FileInfoVO fileInfo = getFileById(fileId);
        if (fileInfo == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        // 获取绝对路径（兼容相对路径和绝对路径）
        java.nio.file.Path filePath = fileStorageService.getAbsoluteFilePath(fileInfo.getFilePath());
        File file = filePath.toFile();
        if (!file.exists()) {
            log.error("文件不存在 - 路径: {}", filePath);
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        long fileLength = file.length();
        
        // 编码文件名
        String encodedFileName;
        try {
            encodedFileName = java.net.URLEncoder.encode(fileInfo.getFileName(), "UTF-8")
                    .replaceAll("\\+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 是标准编码，理论上不会抛出此异常，但为了编译通过需要处理
            encodedFileName = fileInfo.getFileName();
            log.warn("文件名编码失败，使用原始文件名: {}", fileInfo.getFileName());
        }
        
        // 支持断点续传 - 解析Range头
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            // 修复：检查数组长度，防止数组越界
            if (ranges.length == 0 || ranges[0].isEmpty()) {
                log.warn("Range头格式错误: {}", rangeHeader);
                return org.springframework.http.ResponseEntity.status(416).build(); // Range Not Satisfiable
            }
            
            long start;
            long end;
            try {
                start = Long.parseLong(ranges[0]);
                end = ranges.length > 1 && !ranges[1].isEmpty() 
                        ? Long.parseLong(ranges[1]) : fileLength - 1;
            } catch (NumberFormatException e) {
                log.warn("Range头格式错误（数字解析失败）: {}", rangeHeader);
                return org.springframework.http.ResponseEntity.status(416).build(); // Range Not Satisfiable
            }
            
            // 校验范围
            if (start >= fileLength) {
                return org.springframework.http.ResponseEntity.status(416).build(); // Range Not Satisfiable
            }
            if (end >= fileLength) {
                end = fileLength - 1;
            }
            
            final long contentLength = end - start + 1;
            final long startPos = start;
            
            // 使用InputStreamResource返回部分内容
            org.springframework.core.io.InputStreamResource resource;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                fis.skip(startPos);
                
                final FileInputStream finalFis = fis;
                fis = null; // 转移所有权给FilterInputStream，避免在finally中关闭
                resource = new org.springframework.core.io.InputStreamResource(
                        new java.io.FilterInputStream(finalFis) {
                            private long remaining = contentLength;
                            
                            @Override
                            public int read() throws java.io.IOException {
                                if (remaining <= 0) {
                                    close();
                                    return -1;
                                }
                                remaining--;
                                return super.read();
                            }
                            
                            @Override
                            public int read(byte[] b, int off, int len) throws java.io.IOException {
                                if (remaining <= 0) {
                                    close();
                                    return -1;
                                }
                                len = (int) Math.min(len, remaining);
                                int read = super.read(b, off, len);
                                if (read > 0) remaining -= read;
                                return read;
                            }
                            
                            @Override
                            public void close() throws java.io.IOException {
                                super.close();
                            }
                        });
            } catch (java.io.FileNotFoundException e) {
                // 文件路径存在但文件不存在，这是系统状态不一致的问题，返回500
                log.error("文件未找到（系统错误）: {}", file.getAbsolutePath(), e);
                return org.springframework.http.ResponseEntity.status(500).build();
            } catch (java.io.IOException e) {
                log.error("读取文件失败: {}", file.getAbsolutePath(), e);
                return org.springframework.http.ResponseEntity.status(500).build();
            } finally {
                // 修复：确保在异常情况下关闭FileInputStream
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (java.io.IOException e) {
                        log.warn("关闭FileInputStream失败", e);
                    }
                }
            }
            
            return org.springframework.http.ResponseEntity.status(206) // Partial Content
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(org.springframework.http.HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                    .header(org.springframework.http.HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(contentLength)
                    .body(resource);
        }
        
        // 普通下载（无Range请求）
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .header(org.springframework.http.HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(fileLength)
                .body(resource);
    }
    
    @Override
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> previewFile(Long fileId) {
        FileInfoVO fileInfo = getFileById(fileId);
        if (fileInfo == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        // 获取绝对路径（兼容相对路径和绝对路径）
        java.nio.file.Path filePath = fileStorageService.getAbsoluteFilePath(fileInfo.getFilePath());
        File file = filePath.toFile();
        if (!file.exists()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        String contentType = getContentType(fileInfo.getExtendName());
        
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(resource);
    }
    
    /**
     * 获取文件MIME类型
     */
    private String getContentType(String extendName) {
        if (extendName == null) return "application/octet-stream";
        
        switch (extendName.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "pdf":
                return "application/pdf";
            case "txt":
            case "md":
                return "text/plain";
            case "html":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
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

