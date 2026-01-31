package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 独立 RTT 探测上报 DTO（前端 GET /file/rtt-probe 测得后 POST 此值）
 */
@Data
public class ProbeRttDTO {

    /**
     * 往返时延（毫秒），与 Clumsy 配置一致（如 20ms 单向约 40–50ms）
     */
    @NotNull(message = "rttMs 不能为空")
    @Min(value = 0, message = "rttMs 不能为负")
    @Max(value = 60000, message = "rttMs 不能超过 60000")
    private Long rttMs;
}
