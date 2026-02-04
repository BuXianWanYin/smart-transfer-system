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
      
      <!-- Word文档预览 -->
      <template v-else-if="fileType === 'word'">
        <div ref="docxContainer" class="preview-office preview-docx"></div>
      </template>
      
      <!-- Excel表格预览 -->
      <template v-else-if="fileType === 'excel'">
        <div ref="xlsxContainer" class="preview-office preview-xlsx" v-html="xlsxHtml"></div>
      </template>
      
      <!-- PPT演示文稿预览 -->
      <template v-else-if="fileType === 'ppt'">
        <div ref="pptxContainer" class="preview-office preview-pptx"></div>
      </template>
      
      <!-- 旧格式Office文档提示 -->
      <template v-else-if="fileType === 'office-old'">
        <div class="no-preview">
          <el-icon class="no-preview-icon"><Document /></el-icon>
          <p>旧格式Office文档暂不支持在线预览</p>
          <p class="sub-text">支持预览的格式：docx、xlsx、pptx</p>
          <el-button type="primary" @click="downloadFile">
            <el-icon><Download /></el-icon>
            下载文件查看
          </el-button>
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
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Headset, Document, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchPreviewBlob, revokePreviewBlob, getDownloadUrl } from '@/api/fileApi'
import { useTransferStore } from '@/store/transferStore'
import { userStorage } from '@/utils/storage'
import { renderAsync as renderDocx } from 'docx-preview'
import { xlsx2Html } from 'xlsx-preview'

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
onUnmounted(() => {
  window.removeEventListener('resize', updateScreenWidth)
  // 组件销毁时释放Blob URL
  if (previewUrl.value) {
    revokePreviewBlob(previewUrl.value)
  }
})

const isMobile = computed(() => screenWidth.value < 768)
const dialogWidth = computed(() => {
  if (screenWidth.value < 768) return '100%'
  if (screenWidth.value < 1024) return '90%'
  return '80%'
})

const loading = ref(true)
const textContent = ref('')
// 使用ref存储Blob URL
const previewUrl = ref('')

// Office文档预览容器引用
const docxContainer = ref(null)
const xlsxContainer = ref(null)
const pptxContainer = ref(null)
// Excel HTML内容
const xlsxHtml = ref('')

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
  
  // Word文档（仅支持docx格式）
  if (ext === 'docx') {
    return 'word'
  }
  
  // Excel表格（仅支持xlsx格式）
  if (ext === 'xlsx') {
    return 'excel'
  }
  
  // PPT演示文稿（仅支持pptx格式）
  if (ext === 'pptx') {
    return 'ppt'
  }
  
  // 旧格式Office文档（不支持预览）
  if (['doc', 'xls', 'ppt'].includes(ext)) {
    return 'office-old'
  }
  
  return 'other'
})

// 加载预览内容（通过Blob URL）
const loadPreviewContent = async () => {
  if (!props.file) return
  
  loading.value = true
  
  // 释放旧的Blob URL
  if (previewUrl.value) {
    revokePreviewBlob(previewUrl.value)
    previewUrl.value = ''
  }
  
  // 清空Office预览内容
  xlsxHtml.value = ''
  
  try {
    // 通过axios获取文件并创建Blob URL
    const blobUrl = await fetchPreviewBlob(props.file.id)
    previewUrl.value = blobUrl
    
    // 如果是文本文件，还需要读取文本内容
    if (fileType.value === 'text') {
      const response = await fetch(blobUrl)
      textContent.value = await response.text()
      loading.value = false
    }
    // Word文档预览
    else if (fileType.value === 'word') {
      await renderDocxFile(blobUrl)
    }
    // Excel表格预览
    else if (fileType.value === 'excel') {
      await renderXlsxFile(blobUrl)
    }
    // PPT演示文稿预览
    else if (fileType.value === 'ppt') {
      await renderPptxFile(blobUrl)
    }
    // 图片、视频、音频会在onload事件中设置loading为false
  } catch (error) {
    console.error('加载预览失败:', error)
    if (fileType.value === 'text') {
      textContent.value = '加载失败'
    }
    loading.value = false
  }
}

/**
 * 渲染Word文档
 */
const renderDocxFile = async (blobUrl) => {
  try {
    const response = await fetch(blobUrl)
    const arrayBuffer = await response.arrayBuffer()
    
    await nextTick()
    if (docxContainer.value) {
      // 清空容器
      docxContainer.value.innerHTML = ''
      // 使用docx-preview渲染，需要提供正确的容器参数
      await renderDocx(arrayBuffer, docxContainer.value, null, {
        className: 'docx-preview-wrapper',
        inWrapper: true,
        ignoreWidth: false,
        ignoreHeight: false,
        ignoreFonts: false,
        breakPages: true,
        renderHeaders: true,
        renderFooters: true,
        renderFootnotes: true,
        renderEndnotes: true
      })
    }
    loading.value = false
  } catch (error) {
    console.error('Word文档渲染失败:', error)
    ElMessage.error('Word文档预览失败，请下载后查看')
    loading.value = false
  }
}

