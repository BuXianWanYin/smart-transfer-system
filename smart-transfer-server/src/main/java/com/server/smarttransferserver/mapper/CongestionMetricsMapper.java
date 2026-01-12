package com.server.smarttransferserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.server.smarttransferserver.entity.CongestionMetrics;
import org.apache.ibatis.annotations.Delete;
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
     * @param taskId 任务ID（UUID字符串）
     * @return 指标列表
     */
    @Select("SELECT * FROM congestion_metrics WHERE task_id = #{taskId} ORDER BY record_time DESC")
    List<CongestionMetrics> selectByTaskIdOrderByRecordTimeDesc(@Param("taskId") String taskId);

    /**
     * 根据算法查询指标列表
     *
     * @param algorithm 算法名称
     * @return 指标列表
     */
    @Select("SELECT * FROM congestion_metrics WHERE algorithm = #{algorithm} ORDER BY record_time DESC")
    List<CongestionMetrics> selectByAlgorithm(@Param("algorithm") String algorithm);

    /**
     * 统计任务的指标记录数量
     *
     * @param taskId 任务ID（UUID字符串）
     * @return 记录数量
     */
    @Select("SELECT COUNT(*) FROM congestion_metrics WHERE task_id = #{taskId}")
    Long countByTaskId(@Param("taskId") String taskId);

    /**
     * 获取任务的平均RTT
     *
     * @param taskId 任务ID（UUID字符串）
     * @return 平均RTT
     */
    @Select("SELECT AVG(rtt) FROM congestion_metrics WHERE task_id = #{taskId}")
    Double getAvgRttByTaskId(@Param("taskId") String taskId);

    /**
     * 获取任务的平均带宽
     *
     * @param taskId 任务ID（UUID字符串）
     * @return 平均带宽
     */
    @Select("SELECT AVG(bandwidth) FROM congestion_metrics WHERE task_id = #{taskId}")
    Double getAvgBandwidthByTaskId(@Param("taskId") String taskId);

    /**
     * 获取任务的平均丢包率
     *
     * @param taskId 任务ID（UUID字符串）
     * @return 平均丢包率
     */
    @Select("SELECT AVG(loss_rate) FROM congestion_metrics WHERE task_id = #{taskId}")
    Double getAvgLossRateByTaskId(@Param("taskId") String taskId);
    
    /**
     * 删除指定天数前的历史指标数据
     *
     * @param days 保留天数（删除 days 天前的数据）
     * @return 删除的记录数
     */
    @Delete("DELETE FROM congestion_metrics WHERE record_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteOldMetrics(@Param("days") int days);
    
    /**
     * 根据任务ID删除拥塞指标数据（级联删除）
     *
     * @param taskId 任务ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM congestion_metrics WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") String taskId);
}
