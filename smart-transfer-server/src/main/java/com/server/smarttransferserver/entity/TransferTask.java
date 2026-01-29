package com.server.smarttransferserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 传输任务实体类
 * 对应数据库表 transfer_task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("transfer_task")
public class TransferTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID（UUID）
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 文件ID
     */
    @TableField("file_id")
    private Long fileId;

    /**
     * 用户ID（任务所属用户：上传为文件所属用户，下载为发起下载的用户）
     * 用于刷新/重登后恢复「我的」未完成任务列表
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 任务类型：UPLOAD-上传 DOWNLOAD-下载
     */
    @TableField("task_type")
    private String taskType;

    /**
     * 传输状态：PENDING-待处理 PROCESSING-处理中 COMPLETED-已完成 FAILED-失败 PAUSED-已暂停
     */
    @TableField("transfer_status")
    private String transferStatus;

    /**
     * 传输进度（0-100）
     */
    @TableField("progress")
    private BigDecimal progress;

    /**
     * 传输速率（字节/秒）
     */
    @TableField("transfer_speed")
    private Long transferSpeed;

    /**
     * 拥塞窗口大小（字节）
     */
    @TableField(exist = false)
    private Long cwnd;

    /**
     * 往返时延RTT（毫秒）
     */
    @TableField(exist = false)
    private Long rtt;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;
}
