import { createRouter, createWebHistory } from 'vue-router'

const routes = [
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
    name: 'FileList',
    component: () => import('@/views/FileList.vue'),
    meta: { title: '文件管理' }
  },
  {
    path: '/config',
    name: 'CongestionConfig',
    component: () => import('@/views/CongestionConfig.vue'),
    meta: { title: '系统配置' }
  },
  // 保留旧路由以兼容
  {
    path: '/upload',
    redirect: '/transfer'
  },
  {
    path: '/monitor',
    redirect: '/transfer'
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
  next()
})

export default router