/**
 * 渲染Excel表格
 */
const renderXlsxFile = async (blobUrl) => {
  try {
    const response = await fetch(blobUrl)
    const arrayBuffer = await response.arrayBuffer()
    
    // 使用xlsx-preview渲染为HTML
    const htmlResult = await xlsx2Html(arrayBuffer, {
      output: 'string',
      minColCount: 10,
      minRowCount: 20
    })
    
    xlsxHtml.value = htmlResult
    loading.value = false
  } catch (error) {
    console.error('Excel表格渲染失败:', error)
    ElMessage.error('Excel表格预览失败，请下载后查看')
    loading.value = false
  }
}

/**
 * 渲染PPT演示文稿
 */
const renderPptxFile = async (blobUrl) => {
  try {
    const response = await fetch(blobUrl)
    const arrayBuffer = await response.arrayBuffer()
    
    await nextTick()
    if (pptxContainer.value) {
      // 清空容器
      pptxContainer.value.innerHTML = ''
      
      // 动态导入pptx-preview（该库可能没有默认导出）
      const pptxModule = await import('pptx-preview')
      const renderPptx = pptxModule.default || pptxModule.render || pptxModule.pptx2Html
      
      if (typeof renderPptx === 'function') {
        await renderPptx(arrayBuffer, pptxContainer.value)
      } else {
        // 如果库不支持直接渲染，显示提示
        pptxContainer.value.innerHTML = `
          <div class="pptx-fallback">
            <p>PPT预览功能加载中...</p>
            <p class="sub-text">如果长时间无法加载，请下载后查看</p>
          </div>
        `
      }
    }
    loading.value = false
  } catch (error) {
    console.error('PPT演示文稿渲染失败:', error)
    ElMessage.error('PPT预览失败，请下载后查看')
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
    loadPreviewContent()
  }
}, { immediate: true })

// 监听对话框关闭，释放Blob URL
watch(visible, (newVal) => {
  if (!newVal && previewUrl.value) {
    revokePreviewBlob(previewUrl.value)
    previewUrl.value = ''
  }
})
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
      margin: 0;
    }
    
    .sub-text {
      font-size: 13px;
      color: #b0b3b8;
    }
  }
  
  // Office文档预览通用样式
  .preview-office {
    width: 100%;
    height: 70vh;
    overflow: auto;
    background: #fff;
    
    // Word文档预览样式
    &.preview-docx {
      padding: 0;
      
      :deep(.docx-preview-wrapper) {
        padding: 20px;
      }
      
      :deep(.docx-wrapper) {
        background: #fff;
        padding: 20px;
        
        section.docx {
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          margin-bottom: 20px;
          padding: 40px 60px;
          background: #fff;
        }
      }
    }
    
    // Excel表格预览样式
    &.preview-xlsx {
      padding: 16px;
      
      :deep(table) {
        border-collapse: collapse;
        width: 100%;
        font-size: 13px;
        
        th, td {
          border: 1px solid #e4e7ed;
          padding: 8px 12px;
          text-align: left;
          white-space: nowrap;
        }
        
        th {
          background: #f5f7fa;
          font-weight: 600;
          color: #303133;
        }
        
        tr:nth-child(even) {
          background: #fafafa;
        }
        
        tr:hover {
          background: #ecf5ff;
        }
      }
    }
    
    // PPT预览样式
    &.preview-pptx {
      padding: 20px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
      
      :deep(.slide) {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        margin-bottom: 20px;
        max-width: 100%;
      }
      
      .pptx-fallback {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: #909399;
        
        p {
          margin: 8px 0;
        }
        
        .sub-text {
          font-size: 13px;
          color: #b0b3b8;
        }
      }
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
    
    .preview-office {
      &.preview-docx {
        :deep(.docx-wrapper section.docx) {
          padding: 30px 40px;
        }
      }
      
      &.preview-xlsx {
        padding: 12px;
        
        :deep(table) {
          font-size: 12px;
          
          th, td {
            padding: 6px 10px;
          }
        }
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
    
    .preview-office {
      height: calc(100vh - 120px);
      
      &.preview-docx {
        :deep(.docx-wrapper section.docx) {
          padding: 20px;
        }
      }
      
      &.preview-xlsx {
        padding: 8px;
        
        :deep(table) {
          font-size: 11px;
          
          th, td {
            padding: 4px 6px;
          }
        }
      }
      
      &.preview-pptx {
        padding: 10px;
        gap: 10px;
      }
    }
  }
}
</style>
