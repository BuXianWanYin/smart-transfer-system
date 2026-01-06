package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.CongestionMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 拥塞指标Mapper接口
 */
@Mapper
public interface CongestionMetricsMapper extends BaseMapper<CongestionMetrics> {

    /**
     * 根据任务ID查询指标列表（按时间倒序）
     *
     * @param taskId 任务ID
     * @return 指标列表
     */
    @Select("SELECT * FROM t_congestion_metrics WHERE task_id = #{taskId} ORDER BY record_time DESC")
    List<CongestionMetrics> selectByTaskIdOrderByRecordTimeDesc(@Param("taskId") Long taskId);

    /**
     * 根据算法查询指标列表
     *
     * @param algorithm 算法名称
     * @return 指标列表
     */
    @Select("SELECT * FROM t_congestion_metrics WHERE algorithm = #{algorithm} ORDER BY record_time DESC")
    List<CongestionMetrics> selectByAlgorithm(@Param("algorithm") String algorithm);

    /**
     * 统计任务的指标记录数量
     *
     * @param taskId 任务ID
     * @return 记录数量
     */
    @Select("SELECT COUNT(*) FROM t_congestion_metrics WHERE task_id = #{taskId}")
    Long countByTaskId(@Param("taskId") Long taskId);

    /**
     * 获取任务的平均RTT
     *
     * @param taskId 任务ID
     * @return 平均RTT
     */
    @Select("SELECT AVG(rtt) FROM t_congestion_metrics WHERE task_id = #{taskId}")
    Double getAvgRttByTaskId(@Param("taskId") Long taskId);

    /**
     * 获取任务的平均带宽
     *
     * @param taskId 任务ID
     * @return 平均带宽
     */
    @Select("SELECT AVG(bandwidth) FROM t_congestion_metrics WHERE task_id = #{taskId}")
    Double getAvgBandwidthByTaskId(@Param("taskId") Long taskId);

    /**
     * 获取任务的平均丢包率
     *
     * @param taskId 任务ID
     * @return 平均丢包率
     */
    @Select("SELECT AVG(loss_rate) FROM t_congestion_metrics WHERE task_id = #{taskId}")
    Double getAvgLossRateByTaskId(@Param("taskId") Long taskId);
}
