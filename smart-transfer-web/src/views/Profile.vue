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
            <el-avatar :size="80" :src="userInfo.avatar || undefined">
              <el-icon :size="40"><UserFilled /></el-icon>
            </el-avatar>
            <div class="user-basic">
              <h3>{{ userInfo.nickname || userInfo.username }}</h3>
            </div>
          </div>
          
          <el-form
            ref="infoFormRef"
            :model="infoForm"
            label-width="80px"
            class="info-form"
          >
            <el-form-item label="用户名">
              <el-input v-model="userInfo.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
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
import { UserFilled, Refresh, Picture, VideoPlay, Headset, Document, Folder } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/userStore'
import { updateUserInfo, changePassword, getStorageStats } from '@/api/userApi'

const userStore = useUserStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo || {})

// 信息表单
const infoFormRef = ref(null)
const infoForm = reactive({
  nickname: '',
  email: '',
  phone: ''
})
const infoLoading = ref(false)

// 存储统计
const storageLoading = ref(false)
const storageStats = ref({})

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

// 更新用户信息
const handleUpdateInfo = async () => {
  infoLoading.value = true
  try {
    await updateUserInfo({
      nickname: infoForm.nickname,
      email: infoForm.email,
      phone: infoForm.phone
    })
    ElMessage.success('信息更新成功')
    await userStore.refreshUserInfo()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
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

// 修改密码卡片
.password-card {
  .password-form {
    max-width: 400px;
  }
}
</style>

