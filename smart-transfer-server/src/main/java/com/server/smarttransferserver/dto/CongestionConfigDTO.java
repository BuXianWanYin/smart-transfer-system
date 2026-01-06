package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 拥塞控制配置DTO
 */
@Data
public class CongestionConfigDTO {
    
    /**
     * 算法名称：CUBIC, BBR, ADAPTIVE
     */
    @NotBlank(message = "算法名称不能为空")
    private String algorithm;
    
    /**
     * 初始拥塞窗口（字节）
     */
    @Min(value = 1024, message = "初始拥塞窗口不能小于1KB")
    private Long initialCwnd;
    
    /**
     * 慢启动阈值（字节）
     */
    @Min(value = 1024, message = "慢启动阈值不能小于1KB")
    private Long ssthresh;
    
    /**
     * 最大拥塞窗口（字节）
     */
    @Min(value = 1024, message = "最大拥塞窗口不能小于1KB")
    private Long maxCwnd;
    
    /**
     * 最小拥塞窗口（字节）
     */
    @Min(value = 1024, message = "最小拥塞窗口不能小于1KB")
    private Long minCwnd;
    
    /**
     * 最大速率（字节/秒，0表示不限制）
     */
    @Min(value = 0, message = "最大速率不能小于0")
    private Long maxRate;
    
    /**
     * 最小速率（字节/秒）
     */
    @Min(value = 1024, message = "最小速率不能小于1KB/s")
    private Long minRate;
}

