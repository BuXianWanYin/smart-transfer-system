package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.domain.Folder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 文件夹Mapper
 */
@Mapper
public interface FolderMapper extends BaseMapper<Folder> {

    /**
     * 根据批次号还原文件夹（绕过@TableLogic的del_flag自动条件）
     * 用于回收站还原功能
     *
     * @param batchNum 删除批次号
     * @param now 更新时间
     * @return 影响行数
     */
    @Update("UPDATE folder SET del_flag = 0, delete_batch_num = NULL, delete_time = NULL, update_time = #{now} WHERE delete_batch_num = #{batchNum}")
    int restoreByBatchNum(@Param("batchNum") String batchNum, @Param("now") LocalDateTime now);
}

