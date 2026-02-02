<template>
  <div class="user-management-page">
    
    <!-- 搜索筛选区域 -->
    <div class="search-section">
      <el-form :model="queryParams" class="search-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="用户名" class="form-item">
              <el-input
                v-model="queryParams.username"
                placeholder="搜索用户名或昵称"
                clearable
                :prefix-icon="Search"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="状态" class="form-item">
              <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="角色" class="form-item">
              <el-select v-model="queryParams.role" placeholder="全部角色" clearable>
                <el-option label="管理员" value="ADMIN" />
                <el-option label="普通用户" value="USER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="注册时间" class="form-item">
              <el-date-picker
                v-model="queryParams.dateRange"
                type="daterange"
                range-separator="-"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" class="form-actions">
            <el-button type="primary" @click="handleQuery" :icon="Search">搜索</el-button>
            <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>
    
    <!-- 批量操作栏 - 玻璃态设计 -->
    <transition name="batch-slide">
      <div class="batch-actions-bar" v-if="selectedUsers.length > 0">
        <div class="batch-actions-container">
          <div class="batch-info-section">
            <div class="selection-badge">
              <div class="badge-content">
                <span class="badge-label">已选择</span>
                <span class="badge-count">{{ selectedUsers.length }}</span>
                <span class="badge-unit">个用户</span>
              </div>
            </div>
          </div>
          
          <div class="batch-actions-section">
            <button class="batch-btn batch-btn-enable" @click="handleBatchEnable">
              <span class="btn-icon">
                <el-icon><CircleCheck /></el-icon>
              </span>
              <span class="btn-text">批量启用</span>
            </button>
            
            <button class="batch-btn batch-btn-disable" @click="handleBatchDisable">
              <span class="btn-icon">
                <el-icon><CircleClose /></el-icon>
              </span>
              <span class="btn-text">批量禁用</span>
            </button>
            
            <button class="batch-btn batch-btn-delete" @click="handleBatchDelete">
              <span class="btn-icon">
                <el-icon><Delete /></el-icon>
              </span>
              <span class="btn-text">批量删除</span>
            </button>
            
            <button class="batch-btn batch-btn-clear" @click="selectedUsers = []">
              <span class="btn-icon">
                <el-icon><Close /></el-icon>
              </span>
            </button>
          </div>
        </div>
      </div>
    </transition>
    
    <!-- 用户表格 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="filteredUserList"
        class="modern-table"
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: '600' }"
        :row-class-name="tableRowClassName"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="id" label="ID" width="80" align="center">
          <template #default="{ row }">
            <span class="id-badge">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="200">
          <template #default="{ row }">
            <div class="user-info-cell">
              <el-avatar 
                :size="40" 
                :src="getAvatarUrl(row.avatar)" 
                class="user-avatar-modern"
              >
                <el-icon :size="20"><UserFilled /></el-icon>
              </el-avatar>
              <div class="user-details">
                <span class="user-name-primary">{{ row.username }}</span>
                <span class="user-nickname">{{ row.nickname || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="row.role === 'ADMIN' ? 'danger' : 'primary'"
              effect="plain"
              class="role-tag"
            >
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="contact-cell">
              <el-icon class="contact-icon"><Message /></el-icon>
              <span>{{ row.email || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140">
          <template #default="{ row }">
            <div class="contact-cell">
              <el-icon class="contact-icon"><Phone /></el-icon>
              <span>{{ row.phone || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <span :class="['status-dot', row.status === 1 ? 'status-active' : 'status-inactive']"></span>
              <span :class="['status-text', row.status === 1 ? 'text-active' : 'text-inactive']">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="160">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon class="time-icon"><Clock /></el-icon>
              <span>{{ formatDateShort(row.lastLoginTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="160">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon class="time-icon"><Calendar /></el-icon>
              <span>{{ formatDateShort(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons-modern">
              <el-tooltip content="编辑用户" placement="top">
                <el-button
                  circle
                  size="small"
                  @click="handleEdit(row)"
                  :icon="Edit"
                />
              </el-tooltip>
              <el-tooltip :content="row.status === 1 ? '禁用用户' : '启用用户'" placement="top">
                <el-button
                  circle
                  size="small"
                  :type="row.status === 1 ? 'warning' : 'success'"
                  @click="handleToggleStatus(row)"
                  :disabled="isDisableButtonDisabled(row)"
                  :icon="row.status === 1 ? CircleClose : CircleCheck"
                />
              </el-tooltip>
              <el-tooltip content="删除用户" placement="top">
                <el-button
                  circle
                  size="small"
                  type="danger"
                  @click="handleDelete(row)"
                  :disabled="row.role === 'ADMIN'"
                  :icon="Delete"
                />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="editVisible"
      :title="`编辑用户 - ${editForm.username}`"
      width="600px"
      @close="handleEditDialogClose"
      class="modern-dialog"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="90px"
        label-position="left"
        class="edit-form-modern"
      >
        <el-form-item label="用户头像">
          <div class="avatar-upload-section">
            <el-avatar :size="80" :src="getAvatarUrl(editForm.avatar)" class="preview-avatar">
              <el-icon :size="36"><UserFilled /></el-icon>
            </el-avatar>
            <div class="upload-actions">
              <el-upload
                class="avatar-uploader-modern"
                :http-request="handleEditAvatarUpload"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
              >
                <el-button type="primary" :icon="Upload">上传新头像</el-button>
              </el-upload>
              <p class="upload-tip">支持 JPG、PNG 格式，文件小于 5MB</p>
            </div>
          </div>
        </el-form-item>
        
        <el-divider />
        
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled :prefix-icon="User">
            <template #suffix>
              <el-tag size="small" type="info">不可修改</el-tag>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="昵称" prop="nickname">
          <el-input 
            v-model="editForm.nickname" 
            placeholder="请输入用户昵称"
            :prefix-icon="Edit"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="邮箱地址" prop="email">
          <el-input 
            v-model="editForm.email" 
            placeholder="请输入邮箱地址"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="手机号码" prop="phone">
          <el-input 
            v-model="editForm.phone" 
            placeholder="请输入手机号码"
            :prefix-icon="Phone"
            maxlength="11"
            clearable
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer-modern">
          <el-button @click="editVisible = false" size="large">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleSaveEdit" 
            :loading="editLoading"
            size="large"
            :icon="Check"
          >
            保存更改
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  UserFilled, Search, RefreshLeft, RefreshRight, 
  Edit, Delete, CircleCheck, CircleClose, InfoFilled, Close,
  Message, Phone, Clock, Calendar, Upload, User, Check
} from '@element-plus/icons-vue'
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
  role: null,
  dateRange: null
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
  
  // 日期范围过滤
  if (queryParams.dateRange && queryParams.dateRange.length === 2) {
    const startDate = new Date(queryParams.dateRange[0])
    const endDate = new Date(queryParams.dateRange[1])
    endDate.setHours(23, 59, 59, 999) // 设置为当天结束时间
    
    list = list.filter(user => {
      if (!user.createTime) return false
      const createTime = new Date(user.createTime)
      return createTime >= startDate && createTime <= endDate
    })
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
  queryParams.dateRange = null
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
 * 格式化日期（完整格式）
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
 * 格式化日期（简短格式，用于表格）
 */
const formatDateShort = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  const now = new Date()
  const diffTime = now - d
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  // 如果是今天
  if (diffDays === 0) {
    return '今天 ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  // 如果是昨天
  if (diffDays === 1) {
    return '昨天 ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  // 如果在一周内
  if (diffDays < 7) {
    return `${diffDays}天前`
  }
  // 否则显示完整日期
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

/**
 * 表格行类名
 */
const tableRowClassName = ({ row }) => {
  if (row.status === 0) {
    return 'row-disabled'
  }
  return ''
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
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
  
  /* 页面头部 */
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding: 0 4px;
    
    .page-title {
      h2 {
        margin: 0;
        font-size: 24px;
        font-weight: 700;
        color: #1f2937;
        letter-spacing: -0.02em;
      }
      
      .page-subtitle {
        display: block;
        margin-top: 4px;
        font-size: 14px;
        color: #6b7280;
        font-weight: 400;
      }
    }
  }
  
  /* 搜索筛选区域 */
  .search-section {
    background: white;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 20px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    transition: box-shadow 0.3s ease;
    
    &:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }
    
    .search-form {
      .form-item {
        margin-bottom: 16px;
        
        :deep(.el-form-item__label) {
          font-weight: 500;
          color: #374151;
          font-size: 13px;
        }
        
        :deep(.el-input),
        :deep(.el-select),
        :deep(.el-date-editor) {
          width: 100%;
        }
        
        :deep(.el-input__wrapper) {
          border-radius: 8px;
          box-shadow: 0 1px 2px rgba(0,0,0,0.05);
          transition: all 0.2s ease;
          
          &:hover {
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
          }
          
          &.is-focus {
            box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
          }
        }
      }
      
      .form-actions {
        display: flex;
        gap: 12px;
        justify-content: flex-start;
        margin-top: 8px;
        
        .el-button {
          border-radius: 8px;
          font-weight: 500;
          padding: 10px 24px;
        }
      }
    }
  }
  
  /* 批量操作栏 - 基于项目配色的玻璃态设计 */
  .batch-actions-bar {
    background: linear-gradient(135deg, 
      rgba(var(--art-bg-primary), 0.6) 0%, 
      rgba(var(--art-bg-secondary), 0.5) 100%
    );
    backdrop-filter: blur(20px) saturate(180%);
    -webkit-backdrop-filter: blur(20px) saturate(180%);
    border: 1px solid rgba(var(--art-primary), 0.12);
    border-radius: 16px;
    padding: 20px 28px;
    margin-bottom: 20px;
    box-shadow: 
      0 8px 32px rgba(var(--art-primary), 0.08),
      0 2px 8px rgba(0, 0, 0, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.8);
    animation: slideDown 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    position: relative;
    overflow: hidden;
    
    /* 装饰性背景元素 */
    &::before {
      content: '';
      position: absolute;
      top: -50%;
      right: -20%;
      width: 200px;
      height: 200px;
      background: radial-gradient(circle, rgba(var(--art-primary), 0.05) 0%, transparent 70%);
      border-radius: 50%;
      animation: float 6s ease-in-out infinite;
    }
    
    &::after {
      content: '';
      position: absolute;
      bottom: -30%;
      left: -10%;
      width: 150px;
      height: 150px;
      background: radial-gradient(circle, rgba(var(--art-secondary), 0.04) 0%, transparent 70%);
      border-radius: 50%;
      animation: float 8s ease-in-out infinite reverse;
    }
    
    .batch-actions-container {
      position: relative;
      z-index: 1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 24px;
    }
    
    /* 选择信息区域 */
    .batch-info-section {
      flex-shrink: 0;
      
      .selection-badge {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 12px 20px;
        background: rgba(255, 255, 255, 0.85);
        backdrop-filter: blur(10px);
        border-radius: 12px;
        border: 1px solid rgba(var(--art-primary), 0.15);
        box-shadow: 0 2px 8px rgba(var(--art-primary), 0.08);
        transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
        
        &:hover {
          transform: translateY(-2px) scale(1.02);
          box-shadow: 0 4px 16px rgba(var(--art-primary), 0.12);
          border-color: rgba(var(--art-primary), 0.25);
        }
        
        .badge-icon {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 36px;
          height: 36px;
          background: linear-gradient(135deg, 
            rgb(var(--art-primary)) 0%, 
            rgba(var(--art-primary), 0.85) 100%
          );
          border-radius: 10px;
          color: white;
          font-size: 18px;
          box-shadow: 0 2px 8px rgba(var(--art-primary), 0.25);
          animation: pulse-soft 2s ease-in-out infinite;
        }
        
        .badge-content {
          display: flex;
          align-items: baseline;
          gap: 6px;
          
          .badge-label {
            font-size: 13px;
            color: var(--art-gray-600);
            font-weight: 500;
          }
          
          .badge-count {
            font-size: 24px;
            font-weight: 800;
            background: linear-gradient(135deg, 
              rgb(var(--art-primary)) 0%, 
              rgb(var(--art-secondary)) 100%
            );
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            letter-spacing: -0.02em;
          }
          
          .badge-unit {
            font-size: 13px;
            color: var(--art-gray-600);
            font-weight: 500;
          }
        }
      }
    }
    
    /* 操作按钮区域 */
    .batch-actions-section {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      
      .batch-btn {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 20px;
        border: none;
        border-radius: 10px;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
        position: relative;
        overflow: hidden;
        backdrop-filter: blur(10px);
        
        .btn-icon {
          display: flex;
          align-items: center;
          font-size: 18px;
          transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
        }
        
        .btn-text {
          transition: transform 0.3s ease;
        }
        
        /* 按钮涟漪效果 */
        &::before {
          content: '';
          position: absolute;
          top: 50%;
          left: 50%;
          width: 0;
          height: 0;
          border-radius: 50%;
          background: rgba(255, 255, 255, 0.3);
          transform: translate(-50%, -50%);
          transition: width 0.6s, height 0.6s;
        }
        
        &:hover {
          transform: translateY(-3px) scale(1.02);
          
          .btn-icon {
            transform: scale(1.15) rotate(5deg);
          }
          
          &::before {
            width: 300px;
            height: 300px;
          }
        }
        
        &:active {
          transform: translateY(-1px) scale(0.98);
        }
        
        /* 启用按钮 - 使用项目成功色 */
        &.batch-btn-enable {
          background: rgba(var(--art-bg-success), 0.8);
          color: rgb(var(--art-success));
          border: 1px solid rgba(var(--art-success), 0.3);
          box-shadow: 
            0 4px 12px rgba(var(--art-success), 0.15),
            inset 0 1px 0 rgba(255, 255, 255, 0.5);
          
          &:hover {
            background: rgba(var(--art-bg-success), 1);
            border-color: rgba(var(--art-success), 0.5);
            box-shadow: 
              0 6px 20px rgba(var(--art-success), 0.25),
              inset 0 1px 0 rgba(255, 255, 255, 0.6);
          }
        }
        
        /* 禁用按钮 - 使用项目警告色 */
        &.batch-btn-disable {
          background: rgba(var(--art-bg-warning), 0.8);
          color: rgb(var(--art-warning));
          border: 1px solid rgba(var(--art-warning), 0.3);
          box-shadow: 
            0 4px 12px rgba(var(--art-warning), 0.15),
            inset 0 1px 0 rgba(255, 255, 255, 0.5);
          
          &:hover {
            background: rgba(var(--art-bg-warning), 1);
            border-color: rgba(var(--art-warning), 0.5);
            box-shadow: 
              0 6px 20px rgba(var(--art-warning), 0.25),
              inset 0 1px 0 rgba(255, 255, 255, 0.6);
          }
        }
        
        /* 删除按钮 - 使用项目危险色 */
        &.batch-btn-delete {
          background: rgba(var(--art-bg-danger), 0.8);
          color: rgb(var(--art-danger));
          border: 1px solid rgba(var(--art-danger), 0.3);
          box-shadow: 
            0 4px 12px rgba(var(--art-danger), 0.15),
            inset 0 1px 0 rgba(255, 255, 255, 0.5);
          
          &:hover {
            background: rgba(var(--art-bg-danger), 1);
            border-color: rgba(var(--art-danger), 0.5);
            box-shadow: 
              0 6px 20px rgba(var(--art-danger), 0.25),
              inset 0 1px 0 rgba(255, 255, 255, 0.6);
          }
        }
        
        /* 清除按钮 */
        &.batch-btn-clear {
          width: 40px;
          height: 40px;
          padding: 0;
          background: rgba(var(--art-gray-500-rgb), 0.06);
          border: 1px solid rgba(var(--art-gray-500-rgb), 0.15);
          color: var(--art-gray-600);
          display: flex;
          align-items: center;
          justify-content: center;
          
          .btn-icon {
            font-size: 20px;
            margin: 0;
          }
          
          &:hover {
            background: rgba(var(--art-danger), 0.08);
            border-color: rgba(var(--art-danger), 0.25);
            color: rgb(var(--art-danger));
            box-shadow: 0 4px 12px rgba(var(--art-danger), 0.12);
          }
        }
      }
    }
  }
  
  /* 浮动动画 */
  @keyframes float {
    0%, 100% {
      transform: translateY(0) scale(1);
    }
    50% {
      transform: translateY(-20px) scale(1.05);
    }
  }
  
  /* 脉冲动画 - 使用项目主色 */
  @keyframes pulse-soft {
    0%, 100% {
      box-shadow: 0 2px 8px rgba(var(--art-primary), 0.25);
    }
    50% {
      box-shadow: 0 4px 16px rgba(var(--art-primary), 0.4);
    }
  }
  
  /* 表格容器 */
  .table-container {
    background: white;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    overflow: hidden;
    
    .modern-table {
      :deep(.el-table__header) {
        th {
          border-bottom: 2px solid #e5e7eb;
          font-size: 13px;
          letter-spacing: 0.02em;
          text-transform: uppercase;
        }
      }
      
      :deep(.el-table__body) {
        tr {
          transition: all 0.2s ease;
          
          &:hover {
            background: #f9fafb !important;
            transform: scale(1.001);
            box-shadow: 0 2px 8px rgba(0,0,0,0.04);
          }
          
          &.row-disabled {
            opacity: 0.6;
            background: #fef3f2;
            
            &:hover {
              background: #fde8e7 !important;
            }
          }
          
          td {
            border-bottom: 1px solid #f3f4f6;
            padding: 16px 0;
          }
        }
      }
      
      :deep(.el-checkbox) {
        .el-checkbox__inner {
          border-radius: 4px;
          border-width: 2px;
        }
      }
    }
  }
  
  /* ID徽章 */
  .id-badge {
    display: inline-block;
    padding: 4px 10px;
    background: #f3f4f6;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    color: #6b7280;
    font-family: 'Monaco', 'Menlo', monospace;
  }
  
  /* 用户信息单元格 */
  .user-info-cell {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 4px 0;
    
    .user-avatar-modern {
      flex-shrink: 0;
      border: 2px solid #f3f4f6;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08);
      transition: all 0.2s ease;
      
      &:hover {
        transform: scale(1.1);
        border-color: #3b82f6;
      }
    }
    
    .user-details {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 4px;
      
      .user-name-primary {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }
      
      .user-nickname {
        font-size: 13px;
        color: #6b7280;
      }
    }
  }
  
  /* 角色标签 */
  .role-tag {
    font-weight: 500;
    border-radius: 6px;
    padding: 4px 12px;
    
    &.el-tag--danger {
      background: #3b82f6;
      border: none;
      color: white;
    }
    
    &.el-tag--primary {
      background: #10b981;
      border: none;
      color: white;
    }
  }
  
  /* 联系方式单元格 */
  .contact-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: #4b5563;
    
    .contact-icon {
      color: #9ca3af;
      font-size: 16px;
    }
  }
  
  /* 状态单元格 */
  .status-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      animation: pulse 2s infinite;
      
      &.status-active {
        background: #10b981;
        box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
      }
      
      &.status-inactive {
        background: #ef4444;
        box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
      }
    }
    
    .status-text {
      font-size: 13px;
      font-weight: 500;
      
      &.text-active {
        color: #059669;
      }
      
      &.text-inactive {
        color: #dc2626;
      }
    }
  }
  
  /* 时间单元格 */
  .time-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #6b7280;
    
    .time-icon {
      color: #9ca3af;
      font-size: 14px;
    }
  }
  
  /* 操作按钮 */
  .action-buttons-modern {
    display: flex;
    gap: 8px;
    justify-content: center;
    
    .el-button {
      transition: all 0.2s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      }
      
      &.is-disabled {
        opacity: 0.4;
        cursor: not-allowed;
        
        &:hover {
          transform: none;
          box-shadow: none;
        }
      }
    }
  }
  
  /* 编辑对话框 */
  :deep(.modern-dialog) {
    border-radius: 16px;
    overflow: hidden;
    
    .el-dialog__header {
      background: #10b981;
      color: white;
      padding: 24px;
      margin: 0;
      
      .el-dialog__title {
        color: white;
        font-weight: 700;
        font-size: 18px;
      }
      
      .el-dialog__headerbtn {
        .el-dialog__close {
          color: white;
          font-size: 20px;
          
          &:hover {
            color: rgba(255, 255, 255, 0.8);
          }
        }
      }
    }
    
    .el-dialog__body {
      padding: 32px;
    }
    
    .el-dialog__footer {
      padding: 20px 32px 32px;
    }
  }
  
  .edit-form-modern {
    .el-form-item {
      margin-bottom: 24px;
      
      :deep(.el-form-item__label) {
        font-weight: 600;
        color: #374151;
        font-size: 14px;
      }
      
      :deep(.el-input__wrapper) {
        border-radius: 8px;
        box-shadow: 0 1px 2px rgba(0,0,0,0.05);
        transition: all 0.2s ease;
        
        &:hover {
          box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }
        
        &.is-focus {
          box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
        }
      }
      
      :deep(.el-input.is-disabled .el-input__wrapper) {
        background: #f9fafb;
      }
    }
  }
  
  /* 头像上传区域 */
  .avatar-upload-section {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 20px;
    background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
    border-radius: 12px;
    border: 2px dashed #e5e7eb;
    
    .preview-avatar {
      flex-shrink: 0;
      border: 3px solid white;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
    
    .upload-actions {
      flex: 1;
      
      .avatar-uploader-modern {
        :deep(.el-upload) {
          margin: 0;
        }
        
        .el-button {
          border-radius: 8px;
          font-weight: 500;
        }
      }
      
      .upload-tip {
        margin: 12px 0 0 0;
        font-size: 12px;
        color: #6b7280;
        line-height: 1.5;
      }
    }
  }
  
  .dialog-footer-modern {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    
    .el-button {
      border-radius: 8px;
      font-weight: 500;
      padding: 10px 24px;
    }
  }
  
  /* 滑入动画（增强版） */
  @keyframes slideDown {
    0% {
      transform: translateY(-30px) scale(0.95);
      opacity: 0;
    }
    60% {
      transform: translateY(5px) scale(1.01);
      opacity: 1;
    }
    100% {
      transform: translateY(0) scale(1);
      opacity: 1;
    }
  }
  
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.6;
    }
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    padding: 16px;
    
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
    }
    
    .search-section {
      padding: 16px;
    }
    
    .batch-actions-bar {
      padding: 16px 20px;
      
      .batch-actions-container {
        flex-direction: column;
        gap: 16px;
      }
      
      .batch-info-section {
        width: 100%;
        
        .selection-badge {
          width: 100%;
          justify-content: center;
        }
      }
      
      .batch-actions-section {
        width: 100%;
        justify-content: center;
        
        .batch-btn {
          flex: 1;
          min-width: 0;
          justify-content: center;
          
          .btn-text {
            display: none;
          }
          
          &:not(.batch-btn-clear) {
            .btn-text {
              display: inline;
            }
          }
        }
      }
    }
    
    .table-container {
      padding: 12px;
      border-radius: 8px;
    }
    
    :deep(.modern-dialog) {
      .el-dialog__body {
        padding: 20px;
      }
    }
  }
}

/* 批量操作栏过渡动画 - 弹性效果 */
.batch-slide-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.batch-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.6, 1);
}

.batch-slide-enter-from {
  transform: translateY(-30px) scale(0.9);
  opacity: 0;
}

.batch-slide-leave-to {
  transform: translateY(-20px) scale(0.95);
  opacity: 0;
}
</style>
