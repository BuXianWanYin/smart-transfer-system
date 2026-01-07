<template>
  <div class="file-grid-wrapper" v-loading="loading" element-loading-text="文件加载中……">
    <!-- 空状态 -->
    <el-empty v-if="fileList.length === 0" description="暂无文件" />
    
    <!-- 文件列表头部（全选） -->
    <div class="grid-header" v-if="fileList.length > 0">
      <el-checkbox
        v-model="isSelectAll"
        :indeterminate="isIndeterminate"
        @change="handleSelectAllChange"
      >
        全选
      </el-checkbox>
      <span class="file-count">共 {{ fileList.length }} 项</span>
    </div>
    
    <!-- 网格列表 -->
    <div class="grid-container" :style="gridStyle" v-if="fileList.length > 0">
      <div
        class="grid-item"
        :class="{ selected: selectedIds.includes(item.id) }"
        v-for="item in fileList"
        :key="item.id"
        @click="handleItemClick(item)"
        @dblclick="handleItemDblClick(item)"
        @contextmenu.prevent="handleContextMenu(item, $event)"
      >
        <!-- 选择框 -->
        <div class="checkbox-wrapper" @click.stop>
          <el-checkbox v-model="item._checked" @change="handleCheckChange(item)" />
        </div>
        
        <!-- 图标/缩略图 -->
        <div class="icon-wrapper" :style="iconStyle">
          <!-- 图片文件显示缩略图 -->
          <img
            v-if="isImageFile(item)"
            :src="getThumbnailUrl(item)"
            class="file-thumbnail"
            @error="handleThumbnailError($event, item)"
          />
          <!-- 其他文件显示图标 -->
          <img v-else :src="getFileIcon(item)" class="file-icon" />
        </div>
        
        <!-- 文件名 -->
        <div class="file-name" :title="getFileName(item)">
          {{ getFileName(item) }}
        </div>
      </div>
    </div>
    
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
        <div class="right-menu-item" @click="handleMenuCodePreview" v-if="isCodeFile(contextMenuRow)">
          <el-icon><Document /></el-icon>
          代码预览
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
      
      <!-- 回收站菜单 -->
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
    <el-dialog v-model="renameVisible" title="重命名" width="400px">
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
    <CopyFileDialog v-model="copyDialogVisible" :files="[contextMenuRow]" @success="handleRefresh" />
    
    <!-- 解压文件对话框 -->
    <UnzipDialog v-model="unzipDialogVisible" :file="contextMenuRow" @success="handleRefresh" />
    
    <!-- 文件详情弹窗 -->
    <FileDetailDialog
      v-model="detailDialogVisible"
      :file="contextMenuRow"
      :file-type="fileType"
      @preview="handleItemDblClick"
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
import { Download, View, Edit, FolderOpened, Delete, RefreshLeft, CopyDocument, Files, InfoFilled, Document } from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import CopyFileDialog from './CopyFileDialog.vue'
import UnzipDialog from './UnzipDialog.vue'
import FileDetailDialog from './FileDetailDialog.vue'
import CodePreview from './CodePreview.vue'
import { getFileIconByType, canPreviewFile } from '@/utils/fileType'
import { renameFile, moveFile, deleteFile, getPreviewUrl } from '@/api/fileApi'
import { restoreRecoveryFile, deleteRecoveryFile } from '@/api/recoveryApi'

const props = defineProps({
  fileType: { type: Number, required: true },
  filePath: { type: String, required: true },
  fileList: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  gridSize: { type: Number, default: 120 }
})

const emit = defineEmits(['refresh', 'row-click', 'selection-change'])

// 选中的项
const selectedIds = ref([])
const selectedItems = computed(() => props.fileList.filter(item => selectedIds.value.includes(item.id)))

// 全选状态
const isSelectAll = computed(() => {
  return props.fileList.length > 0 && selectedIds.value.length === props.fileList.length
})

const isIndeterminate = computed(() => {
  return selectedIds.value.length > 0 && selectedIds.value.length < props.fileList.length
})

// 全选/取消全选
const handleSelectAllChange = (val) => {
  if (val) {
    selectAll()
  } else {
    clearSelection()
  }
}

