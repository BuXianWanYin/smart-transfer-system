<template>
  <div class="aside-menu-wrapper">
    <el-menu
      :default-active="activeIndex"
      :default-openeds="['files']"
      class="file-menu"
      @select="handleMenuSelect"
    >
      <!-- 传输中心 -->
      <el-menu-item index="transfer">
        <el-icon><Upload /></el-icon>
        <span>传输中心</span>
      </el-menu-item>
      
      <!-- 我的文件 -->
      <el-sub-menu index="files">
        <template #title>
          <el-icon><Files /></el-icon>
          <span>我的文件</span>
        </template>
        
        <el-menu-item index="file-0">
          <el-icon><FolderOpened /></el-icon>
          <span>全部</span>
        </el-menu-item>
        
        <el-menu-item index="file-1">
          <el-icon><Picture /></el-icon>
          <span>图片</span>
        </el-menu-item>
        
        <el-menu-item index="file-2">
          <el-icon><Document /></el-icon>
          <span>文档</span>
        </el-menu-item>
        
        <el-menu-item index="file-3">
          <el-icon><VideoPlay /></el-icon>
          <span>视频</span>
        </el-menu-item>
        
        <el-menu-item index="file-4">
          <el-icon><Headset /></el-icon>
          <span>音乐</span>
        </el-menu-item>
        
        <el-menu-item index="file-5">
          <el-icon><MoreFilled /></el-icon>
          <span>其他</span>
        </el-menu-item>
      </el-sub-menu>
      
      <!-- 回收站 -->
      <el-menu-item index="file-6">
        <el-icon><Delete /></el-icon>
        <span>回收站</span>
      </el-menu-item>
      
      <!-- 系统配置 -->
      <el-menu-item index="config">
        <el-icon><Setting /></el-icon>
        <span>系统配置</span>
      </el-menu-item>
    </el-menu>
    
    <!-- 存储空间 -->
    <div class="storage-info">
      <div class="storage-header">
        <el-icon><Coin /></el-icon>
        <span>存储空间</span>
      </div>
      <el-progress
        :percentage="storagePercent"
        :stroke-width="6"
        :show-text="false"
      />
      <div class="storage-text">
        {{ formatSize(usedStorage) }} / {{ formatSize(totalStorage) }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Files, FolderOpened, Picture, Document, VideoPlay, 
  Headset, MoreFilled, Delete, Coin, Upload, Setting 
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 当前激活的菜单项
const activeIndex = computed(() => {
  const routeName = route.name
  
  // 传输中心
  if (routeName === 'TransferCenter') {
    return 'transfer'
  }
  
  // 系统配置
  if (routeName === 'CongestionConfig') {
    return 'config'
  }
  
  // 文件管理
  if (routeName === 'File') {
    const fileType = route.query.fileType
    return fileType !== undefined ? `file-${fileType}` : 'file-0'
  }
  
  return 'transfer'
})

// 存储空间
const usedStorage = ref(0)
const totalStorage = ref(10 * 1024 * 1024 * 1024) // 10GB

const storagePercent = computed(() => {
  if (totalStorage.value === 0) return 0
  return Math.round((usedStorage.value / totalStorage.value) * 100)
})

// 格式化大小
const formatSize = (size) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 菜单选择
const handleMenuSelect = (index) => {
  // 传输中心
  if (index === 'transfer') {
    router.push({ name: 'TransferCenter' })
    return
  }
  
  // 系统配置
  if (index === 'config') {
    router.push({ name: 'CongestionConfig' })
    return
  }
  
  // 文件管理
  if (index.startsWith('file-')) {
    const fileType = parseInt(index.replace('file-', ''))
    router.push({
      name: 'File',
      query: {
        fileType,
        filePath: '/',
        folderId: 0
      }
    })
  }
}

// 加载存储信息
const loadStorageInfo = async () => {
  // TODO: 从后端获取存储信息
  usedStorage.value = 1.5 * 1024 * 1024 * 1024 // 模拟 1.5GB
}

onMounted(() => {
  loadStorageInfo()
})
</script>

<style lang="scss" scoped>
.aside-menu-wrapper {
  width: 200px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #ebeef5;
  
  .file-menu {
    flex: 1;
    border-right: none;
    overflow-y: auto;
    
    :deep(.el-sub-menu__title),
    :deep(.el-menu-item) {
      height: 48px;
      line-height: 48px;
      
      .el-icon {
        margin-right: 8px;
      }
    }
    
    :deep(.el-menu-item.is-active) {
      background-color: #ecf5ff;
    }
  }
  
  .storage-info {
    padding: 16px;
    border-top: 1px solid #ebeef5;
    
    .storage-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      font-size: 14px;
      color: #606266;
    }
    
    :deep(.el-progress) {
      margin-bottom: 8px;
    }
    
    .storage-text {
      font-size: 12px;
      color: #909399;
      text-align: center;
    }
  }
}
</style>
