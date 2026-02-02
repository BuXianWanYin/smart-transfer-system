package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.domain.SystemActivity;

import org.apache.ibatis.annotations.Mapper;

/**
 * 系统活动记录数据访问层
 */
@Mapper
public interface SystemActivityMapper extends BaseMapper<SystemActivity> {
}
