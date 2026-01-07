<template>
  <div class="file-uploader">
    <!-- 隐藏的文件选择 -->
    <input 
      ref="fileInputRef" 
      type="file" 
      multiple 
      style="display: none"
      @change="handleFileSelect"
    />
    
    <!-- 上传面板 -->
    <div class="upload-panel" v-show="showPanel">
      <div class="panel-header">
        <span>上传列表 ({{ uploadList.length }})</span>
        <div class="panel-actions">
          <el-button link @click="toggleCollapse">
            <el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
          </el-button>
          <el-button link @click="closePanel">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
      
      <el-collapse-transition>
        <div class="panel-content" v-show="!collapsed">
          <div class="upload-list">
            <div 
              v-for="item in uploadList" 
              :key="item.id" 
              class="upload-item"
            >
              <div class="item-info">
                <span class="item-name" :title="item.fileName">{{ item.fileName }}</span>
                <span class="item-size">{{ formatFileSize(item.fileSize) }}</span>
              </div>
              <div class="item-progress">
                <el-progress 
                  :percentage="item.progress" 
                  :status="getProgressStatus(item)"
                  :stroke-width="4"
                />
              </div>
              <div class="item-status">
                <span v-if="item.status === 'md5'">计算MD5 {{ item.md5Progress }}%</span>
                <span v-else-if="item.status === 'uploading'">上传中</span>
                <span v-else-if="item.status === 'success'" class="success">上传成功</span>
                <span v-else-if="item.status === 'error'" class="error">{{ item.errorMsg || '上传失败' }}</span>
                <span v-else-if="item.status === 'paused'">已暂停</span>
                <span v-else>等待中</span>
              </div>
              <div class="item-actions">
                <el-button 
                  v-if="item.status === 'uploading'" 
                  link 
                  @click="pauseUpload(item)"
                >
                  <el-icon><VideoPause /></el-icon>
                </el-button>
                <el-button 
                  v-if="item.status === 'paused'" 
                  link 
                  @click="resumeUpload(item)"
                >
                  <el-icon><VideoPlay /></el-icon>
                </el-button>
                <el-button 
                  v-if="['error', 'paused'].includes(item.status)" 
                  link 
                  @click="retryUpload(item)"
                >
                  <el-icon><RefreshRight /></el-icon>
                </el-button>
                <el-button link @click="removeUpload(item)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
            
            <div class="empty-upload" v-if="uploadList.length === 0">
              暂无上传任务
            </div>
          </div>
        </div>
      </el-collapse-transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Expand, Fold, VideoPause, VideoPlay, RefreshRight } from '@element-plus/icons-vue'
import SparkMD5 from 'spark-md5'
import { initUpload, uploadChunk, mergeFile } from '@/api/fileApi'
import { formatFileSize } from '@/utils/format'

