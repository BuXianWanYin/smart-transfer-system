<template>
  <div class="operation-menu-wrapper">
    <!-- 左侧操作按钮 -->
    <div class="left-operations">
      <!-- 上传按钮 -->
      <el-button type="primary" @click="$emit('show-upload')" v-if="fileType !== 6">
        <el-icon><Upload /></el-icon>
        上传
      </el-button>
      
      <!-- 新建文件夹 -->
      <el-button @click="$emit('new-folder')" v-if="fileType !== 6">
        <el-icon><FolderAdd /></el-icon>
        新建文件夹
      </el-button>
      
      <!-- 批量操作 -->
      <template v-if="operationFileList.length > 0">
        <el-button @click="handleBatchDownload" v-if="fileType !== 6">
          <el-icon><Download /></el-icon>
          下载
        </el-button>
        
        <el-button @click="handleBatchCopy" v-if="fileType !== 6">
          <el-icon><CopyDocument /></el-icon>
          复制到
        </el-button>
        
        <el-button @click="handleBatchMove" v-if="fileType !== 6">
          <el-icon><FolderOpened /></el-icon>
          移动到
        </el-button>
        
        <el-button type="danger" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          {{ fileType === 6 ? '彻底删除' : '删除' }}
        </el-button>
        
        <el-button @click="handleBatchRestore" v-if="fileType === 6">
          <el-icon><RefreshLeft /></el-icon>
          还原
        </el-button>
      </template>
      
      <!-- 回收站清空 -->
      <el-button type="danger" @click="handleClearRecovery" v-if="fileType === 6">
        <el-icon><Delete /></el-icon>
        清空回收站
      </el-button>
    </div>
    
    <!-- 右侧操作 -->
    <div class="right-operations">
      <!-- 搜索框 -->
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文件"
        style="width: 200px"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      
      <!-- 刷新按钮 -->
      <el-tooltip content="刷新" placement="bottom">
        <el-icon class="action-icon" @click="$emit('refresh')"><Refresh /></el-icon>
      </el-tooltip>
      
      <!-- 分隔线 -->
      <el-divider direction="vertical" />
      
      <!-- 显示模式切换 -->
      <el-radio-group v-model="currentMode" @change="handleModeChange" class="mode-switch">
        <el-radio-button :value="0" title="列表模式">
          <el-icon><List /></el-icon>
        </el-radio-button>
        <el-radio-button :value="1" title="网格模式">
          <el-icon><Grid /></el-icon>
        </el-radio-button>
        <el-radio-button :value="2" v-if="fileType === 1" title="时间线模式">
          <el-icon><Clock /></el-icon>
        </el-radio-button>
      </el-radio-group>
      
      <!-- 设置按钮（图标大小调节等） -->
      <el-popover
        v-model:visible="settingPopoverVisible"
        placement="bottom-end"
        :width="240"
        trigger="click"
      >
        <template #reference>
          <el-icon class="action-icon" title="设置"><Setting /></el-icon>
        </template>
        
        <div class="setting-content">
          <!-- 图标大小调节（仅在网格模式或时间线模式下显示） -->
          <template v-if="currentMode === 1 || currentMode === 2">
            <div class="setting-item">
              <span class="setting-label">图标大小</span>
              <el-slider
                v-model="gridSize"
                :min="60"
                :max="200"
                :step="10"
                :format-tooltip="val => `${val}px`"
                @change="handleGridSizeChange"
              />
            </div>
          </template>
          
          <!-- 列选择（仅列表模式） -->
          <template v-if="currentMode === 0">
            <el-divider style="margin: 12px 0" />
            <SelectColumn
              v-model="selectedColumns"
              :file-type="fileType"
              @change="handleColumnChange"
            />
          </template>
        </div>
      </el-popover>
    </div>
    
    <!-- 移动文件对话框 -->
    <MoveFileDialog v-model="moveDialogVisible" @confirm="confirmBatchMove" />
    
    <!-- 复制文件对话框 -->
    <CopyFileDialog v-model="copyDialogVisible" :files="operationFileList" @success="handleCopySuccess" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, FolderAdd, Download, FolderOpened, Delete, 
  RefreshLeft, Search, List, Grid, Clock, Setting, Refresh, CopyDocument
} from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import CopyFileDialog from './CopyFileDialog.vue'
import SelectColumn from './SelectColumn.vue'
import { batchDeleteFiles, batchMoveFiles, getBatchDownloadUrl } from '@/api/fileApi'
import { deleteFolder } from '@/api/folderApi'
import { batchRestoreRecoveryFiles, batchDeleteRecoveryFiles, clearAllRecoveryFiles } from '@/api/recoveryApi'
import { useTransferStore } from '@/store/transferStore'

