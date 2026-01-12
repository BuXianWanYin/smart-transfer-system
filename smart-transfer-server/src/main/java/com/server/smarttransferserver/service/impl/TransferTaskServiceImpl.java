package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.dto.TransferTaskQueryDTO;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.mapper.CongestionMetricsMapper;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.TransferTaskMapper;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.vo.TransferTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 传输任务服务实现
 */
@Slf4j
@Service
public class TransferTaskServiceImpl extends ServiceImpl<TransferTaskMapper, TransferTask> implements TransferTaskService {
    
    @Autowired
    private TransferTaskMapper transferTaskMapper;
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private CongestionMetricsMapper congestionMetricsMapper;
    
    /**
     * 创建传输任务
     *
     * @param fileId 文件ID
     * @param taskType 任务类型（UPLOAD/DOWNLOAD）
     * @return 任务ID
     */
    @Override
    @Transactional
    public String createTask(Long fileId, String taskType) {
        String taskId = UUID.randomUUID().toString();
        
        TransferTask task = TransferTask.builder()
                .taskId(taskId)
                .fileId(fileId)
                .taskType(taskType)
                .transferStatus("PENDING")
                .progress(BigDecimal.ZERO)
                .startTime(LocalDateTime.now())
                .build();
        
        save(task);
        log.info("创建传输任务 - 任务ID: {}, 文件ID: {}, 类型: {}", taskId, fileId, taskType);
        
        return taskId;
    }
    
    /**
     * 根据任务ID查询任务
     *
     * @param taskId 任务ID
     * @return 任务VO
     */
    @Override
    public TransferTaskVO getTaskByTaskId(String taskId) {
        TransferTask task = transferTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            return null;
        }
        return convertToVO(task);
    }
    
    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 传输状态
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateTaskStatus(String taskId, String status) {
        TransferTask task = transferTaskMapper.selectByTaskId(taskId);
        if (task != null) {
            task.setTransferStatus(status);
            if ("COMPLETED".equals(status)) {
                task.setEndTime(LocalDateTime.now());
            }
            return updateById(task);
        }
        return false;
    }
    
    /**
     * 更新任务进度
     *
     * @param taskId 任务ID
     * @param progress 进度百分比
     * @param transferSpeed 传输速率
     * @param cwnd 拥塞窗口大小
     * @param rtt 往返时延
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateTaskProgress(String taskId, Integer progress, Long transferSpeed, Long cwnd, Long rtt) {
        TransferTask task = transferTaskMapper.selectByTaskId(taskId);
        if (task != null) {
            task.setProgress(BigDecimal.valueOf(progress));
            task.setTransferSpeed(transferSpeed);
            task.setCwnd(cwnd);
            task.setRtt(rtt);
            return updateById(task);
        }
        return false;
    }
    
    /**
     * 查询任务列表
     *
     * @param queryDTO 查询条件
     * @return 任务列表
     */
    @Override
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
     * 查询用户所有活跃的传输任务
     * 活跃任务：状态为 PENDING 或 PROCESSING
     *
     * @param userId 用户ID
     * @return 活跃任务列表
     */
    @Override
    public List<TransferTask> getActiveTasksByUserId(Long userId) {
        // 通过文件ID关联查询用户的活跃任务
        // 1. 查询用户的所有文件ID
        QueryWrapper<FileInfo> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("user_id", userId);
        List<FileInfo> userFiles = fileInfoMapper.selectList(fileWrapper);
        
        if (userFiles.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 2. 提取文件ID列表
        List<Long> fileIds = userFiles.stream()
                .map(FileInfo::getId)
                .collect(Collectors.toList());
        
        // 3. 查询这些文件的活跃任务
        QueryWrapper<TransferTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.in("file_id", fileIds)
                   .in("transfer_status", "PENDING", "PROCESSING")
                   .orderByDesc("start_time");
        
        return transferTaskMapper.selectList(taskWrapper);
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
     * 同时级联删除相关的拥塞指标数据
     *
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteTask(String taskId) {
        TransferTask task = transferTaskMapper.selectByTaskId(taskId);
        if (task != null) {
            // 级联删除拥塞指标数据
            int deletedMetricsCount = congestionMetricsMapper.deleteByTaskId(taskId);
            log.info("删除任务时级联删除拥塞指标 - 任务ID: {}, 删除指标数: {}", taskId, deletedMetricsCount);
            
            // 删除任务
            removeById(task.getId());
            log.info("删除任务 - 任务ID: {}", taskId);
            return true;
        }
        return false;
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

