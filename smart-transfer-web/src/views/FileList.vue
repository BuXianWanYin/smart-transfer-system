<template>
  <div class="file-manager page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-title">文件管理</div>
      <div class="page-description">管理您的文件和文件夹</div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item @click="navigateToFolder(0)">
            <el-icon><HomeFilled /></el-icon>
            <span class="breadcrumb-link">全部文件</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item 
            v-for="folder in breadcrumb" 
            :key="folder.id"
            @click="navigateToFolder(folder.id)"
          >
            <span class="breadcrumb-link">{{ folder.folderName }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="showCreateFolderDialog">
          <el-icon><FolderAdd /></el-icon>
          新建文件夹
        </el-button>
        <el-button @click="refreshContent">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-radio-group v-model="viewMode" size="small" style="margin-left: 12px;">
          <el-radio-button value="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
          <el-radio-button value="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 文件内容区域 -->
    <el-card class="content-card" v-loading="loading">
      <!-- 列表视图 -->
      <template v-if="viewMode === 'list'">
        <el-table :data="tableData" style="width: 100%" @row-dblclick="handleDoubleClick">
          <el-table-column width="50">
            <template #default="{ row }">
              <el-icon size="24" :class="row.type === 'folder' ? 'folder-icon' : 'file-icon'">
                <Folder v-if="row.type === 'folder'" />
                <Document v-else />
              </el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="300">
            <template #default="{ row }">
              <span class="item-name" @dblclick="handleDoubleClick(row)">{{ row.name }}</span>
              <el-tag v-if="row.type === 'folder'" size="small" type="info" style="margin-left: 8px;">
                {{ row.subFolderCount || 0 }}个文件夹, {{ row.fileCount || 0 }}个文件
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="size" label="大小" width="120">
            <template #default="{ row }">
              <span v-if="row.type === 'file'">{{ formatFileSize(row.fileSize) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <template v-if="row.type === 'folder'">
                <el-button link type="primary" @click="navigateToFolder(row.id)">
                  <el-icon><FolderOpened /></el-icon>
                  打开
                </el-button>
                <el-button link type="warning" @click="showRenameDialog(row)">
                  <el-icon><Edit /></el-icon>
                  重命名
                </el-button>
                <el-button link type="danger" @click="handleDeleteFolder(row)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
              <template v-else>
                <el-button link type="primary" @click="handleDownload(row)">
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
                <el-button link type="danger" @click="handleDeleteFile(row)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 网格视图 -->
      <template v-else>
        <div class="grid-view">
          <div 
            v-for="item in tableData" 
            :key="item.type + '-' + item.id" 
            class="grid-item"
            @dblclick="handleDoubleClick(item)"
          >
            <div class="grid-item-icon">
              <el-icon size="48" :class="item.type === 'folder' ? 'folder-icon' : 'file-icon'">
                <Folder v-if="item.type === 'folder'" />
                <Document v-else />
              </el-icon>
            </div>
            <div class="grid-item-name" :title="item.name">{{ item.name }}</div>
            <div class="grid-item-info" v-if="item.type === 'file'">
              {{ formatFileSize(item.fileSize) }}
            </div>
            <div class="grid-item-actions">
              <template v-if="item.type === 'folder'">
                <el-button size="small" circle @click.stop="navigateToFolder(item.id)">
                  <el-icon><FolderOpened /></el-icon>
                </el-button>
                <el-button size="small" circle type="danger" @click.stop="handleDeleteFolder(item)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
              <template v-else>
                <el-button size="small" circle @click.stop="handleDownload(item)">
                  <el-icon><Download /></el-icon>
                </el-button>
                <el-button size="small" circle type="danger" @click.stop="handleDeleteFile(item)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-if="!loading && tableData.length === 0" class="empty-state">
        <el-icon class="empty-state-icon"><FolderOpened /></el-icon>
        <div class="empty-state-text">当前文件夹为空</div>
        <el-button type="primary" @click="showCreateFolderDialog">新建文件夹</el-button>
      </div>

      <!-- 分页 -->
      <div v-if="fileTotal > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="fileTotal"
          :page-sizes="[20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next"
          @size-change="loadFolderContent"
          @current-change="loadFolderContent"
        />
      </div>
    </el-card>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="createFolderVisible" title="新建文件夹" width="400px">
      <el-form>
        <el-form-item label="文件夹名称">
          <el-input v-model="newFolderName" placeholder="请输入文件夹名称" @keyup.enter="handleCreateFolder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFolderVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameVisible" title="重命名" width="400px">
      <el-form>
        <el-form-item label="新名称">
          <el-input v-model="renameValue" placeholder="请输入新名称" @keyup.enter="handleRename" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  HomeFilled, FolderAdd, Refresh, List, Grid, Folder, Document, 
  FolderOpened, Edit, Delete, Download 
} from '@element-plus/icons-vue'
import { getFolderContent, createFolder, renameFolder, deleteFolder } from '@/api/folderApi'
import { deleteFile, getDownloadUrl } from '@/api/fileApi'
import { formatFileSize } from '@/utils/file'
import { formatDateTime } from '@/utils/format'

// 状态
const loading = ref(false)
const creating = ref(false)
const viewMode = ref('list')
const currentFolderId = ref(0)
const breadcrumb = ref([])
const folders = ref([])
const files = ref([])
const fileTotal = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

// 对话框状态
const createFolderVisible = ref(false)
const newFolderName = ref('')
const renameVisible = ref(false)
const renameValue = ref('')
const renameTarget = ref(null)

// 合并文件夹和文件为表格数据
const tableData = computed(() => {
  const folderItems = folders.value.map(f => ({
    ...f,
    type: 'folder',
    name: f.folderName
  }))
  const fileItems = files.value.map(f => ({
    ...f,
    type: 'file',
    name: f.fileName
  }))
  return [...folderItems, ...fileItems]
})

// 加载文件夹内容
const loadFolderContent = async () => {
  try {
    loading.value = true
    const res = await getFolderContent({
      folderId: currentFolderId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    breadcrumb.value = res.breadcrumb || []
    folders.value = res.folders || []
    files.value = res.files || []
    fileTotal.value = res.fileTotal || 0
  } catch (error) {
    console.error('加载文件夹内容失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 导航到文件夹
const navigateToFolder = (folderId) => {
  currentFolderId.value = folderId
  pageNum.value = 1
  loadFolderContent()
}

// 双击处理
const handleDoubleClick = (row) => {
  if (row.type === 'folder') {
    navigateToFolder(row.id)
  }
}

// 刷新
const refreshContent = () => {
  loadFolderContent()
}

// 显示创建文件夹对话框
const showCreateFolderDialog = () => {
  newFolderName.value = ''
  createFolderVisible.value = true
}

// 创建文件夹
const handleCreateFolder = async () => {
  if (!newFolderName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  
  try {
    creating.value = true
    await createFolder({
      folderName: newFolderName.value.trim(),
      parentId: currentFolderId.value
    })
    ElMessage.success('创建成功')
    createFolderVisible.value = false
    loadFolderContent()
  } catch (error) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 显示重命名对话框
const showRenameDialog = (row) => {
  renameTarget.value = row
  renameValue.value = row.name
  renameVisible.value = true
}

// 重命名
const handleRename = async () => {
  if (!renameValue.value.trim()) {
    ElMessage.warning('请输入新名称')
    return
  }

  try {
    await renameFolder({
      folderId: renameTarget.value.id,
      newName: renameValue.value.trim()
    })
    ElMessage.success('重命名成功')
    renameVisible.value = false
    loadFolderContent()
  } catch (error) {
    ElMessage.error(error.message || '重命名失败')
  }
}

// 删除文件夹
const handleDeleteFolder = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除文件夹"${row.name}"吗？文件夹内的所有内容将被删除！`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteFolder(row.id)
    ElMessage.success('删除成功')
    loadFolderContent()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 下载文件
const handleDownload = (row) => {
  const url = getDownloadUrl(row.id)
  window.open(url, '_blank')
}

// 删除文件
const handleDeleteFile = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除文件"${row.name}"吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteFile(row.id)
    ElMessage.success('删除成功')
    loadFolderContent()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadFolderContent()
})
</script>

<style scoped>
.file-manager {
  min-height: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-radius: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-link {
  cursor: pointer;
  color: var(--el-color-primary);
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.content-card {
  min-height: 500px;
}

.folder-icon {
  color: #f9a825;
}

.file-icon {
  color: #42a5f5;
}

.item-name {
  cursor: pointer;
}

.item-name:hover {
  color: var(--el-color-primary);
}

/* 网格视图样式 */
.grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  padding: 16px;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.grid-item:hover {
  background: var(--el-fill-color-light);
}

.grid-item:hover .grid-item-actions {
  opacity: 1;
}

.grid-item-icon {
  margin-bottom: 8px;
}

.grid-item-name {
  font-size: 13px;
  text-align: center;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.grid-item-info {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.grid-item-actions {
  position: absolute;
  bottom: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.empty-state-icon {
  font-size: 64px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 16px;
}

.empty-state-text {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
}
</style>
