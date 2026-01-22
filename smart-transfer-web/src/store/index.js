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


