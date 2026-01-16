package com.server.smarttransferserver.service.impl;

import com.server.smarttransferserver.service.ActiveUserService;
import com.server.smarttransferserver.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 活跃用户管理服务实现
 * 负责管理Redis中的活跃用户集合，避免循环依赖
 * 只负责纯粹的Redis操作，不依赖其他业务服务
 */
@Slf4j
@Service
public class ActiveUserServiceImpl implements ActiveUserService {
    
    /**
     * Redis key：存储有活跃传输任务的用户ID集合
     */
    public static final String ACTIVE_USERS_KEY = "active:transfer:users";
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 添加活跃用户到Redis集合
     * 当用户有活跃任务时调用
     *
     * @param userId 用户ID
     */
    @Override
    public void addActiveUser(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisService.sAdd(ACTIVE_USERS_KEY, userId);
            log.debug("添加活跃用户 - 用户ID: {}", userId);
        } catch (Exception e) {
            log.warn("添加活跃用户到Redis失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            // 不抛出异常，不影响主业务
        }
    }
    
    /**
     * 从Redis集合中移除活跃用户
     * 当用户没有活跃任务时调用
     *
     * @param userId 用户ID
     */
    @Override
    public void removeActiveUser(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisService.sRemove(ACTIVE_USERS_KEY, userId);
            log.debug("移除活跃用户 - 用户ID: {}", userId);
        } catch (Exception e) {
            log.warn("从Redis移除活跃用户失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            // 不抛出异常，不影响主业务
        }
    }
    
    /**
     * 获取Redis key常量
     * 
     * @return Redis key
     */
    @Override
    public String getActiveUsersKey() {
        return ACTIVE_USERS_KEY;
    }
}
