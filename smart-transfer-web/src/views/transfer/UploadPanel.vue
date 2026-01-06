<template>
  <div class="upload-panel">
    <!-- 上传区域 -->
    <div class="upload-area">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        drag
        multiple
        class="upload-dragger"
      >
        <el-icon class="upload-icon">
          <UploadFilled />
        </el-icon>
        <div class="upload-text">
          <div class="upload-title">拖拽文件到此处或点击选择文件</div>
          <div class="upload-hint">支持大文件上传、断点续传、秒传功能</div>
        </div>
      </el-upload>
    </div>

    <!-- 上传队列 -->
    <div v-if="uploadQueue.length > 0" class="upload-queue">
      <div class="queue-header">
        <div class="queue-title">
          <el-icon><List /></el-icon>
          上传队列 
          <el-tag size="small" type="info" style="margin-left: 10px;">
            {{ uploadQueue.length }} 个任务
          </el-tag>
        </div>
        <div class="queue-actions">
          <el-button size="small" @click="startAll">
            <el-icon><VideoPlay /></el-icon>
            全部开始
          </el-button>
          <el-button size="small" @click="pauseAll">
            <el-icon><VideoPause /></el-icon>
            全部暂停
          </el-button>
          <el-button size="small" type="danger" plain @click="clearCompleted">
            <el-icon><Delete /></el-icon>
            清空已完成
          </el-button>
        </div>
      </div>
      
      <div class="queue-list">
        <div
          v-for="item in uploadQueue"
          :key="item.id"
          class="queue-item"
          :class="{ 
            uploading: item.status === 'uploading',
            completed: item.status === 'completed',
            failed: item.status === 'failed'
          }"
        >
          <!-- 文件信息 -->
          <div class="item-header">
            <div class="file-info">
              <el-icon class="file-icon" :class="getFileIconClass(item.fileName)">
                <Document />
              </el-icon>
              <div class="file-details">
                <div class="file-name" :title="item.fileName">{{ item.fileName }}</div>
                <div class="file-meta">
                  <span class="file-size">{{ formatFileSize(item.fileSize) }}</span>
                  <el-divider direction="vertical" />
                  <span class="file-status" :class="getStatusClass(item.status)">
                    {{ getStatusText(item.status) }}
                  </span>
                </div>
              </div>
            </div>
            <div class="item-actions">
              <el-button
                v-if="item.status === 'pending' || item.status === 'paused'"
                size="small"
                type="primary"
                text
                @click="startUpload(item)"
              >
                <el-icon><VideoPlay /></el-icon>
                开始
              </el-button>
              <el-button
                v-if="item.status === 'uploading' || item.status === 'hashing'"
                size="small"
                type="warning"
                text
                @click="pauseUpload(item)"
              >
                <el-icon><VideoPause /></el-icon>
                暂停
              </el-button>
              <el-button
                size="small"
                type="danger"
                text
                @click="removeFromQueue(item.id)"
              >
                <el-icon><Delete /></el-icon>
                {{ item.status === 'completed' ? '移除' : '取消' }}
              </el-button>
            </div>
          </div>
          
          <!-- 进度条 -->
          <div class="item-progress">
            <el-progress
              :percentage="item.progress"
              :status="getProgressStatus(item.status)"
              :stroke-width="8"
            />
            <div class="progress-details">
              <span class="speed">
                {{ item.speed > 0 ? formatSpeed(item.speed) : '-' }}
              </span>
              <span class="time-remaining">
                {{ item.timeRemaining ? `剩余 ${item.timeRemaining}` : '' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <el-empty
      v-else
      description="暂无上传任务"
      :image-size="120"
    >
      <template #image>
        <el-icon :size="80" color="#909399">
          <FolderOpened />
        </el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  UploadFilled, Document, List, VideoPlay, VideoPause, Delete, FolderOpened 
} from '@element-plus/icons-vue'
import { useFileStore } from '@/store/fileStore'
import { formatFileSize, formatSpeed, createFileChunks, calculateFileHash } from '@/utils/file'
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
    timeRemaining: '',
    uploadedChunks: []
  }
  
  uploadQueue.value.push(fileInfo)
  fileStore.addToQueue(fileInfo)
  
  ElMessage.success(`已添加 ${file.name} 到上传队列`)
}

