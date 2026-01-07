import request from '@/utils/http'

/**
 * 回收站文件API服务
 */

/**
 * 获取回收站文件列表
 * @returns {Promise}
 */
export function getRecoveryFileList() {
  return request.get({
    url: '/recovery/list'
  })
}

/**
 * 还原回收站文件
 * @param {number} recoveryFileId - 回收站记录ID
 * @returns {Promise}
 */
export function restoreRecoveryFile(recoveryFileId) {
  return request.post({
    url: `/recovery/restore/${recoveryFileId}`
  })
}

/**
 * 批量还原回收站文件
 * @param {Array<number>} recoveryFileIds - 回收站记录ID列表
 * @returns {Promise}
 */
export function batchRestoreRecoveryFiles(recoveryFileIds) {
  return request.post({
    url: '/recovery/restore/batch',
    data: { ids: recoveryFileIds }
  })
}

/**
 * 彻底删除回收站文件
 * @param {number} recoveryFileId - 回收站记录ID
 * @returns {Promise}
 */
export function deleteRecoveryFile(recoveryFileId) {
  return request.del({
    url: `/recovery/${recoveryFileId}`
  })
}

/**
 * 批量彻底删除回收站文件
 * @param {Array<number>} recoveryFileIds - 回收站记录ID列表
 * @returns {Promise}
 */
export function batchDeleteRecoveryFiles(recoveryFileIds) {
  return request.del({
    url: '/recovery/batch',
    data: { ids: recoveryFileIds }
  })
}

/**
 * 清空回收站
 * @returns {Promise}
 */
export function clearAllRecoveryFiles() {
  return request.del({
    url: '/recovery/clear'
  })
}
