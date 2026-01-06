package com.server.smarttransferserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.dto.TransferTaskQueryDTO;
import com.server.smarttransferserver.entity.TransferTask;
import com.server.smarttransferserver.vo.TransferTaskVO;

/**
 * 传输任务服务接口
 * 提供传输任务的创建、查询、更新等功能
 */
public interface TransferTaskService extends IService<TransferTask> {
    
    /**
     * 创建传输任务
     *
     * @param fileId 文件ID
     * @param taskType 任务类型（UPLOAD/DOWNLOAD）
     * @return 任务ID
     */
    String createTask(Long fileId, String taskType);
    
    /**
     * 根据任务ID查询任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    TransferTaskVO getTaskByTaskId(String taskId);
    
    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 传输状态
     * @return 是否更新成功
     */
    boolean updateTaskStatus(String taskId, String status);
    
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
    boolean updateTaskProgress(String taskId, Integer progress, Long transferSpeed, Long cwnd, Long rtt);
    
    /**
     * 分页查询传输任务
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<TransferTaskVO> queryTasks(TransferTaskQueryDTO queryDTO);
    
    /**
     * 删除传输任务
     *
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    boolean deleteTask(String taskId);
}

