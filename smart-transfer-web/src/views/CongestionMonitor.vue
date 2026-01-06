<template>
  <div class="congestion-monitor-page">
    <!-- 当前指标 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实时监控</span>
          <div>
            <el-button
              :type="isMonitoring ? 'danger' : 'success'"
              @click="toggleMonitoring"
            >
              {{ isMonitoring ? '停止监控' : '开始监控' }}
            </el-button>
          </div>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover">
            <el-statistic
              title="当前算法"
              :value="currentMetrics.algorithm"
            />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <el-statistic
              title="拥塞窗口 (CWND)"
              :value="formatFileSize(currentMetrics.cwnd)"
            />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <el-statistic
              title="传输速率"
              :value="formatSpeed(currentMetrics.rate)"
            />
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic
              title="RTT"
              :value="currentMetrics.rtt"
            >
              <template #suffix>ms</template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic
              title="丢包率"
              :value="formatPercent(currentMetrics.lossRate)"
            />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic
              title="带宽"
              :value="formatSpeed(currentMetrics.bandwidth)"
            />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="网络质量">
              <template #default>
                <el-tag :type="getQualityType(currentMetrics.networkQuality)">
                  {{ currentMetrics.networkQuality }}
                </el-tag>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
    
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
    currentMetrics.value = res.data
    congestionStore.updateMetrics(res.data)
  } catch (error) {
    console.error('获取指标失败', error)
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
.congestion-monitor-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

