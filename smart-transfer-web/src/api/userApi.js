import request from '@/utils/http'
import axios from 'axios'
import { userStorage } from '@/utils/storage'

/**
 * 用户登录
 */
export const login = (data) => {
  return request.post({ url: '/user/login', data })
}

/**
 * 用户注册
 */
export const register = (data) => {
  return request.post({ url: '/user/register', data })
}

/**
 * 获取当前用户信息
 */
export const getUserInfo = () => {
  return request.get({ url: '/user/info' })
}

/**
 * 修改密码
 */
export const changePassword = (data) => {
  return request.put({ url: '/user/password', data })
}

/**
 * 更新用户信息
 */
export const updateUserInfo = (data) => {
  return request.put({ url: '/user/info', data })
}

/**
 * 检查 Token 是否有效
 */
export const checkToken = () => {
  return request.get({ url: '/user/check-token' })
}

/**
 * 获取存储统计
 */
export const getStorageStats = () => {
  return request.get({ url: '/user/storage' })
}

// ========== 管理员接口 ==========

/**
 * 获取用户列表（管理员）
 */
export const getUserList = () => {
  return request.get({ url: '/user/list' })
}

/**
 * 更新用户状态（管理员）
 */
export const updateUserStatus = (userId, status) => {
  return request.put({ url: `/user/status/${userId}`, data: { status } })
}

/**
 * 删除用户（管理员）
 */
export const deleteUser = (userId) => {
  return request.del({ url: `/user/${userId}` })
}

/**
 * 获取系统级存储统计（管理员）
 */
export const getSystemStorageStats = () => {
  return request.get({ url: '/user/system-storage' })
}

/**
 * 获取用户详情（存储使用、传输统计）（管理员）
 */
export const getUserDetail = (userId) => {
  return request.get({ url: `/user/detail/${userId}` })
}

/**
 * 批量更新用户状态（管理员）
 */
export const batchUpdateUserStatus = (userIds, status) => {
  return request.put({ url: '/user/batch-status', data: { userIds, status } })
}

/**
 * 批量删除用户（管理员）
 */
export const batchDeleteUsers = (userIds) => {
  return request.del({ url: '/user/batch', data: { userIds } })
}

/**
 * 管理员更新用户信息
 */
export const updateUserInfoByAdmin = (userId, data) => {
  return request.put({ url: `/user/${userId}/info`, data })
}

/**
 * 管理员上传用户头像
 */
export const uploadAvatarByAdmin = (userId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = userStorage.getToken()
  
  return axios.post(`${baseURL}/user/${userId}/avatar`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${token}`
    }
  }).then(res => {
    if (res.data.code === 200) {
      return res.data.data
    } else {
      const errorMessage = res.data.message || res.data.msg || '上传失败'
      console.error('头像上传失败:', res.data)
      throw new Error(errorMessage)
    }
  }).catch(error => {
    if (error.response) {
      const errorData = error.response.data
      const errorMessage = errorData?.message || errorData?.msg || error.message || '上传失败'
      console.error('头像上传失败 - 响应:', error.response.status, errorData)
      throw new Error(errorMessage)
    } else if (error.request) {
      console.error('头像上传失败 - 网络错误:', error.message)
      throw new Error('网络错误，请检查网络连接')
    } else {
      console.error('头像上传失败:', error.message)
      throw error
    }
  })
}

/**
 * 上传头像
 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  // 使用axios直接上传，因为需要multipart/form-data
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = userStorage.getToken()
  
  return axios.post(`${baseURL}/user/avatar`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${token}`
    }
  }).then(res => {
    // 统一响应格式处理
    if (res.data.code === 200) {
      return res.data.data
    } else {
      const errorMessage = res.data.message || res.data.msg || '上传失败'
      console.error('头像上传失败:', res.data)
      throw new Error(errorMessage)
    }
  }).catch(error => {
    // 增强错误处理
    if (error.response) {
      // 服务器返回了错误响应
      const errorData = error.response.data
      const errorMessage = errorData?.message || errorData?.msg || error.message || '上传失败'
      console.error('头像上传失败 - 响应:', error.response.status, errorData)
      throw new Error(errorMessage)
    } else if (error.request) {
      // 请求已发出但没有收到响应
      console.error('头像上传失败 - 网络错误:', error.message)
      throw new Error('网络错误，请检查网络连接')
    } else {
      // 其他错误
      console.error('头像上传失败:', error.message)
      throw error
    }
  })
}

