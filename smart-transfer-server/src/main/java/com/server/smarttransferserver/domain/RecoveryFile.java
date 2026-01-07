package com.server.smarttransferserver.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回收站文件实体类
 * 对应数据库 t_recovery_file 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_recovery_file")
public class RecoveryFile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回收站记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
     * 文件扩展名
     */
    @TableField(value = "extend_name")
    private String extendName;

    /**
     * 原文件路径
     */
    @TableField(value = "file_path")
    private String filePath;

    /**
     * 原所属文件夹ID
     */
    @TableField(value = "folder_id")
    private Long folderId;

    /**
     * 是否目录（0文件 1目录）
     */
    @TableField(value = "is_dir")
    private Integer isDir;

    /**
     * 文件大小
     */
    @TableField(value = "file_size")
    private Long fileSize;

    /**
     * 删除时间
     */
    @TableField(value = "delete_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deleteTime;

    /**
     * 删除批次号
     */
    @TableField(value = "delete_batch_num")
    private String deleteBatchNum;
}

