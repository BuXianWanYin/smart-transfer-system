import request from '@/utils/http'

/**
 * 拥塞控制 API
 */

/**
 * 获取当前拥塞控制指标
 * @returns {Promise}
 */
export function getCongestionMetrics() {
  return request.get({
    url: '/congestion/metrics'
  })
}

/**
 * 获取当前使用的算法
 * @returns {Promise}
 */
export function getCurrentAlgorithm() {
  return request.get({
    url: '/congestion/algorithm'
  })
}

/**
 * 切换拥塞控制算法
 * @param {String} algorithm - 算法名称（RENO, VEGAS, CUBIC, BBR, ADAPTIVE）
 * @returns {Promise}
 */
export function switchAlgorithm(algorithm) {
  return request.post({
    url: '/congestion/algorithm',
    params: { algorithm }
  })
}

/**
 * 获取算法状态详情
 * @returns {Promise}
 */
export function getAlgorithmStatus() {
  return request.get({
    url: '/congestion/status'
  })
}

/**
 * 获取自适应算法详细指标（用于可观测性）
 * @returns {Promise}
 */
export function getAdaptiveMetrics() {
  return request.get({
    url: '/congestion/adaptive-metrics'
  })
}
