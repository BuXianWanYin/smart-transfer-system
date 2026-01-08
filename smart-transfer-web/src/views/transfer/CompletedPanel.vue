<template>
  <div class="completed-panel">
    <!-- 筛选工具栏 -->
    <div class="filter-toolbar">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="传输类型">
          <el-select v-model="filterForm.type" placeholder="全部" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="上传" value="upload" />
            <el-option label="下载" value="download" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索文件名"
            clearable
            style="width: 200px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="toolbar-actions">
        <el-button @click="clearAll" type="danger" plain>
          <el-icon><Delete /></el-icon>
          清空全部记录
        </el-button>
      </div>
    </div>

    <!-- 历史记录列表 -->
    <div v-if="filteredList.length > 0" class="history-list">
      <div class="list-header">
        <el-checkbox v-model="selectAll" @change="handleSelectAll" />
        <span class="header-label">文件名</span>
        <span class="header-label">大小</span>
        <span class="header-label">类型</span>
        <span class="header-label">完成时间</span>
        <span class="header-label">操作</span>
      </div>
      
      <div class="list-body">
        <div
          v-for="item in paginatedList"
          :key="item.id"
          class="list-item"
          :class="{ selected: item.selected }"
        >
          <el-checkbox v-model="item.selected" />
          
          <div class="item-file">
            <el-icon class="file-icon" :class="getFileIconClass(item.fileName)">
              <Document />
            </el-icon>
            <div class="file-info">
              <div class="file-name" :title="item.fileName">{{ item.fileName }}</div>
              <div class="file-hash" v-if="item.fileHash">
                <el-tag size="small" type="info" effect="plain">
                  {{ item.fileHash.substring(0, 8) }}...
                </el-tag>
              </div>
            </div>
          </div>
          
          <div class="item-size">
            {{ formatFileSize(item.fileSize) }}
          </div>
          
          <div class="item-type">
            <el-tag :type="item.transferType === 'upload' ? 'primary' : 'success'" size="small">
              <el-icon>
                <component :is="item.transferType === 'upload' ? 'Upload' : 'Download'" />
              </el-icon>
              {{ item.transferType === 'upload' ? '上传' : '下载' }}
            </el-tag>
          </div>
          
          <div class="item-time">
            <div class="time-text">{{ formatDateTime(item.completedTime) }}</div>
            <div class="speed-text">平均: {{ formatSpeed(item.avgSpeed) }}</div>
          </div>
          
          <div class="item-actions">
            <el-button
              v-if="item.transferType === 'upload'"
              size="small"
              text
              type="primary"
              @click="handleDownload(item)"
            >
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button
              v-if="item.transferType === 'download'"
              size="small"
              text
              type="success"
              @click="handleOpen(item)"
            >
              <el-icon><FolderOpened /></el-icon>
              打开
            </el-button>
            <el-dropdown trigger="click">
              <el-button size="small" text>
                <el-icon><More /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="showDetails(item)">
                    <el-icon><View /></el-icon>
                    查看详情
                  </el-dropdown-item>
                  <el-dropdown-item @click="copyInfo(item)">
                    <el-icon><CopyDocument /></el-icon>
                    复制信息
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="deleteRecord(item)">
                    <el-icon><Delete /></el-icon>
                    删除记录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
      
      <!-- 批量操作 -->
      <div v-if="selectedCount > 0" class="batch-actions">
        <span class="selected-info">已选择 {{ selectedCount }} 项</span>
        <el-button size="small" @click="batchDownload" type="primary">
          批量下载
        </el-button>
        <el-button size="small" @click="batchDelete" type="danger">
          批量删除
        </el-button>
        <el-button size="small" @click="cancelSelection">
          取消选择
        </el-button>
      </div>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredList.length"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    
    <!-- 空状态 -->
    <el-empty
      v-else
      description="暂无传输历史记录"
      :image-size="120"
    >
      <template #image>
        <el-icon :size="80" color="#909399">
          <Clock />
        </el-icon>
      </template>
    </el-empty>
    
    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailsVisible"
      title="传输详情"
      width="600px"
    >
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="文件名">{{ currentItem.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(currentItem.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="传输类型">
          <el-tag :type="currentItem.transferType === 'upload' ? 'primary' : 'success'">
            {{ currentItem.transferType === 'upload' ? '上传' : '下载' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件哈希">{{ currentItem.fileHash || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(currentItem.completedTime) }}</el-descriptions-item>
        <el-descriptions-item label="传输时长">{{ currentItem.duration || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平均速度">{{ formatSpeed(currentItem.avgSpeed) }}</el-descriptions-item>
        <el-descriptions-item label="使用算法">{{ currentItem.algorithm || 'CUBIC' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailsVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, Refresh, Delete, Document, Upload, Download, 
  FolderOpened, More, View, CopyDocument, Clock
} from '@element-plus/icons-vue'
import { useFileStore } from '@/store/fileStore'
import { formatFileSize, formatSpeed } from '@/utils/file'
import { formatDateTime } from '@/utils/format'
import { getDownloadUrl } from '@/api/fileApi'
import { getHistoryList, deleteHistory, clearAllHistory } from '@/api/historyApi'

const fileStore = useFileStore()

// 筛选表单
const filterForm = ref({
  type: '',
  dateRange: null,
  keyword: ''
})

// 数据列表
const historyList = ref([])
const selectAll = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)

// 详情对话框
const detailsVisible = ref(false)
const currentItem = ref(null)

// 计算属性
const filteredList = computed(() => {
  let list = [...historyList.value]
  
  // 类型筛选
  if (filterForm.value.type) {
    list = list.filter(item => item.transferType === filterForm.value.type)
  }
  
  // 关键词筛选
  if (filterForm.value.keyword) {
    const keyword = filterForm.value.keyword.toLowerCase()
    list = list.filter(item => 
      item.fileName.toLowerCase().includes(keyword)
    )
  }
  
  // 日期筛选
  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    const [start, end] = filterForm.value.dateRange
    list = list.filter(item => {
      const time = new Date(item.completedTime).getTime()
      return time >= start.getTime() && time <= end.getTime()
    })
  }
  
  return list
})

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredList.value.slice(start, end)
})

const selectedCount = computed(() => {
  return historyList.value.filter(item => item.selected).length
})

// 生命周期
onMounted(() => {
  loadHistoryList()
})

// 加载历史记录
const loadHistoryList = async () => {
  try {
    // 从后端API获取传输历史记录
    const res = await getHistoryList({
      fileName: filterForm.value.keyword,
      transferType: filterForm.value.type
    })
    
    // 转换为前端需要的格式
    historyList.value = (res || []).map(record => ({
      id: record.id,
      fileName: record.fileName,
      fileSize: record.fileSize,
      fileHash: record.fileHash,
      transferType: record.transferType?.toLowerCase() || 'upload',
      completedTime: record.completedTime,
      avgSpeed: record.avgSpeed || 0,
      duration: formatDuration(record.duration || 0),
      algorithm: record.algorithm || 'CUBIC',
      selected: false
    }))
  } catch (error) {
    // 加载失败
    ElMessage.error('加载历史记录失败')
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
}

// 重置
const handleReset = () => {
  filterForm.value = {
    type: '',
    dateRange: null,
    keyword: ''
  }
  currentPage.value = 1
}

// 全选
const handleSelectAll = (value) => {
  paginatedList.value.forEach(item => {
    item.selected = value
  })
}

// 分页变化
const handleSizeChange = () => {
  currentPage.value = 1
}

const handleCurrentChange = () => {
  selectAll.value = false
}

// 下载
const handleDownload = (item) => {
  const url = getDownloadUrl(item.id)
  window.open(url, '_blank')
  ElMessage.success('开始下载')
}

// 打开文件
const handleOpen = (item) => {
  ElMessage.success(`正在打开 ${item.fileName}`)
}

// 显示详情
const showDetails = (item) => {
  currentItem.value = item
  detailsVisible.value = true
}

// 复制信息
const copyInfo = (item) => {
  const info = `文件名: ${item.fileName}\n大小: ${formatFileSize(item.fileSize)}\n哈希: ${item.fileHash}`
  navigator.clipboard.writeText(info)
  ElMessage.success('已复制到剪贴板')
}

// 删除记录
const deleteRecord = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除此记录吗？', '提示', {
      type: 'warning'
    })
    
    await deleteHistory([item.id])
    historyList.value = historyList.value.filter(h => h.id !== item.id)
    ElMessage.success('已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 批量下载
const batchDownload = () => {
  const selected = historyList.value.filter(item => item.selected)
  ElMessage.success(`开始批量下载 ${selected.length} 个文件`)
  // TODO: 实现批量下载逻辑
}

// 批量删除
const batchDelete = async () => {
  try {
    const selected = historyList.value.filter(item => item.selected)
    await ElMessageBox.confirm(`确定删除选中的 ${selected.length} 条记录吗？`, '提示', {
      type: 'warning'
    })
    
    const ids = selected.map(item => item.id)
    await deleteHistory(ids)
    historyList.value = historyList.value.filter(item => !item.selected)
    ElMessage.success('已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 取消选择
const cancelSelection = () => {
  historyList.value.forEach(item => {
    item.selected = false
  })
  selectAll.value = false
}

// 清空全部
const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有传输记录吗？此操作不可恢复！', '警告', {
      type: 'warning',
      confirmButtonText: '确定清空',
      cancelButtonText: '取消'
    })
    
    await clearAllHistory()
    historyList.value = []
    ElMessage.success('已清空所有记录')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

// 获取文件图标类
const getFileIconClass = (fileName) => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  const iconMap = {
    'mp4': 'video',
    'avi': 'video',
    'mkv': 'video',
    'jpg': 'image',
    'png': 'image',
    'gif': 'image',
    'pdf': 'pdf',
    'doc': 'document',
    'docx': 'document',
    'zip': 'archive',
    'rar': 'archive'
  }
  return iconMap[ext] || 'file'
}

// 格式化时长
const formatDuration = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  
  if (h > 0) return `${h}时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

// 暴露方法给父组件
defineExpose({
  historyList,
  loadHistoryList
})
</script>

<style scoped>
.completed-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 筛选工具栏 */
.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 15px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

/* 历史记录列表 */
.history-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
}

.list-header {
  display: grid;
  grid-template-columns: 40px 1fr 120px 100px 180px 150px;
  gap: 10px;
  align-items: center;
  padding: 15px 20px;
  background: var(--el-fill-color-light);
  border-bottom: 2px solid var(--el-border-color);
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.header-label {
  text-align: center;
}

.list-body {
  flex: 1;
  overflow-y: auto;
}

.list-item {
  display: grid;
  grid-template-columns: 40px 1fr 120px 100px 180px 150px;
  gap: 10px;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: all 0.3s;
}

.list-item:hover {
  background: var(--el-fill-color-lighter);
}

.list-item.selected {
  background: var(--el-color-primary-light-9);
}

.item-file {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.file-icon {
  font-size: 32px;
  color: var(--el-color-primary);
  flex-shrink: 0;
}

.file-icon.video {
  color: #E040FB;
}

.file-icon.image {
  color: #FF9800;
}

.file-icon.pdf {
  color: #F44336;
}

.file-icon.document {
  color: #2196F3;
}

.file-icon.archive {
  color: #9C27B0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.file-hash {
  font-size: 12px;
}

.item-size {
  text-align: center;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.item-type {
  display: flex;
  justify-content: center;
}

.item-time {
  text-align: center;
}

.time-text {
  font-size: 13px;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.speed-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.item-actions {
  display: flex;
  justify-content: center;
  gap: 5px;
}

/* 批量操作 */
.batch-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px 20px;
  background: var(--el-color-primary-light-9);
  border-top: 1px solid var(--el-border-color);
}

.selected-info {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-right: auto;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 15px 20px;
  background: var(--el-fill-color-lighter);
  border-top: 1px solid var(--el-border-color);
}

/* 平板适配 */
@media (max-width: 1024px) {
  .filter-toolbar {
    flex-direction: column;
    gap: 12px;
    
    :deep(.el-form) {
      width: 100%;
    }
    
    :deep(.el-form-item) {
      margin-bottom: 8px;
    }
    
    .toolbar-actions {
      width: 100%;
    }
  }
  
  .list-header,
  .list-item {
    grid-template-columns: 40px 1fr 100px 80px 150px 120px;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .filter-toolbar {
    padding: 12px;
    
    :deep(.el-date-picker) {
      width: 100% !important;
    }
    
    :deep(.el-input) {
      width: 100% !important;
    }
    
    :deep(.el-select) {
      width: 100% !important;
    }
    
    :deep(.el-form--inline .el-form-item) {
      display: flex;
      width: 100%;
      margin-right: 0;
    }
    
    :deep(.el-form--inline .el-form-item__content) {
      flex: 1;
    }
    
    .toolbar-actions {
      justify-content: center;
    }
  }
  
  .list-header {
    display: none;
  }
  
  .list-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px;
    
    .el-checkbox {
      position: absolute;
      top: 12px;
      left: 12px;
    }
    
    position: relative;
    padding-left: 40px;
  }
  
  .item-file {
    width: 100%;
    
    .file-icon {
      font-size: 28px;
    }
    
    .file-name {
      font-size: 13px;
    }
  }
  
  .item-size,
  .item-type,
  .item-time {
    text-align: left;
    font-size: 12px;
  }
  
  .item-actions {
    justify-content: flex-start;
    width: 100%;
  }
  
  .batch-actions {
    flex-wrap: wrap;
    padding: 10px 12px;
    gap: 8px;
    
    .selected-info {
      width: 100%;
      text-align: center;
      margin-right: 0;
      margin-bottom: 4px;
    }
    
    .el-button {
      flex: 1;
    }
  }
  
  .pagination {
    padding: 10px 12px;
    justify-content: center;
    
    :deep(.el-pagination) {
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
      
      .el-pagination__sizes,
      .el-pagination__jump {
        display: none;
      }
    }
  }
}
</style>

