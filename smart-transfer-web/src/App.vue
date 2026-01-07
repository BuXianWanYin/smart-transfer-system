<template>
  <!-- 登录页面：完全独立的全屏布局 -->
  <div v-if="isPublicPage" class="public-layout">
    <router-view />
  </div>
  
  <!-- 其他页面：主布局 -->
  <div v-else id="app">
    <Header />
    <div class="main-container">
      <AsideMenu />
      <div class="page-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import AsideMenu from '@/components/file/AsideMenu.vue'

const route = useRoute()

// 判断是否是公开页面（登录页等不需要布局的页面）
const isPublicPage = computed(() => {
  // 检查 meta.public 或直接判断路径
  return route.meta?.public === true || route.path === '/login' || route.name === 'Login'
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
  background: #f5f7fa;
}

.main-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.page-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}
</style>
