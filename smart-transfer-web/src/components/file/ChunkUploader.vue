<template>
  <el-dialog
    v-model="dialogVisible"
    title="上传文件"
    width="550px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="chunk-uploader">
      <!-- 上传方式选择 -->
      <div class="upload-actions">
        <el-button type="primary" @click="selectFiles">
          <el-icon><Document /></el-icon>
          选择文件
        </el-button>
        <el-button @click="selectFolder">
          <el-icon><Folder /></el-icon>
          选择文件夹
        </el-button>
        <el-button @click="showDragUpload">
          <el-icon><UploadFilled /></el-icon>
          拖拽上传
        </el-button>
      </div>
      
      <!-- 隐藏的文件选择器 -->
      <input
        ref="fileInputRef"
        type="file"
        multiple
        style="display: none"
        @change="handleFileSelect"
      />
      <input
        ref="folderInputRef"
        type="file"
        webkitdirectory
        directory
        multiple
        style="display: none"
        @change="handleFolderSelect"
      />
      
      <!-- 上传区域 -->
      <div
        class="upload-area"
        @dragover.prevent="handleDragOver"
        @dragleave.prevent="handleDragLeave"
        @drop.prevent="handleDrop"
        :class="{ 'drag-over': isDragOver }"
      >
        <div class="upload-content">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p class="main-text">点击上方按钮或拖拽文件到此处上传</p>
            <p class="sub-text">支持大文件分片上传、断点续传、秒传</p>
          </div>
        </div>
      </div>
      
      <!-- 已选择的文件列表（简化显示） -->
      <div v-if="pendingFiles.length > 0" class="pending-files">
        <div class="pending-header">
          <span>已选择 {{ pendingFiles.length }} 个文件</span>
          <el-button size="small" text type="danger" @click="clearPendingFiles">
            清空
          </el-button>
        </div>
        <div class="pending-list">
          <div v-for="(file, index) in pendingFiles.slice(0, 5)" :key="index" class="pending-item">
            <el-icon class="file-icon"><Document /></el-icon>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ formatSize(file.size) }}</span>
          </div>
          <div v-if="pendingFiles.length > 5" class="more-files">
            ...还有 {{ pendingFiles.length - 5 }} 个文件
          </div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button
        type="primary"
        :disabled="pendingFiles.length === 0"
        @click="addToUploadQueue"
      >
        开始上传
      </el-button>
    </template>
  </el-dialog>
  
  <!-- 全屏拖拽上传遮罩 -->
  <UploadMask ref="uploadMaskRef" @upload="handleMaskUpload" />
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { UploadFilled, Document, Folder } from '@element-plus/icons-vue'
import UploadMask from './UploadMask.vue'
import { useTransferStore } from '@/store/transferStore'
import { useFileStore } from '@/store/fileStore'

const router = useRouter()
const transferStore = useTransferStore()
const fileStore = useFileStore()

// Props
const props = defineProps({
  folderId: {
    type: [Number, String],
    default: 0
  }
})

// Emits
const emit = defineEmits(['success', 'close'])

// 状态
const dialogVisible = ref(false)
const fileInputRef = ref(null)
const folderInputRef = ref(null)
const uploadMaskRef = ref(null)
const isDragOver = ref(false)
const pendingFiles = ref([]) // 待上传的文件列表

// 格式化文件大小
const formatSize = (size) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 打开对话框
const open = () => {
  dialogVisible.value = true
  pendingFiles.value = []
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  pendingFiles.value = []
  emit('close')
}

// 选择文件
const selectFiles = () => {
  fileInputRef.value?.click()
}

// 选择文件夹
const selectFolder = () => {
  folderInputRef.value?.click()
}

// 显示拖拽上传遮罩
const showDragUpload = () => {
  uploadMaskRef.value?.show()
}

// 处理文件选择
const handleFileSelect = (e) => {
  const files = Array.from(e.target.files || [])
  if (files.length > 0) {
    addFilesToPending(files)
  }
  // 清空input，允许重复选择同一文件
  e.target.value = ''
}

// 处理文件夹选择
const handleFolderSelect = (e) => {
  const files = Array.from(e.target.files || [])
  if (files.length > 0) {
    addFilesToPending(files)
  }
  e.target.value = ''
}

// 拖拽相关
const handleDragOver = (e) => {
  isDragOver.value = true
}

const handleDragLeave = (e) => {
  isDragOver.value = false
}

const handleDrop = (e) => {
  isDragOver.value = false
  const files = Array.from(e.dataTransfer?.files || [])
  if (files.length > 0) {
    addFilesToPending(files)
  }
}

// 处理拖拽遮罩上传
const handleMaskUpload = (files) => {
  if (files.length > 0) {
    addFilesToPending(files)
  }
}

// 添加文件到待上传列表
const addFilesToPending = (files) => {
  pendingFiles.value = [...pendingFiles.value, ...files]
  ElMessage.success(`已选择 ${files.length} 个文件`)
}

// 清空待上传文件
const clearPendingFiles = () => {
  pendingFiles.value = []
}

// 添加到上传队列并开始上传
const addToUploadQueue = () => {
  if (pendingFiles.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  // 获取当前文件夹ID
  const currentFolderId = fileStore.currentFolderId || props.folderId || 0
  
  // 将文件添加到传输队列
  pendingFiles.value.forEach(file => {
    transferStore.addUploadTask({
      file: file,
      fileName: file.name,
      fileSize: file.size,
      folderId: currentFolderId,
      relativePath: file.webkitRelativePath || ''
    })
  })
  
  ElMessage.success(`已添加 ${pendingFiles.value.length} 个文件到上传列表`)
  
  // 关闭对话框
  handleClose()
  
  // 跳转到传输中心
  router.push({ name: 'TransferCenter' })
}

// 暴露方法
defineExpose({
  open
})
</script>

<style lang="scss" scoped>
.chunk-uploader {
  .upload-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-bottom: 20px;
  }
  
  .upload-area {
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    padding: 40px;
    text-align: center;
    transition: all 0.3s;
    cursor: pointer;
    
    &:hover,
    &.drag-over {
      border-color: var(--el-color-primary);
      background: #f5f7fa;
    }
    
    .upload-content {
      .upload-icon {
        font-size: 48px;
        color: #c0c4cc;
        margin-bottom: 16px;
      }
      
      .upload-text {
        .main-text {
          font-size: 16px;
          color: #606266;
          margin-bottom: 8px;
        }
        
        .sub-text {
          font-size: 14px;
          color: #909399;
        }
      }
    }
  }
  
  .pending-files {
    margin-top: 20px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    overflow: hidden;
    
    .pending-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 16px;
      background: #fafbfc;
      border-bottom: 1px solid #ebeef5;
      font-size: 14px;
      color: #606266;
    }
    
    .pending-list {
      max-height: 200px;
      overflow-y: auto;
      
      .pending-item {
        display: flex;
        align-items: center;
        padding: 10px 16px;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
        }
        
        .file-icon {
          font-size: 20px;
          color: var(--el-color-primary);
          margin-right: 10px;
        }
        
        .file-name {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          font-size: 14px;
          color: #303133;
        }
        
        .file-size {
          color: #909399;
          font-size: 12px;
          margin-left: 10px;
        }
      }
      
      .more-files {
        padding: 10px 16px;
        text-align: center;
        color: #909399;
        font-size: 13px;
      }
    }
  }
}
</style>
