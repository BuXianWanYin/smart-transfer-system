package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.domain.TransferHistory;

import org.apache.ibatis.annotations.Mapper;

/**
 * 传输历史记录数据访问层
 */
@Mapper
public interface TransferHistoryMapper extends BaseMapper<TransferHistory> {
}

