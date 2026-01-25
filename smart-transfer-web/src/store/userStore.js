import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo, checkToken } from '@/api/userApi'
import router from '@/router'
import { userStorage } from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  // Token（从sessionStorage读取，实现标签页隔离）
  const token = ref(userStorage.getToken())
  
  // 用户信息（从sessionStorage读取，实现标签页隔离）
  const userInfo = ref(userStorage.getUserInfo())
  
  // 是否已登录
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  
  // 用户名
  const username = computed(() => userInfo.value?.username || '')
  
  // 昵称
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')
  
  // 头像URL（完整URL）
  const avatar = computed(() => {
    if (!userInfo.value?.avatar) return ''
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    // 如果avatar已经是完整URL，直接返回；否则拼接
    if (userInfo.value.avatar.startsWith('http://') || userInfo.value.avatar.startsWith('https://')) {
      return userInfo.value.avatar
    }
    // 相对路径格式：avatars/userId/filename
    // 添加时间戳防止缓存
    const timestamp = new Date().getTime()
    return `${baseURL}/user/avatar/${userInfo.value.avatar}?t=${timestamp}`
  })
  
  // 用户角色
  const role = computed(() => userInfo.value?.role || 'USER')
  
  // 是否是管理员
  const isAdmin = computed(() => role.value === 'ADMIN')
  
  
  /**
   * 登录
   */
  const login = async (username, password) => {
    const data = await loginApi({ username, password })
    
    // 保存 Token（使用sessionStorage，实现标签页隔离）
    token.value = data.token
    userStorage.setToken(data.token)
    
    // 保存用户信息（使用sessionStorage，实现标签页隔离）
    userInfo.value = {
      id: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar,
      role: data.role || 'USER'
    }
    userStorage.setUserInfo(userInfo.value)
    
    return data
  }
  
  /**
   * 登出
   */
  const logout = () => {
    token.value = ''
    userInfo.value = null
    // 清除sessionStorage中的用户数据（标签页级别）
    userStorage.clear()
    router.push('/login')
  }
  
  /**
   * 刷新用户信息
   */
  const refreshUserInfo = async () => {
    if (!token.value) return
    
    try {
      const data = await getUserInfo()
      userInfo.value = data
      // 更新sessionStorage中的用户信息
      userStorage.setUserInfo(data)
    } catch {
      // Token 无效，清除登录状态
      logout()
    }
  }
  
  /**
   * 检查登录状态
   */
  const checkLoginStatus = async () => {
    if (!token.value) return false
    
    try {
      const valid = await checkToken()
      if (!valid) {
        logout()
        return false
      }
      return true
    } catch {
      logout()
      return false
    }
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    nickname,
    avatar,
    role,
    isAdmin,
    login,
    logout,
    refreshUserInfo,
    checkLoginStatus
  }
})

