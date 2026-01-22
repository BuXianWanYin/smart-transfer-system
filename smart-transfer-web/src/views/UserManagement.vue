<template>
  <div class="user-management-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button @click="loadUserList">刷新</el-button>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="queryParams.role" placeholder="请选择角色" clearable>
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 批量操作栏 -->
      <div class="batch-actions" v-if="selectedUsers.length > 0">
        <el-button type="primary" @click="handleBatchEnable">
          批量启用 ({{ selectedUsers.length }})
        </el-button>
        <el-button type="warning" @click="handleBatchDisable">
          批量禁用 ({{ selectedUsers.length }})
        </el-button>
        <el-button type="danger" @click="handleBatchDelete">
          批量删除 ({{ selectedUsers.length }})
        </el-button>
      </div>
      
      <!-- 用户表格 -->
      <el-table
        v-loading="loading"
        :data="filteredUserList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastLoginTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="info"
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
            <el-button
              link
              type="primary"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row)"
              :disabled="row.role === 'ADMIN'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="用户详情"
      width="800px"
    >
      <div v-loading="detailLoading" class="user-detail">
        <el-descriptions :column="2" border v-if="userDetail">
          <el-descriptions-item label="用户名">{{ userDetail.userInfo?.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ userDetail.userInfo?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userDetail.userInfo?.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userDetail.userInfo?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="userDetail.userInfo?.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ userDetail.userInfo?.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="userDetail.userInfo?.status === 1 ? 'success' : 'danger'">
              {{ userDetail.userInfo?.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <el-divider>存储统计</el-divider>
        <div class="detail-stats" v-if="userDetail.storageStats">
          <div class="stat-row">
            <span>总存储：</span>
            <strong>{{ formatSize(userDetail.storageStats.totalSize) }}</strong>
          </div>
          <div class="stat-row">
            <span>文件数：</span>
            <strong>{{ userDetail.storageStats.fileCount || 0 }}</strong>
          </div>
          <div class="stat-row">
            <span>图片：</span>
            <strong>{{ formatSize(userDetail.storageStats.imageSize) }} ({{ userDetail.storageStats.imageCount || 0 }} 个)</strong>
          </div>
          <div class="stat-row">
            <span>视频：</span>
            <strong>{{ formatSize(userDetail.storageStats.videoSize) }} ({{ userDetail.storageStats.videoCount || 0 }} 个)</strong>
          </div>
          <div class="stat-row">
            <span>文档：</span>
            <strong>{{ formatSize(userDetail.storageStats.docSize) }} ({{ userDetail.storageStats.docCount || 0 }} 个)</strong>
          </div>
        </div>
        
        <el-divider>传输统计</el-divider>
        <div class="detail-stats" v-if="userDetail.transferStats">
          <div class="stat-row">
            <span>上传总量：</span>
            <strong>{{ formatSize(userDetail.transferStats.uploadValues?.reduce((a, b) => a + b, 0) || 0) }}</strong>
          </div>
          <div class="stat-row">
            <span>下载总量：</span>
            <strong>{{ formatSize(userDetail.transferStats.downloadValues?.reduce((a, b) => a + b, 0) || 0) }}</strong>
          </div>
        </div>
        
        <el-divider>算法使用统计</el-divider>
        <div class="detail-stats" v-if="userDetail.algorithmStats && userDetail.algorithmStats.algorithmLabels">
          <div 
            v-for="(label, index) in userDetail.algorithmStats.algorithmLabels" 
            :key="label"
            class="stat-row"
          >
            <span>{{ label }}：</span>
            <strong>
              {{ userDetail.algorithmStats.countValues && userDetail.algorithmStats.countValues[index] !== undefined ? userDetail.algorithmStats.countValues[index] : 0 }} 次，
              {{ formatSize(userDetail.algorithmStats.sizeValues && userDetail.algorithmStats.sizeValues[index] !== undefined ? userDetail.algorithmStats.sizeValues[index] : 0) }}
            </strong>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUserStatus, deleteUser, getUserDetail, batchUpdateUserStatus, batchDeleteUsers } from '@/api/userApi'

// 加载状态
const loading = ref(false)

// 用户列表
const userList = ref([])

// 选中的用户
const selectedUsers = ref([])

// 用户详情
const detailVisible = ref(false)
const detailLoading = ref(false)
const userDetail = ref(null)

// 查询参数
const queryParams = reactive({
  username: '',
  status: null,
  role: null
})

// 过滤后的用户列表
const filteredUserList = computed(() => {
  let list = userList.value
  
  if (queryParams.username) {
    list = list.filter(user => 
      user.username.toLowerCase().includes(queryParams.username.toLowerCase()) ||
      (user.nickname && user.nickname.toLowerCase().includes(queryParams.username.toLowerCase()))
    )
  }
  
  if (queryParams.status !== null) {
    list = list.filter(user => user.status === queryParams.status)
  }
  
  if (queryParams.role) {
    list = list.filter(user => user.role === queryParams.role)
  }
  
  return list
})

/**
 * 加载用户列表
 */
const loadUserList = async () => {
  loading.value = true
  try {
    const data = await getUserList()
    userList.value = data || []
  } catch (error) {
    ElMessage.error('加载用户列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
const handleQuery = () => {
  // 使用computed自动过滤
}

/**
 * 重置搜索
 */
const handleReset = () => {
  queryParams.username = ''
  queryParams.status = null
  queryParams.role = null
}

/**
 * 切换用户状态
 */
const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户 "${row.username}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败：` + (error.message || '未知错误'))
    }
  }
}

/**
 * 删除用户
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${row.username}" 吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + (error.message || '未知错误'))
    }
  }
}

/**
 * 选择变化
 */
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

/**
 * 查看用户详情
 */
const handleViewDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const data = await getUserDetail(row.id)
    userDetail.value = data
  } catch (error) {
    ElMessage.error('加载用户详情失败：' + (error.message || '未知错误'))
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/**
 * 批量启用
 */
const handleBatchEnable = async () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要启用选中的 ${selectedUsers.value.length} 个用户吗？`,
      '提示',
      { type: 'warning' }
    )
    
    const userIds = selectedUsers.value.map(u => u.id)
    await batchUpdateUserStatus(userIds, 1)
    ElMessage.success('批量启用成功')
    selectedUsers.value = []
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量启用失败：' + (error.message || '未知错误'))
    }
  }
}

/**
 * 批量禁用
 */
const handleBatchDisable = async () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要禁用选中的 ${selectedUsers.value.length} 个用户吗？`,
      '提示',
      { type: 'warning' }
    )
    
    const userIds = selectedUsers.value.map(u => u.id)
    await batchUpdateUserStatus(userIds, 0)
    ElMessage.success('批量禁用成功')
    selectedUsers.value = []
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量禁用失败：' + (error.message || '未知错误'))
    }
  }
}

/**
 * 批量删除
 */
const handleBatchDelete = async () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }
  
  // 检查是否包含管理员
  const hasAdmin = selectedUsers.value.some(u => u.role === 'ADMIN')
  if (hasAdmin) {
    ElMessage.warning('不能删除管理员用户')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedUsers.value.length} 个用户吗？此操作不可恢复！`,
      '警告',
      { type: 'warning' }
    )
    
    const userIds = selectedUsers.value.map(u => u.id)
    await batchDeleteUsers(userIds)
    ElMessage.success('批量删除成功')
    selectedUsers.value = []
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败：' + (error.message || '未知错误'))
    }
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
 * 格式化日期
 */
const formatDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  loadUserList()
})
</script>

<style lang="scss" scoped>
.user-management-page {
  padding: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .search-form {
    margin-bottom: 20px;
  }
  
  .batch-actions {
    margin-bottom: 16px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    display: flex;
    gap: 12px;
  }
  
  .user-detail {
    .detail-stats {
      .stat-row {
        display: flex;
        justify-content: space-between;
        padding: 8px 0;
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
}
</style>
