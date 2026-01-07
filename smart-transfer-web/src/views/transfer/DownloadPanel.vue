<template>
  <div class="download-panel">
    <!-- 新建下载任务 -->
    <div class="download-toolbar">
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        新建下载任务
      </el-button>
      <el-button @click="refreshFromFileList">
        <el-icon><Refresh /></el-icon>
        从文件列表导入
      </el-button>
    </div>

    <!-- 下载队列 -->
    <div v-if="downloadQueue.length > 0" class="download-queue">
      <div class="queue-header">
        <div class="queue-title">
          <el-icon><Download /></el-icon>
          下载队列 
          <el-tag size="small" type="success" style="margin-left: 10px;">
            {{ downloadQueue.length }} 个任务
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
          v-for="item in downloadQueue"
          :key="item.id"
          class="queue-item"
          :class="{ 
            downloading: item.status === 'downloading',
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
                  <el-divider direction="vertical" />
                  <span class="save-path" :title="item.savePath">
                    保存至: {{ item.savePath || '默认位置' }}
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
                @click="startDownload(item)"
              >
                <el-icon><VideoPlay /></el-icon>
                开始
              </el-button>
              <el-button
                v-if="item.status === 'downloading'"
                size="small"
                type="warning"
                text
                @click="pauseDownload(item)"
              >
                <el-icon><VideoPause /></el-icon>
                暂停
              </el-button>
              <el-button
                v-if="item.status === 'completed'"
                size="small"
                type="success"
                text
                @click="openFile(item)"
              >
                <el-icon><FolderOpened /></el-icon>
                打开
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
              <span class="downloaded">
                已下载: {{ formatFileSize(item.downloadedSize) }} / {{ formatFileSize(item.fileSize) }}
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
      description="暂无下载任务"
      :image-size="120"
    >
      <template #image>
        <el-icon :size="80" color="#909399">
          <Download />
        </el-icon>
      </template>
      <template #default>
        <el-button type="primary" @click="showAddDialog = true">
          立即创建下载任务
        </el-button>
      </template>
    </el-empty>
    
    <!-- 新建下载对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="新建下载任务"
      width="600px"
    >
      <el-form :model="newDownload" label-width="100px">
        <el-form-item label="文件ID">
          <el-input 
            v-model="newDownload.fileId" 
            placeholder="请输入文件ID（从文件列表获取）"
          />
        </el-form-item>
        <el-form-item label="文件名">
          <el-input 
            v-model="newDownload.fileName" 
            placeholder="请输入文件名"
          />
        </el-form-item>
        <el-form-item label="保存位置">
          <el-input 
            v-model="newDownload.savePath" 
            placeholder="留空使用默认下载路径"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addDownloadTask">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Plus, Download, Document, VideoPlay, VideoPause, Delete, 
  FolderOpened, Refresh
} from '@element-plus/icons-vue'
import { useFileStore } from '@/store/fileStore'
import { formatFileSize, formatSpeed } from '@/utils/file'
import { getDownloadUrl } from '@/api/fileApi'

const fileStore = useFileStore()
const downloadQueue = ref([])
const showAddDialog = ref(false)
const newDownload = ref({
  fileId: '',
  fileName: '',
  savePath: ''
})

// 添加下载任务
const addDownloadTask = () => {
  if (!newDownload.value.fileId || !newDownload.value.fileName) {
    ElMessage.warning('请填写文件ID和文件名')
    return
  }
  
  const task = {
    id: Date.now() + Math.random(),
    fileId: newDownload.value.fileId,
    fileName: newDownload.value.fileName,
    fileSize: 0, // 实际应从API获取
    downloadedSize: 0,
    savePath: newDownload.value.savePath || '默认下载文件夹',
    status: 'pending',
    progress: 0,
    speed: 0,
    timeRemaining: ''
  }
  
  downloadQueue.value.push(task)
  fileStore.addDownloadTask(task)
  
  showAddDialog.value = false
  newDownload.value = { fileId: '', fileName: '', savePath: '' }
  
  ElMessage.success('已添加下载任务')
}

