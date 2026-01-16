package com.server.smarttransferserver.service;

/**
 * 活跃用户管理服务
 * 负责管理Redis中的活跃用户集合，避免循环依赖
 * 只负责纯粹的Redis操作，不依赖其他业务服务
 */
public interface ActiveUserService {
    
    /**
     * 添加活跃用户到Redis集合
     * 当用户有活跃任务时调用
     *
     * @param userId 用户ID
     */
    void addActiveUser(Long userId);
    
    /**
     * 从Redis集合中移除活跃用户
     * 当用户没有活跃任务时调用
     *
     * @param userId 用户ID
     */
    void removeActiveUser(Long userId);
    
    /**
     * 获取Redis key常量
     * 
     * @return Redis key
     */
    String getActiveUsersKey();
}
