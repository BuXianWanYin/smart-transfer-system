<template>
  <div class="transfer-center">
    <!-- 左侧菜单 -->
    <div class="transfer-sidebar">
      <div 
        class="sidebar-item" 
        :class="{ active: activeMenu === 'upload' }"
        @click="activeMenu = 'upload'"
      >
        <el-icon><Upload /></el-icon>
        <span>上传</span>
      </div>
      <div 
        class="sidebar-item" 
        :class="{ active: activeMenu === 'download' }"
        @click="activeMenu = 'download'"
      >
        <el-icon><Download /></el-icon>
        <span>下载</span>
      </div>
    </div>
    
    <!-- 右侧内容区 -->
    <div class="transfer-content">
      <!-- 顶部操作栏 -->
      <div class="content-toolbar">
        <div class="toolbar-actions">
          <el-button text @click="handlePauseAll">
            <el-icon><VideoPause /></el-icon>
            全部暂停
          </el-button>
          <el-button text @click="handleStartAll">
            <el-icon><VideoPlay /></el-icon>
            全部开始
          </el-button>
          <el-button text @click="handleDeleteAll">
            <el-icon><Delete /></el-icon>
            全部删除
          </el-button>
        </div>
        
        <!-- 监控面板（可折叠） -->
        <div class="monitor-toggle">
          <el-button 
            text 
            :type="showMonitor ? 'primary' : 'default'"
            @click="showMonitor = !showMonitor"
          >
            <el-icon><Monitor /></el-icon>
            {{ showMonitor ? '隐藏监控' : '显示监控' }}
          </el-button>
        </div>
      </div>
      
      <!-- 拥塞监控面板 -->
      <transition name="slide">
        <div v-show="showMonitor" class="monitor-panel">
          <div class="monitor-grid">
            <div class="monitor-item">
              <div class="monitor-label">当前算法</div>
              <div class="monitor-value primary">{{ currentMetrics.algorithm || 'CUBIC' }}</div>
            </div>
            <div class="monitor-item">
              <div class="monitor-label">拥塞窗口</div>
              <div class="monitor-value info">{{ formatFileSize(currentMetrics.cwnd) }}</div>
            </div>
            <div class="monitor-item">
              <div class="monitor-label">传输速率</div>
              <div class="monitor-value success">{{ formatSpeed(realTimeSpeed) }}</div>
            </div>
            <div class="monitor-item">
              <div class="monitor-label">RTT</div>
              <div class="monitor-value">{{ currentMetrics.rtt || 0 }}ms</div>
            </div>
            <div class="monitor-item">
              <div class="monitor-label">丢包率</div>
              <div class="monitor-value" :class="{ danger: currentMetrics.lossRate > 0.05 }">
                {{ formatPercent(currentMetrics.lossRate) }}
              </div>
            </div>
            <div class="monitor-item">
              <div class="monitor-label">网络质量</div>
              <div class="monitor-value">
                <el-tag :type="getQualityType(currentMetrics.networkQuality)" size="small">
                  {{ currentMetrics.networkQuality || '良好' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </transition>
      
      <!-- 子标签切换 -->
      <div class="sub-tabs">
        <div 
          class="sub-tab" 
          :class="{ active: activeSubTab === 'transferring' }"
          @click="activeSubTab = 'transferring'"
        >
          {{ activeMenu === 'upload' ? '上传中' : '下载中' }}({{ transferringCount }})
        </div>
        <div 
          class="sub-tab" 
          :class="{ active: activeSubTab === 'completed' }"
          @click="activeSubTab = 'completed'"
        >
          已完成({{ completedCount }})
        </div>
        
        <!-- 右侧操作 -->
        <div class="sub-tab-actions">
          <el-button 
            v-if="activeSubTab === 'completed'" 
            text 
            type="primary" 
            @click="handleClearRecords"
          >
            <el-icon><Delete /></el-icon>
            清空全部记录
          </el-button>
        </div>
      </div>
      
      <!-- 列表头部 -->
      <div class="list-header" v-if="activeSubTab === 'completed'">
        <span class="header-select">全部文件</span>
        <span class="header-col">文件</span>
        <span class="header-col">大小</span>
        <span class="header-col">{{ activeMenu === 'upload' ? '完成时间' : '完成时间' }}</span>
      </div>
      
      <!-- 文件列表区域 -->
      <div class="file-list-area">
        <!-- 传输中列表 -->
        <div v-if="activeSubTab === 'transferring'" class="transfer-list">
          <template v-if="transferringList.length > 0">
            <div 
              v-for="item in transferringList" 
              :key="item.id" 
              class="transfer-item"
            >
              <div class="item-icon">
                <img :src="getFileIcon(item.fileName)" alt="" />
              </div>
              <div class="item-info">
                <div class="item-name">{{ item.fileName }}</div>
                <div class="item-progress">
                  <el-progress 
                    :percentage="item.progress || 0" 
                    :stroke-width="4"
                    :show-text="false"
                  />
                  <span class="progress-text">
                    {{ formatFileSize(activeMenu === 'upload' ? (item.uploadedSize || 0) : (item.downloadedSize || 0)) }} / {{ formatFileSize(item.fileSize) }}
                  </span>
                </div>
              </div>
              <div class="item-speed">
                {{ formatSpeed(item.speed || 0) }}
              </div>
              <div class="item-status">
                <el-tag :type="getStatusType(item.status)" size="small">
                  {{ getStatusText(item.status) }}
                </el-tag>
              </div>
              <div class="item-actions">
                <el-button 
                  v-if="item.status === 'uploading' || item.status === 'downloading'"
                  text 
                  @click="handlePause(item)"
                >
                  <el-icon><VideoPause /></el-icon>
                </el-button>
                <el-button 
                  v-if="item.status === 'paused'"
                  text 
                  type="primary"
                  @click="handleResume(item)"
                >
                  <el-icon><VideoPlay /></el-icon>
                </el-button>
                <el-button text type="danger" @click="handleCancel(item)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </template>
          <el-empty v-else :description="activeMenu === 'upload' ? '暂无正在上传的文件' : '暂无正在下载的文件'">
            <template #image>
              <el-icon :size="80" color="#c0c4cc">
                <component :is="activeMenu === 'upload' ? 'Upload' : 'Download'" />
              </el-icon>
            </template>
          </el-empty>
        </div>
        
        <!-- 已完成列表 -->
        <div v-else class="completed-list">
          <template v-if="completedList.length > 0">
            <div 
              v-for="item in completedList" 
              :key="item.id" 
              class="completed-item"
            >
              <el-checkbox v-model="item.selected" />
              <div class="item-icon">
                <img :src="getFileIcon(item.fileName)" alt="" />
              </div>
              <div class="item-info">
                <div class="item-name">{{ item.fileName }}</div>
              </div>
              <div class="item-size">
                {{ formatFileSize(item.fileSize) }}
              </div>
              <div class="item-time">
                {{ formatDateTime(item.completedTime) }}
              </div>
              <div class="item-actions">
                <el-button 
                  v-if="activeMenu === 'upload'"
                  text 
                  type="primary" 
                  size="small"
                  title="打开所在文件夹"
                  @click="handleOpenFolder(item)"
                >
                  <el-icon><FolderOpened /></el-icon>
                </el-button>
                <el-button 
                  v-if="activeMenu === 'download'"
                  text 
                  type="primary" 
                  size="small"
                  title="打开文件"
                  @click="handleOpenFile(item)"
                >
                  <el-icon><Document /></el-icon>
                </el-button>
                <el-button 
                  text 
                  size="small"
                  title="删除记录"
                  @click="handleDeleteRecord(item)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </template>
          <el-empty v-else description="暂无完成记录">
            <template #image>
              <el-icon :size="80" color="#c0c4cc">
                <CircleCheck />
              </el-icon>
            </template>
          </el-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, Download, VideoPause, VideoPlay, Delete, Monitor,
  Close, FolderOpened, Document, CircleCheck
} from '@element-plus/icons-vue'
import { useCongestionStore } from '@/store/congestionStore'
import { useTransferStore } from '@/store/transferStore'
import { monitorWs } from '@/utils/websocket'
import { formatFileSize, formatSpeed } from '@/utils/format'
import { formatDateTime } from '@/utils/format'
import { getHistoryList, deleteHistory, clearAllHistory } from '@/api/historyApi'
import { getFileIconByType } from '@/utils/fileType'
import { getDownloadUrl } from '@/api/fileApi'

const congestionStore = useCongestionStore()
const transferStore = useTransferStore()

// 状态
const activeMenu = ref('upload')
const activeSubTab = ref('transferring')
const showMonitor = ref(false)
const isMonitoring = ref(true)
const wsConnected = ref(false)

// 监控数据
const currentMetrics = ref({
  algorithm: 'CUBIC',
  cwnd: 0,
  rate: 0,
  rtt: 0,
  lossRate: 0,
  bandwidth: 0,
  networkQuality: '良好'
})

// 已完成列表
const uploadCompletedList = ref([])
const downloadCompletedList = ref([])

// 计算属性 - 传输中数量
const transferringCount = computed(() => {
  if (activeMenu.value === 'upload') {
    return transferStore.uploadingTasks?.length || 0
  }
  return transferStore.downloadingTasks?.length || 0
})

// 计算属性 - 已完成数量
const completedCount = computed(() => {
  if (activeMenu.value === 'upload') {
    return uploadCompletedList.value.length
  }
  return downloadCompletedList.value.length
})

// 计算属性 - 传输中列表
const transferringList = computed(() => {
  if (activeMenu.value === 'upload') {
    return transferStore.uploadQueue || []
  }
  return transferStore.downloadQueue || []
})

// 计算属性 - 已完成列表
const completedList = computed(() => {
  if (activeMenu.value === 'upload') {
    return uploadCompletedList.value
  }
  return downloadCompletedList.value
})

// 实时传输速率
const realTimeSpeed = computed(() => {
  return transferStore.totalUploadSpeed + transferStore.totalDownloadSpeed
})

let wsUnsubscribe = null

// 生命周期
onMounted(() => {
  loadCompletedList()
  startMonitoring()
  // 自动开始待处理的下载任务
  startPendingDownloads()
})

onUnmounted(() => {
  stopMonitoring()
})

// 监听菜单切换，重新加载已完成列表
watch(activeMenu, () => {
  if (activeSubTab.value === 'completed') {
    loadCompletedList()
  }
})

// 监听下载队列变化，自动开始新任务
watch(
  () => transferStore.downloadQueue.length,
  (newLen, oldLen) => {
    if (newLen > oldLen) {
      // 有新任务添加，自动开始
      startPendingDownloads()
    }
  }
)

// 自动开始待处理的下载任务
const startPendingDownloads = () => {
  const pendingTasks = transferStore.downloadQueue.filter(t => t.status === 'pending')
  pendingTasks.forEach(task => {
    startDownloadTask(task)
  })
}

// 加载已完成列表
const loadCompletedList = async () => {
  try {
    const transferType = activeMenu.value === 'upload' ? 'UPLOAD' : 'DOWNLOAD'
    const res = await getHistoryList({ transferType })
    
    const list = (res || []).map(record => ({
      id: record.id,
      fileName: record.fileName,
      fileSize: record.fileSize,
      fileHash: record.fileHash,
      completedTime: record.completedTime,
      avgSpeed: record.avgSpeed || 0,
      selected: false
    }))
    
    if (activeMenu.value === 'upload') {
      uploadCompletedList.value = list
    } else {
      downloadCompletedList.value = list
    }
  } catch (error) {
    console.error('加载历史记录失败', error)
  }
}

// WebSocket消息处理
const handleWsEvent = (event) => {
  if (event.type === 'connected') {
    wsConnected.value = true
  } else if (event.type === 'disconnected') {
    wsConnected.value = false
  } else if (event.type === 'message' && event.data) {
    currentMetrics.value = event.data
    congestionStore.updateMetrics(event.data)
  }
}

const startMonitoring = () => {
  isMonitoring.value = true
  congestionStore.startMonitoring()
  wsUnsubscribe = monitorWs.addListener(handleWsEvent)
  monitorWs.connect()
}

const stopMonitoring = () => {
  isMonitoring.value = false
  congestionStore.stopMonitoring()
  if (wsUnsubscribe) {
    wsUnsubscribe()
    wsUnsubscribe = null
  }
  monitorWs.disconnect()
}

// 格式化百分比
const formatPercent = (value) => {
  if (value === null || value === undefined) return '0%'
  return (value * 100).toFixed(2) + '%'
}

// 获取网络质量标签类型
const getQualityType = (quality) => {
  const typeMap = {
    '优秀': 'success',
    '良好': 'primary',
    '一般': 'warning',
    '差': 'danger'
  }
  return typeMap[quality] || 'info'
}

// 获取文件图标
const getFileIcon = (fileName) => {
  const ext = fileName?.split('.').pop()?.toLowerCase() || ''
  return getFileIconByType(ext)
}

// 获取状态标签类型
const getStatusType = (status) => {
  const map = {
    'uploading': 'primary',
    'downloading': 'primary',
    'paused': 'warning',
    'waiting': 'info',
    'error': 'danger',
    'completed': 'success'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    'uploading': '上传中',
    'downloading': '下载中',
    'paused': '已暂停',
    'waiting': '等待中',
    'error': '失败',
    'completed': '已完成'
  }
  return map[status] || status
}

// 操作方法
const handlePauseAll = () => {
  if (activeMenu.value === 'upload') {
    transferStore.pauseAllUploads()
  } else {
    transferStore.pauseAllDownloads()
  }
  ElMessage.success('已全部暂停')
}

const handleStartAll = () => {
  // 开始所有待处理的下载任务
  if (activeMenu.value === 'download') {
    transferStore.downloadQueue.forEach(task => {
      if (task.status === 'pending') {
        startDownloadTask(task)
      }
    })
  }
  ElMessage.success('已全部开始')
}

const handleDeleteAll = async () => {
  try {
    await ElMessageBox.confirm('确定删除所有传输任务吗？', '提示', { type: 'warning' })
    if (activeMenu.value === 'upload') {
      transferStore.clearCompletedUploads()
    } else {
      transferStore.clearCompletedDownloads()
    }
    ElMessage.success('已删除所有任务')
  } catch {}
}

const handlePause = (item) => {
  if (activeMenu.value === 'upload') {
    transferStore.updateUploadTask(item.id, { status: 'paused', speed: 0 })
  } else {
    transferStore.updateDownloadTask(item.id, { status: 'paused', speed: 0 })
  }
  ElMessage.info(`已暂停: ${item.fileName}`)
}

const handleResume = (item) => {
  if (activeMenu.value === 'download') {
    startDownloadTask(item)
  }
  ElMessage.info(`继续: ${item.fileName}`)
}

const handleCancel = async (item) => {
  try {
    await ElMessageBox.confirm(`确定取消 "${item.fileName}" 的传输吗？`, '提示', { type: 'warning' })
    if (activeMenu.value === 'upload') {
      transferStore.removeUploadTask(item.id)
    } else {
      transferStore.removeDownloadTask(item.id)
    }
    ElMessage.success('已取消')
  } catch {}
}

// 开始下载任务
const startDownloadTask = async (task) => {
  transferStore.updateDownloadTask(task.id, { 
    status: 'downloading', 
    startTime: Date.now(),
    progress: 0
  })
  
  try {
    // 使用 fetch 获取文件并跟踪进度
    const response = await fetch(getDownloadUrl(task.fileId))
    
    if (!response.ok) {
      throw new Error('下载失败')
    }
    
    const contentLength = response.headers.get('content-length')
    const total = parseInt(contentLength, 10) || task.fileSize
    
    // 更新文件大小
    if (total && total !== task.fileSize) {
      transferStore.updateDownloadTask(task.id, { fileSize: total })
    }
    
    const reader = response.body.getReader()
    let receivedLength = 0
    const chunks = []
    const startTime = Date.now()
    
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) break
      
      chunks.push(value)
      receivedLength += value.length
      
      // 计算进度和速度
      const progress = total > 0 ? Math.round((receivedLength / total) * 100) : 0
      const elapsed = (Date.now() - startTime) / 1000
      const speed = elapsed > 0 ? Math.round(receivedLength / elapsed) : 0
      
      transferStore.updateDownloadTask(task.id, {
        progress,
        speed,
        downloadedSize: receivedLength
      })
    }
    
    // 创建 Blob 并下载
    const blob = new Blob(chunks)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = task.fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    
    // 标记完成
    await transferStore.completeDownloadTask(task.id)
    ElMessage.success(`${task.fileName} 下载完成`)
    
  } catch (error) {
    transferStore.updateDownloadTask(task.id, { 
      status: 'error', 
      error: error.message,
      speed: 0
    })
    ElMessage.error(`下载失败: ${error.message}`)
  }
}

