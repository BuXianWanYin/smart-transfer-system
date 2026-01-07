<template>
  <div class="file-list-wrapper">
    <!-- 操作菜单 -->
    <OperationMenu
      :file-type="fileType"
      :file-path="filePath"
      :operation-file-list="selectedFiles"
      :is-batch-operation="selectedFiles.length > 1"
      @refresh="loadFileList"
      @upload-file="handleUploadFile"
      @new-folder="handleNewFolder"
      @search-file="handleSearchFile"
      @show-upload="showUploader = true"
      @change-mode="handleChangeMode"
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
        @refresh="loadFileList"
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      />
      
      <!-- 网格视图 -->
      <FileGrid
        v-else-if="displayMode === 1"
        :file-type="fileType"
        :file-path="filePath"
        :file-list="fileList"
        :loading="loading"
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
      v-if="showUploader"
      :folder-id="currentFolderId"
      @close="showUploader = false"
      @uploaded="loadFileList"
    />
    
    <!-- 文件预览 -->
    <FilePreview
      v-model="previewVisible"
      :file="previewFile"
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
import { getFolderContent } from '@/api/folderApi'
import { createFolder as createFolderApi } from '@/api/folderApi'
import { searchFile } from '@/api/fileApi'
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

// 选中的文件
const selectedFiles = ref([])

// 上传组件
const showUploader = ref(false)

// 文件预览
const previewVisible = ref(false)
const previewFile = ref(null)

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
  } else {
    // 预览文件
    previewFile.value = row
    previewVisible.value = true
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
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
  showUploader.value = true
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
</style>
