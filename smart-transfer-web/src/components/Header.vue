<template>
  <div class="header-wrapper">
    <!-- 左侧区域 -->
    <div class="header-left">
      <!-- 移动端菜单按钮（仅移动端显示） -->
      <el-icon 
        v-if="appStore.isMobile" 
        class="menu-toggle"
        @click="appStore.toggleSidebarVisible"
      >
        <Fold v-if="appStore.sidebarVisible" />
        <Expand v-else />
      </el-icon>
      
      <!-- Logo -->
      <div class="logo-area" @click="handleLogoClick">
        <el-icon class="logo-icon"><Promotion /></el-icon>
        <span class="logo-text" v-if="!appStore.isMobile">基于Java+Vue的TCP拥塞控制优化大文件传输工具</span>
      </div>
    </div>
    
    <!-- 右侧用户信息 -->
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="userStore.avatar || undefined">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="user-name" v-if="!appStore.isMobile">{{ userStore.nickname || userStore.username }}</span>
          <el-icon class="arrow-icon" v-if="!appStore.isMobile"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Promotion, UserFilled, ArrowDown, User, SwitchButton, Fold, Expand } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/userStore'
import { useAppStore } from '@/store/appStore'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push({ name: 'Profile' })
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        type: 'warning'
      })
      await userStore.logout()
    } catch {
      // 取消
    }
  }
}

const handleLogoClick = () => {
  // 管理员跳转到文件管理，普通用户跳转到传输中心
  if (userStore.isAdmin) {
    router.push({ name: 'File' })
  } else {
    router.push({ name: 'TransferCenter' })
  }
}
</script>

<style lang="scss" scoped>
.header-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 60px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  z-index: 200;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .menu-toggle {
      font-size: 26px;
      color: #606266;
      cursor: pointer;
      padding: 10px 12px;
      border-radius: 8px;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 44px;
      min-height: 44px;
      flex-shrink: 0;
      
      &:hover {
        background: #f5f7fa;
        color: var(--el-color-primary);
      }
      
      &:active {
        transform: scale(0.95);
      }
    }
  }
  
  .logo-area {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    
    .logo-icon {
      font-size: 28px;
      color: var(--el-color-primary);
    }
    
    .logo-text {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 20px;
      transition: background 0.2s;
      
      &:hover {
        background: #f5f7fa;
      }
      
      .user-name {
        font-size: 14px;
        color: #606266;
        max-width: 100px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .arrow-icon {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

/* 移动端样式 */
@media (max-width: 768px) {
  .header-wrapper {
    padding: 0 12px;
    
    .logo-area {
      .logo-icon {
        font-size: 24px;
      }
    }
  }
}
</style>
