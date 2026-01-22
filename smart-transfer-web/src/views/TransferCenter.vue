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
import { getDownloadUrl, initUpload, uploadChunk, mergeFile, cancelUpload, initDownload, downloadChunk, completeDownload, cancelDownload } from '@/api/fileApi'
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
    // **修复ISSUE-2: 合并前端和服务器端的已上传分片记录（确保不丢失进度）**
    const serverUploadedChunks = initRes.uploaded || []
    const frontendUploadedChunks = currentTask.uploadedChunks || []
    // 合并两个集合，确保不丢失任何已上传的分片
    const uploadedSet = new Set([...serverUploadedChunks, ...frontendUploadedChunks])
    
    // **修复ISSUE-2-补充: 计算已上传的大小（使用合并后的uploadedSet）**
    let uploadedSize = 0
    uploadedSet.forEach(chunkIndex => {
      const start = chunkIndex * CHUNK_SIZE  // 修复：chunkIndex从0开始
      const end = Math.min(start + CHUNK_SIZE, task.fileSize)
      uploadedSize += (end - start)
    })
    
    // 更新任务状态
    transferStore.updateUploadTask(task.id, { 
      status: 'uploading',
      fileId: initRes.fileId,
      totalChunks,
      uploadedChunks: [...uploadedSet],  // **修复ISSUE-2-补充: 保存合并后的分片列表**
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
    // **改进：使用动态队列，每个分片完成后立即更新并发数并开始新的上传**
    const uploadQueue = [...pendingChunks] // 待上传队列
    const activeUploads = new Map() // 正在上传的分片 (index -> Promise)
    let completedCount = uploadedSet.size
    
    // **修复CRITICAL-4: 创建AbortController用于取消上传请求（与下载任务保持一致）**
    const abortController = new AbortController()
    
    // **修复CRITICAL-4: 将清理资源存储到任务对象，以便取消/暂停时调用**
    if (!task._uploadCleanup) {
      task._uploadCleanup = {
        abortController,
        activeUploads,
        uploadQueue
      }
    } else {
      // 如果已存在，更新引用
      task._uploadCleanup.abortController = abortController
      task._uploadCleanup.activeUploads = activeUploads
      task._uploadCleanup.uploadQueue = uploadQueue
    }
    
    // 动态控制并发上传
    async function startNextChunk() {
      // 检查任务状态
      currentTask = transferStore.uploadQueue.find(t => t.id === task.id)
      if (!currentTask || currentTask.status === 'paused' || currentTask.status === 'error') {
        console.log(`任务 ${task.fileName} 已暂停或出错，停止上传`)
        return
      }
      
      // 如果队列为空且没有正在上传的，上传完成
      if (uploadQueue.length === 0 && activeUploads.size === 0) {
        return
      }
      
      // 如果队列为空，等待正在上传的完成
      if (uploadQueue.length === 0) {
        return
      }
      
      // **改进：根据当前cwnd动态计算并发数（每个分片完成后立即重新计算）**
      const maxConcurrent = Math.max(1, Math.min(6, Math.floor(currentCwnd / CHUNK_SIZE)))
      
      // 如果当前活跃数未达到最大并发数，启动新的上传
      while (activeUploads.size < maxConcurrent && uploadQueue.length > 0) {
        const i = uploadQueue.shift()
        
        // 创建上传Promise
        const uploadPromise = (async () => {
          try {
            const start = i * CHUNK_SIZE
            const end = Math.min(start + CHUNK_SIZE, task.file.size)
            const chunk = task.file.slice(start, end)
            
            const formData = new FormData()
            formData.append('file', chunk)
            formData.append('fileId', initRes.fileId)
            formData.append('chunkNumber', i)
            formData.append('totalChunks', totalChunks)
            formData.append('chunkHash', fileHash)
            
            // **修复CRITICAL-4: 传递AbortSignal以支持取消上传**
            const result = await uploadChunk(formData, () => {}, 3, abortController.signal)
            
            // **改进：每个分片完成后立即更新cwnd和并发数**
            if (result && result.cwnd) {
              const receivedCwnd = Number(result.cwnd)
              if (receivedCwnd > 0 && receivedCwnd < 1024 * 1024) {
                console.warn(`收到异常的cwnd值: ${receivedCwnd}字节，使用最小值1MB`)
                currentCwnd = Math.max(currentCwnd, 1024 * 1024)
              } else {
                currentCwnd = receivedCwnd
              }
              console.log(`分片${i}上传完成 - cwnd: ${(currentCwnd / 1024 / 1024).toFixed(2)}MB, 新并发数: ${Math.max(1, Math.min(6, Math.floor(currentCwnd / CHUNK_SIZE)))}`)
            }
            
            // 更新metrics数据
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
            
            // 更新进度
            completedCount++
            uploadedSize += chunk.size
            uploadedSet.add(i)
            
            const elapsed = (Date.now() - startTime) / 1000
            const speed = elapsed > 0 ? Math.round(uploadedSize / elapsed) : 0
            const progress = Math.round((uploadedSize / task.fileSize) * 100)
            
            transferStore.updateUploadTask(task.id, {
              progress,
              speed,
              uploadedSize,
              uploadedChunks: [...uploadedSet]
            })
            
            return { index: i, size: chunk.size, result }
          } catch (error) {
            console.error(`分片${i}上传失败:`, error)
            // 重新加入队列重试
            uploadQueue.unshift(i)
            throw error
          } finally {
            // 从活跃队列移除
            activeUploads.delete(i)
            // **关键：分片完成后立即尝试启动新的上传（使用新的cwnd计算并发数）**
            startNextChunk()
          }
        })()
        
        activeUploads.set(i, uploadPromise)
      }
    }
    
    // 启动初始并发上传
    await startNextChunk()
    
    // 等待所有上传完成
    while (activeUploads.size > 0 || uploadQueue.length > 0) {
      // 等待至少一个上传完成
      if (activeUploads.size > 0) {
        await Promise.race(Array.from(activeUploads.values()))
      } else {
        // 如果没有正在上传的，但有队列中的，继续启动
        await startNextChunk()
      }
      
      // 检查任务状态
      currentTask = transferStore.uploadQueue.find(t => t.id === task.id)
      if (!currentTask || currentTask.status === 'paused' || currentTask.status === 'error') {
        console.log(`任务 ${task.fileName} 已暂停或出错，停止上传`)
        // **修复CRITICAL-4: 任务暂停/取消时取消所有正在进行的请求**
        abortController.abort()
        break
      }
    }
    
    // **修复CRITICAL-5: 验证所有分片是否已上传完成（使用uploadedSet而不是completedCount）**
    if (uploadedSet.size < totalChunks) {
      console.warn(`分片未全部上传 - 已完成: ${uploadedSet.size}/${totalChunks}`)
      throw new Error(`分片未全部上传，已完成 ${uploadedSet.size}/${totalChunks}，请重试`)
    }
    
    // 5. 合并文件
    // **修复CRITICAL-3: 检查合并是否成功，失败则抛出错误**
    const mergeRes = await mergeFile({
      fileId: initRes.fileId,
      fileName: task.fileName,
      fileHash: fileHash,
      totalChunks
    })
    
    if (!mergeRes.success) {
      throw new Error(mergeRes.message || '文件合并失败')
    }
    
    // 6. 完成
    await transferStore.completeUploadTask(task.id, fileHash)
    
    // **修复CRITICAL-4: 上传完成时清理任务对象上的清理函数引用**
    if (task._uploadCleanup) {
      delete task._uploadCleanup
    }
    
    ElMessage.success(`${task.fileName} 上传完成`)
    emit('refresh')
    // 刷新已完成列表
    loadCompletedList()
    
  } catch (error) {
    console.error('上传失败:', error)
    
    // **修复CRITICAL-4: 上传失败时清理任务对象上的清理函数引用**
    if (task._uploadCleanup) {
      delete task._uploadCleanup
    }
    
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
// **改进：支持按任务分别推送指标**
const handleWsEvent = (event) => {
  if (event.type === 'connected') {
    wsConnected.value = true
  } else if (event.type === 'disconnected') {
    wsConnected.value = false
  } else if (event.type === 'message' && event.data) {
    const data = event.data
    
    // **改进：支持新的按任务推送格式**
    if (data.type === 'metrics' && data.tasks) {
      // 新格式：按任务分别推送
      // data.tasks 是一个Map，key是taskId，value是CongestionMetricsVO
      const taskMetricsMap = data.tasks
      
      // 如果有活跃的上传任务，使用第一个任务的指标（或可以按任务ID匹配）
      const activeUploadTask = transferStore.uploadQueue.find(t => 
        t.status === 'uploading' && taskMetricsMap[t.taskId]
      )
      
      if (activeUploadTask && taskMetricsMap[activeUploadTask.taskId]) {
        // 使用当前活跃任务的指标
        const taskMetrics = taskMetricsMap[activeUploadTask.taskId]
        currentMetrics.value = taskMetrics
        congestionStore.updateMetrics(taskMetrics)
      } else if (Object.keys(taskMetricsMap).length > 0) {
        // 如果没有活跃任务，使用第一个任务的指标
        const firstTaskId = Object.keys(taskMetricsMap)[0]
        const taskMetrics = taskMetricsMap[firstTaskId]
        currentMetrics.value = taskMetrics
        congestionStore.updateMetrics(taskMetrics)
      }
    } else {
      // 兼容旧格式：单个指标对象
      currentMetrics.value = data
      congestionStore.updateMetrics(data)
    }
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
    // **修复CRITICAL-4: 上传暂停时取消所有正在进行的请求并清理前端资源**
    if (item._uploadCleanup) {
      // 取消所有正在进行的上传请求
      if (item._uploadCleanup.abortController) {
        item._uploadCleanup.abortController.abort()
        console.log('已取消所有正在进行的上传请求')
      }
      // 清理前端资源（但不删除uploadedSet，以便恢复时继续）
      if (item._uploadCleanup.activeUploads) {
        item._uploadCleanup.activeUploads.clear()
      }
      // 注意：不清理uploadedSet，以便恢复时继续
    }
    transferStore.updateUploadTask(item.id, { status: 'paused', speed: 0 })
  } else {
    // **修复P3: 下载暂停时取消所有正在进行的请求并清理前端资源**
    if (item._downloadCleanup) {
      // 取消所有正在进行的下载请求
      if (item._downloadCleanup.abortController) {
        item._downloadCleanup.abortController.abort()
        console.log('已取消所有正在进行的下载请求')
      }
      // 清理前端资源（但不删除chunkDataMap，以便恢复时继续）
      if (item._downloadCleanup.activeDownloads) {
        item._downloadCleanup.activeDownloads.clear()
      }
      // 注意：不清理chunkDataMap，以便恢复时继续
    }
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
      // **修复CRITICAL-4: 上传取消时取消所有正在进行的请求并清理前端资源**
      if (item._uploadCleanup) {
        // 取消所有正在进行的上传请求
        if (item._uploadCleanup.abortController) {
          item._uploadCleanup.abortController.abort()
          console.log('已取消所有正在进行的上传请求')
        }
        // 清理前端资源
        if (item._uploadCleanup.activeUploads) {
          item._uploadCleanup.activeUploads.clear()
        }
        // 删除清理函数引用
        delete item._uploadCleanup
      }
      
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
      // **修复M1: 下载取消时也清理后端资源（Redis、算法实例）**
      // **修复P2: 下载取消时取消所有正在进行的请求并清理前端资源**
      if (item._downloadCleanup) {
        // 取消所有正在进行的下载请求
        if (item._downloadCleanup.abortController) {
          item._downloadCleanup.abortController.abort()
          console.log('已取消所有正在进行的下载请求')
        }
        // 清理前端资源
        if (item._downloadCleanup.activeDownloads) {
          item._downloadCleanup.activeDownloads.clear()
        }
        if (item._downloadCleanup.chunkDataMap) {
          item._downloadCleanup.chunkDataMap.clear()
        }
        // 删除清理函数引用
        delete item._downloadCleanup
      }
      
      if (item.taskId) {
        try {
          await cancelDownload(item.taskId)
          console.log('已清理后端下载数据')
        } catch (cleanupError) {
          console.error('清理下载数据失败:', cleanupError)
          // 即使清理失败也继续取消任务
        }
      }
      transferStore.removeDownloadTask(item.id)
    }
    ElMessage.success('已取消')
  } catch {}
}

// 开始下载任务（支持断点续传 + 拥塞控制分块下载）
const startDownloadTask = async (task) => {
  // 获取当前任务状态
  let currentTask = transferStore.downloadQueue.find(t => t.id === task.id)
  if (!currentTask) return
  
  const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB分块
  let currentCwnd = 10 * 1024 * 1024 // 初始拥塞窗口10MB
  
  transferStore.updateDownloadTask(task.id, { 
    status: 'downloading', 
    startTime: currentTask.startTime || Date.now()
  })
  
  try {
    // 1. 初始化下载（获取分块信息）
    const initRes = await initDownload(task.fileId, CHUNK_SIZE)
    if (!initRes.success) {
      throw new Error(initRes.message || '初始化下载失败')
    }
    
    const { totalChunks, chunkSize, taskId, downloaded } = initRes.data
    const downloadedSet = new Set(downloaded || [])
    
    // 2. 计算待下载的分块
    const pendingChunks = []
    for (let i = 0; i < totalChunks; i++) {
      if (!downloadedSet.has(i)) {
        pendingChunks.push(i)
      }
    }
    
    if (pendingChunks.length === 0) {
      // **修复C3: 空文件或所有分块已下载，直接完成（也调用completeDownload清理资源）**
      transferStore.updateDownloadTask(task.id, { 
        status: 'completed', 
        progress: 100,
        speed: 0
      })
      
      // **修复C3: 空文件完成时也调用completeDownload清理后端资源**
      try {
        if (taskId) {
          await completeDownload(taskId)
          console.log('空文件下载完成，已清理后端资源')
        }
      } catch (error) {
        console.error('标记空文件下载完成失败:', error)
      }
      
      ElMessage.success(`${task.fileName} 下载完成`)
      loadCompletedList()
      return
    }
    
    // 3. **修复ISSUE-1: 复用已有的chunkDataMap（如果任务从暂停恢复，保留已下载的分块）**
    const existingChunkDataMap = task._downloadCleanup?.chunkDataMap || new Map()
    const chunkDataMap = existingChunkDataMap // 复用已有的Map，不创建新的
    
    // **修复ISSUE-5: 合并chunkDataMap和downloadedSet计算已下载大小**
    let downloadedSize = 0
    
    // 从chunkDataMap计算已下载大小（已下载到内存的分块）
    chunkDataMap.forEach((chunkData, chunkNumber) => {
      downloadedSize += chunkData.length
      // 同步到downloadedSet（确保进度一致性）
      downloadedSet.add(chunkNumber)
    })
    
    // 从downloadedSet补充（服务器端已确认但chunkDataMap中没有的）
    downloadedSet.forEach(chunkNumber => {
      if (!chunkDataMap.has(chunkNumber)) {
        const start = chunkNumber * chunkSize
        const end = Math.min(start + chunkSize - 1, task.fileSize - 1)
        downloadedSize += (end - start + 1)
      }
    })
    
    // 重新计算待下载的分块（排除已下载的）
    const pendingChunksAfterRecovery = []
    for (let i = 0; i < totalChunks; i++) {
      if (!downloadedSet.has(i)) {
        pendingChunksAfterRecovery.push(i)
      }
    }
    
    const startTime = currentTask.startTime || Date.now()
    
    // 4. **改进：使用动态队列，每个分块完成后立即更新并发数**
    const downloadQueue = [...pendingChunksAfterRecovery]  // 使用重新计算后的队列
    const activeDownloads = new Map() // 正在下载的分块 (chunkNumber -> Promise)
    
    // **修复CRITICAL-1: 创建AbortController用于取消下载请求（必须在使用前创建）**
    const abortController = new AbortController()
    
    // **修复M3: 为每个分块添加重试次数计数器（防止无限重试）**
    const chunkRetryCount = new Map() // chunkNumber -> retryCount
    const MAX_CHUNK_RETRIES = 3 // 每个分块最大重试次数
    
    // **修复ISSUE-1: 将AbortController存储到任务对象，复用chunkDataMap**
    if (!task._downloadCleanup) {
      task._downloadCleanup = {
        abortController,
        activeDownloads,
        chunkDataMap,  // 使用复用的Map
        downloadQueue
      }
    } else {
      // 如果已存在，更新引用（但保留chunkDataMap）
      task._downloadCleanup.abortController = abortController
      task._downloadCleanup.activeDownloads = activeDownloads
      // ❌ 不要覆盖 chunkDataMap，保留已下载的数据
      // task._downloadCleanup.chunkDataMap = chunkDataMap  // 删除这行
      task._downloadCleanup.downloadQueue = downloadQueue
    }
    
    // 动态控制并发下载
    async function startNextChunk() {
      // 检查任务状态
      currentTask = transferStore.downloadQueue.find(t => t.id === task.id)
      if (!currentTask || currentTask.status === 'paused' || currentTask.status === 'error') {
        console.log(`下载任务 ${task.fileName} 已暂停或出错`)
        return
      }
      
      if (downloadQueue.length === 0 && activeDownloads.size === 0) {
        return
      }
      
      if (downloadQueue.length === 0) {
        return
      }
      
      // **改进：根据当前cwnd动态计算并发数**
      const maxConcurrent = Math.max(1, Math.min(6, Math.floor(currentCwnd / CHUNK_SIZE)))
      
      // 如果当前活跃数未达到最大并发数，启动新的下载
      while (activeDownloads.size < maxConcurrent && downloadQueue.length > 0) {
        const chunkNumber = downloadQueue.shift()
        
        // **修复P2-3：防止重复下载同一分块**
        if (activeDownloads.has(chunkNumber)) {
          console.warn(`分块${chunkNumber}正在下载中，跳过`)
          continue  // 继续处理下一个分块，而不是直接return
        }
        
        // 创建下载Promise
        const downloadPromise = (async () => {
          try {
            const startByte = chunkNumber * chunkSize
            const endByte = Math.min(startByte + chunkSize - 1, task.fileSize - 1)
            
            // **优化：使用二进制流传输，从响应头获取元数据**
            // **修复P2/P3: 支持请求取消（通过AbortController）**
            const response = await downloadChunk(task.fileId, chunkNumber, startByte, endByte, abortController.signal)
            
            // **修复P3: 在下载完成后检查任务状态，如果被暂停/取消则中断处理**
            currentTask = transferStore.downloadQueue.find(t => t.id === task.id)
            if (!currentTask || currentTask.status === 'paused' || currentTask.status === 'error') {
              throw new Error('任务已暂停或取消')
            }
            
            // 从响应头获取元数据（axios会将响应头转换为小写）
            const headers = response.headers
            const getHeader = (name) => {
              // axios会将响应头转换为小写，但尝试多种可能
              const lowerName = name.toLowerCase()
              return headers[lowerName] || headers[name] || headers[name.toUpperCase()] || ''
            }
            
            const success = getHeader('x-success') === 'true'
            
            if (success) {
              // **改进：每个分块完成后立即更新cwnd和并发数**
              // **修复P5: 增强响应头解析错误处理，防止NaN**
              const receivedCwndStr = getHeader('x-cwnd') || '0'
              const receivedCwnd = parseInt(receivedCwndStr) || 0
              if (isNaN(receivedCwnd)) {
                console.warn(`收到无效的cwnd值: "${receivedCwndStr}", 保持当前cwnd`)
              } else if (receivedCwnd > 0 && receivedCwnd < 1024 * 1024) {
                console.warn(`收到异常的cwnd值: ${receivedCwnd}字节，使用最小值1MB`)
                currentCwnd = Math.max(currentCwnd, 1024 * 1024)
              } else if (receivedCwnd > 0) {
                currentCwnd = receivedCwnd
              }
              console.log(`分块${chunkNumber}下载完成 - cwnd: ${(currentCwnd / 1024 / 1024).toFixed(2)}MB, 新并发数: ${Math.max(1, Math.min(6, Math.floor(currentCwnd / CHUNK_SIZE)))}`)
              
              // **优化：直接使用二进制数据（无需Base64解码）**
              const chunkData = new Uint8Array(response.data)  // 直接使用二进制数据
              chunkDataMap.set(chunkNumber, chunkData)  // 存储Uint8Array
              downloadedSet.add(chunkNumber)
              downloadedSize += (endByte - startByte + 1)
              
              // 从响应头获取进度
              // **修复P5: 增强响应头解析错误处理，防止NaN**
              const progressStr = getHeader('x-progress') || '0'
              const progress = parseFloat(progressStr) || 0
              if (isNaN(progress)) {
                console.warn(`收到无效的进度值: "${progressStr}", 使用0`)
              }
              const elapsed = (Date.now() - startTime) / 1000
              const speed = elapsed > 0 ? Math.round(downloadedSize / elapsed) : 0
              
              transferStore.updateDownloadTask(task.id, {
                progress: Math.round(progress),
                speed,
                downloadedSize,
                downloadedChunks: [...downloadedSet]
              })
              
              return { chunkNumber, success: true }
            } else {
              // 下载失败，重新加入队列重试
              const errorMessage = getHeader('x-error-message') || '下载分块失败'
              downloadQueue.unshift(chunkNumber)
              throw new Error(errorMessage)
            }
          } catch (error) {
            console.error(`分块${chunkNumber}下载失败:`, error)
            
            // **修复P2/P3: 如果是取消错误，不重试，直接退出**
            if (error.name === 'AbortError' || error.message?.includes('取消') || error.message?.includes('Abort')) {
              console.log(`分块${chunkNumber}下载已取消`)
              throw error
            }
            
            // **修复P3: 如果任务已暂停/取消，不重试**
            currentTask = transferStore.downloadQueue.find(t => t.id === task.id)
            if (!currentTask || currentTask.status === 'paused' || currentTask.status === 'error') {
              console.log(`分块${chunkNumber}下载已暂停或取消，不重试`)
              throw error
            }
            
            // **修复M3: 检查重试次数，超过最大重试次数则标记失败**
            const retryCount = chunkRetryCount.get(chunkNumber) || 0
            if (retryCount >= MAX_CHUNK_RETRIES) {
              console.error(`分块${chunkNumber}重试次数已达上限(${MAX_CHUNK_RETRIES})，标记下载失败`)
              transferStore.updateDownloadTask(task.id, {
                status: 'error',
                error: `分块${chunkNumber}下载失败，已重试${MAX_CHUNK_RETRIES}次`
              })
              throw new Error(`分块${chunkNumber}下载失败，已重试${MAX_CHUNK_RETRIES}次`)
            }
            
            // **修复P2-3：下载失败时，如果已在activeDownloads中，需要确保清理**
            // **修复M3: 增加重试次数并重新加入队列重试（但要避免重复）**
            chunkRetryCount.set(chunkNumber, retryCount + 1)
            if (!downloadQueue.includes(chunkNumber)) {
              downloadQueue.unshift(chunkNumber)
              console.warn(`分块${chunkNumber}下载失败，${(retryCount + 1)}/${MAX_CHUNK_RETRIES}次重试`)
            }
            throw error
          } finally {
            // 从活跃队列移除
            activeDownloads.delete(chunkNumber)
            // **关键：分块完成后立即尝试启动新的下载（使用新的cwnd计算并发数）**
            startNextChunk()
          }
        })()
        
        activeDownloads.set(chunkNumber, downloadPromise)
      }
    }
    
    // 启动初始并发下载
    await startNextChunk()
    
    // 等待所有下载完成
    while (activeDownloads.size > 0 || downloadQueue.length > 0) {
      if (activeDownloads.size > 0) {
        await Promise.race(Array.from(activeDownloads.values()))
      } else {
        await startNextChunk()
      }
      
          // 检查任务状态
          currentTask = transferStore.downloadQueue.find(t => t.id === task.id)
          if (!currentTask || currentTask.status === 'paused' || currentTask.status === 'error') {
            console.log(`下载任务 ${task.fileName} 已暂停或出错`)
            // **修复P3: 任务暂停/取消时取消所有正在进行的请求**
            abortController.abort()
            break
          }
    }
    
    // 5. **修复P2-2：合并分块数据并下载（优化大文件内存使用）**
    if (chunkDataMap.size === totalChunks) {
      // **优化：直接使用Uint8Array，无需Base64解码**
      // 按分块编号顺序合并数据
      const chunks = []
      let totalSize = 0
      
      // 先计算总大小（用于内存优化判断）
      for (let i = 0; i < totalChunks; i++) {
        const chunkData = chunkDataMap.get(i)
        if (chunkData) {
          totalSize += chunkData.length
        }
      }
      
      // **修复P2-2：对于大文件（>100MB），使用流式下载方式，减少内存占用**
      // 但考虑到浏览器API限制，当前仍使用Blob方式
      // 建议：如果文件超过100MB，可以考虑提示用户或使用其他下载方式
      if (totalSize > 100 * 1024 * 1024) {
        console.warn(`文件较大(${(totalSize / 1024 / 1024).toFixed(2)}MB)，可能占用较多内存`)
      }
      
      // **修复C4 + m3: 验证所有分块是否都存在（0到totalChunks-1）**
      const missingChunks = []
      for (let i = 0; i < totalChunks; i++) {
        const chunkData = chunkDataMap.get(i)
        if (chunkData) {
          // 直接使用Uint8Array（已经是二进制数据）
          chunks.push(chunkData)
        } else {
          missingChunks.push(i)
          console.error(`分块${i}缺失，下载不完整`)
        }
      }
      
      // **修复C4: 如果发现分块缺失，不创建Blob，标记下载失败**
      if (missingChunks.length > 0) {
        console.error(`分块缺失，下载失败 - 缺失分块: ${missingChunks.join(', ')}`)
        transferStore.updateDownloadTask(task.id, {
          status: 'error',
          error: `下载失败：缺失${missingChunks.length}个分块（${missingChunks.slice(0, 5).join(', ')}${missingChunks.length > 5 ? '...' : ''}）`
        })
        ElMessage.error(`${task.fileName} 下载失败：缺失${missingChunks.length}个分块，请重试`)
        // 清理内存
        chunkDataMap.clear()
        activeDownloads.clear()
        return
      }
      
      // **修复M4: 验证文件大小是否与预期一致**
      const blob = new Blob(chunks)
      if (blob.size !== task.fileSize) {
        console.error(`文件大小不匹配 - 预期: ${task.fileSize}字节, 实际: ${blob.size}字节, 差异: ${Math.abs(blob.size - task.fileSize)}字节`)
        transferStore.updateDownloadTask(task.id, {
          status: 'error',
          error: `文件大小不匹配：预期${task.fileSize}字节，实际${blob.size}字节`
        })
        ElMessage.error(`${task.fileName} 下载失败：文件大小不匹配，请重试`)
        // 清理内存
        chunkDataMap.clear()
        activeDownloads.clear()
        return
      }
      
      // 创建 Blob 并下载
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = task.fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
      
      // **修复P2-2：下载完成后清理内存（显式清空chunkDataMap）**
      chunkDataMap.clear()
      
      // **关键：标记下载完成，清理后端资源（包括算法实例）**
      // 更新前端状态
      transferStore.updateDownloadTask(task.id, {
        status: 'completed',
        progress: 100,
        speed: 0
      })
      
      // 调用后端接口标记下载完成（清理算法实例和更新任务状态）
      try {
        if (taskId) {
          await completeDownload(taskId)
          console.log('下载任务完成，已清理后端资源')
        }
      } catch (error) {
        console.error('标记下载完成失败:', error)
        // 即使清理失败也不影响下载完成
      }
      
      ElMessage.success(`${task.fileName} 下载完成`)
      // 刷新已完成列表
      loadCompletedList()
    } else {
      // 分块未全部下载完成（可能被暂停或出错）
      console.warn('分块未全部下载完成')
    }
  } catch (error) {
    console.error('下载失败:', error)
    // **修复P2-1：下载失败时清理内存和资源**
    chunkDataMap.clear()
    activeDownloads.clear()
    
    // **修复P2/P3: 下载失败时清理任务对象上的清理函数引用**
    if (task._downloadCleanup) {
      delete task._downloadCleanup
    }
    
    // 标记失败
    transferStore.updateDownloadTask(task.id, {
      status: 'error',
      speed: 0
    })
    ElMessage.error(`下载失败: ${error.message}，可点击重试`)
    // 失败也刷新列表
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
