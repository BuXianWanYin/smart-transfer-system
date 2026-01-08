package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文件信息Mapper接口
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 根据文件哈希查询文件
     *
     * @param fileHash 文件哈希
     * @return 文件信息
     */
    @Select("SELECT * FROM t_file_info WHERE file_hash = #{fileHash} AND del_flag = 0 LIMIT 1")
    FileInfo selectByFileHash(@Param("fileHash") String fileHash);

    /**
     * 根据上传状态查询文件列表
     *
     * @param uploadStatus 上传状态
     * @return 文件列表
     */
    @Select("SELECT * FROM t_file_info WHERE upload_status = #{uploadStatus} AND del_flag = 0")
    List<FileInfo> selectByUploadStatus(@Param("uploadStatus") String uploadStatus);

    /**
     * 统计指定状态的文件数量
     *
     * @param uploadStatus 上传状态
     * @return 文件数量
     */
    @Select("SELECT COUNT(*) FROM t_file_info WHERE upload_status = #{uploadStatus} AND del_flag = 0")
    Long countByUploadStatus(@Param("uploadStatus") String uploadStatus);
    
    /**
     * 更新删除标志（跳过 @TableLogic 的限制）
     *
     * @param fileId 文件ID
     * @param delFlag 删除标志
     * @return 影响行数
     */
    @Update("UPDATE t_file_info SET del_flag = #{delFlag}, update_time = NOW() WHERE id = #{fileId}")
    int updateDelFlag(@Param("fileId") Long fileId, @Param("delFlag") Integer delFlag);
    
    /**
     * 物理删除文件（真正删除数据库记录）
     *
     * @param fileId 文件ID
     * @return 影响行数
     */
    @Delete("DELETE FROM t_file_info WHERE id = #{fileId}")
    int deletePhysically(@Param("fileId") Long fileId);

    /**
     * 更新删除标志并设置批次号（用于文件夹删除）
     *
     * @param fileId 文件ID
     * @param delFlag 删除标志
     * @param batchNum 删除批次号
     * @return 影响行数
     */
    @Update("UPDATE t_file_info SET del_flag = #{delFlag}, delete_batch_num = #{batchNum}, update_time = NOW() WHERE id = #{fileId}")
    int updateDelFlagWithBatch(@Param("fileId") Long fileId, @Param("delFlag") Integer delFlag, @Param("batchNum") String batchNum);

    /**
     * 根据批次号还原文件
     *
     * @param batchNum 删除批次号
     * @return 影响行数
     */
    @Update("UPDATE t_file_info SET del_flag = 0, delete_batch_num = NULL, update_time = NOW() WHERE delete_batch_num = #{batchNum}")
    int restoreByBatchNum(@Param("batchNum") String batchNum);

    /**
     * 根据批次号物理删除文件
     *
     * @param batchNum 删除批次号
     * @return 影响行数
     */
    @Delete("DELETE FROM t_file_info WHERE delete_batch_num = #{batchNum}")
    int deletePhysicallyByBatchNum(@Param("batchNum") String batchNum);
}
