package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.domain.SystemActivity;
import com.server.smarttransferserver.service.DashboardService;
import com.server.smarttransferserver.service.SystemActivityService;
import com.server.smarttransferserver.vo.DashboardVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仪表盘控制器
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private SystemActivityService systemActivityService;
    
    /**
     * 获取仪表盘数据（需要管理员权限）
     */
    @RequireAdmin
    @GetMapping("/data")
    public Result<DashboardVO> getDashboardData() {
        try {
            DashboardVO dashboardVO = dashboardService.getDashboardData();
            return Result.success(dashboardVO);
        } catch (Exception e) {
            log.error("获取仪表盘数据失败", e);
            return Result.error("获取仪表盘数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近动态列表（需要管理员权限）
     */
    @RequireAdmin
    @GetMapping("/activities")
    public Result<List<SystemActivity>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<SystemActivity> activities = systemActivityService.getRecentActivities(limit);
            return Result.success(activities);
        } catch (Exception e) {
            log.error("获取最近动态失败", e);
            return Result.error("获取最近动态失败: " + e.getMessage());
        }
    }
}
