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
          <el-tooltip content="暂停所有正在传输的任务" :disabled="canPause">
            <el-button text :disabled="!canPause" @click="handlePauseAll">
              <el-icon><VideoPause /></el-icon>
              全部暂停
            </el-button>
          </el-tooltip>
          <el-tooltip content="开始所有待处理的任务" :disabled="canStart">
            <el-button text :disabled="!canStart" @click="handleStartAll">
              <el-icon><VideoPlay /></el-icon>
              全部开始
            </el-button>
          </el-tooltip>
          <el-tooltip content="删除所有任务" :disabled="canDelete">
            <el-button text :disabled="!canDelete" @click="handleDeleteAll">
              <el-icon><Delete /></el-icon>
              全部删除
            </el-button>
          </el-tooltip>
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
                  title="暂停"
                  @click="handlePause(item)"
                >
                  <el-icon><VideoPause /></el-icon>
                </el-button>
                <el-button 
                  v-if="item.status === 'paused'"
                  text 
                  type="primary"
                  title="继续"
                  @click="handleResume(item)"
                >
                  <el-icon><VideoPlay /></el-icon>
                </el-button>
                <el-button 
                  v-if="item.status === 'error'"
                  text 
                  type="warning"
                  title="重试"
                  @click="handleRetry(item)"
                >
                  <el-icon><RefreshRight /></el-icon>
                </el-button>
                <el-button text type="danger" title="取消" @click="handleCancel(item)">
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
  Close, FolderOpened, Document, CircleCheck, RefreshRight
} from '@element-plus/icons-vue'
import { useCongestionStore } from '@/store/congestionStore'
import { useTransferStore } from '@/store/transferStore'
import { monitorWs } from '@/utils/websocket'
import { formatFileSize, formatSpeed } from '@/utils/format'
import { formatDateTime } from '@/utils/format'
import { getHistoryList, deleteHistory, clearAllHistory } from '@/api/historyApi'
import { getFileIconByType } from '@/utils/fileType'
import { getDownloadUrl, initUpload, uploadChunk, mergeFile, cancelUpload } from '@/api/fileApi'
import SparkMD5 from 'spark-md5'

const congestionStore = useCongestionStore()
const transferStore = useTransferStore()

// 状态
const activeMenu = ref('upload')
const activeSubTab = ref('transferring')
const showMonitor = ref(true) // 监控面板默认显示
const isMonitoring = ref(true)
const wsConnected = ref(false)

// 监控数据 - 从 congestionStore 同步
const currentMetrics = ref({
  algorithm: 'CUBIC',
  cwnd: 0,
  rate: 0,
  rtt: 0,
  lossRate: 0,
  bandwidth: 0,
  networkQuality: '-'
})

// 监听 congestionStore 的变化，实时同步算法和其他指标
watch(() => congestionStore.currentMetrics, (newMetrics) => {
  if (newMetrics) {
    currentMetrics.value = {
      ...currentMetrics.value,
      ...newMetrics,
      // 如果没有传输任务，网络质量显示"-"
      networkQuality: newMetrics.networkQuality || '-'
    }
  }
}, { deep: true, immediate: true })

// 已完成列表
const uploadCompletedList = ref([])
const downloadCompletedList = ref([])

// 计算属性 - 传输中数量（包括error状态，因为可以重试）
const transferringCount = computed(() => {
  if (activeMenu.value === 'upload') {
    return (transferStore.uploadQueue || []).filter(item => 
      item.status !== 'completed'
    ).length
  }
  return (transferStore.downloadQueue || []).filter(item => 
    item.status !== 'completed'
  ).length
})

// 计算属性 - 已完成数量
const completedCount = computed(() => {
  if (activeMenu.value === 'upload') {
    return uploadCompletedList.value.length
  }
  return downloadCompletedList.value.length
})

