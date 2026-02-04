<template>
  <teleport to="body">
    <transition name="fade">
      <div class="image-preview-wrapper" v-show="visible" @click.self="handleClose">
        <!-- 顶部工具栏 -->
        <div class="toolbar" v-if="visible">
          <!-- 左侧折叠按钮 -->
          <div class="toolbar-left">
            <el-icon
              class="fold-icon"
              :title="showThumbnails ? '折叠缩略图' : '展开缩略图'"
              @click="showThumbnails = !showThumbnails"
            >
              <DArrowLeft v-if="showThumbnails" />
              <DArrowRight v-else />
            </el-icon>
          </div>
          
          <!-- 中间文件名 -->
          <div class="toolbar-center">
            <span class="file-name" :title="currentImageName">{{ currentImageName }}</span>
            <span class="image-count">{{ activeIndex + 1 }} / {{ imageList.length }}</span>
          </div>
          
          <!-- 右侧工具按钮 -->
          <div class="toolbar-right">
            <!-- 旋转 -->
            <el-tooltip content="向左旋转" placement="bottom">
              <el-icon class="tool-btn" @click="rotateLeft"><RefreshLeft /></el-icon>
            </el-tooltip>
            <el-tooltip content="向右旋转" placement="bottom">
              <el-icon class="tool-btn" @click="rotateRight"><RefreshRight /></el-icon>
            </el-tooltip>
            
            <!-- 缩放 -->
            <el-tooltip content="放大" placement="bottom">
              <el-icon class="tool-btn" @click="zoomIn"><ZoomIn /></el-icon>
            </el-tooltip>
            <el-tooltip content="缩小" placement="bottom">
              <el-icon class="tool-btn" @click="zoomOut"><ZoomOut /></el-icon>
            </el-tooltip>
            <el-tooltip content="原始大小" placement="bottom">
              <el-icon class="tool-btn" @click="resetZoom"><FullScreen /></el-icon>
            </el-tooltip>
            
            <!-- 下载 -->
            <el-tooltip content="下载图片" placement="bottom">
              <el-icon class="tool-btn" @click="downloadImage"><Download /></el-icon>
            </el-tooltip>
            
            <!-- 操作提示 -->
            <el-tooltip placement="bottom">
              <template #content>
                <div style="line-height: 1.8">
                  1. 点击图片以外区域可退出预览<br />
                  2. 按 Escape 键可退出预览<br />
                  3. 按左、右方向键可切换图片<br />
                  4. 鼠标滚轮可放大、缩小图片<br />
                  5. 点击左上角图标可折叠缩略图
                </div>
              </template>
              <el-icon class="tool-btn"><QuestionFilled /></el-icon>
            </el-tooltip>
            
            <!-- 关闭 -->
            <el-icon class="tool-btn close-btn" @click="handleClose"><Close /></el-icon>
          </div>
        </div>
        
        <!-- 左侧缩略图列表 -->
        <div class="thumbnail-list" v-show="showThumbnails">
          <div
            v-for="(img, index) in imageList"
            :key="index"
            class="thumbnail-item"
            :class="{ active: index === activeIndex }"
            @click="activeIndex = index"
          >
            <img :src="getImageUrl(img)" :alt="getImageName(img)" />
          </div>
        </div>
        
        <!-- 主预览区域 -->
        <div
          class="preview-area"
          :class="{ 'full-width': !showThumbnails }"
          @wheel.prevent="handleWheel"
          @click.self="handleClose"
        >
          <!-- 大图 -->
          <img
            ref="mainImageRef"
            class="main-image"
            :src="currentImageUrl"
            :style="imageStyle"
            @load="handleImageLoad"
            draggable="false"
          />
          
          <!-- 左右切换按钮 -->
          <div
            class="nav-btn prev-btn"
            v-show="activeIndex > 0"
            @click.stop="prevImage"
          >
            <el-icon><ArrowLeft /></el-icon>
          </div>
          <div
            class="nav-btn next-btn"
            v-show="activeIndex < imageList.length - 1"
            @click.stop="nextImage"
          >
            <el-icon><ArrowRight /></el-icon>
          </div>
          
          <!-- 底部缩放条 -->
          <div class="zoom-bar">
            <el-slider
              v-model="zoomLevel"
              :min="10"
              :max="300"
              :format-tooltip="val => `${val}%`"
            />
            <span class="zoom-value">{{ zoomLevel }}%</span>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import {
  DArrowLeft, DArrowRight, RefreshLeft, RefreshRight,
  ZoomIn, ZoomOut, FullScreen, Download, QuestionFilled,
  Close, ArrowLeft, ArrowRight
} from '@element-plus/icons-vue'
import { getDownloadUrl } from '@/api/fileApi'
import { userStorage } from '@/utils/storage'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  imageList: { type: Array, default: () => [] },
  initialIndex: { type: Number, default: 0 }
})

const emit = defineEmits(['update:modelValue', 'close'])

// 状态
const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const activeIndex = ref(0)
const showThumbnails = ref(true)
const zoomLevel = ref(100)
const rotation = ref(0)
const mainImageRef = ref(null)

