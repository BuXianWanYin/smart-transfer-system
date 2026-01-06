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
 * 上传分片
 * @param {FormData} formData - 分片数据
 * @param {Function} onProgress - 进度回调
 * @returns {Promise}
 */
export function uploadChunk(formData, onProgress) {
  return request.post({
    url: '/file/upload/chunk',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: progressEvent => {
      if (onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

/**
 * 合并文件
 * @param {Object} data - 合并请求数据
 * @returns {Promise}
 */
export function mergeFile(data) {
  return request.post({
    url: '/file/merge',
    data
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
 * 下载文件
 * @param {Number} id - 文件ID
 * @returns {String} - 下载URL
 */
export function getDownloadUrl(id) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseURL}/file/download/${id}`
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