// 计算属性 - 传输中列表（包括error状态，因为可以重试）
const transferringList = computed(() => {
  if (activeMenu.value === 'upload') {
    return (transferStore.uploadQueue || []).filter(item => 
      item.status !== 'completed'
    )
  }
  return (transferStore.downloadQueue || []).filter(item => 
    item.status !== 'completed'
  )
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

// 计算属性 - 按钮禁用状态
// 是否可以暂停（有正在传输的任务）
const canPause = computed(() => {
  if (activeMenu.value === 'upload') {
    return transferStore.uploadQueue.some(t => 
      t.status === 'uploading' || t.status === 'hashing'
    )
  }
  return transferStore.downloadQueue.some(t => t.status === 'downloading')
})

// 是否可以开始（有暂停或待处理的任务）
const canStart = computed(() => {
  if (activeMenu.value === 'upload') {
    return transferStore.uploadQueue.some(t => 
      t.status === 'paused' || t.status === 'pending'
    )
  }
  return transferStore.downloadQueue.some(t => 
    t.status === 'paused' || t.status === 'pending'
  )
})

// 是否可以删除（有任务）
const canDelete = computed(() => {
  if (activeMenu.value === 'upload') {
    return transferStore.uploadQueue.length > 0
  }
  return transferStore.downloadQueue.length > 0
})

let wsUnsubscribe = null

// emit用于刷新文件列表
const emit = defineEmits(['refresh'])

// 生命周期
onMounted(() => {
  loadCompletedList()
  startMonitoring()
  // 自动开始待处理的任务
  startPendingDownloads()
  startPendingUploads()
})

onUnmounted(() => {
  stopMonitoring()
})

// 监听菜单切换，重新加载已完成列表（不管当前在哪个标签，都加载数据）
watch(activeMenu, () => {
  loadCompletedList()
})

// 监听子标签切换，切换到已完成时加载数据
watch(activeSubTab, (newVal) => {
  if (newVal === 'completed') {
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

// 监听上传队列变化，自动开始新任务
watch(
  () => transferStore.uploadQueue.length,
  (newLen, oldLen) => {
    if (newLen > oldLen) {
      // 有新任务添加，自动开始
      startPendingUploads()
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

// 自动开始待处理的上传任务
const startPendingUploads = () => {
  const pendingTasks = transferStore.uploadQueue.filter(t => t.status === 'pending')
  pendingTasks.forEach(task => {
    startUploadTask(task)
  })
}

// 开始上传任务（支持断点续传 + 拥塞控制动态并发）
const startUploadTask = async (task) => {
  const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB分片
  let currentCwnd = 10 * 1024 * 1024 // 初始拥塞窗口10MB
  
  try {
    // 获取当前任务的最新状态
    let currentTask = transferStore.uploadQueue.find(t => t.id === task.id)
    if (!currentTask) return
    
    // 如果是暂停或错误状态恢复，检查是否已有fileHash和fileId
    const hasExistingProgress = currentTask.fileHash && currentTask.fileId
    let fileHash = currentTask.fileHash
    
    // 1. 计算文件哈希（如果还没有计算过）
    if (!fileHash) {
      transferStore.updateUploadTask(task.id, { 
        status: 'hashing', 
        startTime: currentTask.startTime || Date.now() 
      })
      
      fileHash = await calculateFileHash(task.file, (progress) => {
        transferStore.updateUploadTask(task.id, { hashProgress: progress })
      })
      
      transferStore.updateUploadTask(task.id, { 
        fileHash, 
        hashProgress: 100 
      })
    }
    
    // 2. 初始化上传（检查秒传和已上传分片）
    const totalChunks = Math.ceil(task.file.size / CHUNK_SIZE)
    const initRes = await initUpload({
      fileName: task.fileName,
      fileSize: task.fileSize,
      fileHash: fileHash,
      folderId: task.folderId || 0,
      chunkSize: CHUNK_SIZE,
      totalChunks
    })
    
    // 秒传成功
    if (initRes.skipUpload || initRes.quickUpload) {
      transferStore.updateUploadTask(task.id, { 
        fileId: initRes.fileId,
        status: 'completed',
        progress: 100,
        endTime: Date.now()
      })
      await transferStore.completeUploadTask(task.id, fileHash)
      ElMessage.success(`${task.fileName} 秒传成功`)
      emit('refresh')
      // 刷新已完成列表
      loadCompletedList()
      return
    }
    
    // 3. 获取已上传的分片（用于断点续传）
    // 后端返回的 uploaded 数组包含已上传的分片索引（从0开始）
    const serverUploadedChunks = initRes.uploaded || []
    const uploadedSet = new Set(serverUploadedChunks)
    
    // 计算已上传的大小
    let uploadedSize = 0
    serverUploadedChunks.forEach(chunkIndex => {
      const start = chunkIndex * CHUNK_SIZE  // 修复：chunkIndex从0开始
      const end = Math.min(start + CHUNK_SIZE, task.fileSize)
      uploadedSize += (end - start)
    })
    
    // 更新任务状态
    transferStore.updateUploadTask(task.id, { 
      status: 'uploading',
      fileId: initRes.fileId,
      totalChunks,
      uploadedChunks: [...serverUploadedChunks],
      uploadedSize,
      progress: Math.round((uploadedSize / task.fileSize) * 100)
    })
    
    const startTime = currentTask.startTime || Date.now()
    
    // 4. 构建待上传分片列表（跳过已上传的）
    const pendingChunks = []
    for (let i = 0; i < totalChunks; i++) {
      if (!uploadedSet.has(i)) {  // 修复：分片编号从0开始
        pendingChunks.push(i)
      }
    }
    
    // 5. 并发上传分片（根据拥塞窗口动态调整并发数）
    let chunkIndex = 0
    
    while (chunkIndex < pendingChunks.length) {
      // 检查任务状态（暂停或取消检测）
      currentTask = transferStore.uploadQueue.find(t => t.id === task.id)
      if (!currentTask || currentTask.status === 'paused' || currentTask.status === 'error') {
        console.log(`任务 ${task.fileName} 已暂停或出错，停止上传`)
        return
      }
      
      // 根据拥塞窗口计算最大并发数（cwnd / 分片大小，最少1个，最多6个）
      const maxConcurrent = Math.max(1, Math.min(6, Math.floor(currentCwnd / CHUNK_SIZE)))
      console.log(`拥塞控制 - cwnd: ${(currentCwnd / 1024 / 1024).toFixed(2)}MB, 并发数: ${maxConcurrent}`)
      
      // 取当前批次要上传的分片
      const batchSize = Math.min(maxConcurrent, pendingChunks.length - chunkIndex)
      const batch = pendingChunks.slice(chunkIndex, chunkIndex + batchSize)
      
      // 并发上传当前批次
      const uploadPromises = batch.map(async (i) => {
        const start = i * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, task.file.size)
        const chunk = task.file.slice(start, end)
        
        const formData = new FormData()
        formData.append('file', chunk)
        formData.append('fileId', initRes.fileId)
        formData.append('chunkNumber', i)  // 修复：分片编号从0开始，与后端一致
        formData.append('totalChunks', totalChunks)
        formData.append('chunkHash', fileHash)
        
        const result = await uploadChunk(formData, () => {})
        
        // 后端返回了新的拥塞窗口大小，动态调整
        if (result && result.cwnd) {
          // 确保cwnd值合理（至少1MB），避免异常小的值导致并发数过低
          const receivedCwnd = Number(result.cwnd)
          if (receivedCwnd > 0 && receivedCwnd < 1024 * 1024) {
            console.warn(`收到异常的cwnd值: ${receivedCwnd}字节，使用最小值1MB`)
            currentCwnd = Math.max(currentCwnd, 1024 * 1024) // 至少保持1MB
          } else {
            currentCwnd = receivedCwnd
          }
          console.log(`分片${i}上传完成 - 收到cwnd: ${receivedCwnd}字节, 当前cwnd: ${currentCwnd}字节`)
        }
        
        // 从上传响应中更新metrics数据
        if (result && result.rtt !== undefined) {
          const metrics = {
            algorithm: currentMetrics.value.algorithm || 'CUBIC',
            cwnd: result.cwnd || currentMetrics.value.cwnd || 0,
            rtt: result.rtt || 0,
            lossRate: currentMetrics.value.lossRate || 0
          }
          currentMetrics.value = { ...currentMetrics.value, ...metrics }
          congestionStore.updateMetrics(metrics)
        }
        
        return { index: i, size: chunk.size, result }
      })
      
      // 等待当前批次完成
      const results = await Promise.all(uploadPromises)
      
      // 更新进度
      results.forEach(({ index, size }) => {
        uploadedSize += size
        uploadedSet.add(index)  // 修复：分片编号从0开始
      })
      
      const elapsed = (Date.now() - startTime) / 1000
      const speed = elapsed > 0 ? Math.round(uploadedSize / elapsed) : 0
      const progress = Math.round((uploadedSize / task.fileSize) * 100)
      
      transferStore.updateUploadTask(task.id, {
        progress,
        speed,
        uploadedSize,
        uploadedChunks: [...uploadedSet]
      })
      
      chunkIndex += batchSize
    }
    
    // 5. 合并文件
    await mergeFile({
      fileId: initRes.fileId,
      fileName: task.fileName,
      fileHash: fileHash,
      totalChunks
    })
    
    // 6. 完成
    await transferStore.completeUploadTask(task.id, fileHash)
    ElMessage.success(`${task.fileName} 上传完成`)
    emit('refresh')
    // 刷新已完成列表
    loadCompletedList()
    
  } catch (error) {
    console.error('上传失败:', error)
    
    // 不清理后端数据，保留用于重试（断点续传）
    // 只有在用户明确取消时才清理
    
    // 使用 failUploadTask 记录失败历史
    await transferStore.failUploadTask(task.id, error.message || '上传失败')
    ElMessage.error(`${task.fileName} 上传失败: ${error.message}，可点击重试`)
    // 刷新已完成列表（记录失败历史）
    loadCompletedList()
  }
}

// 计算文件哈希
const calculateFileHash = (file, onProgress) => {
  return new Promise((resolve, reject) => {
    const chunkSize = 2 * 1024 * 1024 // 2MB chunks for hashing
    const chunks = Math.ceil(file.size / chunkSize)
    let currentChunk = 0
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    
    reader.onload = (e) => {
      spark.append(e.target.result)
      currentChunk++
      
      if (onProgress) {
        onProgress(Math.round((currentChunk / chunks) * 100))
      }
      
      if (currentChunk < chunks) {
        loadNext()
      } else {
        resolve(spark.end())
      }
    }
    
    reader.onerror = () => {
      reject(new Error('文件读取失败'))
    }
    
    const loadNext = () => {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      reader.readAsArrayBuffer(file.slice(start, end))
    }
    
    loadNext()
  })
}

// 加载已完成列表
const loadCompletedList = async () => {
  try {
    const transferType = activeMenu.value === 'upload' ? 'UPLOAD' : 'DOWNLOAD'
    console.log('加载历史记录, transferType:', transferType)
    const res = await getHistoryList({ transferType })
    console.log('历史记录API返回:', res)
    
    const list = (res || []).map(record => ({
      id: record.id,
      fileName: record.fileName,
      fileSize: record.fileSize,
      fileHash: record.fileHash,
      completedTime: record.completedTime,
      avgSpeed: record.avgSpeed || 0,
      selected: false
    }))
    
    console.log('处理后的列表:', list)
    
    if (activeMenu.value === 'upload') {
      uploadCompletedList.value = list
      console.log('设置上传完成列表:', uploadCompletedList.value)
    } else {
      downloadCompletedList.value = list
      console.log('设置下载完成列表:', downloadCompletedList.value)
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
    'pending': 'info',
    'hashing': 'primary',
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
    'pending': '等待中',
    'hashing': '计算MD5',
    'error': '失败(可重试)',
    'completed': '已完成'
  }
  return map[status] || status
}

// 操作方法
const handlePauseAll = () => {
  if (!canPause.value) {
    ElMessage.warning('没有正在传输的任务')
    return
  }
  if (activeMenu.value === 'upload') {
    transferStore.pauseAllUploads()
  } else {
    transferStore.pauseAllDownloads()
  }
  ElMessage.success('已全部暂停')
}

const handleStartAll = () => {
  if (!canStart.value) {
    ElMessage.warning('没有待开始的任务')
    return
  }
  if (activeMenu.value === 'upload') {
    // 开始所有待处理的上传任务
    transferStore.uploadQueue.forEach(task => {
      if (task.status === 'pending' || task.status === 'paused') {
        startUploadTask(task)
      }
    })
  } else {
    // 开始所有待处理的下载任务
    transferStore.downloadQueue.forEach(task => {
      if (task.status === 'pending' || task.status === 'paused') {
        startDownloadTask(task)
      }
    })
  }
  ElMessage.success('已全部开始')
}

const handleDeleteAll = async () => {
  if (!canDelete.value) {
    ElMessage.warning('没有可删除的任务')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除所有传输任务吗？', '提示', { type: 'warning' })
    if (activeMenu.value === 'upload') {
      // 清空上传队列中的所有任务
      transferStore.uploadQueue.splice(0, transferStore.uploadQueue.length)
    } else {
      // 清空下载队列中的所有任务
      transferStore.downloadQueue.splice(0, transferStore.downloadQueue.length)
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
  if (activeMenu.value === 'upload') {
    // 从断点继续上传
    startUploadTask(item)
  } else {
    // 从断点继续下载
    startDownloadTask(item)
  }
  ElMessage.info(`继续: ${item.fileName}`)
}

// 重试失败的任务
const handleRetry = (item) => {
  if (activeMenu.value === 'upload') {
    if (transferStore.retryUploadTask(item.id)) {
      startUploadTask(item)
      ElMessage.info(`重试上传: ${item.fileName}`)
    }
  } else {
    if (transferStore.retryDownloadTask(item.id)) {
      startDownloadTask(item)
      ElMessage.info(`重试下载: ${item.fileName}`)
    }
  }
}

const handleCancel = async (item) => {
  try {
    await ElMessageBox.confirm(`确定取消 "${item.fileName}" 的传输吗？取消后无法恢复。`, '提示', { type: 'warning' })
    if (activeMenu.value === 'upload') {
      // 如果有fileId，清理后端数据
      if (item.fileId) {
        try {
          await cancelUpload(item.fileId)
          console.log('已清理后端上传数据')
        } catch (cleanupError) {
          console.error('清理上传数据失败:', cleanupError)
        }
      }
      transferStore.removeUploadTask(item.id)
    } else {
      transferStore.removeDownloadTask(item.id)
    }
    ElMessage.success('已取消')
  } catch {}
}

// 开始下载任务（支持断点续传）
const startDownloadTask = async (task) => {
  // 获取当前任务状态
  let currentTask = transferStore.downloadQueue.find(t => t.id === task.id)
  if (!currentTask) return
  
  // 获取已下载的字节数（用于断点续传）
  const downloadedSize = currentTask.downloadedSize || 0
  const existingChunks = currentTask.downloadedChunks || []
  
  transferStore.updateDownloadTask(task.id, { 
    status: 'downloading', 
    startTime: currentTask.startTime || Date.now()
  })
  
  try {
    // 获取token用于认证
    const token = localStorage.getItem('token')
    
    // 构建请求头（支持Range请求进行断点续传）
    const headers = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    // 如果有已下载的数据，使用Range请求从断点继续
    if (downloadedSize > 0) {
      headers['Range'] = `bytes=${downloadedSize}-`
    }
    
    // 使用 fetch 获取文件并跟踪进度
    const response = await fetch(getDownloadUrl(task.fileId), { headers })
    
    if (!response.ok && response.status !== 206) {
      if (response.status === 404) {
        throw new Error('文件不存在或已被删除')
      } else if (response.status === 401) {
        throw new Error('未授权，请重新登录')
      } else if (response.status === 416) {
        // Range不满足，可能文件已下载完成或文件变化
        throw new Error('文件已变化，请重新下载')
      } else {
        throw new Error(`下载失败 (${response.status})`)
      }
    }
    
    // 获取文件总大小
    const contentLength = response.headers.get('content-length')
    const contentRange = response.headers.get('content-range')
    let total = task.fileSize
    
    if (contentRange) {
      // 断点续传响应: bytes 0-999/1000
      const match = contentRange.match(/\/(\d+)$/)
      if (match) {
        total = parseInt(match[1], 10)
      }
    } else if (contentLength) {
      total = parseInt(contentLength, 10) + downloadedSize
    }
    
    // 更新文件大小
    if (total && total !== task.fileSize) {
      transferStore.updateDownloadTask(task.id, { fileSize: total })
    }
    
    const reader = response.body.getReader()
    let receivedLength = downloadedSize // 从已下载的位置继续计算
    const chunks = [...existingChunks] // 保留已下载的数据
    const startTime = currentTask.startTime || Date.now()
    
    while (true) {
      // 检查任务状态（暂停检测）
      currentTask = transferStore.downloadQueue.find(t => t.id === task.id)
      if (!currentTask || currentTask.status === 'paused') {
        console.log(`下载任务 ${task.fileName} 已暂停`)
        return
      }
      
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
        downloadedSize: receivedLength,
        downloadedChunks: chunks // 保存已下载的数据块用于断点续传
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
    // 刷新已完成列表
    loadCompletedList()
    
  } catch (error) {
    // 使用 failDownloadTask 记录失败历史
    await transferStore.failDownloadTask(task.id, error.message)
    ElMessage.error(`下载失败: ${error.message}，可点击重试`)
    // 失败也刷新列表（记录失败历史）
    loadCompletedList()
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
