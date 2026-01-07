<template>
  <el-dialog
    v-model="visible"
    title="文件详情"
    width="550px"
    :close-on-click-modal="true"
    class="file-detail-dialog"
  >
    <div class="detail-content" v-if="file">
      <!-- 文件图标/缩略图 -->
      <div class="file-preview-area">
        <img
          v-if="isImage"
          :src="thumbnailUrl"
          class="file-thumbnail"
          @click="handlePreview"
          :title="file.isDir ? '' : '点击预览'"
        />
        <img
          v-else
          :src="fileIcon"
          class="file-icon-large"
        />
      </div>
      
      <!-- 文件信息表单 -->
      <el-form
        class="file-info-form"
        label-width="82px"
        label-position="right"
        label-suffix="："
        size="small"
      >
        <el-form-item label="文件名">
          <el-input :value="fullFileName" readonly />
        </el-form-item>
        
        <el-form-item v-if="showPath" :label="isRecovery ? '原路径' : '路径'">
          <el-input :value="file.filePath || '/'" readonly />
        </el-form-item>
        
        <el-form-item label="类型">
          <el-input :value="fileTypeText" readonly />
        </el-form-item>
        
        <el-form-item label="大小" v-if="!file.isDir">
          <el-input :value="fileSizeText" readonly />
        </el-form-item>
        
        <el-form-item label="修改日期" v-if="!isRecovery && file.updateTime">
          <el-input :value="formatDate(file.updateTime)" readonly />
        </el-form-item>
        
        <el-form-item label="创建日期" v-if="file.createTime">
          <el-input :value="formatDate(file.createTime)" readonly />
        </el-form-item>
        
        <el-form-item label="删除日期" v-if="isRecovery && file.deleteTime">
          <el-input :value="formatDate(file.deleteTime)" readonly />
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <el-button @click="visible = false">关 闭</el-button>
      <el-button v-if="!file?.isDir" type="primary" @click="handleDownload">
        <el-icon><Download /></el-icon>
        下载
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { getPreviewUrl, getDownloadUrl } from '@/api/fileApi'
import { getFileIconByType } from '@/utils/fileType'

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

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  if (typeof dateStr === 'string') return dateStr
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 预览
const handlePreview = () => {
  if (props.file && !props.file.isDir) {
    emit('preview', props.file)
  }
}

// 下载
const handleDownload = () => {
  if (props.file && !props.file.isDir) {
    window.open(getDownloadUrl(props.file.id))
  }
}
</script>

<style lang="scss" scoped>
.file-detail-dialog {
  :deep(.el-dialog__body) {
    padding-top: 10px;
  }
}

.detail-content {
  .file-preview-area {
    text-align: center;
    margin-bottom: 20px;
    
    .file-thumbnail {
      max-width: 200px;
      max-height: 200px;
      border-radius: 8px;
      cursor: pointer;
      transition: transform 0.2s;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      
      &:hover {
        transform: scale(1.02);
      }
    }
    
    .file-icon-large {
      width: 80px;
      height: 80px;
    }
  }
  
  .file-info-form {
    :deep(.el-form-item) {
      margin-bottom: 12px;
      
      .el-input__inner {
        border: 1px solid #ebeef5;
        background: #fafbfc;
        font-size: 14px;
        color: #606266;
        border-radius: 4px;
      }
    }
  }
}
</style>

