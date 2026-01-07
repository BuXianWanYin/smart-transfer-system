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
        <h1 class="title">智能传输系统</h1>
        <p class="subtitle">基于TCP拥塞控制优化的大文件传输工具</p>
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
        <p v-if="!isRegister" class="hint">默认账号：admin / 123456</p>
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
    
    <div class="copyright">
      © 2024 智能传输系统 | 毕业设计作品
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
    
    // 跳转到之前的页面或首页
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
    
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

// 背景动画
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  
  .bg-shape {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
    animation: float 20s infinite ease-in-out;
    
    &.shape-1 {
      width: 500px;
      height: 500px;
      background: #fff;
      top: -200px;
      left: -100px;
      animation-delay: 0s;
    }
    
    &.shape-2 {
      width: 400px;
      height: 400px;
      background: #fff;
      bottom: -150px;
      right: -100px;
      animation-delay: -5s;
    }
    
    &.shape-3 {
      width: 300px;
      height: 300px;
      background: #fff;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      animation-delay: -10s;
    }
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-30px) rotate(10deg);
  }
}

// 登录卡片
.login-card {
  width: 420px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  
  .logo-icon {
    font-size: 48px;
    color: #667eea;
    margin-bottom: 16px;
  }
  
  .title {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
    margin: 0 0 8px 0;
  }
  
  .subtitle {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

.login-form {
  :deep(.el-input__wrapper) {
    border-radius: 8px;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
    
    &:hover {
      box-shadow: 0 0 0 1px #c0c4cc inset;
    }
    
    &.is-focus {
      box-shadow: 0 0 0 1px #667eea inset;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  
  &:hover {
    opacity: 0.9;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

