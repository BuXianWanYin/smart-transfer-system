package com.server.smarttransferserver.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务接口
 * 提供常用的 Redis 操作
 */
public interface RedisService {
    
    // ========== String 操作 ==========
    
    /**
     * 设置缓存
     */
    void set(String key, Object value);
    
    /**
     * 设置缓存并指定过期时间
     */
    void set(String key, Object value, long timeout, TimeUnit unit);
    
    /**
     * 获取缓存
     */
    Object get(String key);
    
    /**
     * 删除缓存
     */
    Boolean delete(String key);
    
    /**
     * 批量删除缓存
     */
    Long delete(List<String> keys);
    
    /**
     * 设置过期时间
     */
    Boolean expire(String key, long timeout, TimeUnit unit);
    
    /**
     * 判断 key 是否存在
     */
    Boolean hasKey(String key);
    
    /**
     * 递增
     */
    Long increment(String key, long delta);
    
    /**
     * 递减
     */
    Long decrement(String key, long delta);
    
    // ========== Hash 操作 ==========
    
    /**
     * Hash 设置
     */
    void hSet(String key, String hashKey, Object value);
    
    /**
     * Hash 获取
     */
    Object hGet(String key, String hashKey);
    
    /**
     * Hash 删除
     */
    Long hDelete(String key, Object... hashKeys);
    
    /**
     * Hash 是否存在
     */
    Boolean hHasKey(String key, String hashKey);
    
    /**
     * Hash 递增
     */
    Long hIncrement(String key, String hashKey, long delta);
    
    // ========== Set 操作 ==========
    
    /**
     * Set 添加
     */
    Long sAdd(String key, Object... values);
    
    /**
     * Set 获取所有成员
     */
    Set<Object> sMembers(String key);
    
    /**
     * Set 是否包含
     */
    Boolean sIsMember(String key, Object value);
    
    /**
     * Set 删除
     */
    Long sRemove(String key, Object... values);
    
    /**
     * Set 大小
     */
    Long sSize(String key);
    
    // ========== 分布式锁 ==========
    
    /**
     * 获取分布式锁
     *
     * @param key     锁的键
     * @param value   锁的值（通常是 UUID）
     * @param timeout 锁的超时时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    Boolean tryLock(String key, String value, long timeout, TimeUnit unit);
    
    /**
     * 释放分布式锁
     *
     * @param key   锁的键
     * @param value 锁的值
     * @return 是否释放成功
     */
    Boolean releaseLock(String key, String value);
}

