package com.server.smarttransferserver.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件夹实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_folder")
public class Folder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "folder_name")
    private String folderName;

    @TableField(value = "parent_id")
    private Long parentId;

    @TableField(value = "path")
    private String path;

    @TableField(value = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 删除标记 0-正常 1-已删除
     * 使用MyBatis Plus的逻辑删除注解
     */
    @TableField(value = "del_flag")
    @TableLogic(value = "0", delval = "1")
    @Builder.Default
    private Integer delFlag = 0;

    /**
     * 删除批次号
     */
    @TableField(value = "delete_batch_num")
    private String deleteBatchNum;

    /**
     * 删除时间
     */
    @TableField(value = "delete_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deleteTime;
}

