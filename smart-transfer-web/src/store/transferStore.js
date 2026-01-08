import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { addHistory } from '@/api/historyApi'

/**
 * 传输任务 Store
 * 统一管理上传和下载任务状态
 */
export const useTransferStore = defineStore('transfer', () => {
  // 上传队列
  const uploadQueue = ref([])
  // 下载队列
  const downloadQueue = ref([])
  
  // 计算属性 - 正在上传的任务
  const uploadingTasks = computed(() => {
    return uploadQueue.value.filter(item => 
      item.status === 'uploading' || item.status === 'hashing' || item.status === 'pending'
    )
  })
  
  // 计算属性 - 正在下载的任务
  const downloadingTasks = computed(() => {
    return downloadQueue.value.filter(item => 
      item.status === 'downloading' || item.status === 'pending'
    )
  })
  
  // 计算属性 - 总传输任务数（徽章显示）
  const totalTransferCount = computed(() => {
    return uploadingTasks.value.length + downloadingTasks.value.length
  })
  
  // 计算属性 - 总上传速度
  const totalUploadSpeed = computed(() => {
    return uploadQueue.value
      .filter(item => item.status === 'uploading')
      .reduce((total, item) => total + (item.speed || 0), 0)
  })
  
  // 计算属性 - 总下载速度
  const totalDownloadSpeed = computed(() => {
    return downloadQueue.value
      .filter(item => item.status === 'downloading')
      .reduce((total, item) => total + (item.speed || 0), 0)
  })
  
  /**
   * 添加上传任务
   */
  function addUploadTask(task) {
    const newTask = {
      id: task.id || Date.now() + Math.random(),
      file: task.file,
      fileName: task.fileName,
      fileSize: task.fileSize,
      fileId: null,
      fileHash: null,
      status: 'pending',
      progress: 0,
      speed: 0,
      uploadedSize: 0,
      uploadedChunks: [],
      startTime: null,
      endTime: null,
      error: null
    }
    uploadQueue.value.push(newTask)
    return newTask
  }
  
  /**
   * 更新上传任务
   */
  function updateUploadTask(id, updates) {
    const task = uploadQueue.value.find(t => t.id === id)
    if (task) {
      Object.assign(task, updates)
    }
  }
  
  /**
   * 移除上传任务
   */
  function removeUploadTask(id) {
    const index = uploadQueue.value.findIndex(t => t.id === id)
    if (index !== -1) {
      uploadQueue.value.splice(index, 1)
    }
  }
  
  /**
   * 上传完成
   */
  async function completeUploadTask(id, fileHash) {
    const task = uploadQueue.value.find(t => t.id === id)
    if (task) {
      task.status = 'completed'
      task.progress = 100
      task.endTime = Date.now()
      task.speed = 0
      task.fileHash = fileHash
      
      // 记录历史
      const duration = Math.floor((task.endTime - task.startTime) / 1000)
      try {
        await addHistory({
          fileId: task.fileId,
          fileName: task.fileName,
          fileSize: task.fileSize,
          fileHash: fileHash,
          transferType: 'UPLOAD',
          transferStatus: 'COMPLETED',
          avgSpeed: duration > 0 ? Math.floor(task.fileSize / duration) : 0,
          duration: duration,
          algorithm: 'CUBIC',
          completedTime: new Date().toISOString()
        })
      } catch (error) {
        console.error('记录上传历史失败', error)
      }
    }
  }
  
  /**
   * 添加下载任务
   */
  function addDownloadTask(task) {
    const newTask = {
      id: task.id || Date.now() + Math.random(),
      fileId: task.fileId,
      fileName: task.fileName,
      fileSize: task.fileSize || 0,
      fileHash: task.fileHash || null,
      status: 'pending',
      progress: 0,
      speed: 0,
      downloadedSize: 0,
      startTime: null,
      endTime: null,
      error: null
    }
    downloadQueue.value.push(newTask)
    return newTask
  }
  
  /**
   * 更新下载任务
   */
  function updateDownloadTask(id, updates) {
    const task = downloadQueue.value.find(t => t.id === id)
    if (task) {
      Object.assign(task, updates)
    }
  }
  
  /**
   * 移除下载任务
   */
  function removeDownloadTask(id) {
    const index = downloadQueue.value.findIndex(t => t.id === id)
    if (index !== -1) {
      downloadQueue.value.splice(index, 1)
    }
  }
  
  /**
   * 下载完成
   */
  async function completeDownloadTask(id) {
    const task = downloadQueue.value.find(t => t.id === id)
    if (task) {
      task.status = 'completed'
      task.progress = 100
      task.endTime = Date.now()
      task.speed = 0
      
      // 记录历史
      const duration = Math.floor((task.endTime - task.startTime) / 1000)
      try {
        await addHistory({
          fileId: task.fileId,
          fileName: task.fileName,
          fileSize: task.fileSize,
          fileHash: task.fileHash || '',
          transferType: 'DOWNLOAD',
          transferStatus: 'COMPLETED',
          avgSpeed: duration > 0 ? Math.floor(task.fileSize / duration) : 0,
          duration: duration,
          algorithm: 'CUBIC',
          completedTime: new Date().toISOString()
        })
      } catch (error) {
        console.error('记录下载历史失败', error)
      }
    }
  }
  
  /**
   * 清除已完成的上传任务
   */
  function clearCompletedUploads() {
    uploadQueue.value = uploadQueue.value.filter(t => t.status !== 'completed')
  }
  
  /**
   * 清除已完成的下载任务
   */
  function clearCompletedDownloads() {
    downloadQueue.value = downloadQueue.value.filter(t => t.status !== 'completed')
  }
  
  /**
   * 全部暂停上传
   */
  function pauseAllUploads() {
    uploadQueue.value.forEach(task => {
      if (task.status === 'uploading' || task.status === 'hashing') {
        task.status = 'paused'
        task.speed = 0
      }
    })
  }
  
  /**
   * 全部暂停下载
   */
  function pauseAllDownloads() {
    downloadQueue.value.forEach(task => {
      if (task.status === 'downloading') {
        task.status = 'paused'
        task.speed = 0
      }
    })
  }
  
  return {
    uploadQueue,
    downloadQueue,
    uploadingTasks,
    downloadingTasks,
    totalTransferCount,
    totalUploadSpeed,
    totalDownloadSpeed,
    addUploadTask,
    updateUploadTask,
    removeUploadTask,
    completeUploadTask,
    addDownloadTask,
    updateDownloadTask,
    removeDownloadTask,
    completeDownloadTask,
    clearCompletedUploads,
    clearCompletedDownloads,
    pauseAllUploads,
    pauseAllDownloads
  }
})

