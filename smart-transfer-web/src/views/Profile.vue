<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- 用户信息卡片 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>个人信息</span>
          </div>
        </template>
        
        <div class="user-info">
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              :http-request="handleAvatarUpload"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
            >
              <el-avatar :size="80" :src="avatarUrl">
                <el-icon :size="40"><UserFilled /></el-icon>
              </el-avatar>
              <div class="avatar-upload-tip">
                <el-icon><Camera /></el-icon>
                <span>点击上传头像</span>
              </div>
            </el-upload>
            <div class="user-basic">
              <h3>{{ userInfo.nickname || userInfo.username }}</h3>
            </div>
          </div>
          
          <el-form
            ref="infoFormRef"
            :model="infoForm"
            :rules="infoRules"
            label-width="80px"
            class="info-form"
          >
            <el-form-item label="用户名">
              <el-input v-model="userInfo.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateInfo" :loading="infoLoading">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
      
      <!-- 存储统计卡片 -->
      <el-card class="storage-card">
        <template #header>
          <div class="card-header">
            <span>存储统计</span>
            <el-button text @click="loadStorageStats">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </template>
        
        <div class="storage-stats" v-loading="storageLoading">
          <!-- 总体统计 -->
          <div class="total-stats">
            <div class="stat-item main">
              <div class="stat-value">{{ formatSize(storageStats.totalSize) }}</div>
              <div class="stat-label">已使用空间</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ storageStats.fileCount || 0 }}</div>
              <div class="stat-label">文件数量</div>
            </div>
          </div>
          
          <!-- 分类统计 -->
          <div class="category-stats">
            <div class="category-item" v-for="item in categoryList" :key="item.key">
              <div class="category-icon" :style="{ background: item.color }">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="category-info">
                <div class="category-name">{{ item.name }}</div>
                <div class="category-detail">
                  {{ storageStats[item.countKey] || 0 }} 个文件，
                  {{ formatSize(storageStats[item.sizeKey]) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 传输统计卡片 -->
      <el-card class="transfer-stats-card">
        <template #header>
          <div class="card-header">
            <span>传输统计</span>
            <el-button text @click="loadTransferStats">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </template>
        
        <div class="transfer-stats" v-loading="transferStatsLoading">
          <el-radio-group v-model="transferPeriod" @change="loadTransferStats" size="small">
            <el-radio-button label="day">日</el-radio-button>
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
          </el-radio-group>
          
          <div class="transfer-summary" v-if="transferStats.uploadValues">
            <div class="summary-item">
              <div class="summary-label">上传总量</div>
              <div class="summary-value">{{ formatSize(getTotalUpload()) }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">下载总量</div>
              <div class="summary-value">{{ formatSize(getTotalDownload()) }}</div>
            </div>
          </div>
          
          <div class="transfer-chart" v-if="transferStats.uploadLabels && transferStats.uploadLabels.length > 0">
            <div class="chart-item">
              <div class="chart-title">上传趋势</div>
              <div class="chart-bars">
                <div 
                  v-for="(value, index) in transferStats.uploadValues" 
                  :key="index"
                  class="chart-bar upload"
                  :style="{ height: getBarHeight(value, transferStats.uploadValues) + '%' }"
                  :title="`${transferStats.uploadLabels && transferStats.uploadLabels[index] ? transferStats.uploadLabels[index] : ''}: ${formatSize(value)}`"
                >
                  <span class="bar-value">{{ formatSize(value) }}</span>
                </div>
              </div>
            </div>
            <div class="chart-item">
              <div class="chart-title">下载趋势</div>
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
            </div>
          </div>
          <el-empty v-else description="暂无传输数据" />
        </div>
      </el-card>
      
      <!-- 修改密码卡片 -->
      <el-card class="password-card">
        <template #header>
          <div class="card-header">
            <span>修改密码</span>
          </div>
        </template>
        
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          class="password-form"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Refresh, Picture, VideoPlay, Headset, Document, Folder, Camera } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/userStore'
import { updateUserInfo, changePassword, getStorageStats, uploadAvatar } from '@/api/userApi'
import { getTransferStats } from '@/api/historyApi'

const userStore = useUserStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo || {})

// 头像URL（根据baseURL拼接）
const avatarUrl = computed(() => {
  if (!userInfo.value?.avatar) return undefined
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  // 如果avatar已经是完整URL，直接返回；否则拼接
  if (userInfo.value.avatar.startsWith('http://') || userInfo.value.avatar.startsWith('https://')) {
    return userInfo.value.avatar
  }
  // 相对路径格式：avatars/userId/filename
  // 添加时间戳防止缓存
  const timestamp = new Date().getTime()
  return `${baseURL}/user/avatar/${userInfo.value.avatar}?t=${timestamp}`
})

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('头像只能是图片格式!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('头像大小不能超过 5MB!')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  try {
    const avatarPath = await uploadAvatar(options.file)
    if (avatarPath) {
      ElMessage.success('头像上传成功')
      // 更新用户信息
      await userStore.refreshUserInfo()
      // 强制刷新头像（通过更新key来触发重新渲染）
      // 由于使用了computed，userInfo更新后avatarUrl会自动更新
      // 但为了确保浏览器刷新缓存，我们在URL中添加了时间戳
    }
  } catch (error) {
    ElMessage.error('头像上传失败: ' + (error.message || '未知错误'))
  }
}

