<template>
  <el-dialog
    v-model="dialogVisible"
    title="上传文件"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="chunk-uploader">
      <!-- 上传区域 -->
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        multiple
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
      >
        <div class="upload-content">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p class="main-text">点击或拖拽文件到此处上传</p>
            <p class="sub-text">支持大文件分片上传、断点续传、秒传</p>
          </div>
        </div>
      </el-upload>
      
      <!-- 文件列表 -->
      <div v-if="fileList.length > 0" class="file-list">
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
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import SparkMD5 from 'spark-md5'
import { initUpload, uploadChunk, mergeFile } from '@/api/fileApi'

const props = defineProps({
  folderId: { type: Number, default: 0 }
})

const emit = defineEmits(['close', 'uploaded'])

const dialogVisible = ref(true)
const uploadRef = ref(null)
const fileList = ref([])
const isUploading = ref(false)

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

// 处理文件选择
const handleFileChange = (uploadFile) => {
  const file = uploadFile.raw
  
  // 检查文件是否已存在
  const exists = fileList.value.some(item => 
    item.file.name === file.name && item.file.size === file.size
  )
  if (exists) {
    ElMessage.warning(`文件 ${file.name} 已在列表中`)
    return
  }
  
  fileList.value.push({
    id: Date.now() + Math.random(),
    file: file,
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
      folderId: props.folderId
    })
    
    // 秒传成功
    if (initRes.data?.quickUpload) {
      item.status = 'quick'
      item.progress = 100
      ElMessage.success(`${item.file.name} 秒传成功`)
      emit('uploaded')
      return
    }
    
    item.fileId = initRes.data?.fileId || initRes.fileId
    item.uploadedChunks = initRes.data?.uploadedChunks || []
    
    // 开始分片上传
    await uploadChunks(item)
    
    // 合并文件
    await mergeFile({
      fileId: item.fileId,
      fileHash: hash
    })
    
    item.status = 'completed'
    item.progress = 100
    ElMessage.success(`${item.file.name} 上传成功`)
    emit('uploaded')
    
  } catch (error) {
    if (error.name === 'AbortError' || item.status === 'paused') {
      return
    }
    item.status = 'error'
    item.error = error.message || '上传失败'
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
    
    await uploadChunk(formData, {
      signal: item.abortController.signal,
      onUploadProgress: (progressEvent) => {
        const chunkUploaded = progressEvent.loaded
        const totalUploaded = uploadedSize + chunkUploaded
        item.progress = Math.floor((totalUploaded / file.size) * 100)
        
        // 计算速度
        const elapsed = (Date.now() - startTime) / 1000
        if (elapsed > 0) {
          item.speed = Math.floor(totalUploaded / elapsed)
        }
      }
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
    fileList.value.splice(index, 1)
  }
}

// 清空所有文件
const clearAllFiles = () => {
  fileList.value.forEach(item => {
    if (item.abortController) {
      item.abortController.abort()
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
  .upload-area {
    :deep(.el-upload) {
      width: 100%;
    }
    
    :deep(.el-upload-dragger) {
      width: 100%;
      height: auto;
      padding: 40px 20px;
    }
  }
  
  .upload-content {
    text-align: center;
    
    .upload-icon {
      font-size: 48px;
      color: #c0c4cc;
      margin-bottom: 16px;
    }
    
    .upload-text {
      .main-text {
        font-size: 16px;
        color: #606266;
        margin-bottom: 8px;
      }
      
      .sub-text {
        font-size: 12px;
        color: #909399;
      }
    }
  }
  
  .file-list {
    margin-top: 20px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    
    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #ebeef5;
      background: #fafafa;
      
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
</style>
