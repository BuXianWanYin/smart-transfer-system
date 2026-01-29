import request from '@/utils/http'

/**
 * 传输历史记录API服务
 */

/**
 * 查询传输历史记录列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getHistoryList(params) {
  return request.get({
    url: '/transfer/history/list',
    params
  })
}

/**
 * 查询传输历史记录详情
 * @param {number} id - 记录ID
 * @returns {Promise}
 */
export function getHistoryDetail(id) {
  return request.get({
    url: `/transfer/history/${id}`
  })
}

/**
 * 新增传输历史记录
 * @param {Object} data - 历史记录数据
 * @returns {Promise}
 */
export function addHistory(data) {
  return request.post({
    url: '/transfer/history',
    data
  })
}

/**
 * 删除传输历史记录
 * @param {Array} ids - 记录ID数组
 * @returns {Promise}
 */
export function deleteHistory(ids) {
  return request.del({
    url: '/transfer/history',
    data: ids
  })
}

/**
 * 清空所有传输历史记录
 * @returns {Promise}
 */
export function clearAllHistory() {
  return request.del({
    url: '/transfer/history/clear'
  })
}

/**
 * 删除指定文件在最近若干秒内完成的传输历史（取消上传后移除误记的「已完成」）
 * @param {Number} fileId - 文件ID
 * @param {String} transferType - UPLOAD | DOWNLOAD
 * @param {Number} withinSeconds - 完成时间在多少秒以内，默认 120
 * @returns {Promise<number>} 删除条数
 */
export function deleteRecentHistoryByFile(fileId, transferType, withinSeconds = 120) {
  return request.del({
    url: '/transfer/history/recent-by-file',
    params: { fileId, transferType, withinSeconds }
  })
}

/**
 * 获取传输统计（按日/周/月）
 * @param {String} period - 统计周期：day-日, week-周, month-月
 * @param {Number} userId - 用户ID（可选，仅管理员可用）
 * @returns {Promise}
 */
export function getTransferStats(period = 'day', userId = null) {
  const params = { period }
  if (userId) {
    params.userId = userId
  }
  return request.get({
    url: '/transfer/history/stats',
    params
  })
}

/**
 * 获取算法使用统计
 * @param {Number} userId - 用户ID（可选，仅管理员可用）
 * @returns {Promise}
 */
export function getAlgorithmStats(userId = null) {
  const params = {}
  if (userId) {
    params.userId = userId
  }
  return request.get({
    url: '/transfer/history/algorithm-stats',
    params
  })
}

