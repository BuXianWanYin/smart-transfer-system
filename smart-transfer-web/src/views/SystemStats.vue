<template>
  <div class="system-stats-page">
    <el-row :gutter="20">
      <!-- 系统总览 -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>系统总览</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ stats.totalUsers }}</div>
                <div class="stat-label">总用户数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ stats.activeUsers }}</div>
                <div class="stat-label">活跃用户数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ formatSize(stats.totalStorage) }}</div>
                <div class="stat-label">总存储空间</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ stats.totalFiles }}</div>
                <div class="stat-label">总文件数</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      
      <!-- 传输统计图表 -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>传输统计</span>
              <div style="display: flex; align-items: center; gap: 12px;">
                <!-- 用户筛选（仅管理员） -->
                <el-select
                  v-if="userStore.isAdmin"
                  v-model="selectedUserId"
                  placeholder="全部用户"
                  clearable
                  filterable
                  style="width: 150px"
                  @change="handleUserChange"
                >
                  <el-option label="全部用户" value="" />
                  <el-option
                    v-for="user in userList"
                    :key="user.id"
                    :label="user.nickname || user.username"
                    :value="user.id"
                  />
                </el-select>
                <el-radio-group v-model="transferPeriod" @change="loadTransferStats">
                  <el-radio-button value="day">日</el-radio-button>
                  <el-radio-button value="week">周</el-radio-button>
                  <el-radio-button value="month">月</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div class="chart-container" v-loading="transferStatsLoading">
            <div class="chart-placeholder" v-if="!transferStatsLoading && transferStats.uploadLabels && transferStats.uploadLabels.length === 0">
              <el-empty description="暂无传输数据" />
            </div>
            <div v-else class="chart-content">
              <div class="chart-item">
                <div class="chart-title">上传量</div>
                <div class="chart-bars">
                  <div 
                    v-for="(value, index) in transferStats.uploadValues" 
                    :key="index"
                    class="chart-bar"
                    :style="{ height: getBarHeight(value, transferStats.uploadValues) + '%' }"
                    :title="`${transferStats.uploadLabels && transferStats.uploadLabels[index] ? transferStats.uploadLabels[index] : ''}: ${formatSize(value)}`"
                  >
                    <span class="bar-value">{{ formatSize(value) }}</span>
                  </div>
                </div>
                <div class="chart-labels">
                  <span v-for="(label, index) in transferStats.uploadLabels" :key="index" class="chart-label">
                    {{ formatLabel(label, transferPeriod) }}
                  </span>
                </div>
              </div>
              <div class="chart-item">
                <div class="chart-title">下载量</div>
                <div class="chart-bars">
                  <div 
                    v-for="(value, index) in transferStats.downloadValues" 
                    :key="index"
                    class="chart-bar download"
                    :style="{ height: getBarHeight(value, transferStats.downloadValues) + '%' }"
                    :title="`${transferStats.downloadLabels && transferStats.downloadLabels[index] ? transferStats.downloadLabels[index] : ''}: ${formatSize(value)}`"
                  >
                    <span class="bar-value">{{ formatSize(value) }}</span>
                  </div>
                </div>
                <div class="chart-labels">
                  <span v-for="(label, index) in transferStats.downloadLabels" :key="index" class="chart-label">
                    {{ formatLabel(label, transferPeriod) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 用户统计 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>用户统计</span>
          </template>
          <div class="chart-container">
            <div class="stat-item">
              <span>管理员：</span>
              <strong>{{ stats.adminCount }}</strong>
            </div>
            <div class="stat-item">
              <span>普通用户：</span>
              <strong>{{ stats.userCount }}</strong>
            </div>
            <div class="stat-item">
              <span>启用用户：</span>
              <strong>{{ stats.enabledUsers }}</strong>
            </div>
            <div class="stat-item">
              <span>禁用用户：</span>
              <strong>{{ stats.disabledUsers }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 存储统计 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>存储统计</span>
          </template>
          <div class="chart-container">
            <div class="stat-item">
              <span>图片：</span>
              <strong>{{ formatSize(stats.imageSize) }} ({{ stats.imageCount }} 个文件)</strong>
            </div>
            <div class="stat-item">
              <span>视频：</span>
              <strong>{{ formatSize(stats.videoSize) }} ({{ stats.videoCount }} 个文件)</strong>
            </div>
            <div class="stat-item">
              <span>文档：</span>
              <strong>{{ formatSize(stats.docSize) }} ({{ stats.docCount }} 个文件)</strong>
            </div>
            <div class="stat-item">
              <span>其他：</span>
              <strong>{{ formatSize(stats.otherSize) }} ({{ stats.otherCount }} 个文件)</strong>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 网络质量统计 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>网络质量统计</span>
          </template>
          <div class="chart-container" v-loading="networkQualityLoading">
            <div v-if="networkQualityStats.qualityCount">
              <div class="stat-item">
                <span>优秀：</span>
                <strong>{{ networkQualityStats.qualityCount.EXCELLENT || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>良好：</span>
                <strong>{{ networkQualityStats.qualityCount.GOOD || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>一般：</span>
                <strong>{{ networkQualityStats.qualityCount.FAIR || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>差：</span>
                <strong>{{ networkQualityStats.qualityCount.POOR || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>当前质量：</span>
                <el-tag :type="getQualityTagType(networkQualityStats.currentQuality)">
                  {{ getQualityText(networkQualityStats.currentQuality) }}
                </el-tag>
              </div>
            </div>
            <el-empty v-else description="暂无数据" />
          </div>
        </el-card>
      </el-col>
      
      <!-- 算法使用统计 -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>算法使用统计</span>
          </template>
          <div class="chart-container" v-loading="algorithmStatsLoading">
            <div v-if="algorithmStats.algorithmLabels && algorithmStats.algorithmLabels.length > 0" class="algorithm-stats">
              <div class="algorithm-item" v-for="(label, index) in algorithmStats.algorithmLabels" :key="label">
                <div class="algorithm-name">{{ label }}</div>
                <div class="algorithm-details">
                  <span>使用次数：{{ algorithmStats.countValues && algorithmStats.countValues[index] !== undefined ? algorithmStats.countValues[index] : 0 }}</span>
                  <span>传输量：{{ formatSize(algorithmStats.sizeValues && algorithmStats.sizeValues[index] !== undefined ? algorithmStats.sizeValues[index] : 0) }}</span>
                </div>
                <div class="algorithm-bar">
                  <div 
                    class="algorithm-progress" 
                    :style="{ width: getAlgorithmPercentage(algorithmStats.sizeValues && algorithmStats.sizeValues[index] !== undefined ? algorithmStats.sizeValues[index] : 0, algorithmStats.sizeValues || []) + '%' }"
                  ></div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无算法使用数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserList, getSystemStorageStats } from '@/api/userApi'
import { getTransferStats, getAlgorithmStats } from '@/api/historyApi'
import { useUserStore } from '@/store/userStore'

const userStore = useUserStore()

// 统计数据
const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  totalStorage: 0,
  totalFiles: 0,
  adminCount: 0,
  userCount: 0,
  enabledUsers: 0,
  disabledUsers: 0,
  imageSize: 0,
  imageCount: 0,
  videoSize: 0,
  videoCount: 0,
  docSize: 0,
  docCount: 0,
  otherSize: 0,
  otherCount: 0
})

// 用户筛选（仅管理员）
const selectedUserId = ref('')
const userList = ref([])

// 用户筛选变化
const handleUserChange = () => {
  loadTransferStats()
  loadAlgorithmStats()
}

// 传输统计
const transferPeriod = ref('day')
const transferStats = reactive({
  uploadLabels: [],
  uploadValues: [],
  downloadLabels: [],
  downloadValues: []
})
const transferStatsLoading = ref(false)

// 网络质量统计
const networkQualityStats = reactive({
  qualityCount: {},
  currentQuality: 'UNKNOWN'
})
const networkQualityLoading = ref(false)

// 算法使用统计
const algorithmStats = reactive({
  algorithmLabels: [],
  countValues: [],
  sizeValues: []
})
const algorithmStatsLoading = ref(false)

/**
 * 加载统计数据
 */
const loadStats = async () => {
  try {
    // 获取用户列表
    const userList = await getUserList()
    
    // 统计用户数据
    stats.totalUsers = userList.length
    stats.adminCount = userList.filter(u => u.role === 'ADMIN').length
    stats.userCount = userList.filter(u => u.role === 'USER').length
    stats.enabledUsers = userList.filter(u => u.status === 1).length
    stats.disabledUsers = userList.filter(u => u.status === 0).length
    
    // 统计活跃用户（有最后登录时间的）
    stats.activeUsers = userList.filter(u => u.lastLoginTime).length
    
    // 获取系统级存储统计
    try {
      const storageStats = await getSystemStorageStats()
      stats.totalStorage = storageStats.totalSize || 0
      stats.totalFiles = storageStats.fileCount || 0
      stats.imageSize = storageStats.imageSize || 0
      stats.imageCount = storageStats.imageCount || 0
      stats.videoSize = storageStats.videoSize || 0
      stats.videoCount = storageStats.videoCount || 0
      stats.docSize = storageStats.docSize || 0
      stats.docCount = storageStats.docCount || 0
      stats.otherSize = storageStats.otherSize || 0
      stats.otherCount = storageStats.otherCount || 0
    } catch (error) {
      // 如果获取存储统计失败，使用默认值0
      stats.totalStorage = 0
      stats.totalFiles = 0
      stats.imageSize = 0
      stats.imageCount = 0
      stats.videoSize = 0
      stats.videoCount = 0
      stats.docSize = 0
      stats.docCount = 0
      stats.otherSize = 0
      stats.otherCount = 0
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败：' + (error.message || '未知错误'))
  }
}

/**
 * 加载传输统计
 */
const loadTransferStats = async () => {
  transferStatsLoading.value = true
  try {
    // 如果是管理员且指定了用户，传递userId参数
    const userId = userStore.isAdmin && selectedUserId.value ? selectedUserId.value : null
    const data = await getTransferStats(transferPeriod.value, userId)
    transferStats.uploadLabels = data.uploadLabels || []
    transferStats.uploadValues = data.uploadValues || []
    transferStats.downloadLabels = data.downloadLabels || []
    transferStats.downloadValues = data.downloadValues || []
  } catch (error) {
    ElMessage.error('加载传输统计失败：' + (error.message || '未知错误'))
  } finally {
    transferStatsLoading.value = false
  }
}

/**
 * 加载网络质量统计（该API已被后端禁用）
 */
const loadNetworkQualityStats = async () => {
  networkQualityLoading.value = true
  try {
    // 该API已被后端禁用，使用默认数据避免页面加载失败
    networkQualityStats.qualityCount = {
      '优秀': 0,
      '良好': 0,
      '一般': 0,
      '较差': 0
    }
    networkQualityStats.currentQuality = 'UNKNOWN'
  } catch (error) {
    console.error('加载网络质量统计失败:', error)
  } finally {
    networkQualityLoading.value = false
  }
}

/**
 * 加载算法使用统计
 */
const loadAlgorithmStats = async () => {
  algorithmStatsLoading.value = true
  try {
    // 如果是管理员且指定了用户，传递userId参数
    const userId = userStore.isAdmin && selectedUserId.value ? selectedUserId.value : null
    const data = await getAlgorithmStats(userId)
    algorithmStats.algorithmLabels = data.algorithmLabels || []
    algorithmStats.countValues = data.countValues || []
    algorithmStats.sizeValues = data.sizeValues || []
  } catch (error) {
    ElMessage.error('加载算法统计失败：' + (error.message || '未知错误'))
  } finally {
    algorithmStatsLoading.value = false
  }
}

/**
 * 格式化文件大小
 */
const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 计算柱状图高度
 */
const getBarHeight = (value, values) => {
  if (!values || values.length === 0) return 0
  const max = Math.max(...values)
  if (max === 0) return 0
  return (value / max) * 100
}

/**
 * 格式化标签
 */
const formatLabel = (label, period) => {
  if (!label) return ''
  
  if (period === 'day') {
    const parts = label.split('-')
    return parts.length >= 3 ? parts[2] : label // 只显示日期
  } else if (period === 'week') {
    const parts = label.split('-')
    return parts.length >= 3 ? parts[1] + '/' + parts[2] : label // 显示月/日
  } else {
    return label // 显示年月
  }
}

/**
 * 获取网络质量标签类型
 */
const getQualityTagType = (quality) => {
  const map = {
    'EXCELLENT': 'success',
    'GOOD': 'primary',
    'FAIR': 'warning',
    'POOR': 'danger',
    'UNKNOWN': 'info'
  }
  return map[quality] || 'info'
}

/**
 * 获取网络质量文本
 */
const getQualityText = (quality) => {
  const map = {
    'EXCELLENT': '优秀',
    'GOOD': '良好',
    'FAIR': '一般',
    'POOR': '差',
    'UNKNOWN': '未知'
  }
  return map[quality] || '未知'
}

/**
 * 计算算法使用百分比
 */
const getAlgorithmPercentage = (value, values) => {
  if (!values || values.length === 0) return 0
  const total = values.reduce((sum, v) => sum + v, 0)
  if (total === 0) return 0
  return (value / total) * 100
}

// 加载用户列表（仅管理员）
const loadUserList = async () => {
  if (userStore.isAdmin) {
    try {
      const res = await getUserList()
      userList.value = res || []
    } catch (error) {
      console.error('加载用户列表失败', error)
    }
  }
}

onMounted(() => {
  loadUserList()
  loadStats()
  loadTransferStats()
  loadNetworkQualityStats()
  loadAlgorithmStats()
})
</script>

<style lang="scss" scoped>
.system-stats-page {
  padding: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .stat-card {
    text-align: center;
    padding: 20px;
    background: var(--art-fill-light);
    border-radius: 10px;
    
    .stat-value {
      font-family: var(--art-font-display);
      font-size: 32px;
      font-weight: 600;
      color: rgb(var(--art-primary));
      margin-bottom: 8px;
    }
    
    .stat-label {
      font-size: 14px;
      color: var(--art-text-gray-500);
    }
  }
  
  .chart-container {
    padding: 20px;
    min-height: 200px;
    
    .chart-placeholder {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 200px;
    }
    
    .chart-content {
      display: flex;
      gap: 40px;
      
      .chart-item {
        flex: 1;
        
        .chart-title {
          font-size: 16px;
          font-weight: bold;
          margin-bottom: 20px;
          text-align: center;
        }
        
        .chart-bars {
          display: flex;
          align-items: flex-end;
          justify-content: space-around;
          height: 200px;
          gap: 8px;
          
          .chart-bar {
            flex: 1;
            background: rgb(var(--art-primary));
            border-radius: 4px 4px 0 0;
            position: relative;
            min-height: 20px;
            transition: opacity var(--art-duration-fast) var(--art-ease-out);
            
            &.download {
              background: rgb(var(--art-success));
            }
            
            .bar-value {
              position: absolute;
              top: -20px;
              left: 50%;
              transform: translateX(-50%);
              font-size: 12px;
              white-space: nowrap;
            }
            
            &:hover {
              opacity: 0.8;
            }
          }
        }
        
        .chart-labels {
          display: flex;
          justify-content: space-around;
          margin-top: 10px;
          
          .chart-label {
            font-size: 12px;
            color: var(--art-text-gray-500);
            flex: 1;
            text-align: center;
          }
        }
      }
    }
    
    .stat-item {
      display: flex;
      justify-content: space-between;
      padding: 12px 0;
      border-bottom: 1px solid var(--art-border-color);
      
      &:last-child {
        border-bottom: none;
      }
      
      span {
        color: var(--art-text-gray-600);
      }
      
      strong {
        color: rgb(var(--art-primary));
      }
    }
    
    .algorithm-stats {
      .algorithm-item {
        margin-bottom: 20px;
        
        .algorithm-name {
          font-size: 16px;
          font-weight: bold;
          margin-bottom: 8px;
        }
        
        .algorithm-details {
          display: flex;
          gap: 20px;
          margin-bottom: 8px;
          font-size: 14px;
          color: var(--art-text-gray-600);
        }
        
        .algorithm-bar {
          height: 20px;
          background: var(--art-fill-light);
          border-radius: 10px;
          overflow: hidden;
          
          .algorithm-progress {
            height: 100%;
            background: linear-gradient(90deg, rgb(var(--art-primary)), rgb(var(--art-success)));
            transition: width var(--art-duration-normal) var(--art-ease-out);
          }
        }
      }
    }
  }
}
</style>
