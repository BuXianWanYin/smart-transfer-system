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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    
    /**
     * 根据ID获取文件信息
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
        }
        
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
}

