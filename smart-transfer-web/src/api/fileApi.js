import request from '@/utils/http'
import axios from 'axios'
import { userStorage } from '@/utils/storage'

/**
 * 文件传输 API
 */

/**
 * 初始化文件上传
 * @param {Object} data - 上传初始化数据
 * @returns {Promise}
 */
export function initUpload(data) {
  return request.post({
    url: '/file/upload/init',
    data
  })
}

/**
 * 上传分片（进度停滞检测 + 自动重试）
 * 不设超时，只有进度卡死30秒才重试
 * @param {FormData} formData - 分片数据
 * @param {Function} onProgress - 进度回调
 * @param {number} maxRetries - 最大重试次数
 * @param {AbortSignal} signal - 取消信号（可选，用于取消请求）
 * @returns {Promise}
 */
export async function uploadChunk(formData, onProgress, maxRetries = 3, signal) {
  const STALL_TIMEOUT = 30000 // 30秒无进度才认为卡死
  const CONNECTION_TIMEOUT = 60000 // **新增：60秒连接建立超时**

  const uploadWithStallDetection = () => {
    return new Promise((resolve, reject) => {
      const startTime = Date.now()
      let lastProgressTime = Date.now()
      let hasStartedTransfer = false  // **新增：标记是否已开始传输**
      let stallTimer = null
      let abortController = new AbortController()

      // 检测进度停滞和连接超时
      const checkStall = () => {
        const now = Date.now()
        
        // **修复：连接建立阶段超时检测（60秒）**
        if (!hasStartedTransfer && now - startTime > CONNECTION_TIMEOUT) {
          console.warn('连接建立超时（60秒），中止请求')
          abortController.abort()
          reject(new Error('CONNECTION_TIMEOUT'))
          return
        }
        
        // **修复：只有在开始传输后才检测停滞，避免连接建立阶段误判**
        if (hasStartedTransfer && now - lastProgressTime > STALL_TIMEOUT) {
          console.warn('上传停滞超过30秒，中止请求')
          abortController.abort()
          reject(new Error('STALL_TIMEOUT'))
        }
      }
      stallTimer = setInterval(checkStall, 5000)

      // 使用外部传入的 signal 或内部创建的 abortController
      const finalSignal = signal || abortController.signal

      request.post({
        url: '/file/upload/chunk',
        data: formData,
        timeout: 0, // 不设超时，但axios可能仍使用实例默认值
        signal: finalSignal,  // **修复CRITICAL-4: 支持外部传入的signal**
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        // 明确设置超时为0（或很大的值）以覆盖实例默认值
        validateStatus: () => true, // 允许所有状态码，避免被拦截器拦截
        onUploadProgress: progressEvent => {
          hasStartedTransfer = true  // **标记已开始传输**
          lastProgressTime = Date.now() // 有进度就更新时间
          if (onProgress) {
            const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(percent)
          }
        }
      })
      .then(res => {
        clearInterval(stallTimer)
        resolve(res)
      })
      .catch(err => {
        clearInterval(stallTimer)
        // 处理不同类型的错误
        if (err.code === 'ECONNABORTED' || err.message?.includes('timeout') || err.message?.includes('TIMED_OUT')) {
          reject(new Error('UPLOAD_TIMEOUT'))
        } else if (err.name === 'AbortError' || err.message === 'STALL_TIMEOUT') {
          reject(new Error('STALL_TIMEOUT'))
        } else {
          reject(err)
        }
      })
    })
  }

  let lastError = null
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await uploadWithStallDetection()
    } catch (error) {
      lastError = error
      const isStall = error.message === 'STALL_TIMEOUT'
      const isConnectionTimeout = error.message === 'CONNECTION_TIMEOUT'
      const isTimeout = error.message === 'UPLOAD_TIMEOUT' || error.code === 'ECONNABORTED'
      const isNetworkError = error.message === 'Network Error' || error.code === 'ERR_NETWORK'
      
      // **修复：增加重试条件，包括网络错误**
      const shouldRetry = isStall || isTimeout || isConnectionTimeout || isNetworkError
      
      if (attempt < maxRetries && shouldRetry) {
        const delay = Math.pow(2, attempt) * 1000
        let errorType = '未知错误'
        if (isStall) errorType = '停滞'
        else if (isConnectionTimeout) errorType = '连接超时'
        else if (isTimeout) errorType = '传输超时'
        else if (isNetworkError) errorType = '网络错误'
        
        console.warn(`分片上传${errorType}，${delay/1000}秒后重试 (${attempt + 1}/${maxRetries})`)
        // 分片上传重试
        await new Promise(resolve => setTimeout(resolve, delay))
      } else if (!shouldRetry) {
        // 非可重试错误，直接抛出
        throw error
      }
    }
  }

  throw lastError
}

