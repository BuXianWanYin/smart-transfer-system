<template>
  <teleport to="body">
    <transition name="fade">
      <div
        class="upload-mask"
        v-show="visible"
        @dragover.prevent="handleDragOver"
        @dragleave.prevent="handleDragLeave"
        @drop.prevent="handleDrop"
      >
        <div class="mask-content">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <p class="main-text">将文件拖拽至此处上传</p>
          <p class="sub-text">支持大文件分片上传</p>
        </div>
        
        <el-icon class="close-btn" @click="handleClose"><CircleClose /></el-icon>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled, CircleClose } from '@element-plus/icons-vue'

defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'upload', 'close'])

const visible = ref(false)

// 显示遮罩
const show = () => {
  visible.value = true
}

// 隐藏遮罩
const hide = () => {
  visible.value = false
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

