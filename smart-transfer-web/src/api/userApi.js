import request from '@/utils/http'

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

