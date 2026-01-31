package com.server.smarttransferserver.service;

/**
 * 独立 RTT 探测值存储（按用户）
 * 前端通过 GET /file/rtt-probe 测得往返时延后，POST 到此存储，供算法与 WebSocket 推送使用。
 */
public interface ProbeRttStore {

    /**
     * 设置用户最近一次探测 RTT（往返，毫秒）
     *
     * @param userId 用户ID
     * @param rttMs  往返时延（ms），与 Clumsy 配置一致
     */
    void set(Long userId, long rttMs);

    /**
     * 获取用户最近一次探测 RTT（往返，毫秒）
     *
     * @param userId 用户ID
     * @return 往返时延（ms），未上报过返回 null
     */
    Long get(Long userId);
}
