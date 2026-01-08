<template>
  <el-dialog
    v-model="dialogVisible"
    title="上传文件"
    width="650px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="chunk-uploader">
      <!-- 上传方式选择 -->
      <div class="upload-actions">
        <el-button type="primary" @click="selectFiles">
          <el-icon><Document /></el-icon>
          选择文件
        </el-button>
        <el-button @click="selectFolder">
          <el-icon><Folder /></el-icon>
          选择文件夹
        </el-button>
        <el-button @click="showDragUpload">
          <el-icon><UploadFilled /></el-icon>
          拖拽上传
        </el-button>
      </div>
      
      <!-- 隐藏的文件选择器 -->
      <input
        ref="fileInputRef"
        type="file"
        multiple
        style="display: none"
        @change="handleFileSelect"
      />
      <input
        ref="folderInputRef"
        type="file"
        webkitdirectory
        directory
        multiple
        style="display: none"
        @change="handleFolderSelect"
      />
      
      <!-- 上传区域 -->
      <div
        class="upload-area"
        @dragover.prevent="handleDragOver"
        @dragleave.prevent="handleDragLeave"
        @drop.prevent="handleDrop"
        :class="{ 'drag-over': isDragOver }"
      >
        <div class="upload-content" v-if="fileList.length === 0">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p class="main-text">点击上方按钮或拖拽文件到此处上传</p>
            <p class="sub-text">支持大文件分片上传、断点续传、秒传</p>
          </div>
        </div>
        
        <!-- 文件列表 -->
        <div v-else class="file-list">
          <div class="list-header">
            <span>待上传文件 ({{ fileList.length }})</span>
            <el-button size="small" text type="danger" @click="clearAllFiles">
              清空
            </el-button>
          </div>
          
          <div class="list-body">
            <div
              v-for="item in fileList"
              :key="item.id"
              class="file-item"
            >
              <div class="file-info">
                <el-icon class="file-icon"><Document /></el-icon>
                <div class="file-details">
                  <span class="file-name" :title="item.file.name">{{ item.file.name }}</span>
                  <span class="file-size">{{ formatSize(item.file.size) }}</span>
                  <span class="file-path" v-if="item.relativePath">{{ item.relativePath }}</span>
                </div>
              </div>
              
              <div class="file-status">
                <!-- 计算哈希 -->
                <template v-if="item.status === 'hashing'">
                  <span class="status-text">计算哈希: {{ item.hashProgress }}%</span>
                  <el-progress :percentage="item.hashProgress" :show-text="false" />
                </template>
                
                <!-- 秒传成功 -->
                <template v-else-if="item.status === 'quick'">
                  <span class="status-text success">
                    <el-icon><SuccessFilled /></el-icon>
                    秒传成功
                  </span>
                </template>
                
                <!-- 上传中 -->
                <template v-else-if="item.status === 'uploading'">
                  <span class="status-text">上传中: {{ item.progress }}%</span>
                  <el-progress :percentage="item.progress" :show-text="false" />
                  <span class="speed-text">{{ formatSpeed(item.speed) }}</span>
                </template>
                
                <!-- 暂停 -->
                <template v-else-if="item.status === 'paused'">
                  <span class="status-text warning">已暂停 ({{ item.progress }}%)</span>
                  <el-progress :percentage="item.progress" status="warning" :show-text="false" />
                </template>
                
                <!-- 完成 -->
                <template v-else-if="item.status === 'completed'">
                  <span class="status-text success">
                    <el-icon><SuccessFilled /></el-icon>
                    上传完成
                  </span>
                </template>
                
                <!-- 错误 -->
                <template v-else-if="item.status === 'error'">
                  <span class="status-text danger">
                    <el-icon><CircleCloseFilled /></el-icon>
                    {{ item.error || '上传失败' }}
                  </span>
                </template>
                
                <!-- 等待中 -->
                <template v-else>
                  <span class="status-text">等待上传</span>
                </template>
              </div>
              
              <div class="file-actions">
                <el-button
                  v-if="item.status === 'paused'"
                  size="small"
                  type="primary"
                  @click="resumeUpload(item)"
                >
                  继续
                </el-button>
                <el-button
                  v-if="item.status === 'uploading'"
                  size="small"
                  @click="pauseUpload(item)"
                >
                  暂停
                </el-button>
                <el-button
                  v-if="item.status === 'error'"
                  size="small"
                  type="warning"
                  @click="retryUpload(item)"
                >
                  重试
                </el-button>
                <el-button
                  v-if="!['completed', 'quick'].includes(item.status)"
                  size="small"
                  type="danger"
                  @click="removeFile(item.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button
        type="primary"
        :disabled="!canUpload"
        :loading="isUploading"
        @click="startUploadAll"
      >
        {{ isUploading ? '上传中...' : '开始上传' }}
      </el-button>
    </template>
  </el-dialog>
  
  <!-- 全屏拖拽上传遮罩 -->
  <UploadMask ref="uploadMaskRef" @upload="handleMaskUpload" />
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, Folder, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import SparkMD5 from 'spark-md5'
import { initUpload, uploadChunk, mergeFile } from '@/api/fileApi'
import UploadMask from './UploadMask.vue'
import { useTransferStore } from '@/store/transferStore'