// 信息表单
const infoFormRef = ref(null)
const infoForm = reactive({
  nickname: '',
  email: '',
  phone: ''
})
const infoLoading = ref(false)

// 信息表单验证规则
const infoRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { 
      validator: (rule, value, callback) => {
        if (!value || value.trim() === '') {
          callback() // 邮箱可以为空
        } else if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) {
          callback(new Error('请输入正确的邮箱地址'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  phone: [
    { 
      validator: (rule, value, callback) => {
        if (!value || value.trim() === '') {
          callback() // 手机号可以为空
        } else if (!/^1[3-9]\d{9}$/.test(value)) {
          callback(new Error('请输入正确的手机号（11位数字，以1开头）'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// 存储统计
const storageLoading = ref(false)
const storageStats = ref({})

// 传输统计
const transferPeriod = ref('day')
const transferStats = reactive({
  uploadLabels: [],
  uploadValues: [],
  downloadLabels: [],
  downloadValues: []
})
const transferStatsLoading = ref(false)

// 分类列表
const categoryList = [
  { key: 'image', name: '图片', icon: Picture, color: '#67c23a', countKey: 'imageCount', sizeKey: 'imageSize' },
  { key: 'video', name: '视频', icon: VideoPlay, color: '#409eff', countKey: 'videoCount', sizeKey: 'videoSize' },
  { key: 'audio', name: '音乐', icon: Headset, color: '#e6a23c', countKey: 'audioCount', sizeKey: 'audioSize' },
  { key: 'doc', name: '文档', icon: Document, color: '#f56c6c', countKey: 'docCount', sizeKey: 'docSize' },
  { key: 'other', name: '其他', icon: Folder, color: '#909399', countKey: 'otherCount', sizeKey: 'otherSize' }
]

// 密码表单
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)

// 密码验证规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 格式化大小
const formatSize = (size) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 加载存储统计
const loadStorageStats = async () => {
  storageLoading.value = true
  try {
    const data = await getStorageStats()
    storageStats.value = data || {}
  } catch (error) {
    // 忽略错误
  } finally {
    storageLoading.value = false
  }
}

// 加载传输统计
const loadTransferStats = async () => {
  transferStatsLoading.value = true
  try {
    const data = await getTransferStats(transferPeriod.value)
    transferStats.uploadLabels = data.uploadLabels || []
    transferStats.uploadValues = data.uploadValues || []
    transferStats.downloadLabels = data.downloadLabels || []
    transferStats.downloadValues = data.downloadValues || []
  } catch (error) {
    // 忽略错误
  } finally {
    transferStatsLoading.value = false
  }
}

// 计算总上传量
const getTotalUpload = () => {
  if (!transferStats.uploadValues || transferStats.uploadValues.length === 0) return 0
  return transferStats.uploadValues.reduce((a, b) => a + b, 0)
}

// 计算总下载量
const getTotalDownload = () => {
  if (!transferStats.downloadValues || transferStats.downloadValues.length === 0) return 0
  return transferStats.downloadValues.reduce((a, b) => a + b, 0)
}

// 计算柱状图高度
const getBarHeight = (value, values) => {
  if (!values || values.length === 0) return 0
  const max = Math.max(...values)
  if (max === 0) return 0
  return (value / max) * 100
}

// 更新用户信息
const handleUpdateInfo = async () => {
  if (!infoFormRef.value) return
  
  try {
    // 表单验证
    await infoFormRef.value.validate()
    infoLoading.value = true
    
    await updateUserInfo({
      nickname: infoForm.nickname,
      email: infoForm.email,
      phone: infoForm.phone
    })
    ElMessage.success('信息更新成功')
    await userStore.refreshUserInfo()
  } catch (error) {
    if (error !== false) { // 表单验证失败会返回false
      ElMessage.error(error.message || '更新失败')
    }
  } finally {
    infoLoading.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    
    ElMessage.success('密码修改成功，请重新登录')
    passwordFormRef.value.resetFields()
    
    // 修改密码后退出登录
    setTimeout(() => {
      userStore.logout()
    }, 1500)
    
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    passwordLoading.value = false
  }
}

// 初始化
onMounted(() => {
  // 填充表单
  infoForm.nickname = userInfo.value.nickname || ''
  infoForm.email = userInfo.value.email || ''
  infoForm.phone = userInfo.value.phone || ''
  
  // 加载存储统计
  loadStorageStats()
  
  // 加载传输统计
  loadTransferStats()
})
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  padding: 20px;
}

.profile-container {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  span {
    font-size: 16px;
    font-weight: 500;
  }
}

// 用户信息卡片
.info-card {
  .user-info {
    .avatar-section {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 30px;
      padding-bottom: 20px;
      border-bottom: 1px solid #ebeef5;
      
      .avatar-uploader {
        position: relative;
        
        :deep(.el-upload) {
          position: relative;
          cursor: pointer;
          
          &:hover {
            .avatar-upload-tip {
              opacity: 1;
            }
          }
        }
        
        .avatar-upload-tip {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.5);
          border-radius: 50%;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: #fff;
          font-size: 12px;
          opacity: 0;
          transition: opacity 0.3s;
          
          .el-icon {
            font-size: 20px;
            margin-bottom: 4px;
          }
        }
      }
      
      .user-basic {
        h3 {
          margin: 0 0 8px 0;
          font-size: 20px;
          color: #303133;
        }
      }
    }
    
    .info-form {
      max-width: 500px;
    }
  }
}

// 存储统计卡片
.storage-card {
  .storage-stats {
    .total-stats {
      display: flex;
      gap: 40px;
      margin-bottom: 30px;
      padding-bottom: 20px;
      border-bottom: 1px solid #ebeef5;
      
      .stat-item {
        text-align: center;
        
        .stat-value {
          font-size: 24px;
          font-weight: 600;
          color: #303133;
        }
        
        .stat-label {
          margin-top: 4px;
          font-size: 14px;
          color: #909399;
        }
        
        &.main {
          .stat-value {
            color: var(--el-color-primary);
          }
        }
      }
    }
    
    .category-stats {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 16px;
      
      .category-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        background: #f5f7fa;
        border-radius: 8px;
        
        .category-icon {
          width: 40px;
          height: 40px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          
          .el-icon {
            font-size: 20px;
          }
        }
        
        .category-info {
          flex: 1;
          
          .category-name {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
          }
          
          .category-detail {
            margin-top: 2px;
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
  }
}

// 传输统计卡片
.transfer-stats-card {
  .transfer-stats {
    .transfer-summary {
      display: flex;
      gap: 40px;
      margin: 20px 0;
      padding: 20px;
      background: #f5f7fa;
      border-radius: 8px;
      
      .summary-item {
        flex: 1;
        text-align: center;
        
        .summary-label {
          font-size: 14px;
          color: #909399;
          margin-bottom: 8px;
        }
        
        .summary-value {
          font-size: 24px;
          font-weight: bold;
          color: var(--el-color-primary);
        }
      }
    }
    
    .transfer-chart {
      display: flex;
      gap: 40px;
      margin-top: 20px;
      
      .chart-item {
        flex: 1;
        
        .chart-title {
          font-size: 14px;
          font-weight: bold;
          margin-bottom: 12px;
          text-align: center;
        }
        
        .chart-bars {
          display: flex;
          align-items: flex-end;
          justify-content: space-around;
          height: 150px;
          gap: 4px;
          
          .chart-bar {
            flex: 1;
            border-radius: 4px 4px 0 0;
            position: relative;
            min-height: 20px;
            transition: all 0.3s;
            
            &.upload {
              background: var(--el-color-primary);
            }
            
            &.download {
              background: var(--el-color-success);
            }
            
            .bar-value {
              position: absolute;
              top: -20px;
              left: 50%;
              transform: translateX(-50%);
              font-size: 10px;
              white-space: nowrap;
            }
            
            &:hover {
              opacity: 0.8;
            }
          }
        }
      }
    }
  }
}

// 修改密码卡片
.password-card {
  .password-form {
    max-width: 400px;
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .profile-page {
    padding: 16px;
  }
  
  .profile-container {
    gap: 16px;
  }
  
  .info-card {
    .user-info {
      .info-form {
        max-width: 100%;
      }
    }
  }
  
  .storage-card {
    .storage-stats {
      .total-stats {
        gap: 24px;
        
        .stat-item {
          .stat-value {
            font-size: 20px;
          }
        }
      }
      
      .category-stats {
        grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
        gap: 12px;
      }
    }
  }
  
  .password-card {
    .password-form {
      max-width: 100%;
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .profile-page {
    padding: 12px;
  }
  
  .profile-container {
    gap: 12px;
  }
  
  .card-header {
    span {
      font-size: 15px;
    }
  }
  
  .info-card {
    .user-info {
      .avatar-section {
        flex-direction: column;
        text-align: center;
        gap: 12px;
        margin-bottom: 20px;
        padding-bottom: 16px;
        
        .user-basic {
          h3 {
            font-size: 18px;
          }
        }
      }
      
      .info-form {
        :deep(.el-form-item__label) {
          width: 70px !important;
          font-size: 13px;
        }
        
        :deep(.el-form-item) {
          margin-bottom: 16px;
        }
      }
    }
  }
  
  .storage-card {
    .storage-stats {
      .total-stats {
        flex-direction: column;
        gap: 16px;
        align-items: flex-start;
        
        .stat-item {
          text-align: left;
          
          .stat-value {
            font-size: 22px;
          }
          
          .stat-label {
            font-size: 13px;
          }
        }
      }
      
      .category-stats {
        grid-template-columns: 1fr;
        gap: 10px;
        
        .category-item {
          padding: 10px;
          
          .category-icon {
            width: 36px;
            height: 36px;
            
            .el-icon {
              font-size: 18px;
            }
          }
          
          .category-info {
            .category-name {
              font-size: 13px;
            }
            
            .category-detail {
              font-size: 11px;
            }
          }
        }
      }
    }
  }
  
  .password-card {
    .password-form {
      :deep(.el-form-item__label) {
        width: 80px !important;
        font-size: 13px;
      }
      
      :deep(.el-form-item) {
        margin-bottom: 16px;
      }
    }
  }
}
</style>

