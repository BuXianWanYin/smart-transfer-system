import request from '@/utils/http'

/**
 * 文件夹管理API
 */

/**
 * 创建文件夹
 * @param {Object} data - 包含 folderName 和 parentId
 * @returns {Promise}
 */
export function createFolder(data) {
  return request.post({
    url: '/folder/create',
    data
  })
}

/**
 * 获取文件夹内容（文件夹+文件列表）
 * @param {Object} params - 查询参数
 * @param {number} params.parentId - 父文件夹ID
 * @param {number} params.fileType - 文件类型筛选
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise}
 */
export function getFolderContent(params) {
  return request.get({
    url: '/folder/content',
    params
  })
}

/**
 * 获取文件夹列表
 * @param {Object} params - 查询参数
 * @param {number} params.parentId - 父文件夹ID
 * @returns {Promise}
 */
export function getFolderList(params) {
  return request.get({
    url: '/folder/list',
    params
  })
}

/**
 * 获取面包屑路径
 * @param {number} folderId - 文件夹ID
 * @returns {Promise}
 */
export function getBreadcrumb(folderId) {
  return request.get({
    url: '/folder/breadcrumb',
    params: { folderId }
  })
}

/**
 * 重命名文件夹
 * @param {Object} data - 包含 id 和 folderName
 * @returns {Promise}
 */
export function renameFolder(data) {
  return request.put({
    url: '/folder/rename',
    data
  })
}

/**
 * 删除文件夹
 * @param {number} folderId - 文件夹ID
 * @returns {Promise}
 */
export function deleteFolder(folderId) {
  return request.del({
    url: `/folder/${folderId}`
  })
}

/**
 * 移动文件到文件夹
 * @param {Object} data - 包含 fileId 和 targetFolderId
 * @returns {Promise}
 */
export function moveFileToFolder(data) {
  return request.post({
    url: '/folder/move/file',
    data
  })
}

/**
 * 移动文件夹
 * @param {Object} data - 包含 folderId 和 targetFolderId
 * @returns {Promise}
 */
export function moveFolderTo(data) {
  return request.post({
    url: '/folder/move/folder',
    data
  })
}