// 当前图片信息
const currentImage = computed(() => props.imageList[activeIndex.value] || {})
const currentImageName = computed(() => getImageName(currentImage.value))
const currentImageUrl = computed(() => getImageUrl(currentImage.value))

// 图片样式
const imageStyle = computed(() => ({
  transform: `scale(${zoomLevel.value / 100}) rotate(${rotation.value}deg)`,
  transition: 'transform 0.3s ease'
}))

// 获取图片名称
const getImageName = (img) => {
  if (!img) return ''
  return img.fileName || img.name || '未知图片'
}

// 获取图片URL（带token参数）
const getImageUrl = (img) => {
  if (!img) return ''
  // 如果已经有fileUrl，检查是否需要添加token
  if (img.fileUrl) {
    // 如果fileUrl已包含token参数则直接返回
    if (img.fileUrl.includes('token=')) return img.fileUrl
    // 否则添加token参数
    const token = userStorage.getToken()
    const separator = img.fileUrl.includes('?') ? '&' : '?'
    return `${img.fileUrl}${token ? `${separator}token=${encodeURIComponent(token)}` : ''}`
  }
  // 使用文件ID构建URL
  if (img.id) {
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    const token = userStorage.getToken()
    return `${baseURL}/file/preview/${img.id}${token ? `?token=${encodeURIComponent(token)}` : ''}`
  }
  return ''
}

// 图片加载完成
const handleImageLoad = () => {
  // 自动调整缩放以适应视口
  nextTick(() => {
    if (mainImageRef.value) {
      const img = mainImageRef.value
      const container = img.parentElement
      if (container) {
        const containerWidth = container.clientWidth - 100
        const containerHeight = container.clientHeight - 150
        const imgWidth = img.naturalWidth
        const imgHeight = img.naturalHeight
        
        if (imgWidth > containerWidth || imgHeight > containerHeight) {
          const widthRatio = containerWidth / imgWidth
          const heightRatio = containerHeight / imgHeight
          const ratio = Math.min(widthRatio, heightRatio, 1)
          zoomLevel.value = Math.floor(ratio * 100)
        } else {
          zoomLevel.value = 100
        }
      }
    }
  })
}

// 旋转
const rotateLeft = () => {
  rotation.value -= 90
}

const rotateRight = () => {
  rotation.value += 90
}

// 缩放
const zoomIn = () => {
  if (zoomLevel.value < 300) {
    zoomLevel.value = Math.min(300, zoomLevel.value + 20)
  }
}

const zoomOut = () => {
  if (zoomLevel.value > 10) {
    zoomLevel.value = Math.max(10, zoomLevel.value - 20)
  }
}

const resetZoom = () => {
  zoomLevel.value = 100
  rotation.value = 0
}

// 滚轮缩放
const handleWheel = (event) => {
  const delta = event.deltaY > 0 ? -10 : 10
  const newZoom = zoomLevel.value + delta
  if (newZoom >= 10 && newZoom <= 300) {
    zoomLevel.value = newZoom
  }
}

// 切换图片
const prevImage = () => {
  if (activeIndex.value > 0) {
    activeIndex.value--
    resetZoom()
  }
}

const nextImage = () => {
  if (activeIndex.value < props.imageList.length - 1) {
    activeIndex.value++
    resetZoom()
  }
}

// 下载图片
const downloadImage = () => {
  const img = currentImage.value
  if (img && img.id) {
    window.open(getDownloadUrl(img.id))
  }
}

// 关闭预览
const handleClose = () => {
  visible.value = false
  emit('close')
}

// 键盘事件
const handleKeydown = (event) => {
  if (!visible.value) return
  
  switch (event.key) {
    case 'Escape':
      handleClose()
      break
    case 'ArrowLeft':
      prevImage()
      break
    case 'ArrowRight':
      nextImage()
      break
  }
}

// 监听显示状态
watch(visible, (val) => {
  if (val) {
    activeIndex.value = props.initialIndex
    resetZoom()
    document.addEventListener('keydown', handleKeydown)
    document.body.style.overflow = 'hidden'
    
    // 保存缩略图显示状态
    const savedState = localStorage.getItem('img_preview_show_thumbnails')
    if (savedState !== null) {
      showThumbnails.value = savedState === 'true'
    }
  } else {
    document.removeEventListener('keydown', handleKeydown)
    document.body.style.overflow = ''
  }
})

// 监听缩略图状态
watch(showThumbnails, (val) => {
  localStorage.setItem('img_preview_show_thumbnails', val.toString())
})

