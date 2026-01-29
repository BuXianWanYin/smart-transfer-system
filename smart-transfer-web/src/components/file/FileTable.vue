<template>
  <div class="file-table-wrapper" :class="{ 'mobile': isMobile }">
    <el-table
      ref="tableRef"
      class="file-table"
      v-loading="loading"
      element-loading-text="文件加载中……"
      :data="fileList"
      :highlight-current-row="true"
      @selection-change="handleSelectRow"
      @row-contextmenu="handleContextMenu"
      @row-dblclick="handleRowDblClick"
    >
      <!-- 空状态 -->
      <template #empty>
        <el-empty description="暂无数据">
          <template #image>
            <el-icon :size="80" color="#c0c4cc">
              <FolderOpened />
            </el-icon>
          </template>
        </el-empty>
      </template>
      <!-- 选择列 -->
      <el-table-column type="selection" width="55" />
      
      <!-- 图标列（支持图片缩略图） -->
      <el-table-column label="" width="56" align="center" class-name="file-icon-column">
        <template #default="{ row }">
          <!-- 图片文件显示缩略图 -->
          <img
            v-if="isImageFile(row)"
            :src="getThumbnailUrl(row)"
            class="file-thumbnail"
            :title="'点击预览'"
            @click="handleFileClick(row)"
            @error="handleThumbnailError($event, row)"
          />
          <!-- 其他文件显示图标 -->
          <img
            v-else
            :src="getFileIcon(row)"
            class="file-icon"
            :title="row.isDir ? '' : '点击预览'"
            @click="handleFileClick(row)"
          />
        </template>
      </el-table-column>
      
      <!-- 文件名列 -->
      <el-table-column prop="fileName" label="文件名" sortable show-overflow-tooltip min-width="200">
        <template #default="{ row }">
          <span class="file-name" @click="handleFileClick(row)">
            {{ getFileName(row) }}
          </span>
        </template>
      </el-table-column>
      
      <!-- 所属用户列（仅管理员可见） -->
      <el-table-column
        prop="userId"
        label="所属用户"
        width="160"
        sortable
        align="center"
        v-if="userStore.isAdmin && !isMobile"
      >
        <template #default="{ row }">
          <div v-if="row.userId" class="user-info">
            <el-avatar 
              :size="28" 
              :src="getUserAvatar(row.userId)"
              class="user-avatar"
            >
              {{ getUserName(row.userId).charAt(0) }}
            </el-avatar>
            <span class="user-name">{{ getUserName(row.userId) }}</span>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      
      <!-- 类型列（可配置显示） -->
      <el-table-column 
        prop="extendName" 
        label="类型" 
        width="100" 
        sortable 
        show-overflow-tooltip 
        v-if="!isMobile && visibleColumns.includes('extendName')"
      >
        <template #default="{ row }">
          <span>{{ getFileType(row) }}</span>
        </template>
      </el-table-column>
      
      <!-- 大小列（可配置显示） -->
      <el-table-column 
        prop="fileSize" 
        label="大小" 
        width="100" 
        sortable 
        align="right"
        v-if="visibleColumns.includes('fileSize')"
      >
        <template #default="{ row }">
          {{ row.isDir === 1 ? '-' : formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      
      <!-- 修改日期列（可配置显示） -->
      <el-table-column
        prop="updateTime"
        label="修改日期"
        width="180"
        sortable
        align="center"
        v-if="fileType !== 6 && !isMobile && visibleColumns.includes('updateTime')"
      >
        <template #default="{ row }">
          {{ formatDate(row.updateTime || row.createTime) }}
        </template>
      </el-table-column>
      
      <!-- 删除日期列（回收站，可配置显示） -->
      <el-table-column
        prop="deleteTime"
        label="删除日期"
        width="180"
        sortable
        align="center"
        v-if="fileType === 6 && !isMobile && visibleColumns.includes('deleteTime')"
      >
        <template #default="{ row }">
          {{ formatDate(row.deleteTime) }}
        </template>
      </el-table-column>
      
      <!-- 操作列（移动端显示） -->
      <el-table-column label="" width="48" v-if="isMobile">
        <template #default="{ row }">
          <el-icon class="file-operate" @click.stop="handleClickMore(row, $event)">
            <More />
          </el-icon>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 右键菜单 -->
    <div
      v-if="contextMenuVisible"
      class="right-menu-list"
      :style="menuStyle"
    >
      <!-- 普通文件菜单 -->
      <template v-if="fileType !== 6">
        <div class="right-menu-item" @click="handleMenuDownload" v-if="contextMenuRow?.isDir !== 1">
          <el-icon><Download /></el-icon>
          下载
        </div>
        <div class="right-menu-item" @click="handleMenuPreview" v-if="canPreview(contextMenuRow)">
          <el-icon><View /></el-icon>
          预览
        </div>
        <div class="right-menu-item" @click="handleMenuRename">
          <el-icon><Edit /></el-icon>
          重命名
        </div>
        <div class="right-menu-item" @click="handleMenuCopy">
          <el-icon><CopyDocument /></el-icon>
          复制到
        </div>
        <div class="right-menu-item" @click="handleMenuMove">
          <el-icon><FolderOpened /></el-icon>
          移动到
        </div>
        <div class="right-menu-item" @click="handleMenuCodePreview" v-if="isCodeFile(contextMenuRow)">
          <el-icon><Document /></el-icon>
          代码预览
        </div>
        <div class="right-menu-item" @click="handleMenuUnzip" v-if="canUnzip(contextMenuRow)">
          <el-icon><Files /></el-icon>
          解压
        </div>
        <div class="right-menu-item" @click="handleMenuDetail">
          <el-icon><InfoFilled /></el-icon>
          详情
        </div>
        <div class="right-menu-item danger" @click="handleMenuDelete">
          <el-icon><Delete /></el-icon>
          删除
        </div>
      </template>
      
      <!-- 回收站菜单：只有还原和彻底删除 -->
      <template v-else>
        <div class="right-menu-item" @click="handleMenuRestore">
          <el-icon><RefreshLeft /></el-icon>
          还原
        </div>
        <div class="right-menu-item danger" @click="handleMenuDelete">
          <el-icon><Delete /></el-icon>
          彻底删除
        </div>
      </template>
    </div>
    
    <!-- 重命名对话框 -->
    <el-dialog v-model="renameVisible" title="重命名" width="400px" append-to-body>
      <el-form ref="renameFormRef" :model="renameForm" :rules="renameRules">
        <el-form-item prop="fileName">
          <el-input v-model="renameForm.fileName" placeholder="请输入新名称" @keyup.enter="confirmRename" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename" :loading="renameLoading">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 移动文件对话框 -->
    <MoveFileDialog v-model="moveDialogVisible" @confirm="confirmMove" />
    
    <!-- 复制文件对话框 -->
    <CopyFileDialog v-model="copyDialogVisible" :files="copyFileData ? [copyFileData] : []" @success="handleRefresh" />
    
    <!-- 解压文件对话框 -->
    <UnzipDialog v-model="unzipDialogVisible" :file="unzipFileData" @success="handleRefresh" />
    
    <!-- 增强版图片预览 -->
    <ImagePreview
      v-model="imagePreviewVisible"
      :image-list="imageListForPreview"
      :initial-index="imagePreviewIndex"
    />
    
    <!-- 文件详情弹窗 -->
    <FileDetailDialog
      v-model="detailDialogVisible"
      :file="detailFileData"
      :file-type="fileType"
      @preview="handleFileClick"
    />
    
    <!-- 代码预览 -->
    <CodePreview
      v-model="codePreviewVisible"
      :file="codePreviewFile"
      :read-only="true"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { More, Download, View, Edit, FolderOpened, Delete, RefreshLeft, CopyDocument, Files, InfoFilled, Document } from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import CopyFileDialog from './CopyFileDialog.vue'
import UnzipDialog from './UnzipDialog.vue'
import ImagePreview from './ImagePreview.vue'
import FileDetailDialog from './FileDetailDialog.vue'
import CodePreview from './CodePreview.vue'
import { formatFileSize, formatDateTime } from '@/utils/format'
import { getFileIconByType, canPreviewFile } from '@/utils/fileType'
import { renameFile, moveFile, deleteFile, getPreviewUrl } from '@/api/fileApi'
import { deleteFolder } from '@/api/folderApi'
import { restoreRecoveryFile, deleteRecoveryFile } from '@/api/recoveryApi'
import { useTransferStore } from '@/store/transferStore'
import { useUserStore } from '@/store/userStore'
import { getUserList } from '@/api/userApi'
import { useRouter } from 'vue-router'

const props = defineProps({
  fileType: { type: Number, required: true },
  filePath: { type: String, required: true },
  fileList: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  visibleColumns: { type: Array, default: () => ['extendName', 'fileSize', 'updateTime'] }
})

const emit = defineEmits(['refresh', 'row-click', 'selection-change'])

const transferStore = useTransferStore()
const userStore = useUserStore()
const router = useRouter()
const tableRef = ref(null)

// 用户列表（仅管理员使用）
const userList = ref([])

// 加载用户列表（仅管理员）
onMounted(async () => {
  if (userStore.isAdmin) {
    try {
      const res = await getUserList()
      userList.value = res || []
      console.log('✅ 用户列表加载成功:', userList.value)
    } catch (error) {
      console.error('❌ 加载用户列表失败:', error)
    }
  }
})

// 获取用户名（根据用户ID）
const getUserName = (userId) => {
  console.log('🔍 查找用户:', userId, '用户列表:', userList.value)
  if (!userId) return '-'
  const user = userList.value.find(u => u.id === userId)
  const name = user ? (user.nickname || user.username) : `用户${userId}`
  console.log('👤 用户名:', name, '用户对象:', user)
  return name
}

// 获取用户头像（根据用户ID）
const getUserAvatar = (userId) => {
  if (!userId) return ''
  const user = userList.value.find(u => u.id === userId)
  if (user && user.avatar) {
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    // 如果avatar已经是完整URL，直接返回；否则拼接
    if (user.avatar.startsWith('http://') || user.avatar.startsWith('https://')) {
      return user.avatar
    }
    // 相对路径格式：avatars/userId/filename
    const timestamp = new Date().getTime()
    return `${baseURL}/user/avatar/${user.avatar}?t=${timestamp}`
  }
  return ''
}

// 屏幕宽度检测
const screenWidth = ref(window.innerWidth)
const isMobile = computed(() => screenWidth.value <= 768)

// 右键菜单
const contextMenuVisible = ref(false)
const contextMenuPos = ref({ x: 0, y: 0 })
const contextMenuRow = ref(null)

// 计算菜单位置，防止溢出屏幕
const menuStyle = computed(() => {
  const menuWidth = 140
  const menuHeight = 220
  let x = contextMenuPos.value.x
  let y = contextMenuPos.value.y
  
  // 防止右侧溢出
  if (x + menuWidth > window.innerWidth) {
    x = window.innerWidth - menuWidth - 10
  }
  // 防止底部溢出
  if (y + menuHeight > window.innerHeight) {
    y = window.innerHeight - menuHeight - 10
  }
  
  return { left: x + 'px', top: y + 'px' }
})

// 重命名
const renameVisible = ref(false)
const renameFormRef = ref(null)
const renameForm = ref({ fileName: '' })
const renameRules = { fileName: [{ required: true, message: '请输入名称', trigger: 'blur' }] }
const renameLoading = ref(false)

// 移动
const moveDialogVisible = ref(false)
const moveFileData = ref(null)

// 格式化
const formatSize = (size) => formatFileSize(size)
const formatDate = (date) => formatDateTime(date)

// 获取文件图标
const getFileIcon = (row) => {
  if (row.isDir === 1) return '/icons/folder.svg'
  return getFileIconByType(row.extendName)
}

// 判断是否是图片文件
const isImageFile = (row) => {
  if (row.isDir === 1) return false
  const ext = (row.extendName || '').toLowerCase()
  return imageExtensions.includes(ext)
}

// 获取缩略图 URL
const getThumbnailUrl = (row) => {
  return getPreviewUrl(row.id)
}

// 缩略图加载失败时显示图标
const handleThumbnailError = (event, row) => {
  event.target.src = getFileIconByType(row.extendName)
  event.target.classList.remove('file-thumbnail')
  event.target.classList.add('file-icon')
}

// 获取文件名
const getFileName = (row) => {
  return row.fileName || row.folderName || ''
}

// 获取文件类型
const getFileType = (row) => {
  if (row.isDir === 1) return '文件夹'
  return row.extendName || '-'
}

// 是否可预览
const canPreview = (row) => {
  if (!row || row.isDir === 1) return false
  return canPreviewFile(row.extendName)
}

// 文件点击
const handleFileClick = (row) => {
  emit('row-click', row)
}

// 双击行
const handleRowDblClick = (row) => {
  emit('row-click', row)
}

// 选择变化
const handleSelectRow = (selection) => {
  emit('selection-change', selection)
}

// 右键菜单
const handleContextMenu = (row, column, event) => {
  event.cancelBubble = true
  event.preventDefault()
  tableRef.value?.setCurrentRow(row)
  contextMenuRow.value = row
  contextMenuPos.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

// 更多按钮点击（移动端）
const handleClickMore = (row, event) => {
  contextMenuRow.value = row
  contextMenuPos.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

// 关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false
  contextMenuRow.value = null
  tableRef.value?.setCurrentRow()
}

// 右键菜单操作 - 添加到下载列表
const handleMenuDownload = () => {
  if (contextMenuRow.value) {
    const file = contextMenuRow.value
    // 添加到传输列表
    transferStore.addDownloadTask({
      fileId: file.id,
      fileName: file.fileName,
      fileSize: file.fileSize,
      fileHash: file.fileHash
    })
    ElMessage.success(`已添加 "${file.fileName}" 到下载列表`)
    // 跳转到传输中心
    router.push({ name: 'TransferCenter' })
  }
  closeContextMenu()
}

const handleMenuPreview = () => {
  if (contextMenuRow.value) {
    emit('row-click', contextMenuRow.value)
  }
  closeContextMenu()
}

const handleMenuRename = () => {
  renameForm.value.fileName = getFileName(contextMenuRow.value)
  renameVisible.value = true
  closeContextMenu()
}

const handleMenuMove = () => {
  moveFileData.value = { ...contextMenuRow.value }
  moveDialogVisible.value = true
  closeContextMenu()
}

const handleMenuDelete = async () => {
  const row = contextMenuRow.value
  closeContextMenu()
  
  try {
    await ElMessageBox.confirm(
      `确定要${props.fileType === 6 ? '彻底删除' : '删除'} "${getFileName(row)}" 吗？`,
      '确认删除',
      { type: 'warning' }
    )
    
    if (props.fileType === 6) {
      // 回收站 - 彻底删除
      await deleteRecoveryFile(row.id)
    } else if (row.isDir === 1) {
      // 删除文件夹
      await deleteFolder(row.id)
    } else {
      // 删除文件
      await deleteFile(row.id)
    }
    
    ElMessage.success('删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleMenuRestore = async () => {
  const row = contextMenuRow.value
  closeContextMenu()
  
  try {
    await restoreRecoveryFile(row.id)
    ElMessage.success('还原成功')
    emit('refresh')
  } catch {
    ElMessage.error('还原失败')
  }
}

// 确认重命名
const confirmRename = async () => {
  try {
    await renameFormRef.value.validate()
    renameLoading.value = true
    
    await renameFile({
      id: contextMenuRow.value.id,
      fileName: renameForm.value.fileName
    })
    
    ElMessage.success('重命名成功')
    renameVisible.value = false
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重命名失败')
    }
  } finally {
    renameLoading.value = false
  }
}

// 确认移动
const confirmMove = async (targetFolderId) => {
  if (!moveFileData.value) {
    ElMessage.error('请选择要移动的文件')
    return
  }
  try {
    await moveFile({
      id: moveFileData.value.id,
      targetFolderId
    })
    ElMessage.success('移动成功')
    emit('refresh')
  } catch (error) {
    console.error('移动失败', error)
    ElMessage.error('移动失败')
  } finally {
    moveFileData.value = null
  }
}

// 复制文件
const copyDialogVisible = ref(false)
const copyFileData = ref(null)
const handleMenuCopy = () => {
  copyFileData.value = { ...contextMenuRow.value }
  copyDialogVisible.value = true
  closeContextMenu()
}

// 解压文件
const unzipDialogVisible = ref(false)
const unzipFileData = ref(null)
const zipExtensions = ['zip', 'rar', '7z', 'tar', 'gz', 'bz2']

const canUnzip = (row) => {
  if (!row || row.isDir === 1) return false
  const ext = (row.extendName || '').toLowerCase()
  return zipExtensions.includes(ext)
}

const handleMenuUnzip = () => {
  unzipFileData.value = { ...contextMenuRow.value }
  unzipDialogVisible.value = true
  closeContextMenu()
}

// 文件详情
const detailDialogVisible = ref(false)
const detailFileData = ref(null)
const handleMenuDetail = () => {
  detailFileData.value = { ...contextMenuRow.value }
  detailDialogVisible.value = true
  closeContextMenu()
}

// 代码预览菜单
const handleMenuCodePreview = () => {
  openCodePreview(contextMenuRow.value)
  closeContextMenu()
}

// 代码预览
const codePreviewVisible = ref(false)
const codePreviewFile = ref(null)
const codeExtensions = ['js', 'ts', 'vue', 'jsx', 'tsx', 'json', 'html', 'css', 'scss', 'less', 
  'java', 'py', 'go', 'c', 'cpp', 'h', 'hpp', 'sql', 'sh', 'bash', 'xml', 'yml', 'yaml', 'md', 
  'txt', 'ini', 'conf', 'properties', 'php', 'rb', 'rs', 'swift', 'kt']

const isCodeFile = (row) => {
  if (!row || row.isDir === 1) return false
  const ext = (row.extendName || '').toLowerCase()
  return codeExtensions.includes(ext)
}

const openCodePreview = (file) => {
  codePreviewFile.value = file
  codePreviewVisible.value = true
}

// 图片预览增强
const imagePreviewVisible = ref(false)
const imagePreviewIndex = ref(0)
const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']

const imageListForPreview = computed(() => {
  return props.fileList.filter(item => {
    if (item.isDir === 1) return false
    const ext = (item.extendName || '').toLowerCase()
    return imageExtensions.includes(ext)
  }).map(item => ({
    ...item,
    fileUrl: getPreviewUrl(item.id)
  }))
})

// 打开增强版图片预览
const openImagePreview = (file) => {
  const index = imageListForPreview.value.findIndex(img => img.id === file.id)
  if (index >= 0) {
    imagePreviewIndex.value = index
    imagePreviewVisible.value = true
  }
}

// 刷新
const handleRefresh = () => {
  emit('refresh')
}

// 窗口大小变化
const handleResize = () => {
  screenWidth.value = window.innerWidth
}

// 点击其他地方关闭菜单
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
  window.removeEventListener('resize', handleResize)
})

// 暴露方法
defineExpose({
  clearSelection: () => tableRef.value?.clearSelection()
})
</script>

<style lang="scss" scoped>
.file-table-wrapper {
  flex: 1;
  overflow: hidden;
  
  .file-table {
    width: 100%;
    
    // 图标列样式 - 清除多余内容
    :deep(.file-icon-column) {
      .cell {
        padding: 0 !important;
        display: flex;
        justify-content: center;
        align-items: center;
        
        // 隐藏所有文本节点
        font-size: 0;
        line-height: 0;
        
        img {
          font-size: 14px; // 恢复 img 的 alt 文字大小
        }
      }
    }
    
    :deep(.el-table__header-wrapper th) {
      padding: 8px 0;
      color: #606266;
      background: #f5f7fa;
      font-weight: 500;
    }
    
    :deep(.el-table__body-wrapper) {
      overflow-y: auto;
      
      &::-webkit-scrollbar {
        width: 6px;
      }
      &::-webkit-scrollbar-thumb {
        background: #c0c4cc;
        border-radius: 3px;
      }
      
      td {
        padding: 8px 0;
      }
    }
    
    :deep(.el-table__row) {
      cursor: pointer;
      
      &:hover {
        td {
          background-color: #f5f7fa !important;
        }
      }
    }
    
    :deep(.current-row) {
      td {
        background-color: #ecf5ff !important;
      }
    }
  }
  
  .file-icon {
    width: 36px;
    height: 36px;
    cursor: pointer;
    vertical-align: middle;
    transition: transform 0.2s;
    
    &:hover {
      transform: scale(1.1);
    }
  }
  
  .file-thumbnail {
    width: 40px;
    height: 40px;
    cursor: pointer;
    vertical-align: middle;
    object-fit: cover;
    border-radius: 6px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    transition: all 0.2s;
    
    &:hover {
      transform: scale(1.15);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    }
  }
  
  .file-name {
    cursor: pointer;
    color: #303133;
    
    &:hover {
      color: var(--el-color-primary);
    }
  }
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    justify-content: center;
    
    .user-avatar {
      flex-shrink: 0;
    }
    
    .user-name {
      font-size: 13px;
      color: #606266;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 100px;
    }
  }
  
  .file-operate {
    font-size: 18px;
    cursor: pointer;
    color: #909399;
    padding: 4px;
    border-radius: 4px;
    
    &:hover {
      color: var(--el-color-primary);
      background: #ecf5ff;
    }
  }
  
  // 移动端样式
  &.mobile {
    .file-table {
      :deep(.el-table__header-wrapper th),
      :deep(.el-table__body-wrapper td) {
        padding: 4px 0;
      }
      
      .file-icon {
        width: 28px;
        height: 28px;
      }
    }
  }
}

// 右键菜单
.right-menu-list {
  position: fixed;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.12);
  z-index: 9999;
  padding: 8px 0;
  min-width: 140px;
  
  .right-menu-item {
    padding: 0 20px;
    height: 40px;
    line-height: 40px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 10px;
    color: #606266;
    font-size: 14px;
    transition: all 0.15s;
    
    .el-icon {
      font-size: 18px;
    }
    
    &:hover {
      background: #ecf5ff;
      color: var(--el-color-primary);
    }
    
    &:active {
      background: #d9ecff;
    }
    
    &.danger {
      color: var(--el-color-danger);
      
      &:hover {
        background: #fef0f0;
      }
      
      &:active {
        background: #fde2e2;
      }
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .file-table-wrapper {
    .file-table {
      :deep(.el-table__header-wrapper th),
      :deep(.el-table__body-wrapper td) {
        padding: 6px 0;
      }
    }
    
    .file-icon {
      width: 32px;
      height: 32px;
    }
    
    .file-thumbnail {
      width: 36px;
      height: 36px;
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .file-table-wrapper {
    .file-table {
      :deep(.el-table__header-wrapper th),
      :deep(.el-table__body-wrapper td) {
        padding: 4px 0;
        font-size: 13px;
      }
      
      :deep(.el-table-column--selection) {
        width: 40px !important;
        
        .cell {
          padding: 0 8px;
        }
      }
      
      :deep(.file-icon-column) {
        width: 44px !important;
        
        .cell {
          padding: 0 6px;
        }
      }
    }
    
    .file-icon {
      width: 28px;
      height: 28px;
    }
    
    .file-thumbnail {
      width: 32px;
      height: 32px;
    }
    
    .file-name {
      font-size: 14px;
    }
  }
  
  // 移动端右键菜单
  .right-menu-list {
    min-width: 120px;
    
    .right-menu-item {
      padding: 0 14px;
      height: 44px;
      line-height: 44px;
      font-size: 15px;
      
      .el-icon {
        font-size: 20px;
      }
    }
  }
}
</style>
