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

  // 上传配置：分片大小、最大文件大小（从系统配置接口获取）
  const transferConfig = ref({
    chunkSize: 5242880,      // 5MB，与后端 transfer.chunk-size 默认一致
    maxFileSize: 10737418240 // 10GB，与后端 max-file-size 默认一致
  })
  
  // 方法
  function updateCongestionConfig(config) {
    congestionConfig.value = {
      ...congestionConfig.value,
      ...config
    }
  }
  
  function updateTransferConfig(config) {
    if (config.chunkSize != null) transferConfig.value.chunkSize = Number(config.chunkSize)
    if (config.maxFileSize != null) transferConfig.value.maxFileSize = Number(config.maxFileSize)
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
    transferConfig,
    updateCongestionConfig,
    updateTransferConfig,
    updateSystemConfig,
    loadSystemConfig,
    resetCongestionConfig
  }
})

