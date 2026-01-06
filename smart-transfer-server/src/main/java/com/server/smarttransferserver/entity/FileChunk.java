package com.server.smarttransferserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分片实体类
 * 对应数据库表 t_file_chunk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_file_chunk")
public class FileChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    @TableField("file_id")
    private Long fileId;

    /**
     * 分片序号（从0开始）
     */
    @TableField("chunk_number")
    private Integer chunkNumber;

    /**
     * 分片大小（字节）
     */
    @TableField("chunk_size")
    private Long chunkSize;

    /**
     * 分片哈希值
     */
    @TableField("chunk_hash")
    private String chunkHash;

    /**
     * 上传状态：PENDING-待上传 UPLOADING-上传中 COMPLETED-已完成
     */
    @TableField("upload_status")
    private String uploadStatus;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
