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
      
      <!-- 用户统计 -->
      <el-col :span="12">
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
      <el-col :span="12">
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
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserList, getSystemStorageStats } from '@/api/userApi'

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
 * 格式化文件大小
 */
const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.system-stats-page {
  padding: 20px;
  
  .stat-card {
    text-align: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    
    .stat-value {
      font-size: 32px;
      font-weight: bold;
      color: var(--el-color-primary);
      margin-bottom: 8px;
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
    }
  }
  
  .chart-container {
    padding: 20px;
    
    .stat-item {
      display: flex;
      justify-content: space-between;
      padding: 12px 0;
      border-bottom: 1px solid #ebeef5;
      
      &:last-child {
        border-bottom: none;
      }
      
      span {
        color: #606266;
      }
      
      strong {
        color: var(--el-color-primary);
      }
    }
  }
}
</style>
