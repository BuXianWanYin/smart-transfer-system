package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