const transferStore = useTransferStore()

const props = defineProps({
  folderId: { type: Number, default: 0 }
})

const emit = defineEmits(['close', 'uploaded'])

const dialogVisible = ref(true)
const fileInputRef = ref(null)
const folderInputRef = ref(null)
const uploadMaskRef = ref(null)
const fileList = ref([])
const isUploading = ref(false)
const isDragOver = ref(false)

// 分片大小 5MB
const CHUNK_SIZE = 5 * 1024 * 1024

// 是否可以上传
const canUpload = computed(() => {
  return fileList.value.some(item => 
    ['pending', 'paused', 'error'].includes(item.status)
  )
})

// 格式化文件大小
const formatSize = (size) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 格式化速度
const formatSpeed = (speed) => {
  if (!speed) return '0 B/s'
  return formatSize(speed) + '/s'
}

// 选择文件
const selectFiles = () => {
  fileInputRef.value?.click()
}

// 选择文件夹
const selectFolder = () => {
  folderInputRef.value?.click()
}

// 显示拖拽上传
const showDragUpload = () => {
  uploadMaskRef.value?.show()
}

// 处理文件选择
const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  addFiles(files)
  event.target.value = ''
}

// 处理文件夹选择
const handleFolderSelect = (event) => {
  const files = Array.from(event.target.files)
  addFiles(files, true)
  event.target.value = ''
}

