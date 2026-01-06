import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 文件管理 Store
 */
export const useFileStore = defineStore('file', () => {
  // 状态
  const fileList = ref([])
  const currentUpload = ref(null)
  const uploadQueue = ref([])
  const uploadHistory = ref([])
  
  // 计算属性
  const uploadingFiles = computed(() => {
    return uploadQueue.value.filter(item => item.status === 'uploading')
  })
  
  const completedFiles = computed(() => {
    return uploadHistory.value.filter(item => item.status === 'completed')
  })
  
  const failedFiles = computed(() => {
    return uploadHistory.value.filter(item => item.status === 'failed')
  })
  
  // 方法
  function setFileList(list) {
    fileList.value = list
  }
  
  function addToQueue(fileInfo) {
    uploadQueue.value.push({
      id: Date.now() + Math.random(),
      ...fileInfo,
      status: 'pending',
      progress: 0,
      speed: 0,
      uploadedChunks: [],
      startTime: null,
      endTime: null
    })
  }
  
  function updateUploadProgress(id, progress) {
    const item = uploadQueue.value.find(f => f.id === id)
    if (item) {
      item.progress = progress
    }
  }
  
  function updateUploadSpeed(id, speed) {
    const item = uploadQueue.value.find(f => f.id === id)
    if (item) {
      item.speed = speed
    }
  }
  
  function setUploadStatus(id, status) {
    const item = uploadQueue.value.find(f => f.id === id)
    if (item) {
      item.status = status
      if (status === 'uploading' && !item.startTime) {
        item.startTime = Date.now()
      }
      if (['completed', 'failed'].includes(status)) {
        item.endTime = Date.now()
        // 移到历史记录
        uploadHistory.value.unshift({ ...item })
        uploadQueue.value = uploadQueue.value.filter(f => f.id !== id)
      }
    }
  }
  
  function removeFromQueue(id) {
    uploadQueue.value = uploadQueue.value.filter(f => f.id !== id)
  }
  
  function clearHistory() {
    uploadHistory.value = []
  }
  
  return {
    fileList,
    currentUpload,
    uploadQueue,
    uploadHistory,
    uploadingFiles,
    completedFiles,
    failedFiles,
    setFileList,
    addToQueue,
    updateUploadProgress,
    updateUploadSpeed,
    setUploadStatus,
    removeFromQueue,
    clearHistory
  }
})

