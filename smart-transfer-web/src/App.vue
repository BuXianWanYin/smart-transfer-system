<template>
  <div id="app">
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
import Header from '@/components/Header.vue'
import AsideMenu from '@/components/file/AsideMenu.vue'
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
