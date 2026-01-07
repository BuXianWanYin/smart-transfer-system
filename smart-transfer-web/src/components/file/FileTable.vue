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
      <!-- 选择列 -->
      <el-table-column type="selection" width="55" v-if="fileType !== 6" />
      
      <!-- 图标列 -->
      <el-table-column label="" prop="isDir" width="56" align="center" class-name="file-icon-column">
        <template #default="{ row }">
          <img
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
      
      <!-- 类型列（桌面端显示） -->
      <el-table-column prop="extendName" label="类型" width="100" sortable show-overflow-tooltip v-if="!isMobile">
        <template #default="{ row }">
          <span>{{ getFileType(row) }}</span>
        </template>
      </el-table-column>
      
      <!-- 大小列 -->
      <el-table-column prop="fileSize" label="大小" width="100" sortable align="right">
        <template #default="{ row }">
          {{ row.isDir === 1 ? '-' : formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      
      <!-- 修改日期列（桌面端显示） -->
      <el-table-column
        prop="updateTime"
        label="修改日期"
        width="180"
        sortable
        align="center"
        v-if="fileType !== 6 && !isMobile"
      >
        <template #default="{ row }">
          {{ formatDate(row.updateTime || row.createTime) }}
        </template>
      </el-table-column>
      
      <!-- 删除日期列（回收站，桌面端显示） -->
      <el-table-column
        prop="deleteTime"
        label="删除日期"
        width="180"
        sortable
        align="center"
        v-if="fileType === 6 && !isMobile"
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
        <div class="right-menu-item" @click="handleMenuMove">
          <el-icon><FolderOpened /></el-icon>
          移动到
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { More, Download, View, Edit, FolderOpened, Delete, RefreshLeft } from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import { formatFileSize, formatDateTime } from '@/utils/format'
import { getFileIconByType, canPreviewFile } from '@/utils/fileType'
import { renameFile, moveFile, deleteFile } from '@/api/fileApi'
import { restoreRecoveryFile, deleteRecoveryFile } from '@/api/recoveryApi'

const props = defineProps({
  fileType: { type: Number, required: true },
  filePath: { type: String, required: true },
  fileList: { type: Array, required: true },
  loading: { type: Boolean, required: true }
})

const emit = defineEmits(['refresh', 'row-click', 'selection-change'])

const tableRef = ref(null)

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

// 格式化
const formatSize = (size) => formatFileSize(size)
const formatDate = (date) => formatDateTime(date)

// 获取文件图标
const getFileIcon = (row) => {
  if (row.isDir === 1) return '/icons/folder.svg'
  return getFileIconByType(row.extendName)
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
  try {
    await moveFile({
      id: contextMenuRow.value.id,
      targetFolderId
    })
    ElMessage.success('移动成功')
    emit('refresh')
  } catch {
    ElMessage.error('移动失败')
  }
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
    width: 32px;
    height: 32px;
    cursor: pointer;
    vertical-align: middle;
  }
  
  .file-name {
    cursor: pointer;
    color: #303133;
    
    &:hover {
      color: var(--el-color-primary);
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
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 9999;
  padding: 6px 0;
  min-width: 120px;
  
  .right-menu-item {
    padding: 0 16px;
    height: 34px;
    line-height: 34px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    color: #606266;
    font-size: 14px;
    transition: all 0.2s;
    
    .el-icon {
      font-size: 16px;
    }
    
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
</style>
