<template>
  <div class="aside-menu-wrapper" :class="{ 'is-collapsed': collapsed }">
    <el-menu
      :default-active="activeIndex"
      :default-openeds="collapsed ? [] : ['files']"
      :collapse="collapsed"
      :collapse-transition="false"
      class="file-menu"
      @select="handleMenuSelect"
    >
      <!-- 传输中心（仅普通用户可见） -->
      <el-menu-item v-if="!userStore.isAdmin" index="transfer">
        <el-icon><Upload /></el-icon>
        <template #title>
          <span>传输中心</span>
          <el-badge 
            v-if="transferCount > 0 && !collapsed" 
            :value="transferCount" 
            :max="99"
            class="transfer-badge"
          />
        </template>
      </el-menu-item>
      
      <!-- 普通用户：我的文件 -->
      <template v-if="!userStore.isAdmin">
        <el-sub-menu index="files" v-if="!collapsed">
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
        
        <!-- 折叠模式下的文件分类菜单 -->
        <el-menu-item v-if="collapsed" index="file-0" title="全部">
          <el-icon><FolderOpened /></el-icon>
          <template #title><span>全部</span></template>
        </el-menu-item>
        
        <el-menu-item v-if="collapsed" index="file-1" title="图片">
          <el-icon><Picture /></el-icon>
          <template #title><span>图片</span></template>
        </el-menu-item>
        
        <el-menu-item v-if="collapsed" index="file-2" title="文档">
          <el-icon><Document /></el-icon>
          <template #title><span>文档</span></template>
        </el-menu-item>
        
        <el-menu-item v-if="collapsed" index="file-3" title="视频">
          <el-icon><VideoPlay /></el-icon>
          <template #title><span>视频</span></template>
        </el-menu-item>
        
        <el-menu-item v-if="collapsed" index="file-4" title="音乐">
          <el-icon><Headset /></el-icon>
          <template #title><span>音乐</span></template>
        </el-menu-item>
        
        <el-menu-item v-if="collapsed" index="file-5" title="其他">
          <el-icon><MoreFilled /></el-icon>
          <template #title><span>其他</span></template>
        </el-menu-item>
        
        <!-- 回收站 -->
        <el-menu-item index="file-6" :title="collapsed ? '回收站' : undefined">
          <el-icon><Delete /></el-icon>
          <template #title><span>回收站</span></template>
        </el-menu-item>
      </template>
      
      <!-- 管理员菜单（按指定顺序） -->
      <template v-if="userStore.isAdmin">
        <!-- 1. 系统统计 -->
        <el-menu-item index="admin-stats" :title="collapsed ? '系统统计' : undefined">
          <el-icon><DataAnalysis /></el-icon>
          <template #title><span>系统统计</span></template>
        </el-menu-item>
        
        <!-- 2. 用户管理 -->
        <el-menu-item index="admin-users" :title="collapsed ? '用户管理' : undefined">
          <el-icon><User /></el-icon>
          <template #title><span>用户管理</span></template>
        </el-menu-item>
        
        <!-- 3. 文件管理 -->
        <el-menu-item index="files" :title="collapsed ? '文件管理' : undefined">
          <el-icon><Folder /></el-icon>
          <template #title><span>文件管理</span></template>
        </el-menu-item>
        
        <!-- 4. 回收站 -->
        <el-menu-item index="recovery" :title="collapsed ? '回收站' : undefined">
          <el-icon><Delete /></el-icon>
          <template #title><span>回收站</span></template>
        </el-menu-item>
        
        <!-- 5. 系统配置 -->
        <el-menu-item index="config" :title="collapsed ? '系统配置' : undefined">
          <el-icon><Setting /></el-icon>
          <template #title><span>系统配置</span></template>
        </el-menu-item>
      </template>
    </el-menu>
    
    <!-- 存储空间（非折叠模式显示） -->
    <div class="storage-info" v-if="!collapsed">
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
    
    <!-- 折叠模式下的存储图标 -->
    <div class="storage-icon" v-else>
      <el-tooltip :content="`${formatSize(usedStorage)} / ${formatSize(totalStorage)}`" placement="right">
        <el-icon><Coin /></el-icon>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Files, FolderOpened, Picture, Document, VideoPlay, 
  Headset, MoreFilled, Delete, Coin, Upload, Setting, 
  User, DataAnalysis
} from '@element-plus/icons-vue'
import { useAppStore } from '@/store/appStore'
import { useTransferStore } from '@/store/transferStore'
import { useUserStore } from '@/store/userStore'

const props = defineProps({
  collapsed: { type: Boolean, default: false }
})

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const transferStore = useTransferStore()
const userStore = useUserStore()

