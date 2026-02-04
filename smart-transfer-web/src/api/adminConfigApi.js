import request from '@/utils/http'

/**
 * 管理员系统配置 API
 */

/**
 * 获取拥塞控制配置
 */
export function getCongestionConfig() {
  return request.get({
    url: '/admin/config/congestion'
  })
}

/**
 * 更新拥塞控制配置
 * @param {Object} data - 配置数据
 */
export function updateCongestionConfig(data) {
  return request.post({
    url: '/admin/config/congestion',
    data
  })
}

/**
 * 获取所有配置
 */
export function getAllConfigs() {
  return request.get({
    url: '/admin/config/list'
  })
}

/**
 * 根据键获取配置值
 * @param {String} key - 配置键
 */
export function getConfigValue(key) {
  return request.get({
    url: '/admin/config/value',
    params: { key }
  })
}

/**
 * 刷新配置
 */
export function refreshConfig() {
  return request.post({
    url: '/admin/config/refresh'
  })
}
