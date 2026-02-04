<template>
  <el-dialog
    v-model="visible"
    title="文件详情"
    :width="dialogWidth"
    :close-on-click-modal="true"
    class="file-detail-dialog"
    :fullscreen="isMobile"
  >
    <!-- 有文件数据时显示 -->
    <div class="detail-content" v-if="file">
      <!-- 文件图标/缩略图 -->
      <div class="file-preview-area">
        <img
          v-if="isImage"
          :src="thumbnailUrl"
          class="file-thumbnail"
          @click="handlePreview"
          title="点击预览"
        />
        <div v-else class="file-icon-wrapper">
          <img :src="fileIcon" class="file-icon-large" />
        </div>
      </div>
      
      <!-- 文件信息列表 -->
      <div class="file-info-list">
        <div class="info-item">
          <span class="label">文件名</span>
          <span class="value">{{ fullFileName }}</span>
        </div>
        
        <div class="info-item" v-if="showPath">
          <span class="label">{{ isRecovery ? '原路径' : '路径' }}</span>
          <span class="value">{{ file.filePath || '/' }}</span>
        </div>
        
        <div class="info-item">
          <span class="label">类型</span>
          <span class="value">{{ fileTypeText }}</span>
        </div>
        
        <div class="info-item" v-if="!file.isDir">
          <span class="label">大小</span>
          <span class="value">{{ fileSizeText }}</span>
        </div>
        
        <div class="info-item" v-if="!isRecovery && file.updateTime">
          <span class="label">修改日期</span>
          <span class="value">{{ formatDate(file.updateTime) }}</span>
        </div>
        
        <div class="info-item" v-if="file.createTime">
          <span class="label">创建日期</span>
          <span class="value">{{ formatDate(file.createTime) }}</span>
        </div>
        
        <div class="info-item" v-if="isRecovery && file.deleteTime">
          <span class="label">删除日期</span>
          <span class="value">{{ formatDate(file.deleteTime) }}</span>
        </div>
      </div>
    </div>
    
    <!-- 无文件数据时显示 -->
    <div class="empty-content" v-else>
      <el-empty description="暂无文件信息" :image-size="80" />
    </div>
    
    <template #footer>
      <el-button @click="visible = false">关 闭</el-button>
      <el-button v-if="file && !file.isDir" type="primary" @click="handleDownload">
        <el-icon><Download /></el-icon>
        下载
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPreviewUrl, getDownloadUrl } from '@/api/fileApi'
import { getFileIconByType } from '@/utils/fileType'
import { useTransferStore } from '@/store/transferStore'

const router = useRouter()
const transferStore = useTransferStore()

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  file: { type: Object, default: null },
  fileType: { type: Number, default: 0 } // 文件类型，6为回收站
})

const emit = defineEmits(['update:modelValue', 'preview'])

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

