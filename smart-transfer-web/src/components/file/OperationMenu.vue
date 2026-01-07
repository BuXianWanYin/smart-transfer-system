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
      
      <!-- 显示模式切换 -->
      <el-radio-group v-model="currentMode" @change="handleModeChange" class="mode-switch">
        <el-radio-button :value="0">
          <el-icon><List /></el-icon>
        </el-radio-button>
        <el-radio-button :value="1">
          <el-icon><Grid /></el-icon>
        </el-radio-button>
        <el-radio-button :value="2" v-if="fileType === 1">
          <el-icon><Clock /></el-icon>
        </el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- 移动文件对话框 -->
    <MoveFileDialog v-model="moveDialogVisible" @confirm="confirmBatchMove" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, FolderAdd, Download, FolderOpened, Delete, 
  RefreshLeft, Search, List, Grid, Clock 
} from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import { batchDeleteFiles, batchMoveFiles } from '@/api/fileApi'
import { batchRestoreRecoveryFiles, batchDeleteRecoveryFiles, clearAllRecoveryFiles } from '@/api/recoveryApi'

const props = defineProps({
  fileType: { type: Number, required: true },
  filePath: { type: String, required: true },
  operationFileList: { type: Array, default: () => [] },
  isBatchOperation: { type: Boolean, default: false }
})

const emit = defineEmits(['refresh', 'upload-file', 'new-folder', 'search-file', 'show-upload', 'change-mode'])

// 搜索关键词
const searchKeyword = ref('')

// 显示模式
const currentMode = ref(0)

// 移动对话框
const moveDialogVisible = ref(false)

// 搜索
const handleSearch = () => {
  emit('search-file', searchKeyword.value)
}

// 切换显示模式
const handleModeChange = (mode) => {
  emit('change-mode', mode)
}

// 批量下载
const handleBatchDownload = () => {
  props.operationFileList.forEach(file => {
    if (file.isDir !== 1) {
      window.open(`/api/file/download/${file.id}`)
    }
  })
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
      `确定要${action}选中的 ${props.operationFileList.length} 个文件吗？`,
      `确认${action}`,
      { type: 'warning' }
    )
    
    if (isRecovery) {
      const ids = props.operationFileList.map(f => f.id)
      await batchDeleteRecoveryFiles(ids)
    } else {
      const ids = props.operationFileList.map(f => f.id)
      await batchDeleteFiles(ids)
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
</script>

<style lang="scss" scoped>
.operation-menu-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
  
  .left-operations {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
  
  .right-operations {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .mode-switch {
      :deep(.el-radio-button__inner) {
        padding: 8px 12px;
      }
    }
  }
}
</style>