/**
 * 合并文件（无超时，服务端处理可能很久）
 * @param {Object} data - 合并请求数据
 * @returns {Promise}
 */
export function mergeFile(data) {
  return request.post({
    url: '/file/merge',
    data,
    timeout: 0 // 不设超时，大文件合并可能需要很长时间
  })
}

/**
 * 取消上传
 * 清理未完成的上传数据
 * @param {Number} fileId - 文件ID
 * @returns {Promise}
 */
export function cancelUpload(fileId) {
  return request.del({
    url: `/file/upload/${fileId}`
  })
}

/**
 * 更新任务状态
 * @param {String} taskId - 任务ID
 * @param {String} status - 新状态（FAILED/CANCELLED）
 * @returns {Promise}
 */
export function updateTaskStatus(taskId, status) {
  return request.put({
    url: `/file/task/${taskId}/status`,
    params: { status }
  })
}

/**
 * 获取文件列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getFileList(params) {
  return request.get({
    url: '/file/list',
    params
  })
}

/**
 * 获取文件详情
 * @param {Number} id - 文件ID
 * @returns {Promise}
 */
export function getFileInfo(id) {
  return request.get({
    url: `/file/${id}`
  })
}

/**
 * 初始化文件下载（分块下载）
 * @param {Number} fileId - 文件ID
 * @param {Number} chunkSize - 分块大小（可选，默认5MB）
 * @returns {Promise}
 */
export function initDownload(fileId, chunkSize) {
  return request.get({
    url: `/file/download/init/${fileId}`,
    params: chunkSize ? { chunkSize } : {}
  })
}

/**
 * 下载文件分块（集成拥塞控制，二进制流传输）
 * **优化：使用二进制流传输（标准做法），元数据通过响应头传输**
 * @param {Number} fileId - 文件ID
 * @param {Number} chunkNumber - 分块编号
 * @param {Number} startByte - 起始字节位置（可选）
 * @param {Number} endByte - 结束字节位置（可选）
 * @param {AbortSignal} signal - 取消信号（可选，用于取消请求）
 * @returns {Promise} 返回完整的axios响应对象（包含响应头和二进制数据）
 */
export function downloadChunk(fileId, chunkNumber, startByte, endByte, signal) {
  const params = {}
  if (startByte !== undefined) params.startByte = startByte
  if (endByte !== undefined) params.endByte = endByte
  
  // 使用axios直接调用，接收二进制数据
  // 注意：需要绕过request封装，直接使用axios实例（因为需要responseType: 'arraybuffer'）
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = userStorage.getToken()
  
  // 创建独立的axios实例用于二进制下载（不受响应拦截器影响）
  const axiosInstance = axios.create({
    baseURL,
    timeout: 0,  // 不设超时
    responseType: 'arraybuffer',  // 接收二进制数据
    headers: {
      'Authorization': token ? `Bearer ${token}` : ''
    }
  })
  
  // **修复P2/P3: 支持请求取消（通过AbortSignal）**
  return axiosInstance.get(`/file/download/chunk/${fileId}/${chunkNumber}`, { 
    params,
    signal  // 传递取消信号
  })
}

/**
 * 获取文件下载URL（兼容旧接口）
 * @param {Number} id - 文件ID
 * @returns {String} - 下载URL
 */
export function getDownloadUrl(id) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseURL}/file/download/${id}`
}

/**
 * 获取文件预览URL
 * @param {Number} id - 文件ID
 * @returns {String} - 预览URL
 */
export function getPreviewUrl(id) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseURL}/file/preview/${id}`
}

