<template>
  <div class="thumbnail-wrapper" :class="{ loading: isLoading, error: hasError }">
    <!-- 加载中 -->
    <div v-if="isLoading" class="thumbnail-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
    </div>
    
    <!-- 加载失败，显示文件图标 -->
    <img 
      v-else-if="hasError" 
      :src="fallbackIcon" 
      class="thumbnail-fallback"
      :style="{ width: size + 'px', height: size + 'px' }"
    />
    
    <!-- 加载成功，显示缩略图 -->
    <img 
      v-else 
      :src="blobUrl" 
      class="thumbnail-image"
      :style="{ width: size + 'px', height: size + 'px' }"
      @load="onImageLoad"
      @error="onImageError"
    />
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { fetchPreviewBlob, revokePreviewBlob } from '@/api/fileApi'
import { getFileIconByType } from '@/utils/fileType'

const props = defineProps({
  /** 文件ID */
  fileId: {
    type: [Number, String],
    required: true
  },
  /** 文件扩展名，用于显示回退图标 */
  extendName: {
    type: String,
    default: ''
  },
  /** 缩略图尺寸 */
  size: {
    type: Number,
    default: 80
  }
})

const blobUrl = ref('')
const isLoading = ref(true)
const hasError = ref(false)

// 获取回退图标
const fallbackIcon = getFileIconByType(props.extendName || 'default')

// 加载缩略图
const loadThumbnail = async () => {
  if (!props.fileId) return
  
  isLoading.value = true
  hasError.value = false
  
  // 释放旧的Blob URL
  if (blobUrl.value) {
    revokePreviewBlob(blobUrl.value)
    blobUrl.value = ''
  }
  
  try {
    const url = await fetchPreviewBlob(props.fileId)
    blobUrl.value = url
    // 获取到Blob URL后立即设为非加载状态，让img开始渲染
    isLoading.value = false
  } catch (error) {
    console.error('加载缩略图失败:', error)
    hasError.value = true
    isLoading.value = false
  }
}

// 图片加载完成
const onImageLoad = () => {
  isLoading.value = false
}

// 图片加载失败
const onImageError = () => {
  hasError.value = true
  isLoading.value = false
}

// 监听fileId变化
watch(() => props.fileId, (newId) => {
  if (newId) {
    loadThumbnail()
  }
}, { immediate: true })

// 组件销毁时释放Blob URL
onUnmounted(() => {
  if (blobUrl.value) {
    revokePreviewBlob(blobUrl.value)
  }
})
</script>

<style lang="scss" scoped>
.thumbnail-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  
  &.loading {
    .loading-icon {
      animation: rotate 1s linear infinite;
      color: var(--el-color-primary);
      font-size: 24px;
    }
  }
}

.thumbnail-image,
.thumbnail-fallback {
  object-fit: cover;
  border-radius: 4px;
}

.thumbnail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
