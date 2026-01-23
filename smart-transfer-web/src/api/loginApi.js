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
      url: '/user/login',
      data
    })
  }
  
  /**
   * 用户登出
   * @returns {Promise} 返回操作结果
   * @deprecated 后端暂无此接口，建议使用前端清除token
   */
  static logout() {
    // 后端暂无logout接口，前端清除token即可
    return Promise.resolve()
  }
  
  /**
   * 获取用户信息
   * @returns {Promise} 返回用户信息
   */
  static getUserInfo() {
    return request.get({
      url: '/user/info'
    })
  }
}

