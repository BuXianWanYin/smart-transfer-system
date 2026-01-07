package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