// 响应式
const screenWidth = ref(window.innerWidth)
const updateScreenWidth = () => { screenWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', updateScreenWidth))
onUnmounted(() => window.removeEventListener('resize', updateScreenWidth))
const isMobile = computed(() => screenWidth.value < 768)
const dialogWidth = computed(() => screenWidth.value < 768 ? '100%' : '500px')

// 是否是回收站
const isRecovery = computed(() => props.fileType === 6)

// 是否显示路径
const showPath = computed(() => ![0, 8].includes(props.fileType))

// 完整文件名
const fullFileName = computed(() => {
  if (!props.file) return ''
  const name = props.file.fileName || props.file.folderName || ''
  const ext = props.file.extendName
  if (props.file.isDir || !ext) return name
  return name.includes('.') ? name : `${name}.${ext}`
})

// 是否是图片
const isImage = computed(() => {
  if (!props.file || props.file.isDir) return false
  const ext = (props.file.extendName || '').toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(ext)
})

// 缩略图 URL
const thumbnailUrl = computed(() => {
  if (!props.file || !isImage.value) return ''
  return getPreviewUrl(props.file.id)
})

// 文件图标
const fileIcon = computed(() => {
  if (!props.file) return ''
  if (props.file.isDir) {
    return '/icons/folder.svg'
  }
  return getFileIconByType(props.file.extendName)
})

// 文件类型文本
const fileTypeText = computed(() => {
  if (!props.file) return ''
  if (props.file.isDir) return '文件夹'
  
  const ext = props.file.extendName
  if (!ext) return '未知'
  
  const typeMap = {
    // 图片
    jpg: 'JPEG 图片', jpeg: 'JPEG 图片', png: 'PNG 图片', gif: 'GIF 图片',
    bmp: 'BMP 图片', webp: 'WebP 图片', svg: 'SVG 图片',
    // 文档
    doc: 'Word 文档', docx: 'Word 文档', xls: 'Excel 表格', xlsx: 'Excel 表格',
    ppt: 'PPT 演示文稿', pptx: 'PPT 演示文稿', pdf: 'PDF 文档', txt: '文本文件',
    // 视频
    mp4: 'MP4 视频', avi: 'AVI 视频', mov: 'MOV 视频', mkv: 'MKV 视频', webm: 'WebM 视频',
    // 音频
    mp3: 'MP3 音频', wav: 'WAV 音频', flac: 'FLAC 音频', ogg: 'OGG 音频',
    // 压缩包
    zip: 'ZIP 压缩包', rar: 'RAR 压缩包', '7z': '7z 压缩包', tar: 'TAR 归档',
    // 代码
    js: 'JavaScript', ts: 'TypeScript', vue: 'Vue 组件', java: 'Java 源码',
    py: 'Python 脚本', go: 'Go 源码', c: 'C 源码', cpp: 'C++ 源码',
    html: 'HTML 文件', css: 'CSS 样式', json: 'JSON 文件', xml: 'XML 文件',
    md: 'Markdown 文件', sql: 'SQL 脚本'
  }
  
  return typeMap[ext.toLowerCase()] || `${ext.toUpperCase()} 文件`
})

// 文件大小文本
const fileSizeText = computed(() => {
  if (!props.file || props.file.isDir) return ''
  const size = props.file.fileSize
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
})

// 格式化日期为 年-月-日 时:分:秒
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 预览
const handlePreview = () => {
  if (props.file && !props.file.isDir) {
    emit('preview', props.file)
  }
}

// 下载 - 添加到传输列表
const handleDownload = () => {
  if (props.file && !props.file.isDir) {
    transferStore.addDownloadTask({
      fileId: props.file.id,
      fileName: fullFileName.value,
      fileSize: props.file.fileSize,
      fileHash: props.file.fileHash
    })
    ElMessage.success(`已添加 "${fullFileName.value}" 到下载列表`)
    router.push({ name: 'TransferCenter' })
  }
}
</script>

<style lang="scss" scoped>
.detail-content {
  .file-preview-area {
    text-align: center;
    padding: 20px;
    margin-bottom: 16px;
    background: var(--art-fill-lighter);
    border-radius: 8px;
    
    .file-thumbnail {
      max-width: 180px;
      max-height: 180px;
      border-radius: 8px;
      cursor: pointer;
      transition: transform 0.2s;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      
      &:hover {
        transform: scale(1.02);
      }
    }
    
    .file-icon-wrapper {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 100px;
      height: 100px;
      background: var(--art-surface);
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }
    
    .file-icon-large {
      width: 64px;
      height: 64px;
    }
  }
  
  .file-info-list {
    .info-item {
      display: flex;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .label {
        width: 80px;
        flex-shrink: 0;
        color: #909399;
        font-size: 14px;
      }
      
      .value {
        flex: 1;
        color: #303133;
        font-size: 14px;
        word-break: break-all;
      }
    }
  }
}

.empty-content {
  padding: 40px 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .detail-content {
    .file-preview-area {
      padding: 16px;
      
      .file-thumbnail {
        max-width: 140px;
        max-height: 140px;
      }
      
      .file-icon-wrapper {
        width: 80px;
        height: 80px;
      }
      
      .file-icon-large {
        width: 48px;
        height: 48px;
      }
    }
    
    .file-info-list {
      .info-item {
        flex-direction: column;
        gap: 4px;
        padding: 10px 0;
        
        .label {
          width: auto;
          font-size: 12px;
        }
        
        .value {
          font-size: 13px;
        }
      }
    }
  }
  
  .empty-content {
    padding: 20px 0;
  }
}
</style>


