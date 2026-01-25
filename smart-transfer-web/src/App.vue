<template>
  <!-- 登录页面：完全独立全屏布局 -->
  <div v-if="isPublicPage" class="public-layout">
    <router-view />
  </div>
  
  <!-- 其他页面：主布局 -->
  <div v-else id="app" :class="appClass">
    <Header />
    <div class="main-container">
      <!-- 移动端遮罩层 -->
      <div 
        v-if="appStore.isMobile && appStore.sidebarVisible" 
        class="sidebar-overlay"
        @click="appStore.hideSidebar"
      />
      
      <!-- 侧边栏 -->
      <aside 
        class="sidebar"
        :class="sidebarClass"
        :style="{ width: appStore.sidebarWidth }"
      >
        <AsideMenu :collapsed="false" />
      </aside>
      
      <!-- 主内容区 -->
      <div class="page-content" :style="contentStyle">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import AsideMenu from '@/components/file/AsideMenu.vue'
import { useAppStore } from '@/store/appStore'

const route = useRoute()
const appStore = useAppStore()

// 判断是否是公开页面（登录等）
const isPublicPage = computed(() => {
  return route.meta?.public === true || route.path === '/login' || route.name === 'Login'
})

// App 容器类名
const appClass = computed(() => ({
  'is-mobile': appStore.isMobile,
  'is-tablet': appStore.isTablet,
  'is-desktop': appStore.isDesktop
}))

// 侧边栏类名
const sidebarClass = computed(() => ({
  'is-hidden': appStore.isMobile && !appStore.sidebarVisible,
  'is-visible': appStore.isMobile && appStore.sidebarVisible
}))

// 内容区样式
const contentStyle = computed(() => {
  if (appStore.isMobile) {
    return { marginLeft: '0' }
  }
  return { marginLeft: appStore.sidebarWidth }
})

// 初始化响应式监听
onMounted(() => {
  appStore.initResponsive()
})

onUnmounted(() => {
  appStore.destroyResponsive()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 公开页面（登录等）- 完全独立 */
.public-layout {
  width: 100vw;
  height: 100vh;
}

/* 主应用布局 */
#app {
  font-family: 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  height: 100vh;
  overflow: hidden;
  -webkit-text-size-adjust: none;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.main-container {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* 侧边栏 */
.sidebar {
  position: fixed;
  top: 60px;
  left: 0;
  height: calc(100vh - 60px);
  background: transparent;
  border-right: 1px solid #e6e6e6;
  transition: all 0.3s ease;
  z-index: 100;
  overflow: hidden;
}

/* 移动端侧边栏 */
.sidebar.is-hidden {
  transform: translateX(-100%);
}

.sidebar.is-visible {
  transform: translateX(0);
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.1);
}

/* 移动端遮罩层 */
.sidebar-overlay {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 内容区 */
.page-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
  transition: margin-left 0.3s ease;
  margin-top: 60px;
  height: calc(100vh - 60px);
}

/* 移动端样式 */
#app.is-mobile {
  .page-content {
    padding: 12px;
    margin-left: 0 !important;
  }
}

/* 平板样式 */
#app.is-tablet {
  .page-content {
    padding: 14px;
  }
}

/* 桌面端样式 */
#app.is-desktop {
  .page-content {
    padding: 16px;
  }
}
</style>
