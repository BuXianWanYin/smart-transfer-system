import request from '@/utils/http'

/**
 * 系统配置 API
 */

/**
 * 获取拥塞控制配置
 * @returns {Promise}
 */
export function getCongestionConfig() {
  return request.get({
    url: '/config/congestion'
  })
}

/**
 * 更新拥塞控制配置
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function updateCongestionConfig(data) {
  return request.post({
    url: '/config/congestion',
    data
  })
}

/**
 * 获取所有配置
 * @returns {Promise}
 */
export function getAllConfigs() {
  return request.get({
    url: '/config/list'
  })
}

/**
 * 根据键获取配置值
 * @param {String} key - 配置键
 * @returns {Promise}
 */
export function getConfigValue(key) {
  return request.get({
    url: '/config/value',
    params: { key }
  })
}

/**
 * 刷新配置
 * @returns {Promise}
 */
export function refreshConfig() {
  return request.post({
    url: '/config/refresh'
  })
}

