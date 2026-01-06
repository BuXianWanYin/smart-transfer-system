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
  return request({
    url: '/api/transfer/history/list',
    method: 'get',
    params
  })
}

/**
 * 查询传输历史记录详情
 * @param {number} id - 记录ID
 * @returns {Promise}
 */
export function getHistoryDetail(id) {
  return request({
    url: `/api/transfer/history/${id}`,
    method: 'get'
  })
}

/**
 * 新增传输历史记录
 * @param {Object} data - 历史记录数据
 * @returns {Promise}
 */
export function addHistory(data) {
  return request({
    url: '/api/transfer/history',
    method: 'post',
    data
  })
}

/**
 * 删除传输历史记录
 * @param {Array} ids - 记录ID数组
 * @returns {Promise}
 */
export function deleteHistory(ids) {
  return request({
    url: `/api/transfer/history/${ids}`,
    method: 'delete'
  })
}

/**
 * 清空所有传输历史记录
 * @returns {Promise}
 */
export function clearAllHistory() {
  return request({
    url: '/api/transfer/history/clear',
    method: 'delete'
  })
}

