package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    @Select("SELECT * FROM t_file_info WHERE file_hash = #{fileHash} LIMIT 1")
    FileInfo selectByFileHash(@Param("fileHash") String fileHash);

    /**
     * 根据上传状态查询文件列表
     *
     * @param uploadStatus 上传状态
     * @return 文件列表
     */
    @Select("SELECT * FROM t_file_info WHERE upload_status = #{uploadStatus}")
    List<FileInfo> selectByUploadStatus(@Param("uploadStatus") String uploadStatus);

    /**
     * 统计指定状态的文件数量
     *
     * @param uploadStatus 上传状态
     * @return 文件数量
     */
    @Select("SELECT COUNT(*) FROM t_file_info WHERE upload_status = #{uploadStatus}")
    Long countByUploadStatus(@Param("uploadStatus") String uploadStatus);
}