// 处理拖拽
const handleDragOver = () => {
  isDragOver.value = true
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const handleDrop = (event) => {
  isDragOver.value = false
  const items = event.dataTransfer.items
  
  if (items) {
    const filePromises = []
    
    for (const item of items) {
      if (item.kind === 'file') {
        const entry = item.webkitGetAsEntry?.()
        if (entry) {
          if (entry.isDirectory) {
            filePromises.push(traverseDirectory(entry))
          } else {
            filePromises.push(getFileFromEntry(entry))
          }
        } else {
          const file = item.getAsFile()
          if (file) filePromises.push(Promise.resolve([{ file, relativePath: '' }]))
        }
      }
    }
    
    Promise.all(filePromises).then(results => {
      const allFiles = results.flat()
      addFilesWithPath(allFiles)
    })
  }
}

// 遍历文件夹
const traverseDirectory = async (dirEntry, path = '') => {
  const files = []
  const reader = dirEntry.createReader()
  
  const readEntries = () => {
    return new Promise((resolve) => {
      reader.readEntries(async (entries) => {
        if (entries.length === 0) {
          resolve()
          return
        }
        
        for (const entry of entries) {
          const relativePath = path ? `${path}/${entry.name}` : entry.name
          
          if (entry.isDirectory) {
            const subFiles = await traverseDirectory(entry, relativePath)
            files.push(...subFiles)
          } else {
            const fileData = await getFileFromEntry(entry, relativePath)
            files.push(...fileData)
          }
        }
        
        // 继续读取（目录可能有多批文件）
        const more = await readEntries()
        resolve(more)
      })
    })
  }
  
  await readEntries()
  return files
}

// 从 entry 获取文件
const getFileFromEntry = (entry, relativePath = '') => {
  return new Promise((resolve) => {
    entry.file((file) => {
      resolve([{ file, relativePath: relativePath || entry.name }])
    }, () => {
      resolve([])
    })
  })
}

// 处理全屏遮罩上传
const handleMaskUpload = (files) => {
  addFiles(files)
}

// 添加文件（带路径）
const addFilesWithPath = (filesWithPath) => {
  for (const { file, relativePath } of filesWithPath) {
    const exists = fileList.value.some(item => 
      item.file.name === file.name && item.file.size === file.size
    )
    if (exists) {
      continue
    }
    
    fileList.value.push({
      id: Date.now() + Math.random(),
      file: file,
      relativePath: relativePath,
      status: 'pending',
      progress: 0,
      hashProgress: 0,
      speed: 0,
      hash: '',
      fileId: '',
      uploadedChunks: [],
      error: '',
      abortController: null
    })
  }
  
  if (filesWithPath.length > 0) {
    ElMessage.success(`已添加 ${filesWithPath.length} 个文件`)
  }
}

// 添加文件
const addFiles = (files, isFolder = false) => {
  let addedCount = 0
  
  for (const file of files) {
    const exists = fileList.value.some(item => 
      item.file.name === file.name && item.file.size === file.size
    )
    if (exists) {
      continue
    }
    
    fileList.value.push({
      id: Date.now() + Math.random(),
      file: file,
      relativePath: isFolder ? file.webkitRelativePath : '',
      status: 'pending',
      progress: 0,
      hashProgress: 0,
      speed: 0,
      hash: '',
      fileId: '',
      uploadedChunks: [],
      error: '',
      abortController: null
    })
    addedCount++
  }
  
  if (addedCount > 0) {
    ElMessage.success(`已添加 ${addedCount} 个文件`)
  }
}

// 计算文件 MD5 哈希
const computeFileHash = async (item) => {
  return new Promise((resolve, reject) => {
    const file = item.file
    const chunkSize = CHUNK_SIZE
    const chunks = Math.ceil(file.size / chunkSize)
    let currentChunk = 0
    const spark = new SparkMD5.ArrayBuffer()
    const fileReader = new FileReader()

    fileReader.onload = (e) => {
      spark.append(e.target.result)
      currentChunk++
      item.hashProgress = Math.floor((currentChunk / chunks) * 100)

      if (currentChunk < chunks) {
        loadNext()
      } else {
        resolve(spark.end())
      }
    }

    fileReader.onerror = () => {
      reject(new Error('文件读取失败'))
    }

    const loadNext = () => {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      fileReader.readAsArrayBuffer(file.slice(start, end))
    }

    loadNext()
  })
}

// 上传单个文件
const uploadFile = async (item) => {
  try {
    item.status = 'hashing'
    item.hashProgress = 0
    
    // 同步到 transferStore（添加或更新）
    if (!item.storeTaskId) {
      const storeTask = transferStore.addUploadTask({
        id: item.id,
        file: item.file,
        fileName: item.file.name,
        fileSize: item.file.size
      })
      item.storeTaskId = storeTask.id
    }
    transferStore.updateUploadTask(item.storeTaskId, { status: 'hashing' })
    
    // 计算文件哈希
    const hash = await computeFileHash(item)
    item.hash = hash
    
    // 计算分片数量
    const totalChunks = Math.ceil(item.file.size / CHUNK_SIZE)
    
    // 初始化上传
    const initRes = await initUpload({
      fileName: item.file.name,
      fileSize: item.file.size,
      fileHash: hash,
      chunkSize: CHUNK_SIZE,
      totalChunks: totalChunks,
      folderId: props.folderId,
      relativePath: item.relativePath
    })
    
    // 秒传成功
    if (initRes.data?.quickUpload) {
      item.status = 'quick'
      item.progress = 100
      transferStore.updateUploadTask(item.storeTaskId, { 
        status: 'completed', 
        progress: 100,
        fileHash: hash
      })
      ElMessage.success(`${item.file.name} 秒传成功`)
      emit('uploaded')
      return
    }
    
    item.fileId = initRes.data?.fileId || initRes.fileId
    item.uploadedChunks = initRes.data?.uploadedChunks || []
    transferStore.updateUploadTask(item.storeTaskId, { fileId: item.fileId })
    
    // 开始分片上传
    await uploadChunks(item)
    
    // 合并文件
    await mergeFile({
      fileId: item.fileId,
      fileHash: hash
    })
    
    item.status = 'completed'
    item.progress = 100
    // 完成上传任务
    await transferStore.completeUploadTask(item.storeTaskId, hash)
    ElMessage.success(`${item.file.name} 上传成功`)
    emit('uploaded')
    
  } catch (error) {
    if (error.name === 'AbortError' || item.status === 'paused') {
      transferStore.updateUploadTask(item.storeTaskId, { status: 'paused', speed: 0 })
      return
    }
    item.status = 'error'
    item.error = error.message || '上传失败'
    transferStore.updateUploadTask(item.storeTaskId, { 
      status: 'error', 
      error: item.error,
      speed: 0
    })
    ElMessage.error(`${item.file.name} 上传失败: ${item.error}`)
  }
}

// 上传分片
const uploadChunks = async (item) => {
  const file = item.file
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  const startTime = Date.now()
  let uploadedSize = 0
  
  item.status = 'uploading'
  item.abortController = new AbortController()
  
  // 同步状态到 transferStore
  transferStore.updateUploadTask(item.storeTaskId, { 
    status: 'uploading',
    startTime: startTime
  })
  
  for (let i = 0; i < totalChunks; i++) {
    // 检查是否暂停
    if (item.status === 'paused') {
      return
    }
    
    // 跳过已上传的分片
    if (item.uploadedChunks.includes(i)) {
      uploadedSize += CHUNK_SIZE
      continue
    }
    
    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    const chunk = file.slice(start, end)
    
    const formData = new FormData()
    formData.append('fileId', item.fileId)
    formData.append('chunkNumber', i)
    formData.append('file', chunk)
    
    await uploadChunk(formData, (percent) => {
      const chunkUploaded = (percent / 100) * (end - start)
      const totalUploaded = uploadedSize + chunkUploaded
      item.progress = Math.floor((totalUploaded / file.size) * 100)
      
      // 计算速度
      const elapsed = (Date.now() - startTime) / 1000
      if (elapsed > 0) {
        item.speed = Math.floor(totalUploaded / elapsed)
      }
      
      // 同步进度到 transferStore
      transferStore.updateUploadTask(item.storeTaskId, {
        progress: item.progress,
        speed: item.speed,
        uploadedSize: totalUploaded
      })
    })
    
    item.uploadedChunks.push(i)
    uploadedSize += (end - start)
  }
}

// 开始上传所有文件
const startUploadAll = async () => {
  isUploading.value = true
  
  const pendingFiles = fileList.value.filter(item => 
    ['pending', 'paused', 'error'].includes(item.status)
  )
  
  for (const item of pendingFiles) {
    if (item.status !== 'paused') {
      item.uploadedChunks = []
    }
    await uploadFile(item)
  }
  
  isUploading.value = false
}

// 暂停上传
const pauseUpload = (item) => {
  item.status = 'paused'
  if (item.abortController) {
    item.abortController.abort()
  }
  // 同步到 transferStore
  if (item.storeTaskId) {
    transferStore.updateUploadTask(item.storeTaskId, { status: 'paused', speed: 0 })
  }
}

// 继续上传
const resumeUpload = async (item) => {
  await uploadFile(item)
}

// 重试上传
const retryUpload = async (item) => {
  item.error = ''
  item.uploadedChunks = []
  await uploadFile(item)
}

// 移除文件
const removeFile = (id) => {
  const index = fileList.value.findIndex(item => item.id === id)
  if (index > -1) {
    const item = fileList.value[index]
    if (item.abortController) {
      item.abortController.abort()
    }
    // 从 transferStore 移除
    if (item.storeTaskId) {
      transferStore.removeUploadTask(item.storeTaskId)
    }
    fileList.value.splice(index, 1)
  }
}

// 清空所有文件
const clearAllFiles = () => {
  fileList.value.forEach(item => {
    if (item.abortController) {
      item.abortController.abort()
    }
    // 从 transferStore 移除（除了已完成的）
    if (item.storeTaskId && item.status !== 'completed' && item.status !== 'quick') {
      transferStore.removeUploadTask(item.storeTaskId)
    }
  })
  fileList.value = []
}

// 关闭对话框
const handleClose = () => {
  clearAllFiles()
  dialogVisible.value = false
  emit('close')
}
</script>

<style lang="scss" scoped>
.chunk-uploader {
  .upload-actions {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }
  
  .upload-area {
    min-height: 200px;
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    transition: all 0.3s;
    background: #fafbfc;
    
    &:hover {
      border-color: #c0c4cc;
    }
    
    &.drag-over {
      border-color: var(--el-color-primary);
      background: #ecf5ff;
    }
  }
  
  .upload-content {
    padding: 50px 20px;
    text-align: center;
    
    .upload-icon {
      font-size: 56px;
      color: #909399;
      margin-bottom: 20px;
    }
    
    .upload-text {
      .main-text {
        font-size: 16px;
        color: #606266;
        margin-bottom: 10px;
      }
      
      .sub-text {
        font-size: 13px;
        color: #909399;
      }
    }
  }
  
  .file-list {
    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #ebeef5;
      background: #fff;
      border-radius: 8px 8px 0 0;
      
      span {
        font-weight: 500;
        color: #303133;
      }
    }
    
    .list-body {
      max-height: 300px;
      overflow-y: auto;
    }
    
    .file-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #ebeef5;
      
      &:last-child {
        border-bottom: none;
      }
      
      .file-info {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;
        
        .file-icon {
          font-size: 24px;
          color: #909399;
          flex-shrink: 0;
        }
        
        .file-details {
          flex: 1;
          min-width: 0;
          
          .file-name {
            display: block;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-size: 14px;
            color: #303133;
          }
          
          .file-size {
            font-size: 12px;
            color: #909399;
            margin-right: 8px;
          }
          
          .file-path {
            font-size: 11px;
            color: #c0c4cc;
            display: block;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
      
      .file-status {
        width: 180px;
        margin: 0 16px;
        flex-shrink: 0;
        
        .status-text {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #909399;
          margin-bottom: 4px;
          
          &.success {
            color: #67c23a;
          }
          
          &.warning {
            color: #e6a23c;
          }
          
          &.danger {
            color: #f56c6c;
          }
        }
        
        .speed-text {
          font-size: 12px;
          color: #909399;
          margin-top: 4px;
        }
        
        :deep(.el-progress) {
          margin-top: 4px;
        }
      }
      
      .file-actions {
        display: flex;
        gap: 8px;
        flex-shrink: 0;
      }
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .chunk-uploader {
    .upload-list {
      .upload-item {
        .file-status {
          width: 140px;
          margin: 0 12px;
        }
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .chunk-uploader {
    .upload-header {
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
      
      .header-left {
        justify-content: center;
      }
      
      .header-right {
        justify-content: center;
        
        :deep(.el-button) {
          padding: 8px 12px;
          font-size: 13px;
        }
      }
    }
    
    .upload-list {
      .upload-item {
        flex-wrap: wrap;
        padding: 10px;
        
        .file-info {
          width: 100%;
          margin-bottom: 8px;
          
          .file-icon {
            width: 32px;
            height: 32px;
          }
          
          .file-details {
            .file-name {
              font-size: 13px;
            }
            
            .file-size {
              font-size: 11px;
            }
          }
        }
        
        .file-status {
          flex: 1;
          width: auto;
          margin: 0 8px 0 0;
          min-width: 100px;
          
          .status-text {
            font-size: 11px;
          }
          
          .speed-text {
            font-size: 11px;
          }
        }
        
        .file-actions {
          gap: 4px;
          
          :deep(.el-button) {
            padding: 4px 8px;
          }
        }
      }
    }
  }
}
</style>
