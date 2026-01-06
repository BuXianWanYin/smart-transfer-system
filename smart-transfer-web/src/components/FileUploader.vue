<template>
  <div class="file-uploader">
    <el-upload
      ref="uploadRef"
      :auto-upload="false"
      :on-change="handleFileChange"
      :show-file-list="false"
      drag
      multiple
    >
      <el-icon class="upload-icon">
        <UploadFilled />
      </el-icon>
      <div class="upload-text">
        <div class="upload-title">点击或拖拽文件到此处上传</div>
        <div class="upload-hint">支持大文件上传，支持断点续传</div>
      </div>
    </el-upload>

    <!-- 上传队列 -->
    <div v-if="uploadQueue.length > 0" class="upload-queue">
      <div class="queue-header">
        <span>上传队列 ({{ uploadQueue.length }})</span>
        <el-button size="small" text @click="clearAll">清空</el-button>
      </div>
      
      <div class="queue-list">
        <div
          v-for="item in uploadQueue"
          :key="item.id"
          class="queue-item"
        >
          <div class="item-info">
            <el-icon class="file-icon">
              <Document />
            </el-icon>
            <div class="file-details">
              <div class="file-name">{{ item.fileName }}</div>
              <div class="file-size">{{ formatFileSize(item.fileSize) }}</div>
            </div>
          </div>
          
          <div class="item-progress">
            <el-progress
              :percentage="item.progress"
              :status="getProgressStatus(item.status)"
            />
            <div class="progress-info">
              <span>{{ item.speed > 0 ? formatSpeed(item.speed) : '-' }}</span>
              <span>{{ getStatusText(item.status) }}</span>
            </div>
          </div>
          
          <div class="item-actions">
            <el-button
              v-if="item.status === 'pending' || item.status === 'paused'"
              size="small"
              type="primary"
              @click="startUpload(item)"
            >
              开始
            </el-button>
            <el-button
              v-if="item.status === 'uploading'"
              size="small"
              @click="pauseUpload(item)"
            >
              暂停
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="removeFromQueue(item.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { useFileStore } from '@/store/fileStore'
import { formatFileSize, formatSpeed } from '@/utils/file'
import { createFileChunks, calculateFileHash } from '@/utils/file'
import { initUpload, uploadChunk, mergeFile } from '@/api/fileApi'

const fileStore = useFileStore()
const uploadRef = ref()
const uploadQueue = ref([])

// 处理文件选择
const handleFileChange = (file) => {
  // 检查文件大小限制
  const maxFileSize = Number(import.meta.env.VITE_MAX_FILE_SIZE) || 10737418240 // 默认 10GB
  if (file.size > maxFileSize) {
    ElMessage.error(`文件大小超过限制 ${formatFileSize(maxFileSize)}`)
    return
  }
  
  const fileInfo = {
    id: Date.now() + Math.random(),
    file: file.raw,
    fileName: file.name,
    fileSize: file.size,
    status: 'pending',
    progress: 0,
    speed: 0,
    uploadedChunks: []
  }
  
  uploadQueue.value.push(fileInfo)
  fileStore.addToQueue(fileInfo)
}

// 开始上传
const startUpload = async (item) => {
  try {
    item.status = 'hashing'
    ElMessage.info('正在计算文件哈希...')
    
    // 计算文件哈希
    const fileHash = await calculateFileHash(item.file, (progress) => {
      item.progress = Math.floor(progress * 0.1) // 哈希计算占10%进度
    })
    
    // 初始化上传 - 从环境变量读取分片大小
    const chunkSize = Number(import.meta.env.VITE_CHUNK_SIZE) || (2 * 1024 * 1024) // 默认 2MB
    const chunks = createFileChunks(item.file, chunkSize)
    
    const initRes = await initUpload({
      fileName: item.fileName,
      fileSize: item.fileSize,
      fileHash: fileHash,
      chunkSize: chunkSize,
      totalChunks: chunks.length
    })
    
    if (initRes.data.quickUpload) {
      ElMessage.success('文件已存在，秒传成功！')
      item.status = 'completed'
      item.progress = 100
      return
    }
    
    item.status = 'uploading'
    item.fileId = initRes.data.fileId
    item.uploadedChunks = initRes.data.uploadedChunks || []
    
    // 上传分片
    await uploadChunks(item, chunks, fileHash)
    
    // 合并文件
    await mergeFile({
      fileId: item.fileId,
      fileHash: fileHash
    })
    
    item.status = 'completed'
    item.progress = 100
    ElMessage.success('上传成功！')
    
  } catch (error) {
    console.error('上传失败', error)
    item.status = 'failed'
    ElMessage.error('上传失败: ' + (error.message || '未知错误'))
  }
}

// 上传分片
const uploadChunks = async (item, chunks, fileHash) => {
  const startTime = Date.now()
  let uploadedSize = 0
  
  for (let i = 0; i < chunks.length; i++) {
    if (item.uploadedChunks.includes(i)) {
      continue // 跳过已上传的分片
    }
    
    const formData = new FormData()
    formData.append('fileId', item.fileId)
    formData.append('chunkNumber', i)
    formData.append('chunkHash', await calculateChunkHash(chunks[i].file))
    formData.append('file', chunks[i].file)
    
    await uploadChunk(formData, (percent) => {
      // 更新进度（10%哈希 + 85%上传 + 5%合并）
      const baseProgress = 10 + (i / chunks.length) * 85
      const chunkProgress = (percent / 100) * (85 / chunks.length)
      item.progress = Math.floor(baseProgress + chunkProgress)
      
      // 计算速度
      uploadedSize += chunks[i].size * (percent / 100)
      const elapsed = (Date.now() - startTime) / 1000
      item.speed = Math.floor(uploadedSize / elapsed)
    })
    
    item.uploadedChunks.push(i)
  }
}

// 计算分片哈希（简化版）
const calculateChunkHash = async (chunk) => {
  // 这里简化处理，实际应使用 spark-md5
  return 'chunk_' + Date.now()
}

// 暂停上传
const pauseUpload = (item) => {
  item.status = 'paused'
}

// 从队列删除
const removeFromQueue = (id) => {
  uploadQueue.value = uploadQueue.value.filter(item => item.id !== id)
  fileStore.removeFromQueue(id)
}

// 清空队列
const clearAll = () => {
  uploadQueue.value = uploadQueue.value.filter(item => item.status === 'uploading')
}

// 获取进度状态
const getProgressStatus = (status) => {
  const statusMap = {
    'completed': 'success',
    'failed': 'exception',
    'uploading': '',
    'hashing': ''
  }
  return statusMap[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    'pending': '等待中',
    'hashing': '计算哈希',
    'uploading': '上传中',
    'paused': '已暂停',
    'completed': '已完成',
    'failed': '失败'
  }
  return textMap[status] || status
}
</script>

<style scoped>
.file-uploader {
  width: 100%;
}

.upload-icon {
  font-size: 67px;
  color: #409eff;
  margin-bottom: 16px;
}

.upload-text {
  text-align: center;
}

.upload-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 14px;
  color: #909399;
}

.upload-queue {
  margin-top: 20px;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.queue-list {
  margin-top: 10px;
}

.queue-item {
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 10px;
}

.item-info {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.file-icon {
  font-size: 32px;
  color: #409eff;
  margin-right: 12px;
}

.file-details {
  flex: 1;
}

.file-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.item-progress {
  margin-bottom: 10px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

