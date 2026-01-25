/**
 * 存储工具
 * 区分用户数据（sessionStorage）和配置数据（localStorage）
 */

/**
 * 用户数据存储（sessionStorage - 标签页级别，不同标签页隔离）
 */
export const userStorage = {
  /**
   * 获取 token
   */
  getToken() {
    return sessionStorage.getItem('token') || ''
  },
  
  /**
   * 设置 token
   */
  setToken(token) {
    if (token) {
      sessionStorage.setItem('token', token)
    } else {
      sessionStorage.removeItem('token')
    }
  },
  
  /**
   * 获取用户信息
   */
  getUserInfo() {
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (!userInfoStr) return null
    try {
      return JSON.parse(userInfoStr)
    } catch {
      return null
    }
  },
  
  /**
   * 设置用户信息
   */
  setUserInfo(userInfo) {
    if (userInfo) {
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
    } else {
      sessionStorage.removeItem('userInfo')
    }
  },
  
  /**
   * 清除所有用户数据
   */
  clear() {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }
}

/**
 * 配置数据存储（localStorage - 浏览器级别，所有标签页共享）
 * 用于存储非用户相关的配置，如UI偏好、文件列表配置等
 */
export const configStorage = {
  /**
   * 获取配置值
   */
  getItem(key) {
    return localStorage.getItem(key)
  },
  
  /**
   * 设置配置值
   */
  setItem(key, value) {
    localStorage.setItem(key, value)
  },
  
  /**
   * 删除配置值
   */
  removeItem(key) {
    localStorage.removeItem(key)
  }
}
