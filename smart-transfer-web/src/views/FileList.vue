<template>
  <div class="file-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件列表</span>
          <el-button type="primary" @click="refreshList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="待上传" value="PENDING" />
            <el-option label="上传中" value="UPLOADING" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 文件表格 -->
      <el-table
        v-loading="loading"
        :data="fileList"
        style="width: 100%"
      >
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.uploadStatus)">
              {{ getStatusText(row.uploadStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.uploadStatus === 'COMPLETED'"
              size="small"
              type="primary"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
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
    fileList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    ElMessage.error('获取文件列表失败')
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
.file-list-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

