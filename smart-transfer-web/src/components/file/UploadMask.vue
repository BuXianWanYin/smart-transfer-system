<template>
  <teleport to="body">
    <transition name="fade">
      <div
        class="upload-mask"
        v-show="visible"
        @dragover.prevent="handleDragOver"
        @dragleave.prevent="handleDragLeave"
        @drop.prevent="handleDrop"
        @paste="handlePaste"
      >
        <div class="mask-content">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <p class="main-text">将文件拖拽至此处上传</p>
          <p class="sub-text">支持截图粘贴上传</p>
          
          <!-- 粘贴图片预览 -->
          <div class="paste-preview" v-if="pasteImage">
            <img :src="pasteImage.preview" :alt="pasteImage.name" />
            <p>{{ pasteImage.name }}</p>
            <div class="paste-actions">
              <el-button type="primary" @click="uploadPasteImage">
                <el-icon><Upload /></el-icon>
                上传图片
              </el-button>
              <el-button @click="clearPasteImage">
                <el-icon><Delete /></el-icon>
                取消
              </el-button>
            </div>
          </div>
        </div>
        
        <el-icon class="close-btn" @click="handleClose"><CircleClose /></el-icon>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled, Upload, Delete, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'upload', 'close'])

const visible = ref(false)
const pasteImage = ref(null)

// 显示遮罩
const show = () => {
  visible.value = true
  pasteImage.value = null
}

// 隐藏遮罩
const hide = () => {
  visible.value = false
  pasteImage.value = null
}

// 拖拽相关
const handleDragOver = (event) => {
  event.dataTransfer.dropEffect = 'copy'
}

const handleDragLeave = (event) => {
  // 只有离开最外层才关闭
  if (event.target === event.currentTarget) {
    hide()
  }
}

const handleDrop = (event) => {
  const files = Array.from(event.dataTransfer.files)
  if (files.length > 0) {
    emit('upload', files)
    hide()
  }
}

// 粘贴图片
const handlePaste = (event) => {
  const items = event.clipboardData?.items
  if (!items) return
  
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        const timestamp = new Date().valueOf()
        const ext = file.type.split('/')[1] || 'png'
        const renamedFile = new File([file], `paste_${timestamp}.${ext}`, { type: file.type })
        
        const reader = new FileReader()
        reader.onload = (e) => {
          pasteImage.value = {
            file: renamedFile,
            name: renamedFile.name,
            preview: e.target.result
          }
        }
        reader.readAsDataURL(file)
        break
      }
    }
  }
}

// 上传粘贴的图片
const uploadPasteImage = () => {
  if (pasteImage.value) {
    emit('upload', [pasteImage.value.file])
    hide()
  }
}

// 清除粘贴的图片
const clearPasteImage = () => {
  pasteImage.value = null
}

// 关闭
const handleClose = () => {
  hide()
  emit('close')
}

// 暴露方法
defineExpose({ show, hide })
</script>

<style lang="scss" scoped>
.upload-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1999;
  background: rgba(255, 255, 255, 0.95);
  border: 5px dashed #8091a5;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.mask-content {
  text-align: center;
  color: #8091a5;
  
  .upload-icon {
    font-size: 80px;
    margin-bottom: 20px;
  }
  
  .main-text {
    font-size: 24px;
    margin-bottom: 8px;
  }
  
  .sub-text {
    font-size: 14px;
    color: #a0a0a0;
  }
}

.paste-preview {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  
  img {
    max-width: 300px;
    max-height: 200px;
    border-radius: 4px;
    margin-bottom: 10px;
  }
  
  p {
    margin-bottom: 16px;
    color: #606266;
  }
  
  .paste-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 32px;
  color: #8091a5;
  cursor: pointer;
  transition: color 0.2s;
  
  &:hover {
    color: #409eff;
  }
}
</style>

