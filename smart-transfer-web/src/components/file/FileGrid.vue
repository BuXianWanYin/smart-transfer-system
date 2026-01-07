<template>
  <div class="file-grid-wrapper" v-loading="loading" element-loading-text="文件加载中……">
    <!-- 空状态 -->
    <el-empty v-if="fileList.length === 0" description="暂无文件" />
    
    <!-- 网格列表 -->
    <div class="grid-container" v-else>
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
        
        <!-- 图标 -->
        <div class="icon-wrapper">
          <img :src="getFileIcon(item)" class="file-icon" />
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
      :style="{ left: contextMenuPos.x + 'px', top: contextMenuPos.y + 'px' }"
    >
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
      <div class="right-menu-item" @click="handleMenuMove" v-if="fileType !== 6">
        <el-icon><FolderOpened /></el-icon>
        移动到
      </div>
      <div class="right-menu-item danger" @click="handleMenuDelete">
        <el-icon><Delete /></el-icon>
        {{ fileType === 6 ? '彻底删除' : '删除' }}
      </div>
      <div class="right-menu-item" @click="handleMenuRestore" v-if="fileType === 6">
        <el-icon><RefreshLeft /></el-icon>
        还原
      </div>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, View, Edit, FolderOpened, Delete, RefreshLeft } from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
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

// 选中的项
const selectedIds = ref([])
const selectedItems = computed(() => props.fileList.filter(item => selectedIds.value.includes(item.id)))

// 右键菜单
const contextMenuVisible = ref(false)
const contextMenuPos = ref({ x: 0, y: 0 })
const contextMenuRow = ref(null)

// 重命名
const renameVisible = ref(false)
const renameFormRef = ref(null)
const renameForm = ref({ fileName: '' })
const renameRules = { fileName: [{ required: true, message: '请输入名称', trigger: 'blur' }] }
const renameLoading = ref(false)

// 移动
const moveDialogVisible = ref(false)

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

// 点击其他地方关闭菜单
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})
</script>

<style lang="scss" scoped>
.file-grid-wrapper {
  flex: 1;
  overflow: auto;
  padding: 8px;
  
  .grid-container {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
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
        opacity: 0;
        transition: opacity 0.2s;
      }
      
      &:hover .checkbox-wrapper,
      &.selected .checkbox-wrapper {
        opacity: 1;
      }
      
      .icon-wrapper {
        width: 64px;
        height: 64px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .file-icon {
          max-width: 100%;
          max-height: 100%;
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
</style>
