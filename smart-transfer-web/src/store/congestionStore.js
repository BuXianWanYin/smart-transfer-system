import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 拥塞控制 Store
 */
export const useCongestionStore = defineStore('congestion', () => {
  // 状态
  const currentMetrics = ref({
    algorithm: 'NONE',
    cwnd: 0,
    ssthresh: 0,
    rate: 0,
    state: '未初始化',
    rtt: 0,
    minRtt: 0,
    lossRate: 0,
    bandwidth: 0,
    networkQuality: '未知',
    inflightCount: 0,
    inflightBytes: 0
  })
  
  const metricsHistory = ref([])
  const currentAlgorithm = ref('CUBIC')
  const isMonitoring = ref(false)
  
  // 方法
  function updateMetrics(metrics) {
    currentMetrics.value = {
      ...currentMetrics.value,
      ...metrics,
      timestamp: Date.now()
    }
    
    // 添加到历史记录（保留最近100条）
    metricsHistory.value.push({
      ...metrics,
      timestamp: Date.now()
    })
    
    if (metricsHistory.value.length > 100) {
      metricsHistory.value.shift()
    }
  }
  
  function setAlgorithm(algorithm) {
    currentAlgorithm.value = algorithm
  }
  
  function startMonitoring() {
    isMonitoring.value = true
  }
  
  function stopMonitoring() {
    isMonitoring.value = false
  }
  
  function clearHistory() {
    metricsHistory.value = []
  }
  
  function reset() {
    currentMetrics.value = {
      algorithm: 'NONE',
      cwnd: 0,
      ssthresh: 0,
      rate: 0,
      state: '未初始化',
      rtt: 0,
      minRtt: 0,
      lossRate: 0,
      bandwidth: 0,
      networkQuality: '未知',
      inflightCount: 0,
      inflightBytes: 0
    }
    metricsHistory.value = []
  }
  
  return {
    currentMetrics,
    metricsHistory,
    currentAlgorithm,
    isMonitoring,
    updateMetrics,
    setAlgorithm,
    startMonitoring,
    stopMonitoring,
    clearHistory,
    reset
  }
})

