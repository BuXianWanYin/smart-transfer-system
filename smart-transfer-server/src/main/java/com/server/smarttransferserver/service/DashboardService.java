package com.server.smarttransferserver.service;

import com.server.smarttransferserver.vo.DashboardVO;

/**
 * 仪表盘服务接口
 */
public interface DashboardService {
    
    /**
     * 获取仪表盘完整数据
     *
     * @return 仪表盘数据
     */
    DashboardVO getDashboardData();
}
