import request from '@/utils/http'

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
 * @returns {Promise}
 */
export async function uploadChunk(formData, onProgress, maxRetries = 3) {
  const STALL_TIMEOUT = 30000 // 30秒无进度才认为卡死

  const uploadWithStallDetection = () => {
    return new Promise((resolve, reject) => {
      let lastProgressTime = Date.now()
      let stallTimer = null
      let abortController = new AbortController()

      // 检测进度停滞
      const checkStall = () => {
        if (Date.now() - lastProgressTime > STALL_TIMEOUT) {
          abortController.abort()
          reject(new Error('STALL_TIMEOUT'))
        }
      }
      stallTimer = setInterval(checkStall, 5000)

      request.post({
        url: '/file/upload/chunk',
        data: formData,
        timeout: 0, // 不设超时
        signal: abortController.signal,
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: progressEvent => {
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
        reject(err)
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
      if (attempt < maxRetries) {
        const delay = Math.pow(2, attempt) * 1000
        // 分片上传重试
        await new Promise(resolve => setTimeout(resolve, delay))
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
 * 获取文件下载URL
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
    url: '/file/batchDelete',
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