const router = useRouter()
const transferStore = useTransferStore()

const props = defineProps({
  fileType: { type: Number, required: true },
  filePath: { type: String, required: true },
  operationFileList: { type: Array, default: () => [] },
  isBatchOperation: { type: Boolean, default: false },
  fileList: { type: Array, default: () => [] }
})

const emit = defineEmits(['refresh', 'upload-file', 'new-folder', 'search-file', 'show-upload', 'change-mode', 'change-grid-size', 'change-columns'])

// 搜索关键词
const searchKeyword = ref('')

// 显示模式
const currentMode = ref(0)

// 移动对话框
const moveDialogVisible = ref(false)

// 复制对话框
const copyDialogVisible = ref(false)

// 设置弹出框
const settingPopoverVisible = ref(false)

// 图标大小
const gridSize = ref(parseInt(localStorage.getItem('file_grid_size') || '120'))

// 列选择
const selectedColumns = ref(['extendName', 'fileSize', 'updateTime'])

// 列变更
const handleColumnChange = (columns) => {
  emit('change-columns', columns)
}

// 搜索
const handleSearch = () => {
  emit('search-file', searchKeyword.value)
}

// 切换显示模式
const handleModeChange = (mode) => {
  emit('change-mode', mode)
}

// 图标大小变化
const handleGridSizeChange = (size) => {
  localStorage.setItem('file_grid_size', size.toString())
  emit('change-grid-size', size)
}

// 批量下载（打包为ZIP）
const handleBatchDownload = () => {
  // 过滤出文件（不包括文件夹）
  const files = props.operationFileList.filter(file => file.isDir !== 1)
  
  if (files.length === 0) {
    ElMessage.warning('没有可下载的文件')
    return
  }
  
  if (files.length === 1) {
    // 单个文件添加到传输列表
    const file = files[0]
    transferStore.addDownloadTask({
      fileId: file.id,
      fileName: file.fileName,
      fileSize: file.fileSize,
      fileHash: file.fileHash
    })
    ElMessage.success(`已添加 "${file.fileName}" 到下载列表`)
    router.push({ name: 'TransferCenter' })
  } else {
    // 多个文件打包下载（服务端打包，直接下载）
    const fileIds = files.map(file => file.id)
    const url = getBatchDownloadUrl(fileIds)
    window.open(url)
    ElMessage.success(`正在打包下载 ${fileIds.length} 个文件...`)
  }
}

// 批量复制
const handleBatchCopy = () => {
  copyDialogVisible.value = true
}

const handleCopySuccess = () => {
  emit('refresh')
}

// 批量移动
const handleBatchMove = () => {
  moveDialogVisible.value = true
}

const confirmBatchMove = async (targetFolderId) => {
  try {
    const fileIds = props.operationFileList.map(f => f.id)
    await batchMoveFiles({ fileIds, targetFolderId })
    ElMessage.success('批量移动成功')
    emit('refresh')
  } catch (error) {
    ElMessage.error('批量移动失败')
  }
}