/**
 * 删除文件
 * @param {Number} id - 文件ID
 * @returns {Promise}
 */
export function deleteFile(id) {
  return request.del({
    url: `/file/${id}`
  })
}

/**
 * 批量删除文件
 * @param {Array<Number>} ids - 文件ID列表
 * @returns {Promise}
 */
export function batchDeleteFiles(ids) {
  return request.del({
    url: '/file/batch',
    data: ids
  })
}

/**
 * 搜索文件
 * @param {Object} params - 搜索参数
 * @returns {Promise}
 */
export function searchFile(params) {
  return request.get({
    url: '/file/search',
    params
  })
}

/**
 * 重命名文件
 * @param {Object} data - 包含id和fileName
 * @returns {Promise}
 */
export function renameFile(data) {
  return request.put({
    url: '/file/rename',
    data
  })
}

/**
 * 移动文件
 * @param {Object} data - 包含文件ID和目标文件夹ID
 * @returns {Promise}
 */
export function moveFile(data) {
  return request.put({
    url: '/file/move',
    data
  })
}

/**
 * 批量移动文件
 * @param {Object} data - 包含文件ID列表和目标文件夹ID
 * @returns {Promise}
 */
export function batchMoveFiles(data) {
  return request.put({
    url: '/file/move/batch',
    data
  })
}

/**
 * 查询传输任务
 * @param {String} taskId - 任务ID
 * @returns {Promise}
 */
export function getTask(taskId) {
  return request.get({
    url: `/file/task/${taskId}`
  })
}

/**
 * 查询任务列表
 * @param {Object} data - 查询条件
 * @returns {Promise}
 */
export function getTaskList(data) {
  return request.post({
    url: '/file/task/list',
    data
  })
}

/**
 * 删除任务
 * @param {String} taskId - 任务ID
 * @returns {Promise}
 */
export function deleteTask(taskId) {
  return request.del({
    url: `/file/task/${taskId}`
  })
}

/**
 * 查询当前用户未完成的传输任务（用于刷新/重进传输中心后恢复列表）
 * @param {String} taskType - UPLOAD | DOWNLOAD
 * @returns {Promise<{ data: Array }>}
 */
export function getIncompleteTasks(taskType) {
  return request.get({
    url: '/file/task/incomplete',
    params: { taskType }
  })
}

/**
 * 暂停当前用户所有进行中/待处理任务（退出登录时调用）
 * @returns {Promise<{ data: number }>}
 */
export function pauseAllTasks() {
  return request.post({
    url: '/file/task/pause-all'
  })
}

/**
 * 复制文件
 * @param {Object} data - 包含文件ID和目标文件夹ID
 * @returns {Promise}
 */
export function copyFile(data) {
  return request.post({
    url: '/file/copy',
    data
  })
}

/**
 * 批量复制文件
 * @param {Object} data - 包含文件ID列表和目标文件夹ID
 * @returns {Promise}
 */
export function batchCopyFiles(data) {
  return request.post({
    url: '/file/copy/batch',
    data
  })
}

/**
 * 解压文件
 * @param {Object} data - 解压参数
 * @returns {Promise}
 */
export function unzipFile(data) {
  return request.post({
    url: '/file/unzip',
    data,
    timeout: 0 // 解压可能需要较长时间
  })
}

/**
 * 获取批量下载URL
 * @param {Array<Number>} ids - 文件ID列表
 * @returns {String} - 批量下载URL
 */
export function getBatchDownloadUrl(ids) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseURL}/file/download/batch?ids=${ids.join(',')}`
}

/**
 * 标记下载任务完成
 * 清理后端资源（包括算法实例和Redis数据）
 * @param {String} taskId - 任务ID
 * @returns {Promise}
 */
export function completeDownload(taskId) {
  return request.post({
    url: `/file/download/complete/${taskId}`
  })
}

/**
 * 取消下载任务
 * 清理后端资源（包括算法实例和Redis数据）
 * @param {String} taskId - 任务ID
 * @returns {Promise}
 */
export function cancelDownload(taskId) {
  return request.del({
    url: `/file/download/${taskId}`
  })
}
