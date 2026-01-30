package com.server.smarttransferserver.util;

/**
 * 客户端上报的 RTT/丢包（重试次数）与前后端约定一致的范围常量。
 * 前端 fileApi.js 使用相同数值限幅，避免前后端不一致。
 */
public final class CongestionClientMetricsConstants {

    /** 客户端 RTT（ms）有效上限，与前端 RTT_MS_MAX 一致 */
    public static final long RTT_MS_MAX = 60_000L;
    /** 客户端 RTT（ms）有效下限（算法使用时的最小值） */
    public static final long RTT_MS_MIN = 1L;
    /** 客户端重试次数计入 onLoss 的上限，与前端 RETRY_COUNT_MAX 一致 */
    public static final int RETRY_COUNT_CAP = 10;

    private CongestionClientMetricsConstants() {}
}
