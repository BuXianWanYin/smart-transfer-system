import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { addHistory } from '@/api/historyApi'
import { formatDateTime } from '@/utils/format'

/**
 * 传输任务 Store
 * 统一管理上传和下载任务状态
 * 支持断点续传和失败重试
 */
export const useTransferStore = defineStore('transfer', () => {
  // 上传队列
  const uploadQueue = ref([])
  // 下载队列
  const downloadQueue = ref([])
  
  // 计算属性 - 正在上传的任务（包括错误状态，可重试）
  const uploadingTasks = computed(() => {
    return uploadQueue.value.filter(item => 
      item.status === 'uploading' || item.status === 'hashing' || item.status === 'pending' || item.status === 'paused' || item.status === 'error'
    )
  })
  
  // 计算属性 - 正在下载的任务（包括错误状态，可重试）
  const downloadingTasks = computed(() => {
    return downloadQueue.value.filter(item => 
      item.status === 'downloading' || item.status === 'pending' || item.status === 'paused' || item.status === 'error'
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
      folderId: task.folderId || 0,
      relativePath: task.relativePath || '',
      fileId: null,
      taskId: null,  // **修复：添加taskId字段用于监控数据匹配**
      fileHash: null,
      hashProgress: 0,
      status: 'pending',
      progress: 0,
      speed: 0,
      uploadedSize: 0,
      uploadedChunks: [],  // 已上传的分片索引（用于断点续传）
      totalChunks: 0,
      chunkSize: 5 * 1024 * 1024,  // 默认5MB分片
      startTime: null,
      endTime: null,
      error: null,
      retryCount: 0,  // 重试次数
      maxRetry: 3     // 最大重试次数
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
      
      // 记录成功历史
      await recordUploadHistory(task, 'COMPLETED')
    }
  }
  
  /**
   * 上传失败
   */
  async function failUploadTask(id, errorMessage) {
    const task = uploadQueue.value.find(t => t.id === id)
    if (task) {
      task.status = 'error'
      task.error = errorMessage
      task.endTime = Date.now()
      task.speed = 0
      
      // 记录失败历史
      await recordUploadHistory(task, 'FAILED', errorMessage)
    }
  }
  
  /**
   * 记录上传历史（成功或失败）
   * **修复：只有成功或失败时才记录，取消时不记录**
   */
  async function recordUploadHistory(task, status, errorMessage = null) {
    // **修复：取消状态不记录历史**
    if (status === 'CANCELLED' || status === 'CANCELED') {
      console.log('任务已取消，不记录历史')
      return
    }
    
    const duration = Math.floor(((task.endTime || Date.now()) - (task.startTime || Date.now())) / 1000)
    try {
      await addHistory({
        taskId: task.taskId || null,
        fileId: task.fileId,
        fileName: task.fileName,
        fileSize: task.fileSize,
        fileHash: task.fileHash || '',
        transferType: 'UPLOAD',
        transferStatus: status,
        avgSpeed: duration > 0 ? Math.floor(task.uploadedSize / duration) : 0,
        duration: duration,
        completedTime: formatDateTime(new Date()),
        errorMessage: errorMessage
      })
    } catch (error) {
      console.error('记录上传历史失败', error)
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
      downloadedChunks: [],  // 已下载的数据块（用于断点续传）
      startTime: null,
      endTime: null,
      error: null,
      retryCount: 0,  // 重试次数
      maxRetry: 3     // 最大重试次数
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
      
      // 记录成功历史
      await recordDownloadHistory(task, 'COMPLETED')
    }
  }
  
  /**
   * 下载失败
   */
  async function failDownloadTask(id, errorMessage) {
    const task = downloadQueue.value.find(t => t.id === id)
    if (task) {
      task.status = 'error'
      task.error = errorMessage
      task.endTime = Date.now()
      task.speed = 0
      
      // 记录失败历史
      await recordDownloadHistory(task, 'FAILED', errorMessage)
    }
  }
  
  /**
   * 记录下载历史（成功或失败）
   */
  async function recordDownloadHistory(task, status, errorMessage = null) {
    const duration = Math.floor(((task.endTime || Date.now()) - (task.startTime || Date.now())) / 1000)
    try {
      await addHistory({
        taskId: task.taskId || null,
        fileId: task.fileId,
        fileName: task.fileName,
        fileSize: task.fileSize,
        fileHash: task.fileHash || '',
        transferType: 'DOWNLOAD',
        transferStatus: status,
        avgSpeed: duration > 0 ? Math.floor(task.downloadedSize / duration) : 0,
        duration: duration,
        completedTime: formatDateTime(new Date()),
        errorMessage: errorMessage
      })
    } catch (error) {
      console.error('记录下载历史失败', error)
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
  
  /**
   * 重试上传任务
   */
  function retryUploadTask(id) {
    const task = uploadQueue.value.find(t => t.id === id)
    if (task && task.status === 'error') {
      task.status = 'pending'
      task.error = null
      task.retryCount = (task.retryCount || 0) + 1
      // 不重置进度，从断点继续
      return true
    }
    return false
  }
  
  /**
   * 重试下载任务
   */
  function retryDownloadTask(id) {
    const task = downloadQueue.value.find(t => t.id === id)
    if (task && task.status === 'error') {
      task.status = 'pending'
      task.error = null
      task.retryCount = (task.retryCount || 0) + 1
      // 不重置进度，从断点继续
      return true
    }
    return false
  }

  /**
   * 将后端状态映射为前端状态
   */
  function mapBackendStatusToFrontend(transferStatus) {
    if (!transferStatus) return 'pending'
    const s = transferStatus.toUpperCase()
    if (s === 'PROCESSING' || s === 'PENDING') return s === 'PROCESSING' ? 'paused' : 'pending'
    if (s === 'PAUSED') return 'paused'
    if (s === 'FAILED') return 'error'
    return 'pending'
  }

  /**
   * 从服务端恢复的未完成任务合并到上传队列
   * 按 fileId 去重：同一文件只保留一条任务，优先保留 FAILED（可重试），避免出现「一个失败 + 一个暂停」两条
   * @param {Array} list - 后端返回的 TransferTaskVO 列表
   */
  function mergeIncompleteUploadTasksFromServer(list) {
    if (!list || !Array.isArray(list)) return
    const existingTaskIds = new Set(uploadQueue.value.map(t => t.taskId).filter(Boolean))
    const existingFileIds = new Set(uploadQueue.value.map(t => t.fileId).filter(Boolean))
    // 同一 fileId 只保留一条：优先 FAILED > PAUSED > PROCESSING > PENDING
    const byFileId = new Map()
    for (const vo of list) {
      if (!vo.taskId || !vo.fileId) continue
      if (existingFileIds.has(vo.fileId)) continue
      const existing = byFileId.get(vo.fileId)
      const statusOrder = { FAILED: 0, PAUSED: 1, PROCESSING: 2, PENDING: 3 }
      const s = (vo.transferStatus || '').toUpperCase()
      const e = (existing?.transferStatus || '').toUpperCase()
      const preferThis = !existing ||
        (statusOrder[s] !== undefined && statusOrder[e] !== undefined && statusOrder[s] <= statusOrder[e])
      if (preferThis) byFileId.set(vo.fileId, vo)
    }
    // 同一逻辑文件（fileName + fileSize）只保留一条，避免后端存在多条 fileId 时出现「一个失败 + 一个暂停」
    const byFileKey = new Map()
    for (const vo of byFileId.values()) {
      const key = `${vo.fileName || ''}\0${vo.fileSize || 0}`
      const existing = byFileKey.get(key)
      const statusOrder = { FAILED: 0, PAUSED: 1, PROCESSING: 2, PENDING: 3 }
      const s = (vo.transferStatus || '').toUpperCase()
      const e = (existing?.transferStatus || '').toUpperCase()
      const preferThis = !existing ||
        (statusOrder[s] !== undefined && statusOrder[e] !== undefined && statusOrder[s] <= statusOrder[e])
      if (preferThis) byFileKey.set(key, vo)
    }
    for (const vo of byFileKey.values()) {
      if (existingTaskIds.has(vo.taskId)) continue
      existingTaskIds.add(vo.taskId)
      existingFileIds.add(vo.fileId)
      const status = mapBackendStatusToFrontend(vo.transferStatus)
      const progress = vo.progress != null ? Number(vo.progress) : 0
      const fileSize = vo.fileSize || 0
      uploadQueue.value.push({
        id: 'server-' + vo.taskId,
        file: null,
        fileName: vo.fileName || '',
        fileSize,
        folderId: 0,
        relativePath: '',
        fileId: vo.fileId,
        taskId: vo.taskId,
        fileHash: null,
        hashProgress: 0,
        status,
        progress,
        speed: 0,
        uploadedSize: Math.round((progress / 100) * fileSize),
        uploadedChunks: [],
        totalChunks: 0,
        chunkSize: 5 * 1024 * 1024,
        startTime: vo.startTime ? new Date(vo.startTime).getTime() : null,
        endTime: vo.endTime ? new Date(vo.endTime).getTime() : null,
        error: vo.errorMessage || null,
        retryCount: 0,
        maxRetry: 3,
        _fromServer: true
      })
    }
  }

  /**
   * 从服务端恢复的未完成任务合并到下载队列（按 taskId 去重）
   * @param {Array} list - 后端返回的 TransferTaskVO 列表
   */
  function mergeIncompleteDownloadTasksFromServer(list) {
    if (!list || !Array.isArray(list)) return
    const existingTaskIds = new Set(downloadQueue.value.map(t => t.taskId).filter(Boolean))
    for (const vo of list) {
      if (!vo.taskId || existingTaskIds.has(vo.taskId)) continue
      existingTaskIds.add(vo.taskId)
      const status = mapBackendStatusToFrontend(vo.transferStatus)
      const progress = vo.progress != null ? Number(vo.progress) : 0
      const fileSize = vo.fileSize || 0
      downloadQueue.value.push({
        id: 'server-' + vo.taskId,
        fileId: vo.fileId,
        fileName: vo.fileName || '',
        fileSize,
        fileHash: null,
        taskId: vo.taskId,
        status,
        progress,
        speed: 0,
        downloadedSize: Math.round((progress / 100) * fileSize),
        downloadedChunks: [],
        startTime: vo.startTime ? new Date(vo.startTime).getTime() : null,
        endTime: vo.endTime ? new Date(vo.endTime).getTime() : null,
        error: vo.errorMessage || null,
        retryCount: 0,
        maxRetry: 3,
        _fromServer: true
      })
    }
  }

  /**
   * 退出登录时清空传输队列（在调用「暂停全部」接口之后使用）
   */
  function clearAllForLogout() {
    uploadQueue.value = []
    downloadQueue.value = []
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
    failUploadTask,
    retryUploadTask,
    addDownloadTask,
    updateDownloadTask,
    removeDownloadTask,
    completeDownloadTask,
    failDownloadTask,
    retryDownloadTask,
    clearCompletedUploads,
    clearCompletedDownloads,
    pauseAllUploads,
    pauseAllDownloads,
    mergeIncompleteUploadTasksFromServer,
    mergeIncompleteDownloadTasksFromServer,
    clearAllForLogout
  }
})

