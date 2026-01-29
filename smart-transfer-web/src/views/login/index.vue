<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h2>智慧转运系统</h2>
        <p>欢迎登录</p>
      </div>
      
      <el-form ref="loginFormRef" :model="loginForm" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LoginService } from '@/api/loginApi'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref()

// 加载状态
const loading = ref(false)

// 表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

/**
 * 处理登录
 */
function handleLogin() {
  loginFormRef.value.validate(async valid => {
    if (!valid) return
    
    loading.value = true
    try {
      const res = await LoginService.login(loginForm)
      
      // 保存 token 和用户信息
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.userInfo)
      
      ElMessage.success('登录成功')
      
      // 跳转到首页
      router.push('/')
    } catch (error) {
      ElMessage.error(error.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 100vh;
  padding: 20px;
  background: #ffffff;
  box-sizing: border-box;
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: var(--art-surface);
  border-radius: 12px;
  box-shadow: var(--art-box-shadow);
  border: 1px solid var(--art-border-color);
  border-top: 3px solid var(--art-primary-color-1);
  animation: loginReveal 0.5s var(--art-ease-out) both;
}

@keyframes loginReveal {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  margin-bottom: 30px;
  text-align: center;
}

.login-header h2 {
  margin-bottom: 10px;
  font-family: var(--art-font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--art-text-gray-800);
  letter-spacing: -0.02em;
}

.login-header p {
  font-size: 14px;
  color: var(--art-text-gray-500);
}

.login-form .login-btn {
  width: 100%;
}

/* 平板适配 */
@media (max-width: 1024px) {
  .login-container {
    padding: 32px;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .login-page {
    padding: 16px;
  }
  
  .login-container {
    padding: 24px;
    border-radius: 8px;
  }
  
  .login-header {
    margin-bottom: 24px;
    
    h2 {
      font-size: 24px;
    }
    
    p {
      font-size: 13px;
    }
  }
  
  :deep(.el-input__inner) {
    height: 44px;
    font-size: 15px;
  }
  
  :deep(.el-button) {
    height: 44px;
    font-size: 16px;
  }
}

/* 超小屏幕适配 */
@media (max-width: 375px) {
  .login-container {
    padding: 20px;
  }
  
  .login-header h2 {
    font-size: 22px;
  }
}
</style>

