import request from '@/utils/http'

/**
 * 管理员仪表盘 API
 */

/**
 * 获取仪表盘数据
 */
export function getDashboardData() {
  return request.get({
    url: '/admin/dashboard/data'
  })
}

/**
 * 获取最近动态列表
 * @param {number} limit - 查询数量限制
 */
export function getRecentActivities(limit = 10) {
  return request.get({
    url: '/admin/dashboard/activities',
    params: { limit }
  })
}
