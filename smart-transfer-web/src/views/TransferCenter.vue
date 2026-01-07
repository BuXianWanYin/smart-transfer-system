<template>
  <div class="transfer-center-page page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-title">传输中心</div>
      <div class="page-description">统一管理文件上传、下载任务，实时监控传输状态</div>
    </div>
    
    <!-- 拥塞监控面板 - 可折叠 -->
    <el-card class="monitor-panel" :class="{ collapsed: monitorCollapsed }">
      <template #header>
        <div class="monitor-header">
          <span>
            <el-icon><Monitor /></el-icon>
            拥塞监控面板
          </span>
          <el-button 
            text 
            @click="toggleMonitor"
            class="collapse-btn"
          >
            <el-icon>
              <component :is="monitorCollapsed ? 'ArrowDown' : 'ArrowUp'" />
            </el-icon>
            {{ monitorCollapsed ? '展开' : '收起' }}
          </el-button>
        </div>
      </template>
      
      <div v-show="!monitorCollapsed" class="monitor-content">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="monitor-item">
              <div class="monitor-label">当前算法</div>
              <div class="monitor-value primary">{{ currentMetrics.algorithm || 'CUBIC' }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="monitor-item">
              <div class="monitor-label">拥塞窗口 (CWND)</div>
              <div class="monitor-value info">{{ formatFileSize(currentMetrics.cwnd) }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="monitor-item">
              <div class="monitor-label">传输速率</div>
              <div class="monitor-value success">{{ formatSpeed(realTimeSpeed) }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="monitor-item">
              <div class="monitor-label">网络质量</div>
              <div class="monitor-value">
                <el-tag :type="getQualityType(currentMetrics.networkQuality)" effect="plain">
                  {{ currentMetrics.networkQuality || '未知' }}
                </el-tag>
              </div>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" style="margin-top: 15px;">
          <el-col :span="6">
            <div class="monitor-item-small">
              <span class="label">RTT:</span>
              <span class="value">{{ currentMetrics.rtt || 0 }}ms</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="monitor-item-small">
              <span class="label">丢包率:</span>
              <span class="value" :class="{ danger: currentMetrics.lossRate > 0.05 }">
                {{ formatPercent(currentMetrics.lossRate) }}
              </span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="monitor-item-small">
              <span class="label">带宽估计:</span>
              <span class="value">{{ formatSpeed(currentMetrics.bandwidth) }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="monitor-item-small">
              <el-tag v-if="wsConnected" type="success" size="small" effect="light" style="margin-right: 8px;">
                <el-icon style="vertical-align: middle;"><Connection /></el-icon>
                已连接
              </el-tag>
              <el-tag v-else type="info" size="small" effect="light" style="margin-right: 8px;">
                未连接
              </el-tag>
              <el-button 
                size="small" 
                :type="isMonitoring ? 'danger' : 'success'"
                @click="toggleMonitoring"
              >
                <el-icon><component :is="isMonitoring ? 'VideoPause' : 'VideoPlay'" /></el-icon>
                {{ isMonitoring ? '停止监控' : '开始监控' }}
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
    
    <!-- 标签页 -->
    <el-card class="tabs-container">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane name="upload">
          <template #label>
            <span class="tab-label">
              <el-icon><Upload /></el-icon>
              上传中
              <el-badge 
                v-if="uploadingCount > 0" 
                :value="uploadingCount" 
                class="tab-badge"
              />
            </span>
          </template>
          <UploadPanel ref="uploadPanelRef" />
        </el-tab-pane>
        
        <el-tab-pane name="download">
          <template #label>
            <span class="tab-label">
              <el-icon><Download /></el-icon>
              下载中
              <el-badge 
                v-if="downloadingCount > 0" 
                :value="downloadingCount" 
                class="tab-badge"
              />
            </span>
          </template>
          <DownloadPanel ref="downloadPanelRef" />
        </el-tab-pane>
        
        <el-tab-pane name="completed">
          <template #label>
            <span class="tab-label">
              <el-icon><CircleCheck /></el-icon>
              传输完成
              <el-badge 
                v-if="completedCount > 0" 
                :value="completedCount" 
                class="tab-badge"
                type="success"
              />
            </span>
          </template>
          <CompletedPanel ref="completedPanelRef" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Monitor, ArrowDown, ArrowUp, Upload, Download, CircleCheck,
  VideoPlay, VideoPause, Connection
} from '@element-plus/icons-vue'
import { useCongestionStore } from '@/store/congestionStore'
import { useFileStore } from '@/store/fileStore'
import { monitorWs } from '@/utils/websocket'
import { formatFileSize, formatSpeed } from '@/utils/format'
import UploadPanel from './transfer/UploadPanel.vue'

// 格式化百分比
const formatPercent = (value) => {
  if (value === null || value === undefined) return '0%'
  return (value * 100).toFixed(2) + '%'
}
import DownloadPanel from './transfer/DownloadPanel.vue'
import CompletedPanel from './transfer/CompletedPanel.vue'

const congestionStore = useCongestionStore()
const fileStore = useFileStore()

// 状态管理
const activeTab = ref('upload')
const monitorCollapsed = ref(false)
const isMonitoring = ref(false)
const wsConnected = ref(false)
const currentMetrics = ref({
  algorithm: 'CUBIC',
  cwnd: 0,
  rate: 0,
  rtt: 0,
  lossRate: 0,
  bandwidth: 0,
  networkQuality: '良好'
})

// 计算属性
const uploadingCount = computed(() => fileStore.uploadingFiles?.length || 0)
const downloadingCount = computed(() => fileStore.downloadingFiles?.length || 0)
const completedCount = computed(() => fileStore.completedFiles?.length || 0)

// 实时传输速率（从上传队列计算）
const realTimeSpeed = computed(() => fileStore.currentTotalSpeed || 0)

// 引用
const uploadPanelRef = ref()
const downloadPanelRef = ref()
const completedPanelRef = ref()

let wsUnsubscribe = null

// 生命周期
onMounted(() => {
  // 默认开始监控
  startMonitoring()
})

onUnmounted(() => {
  stopMonitoring()
})

// 方法
const toggleMonitor = () => {
  monitorCollapsed.value = !monitorCollapsed.value
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

const toggleMonitoring = () => {
  if (isMonitoring.value) {
    stopMonitoring()
  } else {
    startMonitoring()
  }
}

const startMonitoring = () => {
  isMonitoring.value = true
  congestionStore.startMonitoring()
  
  // 连接WebSocket
  wsUnsubscribe = monitorWs.addListener(handleWsEvent)
  monitorWs.connect()
  
  ElMessage.success('开始实时监控')
}

const stopMonitoring = () => {
  isMonitoring.value = false
  congestionStore.stopMonitoring()
  
  // 断开WebSocket
  if (wsUnsubscribe) {
    wsUnsubscribe()
    wsUnsubscribe = null
  }
  monitorWs.disconnect()
}

const handleTabClick = () => {
  // 标签切换处理
}

const getQualityType = (quality) => {
  const typeMap = {
    '优秀': 'success',
    '良好': 'primary',
    '一般': 'warning',
    '差': 'danger'
  }
  return typeMap[quality] || 'info'
}
</script>

<style scoped>
.transfer-center-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 监控面板样式 */
.monitor-panel {
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.monitor-panel.collapsed {
  margin-bottom: 20px;
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.monitor-header span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collapse-btn {
  font-size: 14px;
}

.monitor-content {
  padding: 10px 0;
}

.monitor-item {
  text-align: center;
  padding: 15px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  transition: all 0.3s;
}

.monitor-item:hover {
  background: var(--el-fill-color);
  transform: translateY(-2px);
}

.monitor-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.monitor-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.monitor-value.primary {
  color: var(--el-color-primary);
}

.monitor-value.info {
  color: var(--el-color-info);
}

.monitor-value.success {
  color: var(--el-color-success);
}

.monitor-item-small {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.monitor-item-small .label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.monitor-item-small .value {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.monitor-item-small .value.danger {
  color: var(--el-color-danger);
}

/* 标签页样式 */
.tabs-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tabs-container :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.tabs-container :deep(.el-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tabs-container :deep(.el-tabs__content) {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.tab-badge {
  margin-left: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .monitor-item {
    margin-bottom: 10px;
  }
  
  .monitor-value {
    font-size: 20px;
  }
}
</style>

