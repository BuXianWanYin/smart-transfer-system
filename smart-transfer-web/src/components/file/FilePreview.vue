<template>
  <el-dialog
    v-model="visible"
    :title="file?.fileName || '文件预览'"
    :width="dialogWidth"
    :close-on-click-modal="true"
    class="file-preview-dialog"
    :fullscreen="isMobile"
  >
    <div class="preview-container" v-loading="loading">
      <!-- 图片预览 -->
      <template v-if="fileType === 'image'">
        <img :src="previewUrl" class="preview-image" @load="loading = false" />
      </template>
      
      <!-- 视频预览 -->
      <template v-else-if="fileType === 'video'">
        <video 
          :src="previewUrl" 
          class="preview-video" 
          controls 
          autoplay
          @loadeddata="loading = false"
        />
      </template>
      
      <!-- 音频预览 -->
      <template v-else-if="fileType === 'audio'">
        <div class="audio-preview">
          <el-icon class="audio-icon"><Headset /></el-icon>
          <audio :src="previewUrl" controls @loadeddata="loading = false" />
        </div>
      </template>
      
      <!-- PDF预览 -->
      <template v-else-if="fileType === 'pdf'">
        <iframe :src="previewUrl" class="preview-pdf" @load="loading = false" />
      </template>
      
      <!-- 文本预览 -->
      <template v-else-if="fileType === 'text'">
        <div class="preview-text">
          <pre>{{ textContent }}</pre>
        </div>
      </template>
      
      <!-- 不支持预览 -->
      <template v-else>
        <div class="no-preview">
          <el-icon class="no-preview-icon"><Document /></el-icon>
          <p>该文件类型暂不支持预览</p>
          <el-button type="primary" @click="downloadFile">
            <el-icon><Download /></el-icon>
            下载文件
          </el-button>
        </div>
      </template>
    </div>
    
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="downloadFile">
        <el-icon><Download /></el-icon>
        下载
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Headset, Document, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPreviewUrl, getDownloadUrl } from '@/api/fileApi'
import { useTransferStore } from '@/store/transferStore'

const router = useRouter()
const transferStore = useTransferStore()

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  file: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 响应式
const screenWidth = ref(window.innerWidth)
const updateScreenWidth = () => { screenWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', updateScreenWidth))
onUnmounted(() => window.removeEventListener('resize', updateScreenWidth))

const isMobile = computed(() => screenWidth.value < 768)
const dialogWidth = computed(() => {
  if (screenWidth.value < 768) return '100%'
  if (screenWidth.value < 1024) return '90%'
  return '80%'
})

const loading = ref(true)
const textContent = ref('')

// 预览URL
const previewUrl = computed(() => {
  if (!props.file) return ''
  return getPreviewUrl(props.file.id)
})

// 文件类型
const fileType = computed(() => {
  if (!props.file) return 'other'
  
  const ext = (props.file.extendName || '').toLowerCase()
  
  // 图片
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(ext)) {
    return 'image'
  }
  
  // 视频
  if (['mp4', 'webm', 'ogg', 'avi', 'mov'].includes(ext)) {
    return 'video'
  }
  
  // 音频
  if (['mp3', 'wav', 'ogg', 'flac', 'm4a'].includes(ext)) {
    return 'audio'
  }
  
  // PDF
  if (ext === 'pdf') {
    return 'pdf'
  }
  
  // 文本
  if (['txt', 'md', 'json', 'xml', 'html', 'css', 'js', 'ts', 'vue', 'java', 'py', 'sql'].includes(ext)) {
    return 'text'
  }
  
  return 'other'
})

// 加载文本内容
const loadTextContent = async () => {
  if (fileType.value !== 'text' || !props.file) return
  
  loading.value = true
  try {
    const response = await fetch(previewUrl.value)
    textContent.value = await response.text()
  } catch (error) {
    textContent.value = '加载失败'
  } finally {
    loading.value = false
  }
}

// 下载文件 - 添加到传输列表
const downloadFile = () => {
  if (!props.file) return
  const fileName = props.file.fileName + (props.file.extendName ? '.' + props.file.extendName : '')
  transferStore.addDownloadTask({
    fileId: props.file.id,
    fileName: fileName,
    fileSize: props.file.fileSize,
    fileHash: props.file.fileHash
  })
  ElMessage.success(`已添加 "${fileName}" 到下载列表`)
  router.push({ name: 'TransferCenter' })
}

// 监听文件变化
watch(() => props.file, (newFile) => {
  if (newFile) {
    loading.value = true
    if (fileType.value === 'text') {
      loadTextContent()
    }
  }
}, { immediate: true })
</script>

<style lang="scss" scoped>
.file-preview-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.preview-container {
  min-height: 400px;
  max-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--art-fill-lighter);
  
  .preview-image {
    max-width: 100%;
    max-height: 70vh;
    object-fit: contain;
  }
  
  .preview-video {
    max-width: 100%;
    max-height: 70vh;
  }
  
  .audio-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
    
    .audio-icon {
      font-size: 80px;
      color: var(--el-color-primary);
    }
    
    audio {
      width: 400px;
    }
  }
  
  .preview-pdf {
    width: 100%;
    height: 70vh;
    border: none;
  }
  
  .preview-text {
    width: 100%;
    height: 70vh;
    overflow: auto;
    padding: 16px;
    background: var(--art-surface);
    
    pre {
      margin: 0;
      white-space: pre-wrap;
      word-wrap: break-word;
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 14px;
      line-height: 1.5;
    }
  }
  
  .no-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    color: #909399;
    
    .no-preview-icon {
      font-size: 80px;
    }
    
    p {
      font-size: 16px;
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .preview-container {
    min-height: 300px;
    
    .audio-preview {
      audio {
        width: 300px;
      }
    }
    
    .preview-text {
      padding: 12px;
      
      pre {
        font-size: 13px;
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .preview-container {
    min-height: 200px;
    max-height: calc(100vh - 120px);
    
    .preview-image {
      max-height: calc(100vh - 120px);
    }
    
    .preview-video {
      max-height: calc(100vh - 120px);
    }
    
    .audio-preview {
      gap: 16px;
      padding: 20px;
      
      .audio-icon {
        font-size: 60px;
      }
      
      audio {
        width: 100%;
        max-width: 280px;
      }
    }
    
    .preview-pdf {
      height: calc(100vh - 120px);
    }
    
    .preview-text {
      height: calc(100vh - 120px);
      padding: 10px;
      
      pre {
        font-size: 12px;
      }
    }
    
    .no-preview {
      gap: 12px;
      padding: 20px;
      
      .no-preview-icon {
        font-size: 60px;
      }
      
      p {
        font-size: 14px;
      }
    }
  }
}
</style>
