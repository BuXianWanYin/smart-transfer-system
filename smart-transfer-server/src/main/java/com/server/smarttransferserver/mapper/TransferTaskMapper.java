package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.TransferTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 传输任务Mapper接口
 */
@Mapper
public interface TransferTaskMapper extends BaseMapper<TransferTask> {

    /**
     * 根据任务ID查询任务
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    @Select("SELECT * FROM transfer_task WHERE task_id = #{taskId} LIMIT 1")
    TransferTask selectByTaskId(@Param("taskId") String taskId);

    /**
     * 根据文件ID查询任务列表
     *
     * @param fileId 文件ID
     * @return 任务列表
     */
    @Select("SELECT * FROM transfer_task WHERE file_id = #{fileId} ORDER BY start_time DESC")
    List<TransferTask> selectByFileId(@Param("fileId") Long fileId);

    /**
     * 根据传输状态查询任务列表
     *
     * @param transferStatus 传输状态
     * @return 任务列表
     */
    @Select("SELECT * FROM transfer_task WHERE transfer_status = #{transferStatus} ORDER BY start_time DESC")
    List<TransferTask> selectByStatus(@Param("transferStatus") String transferStatus);

    /**
     * 统计指定状态的任务数量
     *
     * @param transferStatus 传输状态
     * @return 任务数量
     */
    @Select("SELECT COUNT(*) FROM transfer_task WHERE transfer_status = #{transferStatus}")
    Long countByStatus(@Param("transferStatus") String transferStatus);
    
    /**
     * 根据文件ID删除传输任务
     *
     * @param fileId 文件ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM transfer_task WHERE file_id = #{fileId}")
    int deleteByFileId(@Param("fileId") Long fileId);
    
    /**
     * 根据批次号删除传输任务（通过文件ID关联）
     *
     * @param batchNum 删除批次号
     * @return 删除的记录数
     */
    @Delete("DELETE FROM transfer_task WHERE file_id IN (SELECT id FROM file_info WHERE delete_batch_num = #{batchNum})")
    int deleteByBatchNum(@Param("batchNum") String batchNum);
    
    /**
     * 统计用户的活跃传输任务数量（通过文件表关联）
     * 用于快速判断是否有活跃任务，避免不必要的查询
     *
     * @param userId 用户ID
     * @return 活跃任务数量
     */
    @Select("SELECT COUNT(*) FROM transfer_task t " +
            "INNER JOIN file_info f ON t.file_id = f.id " +
            "WHERE f.user_id = #{userId} " +
            "AND f.del_flag = 0 " +
            "AND t.transfer_status IN ('PENDING', 'PROCESSING')")
    Long countActiveTasksByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户的所有活跃传输任务（通过文件表关联）
     * 活跃任务：状态为 PENDING 或 PROCESSING
     *
     * @param userId 用户ID
     * @return 活跃任务列表
     */
    @Select("SELECT t.* FROM transfer_task t " +
            "INNER JOIN file_info f ON t.file_id = f.id " +
            "WHERE f.user_id = #{userId} " +
            "AND f.del_flag = 0 " +
            "AND t.transfer_status IN ('PENDING', 'PROCESSING') " +
            "ORDER BY t.start_time DESC")
    List<TransferTask> selectActiveTasksByUserId(@Param("userId") Long userId);

    /**
     * 查询当前用户未完成的上传任务（用于刷新后恢复列表）
     * 包含 user_id 匹配或通过 file_info 关联的上传任务（兼容历史无 user_id 数据）
     */
    @Select("SELECT t.* FROM transfer_task t " +
            "LEFT JOIN file_info f ON t.file_id = f.id AND f.del_flag = 0 " +
            "WHERE t.task_type = 'UPLOAD' " +
            "AND t.transfer_status IN ('PENDING', 'PROCESSING', 'PAUSED', 'FAILED') " +
            "AND (t.user_id = #{userId} OR (t.user_id IS NULL AND f.user_id = #{userId})) " +
            "ORDER BY t.start_time DESC")
    List<TransferTask> selectIncompleteUploadTasksByUserId(@Param("userId") Long userId);

    /**
     * 查询当前用户未完成的下载任务（用于刷新后恢复列表）
     */
    @Select("SELECT * FROM transfer_task " +
            "WHERE user_id = #{userId} " +
            "AND task_type = 'DOWNLOAD' " +
            "AND transfer_status IN ('PENDING', 'PROCESSING', 'PAUSED', 'FAILED') " +
            "ORDER BY start_time DESC")
    List<TransferTask> selectIncompleteDownloadTasksByUserId(@Param("userId") Long userId);
}
