<template>
  <div class="breadcrumb-wrapper">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item>
        <span class="breadcrumb-item" @click="navigateTo('/', 0)">
          <el-icon><HomeFilled /></el-icon>
          全部文件
        </span>
      </el-breadcrumb-item>
      <el-breadcrumb-item v-for="(item, index) in pathList" :key="index">
        <span 
          class="breadcrumb-item" 
          :class="{ 'is-current': index === pathList.length - 1 }"
          @click="navigateTo(item.path, item.id)"
        >
          {{ item.name }}
        </span>
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { HomeFilled } from '@element-plus/icons-vue'

const props = defineProps({
  filePath: { type: String, required: true }
})

const emit = defineEmits(['navigate'])

// 解析路径为列表
const pathList = computed(() => {
  if (!props.filePath || props.filePath === '/') {
    return []
  }
  
  const parts = props.filePath.split('/').filter(p => p)
  let currentPath = ''
  
  return parts.map((name, index) => {
    currentPath += '/' + name
    return {
      name,
      path: currentPath,
      id: index + 1 // TODO: 应该从后端获取真实的文件夹ID
    }
  })
})

// 导航到指定路径
const navigateTo = (path, folderId) => {
  emit('navigate', path, folderId)
}
</script>

<style lang="scss" scoped>
.breadcrumb-wrapper {
  padding: 12px 0;
  
  .breadcrumb-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    color: #606266;
    
    &:hover:not(.is-current) {
      color: var(--el-color-primary);
    }
    
    &.is-current {
      cursor: default;
      color: #303133;
      font-weight: 500;
    }
  }
  
  :deep(.el-breadcrumb__inner) {
    display: inline-flex;
    align-items: center;
  }
}
</style>
