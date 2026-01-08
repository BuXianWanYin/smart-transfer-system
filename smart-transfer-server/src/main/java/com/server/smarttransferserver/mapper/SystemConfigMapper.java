package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.SystemConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置Mapper接口
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 配置信息
     */
    @Select("SELECT * FROM t_system_config WHERE config_key = #{configKey} LIMIT 1")
    SystemConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 根据配置键删除配置
     *
     * @param configKey 配置键
     * @return 影响行数
     */
    @Delete("DELETE FROM t_system_config WHERE config_key = #{configKey}")
    int deleteByConfigKey(@Param("configKey") String configKey);
}
