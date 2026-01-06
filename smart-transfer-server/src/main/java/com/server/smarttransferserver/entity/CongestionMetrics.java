package com.server.smarttransferserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拥塞指标实体类
 * 对应数据库表 t_congestion_metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_congestion_metrics")
public class CongestionMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 拥塞控制算法
     */
    @TableField("algorithm")
    private String algorithm;

    /**
     * 拥塞窗口大小
     */
    @TableField("cwnd")
    private Long cwnd;

    /**
     * 慢启动阈值
     */
    @TableField("ssthresh")
    private Long ssthresh;

    /**
     * RTT（往返时延）
     */
    @TableField("rtt")
    private Long rtt;

    /**
     * 带宽（字节/秒）
     */
    @TableField("bandwidth")
    private Long bandwidth;

    /**
     * 丢包率
     */
    @TableField("loss_rate")
    private BigDecimal lossRate;

    /**
     * 记录时间
     */
    @TableField(value = "record_time", fill = FieldFill.INSERT)
    private LocalDateTime recordTime;
}
