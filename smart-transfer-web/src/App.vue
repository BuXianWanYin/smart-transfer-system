<template>
  <div id="app">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="200px">
        <div class="logo">
          <h3>文件传输系统</h3>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          class="el-menu-vertical"
        >
          <el-menu-item index="/upload">
            <el-icon><UploadFilled /></el-icon>
            <span>文件上传</span>
          </el-menu-item>
          <el-menu-item index="/files">
            <el-icon><Document /></el-icon>
            <span>文件列表</span>
          </el-menu-item>
          <el-menu-item index="/monitor">
            <el-icon><Monitor /></el-icon>
            <span>拥塞监控</span>
          </el-menu-item>
          <el-menu-item index="/config">
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-container>
        <el-header>
          <div class="header-content">
            <h2>基于TCP拥塞控制优化的大文件传输工具</h2>
          </div>
        </el-header>
        
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { UploadFilled, Document, Monitor, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  height: 100vh;
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: var(--art-sidebar-bg);
  color: #fff;
  box-shadow: var(--art-box-shadow-sm);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--art-sidebar-logo-bg);
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.logo h3 {
  font-size: 18px;
}

.el-menu {
  border-right: none;
  background-color: var(--art-sidebar-bg);
}

.el-menu-item {
  color: var(--art-sidebar-text);
  transition: all 0.3s;
}

.el-menu-item:hover,
.el-menu-item.is-active {
  background-color: var(--art-sidebar-menu-hover) !important;
  color: var(--art-sidebar-text-active) !important;
}

.el-header {
  background-color: var(--art-main-bg-color);
  box-shadow: var(--art-root-card-box-shadow);
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--art-border-color);
}

.header-content h2 {
  font-size: 20px;
  color: var(--art-text-gray-900);
  font-weight: 600;
}

.el-main {
  background-color: var(--art-bg-color);
  overflow: auto;
  padding: 20px;
}
</style>
