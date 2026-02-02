package com.server.smarttransferserver.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.server.smarttransferserver.domain.SystemActivity;
import com.server.smarttransferserver.mapper.SystemActivityMapper;
import com.server.smarttransferserver.service.SystemActivityService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统活动记录服务实现类
 */
@Slf4j
@Service
public class SystemActivityServiceImpl extends ServiceImpl<SystemActivityMapper, SystemActivity> 
        implements SystemActivityService {
    
    /**
     * 记录系统活动
     *
     * @param activityType 活动类型
     * @param activityDesc 活动描述
     * @param relatedUserId 关联用户ID
     * @param relatedUserName 关联用户名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordActivity(String activityType, String activityDesc, Long relatedUserId, String relatedUserName) {
        recordActivity(activityType, activityDesc, relatedUserId, relatedUserName, null);
    }
    
    /**
     * 记录系统活动（带活动数据）
     *
     * @param activityType 活动类型
     * @param activityDesc 活动描述
     * @param relatedUserId 关联用户ID
     * @param relatedUserName 关联用户名
     * @param activityData 活动数据（JSON格式）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordActivity(String activityType, String activityDesc, Long relatedUserId, String relatedUserName, String activityData) {
        try {
            SystemActivity activity = SystemActivity.builder()
                    .activityType(activityType)
                    .activityDesc(activityDesc)
                    .relatedUserId(relatedUserId)
                    .relatedUserName(relatedUserName)
                    .activityData(activityData)
                    .createTime(LocalDateTime.now())
                    .build();
            
            save(activity);
            log.info("记录系统活动成功: type={}, desc={}, userId={}", activityType, activityDesc, relatedUserId);
        } catch (Exception e) {
            log.error("记录系统活动失败: type={}, desc={}, userId={}, error={}", activityType, activityDesc, relatedUserId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取最近的系统活动列表
     *
     * @param limit 查询数量限制
     * @return 系统活动列表
     */
    @Override
    public List<SystemActivity> getRecentActivities(int limit) {
        LambdaQueryWrapper<SystemActivity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(SystemActivity::getCreateTime);
        queryWrapper.last("LIMIT " + limit);
        
        return list(queryWrapper);
    }
}
