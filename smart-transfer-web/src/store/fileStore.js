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
  const downloadQueue = ref([])
  const uploadHistory = ref([])
  const allHistory = ref([]) // 所有传输历史（上传+下载）
  
  // 文件列表相关状态
  const selectedFiles = ref([]) // 选中的文件
  const fileModel = ref(0) // 文件展示模式 0-列表 1-网格 2-时间线
  const gridSize = ref(80) // 网格图标大小
  const showUploadMask = ref(false) // 显示拖拽上传遮罩
  
  // 计算属性
  const uploadingFiles = computed(() => {
    return uploadQueue.value.filter(item => item.status === 'uploading' || item.status === 'hashing')
  })
  
  const downloadingFiles = computed(() => {
    return downloadQueue.value.filter(item => item.status === 'downloading')
  })
  
  const completedFiles = computed(() => {
    return allHistory.value.filter(item => item.status === 'completed')
  })
  
  const failedFiles = computed(() => {
    return allHistory.value.filter(item => item.status === 'failed')
  })

  // 当前总上传速度
  const currentUploadSpeed = computed(() => {
    return uploadQueue.value
      .filter(item => item.status === 'uploading')
      .reduce((total, item) => total + (item.speed || 0), 0)
  })

  // 当前总下载速度
  const currentDownloadSpeed = computed(() => {
    return downloadQueue.value
      .filter(item => item.status === 'downloading')
      .reduce((total, item) => total + (item.speed || 0), 0)
  })

  // 当前总传输速度
  const currentTotalSpeed = computed(() => {
    return currentUploadSpeed.value + currentDownloadSpeed.value
  })
  
  // 方法
  function setFileList(list) {
    fileList.value = list
  }
  
  function addToQueue(fileInfo) {
    uploadQueue.value.push({
      ...fileInfo,
      id: fileInfo.id || Date.now() + Math.random(),
      status: fileInfo.status || 'pending',
      progress: fileInfo.progress || 0,
      speed: fileInfo.speed || 0,
      uploadedChunks: fileInfo.uploadedChunks || [],
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

  function updateUploadStatus(id, status) {
    const item = uploadQueue.value.find(f => f.id === id)
    if (item) {
      item.status = status
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
    allHistory.value = []
  }
  
  // 下载相关方法
  function addDownloadTask(task) {
    downloadQueue.value.push({
      id: Date.now() + Math.random(),
      ...task,
      status: 'pending',
      progress: 0,
      speed: 0,
      downloadedSize: 0,
      startTime: null,
      endTime: null,
      transferType: 'download'
    })
  }
  
  function removeDownloadTask(id) {
    downloadQueue.value = downloadQueue.value.filter(f => f.id !== id)
  }
  
  function moveToCompleted(item) {
    allHistory.value.unshift({
      ...item,
      completedTime: new Date().toISOString(),
      transferType: 'upload'
    })
  }
  
  function moveDownloadToCompleted(item) {
    allHistory.value.unshift({
      ...item,
      completedTime: new Date().toISOString(),
      transferType: 'download'
    })
  }
  
  // 文件列表相关方法
  function setSelectedFiles(files) {
    selectedFiles.value = files
  }
  
  function clearSelectedFiles() {
    selectedFiles.value = []
  }
  
  function setFileModel(model) {
    fileModel.value = model
    localStorage.setItem('file_model', model)
  }
  
  function setGridSize(size) {
    gridSize.value = size
    localStorage.setItem('grid_size', size)
  }
  
  // 初始化从本地存储读取
  function initFromStorage() {
    const savedModel = localStorage.getItem('file_model')
    if (savedModel !== null) {
      fileModel.value = Number(savedModel)
    }
    const savedSize = localStorage.getItem('grid_size')
    if (savedSize !== null) {
      gridSize.value = Number(savedSize)
    }
  }
  
  // 调用初始化
  initFromStorage()
  
  return {
    fileList,
    currentUpload,
    uploadQueue,
    downloadQueue,
    uploadHistory,
    allHistory,
    uploadingFiles,
    downloadingFiles,
    completedFiles,
    failedFiles,
    currentUploadSpeed,
    currentDownloadSpeed,
    currentTotalSpeed,
    selectedFiles,
    fileModel,
    gridSize,
    showUploadMask,
    setFileList,
    addToQueue,
    updateUploadProgress,
    updateUploadSpeed,
    updateUploadStatus,
    setUploadStatus,
    removeFromQueue,
    clearHistory,
    addDownloadTask,
    removeDownloadTask,
    moveToCompleted,
    moveDownloadToCompleted,
    setSelectedFiles,
    clearSelectedFiles,
    setFileModel,
    setGridSize
  }
})

