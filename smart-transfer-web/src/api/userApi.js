import request from '@/utils/http'
import axios from 'axios'

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
 * 上传头像
 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  // 使用axios直接上传，因为需要multipart/form-data
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem('token')
  
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
      throw new Error(res.data.message || '上传失败')
    }
  })
}

