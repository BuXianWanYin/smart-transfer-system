package com.server.smarttransferserver.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.domain.SystemActivity;

/**
 * 系统活动记录服务接口
 */
public interface SystemActivityService extends IService<SystemActivity> {
    
    /**
     * 记录系统活动
     *
     * @param activityType 活动类型
     * @param activityDesc 活动描述
     * @param relatedUserId 关联用户ID
     * @param relatedUserName 关联用户名
     */
    void recordActivity(String activityType, String activityDesc, Long relatedUserId, String relatedUserName);
    
    /**
     * 记录系统活动（带活动数据）
     *
     * @param activityType 活动类型
     * @param activityDesc 活动描述
     * @param relatedUserId 关联用户ID
     * @param relatedUserName 关联用户名
     * @param activityData 活动数据（JSON格式）
     */
    void recordActivity(String activityType, String activityDesc, Long relatedUserId, String relatedUserName, String activityData);
    
    /**
     * 获取最近的系统活动列表
     *
     * @param limit 查询数量限制
     * @return 系统活动列表
     */
    List<SystemActivity> getRecentActivities(int limit);
}
