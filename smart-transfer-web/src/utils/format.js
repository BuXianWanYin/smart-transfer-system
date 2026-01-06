import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 格式化工具函数
 */

/**
 * 格式化日期时间
 * @param {String|Date} date - 日期
 * @param {String} format - 格式
 * @returns {String}
 */
export function formatDateTime(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 格式化相对时间
 * @param {String|Date} date - 日期
 * @returns {String}
 */
export function formatRelativeTime(date) {
  if (!date) return '-'
  return dayjs(date).fromNow()
}

/**
 * 格式化数字（千分位）
 * @param {Number} num - 数字
 * @returns {String}
 */
export function formatNumber(num) {
  if (num === null || num === undefined) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 格式化百分比
 * @param {Number} value - 值
 * @param {Number} digits - 小数位数
 * @returns {String}
 */
export function formatPercent(value, digits = 2) {
  if (value === null || value === undefined) return '0%'
  return (value * 100).toFixed(digits) + '%'
}

/**
 * 格式化带宽（bps）
 * @param {Number} bps - 比特每秒
 * @returns {String}
 */
export function formatBandwidth(bps) {
  if (bps === 0) return '0 bps'
  
  const k = 1000
  const sizes = ['bps', 'Kbps', 'Mbps', 'Gbps']
  const i = Math.floor(Math.log(bps) / Math.log(k))
  
  return (bps / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 格式化延迟（ms）
 * @param {Number} ms - 毫秒
 * @returns {String}
 */
export function formatLatency(ms) {
  if (ms === null || ms === undefined) return '-'
  if (ms < 1) return '<1ms'
  return Math.round(ms) + 'ms'
}

/**
 * 获取上传状态文本
 * @param {String} status - 状态码
 * @returns {String}
 */
export function getUploadStatusText(status) {
  const statusMap = {
    'PENDING': '待上传',
    'UPLOADING': '上传中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'PAUSED': '已暂停'
  }
  return statusMap[status] || status
}

/**
 * 获取任务类型文本
 * @param {String} type - 类型码
 * @returns {String}
 */
export function getTaskTypeText(type) {
  const typeMap = {
    'UPLOAD': '上传',
    'DOWNLOAD': '下载'
  }
  return typeMap[type] || type
}

/**
 * 获取算法名称
 * @param {String} algorithm - 算法代码
 * @returns {String}
 */
export function getAlgorithmName(algorithm) {
  const algorithmMap = {
    'CUBIC': 'CUBIC 算法',
    'BBR': 'BBR 算法',
    'ADAPTIVE': '自适应算法'
  }
  return algorithmMap[algorithm] || algorithm
}

/**
 * 获取网络质量文本和颜色
 * @param {String} quality - 质量等级
 * @returns {Object}
 */
export function getNetworkQualityInfo(quality) {
  const qualityMap = {
    '优秀': { text: '优秀', color: 'success', icon: 'SuccessFilled' },
    '良好': { text: '良好', color: 'primary', icon: 'CircleCheckFilled' },
    '一般': { text: '一般', color: 'warning', icon: 'WarningFilled' },
    '差': { text: '差', color: 'danger', icon: 'CircleCloseFilled' }
  }
  return qualityMap[quality] || { text: quality, color: 'info', icon: 'QuestionFilled' }
}

/**
 * 截断文本
 * @param {String} text - 文本
 * @param {Number} maxLength - 最大长度
 * @returns {String}
 */
export function truncateText(text, maxLength = 50) {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

