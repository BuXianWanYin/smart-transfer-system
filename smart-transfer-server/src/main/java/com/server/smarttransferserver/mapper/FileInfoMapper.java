package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件信息Mapper接口
 * 
 * 说明：
 * 1. 逻辑删除：使用MyBatis Plus的@TableLogic注解，调用deleteById/deleteBatchIds自动触发
 * 2. 还原/更新del_flag：使用LambdaUpdateWrapper在Service层实现
 * 3. 物理删除：必须使用原生SQL，因为@TableLogic会拦截delete操作
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 根据文件哈希查询文件（MyBatis Plus会自动加上del_flag=0条件）
     *
     * @param fileHash 文件哈希
     * @return 文件信息
     */
    @Select("SELECT * FROM file_info WHERE file_hash = #{fileHash} AND del_flag = 0 LIMIT 1")
    FileInfo selectByFileHash(@Param("fileHash") String fileHash);

    /**
     * 根据上传状态查询文件列表
     *
     * @param uploadStatus 上传状态
     * @return 文件列表
     */
    @Select("SELECT * FROM file_info WHERE upload_status = #{uploadStatus} AND del_flag = 0")
    List<FileInfo> selectByUploadStatus(@Param("uploadStatus") String uploadStatus);

    /**
     * 统计指定状态的文件数量
     *
     * @param uploadStatus 上传状态
     * @return 文件数量
     */
    @Select("SELECT COUNT(*) FROM file_info WHERE upload_status = #{uploadStatus} AND del_flag = 0")
    Long countByUploadStatus(@Param("uploadStatus") String uploadStatus);
    
    /**
     * 物理删除文件（真正删除数据库记录，绕过@TableLogic）
     * 用于回收站彻底删除功能
     *
     * @param fileId 文件ID
     * @return 影响行数
     */
    @Delete("DELETE FROM file_info WHERE id = #{fileId}")
    int deletePhysically(@Param("fileId") Long fileId);

    /**
     * 根据批次号物理删除文件（真正删除数据库记录，绕过@TableLogic）
     * 用于批量彻底删除功能
     *
     * @param batchNum 删除批次号
     * @return 影响行数
     */
    @Delete("DELETE FROM file_info WHERE delete_batch_num = #{batchNum}")
    int deletePhysicallyByBatchNum(@Param("batchNum") String batchNum);

    /**
     * 查询同名文件（同一用户、同一文件夹下）
     * 用于检查文件重名，支持自动重命名
     *
     * @param fileName 文件名
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @return 同名文件列表
     */
    @Select("SELECT * FROM file_info WHERE file_name = #{fileName} AND folder_id = #{folderId} AND user_id = #{userId} AND del_flag = 0 AND upload_status = 'COMPLETED'")
    List<FileInfo> selectByFileNameAndFolder(@Param("fileName") String fileName, 
                                              @Param("folderId") Long folderId, 
                                              @Param("userId") Long userId);

    /**
     * 根据批次号还原文件（绕过@TableLogic的del_flag自动条件）
     * 用于回收站还原功能
     *
     * @param batchNum 删除批次号
     * @param now 更新时间
     * @return 影响行数
     */
    @Update("UPDATE file_info SET del_flag = 0, delete_batch_num = NULL, update_time = #{now} WHERE delete_batch_num = #{batchNum}")
    int restoreByBatchNum(@Param("batchNum") String batchNum, @Param("now") LocalDateTime now);

    /**
     * 根据文件ID还原单个文件（绕过@TableLogic的del_flag自动条件）
     * 用于回收站还原功能
     *
     * @param fileId 文件ID
     * @param now 更新时间
     * @return 影响行数
     */
    @Update("UPDATE file_info SET del_flag = 0, delete_batch_num = NULL, update_time = #{now} WHERE id = #{fileId}")
    int restoreByFileId(@Param("fileId") Long fileId, @Param("now") LocalDateTime now);
}
