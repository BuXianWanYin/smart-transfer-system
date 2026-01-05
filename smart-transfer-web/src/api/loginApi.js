import request from '@/utils/http'

/**
 * 登录相关API服务
 */
export class LoginService {
  /**
   * 用户登录
   * @param {Object} data - 登录信息
   * @param {string} data.username - 用户名
   * @param {string} data.password - 密码
   * @returns {Promise} 返回登录结果
   */
  static login(data) {
    return request.post({
      url: '/auth/login',
      data
    })
  }
  
  /**
   * 用户登出
   * @returns {Promise} 返回操作结果
   */
  static logout() {
    return request.post({
      url: '/auth/logout'
    })
  }
  
  /**
   * 获取用户信息
   * @returns {Promise} 返回用户信息
   */
  static getUserInfo() {
    return request.get({
      url: '/auth/getUserInfo'
    })
  }
}

