<template>
  <div class="header-wrapper">
    <!-- Logo -->
    <div class="logo-area" @click="$router.push({ name: 'TransferCenter' })">
      <el-icon class="logo-icon"><Promotion /></el-icon>
      <span class="logo-text">智能传输系统</span>
    </div>
    
    <!-- 右侧留空或放其他内容 -->
    <div class="header-right">
      <!-- 可以放全局搜索、通知等 -->
    </div>
  </div>
</template>

<script setup>
import { Promotion } from '@element-plus/icons-vue'
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
  }
}
</style>