const props = defineProps({
  currentFolderId: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['upload-success'])

// 状态
const fileInputRef = ref(null)
const showPanel = ref(false)
const collapsed = ref(false)
const uploadList = ref([])

// 配置
const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

// 触发上传
function triggerUpload() {
  fileInputRef.value?.click()
}

// 添加文件
function addFiles(files) {
  handleFiles(Array.from(files))
}

// 文件选择
function handleFileSelect(e) {
  const files = e.target.files
  if (files.length > 0) {
    handleFiles(Array.from(files))
  }
  e.target.value = ''
}

// 处理文件
async function handleFiles(files) {
  showPanel.value = true
  
  for (const file of files) {
    const uploadItem = reactive({
      id: Date.now() + Math.random(),
      file: file,
      fileName: file.name,
      fileSize: file.size,
      fileHash: '',
      fileId: null,
      progress: 0,
      md5Progress: 0,
      status: 'pending',
      errorMsg: '',
      uploadedChunks: [],
      totalChunks: Math.ceil(file.size / CHUNK_SIZE),
      abortController: null
    })
    
    uploadList.value.push(uploadItem)
    
    // 开始上传
    startUpload(uploadItem)
  }
}

// 开始上传
async function startUpload(item) {
  try {
    // 1. 计算MD5
    item.status = 'md5'
    const fileHash = await computeMD5(item.file, (progress) => {
      item.md5Progress = progress
    })
    item.fileHash = fileHash
    
    // 2. 初始化上传（检查秒传/断点续传）
    const initRes = await initUpload({
      fileName: item.fileName,
      fileSize: item.fileSize,
      fileHash: fileHash,
      totalChunks: item.totalChunks,
      chunkSize: CHUNK_SIZE,
      folderId: props.currentFolderId
    })
    
    // 3. 秒传
    if (initRes.quickUpload || initRes.skipUpload) {
      item.progress = 100
      item.status = 'success'
      emit('upload-success')
      ElMessage.success(`${item.fileName} 秒传成功`)
      return
    }
    
    // 4. 断点续传
    item.fileId = initRes.fileId
    item.uploadedChunks = initRes.uploaded || []
    
    // 5. 开始上传分片
    item.status = 'uploading'
    await uploadChunks(item)
    
  } catch (error) {
    // 上传失败
    item.status = 'error'
    item.errorMsg = error.message || '上传失败'
  }
}

// 计算MD5
function computeMD5(file, onProgress) {
  return new Promise((resolve, reject) => {
    const blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice
    const chunkSize = 2 * 1024 * 1024 // 2MB
    const chunks = Math.ceil(file.size / chunkSize)
    let currentChunk = 0
    const spark = new SparkMD5.ArrayBuffer()
    const fileReader = new FileReader()
    
    fileReader.onload = (e) => {
      spark.append(e.target.result)
      currentChunk++
      
      if (currentChunk < chunks) {
        onProgress(Math.round((currentChunk / chunks) * 100))
        loadNext()
      } else {
        onProgress(100)
        resolve(spark.end())
      }
    }
    
    fileReader.onerror = () => {
      reject(new Error('文件读取失败'))
    }
    
    function loadNext() {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      fileReader.readAsArrayBuffer(blobSlice.call(file, start, end))
    }
    
    loadNext()
  })
}

// 上传分片
async function uploadChunks(item) {
  const file = item.file
  const totalChunks = item.totalChunks
  const uploadedSet = new Set(item.uploadedChunks)
  
  // 并发上传
  const concurrency = 3
  let currentIndex = 0
  let completedCount = uploadedSet.size
  
  async function uploadNext() {
    while (currentIndex < totalChunks) {
      if (item.status === 'paused') return
      
      const chunkIndex = currentIndex++
      
      // 跳过已上传的分片
      if (uploadedSet.has(chunkIndex)) {
        completedCount++
        item.progress = Math.round((completedCount / totalChunks) * 100)
        continue
      }
      
      const start = chunkIndex * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      const chunk = file.slice(start, end)
      
      const formData = new FormData()
      formData.append('fileId', item.fileId)
      formData.append('chunkNumber', chunkIndex)
      formData.append('chunkHash', '') // 跳过分片哈希验证
      formData.append('file', chunk)
      
      try {
        await uploadChunk(formData)
        completedCount++
        item.progress = Math.round((completedCount / totalChunks) * 100)
      } catch (error) {
        if (error.message !== 'STALL_TIMEOUT') {
          throw error
        }
        // 超时重试
        currentIndex = chunkIndex
      }
    }
  }
  
  // 并发执行
  const tasks = []
  for (let i = 0; i < concurrency; i++) {
    tasks.push(uploadNext())
  }
  await Promise.all(tasks)
  
  // 合并文件
  if (item.status !== 'paused') {
    const mergeRes = await mergeFile({
      fileId: item.fileId,
      fileHash: item.fileHash
    })
    
    if (mergeRes.success) {
      item.progress = 100
      item.status = 'success'
      emit('upload-success')
      ElMessage.success(`${item.fileName} 上传成功`)
    } else {
      throw new Error(mergeRes.message || '合并失败')
    }
  }
}

// 暂停上传
function pauseUpload(item) {
  item.status = 'paused'
}

// 继续上传
function resumeUpload(item) {
  item.status = 'uploading'
  uploadChunks(item)
}

// 重试上传
function retryUpload(item) {
  item.progress = 0
  item.status = 'pending'
  item.errorMsg = ''
  startUpload(item)
}

// 移除上传
function removeUpload(item) {
  const index = uploadList.value.findIndex(i => i.id === item.id)
  if (index > -1) {
    uploadList.value.splice(index, 1)
  }
  if (uploadList.value.length === 0) {
    showPanel.value = false
  }
}

// 获取进度状态
function getProgressStatus(item) {
  if (item.status === 'success') return 'success'
  if (item.status === 'error') return 'exception'
  return ''
}

// 折叠/展开
function toggleCollapse() {
  collapsed.value = !collapsed.value
}

// 关闭面板
function closePanel() {
  // 停止所有上传
  uploadList.value.forEach(item => {
    if (item.status === 'uploading') {
      item.status = 'paused'
    }
  })
  showPanel.value = false
}

// 暴露方法
defineExpose({
  triggerUpload,
  addFiles
})
</script>

<style scoped>
.upload-panel {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 400px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 2000;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  font-weight: 500;
}

.panel-actions {
  display: flex;
  gap: 4px;
}

.panel-content {
  max-height: 300px;
  overflow-y: auto;
}

.upload-list {
  padding: 8px;
}

.upload-item {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
}

.upload-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.item-size {
  color: var(--text-secondary);
  font-size: 12px;
  margin-left: 8px;
}

.item-progress {
  margin-bottom: 4px;
}

.item-status {
  font-size: 12px;
  color: var(--text-secondary);
}

.item-status .success {
  color: var(--success-color);
}

.item-status .error {
  color: var(--danger-color);
}

.item-actions {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}

.empty-upload {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}
</style>

