import request from '@/utils/http'

/**
 * 文件夹管理API
 */

/**
 * 创建文件夹
 */
export function createFolder(data) {
  return request.post({
    url: '/folder/create',
    data
  })
}

/**
 * 获取文件夹内容（文件夹+文件列表）
 */
export function getFolderContent(params) {
  return request.get({
    url: '/folder/content',
    params
  })
}

/**
 * 获取文件夹列表
 */
export function getFolderList(params) {
  return request.get({
    url: '/folder/list',
    params
  })
}

/**
 * 获取面包屑路径
 */
export function getBreadcrumb(folderId) {
  return request.get({
    url: '/folder/breadcrumb',
    params: { folderId }
  })
}

/**
 * 重命名文件夹
 */
export function renameFolder(data) {
  return request.put({
    url: '/folder/rename',
    data
  })
}

/**
 * 删除文件夹
 */
export function deleteFolder(folderId) {
  return request.del({
    url: `/folder/${folderId}`
  })
}

/**
 * 移动文件到文件夹
 */
export function moveFileToFolder(data) {
  return request.post({
    url: '/folder/move/file',
    data
  })
}

/**
 * 移动文件夹
 */
export function moveFolderTo(data) {
  return request.post({
    url: '/folder/move/folder',
    data
  })
}

