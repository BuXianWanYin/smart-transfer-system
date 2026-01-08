package com.server.smarttransferserver.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 传输历史记录实体类
 * 对应数据库 t_transfer_history 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName(value = "t_transfer_history")
public class TransferHistory implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;
    
    /**
     * 传输任务ID
     */
    @TableField(value = "task_id")
    private String taskId;
    
    /**
     * 文件ID
     */
    @TableField(value = "file_id")
    private Long fileId;
    
    /**
     * 文件名
     */
    @TableField(value = "file_name")
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    @TableField(value = "file_size")
    private Long fileSize;
    
    /**
     * 文件哈希值
     */
    @TableField(value = "file_hash")
    private String fileHash;
    
    /**
     * 传输类型：UPLOAD-上传, DOWNLOAD-下载
     */
    @TableField(value = "transfer_type")
    private String transferType;
    
    /**
     * 传输状态：COMPLETED-成功, FAILED-失败
     */
    @TableField(value = "transfer_status")
    private String transferStatus;
    
    /**
     * 平均传输速度（字节/秒）
     */
    @TableField(value = "avg_speed")
    private Long avgSpeed;
    
    /**
     * 传输时长（秒）
     */
    @TableField(value = "duration")
    private Integer duration;
    
    /**
     * 使用的拥塞控制算法：CUBIC, BBR, ADAPTIVE
     */
    @TableField(value = "algorithm")
    private String algorithm;
    
    /**
     * 完成时间
     */
    @TableField(value = "completed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;
    
    /**
     * 错误信息（失败时记录）
     */
    @TableField(value = "error_message")
    private String errorMessage;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

