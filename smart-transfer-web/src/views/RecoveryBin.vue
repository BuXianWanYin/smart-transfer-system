<template>
  <div class="recovery-bin">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <div class="left-actions">
        <template v-if="selectedFiles.length > 0">
          <el-button type="primary" @click="handleBatchRestore">
            <el-icon><RefreshLeft /></el-icon>
            还原
          </el-button>
          <el-button type="danger" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>
            彻底删除
          </el-button>
        </template>
        <el-button type="danger" @click="handleClearAll" v-if="fileList.length > 0">
          <el-icon><DeleteFilled /></el-icon>
          清空回收站
        </el-button>
      </div>
      
      <div class="right-actions">
        <span class="tip-text">回收站中的文件将在30天后自动清除</span>
      </div>
    </div>
    
    <!-- 文件列表 -->
    <div class="file-content" v-loading="loading">
      <el-table
        :data="fileList"
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="fileName" label="文件名" min-width="300">
          <template #default="{ row }">
            <div class="file-name-cell">
              <img :src="getFileIcon(row)" class="file-icon" />
              <span class="file-name">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="大小" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.isDir !== 1">{{ formatSize(row.fileSize) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="deleteTime" label="删除时间" width="180" align="center">
          <template #default="{ row }">
            <span>{{ formatDate(row.deleteTime) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" @click="handleRestore(row)">
                <el-icon><RefreshLeft /></el-icon>
                还原
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && fileList.length === 0" description="回收站为空" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Delete, DeleteFilled } from '@element-plus/icons-vue'
import { formatFileSize, formatDateTime } from '@/utils/format'
import { getFileIconByType } from '@/utils/fileType'
import { 
  getRecoveryFileList, 
  restoreRecoveryFile, 
  deleteRecoveryFile, 
  clearAllRecoveryFiles 
} from '@/api/recoveryApi'

const loading = ref(false)
const fileList = ref([])
const selectedFiles = ref([])

// 格式化文件大小
const formatSize = (size) => {
  return formatFileSize(size)
}

// 格式化日期
const formatDate = (date) => {
  return formatDateTime(date)
}

// 获取文件图标
const getFileIcon = (row) => {
  if (row.isDir === 1) {
    return '/icons/folder.svg'
  }
  return getFileIconByType(row.extendName)
}

// 加载回收站文件列表
const loadFileList = async () => {
  try {
    loading.value = true
    // HTTP拦截器已经处理了响应格式，直接使用返回的数据
    const res = await getRecoveryFileList()
    fileList.value = res || []
  } catch {
    ElMessage.error('加载回收站失败')
  } finally {
    loading.value = false
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

// 还原文件
const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要还原 "${row.fileName}" 吗？`,
      '确认还原',
      { type: 'info' }
    )
    
    loading.value = true
    await restoreRecoveryFile(row.id)
    
    ElMessage.success('还原成功')
    loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('还原失败')
    }
  } finally {
    loading.value = false
  }
}

// 批量还原
const handleBatchRestore = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要还原选中的 ${selectedFiles.value.length} 个文件吗？`,
      '确认还原',
      { type: 'info' }
    )
    
    loading.value = true
    
    for (const file of selectedFiles.value) {
      await restoreRecoveryFile(file.id)
    }
    
    ElMessage.success('批量还原成功')
    selectedFiles.value = []
    loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量还原失败')
    }
  } finally {
    loading.value = false
  }
}

// 彻底删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除 "${row.fileName}" 吗？删除后将无法恢复！`,
      '确认删除',
      { type: 'warning' }
    )
    
    loading.value = true
    await deleteRecoveryFile(row.id)
    
    ElMessage.success('删除成功')
    loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除选中的 ${selectedFiles.value.length} 个文件吗？删除后将无法恢复！`,
      '确认删除',
      { type: 'warning' }
    )
    
    loading.value = true
    
    for (const file of selectedFiles.value) {
      await deleteRecoveryFile(file.id)
    }
    
    ElMessage.success('批量删除成功')
    selectedFiles.value = []
    loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 清空回收站
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空回收站吗？此操作将永久删除所有文件，无法恢复！',
      '确认清空',
      { type: 'warning' }
    )
    
    loading.value = true
    await clearAllRecoveryFiles()
    
    ElMessage.success('回收站已清空')
    loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  loadFileList()
})
</script>

<style scoped>
.recovery-bin {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  flex-wrap: wrap;
  gap: 12px;
}

.left-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tip-text {
  color: #909399;
  font-size: 14px;
}

.file-content {
  flex: 1;
  padding: 16px;
  overflow: auto;
}

.file-name-cell {
  display: flex;
  align-items: center;
}

.file-icon {
  width: 28px;
  height: 28px;
  margin-right: 10px;
  flex-shrink: 0;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-btns {
  display: flex;
  gap: 8px;
}

/* 平板适配 */
@media (max-width: 1024px) {
  .operation-bar {
    padding: 12px 16px;
  }
  
  .left-actions :deep(.el-button) {
    padding: 8px 12px;
  }
  
  .tip-text {
    font-size: 12px;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .operation-bar {
    flex-direction: column;
    align-items: flex-start;
    padding: 12px;
    gap: 10px;
  }
  
  .left-actions {
    width: 100%;
    justify-content: flex-start;
    gap: 8px;
  }
  
  .left-actions :deep(.el-button) {
    padding: 8px 10px;
    font-size: 13px;
  }
  
  .left-actions :deep(.el-button span) {
    display: none;
  }
  
  .left-actions :deep(.el-button .el-icon) {
    margin-right: 0;
  }
  
  .right-actions {
    width: 100%;
  }
  
  .tip-text {
    font-size: 11px;
  }
  
  .file-content {
    padding: 12px;
  }
  
  .file-icon {
    width: 24px;
    height: 24px;
    margin-right: 8px;
  }
  
  /* 隐藏移动端不必要的列 */
  :deep(.el-table) {
    .el-table__header-wrapper,
    .el-table__body-wrapper {
      th:nth-child(3),
      td:nth-child(3),
      th:nth-child(4),
      td:nth-child(4) {
        display: none;
      }
    }
  }
  
  .action-btns {
    flex-direction: column;
    gap: 4px;
  }
  
  .action-btns :deep(.el-button) {
    padding: 4px 8px;
    font-size: 12px;
  }
}
</style>