const handleClearRecords = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有完成记录吗？', '提示', { type: 'warning' })
    await clearAllHistory()
    if (activeMenu.value === 'upload') {
      uploadCompletedList.value = []
    } else {
      downloadCompletedList.value = []
    }
    ElMessage.success('已清空')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

const handleOpenFolder = (item) => {
  ElMessage.success(`打开文件夹: ${item.fileName}`)
}

const handleOpenFile = (item) => {
  ElMessage.success(`打开文件: ${item.fileName}`)
}

const handleDeleteRecord = async (item) => {
  try {
    await deleteHistory([item.id])
    if (activeMenu.value === 'upload') {
      uploadCompletedList.value = uploadCompletedList.value.filter(i => i.id !== item.id)
    } else {
      downloadCompletedList.value = downloadCompletedList.value.filter(i => i.id !== item.id)
    }
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.transfer-center {
  display: flex;
  height: 100%;
  background: #fff;
}

/* 左侧菜单 */
.transfer-sidebar {
  width: 160px;
  background: #f7f9fc;
  border-right: 1px solid #e6e6e6;
  padding: 20px 0;
  flex-shrink: 0;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
}

.sidebar-item:hover {
  background: #ecf5ff;
  color: var(--el-color-primary);
}

.sidebar-item.active {
  background: #ecf5ff;
  color: var(--el-color-primary);
  font-weight: 500;
  border-right: 3px solid var(--el-color-primary);
}

.sidebar-item .el-icon {
  font-size: 18px;
}

/* 右侧内容区 */
.transfer-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* 顶部操作栏 */
.content-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #e6e6e6;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.toolbar-actions .el-button {
  color: #606266;
}

.toolbar-actions .el-button:hover {
  color: var(--el-color-primary);
}

/* 监控面板 */
.monitor-panel {
  padding: 16px 20px;
  background: #f7f9fc;
  border-bottom: 1px solid #e6e6e6;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.monitor-item {
  text-align: center;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
}

.monitor-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.monitor-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.monitor-value.primary { color: var(--el-color-primary); }
.monitor-value.info { color: var(--el-color-info); }
.monitor-value.success { color: var(--el-color-success); }
.monitor-value.danger { color: var(--el-color-danger); }

/* 子标签 */
.sub-tabs {
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #e6e6e6;
}

.sub-tab {
  padding: 14px 0;
  margin-right: 32px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.sub-tab:hover {
  color: var(--el-color-primary);
}

.sub-tab.active {
  color: #303133;
  font-weight: 500;
  border-bottom-color: var(--el-color-primary);
}

.sub-tab-actions {
  margin-left: auto;
}

/* 列表头部 */
.list-header {
  display: grid;
  grid-template-columns: 120px 1fr 100px 150px 100px;
  gap: 16px;
  padding: 12px 20px;
  background: #fafafa;
  font-size: 13px;
  color: #909399;
  border-bottom: 1px solid #e6e6e6;
}

/* 文件列表区域 */
.file-list-area {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}

/* 传输中列表项 */
.transfer-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.transfer-item .item-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.transfer-item .item-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.transfer-item .item-info {
  flex: 1;
  min-width: 0;
}

.transfer-item .item-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-item .item-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.transfer-item .item-progress .el-progress {
  flex: 1;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.transfer-item .item-speed {
  width: 100px;
  text-align: center;
  font-size: 13px;
  color: var(--el-color-success);
  font-weight: 500;
}

.transfer-item .item-status {
  width: 80px;
  text-align: center;
}

.transfer-item .item-actions {
  display: flex;
  gap: 4px;
}

/* 已完成列表项 */
.completed-item {
  display: grid;
  grid-template-columns: 30px 40px 1fr 100px 150px 100px;
  gap: 16px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.completed-item:hover {
  background: #fafafa;
}

.completed-item .item-icon {
  width: 40px;
  height: 40px;
}

.completed-item .item-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.completed-item .item-info {
  min-width: 0;
}

.completed-item .item-name {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.completed-item .item-size,
.completed-item .item-time {
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.completed-item .item-actions {
  display: flex;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.completed-item:hover .item-actions {
  opacity: 1;
}

/* 动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .monitor-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .transfer-sidebar {
    width: 60px;
    padding: 10px 0;
  }
  
  .sidebar-item {
    flex-direction: column;
    padding: 12px 8px;
    gap: 4px;
    font-size: 12px;
  }
  
  .sidebar-item.active {
    border-right-width: 2px;
  }
  
  .monitor-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .list-header {
    display: none;
  }
  
  .completed-item {
    grid-template-columns: 30px 40px 1fr 80px;
  }
  
  .completed-item .item-time {
    display: none;
  }
}
</style>