// 传输任务数量
const transferCount = computed(() => transferStore.totalTransferCount)

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
  
  // 用户管理
  if (routeName === 'UserManagement') {
    return 'admin-users'
  }
  
  // 系统统计
  if (routeName === 'SystemStats') {
    return 'admin-stats'
  }
  
  // 文件管理
  if (routeName === 'File') {
    const fileType = route.query.fileType
    // 回收站
    if (fileType === '6' || fileType === 6) {
      return 'recovery'
    }
    // 管理员：文件管理（无fileType参数或fileType不是6）
    if (userStore.isAdmin && (fileType === undefined || fileType === '')) {
      return 'files'
    }
    // 普通用户：根据文件类型返回
    return fileType !== undefined ? `file-${fileType}` : 'file-0'
  }
  
  // 默认值：管理员返回系统统计，普通用户返回传输中心
  return userStore.isAdmin ? 'admin-stats' : 'transfer'
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
  // 移动端点击菜单后隐藏侧边栏
  if (appStore.isMobile) {
    appStore.hideSidebar()
  }
  
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
  
  // 文件管理（管理员）
  if (index === 'files' && userStore.isAdmin) {
    router.push({ name: 'File' })
    return
  }
  
  // 用户管理
  if (index === 'admin-users') {
    router.push({ name: 'UserManagement' })
    return
  }
  
  // 系统统计
  if (index === 'admin-stats') {
    router.push({ name: 'SystemStats' })
    return
  }
  
  // 回收站
  if (index === 'recovery') {
    router.push({
      name: 'File',
      query: {
        fileType: 6,
        filePath: '/',
        folderId: 0
      }
    })
    return
  }
  
  // 文件管理（普通用户）
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
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
  transition: width 0.3s ease;
  
  &.is-collapsed {
    .file-menu {
      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        padding: 0 20px !important;
        
        .el-icon {
          margin-right: 0;
        }
        
        span {
          display: none;
        }
      }
    }
  }
  
  .file-menu {
    flex: 1;
    border-right: none;
    overflow-y: auto;
    background: transparent;
    
    &:not(.el-menu--collapse) {
      width: 100%;
    }
    
    :deep(.el-menu) {
      background: transparent;
    }
    
    :deep(.el-sub-menu__title),
    :deep(.el-menu-item) {
      height: 52px;
      line-height: 52px;
      font-size: 15px;
      background: transparent;
      transition: background var(--art-duration-fast) var(--art-ease-out);
      
      .el-icon {
        margin-right: 12px;
        font-size: 22px;
        width: 22px;
        height: 22px;
      }
      
      &:hover {
        background-color: rgb(var(--art-hoverColor));
      }
    }
    
    :deep(.el-menu-item.is-active) {
      background-color: rgb(var(--art-bg-primary));
      color: rgb(var(--art-primary));
      font-weight: 500;
    }
    
    // 传输徽章样式
    .transfer-badge {
      margin-left: 8px;
      
      :deep(.el-badge__content) {
        font-size: 10px;
        height: 16px;
        line-height: 16px;
        padding: 0 5px;
      }
    }
  }
  
  .storage-info {
    padding: 16px;
    border-top: 1px solid var(--art-border-color);
    background: transparent;
    
    .storage-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      font-size: 14px;
      color: var(--art-text-gray-600);
    }
    
    :deep(.el-progress) {
      margin-bottom: 8px;
    }
    
    .storage-text {
      font-size: 12px;
      color: var(--art-text-gray-500);
      text-align: center;
    }
  }
  
  .storage-icon {
    padding: 16px;
    text-align: center;
    border-top: 1px solid var(--art-border-color);
    background: transparent;
    
    .el-icon {
      font-size: 20px;
      color: var(--art-text-gray-500);
      cursor: pointer;
      transition: color var(--art-duration-fast) var(--art-ease-out);
      
      &:hover {
        color: rgb(var(--art-primary));
      }
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .aside-menu-wrapper {
    .file-menu {
      :deep(.el-sub-menu__title),
      :deep(.el-menu-item) {
        height: 48px;
        line-height: 48px;
        font-size: 14px;
        
        .el-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
        }
      }
    }
    
    .storage-info {
      padding: 12px;
      
      .storage-header {
        font-size: 13px;
      }
      
      .storage-text {
        font-size: 11px;
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .aside-menu-wrapper {
    background: var(--art-surface);
    
    .file-menu {
      :deep(.el-sub-menu__title),
      :deep(.el-menu-item) {
        height: 52px;
        line-height: 52px;
        font-size: 15px;
        padding-left: 16px !important;
        
        .el-icon {
          font-size: 22px;
          width: 22px;
          height: 22px;
          margin-right: 12px;
        }
        
        // 移动端显示文字
        span {
          display: inline !important;
        }
      }
      
      // 传输徽章
      .transfer-badge {
        margin-left: auto;
        margin-right: 12px;
      }
    }
    
    .storage-info {
      padding: 16px;
      border-top: 1px solid var(--art-border-color);
      
      .storage-header {
        font-size: 14px;
      }
    }
  }
}
</style>
