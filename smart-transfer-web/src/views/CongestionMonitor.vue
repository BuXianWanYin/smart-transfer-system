<template>
  <div class="congestion-monitor-page page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-title">拥塞控制监控</div>
      <div class="page-description">实时监控TCP拥塞控制算法运行状态和网络指标</div>
    </div>
    
    <!-- 控制工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span style="font-weight: 600; color: var(--art-text-gray-800);">监控状态</span>
      </div>
      <div class="toolbar-right">
        <el-button
          :type="isMonitoring ? 'danger' : 'success'"
          @click="toggleMonitoring"
        >
          <el-icon><component :is="isMonitoring ? 'VideoPause' : 'VideoPlay'" /></el-icon>
          {{ isMonitoring ? '停止监控' : '开始监控' }}
        </el-button>
      </div>
    </div>
    
    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="metric-cards">
      <el-col :span="8">
        <div class="stat-card" style="border-left-color: rgb(var(--art-primary))">
          <div class="stat-card-title">当前算法</div>
          <div class="stat-card-value text-primary">{{ currentMetrics.algorithm || '-' }}</div>
          <div class="stat-card-trend">拥塞控制算法</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card" style="border-left-color: rgb(var(--art-info))">
          <div class="stat-card-title">拥塞窗口 (CWND)</div>
          <div class="stat-card-value text-info">{{ formatFileSize(currentMetrics.cwnd) }}</div>
          <div class="stat-card-trend">当前拥塞窗口大小</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card" style="border-left-color: rgb(var(--art-success))">
          <div class="stat-card-title">传输速率</div>
          <div class="stat-card-value text-success">{{ formatSpeed(currentMetrics.rate) }}</div>
          <div class="stat-card-trend">实时传输速度</div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 详细指标卡片 -->
    <el-row :gutter="20" class="metric-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-title">RTT (往返时延)</div>
          <div class="stat-card-value">{{ currentMetrics.rtt || 0 }}<span style="font-size: 16px;">ms</span></div>
          <div class="stat-card-trend">Round-Trip Time</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-title">丢包率</div>
          <div class="stat-card-value" :class="currentMetrics.lossRate > 0.05 ? 'text-danger' : ''">
            {{ formatPercent(currentMetrics.lossRate) }}
          </div>
          <div class="stat-card-trend">Packet Loss Rate</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-title">带宽估计</div>
          <div class="stat-card-value">{{ formatSpeed(currentMetrics.bandwidth) }}</div>
          <div class="stat-card-trend">Bandwidth Estimate</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-title">网络质量</div>
          <div class="stat-card-value">
            <el-tag :type="getQualityType(currentMetrics.networkQuality)" effect="plain" size="large">
              {{ currentMetrics.networkQuality || '-' }}
            </el-tag>
          </div>
          <div class="stat-card-trend">Network Quality</div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 算法切换 -->
    <el-card style="margin-top: 20px">
      <template #header>算法控制</template>
      <el-radio-group v-model="selectedAlgorithm" @change="handleAlgorithmChange">
        <el-radio-button value="CUBIC">CUBIC</el-radio-button>
        <el-radio-button value="BBR">BBR</el-radio-button>
        <el-radio-button value="ADAPTIVE">自适应</el-radio-button>
      </el-radio-group>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { useCongestionStore } from '@/store/congestionStore'
import { getCongestionMetrics, switchAlgorithm } from '@/api/congestionApi'
import { formatFileSize, formatSpeed } from '@/utils/file'
import { formatPercent } from '@/utils/format'

const congestionStore = useCongestionStore()
const isMonitoring = ref(false)
const selectedAlgorithm = ref('CUBIC')
const currentMetrics = ref({
  algorithm: 'NONE',
  cwnd: 0,
  rate: 0,
  rtt: 0,
  lossRate: 0,
  bandwidth: 0,
  networkQuality: '未知'
})

let monitoringTimer = null

onMounted(() => {
  fetchMetrics()
})

onUnmounted(() => {
  stopMonitoring()
})

const fetchMetrics = async () => {
  try {
    const res = await getCongestionMetrics()
    currentMetrics.value = res
    congestionStore.updateMetrics(res)
  } catch (error) {
    // 获取指标失败
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
  monitoringTimer = setInterval(fetchMetrics, 3000) // 每3秒刷新
  ElMessage.success('开始监控')
}

const stopMonitoring = () => {
  isMonitoring.value = false
  congestionStore.stopMonitoring()
  if (monitoringTimer) {
    clearInterval(monitoringTimer)
    monitoringTimer = null
  }
}

const handleAlgorithmChange = async (algorithm) => {
  try {
    await switchAlgorithm(algorithm)
    congestionStore.setAlgorithm(algorithm)
    ElMessage.success(`已切换到 ${algorithm} 算法`)
    fetchMetrics()
  } catch (error) {
    ElMessage.error('切换算法失败')
    selectedAlgorithm.value = currentMetrics.value.algorithm
  }
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
.metric-cards {
  margin-bottom: 20px;
}

.chart-container {
  background: var(--art-main-bg-color);
  border-radius: 8px;
  padding: 20px;
  box-shadow: var(--art-card-shadow);
}
</style>

