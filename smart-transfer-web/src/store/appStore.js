import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 应用全局状态管理
 * 包含响应式布局、侧边栏状态等
 */
export const useAppStore = defineStore('app', () => {
  // 屏幕宽度
  const screenWidth = ref(window.innerWidth)
  
  // 侧边栏是否折叠
  const sidebarCollapsed = ref(false)
  
  // 侧边栏是否显示（移动端用）
  const sidebarVisible = ref(true)
  
  // 设备类型
  const deviceType = computed(() => {
    if (screenWidth.value < 768) return 'mobile'
    if (screenWidth.value < 1024) return 'tablet'
    return 'desktop'
  })
  
  // 是否是移动端
  const isMobile = computed(() => deviceType.value === 'mobile')
  
  // 是否是平板
  const isTablet = computed(() => deviceType.value === 'tablet')
  
  // 是否是桌面端
  const isDesktop = computed(() => deviceType.value === 'desktop')
  
  // 侧边栏宽度
  const sidebarWidth = computed(() => {
    if (isMobile.value) return sidebarVisible.value ? '200px' : '0'
    if (sidebarCollapsed.value) return '64px'
    return '200px'
  })
  
  // 更新屏幕宽度
  const updateScreenWidth = () => {
    screenWidth.value = window.innerWidth
    
    // 移动端默认隐藏侧边栏
    if (isMobile.value) {
      sidebarVisible.value = false
    } else {
      sidebarVisible.value = true
    }
    
    // 平板默认折叠侧边栏
    if (isTablet.value && !sidebarCollapsed.value) {
      sidebarCollapsed.value = true
    }
  }
  
  // 切换侧边栏折叠状态
  const toggleSidebarCollapse = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
  
  // 切换侧边栏显示（移动端）
  const toggleSidebarVisible = () => {
    sidebarVisible.value = !sidebarVisible.value
  }
  
  // 显示侧边栏
  const showSidebar = () => {
    sidebarVisible.value = true
  }
  
  // 隐藏侧边栏
  const hideSidebar = () => {
    sidebarVisible.value = false
  }
  
  // 初始化监听
  const initResponsive = () => {
    updateScreenWidth()
    window.addEventListener('resize', updateScreenWidth)
  }
  
  // 销毁监听
  const destroyResponsive = () => {
    window.removeEventListener('resize', updateScreenWidth)
  }
  
  return {
    screenWidth,
    sidebarCollapsed,
    sidebarVisible,
    deviceType,
    isMobile,
    isTablet,
    isDesktop,
    sidebarWidth,
    updateScreenWidth,
    toggleSidebarCollapse,
    toggleSidebarVisible,
    showSidebar,
    hideSidebar,
    initResponsive,
    destroyResponsive
  }
})


