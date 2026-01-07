/**
 * 文件类型工具函数
 */

// 文件类型分类
const FILE_TYPES = {
  image: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico', 'tiff'],
  video: ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv', 'webm', 'mpeg', 'm4v'],
  audio: ['mp3', 'wav', 'ogg', 'flac', 'm4a', 'aac', 'wma', 'ape'],
  document: ['doc', 'docx', 'pdf', 'txt', 'rtf', 'odt'],
  spreadsheet: ['xls', 'xlsx', 'csv', 'ods'],
  presentation: ['ppt', 'pptx', 'odp'],
  archive: ['zip', 'rar', '7z', 'tar', 'gz', 'bz2'],
  code: ['js', 'ts', 'vue', 'jsx', 'tsx', 'java', 'py', 'c', 'cpp', 'h', 'css', 'scss', 'less', 'html', 'xml', 'json', 'yaml', 'yml', 'sql', 'sh', 'bat', 'md'],
  executable: ['exe', 'msi', 'dmg', 'app', 'apk', 'deb', 'rpm']
}

/**
 * 根据扩展名获取文件类型
 * @param {string} extendName 文件扩展名
 * @returns {string} 文件类型
 */
export function getFileType(extendName) {
  if (!extendName) return 'other'
  
  const ext = extendName.toLowerCase()
  
  for (const [type, extensions] of Object.entries(FILE_TYPES)) {
    if (extensions.includes(ext)) {
      return type
    }
  }
  
  return 'other'
}

/**
 * 根据扩展名获取文件图标路径
 * @param {string} extendName 文件扩展名
 * @returns {string} 图标路径
 */
export function getFileIconByType(extendName) {
  const type = getFileType(extendName)
  
  const iconMap = {
    image: '/icons/image.svg',
    video: '/icons/video.svg',
    audio: '/icons/audio.svg',
    document: '/icons/document.svg',
    spreadsheet: '/icons/excel.svg',
    presentation: '/icons/ppt.svg',
    archive: '/icons/zip.svg',
    code: '/icons/code.svg',
    executable: '/icons/file.svg',
    other: '/icons/file.svg'
  }
  
  // 特殊文件类型
  const ext = (extendName || '').toLowerCase()
  if (ext === 'pdf') return '/icons/pdf.svg'
  if (ext === 'txt') return '/icons/txt.svg'
  if (ext === 'md') return '/icons/markdown.svg'
  if (['doc', 'docx'].includes(ext)) return '/icons/word.svg'
  
  return iconMap[type] || '/icons/file.svg'
}

/**
 * 判断文件是否可预览
 * @param {string} extendName 文件扩展名
 * @returns {boolean} 是否可预览
 */
export function canPreviewFile(extendName) {
  const type = getFileType(extendName)
  const ext = (extendName || '').toLowerCase()
  
  // 图片、视频、音频可预览
  if (['image', 'video', 'audio'].includes(type)) return true
  
  // PDF可预览
  if (ext === 'pdf') return true
  
  // 文本文件可预览
  const textExts = ['txt', 'md', 'json', 'xml', 'html', 'css', 'js', 'ts', 'vue', 'java', 'py', 'sql']
  if (textExts.includes(ext)) return true
  
  return false
}

/**
 * 判断是否为图片文件
 * @param {string} extendName 文件扩展名
 * @returns {boolean}
 */
export function isImageFile(extendName) {
  return getFileType(extendName) === 'image'
}

/**
 * 判断是否为视频文件
 * @param {string} extendName 文件扩展名
 * @returns {boolean}
 */
export function isVideoFile(extendName) {
  return getFileType(extendName) === 'video'
}

/**
 * 判断是否为音频文件
 * @param {string} extendName 文件扩展名
 * @returns {boolean}
 */
export function isAudioFile(extendName) {
  return getFileType(extendName) === 'audio'
}

/**
 * 获取文件类型名称（中文）
 * @param {string} extendName 文件扩展名
 * @returns {string} 类型名称
 */
export function getFileTypeName(extendName) {
  const type = getFileType(extendName)
  
  const nameMap = {
    image: '图片',
    video: '视频',
    audio: '音频',
    document: '文档',
    spreadsheet: '表格',
    presentation: '演示文稿',
    archive: '压缩包',
    code: '代码',
    executable: '可执行文件',
    other: '其他'
  }
  
  return nameMap[type] || '其他'
}
