import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 配置管理 Store
 */
export const useConfigStore = defineStore('config', () => {
  // 状态
  const congestionConfig = ref({
    algorithm: 'CUBIC',
    initialCwnd: 10485760,      // 10MB
    ssthresh: 52428800,         // 50MB
    maxCwnd: 104857600,         // 100MB
    minCwnd: 1048576            // 1MB
  })
  
  const systemConfig = ref({
    theme: 'light',
    language: 'zh-CN',
    autoRefresh: true,
    refreshInterval: 3000
  })
  
  // 方法
  function updateCongestionConfig(config) {
    congestionConfig.value = {
      ...congestionConfig.value,
      ...config
    }
  }
  
  function updateSystemConfig(config) {
    systemConfig.value = {
      ...systemConfig.value,
      ...config
    }
    // 保存到 localStorage
    localStorage.setItem('systemConfig', JSON.stringify(systemConfig.value))
  }
  
  function loadSystemConfig() {
    const saved = localStorage.getItem('systemConfig')
    if (saved) {
      try {
        systemConfig.value = JSON.parse(saved)
      } catch (e) {
        // 加载配置失败
      }
    }
  }
  
  function resetCongestionConfig() {
    congestionConfig.value = {
      algorithm: 'CUBIC',
      initialCwnd: 10485760,
      ssthresh: 52428800,
      maxCwnd: 104857600,
      minCwnd: 1048576
    }
  }
  
  return {
    congestionConfig,
    systemConfig,
    updateCongestionConfig,
    updateSystemConfig,
    loadSystemConfig,
    resetCongestionConfig
  }
})

