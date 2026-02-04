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
 * 系统活动记录实体类
 * 对应数据库 system_activity 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName(value = "system_activity")
public class SystemActivity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 活动ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 活动类型：USER_REGISTER-用户注册, FILE_UPLOAD-文件上传, FILE_DOWNLOAD-文件下载, SYSTEM_CONFIG-系统配置
     */
    @TableField(value = "activity_type")
    private String activityType;
    
    /**
     * 活动描述
     */
    @TableField(value = "activity_desc")
    private String activityDesc;
    
    /**
     * 关联用户ID
     */
    @TableField(value = "related_user_id")
    private Long relatedUserId;
    
    /**
     * 关联用户名
     */
    @TableField(value = "related_user_name")
    private String relatedUserName;
    
    /**
     * 活动数据（JSON格式）
     */
    @TableField(value = "activity_data")
    private String activityData;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
