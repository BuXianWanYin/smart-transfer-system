import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import '@/assets/styles/variables.css'
import '@/assets/styles/index.css'
import { getUserInfo } from '@/api/userApi'
import { userStorage } from '@/utils/storage'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')

// 应用启动时验证 token 有效性（在路由准备好后）
router.isReady().then(async () => {
  const token = userStorage.getToken()
  if (token && token.trim() !== '') {
    try {
      // 验证 token 是否有效
      await getUserInfo()
      console.log('Token 验证成功')
    } catch (error) {
      // Token 无效，清除登录信息（sessionStorage，标签页级别）
      console.warn('Token 无效，清除登录信息')
      userStorage.clear()
      // 如果当前不在登录页，跳转到登录页
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    }
  } else {
    // 没有 token，如果不在登录页，跳转到登录页
    if (router.currentRoute.value.path !== '/login') {
      router.push('/login')
    }
  }
})

