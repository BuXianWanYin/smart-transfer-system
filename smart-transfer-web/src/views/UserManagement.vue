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
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="queryParams.role" placeholder="请选择角色" clearable style="width: 150px">
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
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="id" label="ID" min-width="80" align="center" />
        <el-table-column label="用户" min-width="200" align="center">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="getAvatarUrl(row.avatar)" class="user-avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="user-name">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="150" show-overflow-tooltip align="center" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip align="center" />
        <el-table-column prop="phone" label="手机号" min-width="150" align="center" />
        <el-table-column prop="role" label="角色" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.lastLoginTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                link
                type="primary"
                size="small"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                link
                type="warning"
                size="small"
                @click="handleToggleStatus(row)"
                :disabled="isDisableButtonDisabled(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                @click="handleDelete(row)"
                :disabled="row.role === 'ADMIN'"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="editVisible"
      title="编辑用户"
      width="500px"
      @close="handleEditDialogClose"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="头像">
          <div class="edit-avatar-section">
            <el-avatar :size="64" :src="getAvatarUrl(editForm.avatar)" class="edit-avatar">
              <el-icon :size="32"><UserFilled /></el-icon>
            </el-avatar>
            <el-upload
              class="avatar-uploader"
              :http-request="handleEditAvatarUpload"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
            >
              <el-button size="small" type="primary">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { getUserList, updateUserStatus, deleteUser, batchUpdateUserStatus, batchDeleteUsers, updateUserInfoByAdmin, uploadAvatarByAdmin } from '@/api/userApi'

// 加载状态
const loading = ref(false)

// 用户列表
const userList = ref([])

// 选中的用户
const selectedUsers = ref([])

// 编辑用户
const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  userId: null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

// 编辑表单验证规则
const editRules = {
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
 * 判断禁用按钮是否应该被禁用
 * 如果是管理员且是最后一个启用的管理员，则禁用按钮
 */
const isDisableButtonDisabled = (row) => {
  // 如果要启用用户，按钮不应该被禁用
  if (row.status !== 1) {
    return false
  }
  
  // 如果不是管理员，可以禁用
  if (row.role !== 'ADMIN') {
    return false
  }
  
  // 如果是管理员，检查是否还有其他启用的管理员
  const enabledAdmins = userList.value.filter(user => 
    user.role === 'ADMIN' && user.status === 1 && user.id !== row.id
  )
  
  // 如果没有其他启用的管理员，禁用按钮
  return enabledAdmins.length === 0
}

/**
 * 编辑用户
 */
const handleEdit = (row) => {
  // 先清除之前的验证状态
  if (editFormRef.value) {
    editFormRef.value.clearValidate()
  }
  
  // 填充表单数据
  editForm.userId = row.id
  editForm.username = row.username
  editForm.nickname = row.nickname || ''
  editForm.email = row.email || ''
  editForm.phone = row.phone || ''
  editForm.avatar = row.avatar || ''
  
  // 打开弹窗
  editVisible.value = true
  
  // 使用 nextTick 确保弹窗打开后再清除验证状态（防止之前的状态残留）
  nextTick(() => {
    if (editFormRef.value) {
      editFormRef.value.clearValidate()
    }
  })
}

/**
 * 头像上传前验证
 */
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

/**
 * 编辑弹窗中上传头像（管理员为其他用户上传）
 */
const handleEditAvatarUpload = async (options) => {
  if (!editForm.userId) {
    ElMessage.error('用户ID不存在')
    return
  }
  
  try {
    const avatarPath = await uploadAvatarByAdmin(editForm.userId, options.file)
    if (avatarPath) {
      editForm.avatar = avatarPath
      ElMessage.success('头像上传成功')
    }
  } catch (error) {
    ElMessage.error('头像上传失败: ' + (error.message || '未知错误'))
  }
}

/**
 * 编辑弹窗关闭时的处理
 */
const handleEditDialogClose = () => {
  // 清除表单验证状态
  if (editFormRef.value) {
    editFormRef.value.clearValidate()
  }
  // 重置表单数据（可选，因为下次打开时会重新填充）
  editForm.userId = null
  editForm.username = ''
  editForm.nickname = ''
  editForm.email = ''
  editForm.phone = ''
  editForm.avatar = ''
}

/**
 * 保存编辑
 */
const handleSaveEdit = async () => {
  if (!editFormRef.value) return
  
  try {
    await editFormRef.value.validate()
    editLoading.value = true
    
    // 调用管理员更新用户信息接口
    await updateUserInfoByAdmin(editForm.userId, {
      nickname: editForm.nickname,
      email: editForm.email,
      phone: editForm.phone
    })
    
    // 如果头像已更新，需要单独更新头像
    // 注意：头像在handleEditAvatarUpload中已经上传并更新到数据库，这里不需要再次更新
    
    ElMessage.success('更新成功')
    editVisible.value = false
    loadUserList()
  } catch (error) {
    if (error !== false) { // 表单验证失败会返回false
      ElMessage.error('更新失败：' + (error.message || '未知错误'))
    }
  } finally {
    editLoading.value = false
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

/**
 * 获取头像URL
 */
const getAvatarUrl = (avatar) => {
  if (!avatar) return undefined
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  // 如果avatar已经是完整URL，直接返回；否则拼接
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar
  }
  // 相对路径格式：avatars/userId/filename
  return `${baseURL}/user/avatar/${avatar}`
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
    background: var(--art-fill-light);
    border-radius: 8px;
    display: flex;
    gap: 12px;
  }
  
  .user-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    
    .user-avatar {
      flex-shrink: 0;
    }
    
    .user-name {
      font-size: 14px;
      color: var(--art-text-gray-800);
    }
  }
  
  .edit-avatar-section {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .edit-avatar {
      flex-shrink: 0;
    }
    
    .avatar-uploader {
      :deep(.el-upload) {
        margin: 0;
      }
    }
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
  
  .action-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: nowrap;
    align-items: center;
    justify-content: center;
    
    .el-button {
      white-space: nowrap;
      padding: 0 8px;
    }
  }
}
</style>