// 网格样式 - 根据 gridSize 计算
const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(auto-fill, minmax(${props.gridSize}px, 1fr))`
}))

// 图标样式
const iconStyle = computed(() => ({
  width: `${props.gridSize * 0.6}px`,
  height: `${props.gridSize * 0.6}px`
}))

// 右键菜单
const contextMenuVisible = ref(false)
const contextMenuPos = ref({ x: 0, y: 0 })
const contextMenuRow = ref(null)

// 计算菜单位置，防止溢出屏幕
const menuStyle = computed(() => {
  const menuWidth = 140
  const menuHeight = 300
  let x = contextMenuPos.value.x
  let y = contextMenuPos.value.y
  
  if (x + menuWidth > window.innerWidth) {
    x = window.innerWidth - menuWidth - 10
  }
  if (y + menuHeight > window.innerHeight) {
    y = window.innerHeight - menuHeight - 10
  }
  
  return { left: x + 'px', top: y + 'px' }
})

// 复制对话框
const copyDialogVisible = ref(false)

// 解压对话框
const unzipDialogVisible = ref(false)
const zipExtensions = ['zip', 'rar', '7z', 'tar', 'gz', 'bz2']

// 文件详情
const detailDialogVisible = ref(false)

// 代码预览
const codePreviewVisible = ref(false)
const codePreviewFile = ref(null)
const codeExtensions = ['js', 'ts', 'vue', 'jsx', 'tsx', 'json', 'html', 'css', 'scss', 'less', 
  'java', 'py', 'go', 'c', 'cpp', 'h', 'hpp', 'sql', 'sh', 'bash', 'xml', 'yml', 'yaml', 'md', 
  'txt', 'ini', 'conf', 'properties', 'php', 'rb', 'rs', 'swift', 'kt']

// 判断是否可解压
const canUnzip = (row) => {
  if (!row || row.isDir === 1) return false
  const ext = (row.extendName || '').toLowerCase()
  return zipExtensions.includes(ext)
}

// 判断是否是代码文件
const isCodeFile = (row) => {
  if (!row || row.isDir === 1) return false
  const ext = (row.extendName || '').toLowerCase()
  return codeExtensions.includes(ext)
}

// 重命名
const renameVisible = ref(false)
const renameFormRef = ref(null)
const renameForm = ref({ fileName: '' })
const renameRules = { fileName: [{ required: true, message: '请输入名称', trigger: 'blur' }] }
const renameLoading = ref(false)

// 移动
const moveDialogVisible = ref(false)

// 图片扩展名
const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']

// 判断是否是图片文件
const isImageFile = (item) => {
  if (item.isDir === 1) return false
  const ext = (item.extendName || '').toLowerCase()
  return imageExtensions.includes(ext)
}

// 获取缩略图 URL
const getThumbnailUrl = (item) => {
  return getPreviewUrl(item.id)
}

// 缩略图加载失败时显示图标
const handleThumbnailError = (event, item) => {
  event.target.src = getFileIconByType(item.extendName)
  event.target.classList.remove('file-thumbnail')
  event.target.classList.add('file-icon')
}

// 获取文件图标
const getFileIcon = (item) => {
  if (item.isDir === 1) return '/icons/folder.svg'
  return getFileIconByType(item.extendName)
}

// 获取文件名
const getFileName = (item) => {
  return item.fileName || item.folderName || ''
}

// 是否可预览
const canPreview = (row) => {
  if (!row || row.isDir === 1) return false
  return canPreviewFile(row.extendName)
}

// 单击选中
const handleItemClick = (item) => {
  if (selectedIds.value.includes(item.id)) {
    selectedIds.value = selectedIds.value.filter(id => id !== item.id)
    item._checked = false
  } else {
    selectedIds.value.push(item.id)
    item._checked = true
  }
  emit('selection-change', selectedItems.value)
}

// 双击打开
const handleItemDblClick = (item) => {
  emit('row-click', item)
}

// 选择框变化
const handleCheckChange = (item) => {
  if (item._checked) {
    if (!selectedIds.value.includes(item.id)) {
      selectedIds.value.push(item.id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(id => id !== item.id)
  }
  emit('selection-change', selectedItems.value)
}

// 右键菜单
const handleContextMenu = (item, event) => {
  contextMenuRow.value = item
  contextMenuPos.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

// 关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false
  contextMenuRow.value = null
}

// 右键菜单操作
const handleMenuDownload = () => {
  if (contextMenuRow.value) {
    window.open(`/api/file/download/${contextMenuRow.value.id}`)
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
      await deleteRecoveryFile(row.id)
    } else {
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
  } catch (error) {
    ElMessage.error('还原失败')
  }
}

// 复制文件
const handleMenuCopy = () => {
  copyDialogVisible.value = true
  closeContextMenu()
}

// 解压文件
const handleMenuUnzip = () => {
  unzipDialogVisible.value = true
  closeContextMenu()
}

// 文件详情
const handleMenuDetail = () => {
  detailDialogVisible.value = true
  closeContextMenu()
}

// 代码预览
const handleMenuCodePreview = () => {
  codePreviewFile.value = contextMenuRow.value
  codePreviewVisible.value = true
  closeContextMenu()
}

// 刷新
const handleRefresh = () => {
  emit('refresh')
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
  try {
    await moveFile({
      id: contextMenuRow.value.id,
      targetFolderId
    })
    ElMessage.success('移动成功')
    emit('refresh')
  } catch (error) {
    ElMessage.error('移动失败')
  }
}

// 全选
const selectAll = () => {
  props.fileList.forEach(item => {
    item._checked = true
    if (!selectedIds.value.includes(item.id)) {
      selectedIds.value.push(item.id)
    }
  })
  emit('selection-change', selectedItems.value)
}

// 取消全选
const clearSelection = () => {
  props.fileList.forEach(item => {
    item._checked = false
  })
  selectedIds.value = []
  emit('selection-change', [])
}

// 点击其他地方关闭菜单
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})

// 暴露方法给父组件
defineExpose({
  selectAll,
  clearSelection
})
</script>

<style lang="scss" scoped>
.file-grid-wrapper {
  flex: 1;
  overflow: auto;
  padding: 8px;
  
  .grid-header {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 8px;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 8px;
    
    .file-count {
      font-size: 13px;
      color: #909399;
    }
  }
  
  .grid-container {
    display: grid;
    gap: 16px;
    
    .grid-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px 8px;
      border-radius: 8px;
      cursor: pointer;
      position: relative;
      transition: all 0.2s;
      
      &:hover {
        background: #f5f7fa;
      }
      
      &.selected {
        background: #ecf5ff;
        border: 1px solid var(--el-color-primary);
      }
      
      .checkbox-wrapper {
        position: absolute;
        top: 4px;
        left: 4px;
        opacity: 1;
        transition: opacity 0.2s;
      }
      
      .icon-wrapper {
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        overflow: hidden;
        border-radius: 8px;
        
        .file-icon {
          max-width: 100%;
          max-height: 100%;
        }
        
        .file-thumbnail {
          width: 100%;
          height: 100%;
          object-fit: cover;
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
      }
      
      .file-name {
        margin-top: 8px;
        font-size: 12px;
        color: #303133;
        text-align: center;
        width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
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
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 9999;
  padding: 4px 0;
  
  .right-menu-item {
    padding: 0 16px;
    height: 36px;
    line-height: 36px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    color: #606266;
    
    &:hover {
      background: #ecf5ff;
      color: var(--el-color-primary);
    }
    
    &.danger {
      color: var(--el-color-danger);
      
      &:hover {
        background: #fef0f0;
      }
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .file-grid-wrapper {
    padding: 6px;
    
    .grid-container {
      gap: 12px;
      
      .grid-item {
        padding: 10px 6px;
        
        .file-name {
          font-size: 11px;
        }
      }
    }
  }
}

/* 移动端适配 - 两列显示 */
@media (max-width: 768px) {
  .file-grid-wrapper {
    padding: 4px;
    
    .grid-container {
      gap: 8px;
      grid-template-columns: repeat(2, 1fr) !important;
      
      .grid-item {
        padding: 8px 4px;
        
        .checkbox-wrapper {
          opacity: 1;
        }
        
        .file-name {
          font-size: 12px;
          margin-top: 6px;
        }
      }
    }
  }
  
  // 移动端右键菜单
  .right-menu-list {
    min-width: 120px;
    
    .right-menu-item {
      padding: 0 12px;
      height: 44px;
      line-height: 44px;
      font-size: 15px;
      
      .el-icon {
        font-size: 18px;
      }
    }
  }
}
</style>