// 开始上传
const startUpload = async (item) => {
  try {
    item.status = 'hashing'
    
    // 计算文件哈希
    const fileHash = await calculateFileHash(item.file, (progress) => {
      item.progress = Math.floor(progress * 0.1) // 哈希计算占10%进度
    })
    
    // 初始化上传
    const chunkSize = Number(import.meta.env.VITE_CHUNK_SIZE) || (2 * 1024 * 1024) // 默认 2MB
    const chunks = createFileChunks(item.file, chunkSize)
    
    const initRes = await initUpload({
      fileName: item.fileName,
      fileSize: item.fileSize,
      fileHash: fileHash,
      chunkSize: chunkSize,
      totalChunks: chunks.length
    })
    
    // 检查是否秒传
    if (initRes.data.quickUpload) {
      ElMessage.success(`${item.fileName} 秒传成功！`)
      item.status = 'completed'
      item.progress = 100
      fileStore.moveToCompleted(item)
      return
    }
    
    item.status = 'uploading'
    item.fileId = initRes.data.fileId
    item.uploadedChunks = initRes.data.uploadedChunks || []
    
    // 上传分片
    await uploadChunks(item, chunks, fileHash)
    
    // 合并文件
    item.progress = 95
    await mergeFile({
      fileId: item.fileId,
      fileHash: fileHash
    })
    
    item.status = 'completed'
    item.progress = 100
    item.speed = 0
    ElMessage.success(`${item.fileName} 上传成功！`)
    fileStore.moveToCompleted(item)
    
  } catch (error) {
    console.error('上传失败', error)
    item.status = 'failed'
    item.speed = 0
    ElMessage.error(`${item.fileName} 上传失败: ${error.message || '未知错误'}`)
  }
}

// 上传分片
const uploadChunks = async (item, chunks, fileHash) => {
  const startTime = Date.now()
  let uploadedSize = item.uploadedChunks.length * chunks[0].size
  
  for (let i = 0; i < chunks.length; i++) {
    if (item.status === 'paused') {
      throw new Error('用户暂停')
    }
    
    if (item.uploadedChunks.includes(i)) {
      continue // 跳过已上传的分片
    }
    
    const formData = new FormData()
    formData.append('fileId', item.fileId)
    formData.append('chunkNumber', i)
    formData.append('chunkHash', `${fileHash}_${i}`)
    formData.append('file', chunks[i].file)
    
    await uploadChunk(formData, (percent) => {
      // 更新进度（10%哈希 + 85%上传 + 5%合并）
      const baseProgress = 10 + (i / chunks.length) * 85
      const chunkProgress = (percent / 100) * (85 / chunks.length)
      item.progress = Math.floor(baseProgress + chunkProgress)
      
      // 计算速度和剩余时间
      uploadedSize += (chunks[i].size * percent) / 100
      const elapsed = (Date.now() - startTime) / 1000
      item.speed = Math.floor(uploadedSize / elapsed)
      
      const remaining = item.fileSize - uploadedSize
      const timeLeft = Math.ceil(remaining / item.speed)
      item.timeRemaining = formatTime(timeLeft)
    })
    
    item.uploadedChunks.push(i)
  }
}

// 暂停上传
const pauseUpload = (item) => {
  item.status = 'paused'
  item.speed = 0
  ElMessage.info(`已暂停 ${item.fileName}`)
}

// 从队列删除
const removeFromQueue = (id) => {
  const item = uploadQueue.value.find(i => i.id === id)
  if (item && item.status === 'uploading') {
    ElMessage.warning('请先暂停上传')
    return
  }
  
  uploadQueue.value = uploadQueue.value.filter(item => item.id !== id)
  fileStore.removeFromQueue(id)
  ElMessage.success('已从队列移除')
}

