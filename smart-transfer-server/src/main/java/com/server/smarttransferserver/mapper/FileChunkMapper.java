package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.FileChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件分片Mapper接口
 */
@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunk> {

    /**
     * 根据文件ID和分片序号查询分片
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @return 分片信息
     */
    @Select("SELECT * FROM t_file_chunk WHERE file_id = #{fileId} AND chunk_number = #{chunkNumber} LIMIT 1")
    FileChunk selectByFileIdAndChunkNumber(@Param("fileId") Long fileId, @Param("chunkNumber") Integer chunkNumber);

    /**
     * 根据文件ID和上传状态查询分片列表
     *
     * @param fileId       文件ID
     * @param uploadStatus 上传状态
     * @return 分片列表
     */
    @Select("SELECT * FROM t_file_chunk WHERE file_id = #{fileId} AND upload_status = #{uploadStatus}")
    List<FileChunk> selectByFileIdAndUploadStatus(@Param("fileId") Long fileId, @Param("uploadStatus") String uploadStatus);

    /**
     * 统计文件的分片数量
     *
     * @param fileId 文件ID
     * @return 分片数量
     */
    @Select("SELECT COUNT(*) FROM t_file_chunk WHERE file_id = #{fileId}")
    Long countByFileId(@Param("fileId") Long fileId);

    /**
     * 统计文件已完成的分片数量
     *
     * @param fileId       文件ID
     * @param uploadStatus 上传状态
     * @return 完成数量
     */
    @Select("SELECT COUNT(*) FROM t_file_chunk WHERE file_id = #{fileId} AND upload_status = #{uploadStatus}")
    Long countByFileIdAndStatus(@Param("fileId") Long fileId, @Param("uploadStatus") String uploadStatus);
}
