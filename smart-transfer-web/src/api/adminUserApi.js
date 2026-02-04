import request from '@/utils/http'
import axios from 'axios'
import { userStorage } from '@/utils/storage'

/**
 * 管理员用户管理 API
 */

/**
 * 获取用户列表
 */
export const getUserList = () => {
  return request.get({ url: '/admin/user/list' })
}

/**
 * 更新用户状态
 */
export const updateUserStatus = (userId, status) => {
  return request.put({ url: `/admin/user/status/${userId}`, data: { status } })
}

/**
 * 删除用户
 */
export const deleteUser = (userId) => {
  return request.del({ url: `/admin/user/${userId}` })
}

/**
 * 获取系统级存储统计
 */
export const getSystemStorageStats = () => {
  return request.get({ url: '/admin/user/system-storage' })
}

/**
 * 获取用户详情（存储使用、传输统计）
 */
export const getUserDetail = (userId) => {
  return request.get({ url: `/admin/user/detail/${userId}` })
}

/**
 * 批量更新用户状态
 */
export const batchUpdateUserStatus = (userIds, status) => {
  return request.put({ url: '/admin/user/batch-status', data: { userIds, status } })
}

/**
 * 批量删除用户
 */
export const batchDeleteUsers = (userIds) => {
  return request.del({ url: '/admin/user/batch', data: { userIds } })
}

/**
 * 更新用户信息
 */
export const updateUserInfoByAdmin = (userId, data) => {
  return request.put({ url: `/admin/user/${userId}/info`, data })
}

/**
 * 上传用户头像
 */
export const uploadAvatarByAdmin = (userId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = userStorage.getToken()
  
  return axios.post(`${baseURL}/admin/user/${userId}/avatar`, formData, {
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
