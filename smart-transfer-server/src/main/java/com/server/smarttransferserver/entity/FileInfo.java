package com.server.smarttransferserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息实体类
 * 对应数据库表 t_file_info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_file_info")
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件扩展名
     */
    @TableField("extend_name")
    private String extendName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件哈希值（MD5/SHA256）
     */
    @TableField("file_hash")
    private String fileHash;

    /**
     * 文件存储路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 是否目录（0文件 1目录）
     */
    @TableField("is_dir")
    private Integer isDir;

    /**
     * 所属文件夹ID，0表示根目录
     */
    @TableField("folder_id")
    private Long folderId;

    /**
     * 上传状态：PENDING-待上传 UPLOADING-上传中 COMPLETED-已完成
     */
    @TableField("upload_status")
    private String uploadStatus;

    /**
     * 删除标志（0正常 1已删除）
     */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;

    /**
     * 删除批次号
     */
    @TableField("delete_batch_num")
    private String deleteBatchNum;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