// 从文件列表导入
const refreshFromFileList = () => {
  ElMessage.info('此功能将从文件管理页面导入可下载文件')
  // TODO: 实现从文件列表导入逻辑
}

// 开始下载
const startDownload = async (item) => {
  try {
    item.status = 'downloading'
    
    // 获取下载URL
    const downloadUrl = getDownloadUrl(item.fileId)
    
    // 模拟下载进度（实际应使用 fetch + stream 或 axios + onDownloadProgress）
    const interval = setInterval(() => {
      if (item.status !== 'downloading') {
        clearInterval(interval)
        return
      }
      
      item.progress += Math.random() * 10
      if (item.progress >= 100) {
        item.progress = 100
        item.status = 'completed'
        item.speed = 0
        clearInterval(interval)
        ElMessage.success(`${item.fileName} 下载完成！`)
        fileStore.moveDownloadToCompleted(item)
      } else {
        item.speed = Math.random() * 5 * 1024 * 1024 // 模拟速度
        item.downloadedSize = Math.floor((item.progress / 100) * item.fileSize)
        
        const remaining = item.fileSize - item.downloadedSize
        const timeLeft = Math.ceil(remaining / item.speed)
        item.timeRemaining = formatTime(timeLeft)
      }
    }, 500)
    
    // 实际下载逻辑
    // window.open(downloadUrl, '_blank')
    
  } catch (error) {
    // 下载失败
    item.status = 'failed'
    item.speed = 0
    ElMessage.error(`${item.fileName} 下载失败: ${error.message || '未知错误'}`)
  }
}

// 暂停下载
const pauseDownload = (item) => {
  item.status = 'paused'
  item.speed = 0
  ElMessage.info(`已暂停 ${item.fileName}`)
}

// 打开文件
const openFile = (item) => {
  ElMessage.success(`正在打开 ${item.fileName}`)
  // TODO: 实现打开文件逻辑
}

// 从队列删除
const removeFromQueue = (id) => {
  const item = downloadQueue.value.find(i => i.id === id)
  if (item && item.status === 'downloading') {
    ElMessage.warning('请先暂停下载')
    return
  }
  
  downloadQueue.value = downloadQueue.value.filter(item => item.id !== id)
  fileStore.removeDownloadTask(id)
  ElMessage.success('已从队列移除')
}

// 全部开始
const startAll = () => {
  const pendingItems = downloadQueue.value.filter(
    item => item.status === 'pending' || item.status === 'paused'
  )
  pendingItems.forEach(item => startDownload(item))
}

// 全部暂停
const pauseAll = () => {
  const downloadingItems = downloadQueue.value.filter(
    item => item.status === 'downloading'
  )
  downloadingItems.forEach(item => pauseDownload(item))
}

// 清空已完成
const clearCompleted = () => {
  const completedItems = downloadQueue.value.filter(item => item.status === 'completed')
  if (completedItems.length === 0) {
    ElMessage.info('没有已完成的任务')
    return
  }
  
  downloadQueue.value = downloadQueue.value.filter(item => item.status !== 'completed')
  ElMessage.success(`已清除 ${completedItems.length} 个已完成任务`)
}

// 获取进度状态
const getProgressStatus = (status) => {
  const statusMap = {
    'completed': 'success',
    'failed': 'exception',
    'downloading': ''
  }
  return statusMap[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    'pending': '等待中',
    'downloading': '下载中',
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
  downloadQueue
})
</script>

<style scoped>
.download-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 工具栏 */
.download-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

/* 下载队列（样式与上传队列相同） */
.download-queue {
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

.queue-item.downloading {
  border-left: 4px solid var(--el-color-success);
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
  color: var(--el-color-success);
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

.file-status.status-downloading {
  color: var(--el-color-success);
}

.file-status.status-completed {
  color: var(--el-color-success);
}

.file-status.status-failed {
  color: var(--el-color-danger);
}

.save-path {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.downloaded {
  color: var(--el-text-color-regular);
}

.time-remaining {
  color: var(--el-text-color-regular);
}
</style>

