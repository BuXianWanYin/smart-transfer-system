import SparkMD5 from 'spark-md5'

/**
 * 文件工具函数
 */

/**
 * 创建文件分片
 * @param {File} file - 文件对象
 * @param {Number} chunkSize - 分片大小（字节）
 * @returns {Array} - 分片数组
 */
export function createFileChunks(file, chunkSize = 2 * 1024 * 1024) {
  const chunks = []
  let cur = 0
  
  while (cur < file.size) {
    chunks.push({
      file: file.slice(cur, cur + chunkSize),
      size: Math.min(chunkSize, file.size - cur),
      index: chunks.length
    })
    cur += chunkSize
  }
  
  return chunks
}

/**
 * 计算文件MD5哈希
 * @param {File} file - 文件对象
 * @param {Function} onProgress - 进度回调
 * @returns {Promise<String>} - MD5值
 */
export function calculateFileHash(file, onProgress) {
  return new Promise((resolve, reject) => {
    const chunkSize = 2 * 1024 * 1024 // 2MB
    const chunks = Math.ceil(file.size / chunkSize)
    let currentChunk = 0
    const spark = new SparkMD5.ArrayBuffer()
    const fileReader = new FileReader()

    fileReader.onload = e => {
      spark.append(e.target.result)
      currentChunk++

      if (onProgress) {
        onProgress(Math.floor((currentChunk / chunks) * 100))
      }

      if (currentChunk < chunks) {
        loadNext()
      } else {
        const hash = spark.end()
        resolve(hash)
      }
    }

    fileReader.onerror = () => {
      reject(new Error('文件读取失败'))
    }

    function loadNext() {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      fileReader.readAsArrayBuffer(file.slice(start, end))
    }

    loadNext()
  })
}

/**
 * 计算分片MD5哈希
 * @param {Blob} chunk - 分片对象
 * @returns {Promise<String>} - MD5值
 */
export function calculateChunkHash(chunk) {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader()
    const spark = new SparkMD5.ArrayBuffer()

    fileReader.onload = e => {
      spark.append(e.target.result)
      resolve(spark.end())
    }

    fileReader.onerror = () => {
      reject(new Error('分片读取失败'))
    }

    fileReader.readAsArrayBuffer(chunk)
  })
}

/**
 * 格式化文件大小
 * @param {Number} bytes - 字节数
 * @returns {String} - 格式化后的字符串
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 格式化传输速率
 * @param {Number} bytesPerSecond - 字节/秒
 * @returns {String} - 格式化后的字符串
 */
export function formatSpeed(bytesPerSecond) {
  return formatFileSize(bytesPerSecond) + '/s'
}

/**
 * 格式化时长
 * @param {Number} seconds - 秒数
 * @returns {String} - 格式化后的字符串
 */
export function formatDuration(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  
  if (h > 0) {
    return `${h}小时${m}分${s}秒`
  } else if (m > 0) {
    return `${m}分${s}秒`
  } else {
    return `${s}秒`
  }
}

/**
 * 格式化进度百分比
 * @param {Number} current - 当前值
 * @param {Number} total - 总值
 * @returns {Number} - 百分比
 */
export function formatProgress(current, total) {
  if (total === 0) return 0
  return Math.floor((current / total) * 100)
}

/**
 * 获取文件扩展名
 * @param {String} filename - 文件名
 * @returns {String} - 扩展名
 */
export function getFileExtension(filename) {
  const index = filename.lastIndexOf('.')
  return index > -1 ? filename.substring(index + 1).toLowerCase() : ''
}

/**
 * 获取文件图标类型
 * @param {String} filename - 文件名
 * @returns {String} - 图标类型
 */
export function getFileIconType(filename) {
  const ext = getFileExtension(filename)
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp']
  const videoExts = ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv']
  const audioExts = ['mp3', 'wav', 'flac', 'aac', 'm4a']
  const docExts = ['doc', 'docx', 'pdf', 'txt', 'md']
  const codeExts = ['js', 'java', 'py', 'cpp', 'c', 'html', 'css', 'vue']
  const archiveExts = ['zip', 'rar', '7z', 'tar', 'gz']
  
  if (imageExts.includes(ext)) return 'image'
  if (videoExts.includes(ext)) return 'video'
  if (audioExts.includes(ext)) return 'audio'
  if (docExts.includes(ext)) return 'document'
  if (codeExts.includes(ext)) return 'code'
  if (archiveExts.includes(ext)) return 'archive'
  
  return 'file'
}

