import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 创建 axios 实例
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

/**
 * 请求拦截器
 */
service.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果返回的状态码不是 200，则认为是错误
    if (res.code !== 200) {
      ElMessage.error(res.message || res.msg || '请求失败')
      
      // 401: 未登录或 token 过期
      if (res.code === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      
      return Promise.reject(new Error(res.message || res.msg || '请求失败'))
    }
    
    // 返回响应数据的 data 字段（实际业务数据）
    // 后端返回格式：{ code: 200, message: '...', data: {...} }
    // 其中 data 可能是：
    // - 普通对象：{ quickUpload: false, fileId: 1, ... }
    // - 分页对象：{ records: [...], total: 10, size: 10, current: 1 }
    // 我们直接返回 data 字段
    return res.data
  },
  error => {
    console.error('请求错误：', error)
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

/**
 * 封装 HTTP 请求方法
 */
const request = {
  /**
   * GET 请求
   * @param {Object} config - 请求配置
   * @returns {Promise} 返回 Promise
   */
  get(config) {
    return service.get(config.url, { params: config.params })
  },
  
  /**
   * POST 请求
   * @param {Object} config - 请求配置
   * @returns {Promise} 返回 Promise
   */
  post(config) {
    return service.post(config.url, config.data, config)
  },
  
  /**
   * PUT 请求
   * @param {Object} config - 请求配置
   * @returns {Promise} 返回 Promise
   */
  put(config) {
    return service.put(config.url, config.data, config)
  },
  
  /**
   * DELETE 请求
   * @param {Object} config - 请求配置
   * @returns {Promise} 返回 Promise
   */
  del(config) {
    return service.delete(config.url, { params: config.params })
  }
}

export default request

