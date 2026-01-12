<template>
  <div class="file-list-wrapper">
    <!-- 操作菜单 -->
    <OperationMenu
      ref="operationMenuRef"
      :file-type="fileType"
      :file-path="filePath"
      :operation-file-list="selectedFiles"
      :is-batch-operation="selectedFiles.length > 1"
      :file-list="fileList"
      @refresh="loadFileList"
      @upload-file="handleUploadFile"
      @new-folder="handleNewFolder"
      @search-file="handleSearchFile"
      @show-upload="handleUploadFile"
      @change-mode="handleChangeMode"
      @change-grid-size="handleGridSizeChange"
      @change-columns="handleColumnsChange"
    />
    
    <!-- 面包屑导航 -->
    <BreadCrumb
      :file-path="filePath"
      @navigate="handleBreadcrumbNav"
    />
    
    <!-- 文件列表区域 -->
    <div class="file-list-content">
      <!-- 表格视图 -->
      <FileTable
        v-if="displayMode === 0"
        :file-type="fileType"
        :file-path="filePath"
        :file-list="fileList"
        :loading="loading"
        :visible-columns="visibleColumns"
        @refresh="loadFileList"
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      />
      
      <!-- 网格视图 -->
      <FileGrid
        v-else-if="displayMode === 1"
        ref="fileGridRef"
        :file-type="fileType"
        :file-path="filePath"
        :file-list="fileList"
        :loading="loading"
        :grid-size="gridSize"
        @refresh="loadFileList"
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      />
      
      <!-- 时间线视图 -->
      <FileTimeLine
        v-else
        :file-type="fileType"
        :file-path="filePath"
        :file-list="fileList"
        :loading="loading"
        @refresh="loadFileList"
        @selection-change="handleSelectionChange"
      />
    </div>
    
    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0 && displayMode !== 2">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 上传组件 -->
    <ChunkUploader
      ref="chunkUploaderRef"
      :folder-id="currentFolderId"
      @close="showUploader = false"
      @success="loadFileList"
    />
    
    <!-- 简单文件预览（视频、音频、PDF等） -->
    <FilePreview
      v-model="previewVisible"
      :file="previewFile"
    />
    
    <!-- 增强版图片预览 -->
    <ImagePreview
      v-model="imagePreviewVisible"
      :image-list="imageListForPreview"
      :initial-index="imagePreviewIndex"
    />
    
    <!-- 代码预览 -->
    <CodePreview
      v-model="codePreviewVisible"
      :file="codePreviewFile"
      :read-only="true"
    />
    
    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="newFolderVisible" title="新建文件夹" width="400px">
      <el-form ref="folderFormRef" :model="folderForm" :rules="folderRules">
        <el-form-item prop="folderName">
          <el-input v-model="folderForm.folderName" placeholder="请输入文件夹名称" @keyup.enter="createFolder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newFolderVisible = false">取消</el-button>
        <el-button type="primary" @click="createFolder" :loading="folderLoading">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import OperationMenu from './OperationMenu.vue'
import BreadCrumb from './BreadCrumb.vue'
import FileTable from './FileTable.vue'
import FileGrid from './FileGrid.vue'
import FileTimeLine from './FileTimeLine.vue'
import ChunkUploader from './ChunkUploader.vue'
import FilePreview from './FilePreview.vue'
import ImagePreview from './ImagePreview.vue'
import CodePreview from './CodePreview.vue'
import { getFolderContent } from '@/api/folderApi'
import { createFolder as createFolderApi } from '@/api/folderApi'
import { searchFile, getPreviewUrl } from '@/api/fileApi'
import { getRecoveryFileList } from '@/api/recoveryApi'

const route = useRoute()
const router = useRouter()

// 路由参数
const fileType = computed(() => parseInt(route.query.fileType) || 0)
const filePath = computed(() => route.query.filePath || '/')
const currentFolderId = computed(() => parseInt(route.query.folderId) || 0)

// 文件列表
const fileList = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

// 显示模式: 0-表格 1-网格 2-时间线
const displayMode = ref(0)

// 网格图标大小
const gridSize = ref(parseInt(localStorage.getItem('file_grid_size') || '120'))

// 显示的列（从 localStorage 初始化）
const initVisibleColumns = () => {
  const saved = localStorage.getItem('file_table_columns')
  if (saved) {
    try {
      return JSON.parse(saved)
    } catch {
      return ['extendName', 'fileSize', 'updateTime']
    }
  }
  return ['extendName', 'fileSize', 'updateTime']
}
const visibleColumns = ref(initVisibleColumns())

// 选中的文件
const selectedFiles = ref([])

// 组件引用
const operationMenuRef = ref(null)
const fileGridRef = ref(null)
const chunkUploaderRef = ref(null)

// 上传组件
const showUploader = ref(false)

// 文件预览（简单预览）
const previewVisible = ref(false)
const previewFile = ref(null)

// 图片预览（增强版）
const imagePreviewVisible = ref(false)
const imagePreviewIndex = ref(0)
const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']

// 代码预览
const codePreviewVisible = ref(false)
const codePreviewFile = ref(null)
const codeExtensions = ['js', 'ts', 'vue', 'jsx', 'tsx', 'json', 'html', 'css', 'scss', 'less', 
  'java', 'py', 'go', 'c', 'cpp', 'h', 'hpp', 'sql', 'sh', 'bash', 'xml', 'yml', 'yaml', 'md', 
  'txt', 'ini', 'conf', 'properties', 'php', 'rb', 'rs', 'swift', 'kt']

// 获取图片列表（用于增强预览）
const imageListForPreview = computed(() => {
  return fileList.value.filter(item => {
    if (item.isDir === 1) return false
    const ext = (item.extendName || '').toLowerCase()
    return imageExtensions.includes(ext)
  }).map(item => ({
    ...item,
    fileUrl: getPreviewUrl(item.id)
  }))
})

