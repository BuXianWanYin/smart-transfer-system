<template>
  <div class="home-page">
    <div class="home-content">
      <h1>欢迎使用智慧转运系统</h1>
      <el-button type="primary" @click="goToLogin">前往登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

/**
 * 跳转到登录页
 */
function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.home-page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-sizing: border-box;
}

.home-content {
  text-align: center;
  color: #fff;
}

.home-content h1 {
  margin-bottom: 30px;
  font-size: 48px;
  font-weight: bold;
}

/* 平板适配 */
@media (max-width: 1024px) {
  .home-content h1 {
    font-size: 36px;
    margin-bottom: 24px;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }
  
  .home-content h1 {
    font-size: 24px;
    margin-bottom: 20px;
  }
}
</style>

