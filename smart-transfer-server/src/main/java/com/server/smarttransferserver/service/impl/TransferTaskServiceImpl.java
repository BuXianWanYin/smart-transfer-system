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
import com.server.smarttransferserver.service.ActiveUserService;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.util.UserContextHolder;
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
    
    @Autowired
    private ActiveUserService activeUserService;
    
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
        
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        Long userId = fileInfo != null ? fileInfo.getUserId() : null;

        TransferTask task = TransferTask.builder()
                .taskId(taskId)
                .fileId(fileId)
                .userId(userId)
                .taskType(taskType)
                .transferStatus("PENDING")
                .progress(BigDecimal.ZERO)
                .startTime(LocalDateTime.now())
                .build();

        save(task);
        log.info("创建传输任务 - 任务ID: {}, 文件ID: {}, 类型: {}, userId: {}", taskId, fileId, taskType, userId);

        if (userId != null) {
            activeUserService.addActiveUser(userId);
        }
        
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
            String oldStatus = task.getTransferStatus();
            task.setTransferStatus(status);
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                task.setEndTime(LocalDateTime.now());
            }
            boolean updated = updateById(task);
            
            // 如果任务状态从活跃变为非活跃（COMPLETED/FAILED/PAUSED），检查并更新活跃用户集合
            if (updated && ("PENDING".equals(oldStatus) || "PROCESSING".equals(oldStatus))) {
                if ("COMPLETED".equals(status) || "FAILED".equals(status) || "PAUSED".equals(status)) {
                    FileInfo fileInfo = fileInfoMapper.selectById(task.getFileId());
                    if (fileInfo != null && fileInfo.getUserId() != null) {
                        // 检查该用户是否还有其他活跃任务，如果没有则移除
                        Long activeTaskCount = countActiveTasksByUserId(fileInfo.getUserId());
                        if (activeTaskCount == null || activeTaskCount == 0) {
                            activeUserService.removeActiveUser(fileInfo.getUserId());
                        }
                    }
                }
            }
            // 如果任务状态变为活跃（PENDING/PROCESSING），添加到活跃用户集合
            else if (updated && ("PENDING".equals(status) || "PROCESSING".equals(status))) {
                FileInfo fileInfo = fileInfoMapper.selectById(task.getFileId());
                if (fileInfo != null && fileInfo.getUserId() != null) {
                    activeUserService.addActiveUser(fileInfo.getUserId());
                }
            }
            
            return updated;
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
     * 统计用户的活跃传输任务数量
     * 用于快速判断是否有活跃任务
     *
     * @param userId 用户ID
     * @return 活跃任务数量
     */
    @Override
    public Long countActiveTasksByUserId(Long userId) {
        // 使用 JOIN 查询统计活跃任务数量，性能比查询列表更高效
        Long count = transferTaskMapper.countActiveTasksByUserId(userId);
        return count != null ? count : 0L;
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
        // 使用 JOIN 查询直接关联查询，避免先查文件再查任务的两步查询
        // 这样可以减少数据库查询次数，提高性能
        return transferTaskMapper.selectActiveTasksByUserId(userId);
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
            String oldStatus = task.getTransferStatus();
            task.setTransferStatus(status);
            if (progress != null) {
                task.setProgress(new java.math.BigDecimal(progress.toString()));
            }
            if (speed != null) {
                task.setTransferSpeed(speed);
            }
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                task.setEndTime(LocalDateTime.now());
            }
            transferTaskMapper.updateById(task);
            
            // 如果任务状态从活跃变为非活跃（COMPLETED/FAILED/PAUSED），检查并更新活跃用户集合
            if ("PENDING".equals(oldStatus) || "PROCESSING".equals(oldStatus)) {
                if ("COMPLETED".equals(status) || "FAILED".equals(status) || "PAUSED".equals(status)) {
                    FileInfo fileInfo = fileInfoMapper.selectById(task.getFileId());
                    if (fileInfo != null && fileInfo.getUserId() != null) {
                        // 检查该用户是否还有其他活跃任务，如果没有则移除
                        Long activeTaskCount = countActiveTasksByUserId(fileInfo.getUserId());
                        if (activeTaskCount == null || activeTaskCount == 0) {
                            activeUserService.removeActiveUser(fileInfo.getUserId());
                        }
                    }
                }
            }
            // 如果任务状态变为活跃（PENDING/PROCESSING），添加到活跃用户集合
            else if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
                FileInfo fileInfo = fileInfoMapper.selectById(task.getFileId());
                if (fileInfo != null && fileInfo.getUserId() != null) {
                    activeUserService.addActiveUser(fileInfo.getUserId());
                }
            }
            
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
            Long fileId = task.getFileId();
            String taskStatus = task.getTransferStatus();
            
            // 级联删除拥塞指标数据
            int deletedMetricsCount = congestionMetricsMapper.deleteByTaskId(taskId);
            log.info("删除任务时级联删除拥塞指标 - 任务ID: {}, 删除指标数: {}", taskId, deletedMetricsCount);
            
            // 删除任务
            removeById(task.getId());
            log.info("删除任务 - 任务ID: {}", taskId);
            
            // 如果删除的是活跃任务，检查并更新活跃用户集合
            if ("PENDING".equals(taskStatus) || "PROCESSING".equals(taskStatus)) {
                FileInfo fileInfo = fileInfoMapper.selectById(fileId);
                if (fileInfo != null && fileInfo.getUserId() != null) {
                    // 检查该用户是否还有其他活跃任务，如果没有则移除
                    Long activeTaskCount = countActiveTasksByUserId(fileInfo.getUserId());
                    if (activeTaskCount == null || activeTaskCount == 0) {
                        activeUserService.removeActiveUser(fileInfo.getUserId());
                    }
                }
            }
            
            return true;
        }
        return false;
    }

    @Override
    public List<TransferTaskVO> listIncompleteTasksForCurrentUser(String taskType) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            log.warn("未登录，无法查询未完成任务");
            return Collections.emptyList();
        }
        List<TransferTask> tasks;
        if ("UPLOAD".equalsIgnoreCase(taskType)) {
            tasks = transferTaskMapper.selectIncompleteUploadTasksByUserId(userId);
        } else if ("DOWNLOAD".equalsIgnoreCase(taskType)) {
            tasks = transferTaskMapper.selectIncompleteDownloadTasksByUserId(userId);
        } else {
            log.warn("无效的 taskType: {}", taskType);
            return Collections.emptyList();
        }
        return tasks.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int pauseAllCurrentUserTasks() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return 0;
        }
        List<TransferTask> uploadTasks = transferTaskMapper.selectIncompleteUploadTasksByUserId(userId);
        List<TransferTask> downloadTasks = transferTaskMapper.selectIncompleteDownloadTasksByUserId(userId);
        int count = 0;
        for (TransferTask t : uploadTasks) {
            if ("PENDING".equals(t.getTransferStatus()) || "PROCESSING".equals(t.getTransferStatus())) {
                if (updateTaskStatus(t.getTaskId(), "PAUSED")) {
                    count++;
                }
            }
        }
        for (TransferTask t : downloadTasks) {
            if ("PENDING".equals(t.getTransferStatus()) || "PROCESSING".equals(t.getTransferStatus())) {
                if (updateTaskStatus(t.getTaskId(), "PAUSED")) {
                    count++;
                }
            }
        }
        log.info("退出登录时暂停当前用户传输任务 - userId: {}, 暂停数: {}", userId, count);
        return count;
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

