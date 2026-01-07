<template>
  <div class="header-wrapper">
    <!-- Logo -->
    <div class="logo-area" @click="$router.push({ name: 'TransferCenter' })">
      <el-icon class="logo-icon"><Promotion /></el-icon>
      <span class="logo-text">智能传输系统</span>
    </div>
    
    <!-- 右侧用户信息 -->
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="userStore.avatar || undefined">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
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
import { Promotion, UserFilled, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push({ name: 'Profile' })
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        type: 'warning'
      })
      userStore.logout()
    } catch {
      // 取消
    }
  }
}
</script>

<style lang="scss" scoped>
.header-wrapper {
  width: 100%;
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  
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
    gap: 16px;
    
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
</style>