// 批量删除
const handleBatchDelete = async () => {
  const isRecovery = props.fileType === 6
  const action = isRecovery ? '彻底删除' : '删除'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}选中的 ${props.operationFileList.length} 个项目吗？`,
      `确认${action}`,
      { type: 'warning' }
    )
    
    if (isRecovery) {
      // 回收站批量删除
      const ids = props.operationFileList.map(f => f.id)
      await batchDeleteRecoveryFiles(ids)
    } else {
      // 分离文件和文件夹
      const files = props.operationFileList.filter(f => f.isDir !== 1)
      const folders = props.operationFileList.filter(f => f.isDir === 1)
      
      // 批量删除文件
      if (files.length > 0) {
        const fileIds = files.map(f => f.id)
        await batchDeleteFiles(fileIds)
      }
      
      // 逐个删除文件夹（没有批量删除文件夹的API）
      for (const folder of folders) {
        await deleteFolder(folder.id)
      }
    }
    
    ElMessage.success(`${action}成功`)
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// 批量还原
const handleBatchRestore = async () => {
  try {
    const ids = props.operationFileList.map(f => f.id)
    await batchRestoreRecoveryFiles(ids)
    ElMessage.success('批量还原成功')
    emit('refresh')
  } catch (error) {
    ElMessage.error('批量还原失败')
  }
}

// 清空回收站
const handleClearRecovery = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空回收站吗？此操作不可恢复！',
      '确认清空',
      { type: 'warning' }
    )
    
    await clearAllRecoveryFiles()
    ElMessage.success('回收站已清空')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

// 初始化时发送图标大小
watch(currentMode, (mode) => {
  if (mode === 1 || mode === 2) {
    emit('change-grid-size', gridSize.value)
  }
}, { immediate: true })
</script>

<style lang="scss" scoped>
.operation-menu-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
  flex-wrap: wrap;
  gap: 12px;
  
  .left-operations {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    
    :deep(.el-button) {
      padding: 10px 16px;
      font-size: 14px;
      
      .el-icon {
        font-size: 16px;
        margin-right: 6px;
      }
    }
  }
  
  .right-operations {
    display: flex;
    align-items: center;
    gap: 14px;
    
    .action-icon {
      font-size: 24px;
      color: #606266;
      cursor: pointer;
      padding: 10px;
      border-radius: 6px;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 44px;
      min-height: 44px;
      
      &:hover {
        color: var(--el-color-primary);
        background: #ecf5ff;
        transform: scale(1.05);
      }
      
      &:active {
        transform: scale(0.95);
      }
    }
    
    .mode-switch {
      :deep(.el-radio-button__inner) {
        padding: 10px 16px;
        
        .el-icon {
          font-size: 18px;
        }
      }
    }
  }
}

.setting-content {
  .setting-item {
    margin-bottom: 16px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .setting-label {
      display: block;
      font-size: 14px;
      color: #606266;
      margin-bottom: 8px;
    }
    
    :deep(.el-slider) {
      margin-top: 8px;
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .operation-menu-wrapper {
    padding: 12px 0;
    
    .left-operations {
      :deep(.el-button) {
        padding: 8px 12px;
        font-size: 13px;
        
        // 隐藏部分按钮文字，只显示图标
        span {
          display: none;
        }
        
        .el-icon {
          margin-right: 0;
          font-size: 18px;
        }
      }
    }
    
    .right-operations {
      gap: 10px;
      
      :deep(.el-input) {
        width: 160px !important;
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .operation-menu-wrapper {
    flex-direction: column;
    align-items: stretch;
    padding: 10px 0;
    gap: 10px;
    
    .left-operations {
      justify-content: flex-start;
      gap: 8px;
      
      :deep(.el-button) {
        padding: 8px 10px;
        flex: 0 0 auto;
        min-width: 42px;
        
        span {
          display: none;
        }
        
        .el-icon {
          margin-right: 0;
          font-size: 18px;
        }
      }
    }
    
    .right-operations {
      justify-content: space-between;
      gap: 8px;
      
      :deep(.el-input) {
        flex: 1;
        width: auto !important;
        min-width: 0;
      }
      
      :deep(.el-divider--vertical) {
        display: none;
      }
      
      .action-icon {
        font-size: 20px;
        padding: 6px;
      }
      
      .mode-switch {
        :deep(.el-radio-button__inner) {
          padding: 8px 10px;
          
          .el-icon {
            font-size: 14px;
          }
        }
      }
    }
  }
}
</style>
