package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务实现类
 */
@Slf4j
@Service
public class RedisServiceImpl implements RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // ========== String 操作 ==========
    
    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }
    
    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }
    
    @Override
    public Long delete(List<String> keys) {
        return redisTemplate.delete(keys);
    }
    
    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }
    
    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
    
    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }
    
    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
    
    // ========== Hash 操作 ==========
    
    @Override
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }
    
    @Override
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }
    
    @Override
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }
    
    @Override
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }
    
    @Override
    public Long hIncrement(String key, String hashKey, long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }
    
    // ========== Set 操作 ==========
    
    @Override
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }
    
    @Override
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }
    
    @Override
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }
    
    @Override
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }
    
    @Override
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }
    
    // ========== 分布式锁 ==========
    
    @Override
    public Boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
            log.debug("尝试获取锁 - key: {}, value: {}, result: {}", key, value, result);
            return result != null && result;
        } catch (Exception e) {
            log.error("获取锁失败 - key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public Boolean releaseLock(String key, String value) {
        try {
            // 使用 Lua 脚本保证原子性：只有持有锁的才能释放
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "return redis.call('del', KEYS[1]) " +
                           "else return 0 end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = redisTemplate.execute(redisScript, Collections.singletonList(key), value);
            log.debug("释放锁 - key: {}, value: {}, result: {}", key, value, result);
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("释放锁失败 - key: {}", key, e);
            return false;
        }
    }
}

