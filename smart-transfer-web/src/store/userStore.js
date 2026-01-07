import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo, checkToken } from '@/api/userApi'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // Token
  const token = ref(localStorage.getItem('token') || '')
  
  // 用户信息
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  
  // 是否已登录
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  
  // 用户名
  const username = computed(() => userInfo.value?.username || '')
  
  // 昵称
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')
  
  // 头像
  const avatar = computed(() => userInfo.value?.avatar || '')
  
  
  /**
   * 登录
   */
  const login = async (username, password) => {
    const data = await loginApi({ username, password })
    
    // 保存 Token
    token.value = data.token
    localStorage.setItem('token', data.token)
    
    // 保存用户信息
    userInfo.value = {
      id: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    
    return data
  }
  
  /**
   * 登出
   */
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
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
      localStorage.setItem('userInfo', JSON.stringify(data))
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
    login,
    logout,
    refreshUserInfo,
    checkLoginStatus
  }
})

