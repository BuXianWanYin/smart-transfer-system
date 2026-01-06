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
    url: `/transfer/history/${ids}`
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

