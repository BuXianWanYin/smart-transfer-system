package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.server.smarttransferserver.dto.TransferTaskQueryDTO;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.TransferTaskMapper;
import com.server.smarttransferserver.vo.TransferTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 传输任务服务实现
 */
@Slf4j
@Service
public class TransferTaskServiceImpl {
    
    @Autowired
    private TransferTaskMapper transferTaskMapper;
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    /**
     * 根据任务ID查询任务
     *
     * @param taskId 任务ID
     * @return 任务VO
     */
    public TransferTaskVO getTaskByTaskId(String taskId) {
        TransferTask task = transferTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            return null;
        }
        return convertToVO(task);
    }
    
    /**
     * 查询任务列表
     *
     * @param queryDTO 查询条件
     * @return 任务列表
     */
    public IPage<TransferTaskVO> queryTasks(TransferTaskQueryDTO queryDTO) {
        // 构建查询条件
        QueryWrapper<TransferTask> queryWrapper = new QueryWrapper<>();
        
        if (queryDTO.getTaskType() != null && !queryDTO.getTaskType().isEmpty()) {
            queryWrapper.eq("task_type", queryDTO.getTaskType());
        }
        
        if (queryDTO.getTransferStatus() != null && !queryDTO.getTransferStatus().isEmpty()) {
            queryWrapper.eq("transfer_status", queryDTO.getTransferStatus());
        }
        
        // 按创建时间倒序
        queryWrapper.orderByDesc("start_time");
        
        // 分页查询
        Page<TransferTask> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<TransferTask> taskPage = transferTaskMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        List<TransferTaskVO> voList = taskPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        Page<TransferTaskVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(voList);
        
        log.info("查询传输任务 - 类型: {}, 状态: {}, 结果数: {}", 
                 queryDTO.getTaskType(), queryDTO.getTransferStatus(), voList.size());
        
        return voPage;
    }
    
    /**
     * 根据文件ID查询任务列表
     *
     * @param fileId 文件ID
     * @return 任务列表
     */
    public List<TransferTaskVO> getTasksByFileId(Long fileId) {
        List<TransferTask> tasks = transferTaskMapper.selectByFileId(fileId);
        return tasks.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新任务状态
     *
     * @param taskId   任务ID
     * @param status   新状态
     * @param progress 进度
     * @param speed    速率
     */
    public void updateTaskStatus(String taskId, String status, Double progress, Long speed) {
        TransferTask task = transferTaskMapper.selectByTaskId(taskId);
        if (task != null) {
            task.setTransferStatus(status);
            if (progress != null) {
                task.setProgress(new java.math.BigDecimal(progress.toString()));
            }
            if (speed != null) {
                task.setTransferSpeed(speed);
            }
            transferTaskMapper.updateById(task);
            
            log.info("更新任务状态 - 任务ID: {}, 状态: {}, 进度: {}, 速率: {}", 
                     taskId, status, progress, speed);
        }
    }
    
    /**
     * 删除任务
     *
     * @param taskId 任务ID
     */
    public void deleteTask(String taskId) {
        TransferTask task = transferTaskMapper.selectByTaskId(taskId);
        if (task != null) {
            transferTaskMapper.deleteById(task.getId());
            log.info("删除任务 - 任务ID: {}", taskId);
        }
    }
    
    /**
     * 转换为VO
     *
     * @param task 任务实体
     * @return 任务VO
     */
    private TransferTaskVO convertToVO(TransferTask task) {
        TransferTaskVO vo = new TransferTaskVO();
        BeanUtils.copyProperties(task, vo);
        
        // 获取文件信息
        FileInfo fileInfo = fileInfoMapper.selectById(task.getFileId());
        if (fileInfo != null) {
            vo.setFileName(fileInfo.getFileName());
            vo.setFileSize(fileInfo.getFileSize());
        }
        
        return vo;
    }
}

