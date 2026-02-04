import request from '@/utils/http'

/**
 * 仪表盘API服务
 */

/**
 * 获取仪表盘数据（管理员）
 * @returns {Promise}
 */
export function getDashboardData() {
  return request.get({
    url: '/admin/dashboard/data'
  })
}

/**
 * 获取最近动态列表（管理员）
 * @param {number} limit - 查询数量限制
 * @returns {Promise}
 */
export function getRecentActivities(limit = 10) {
  return request.get({
    url: '/admin/dashboard/activities',
    params: { limit }
  })
}