// 监听图片切换
watch(activeIndex, () => {
  rotation.value = 0
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<style lang="scss" scoped>
.image-preview-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  flex-direction: column;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

// 顶部工具栏
.toolbar {
  height: 48px;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: #fff;
  flex-shrink: 0;
  
  .toolbar-left {
    width: 120px;
    
    .fold-icon {
      font-size: 24px;
      cursor: pointer;
      padding: 8px;
      border-radius: 4px;
      
      &:hover {
        background: rgba(255, 255, 255, 0.1);
      }
    }
  }
  
  .toolbar-center {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 16px;
    
    .file-name {
      max-width: 400px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
    }
    
    .image-count {
      color: rgba(255, 255, 255, 0.7);
      font-size: 14px;
    }
  }
  
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 4px;
    
    .tool-btn {
      font-size: 24px;
      cursor: pointer;
      padding: 10px;
      border-radius: 6px;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 44px;
      min-height: 44px;
      
      &:hover {
        background: rgba(255, 255, 255, 0.15);
        transform: scale(1.05);
      }
      
      &:active {
        transform: scale(0.95);
      }
      
      &.close-btn {
        margin-left: 12px;
        
        &:hover {
          background: rgba(255, 0, 0, 0.4);
        }
      }
    }
  }
}

// 缩略图列表
.thumbnail-list {
  position: fixed;
  top: 48px;
  left: 0;
  width: 120px;
  height: calc(100vh - 48px);
  background: rgba(0, 0, 0, 0.6);
  overflow-y: auto;
  padding: 8px;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.3);
    border-radius: 3px;
  }
  
  .thumbnail-item {
    width: 100%;
    aspect-ratio: 1;
    margin-bottom: 8px;
    cursor: pointer;
    position: relative;
    border-radius: 4px;
    overflow: hidden;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    &:not(.active)::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.4);
      transition: background 0.2s;
    }
    
    &:hover:not(.active)::after {
      background: rgba(0, 0, 0, 0.2);
    }
    
    &.active {
      box-shadow: 0 0 0 2px var(--el-color-primary);
    }
  }
}

// 主预览区域
.preview-area {
  position: fixed;
  top: 48px;
  left: 120px;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  
  &.full-width {
    left: 0;
  }
  
  .main-image {
    max-width: 90%;
    max-height: 80%;
    object-fit: contain;
    user-select: none;
  }
  
  // 导航按钮
  .nav-btn {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 60px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.4);
    color: #fff;
    font-size: 48px;
    cursor: pointer;
    transition: all 0.2s;
    border-radius: 8px;
    
    &:hover {
      background: rgba(0, 0, 0, 0.7);
      transform: translateY(-50%) scale(1.05);
    }
    
    &:active {
      transform: translateY(-50%) scale(0.95);
    }
    
    &.prev-btn {
      left: 30px;
    }
    
    &.next-btn {
      right: 30px;
    }
  }
  
  // 缩放条
  .zoom-bar {
    position: absolute;
    bottom: 30px;
    left: 50%;
    transform: translateX(-50%);
    width: 400px;
    display: flex;
    align-items: center;
    gap: 16px;
    background: rgba(0, 0, 0, 0.5);
    padding: 12px 20px;
    border-radius: 8px;
    
    :deep(.el-slider) {
      flex: 1;
      
      .el-slider__runway {
        background: rgba(255, 255, 255, 0.3);
      }
      
      .el-slider__bar {
        background: var(--el-color-primary);
      }
      
      .el-slider__button {
        border-color: var(--el-color-primary);
      }
    }
    
    .zoom-value {
      color: #fff;
      font-size: 14px;
      width: 50px;
      text-align: right;
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .thumbnail-list {
    width: 100px;
  }
  
  .preview-area {
    left: 100px;
    
    &.full-width {
      left: 0;
    }
    
    .nav-btn {
      width: 50px;
      height: 80px;
      font-size: 36px;
      
      &.prev-btn {
        left: 20px;
      }
      
      &.next-btn {
        right: 20px;
      }
    }
    
    .zoom-bar {
      width: 320px;
      padding: 10px 16px;
    }
  }
  
  .toolbar {
    .toolbar-center {
      .file-name {
        max-width: 250px;
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .thumbnail-list {
    display: none;
  }
  
  .preview-area {
    left: 0 !important;
    
    .main-image {
      max-width: 95%;
      max-height: 70%;
    }
    
    .nav-btn {
      width: 40px;
      height: 60px;
      font-size: 28px;
      background: rgba(0, 0, 0, 0.5);
      
      &.prev-btn {
        left: 10px;
      }
      
      &.next-btn {
        right: 10px;
      }
    }
    
    .zoom-bar {
      width: calc(100% - 32px);
      bottom: 20px;
      padding: 8px 12px;
      gap: 10px;
      
      .zoom-value {
        font-size: 12px;
        width: 40px;
      }
    }
  }
  
  .toolbar {
    height: 44px;
    padding: 0 12px;
    
    .toolbar-left {
      width: auto;
      
      .fold-icon {
        display: none;
      }
    }
    
    .toolbar-center {
      .file-name {
        max-width: 150px;
        font-size: 13px;
      }
      
      .image-count {
        font-size: 12px;
      }
    }
    
    .toolbar-right {
      gap: 2px;
      
      .tool-btn {
        font-size: 20px;
        padding: 8px;
        min-width: 36px;
        min-height: 36px;
        
        &.close-btn {
          margin-left: 8px;
        }
      }
    }
  }
}
</style>

