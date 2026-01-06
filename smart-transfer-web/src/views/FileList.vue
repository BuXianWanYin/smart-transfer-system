<template>
  <div class="file-list-page page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-title">文件列表</div>
      <div class="page-description">管理已上传的文件，支持下载和删除操作</div>
    </div>
    
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-form :inline="true" :model="queryForm">
          <el-form-item label="状态">
            <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 150px">
              <el-option label="全部" value="" />
              <el-option label="待上传" value="PENDING" />
              <el-option label="上传中" value="UPLOADING" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="refreshList">
          <el-icon><Refresh /></el-icon>
          刷新列表
        </el-button>
      </div>
    </div>
    
    <!-- 文件表格卡片 -->
    <el-card class="table-card">
      
      <el-table
        v-loading="loading"
        :data="fileList"
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="fileName" label="文件名" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon size="18"><Document /></el-icon>
              <span>{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            <span class="badge badge-info">{{ formatFileSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="uploadStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.uploadStatus)" effect="plain">
              {{ getStatusText(row.uploadStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.uploadStatus === 'COMPLETED'"
              link
              type="primary"
              @click="handleDownload(row)"
            >
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <div v-if="!loading && fileList.length === 0" class="empty-state">
        <el-icon class="empty-state-icon"><FolderOpened /></el-icon>
        <div class="empty-state-text">暂无文件数据</div>
        <el-button type="primary" @click="refreshList">刷新列表</el-button>
      </div>
      
      <!-- 分页 -->
      <div v-if="fileList.length > 0" style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Document, Download, Delete, FolderOpened } from '@element-plus/icons-vue'
import { getFileList, deleteFile, getDownloadUrl } from '@/api/fileApi'
import { formatFileSize } from '@/utils/file'
import { formatDateTime, getUploadStatusText } from '@/utils/format'

const loading = ref(false)
const fileList = ref([])
const total = ref(0)
const queryForm = ref({
  pageNum: 1,
  pageSize: 10,
  status: ''
})

onMounted(() => {
  fetchFileList()
})

const fetchFileList = async () => {
  try {
    loading.value = true
    const res = await getFileList(queryForm.value)
    fileList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取文件列表失败：', error)
    ElMessage.error('获取文件列表失败')
    fileList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryForm.value.pageNum = 1
  fetchFileList()
}

const handleReset = () => {
  queryForm.value = {
    pageNum: 1,
    pageSize: 10,
    status: ''
  }
  fetchFileList()
}

const refreshList = () => {
  fetchFileList()
}

const handleDownload = (row) => {
  const url = getDownloadUrl(row.id)
  window.open(url, '_blank')
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该文件吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteFile(row.id)
    ElMessage.success('删除成功')
    fetchFileList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'UPLOADING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  return getUploadStatusText(status)
}
</script>

<style scoped>
.table-card {
  margin-top: 0;
}

.el-table {
  margin-top: 0;
}
</style>

