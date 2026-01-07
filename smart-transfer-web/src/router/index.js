import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    redirect: '/transfer'
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
    meta: { title: '系统配置' }
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
    path: '/monitor',
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

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  const baseTitle = '基于Java+Vue的TCP拥塞控制优化大文件传输工具'
  document.title = to.meta.title ? `${to.meta.title} - ${baseTitle}` : baseTitle
  
  // 检查是否需要登录
  const token = localStorage.getItem('token')
  const isPublic = to.meta.public
  
  if (!isPublic && !token) {
    // 未登录且不是公开页面，跳转到登录页
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    // 已登录但访问登录页，跳转到首页
    next('/')
  } else {
    next()
  }
})

export default router
