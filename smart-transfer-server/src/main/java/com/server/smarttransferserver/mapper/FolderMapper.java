package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.domain.Folder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件夹Mapper
 */
@Mapper
public interface FolderMapper extends BaseMapper<Folder> {
}

