package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.domain.RecoveryFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 回收站文件数据访问层
 */
@Mapper
public interface RecoveryFileMapper extends BaseMapper<RecoveryFile> {
}

