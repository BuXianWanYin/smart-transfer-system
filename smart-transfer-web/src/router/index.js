import { createRouter, createWebHistory } from 'vue-router'
import { getUserInfo } from '@/api/userApi'
import { useUserStore } from '@/store/userStore'
import { ElMessage } from 'element-plus'
import { userStorage } from '@/utils/storage'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    redirect: '/transfer' // 默认跳转，实际跳转逻辑在路由守卫中处理
  },
  {
    path: '/transfer',
    name: 'TransferCenter',
    component: () => import('@/views/TransferCenter.vue'),
    meta: { title: '传输中心' }
  },
  {
    path: '/files',
    name: 'File',
    component: () => import('@/views/File.vue'),
    meta: { title: '文件管理' }
  },
  {
    path: '/config',
    name: 'CongestionConfig',
    component: () => import('@/views/CongestionConfig.vue'),
    meta: { title: '系统配置', requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: () => import('@/views/UserManagement.vue'),
    meta: { title: '用户管理', requiresAdmin: true }
  },
  {
    path: '/admin/stats',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '管理员工作台', requiresAdmin: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心' }
  },
  // 兼容旧路由
  {
    path: '/upload',
    redirect: '/transfer'
  },
  {
    path: '/recovery',
    redirect: '/files?fileType=6'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 标记是否正在验证 token（防止重复验证）
let isVerifyingToken = false
// 标记 token 是否已验证为有效
let tokenValidated = false

// 清除登录信息（使用sessionStorage，实现标签页隔离）
const clearAuth = () => {
  userStorage.clear()
  tokenValidated = false
}

// 验证 token 有效性
const validateToken = async () => {
  // 如果正在验证，等待完成
  if (isVerifyingToken) {
    return new Promise((resolve) => {
      const checkInterval = setInterval(() => {
        if (!isVerifyingToken) {
          clearInterval(checkInterval)
          resolve(tokenValidated)
        }
      }, 50)
    })
  }
  
  const token = userStorage.getToken()
  if (!token || token.trim() === '') {
    tokenValidated = false
    return false
  }
  
  // 如果已经验证过且有效，直接返回
  if (tokenValidated) {
    return true
  }
  
  isVerifyingToken = true
  
  try {
    // 尝试获取用户信息来验证 token，并更新userStore
    const userStore = useUserStore()
    const userInfo = await getUserInfo()
    // 更新userStore中的用户信息（包括role）
    userStore.userInfo = userInfo
    // 更新sessionStorage中的用户信息（标签页级别）
    userStorage.setUserInfo(userInfo)
    tokenValidated = true
    return true
  } catch (error) {
    // Token 无效，清除登录信息
    console.warn('Token 验证失败，清除登录信息:', error)
    clearAuth()
    tokenValidated = false
    return false
  } finally {
    isVerifyingToken = false
  }
}

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  const baseTitle = '基于Java+Vue的TCP拥塞控制优化大文件传输工具'
  document.title = to.meta.title ? `${to.meta.title} - ${baseTitle}` : baseTitle
  
  const isPublic = to.meta.public === true
  
  // 处理根路径重定向：根据用户角色跳转
  if (to.path === '/') {
    const token = userStorage.getToken()
    if (token && token.trim() !== '') {
      const isValid = await validateToken()
      if (isValid) {
        const userStore = useUserStore()
        if (!userStore.userInfo || !userStore.userInfo.role) {
          const storedUserInfo = userStorage.getUserInfo()
          if (storedUserInfo) {
            userStore.userInfo = storedUserInfo
          }
        }
        // 管理员跳转到系统统计，普通用户跳转到传输中心
        if (userStore.isAdmin) {
          next('/admin/stats')
        } else {
          next('/transfer')
        }
        return
      }
    }
    // 未登录或token无效，跳转到登录页
    next('/login')
    return
  }
  
  // 如果是登录页，清除验证标记（允许重新登录）
  if (to.path === '/login') {
    tokenValidated = false
    const token = userStorage.getToken()
    if (token && token.trim() !== '') {
      // 如果已有 token，验证其有效性
      const isValid = await validateToken()
      if (isValid) {
        // Token 有效，根据角色跳转
        const userStore = useUserStore()
        // 确保userStore已更新（从sessionStorage读取）
        if (!userStore.userInfo || !userStore.userInfo.role) {
          const storedUserInfo = userStorage.getUserInfo()
          if (storedUserInfo) {
            userStore.userInfo = storedUserInfo
          }
        }
        // 根据角色跳转：管理员跳转到系统统计，普通用户跳转到传输中心
        if (userStore.isAdmin) {
          next('/admin/stats')
        } else {
          next('/transfer')
        }
        return
      }
    }
    // Token 无效或不存在，显示登录页
    next()
    return
  }
  
  // 如果不是公开页面，需要验证 token
  if (!isPublic) {
    const token = userStorage.getToken()
    const hasToken = token && token.trim() !== ''
    
    if (!hasToken) {
      // 没有 token，跳转到登录页
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    
    // 有 token，验证其有效性
    const isValid = await validateToken()
    if (!isValid) {
      // Token 无效，跳转到登录页
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    
    // 检查是否需要管理员权限
    if (to.meta.requiresAdmin) {
      const userStore = useUserStore()
      // 确保userStore已更新（从sessionStorage读取）
      if (!userStore.userInfo || !userStore.userInfo.role) {
        // 如果userInfo中没有role，尝试从sessionStorage读取
        const storedUserInfo = userStorage.getUserInfo()
        if (storedUserInfo && storedUserInfo.role) {
          userStore.userInfo = storedUserInfo
        }
      }
      
      if (!userStore.isAdmin) {
        ElMessage.error('需要管理员权限')
        next('/transfer')
        return
      }
    }
    
    // 管理员不能访问传输中心，重定向到文件管理
    if (to.path === '/transfer' || to.name === 'TransferCenter') {
      const userStore = useUserStore()
      if (!userStore.userInfo || !userStore.userInfo.role) {
        const storedUserInfo = userStorage.getUserInfo()
        if (storedUserInfo && storedUserInfo.role) {
          userStore.userInfo = storedUserInfo
        }
      }
      if (userStore.isAdmin) {
        ElMessage.warning('管理员请使用系统管理功能')
        next('/admin/stats')
        return
      }
    }
  }
  
  // 其他情况正常放行
  next()
})

export default router