// 全部开始
const startAll = () => {
  const pendingItems = uploadQueue.value.filter(
    item => item.status === 'pending' || item.status === 'paused'
  )
  pendingItems.forEach(item => startUpload(item))
}

// 全部暂停
const pauseAll = () => {
  const uploadingItems = uploadQueue.value.filter(
    item => item.status === 'uploading' || item.status === 'hashing'
  )
  uploadingItems.forEach(item => pauseUpload(item))
}

// 清空已完成
const clearCompleted = () => {
  const completedItems = uploadQueue.value.filter(item => item.status === 'completed')
  if (completedItems.length === 0) {
    ElMessage.info('没有已完成的任务')
    return
  }
  
  uploadQueue.value = uploadQueue.value.filter(item => item.status !== 'completed')
  ElMessage.success(`已清除 ${completedItems.length} 个已完成任务`)
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
    'hashing': '计算哈希中',
    'uploading': '上传中',
    'paused': '已暂停',
    'completed': '已完成',
    'failed': '失败'
  }
  return textMap[status] || status
}

// 获取状态样式类
const getStatusClass = (status) => {
  return `status-${status}`
}

// 获取文件图标类
const getFileIconClass = (fileName) => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  const iconMap = {
    'mp4': 'video',
    'avi': 'video',
    'mkv': 'video',
    'jpg': 'image',
    'png': 'image',
    'gif': 'image',
    'pdf': 'pdf',
    'doc': 'document',
    'docx': 'document',
    'zip': 'archive',
    'rar': 'archive'
  }
  return iconMap[ext] || 'file'
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds || seconds < 0) return ''
  
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  
  if (h > 0) return `${h}小时${m}分钟`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

// 暴露方法给父组件
defineExpose({
  uploadQueue
})
</script>

<style scoped>
.upload-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 上传区域 */
.upload-area {
  margin-bottom: 30px;
}

.upload-dragger {
  width: 100%;
}

.upload-dragger :deep(.el-upload-dragger) {
  width: 100%;
  padding: 40px;
  border: 2px dashed var(--el-border-color);
  border-radius: 8px;
  transition: all 0.3s;
}

.upload-dragger :deep(.el-upload-dragger:hover) {
  border-color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}

.upload-icon {
  font-size: 67px;
  color: var(--el-color-primary);
  margin-bottom: 16px;
}

.upload-text {
  text-align: center;
}

.upload-title {
  font-size: 16px;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
  font-weight: 500;
}

.upload-hint {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

/* 上传队列 */
.upload-queue {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: var(--el-fill-color-light);
  border-radius: 8px 8px 0 0;
  border-bottom: 2px solid var(--el-border-color);
}

.queue-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.queue-actions {
  display: flex;
  gap: 10px;
}

.queue-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}

.queue-item {
  padding: 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.queue-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.queue-item.uploading {
  border-left: 4px solid var(--el-color-primary);
}

.queue-item.completed {
  border-left: 4px solid var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.queue-item.failed {
  border-left: 4px solid var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.file-info {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.file-icon {
  font-size: 36px;
  color: var(--el-color-primary);
  margin-right: 15px;
  flex-shrink: 0;
}

.file-icon.video {
  color: #E040FB;
}

.file-icon.image {
  color: #FF9800;
}

.file-icon.pdf {
  color: #F44336;
}

.file-icon.document {
  color: #2196F3;
}

.file-icon.archive {
  color: #9C27B0;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.file-status {
  font-weight: 500;
}

.file-status.status-uploading {
  color: var(--el-color-primary);
}

.file-status.status-completed {
  color: var(--el-color-success);
}

.file-status.status-failed {
  color: var(--el-color-danger);
}

.item-actions {
  display: flex;
  gap: 5px;
  flex-shrink: 0;
  margin-left: 15px;
}

.item-progress {
  margin-top: 10px;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}

.speed {
  font-weight: 600;
  color: var(--el-color-success);
}

.time-remaining {
  color: var(--el-text-color-regular);
}
</style>

