import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 是否正在跳转登录页（防止重复跳转）
let isRedirectingToLogin = false

/**
 * 跳转到登录页
 */
const redirectToLogin = () => {
  if (isRedirectingToLogin) return
  
  isRedirectingToLogin = true
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  
  const currentPath = router.currentRoute.value.fullPath
  if (currentPath !== '/login') {
    router.push({ path: '/login', query: { redirect: currentPath } })
  }
  
  // 1秒后重置状态，允许再次跳转
  setTimeout(() => {
    isRedirectingToLogin = false
  }, 1000)
}

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
      // 401: 未登录或 token 过期
      if (res.code === 401) {
        ElMessage.error('登录已过期，请重新登录')
        redirectToLogin()
        return Promise.reject(new Error('未登录或登录已过期'))
      }
      
      ElMessage.error(res.message || res.msg || '请求失败')
      return Promise.reject(new Error(res.message || res.msg || '请求失败'))
    }
    
    // 返回响应数据的 data 字段（实际业务数据）
    return res.data
  },
  error => {
    // HTTP 状态码错误
    const status = error.response?.status
    
    if (status === 401) {
      // 401 未授权
      ElMessage.error('登录已过期，请重新登录')
      redirectToLogin()
      return Promise.reject(new Error('未登录或登录已过期'))
    }
    
    // 其他错误
    const message = error.response?.data?.message || error.response?.data?.msg || error.message || '网络错误'
    ElMessage.error(message)
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
    // 合并配置，确保timeout等参数正确传递
    const axiosConfig = {
      ...config,
      timeout: config.timeout !== undefined ? config.timeout : service.defaults.timeout,
      headers: {
        ...service.defaults.headers,
        ...config.headers
      }
    }
    return service.post(config.url, config.data, axiosConfig)
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
    return service.delete(config.url, { 
      params: config.params,
      data: config.data  // 支持 DELETE 请求体
    })
  }
}

export default request

