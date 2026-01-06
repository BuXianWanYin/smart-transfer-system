package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.service.FileInfoService;
import com.server.smarttransferserver.vo.FileInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文件信息服务实现
 */
@Slf4j
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
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
     * 删除文件
     *
     * @param id 文件ID
     */
    @Override
    public void deleteFile(Long id) {
        removeById(id);
        log.info("删除文件 - ID: {}", id);
    }
}

