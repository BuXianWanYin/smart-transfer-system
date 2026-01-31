package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.ProbeRttStore;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按用户存储独立 RTT 探测值（内存，不持久化）
 */
@Component
public class ProbeRttStoreImpl implements ProbeRttStore {

    private final ConcurrentHashMap<Long, Long> userProbeRttMs = new ConcurrentHashMap<>();

    @Override
    public void set(Long userId, long rttMs) {
        if (userId == null) return;
        userProbeRttMs.put(userId, rttMs);
    }

    @Override
    public Long get(Long userId) {
        if (userId == null) return null;
        return userProbeRttMs.get(userId);
    }
}
