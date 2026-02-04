import request from '@/utils/http'

/**
 * 系统配置 API（管理员）
 */

/**
 * 获取拥塞控制配置（管理员）
 * @returns {Promise}
 */
export function getCongestionConfig() {
  return request.get({
    url: '/admin/config/congestion'
  })
}

/**
 * 更新拥塞控制配置（管理员）
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function updateCongestionConfig(data) {
  return request.post({
    url: '/admin/config/congestion',
    data
  })
}

/**
 * 获取所有配置（管理员）
 * @returns {Promise}
 */
export function getAllConfigs() {
  return request.get({
    url: '/admin/config/list'
  })
}

/**
 * 根据键获取配置值（管理员）
 * @param {String} key - 配置键
 * @returns {Promise}
 */
export function getConfigValue(key) {
  return request.get({
    url: '/admin/config/value',
    params: { key }
  })
}

/**
 * 刷新配置（管理员）
 * @returns {Promise}
 */
export function refreshConfig() {
  return request.post({
    url: '/admin/config/refresh'
  })
}

