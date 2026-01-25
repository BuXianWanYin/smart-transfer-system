/**
 * 格式化工具函数
 */

/**
 * 格式化文件大小
 * @param {number} size 文件大小（字节）
 * @returns {string} 格式化后的文件大小
 */
export function formatFileSize(size) {
  if (size === null || size === undefined) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

/**
 * 格式化日期时间
 * @param {string|Date} dateStr 日期字符串或Date对象
 * @returns {string} 格式化后的日期时间
 */
export function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '-'
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化日期
 * @param {string|Date} dateStr 日期字符串或Date对象
 * @returns {string} 格式化后的日期
 */
export function formatDate(dateStr) {
  if (!dateStr) return '-'
  
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '-'
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  return `${year}-${month}-${day}`
}

/**
 * 格式化传输速度
 * @param {number} speed 速度（字节/秒）
 * @returns {string} 格式化后的速度
 */
export function formatSpeed(speed) {
  if (!speed || speed <= 0) return '0 B/s'
  return formatFileSize(speed) + '/s'
}

/**
 * 格式化时长
 * @param {number} seconds 秒数
 * @returns {string} 格式化后的时长
 */
export function formatDuration(seconds) {
  if (!seconds || seconds < 0) return '-'
  
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}分${secs}秒`
  }
  
  const hours = Math.floor(seconds / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  return `${hours}时${mins}分${secs}秒`
}

/**
 * 获取相对时间
 * @param {string|Date} dateStr 日期字符串或Date对象
 * @returns {string} 相对时间描述
 */
export function getRelativeTime(dateStr) {
  if (!dateStr) return '-'
  
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '-'
  
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  
  return formatDate(dateStr)
}

/**
 * 格式化百分比
 * @param {number} value 数值（0-1之间的小数或百分比数值）
 * @param {number} decimals 小数位数，默认2位
 * @returns {string} 格式化后的百分比字符串
 */
export function formatPercent(value, decimals = 2) {
  if (value === null || value === undefined || isNaN(value)) return '-'
  
  // 如果值大于1，假设已经是百分比形式（如50表示50%），需要除以100
  // 如果值小于等于1，假设是小数形式（如0.05表示5%）
  const percentValue = value > 1 ? value : value * 100
  
  return percentValue.toFixed(decimals) + '%'
}
