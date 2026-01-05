import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 应用状态管理
 */
export const useAppStore = defineStore('app', () => {
  // 状态
  const loading = ref(false)
  
  // 计算属性
  const isLoading = computed(() => loading.value)
  
  /**
   * 设置加载状态
   * @param {boolean} value - 加载状态
   */
  function setLoading(value) {
    loading.value = value
  }
  
  return {
    loading,
    isLoading,
    setLoading
  }
})

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref('')
  const userInfo = ref(null)
  
  // 计算属性
  const isLogin = computed(() => !!token.value)
  const userName = computed(() => userInfo.value?.userName || '')
  
  /**
   * 设置token
   * @param {string} newToken - 新的token
   */
  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }
  
  /**
   * 设置用户信息
   * @param {Object} info - 用户信息
   */
  function setUserInfo(info) {
    userInfo.value = info
  }
  
  /**
   * 清除用户信息
   */
  function clearUserInfo() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }
  
  return {
    token,
    userInfo,
    isLogin,
    userName,
    setToken,
    setUserInfo,
    clearUserInfo
  }
}, {
  persist: true
})

