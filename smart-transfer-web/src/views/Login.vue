<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
    </div>
    
    <div class="login-card">
      <div class="login-header">
        <el-icon class="logo-icon"><Promotion /></el-icon>
        <h1 class="title">大文件传输工具</h1>
        <p class="subtitle">基于Java+Vue的TCP拥塞控制优化大文件传输工具</p>
      </div>
      
      <!-- 登录表单 -->
      <el-form
        v-if="!isRegister"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 注册表单 -->
      <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="login-form"
        @keyup.enter="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名（3-20个字符）"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称（选填）"
            size="large"
            :prefix-icon="EditPen"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（6-20位）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p v-if="!isRegister" class="hint">默认管理员账号：admin / 123456</p>
        <p v-if="!isRegister" class="hint">默认用户账号：user1 / 123456</p>
        <p class="switch-link">
          <span v-if="!isRegister">
            还没有账号？<el-link type="primary" @click="isRegister = true">立即注册</el-link>
          </span>
          <span v-else>
            已有账号？<el-link type="primary" @click="isRegister = false">返回登录</el-link>
          </span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion, User, Lock, EditPen } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/userStore'
import { register } from '@/api/userApi'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const isRegister = ref(false)

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单
const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

// 登录验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 注册验证规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 如果已登录，直接跳转
onMounted(() => {
  if (userStore.isLoggedIn) {
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  }
})

// 登录
const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
    
    loading.value = true
    await userStore.login(loginForm.username, loginForm.password)
    
    ElMessage.success('登录成功')
    
    // 根据角色和redirect参数决定跳转页面
    const redirect = route.query.redirect
    if (redirect) {
      // 如果有redirect参数，检查是否有权限访问
      if (redirect.includes('/admin/') || redirect.includes('/config')) {
        // 如果是管理员页面，检查权限
        if (!userStore.isAdmin) {
          // 普通用户尝试访问管理员页面，跳转到传输中心
          router.replace('/transfer')
          return
        }
      }
      router.replace(redirect)
    } else {
      // 没有redirect参数，默认跳转到传输中心（所有用户可访问）
      router.replace('/transfer')
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()
    
    loading.value = true
    await register(registerForm)
    
    ElMessage.success('注册成功，请登录')
    
    // 切换到登录表单并填充用户名
    isRegister.value = false
    loginForm.username = registerForm.username
    loginForm.password = ''
    
    // 清空注册表单
    registerForm.username = ''
    registerForm.nickname = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '注册失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  position: relative;
  overflow: hidden;
}

.login-bg {
  display: none;
}

// 登录卡片
.login-card {
  width: 420px;
  padding: 40px;
  background: var(--art-surface);
  border-radius: 12px;
  box-shadow: var(--art-box-shadow-lg);
  border: 1px solid var(--art-border-color);
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  
  .logo-icon {
    font-size: 48px;
    color: var(--art-primary-color-1);
    margin-bottom: 16px;
  }
  
  .title {
    font-family: var(--art-font-display);
    font-size: 26px;
    font-weight: 600;
    color: var(--art-text-gray-800);
    margin: 0 0 8px 0;
    letter-spacing: -0.02em;
  }
  
  .subtitle {
    font-size: 14px;
    color: var(--art-text-gray-500);
    margin: 0;
  }
}

.login-form {
  :deep(.el-input__wrapper) {
    border-radius: 8px;
    box-shadow: 0 0 0 1px var(--art-border-color) inset;
    
    &:hover {
      box-shadow: 0 0 0 1px var(--art-gray-400) inset;
    }
    
    &.is-focus {
      box-shadow: 0 0 0 2px rgba(var(--art-primary), 0.2) inset;
    }
  }
  
  :deep(.el-input__inner) {
    height: 48px;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
  background: var(--art-primary-color-1);
  border: none;
  transition: opacity var(--art-duration-fast) var(--art-ease-out);
  
  &:hover {
    opacity: 0.9;
    background: var(--art-primary-color-1);
  }
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  
  .hint {
    font-size: 13px;
    color: #909399;
    margin: 0 0 8px 0;
  }
  
  .switch-link {
    font-size: 13px;
    color: #606266;
    margin: 0;
  }
}

.copyright {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

// 响应式
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 30px 20px;
  }
  
  .login-header {
    .title {
      font-size: 24px;
    }
    
    .subtitle {
      font-size: 12px;
    }
  }
}
</style>