// 判断文件类型
const isImageFile = (file) => {
  if (!file || file.isDir === 1) return false
  const ext = (file.extendName || '').toLowerCase()
  return imageExtensions.includes(ext)
}

const isCodeFile = (file) => {
  if (!file || file.isDir === 1) return false
  const ext = (file.extendName || '').toLowerCase()
  return codeExtensions.includes(ext)
}

// 新建文件夹
const newFolderVisible = ref(false)
const folderFormRef = ref(null)
const folderForm = ref({ folderName: '' })
const folderRules = { folderName: [{ required: true, message: '请输入文件夹名称', trigger: 'blur' }] }
const folderLoading = ref(false)

// 加载文件列表 (定义在 watch 之前)
const loadFileList = async () => {
  loading.value = true
  try {
    // 回收站特殊处理
    if (fileType.value === 6) {
      const data = await getRecoveryFileList()
      fileList.value = data || []
      total.value = fileList.value.length
      return
    }
    
    // 普通文件列表
    const params = {
      parentId: currentFolderId.value,
      fileType: fileType.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    const res = await getFolderContent(params)
    
    // 合并文件夹和文件
    const folders = (res.folders || []).map(f => ({
      ...f,
      isDir: 1,
      fileName: f.folderName
    }))
    const files = res.files || []
    
    fileList.value = [...folders, ...files]
    total.value = res.total || fileList.value.length
    
  } catch (error) {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

// 监听路由变化
watch([fileType, filePath, currentFolderId], () => {
  pageNum.value = 1
  loadFileList()
}, { immediate: true })

// 切换显示模式
const handleChangeMode = (mode) => {
  displayMode.value = mode
}

// 网格图标大小变化
const handleGridSizeChange = (size) => {
  gridSize.value = size
}

// 列变化（同步更新并保存）
const handleColumnsChange = (columns) => {
  visibleColumns.value = columns
  // 保存到 localStorage（SelectColumn 内部也会保存，这里做双重保证）
  localStorage.setItem('file_table_columns', JSON.stringify(columns))
}

// 面包屑导航
const handleBreadcrumbNav = (path, folderId) => {
  router.push({
    name: 'File',
    query: {
      fileType: fileType.value,
      filePath: path,
      folderId: folderId || 0
    }
  })
}

// 行点击事件
const handleRowClick = (row) => {
  if (row.isDir === 1) {
    // 进入文件夹
    const newPath = filePath.value === '/' 
      ? `/${row.fileName || row.folderName}`
      : `${filePath.value}/${row.fileName || row.folderName}`
    
    router.push({
      name: 'File',
      query: {
        fileType: fileType.value,
        filePath: newPath,
        folderId: row.id
      }
    })
  } else if (isImageFile(row)) {
    // 图片文件 - 使用增强版图片预览
    const index = imageListForPreview.value.findIndex(img => img.id === row.id)
    if (index >= 0) {
      imagePreviewIndex.value = index
      imagePreviewVisible.value = true
    }
  } else if (isCodeFile(row)) {
    // 代码文件 - 使用代码预览
    codePreviewFile.value = row
    codePreviewVisible.value = true
  } else {
    // 其他文件 - 使用普通预览
    previewFile.value = row
    previewVisible.value = true
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

// 全选
const handleSelectAll = (val) => {
  if (fileGridRef.value) {
    if (val) {
      fileGridRef.value.selectAll()
    } else {
      fileGridRef.value.clearSelection()
    }
  }
}

// 清除选择
const handleClearSelection = () => {
  if (fileGridRef.value) {
    fileGridRef.value.clearSelection()
  }
  selectedFiles.value = []
}

// 分页
const handleSizeChange = (size) => {
  pageSize.value = size
  loadFileList()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadFileList()
}

// 上传文件
const handleUploadFile = () => {
  chunkUploaderRef.value?.open()
}

// 新建文件夹
const handleNewFolder = () => {
  folderForm.value.folderName = ''
  newFolderVisible.value = true
}

const createFolder = async () => {
  try {
    await folderFormRef.value.validate()
    folderLoading.value = true
    
    await createFolderApi({
      folderName: folderForm.value.folderName,
      parentId: currentFolderId.value
    })
    
    ElMessage.success('文件夹创建成功')
    newFolderVisible.value = false
    loadFileList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('创建文件夹失败')
    }
  } finally {
    folderLoading.value = false
  }
}

// 搜索文件
const handleSearchFile = async (keyword) => {
  if (!keyword || !keyword.trim()) {
    loadFileList()
    return
  }
  
  loading.value = true
  try {
    const res = await searchFile({ fileName: keyword })
    fileList.value = res || []
    total.value = fileList.value.length
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.file-list-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  
  .file-list-content {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  
  .pagination-wrapper {
    padding: 16px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid #ebeef5;
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .file-list-wrapper {
    .pagination-wrapper {
      padding: 12px;
      
      :deep(.el-pagination) {
        flex-wrap: wrap;
        justify-content: center;
        gap: 8px;
        
        .el-pagination__sizes,
        .el-pagination__jump {
          display: none;
        }
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .file-list-wrapper {
    .pagination-wrapper {
      padding: 10px;
      justify-content: center;
      
      :deep(.el-pagination) {
        .el-pagination__total {
          display: none;
        }
        
        .btn-prev,
        .btn-next {
          padding: 0 8px;
        }
      }
    }
    
    // 移动端新建文件夹对话框全屏
    :deep(.el-dialog) {
      width: 90% !important;
      margin: 5vh auto !important;
    }
  }
}
</style>
