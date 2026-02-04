<template>
  <div class="dashboard-page">
    <!-- KPI行 -->
    <div class="kpi-row" v-loading="loading">
      <div class="kpi-card">
        <div class="change up">{{ dashboardData.kpiData?.usersChangePercent || '+0%' }}</div>
        <div class="kpi-icon-wrap" style="background: #3b82f6;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="kpi-content">
          <div class="value">{{ dashboardData.kpiData?.totalUsers || 0 }}</div>
          <div class="label">总用户数</div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="change up">{{ dashboardData.kpiData?.storageChangePercent || '+0%' }}</div>
        <div class="kpi-icon-wrap" style="background: #10b981;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <ellipse cx="12" cy="5" rx="9" ry="3"></ellipse>
            <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path>
            <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path>
          </svg>
        </div>
        <div class="kpi-content">
          <div class="value">{{ formatSize(dashboardData.kpiData?.totalStorage || 0) }}</div>
          <div class="label">总存储</div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="change" :class="getChangeClass(dashboardData.kpiData?.filesChangePercent)">
          {{ dashboardData.kpiData?.filesChangePercent || '+0%' }}
        </div>
        <div class="kpi-icon-wrap" style="background: #f59e0b;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
            <polyline points="13 2 13 9 20 9"></polyline>
          </svg>
        </div>
        <div class="kpi-content">
          <div class="value">{{ dashboardData.kpiData?.totalFiles || 0 }}</div>
          <div class="label">总文件数</div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="change up">{{ dashboardData.kpiData?.transferChangePercent || '+0%' }}</div>
        <div class="kpi-icon-wrap" style="background: #06b6d4;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
        </div>
        <div class="kpi-content">
          <div class="value">{{ formatSize(dashboardData.kpiData?.todayTransfer || 0) }}</div>
          <div class="label">今日传输量</div>
        </div>
      </div>
    </div>

    <!-- 主体：左右布局 -->
    <div class="grid-main-right">
      <!-- 左侧主内容区 -->
      <div>
        <!-- 传输趋势 -->
        <div class="card transfer-trend-card">
          <div class="card-header">传输趋势</div>
          <div class="card-body">
            <div class="chart-placeholder" v-if="!dashboardData.transferTrend?.uploadLabels?.length">
              <el-empty description="暂无传输数据" />
            </div>
            <div v-else ref="transferChartRef" class="transfer-chart"></div>
          </div>
        </div>

        <!-- 用户概述与存储 - 左右布局 -->
        <div class="stats-row">
          <!-- 用户概述 -->
          <div class="card">
            <div class="card-header">用户分布</div>
            <div class="card-body">
              <!-- 嵌套环形图 -->
              <div class="user-distribution-chart">
                <div ref="userChartRef" style="width: 100%; height: 400px;"></div>
              </div>
            </div>
          </div>

          <!-- 存储统计 -->
          <div class="card">
            <div class="card-header">存储统计</div>
            <div class="card-body">
              <div ref="storageChartRef" style="width: 100%; height: 400px;"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div>
        <!-- 快捷操作 -->
        <div class="card quick-actions-card">
          <div class="card-header">快捷操作</div>
          <div class="card-body quick-actions-body">
            <div class="quick-actions">
              <router-link to="/admin/users" class="action-item">
                <span class="action-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </span>
                <span class="action-text">用户管理</span>
              </router-link>

              <router-link to="/config" class="action-item">
                <span class="action-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                  </svg>
                </span>
                <span class="action-text">系统配置</span>
              </router-link>

              <router-link to="/files" class="action-item">
                <span class="action-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                  </svg>
                </span>
                <span class="action-text">文件管理</span>
              </router-link>

              <router-link to="/files?fileType=6" class="action-item">
                <span class="action-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </span>
                <span class="action-text">回收站</span>
              </router-link>
            </div>
          </div>
        </div>

        <!-- 最近动态 -->
        <div class="card recent-activity-card">
          <div class="card-header">最近动态</div>
          <div class="card-body recent-activity-body">
            <ul class="activity-list" v-if="dashboardData.recentActivities?.length">
              <li v-for="activity in dashboardData.recentActivities" :key="activity.id" class="activity-item">
                <span class="activity-dot"></span>
                <div class="activity-content">
                  <div class="activity-desc">{{ activity.activityDesc }}</div>
                  <div class="activity-time">{{ formatTime(activity.createTime) }}</div>
                </div>
              </li>
            </ul>
            <el-empty v-else description="暂无动态" :image-size="80" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboardData } from '@/api/dashboardApi'
import * as echarts from 'echarts'

// 加载状态
const loading = ref(false)

// 图表实例引用
const transferChartRef = ref(null)
const storageChartRef = ref(null)
const userChartRef = ref(null)

// 图表实例
let transferChart = null
let storageChart = null
let userChart = null

// 仪表盘数据
const dashboardData = reactive({
  kpiData: null,
  userStats: null,
  storageStats: null,
  transferTrend: null,
  recentActivities: []
})

/**
 * 加载仪表盘数据
 */
const loadDashboardData = async () => {
  loading.value = true
  try {
    const data = await getDashboardData()
    Object.assign(dashboardData, data)
    
    // 等待DOM更新后初始化图表
    await nextTick()
    initCharts()
  } catch (error) {
    ElMessage.error('加载仪表盘数据失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

/**
 * 格式化文件大小
 */
const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 格式化时间
 */
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const time = new Date(timeStr)
  const now = new Date()
  const diff = now - time
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return Math.floor(diff / minute) + ' 分钟前'
  } else if (diff < day) {
    return Math.floor(diff / hour) + ' 小时前'
  } else if (diff < 7 * day) {
    return Math.floor(diff / day) + ' 天前'
  } else {
    return time.toLocaleDateString()
  }
}

/**
 * 获取变化趋势的样式类
 */
const getChangeClass = (changePercent) => {
  if (!changePercent) return ''
  return changePercent.startsWith('+') ? 'up' : 'down'
}

/**
 * 计算百分比
 */
const getRolePercent = (count, total) => {
  if (!total || total === 0) return '0%'
  const percent = ((count || 0) / total * 100).toFixed(1)
  return `${percent}%`
}

/**
 * 初始化所有图表
 */
const initCharts = () => {
  initTransferChart()
  initStorageChart()
  initUserChart()
}

/**
 * 初始化传输趋势图表
 */
const initTransferChart = () => {
  if (!transferChartRef.value || !dashboardData.transferTrend) return
  
  if (transferChart) {
    transferChart.dispose()
  }
  
  transferChart = echarts.init(transferChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      formatter: (params) => {
        const formatBytes = (bytes) => {
          if (!bytes || bytes === 0) return '0 B'
          if (bytes < 1024) return bytes + ' B'
          if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
          if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + ' MB'
          return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
        }
        let result = `<div style="font-weight: bold; margin-bottom: 8px;">${params[0].axisValue}</div>`
        params.forEach(item => {
          result += `<div style="display: flex; align-items: center; margin-bottom: 4px;">
            <span style="display: inline-block; width: 10px; height: 10px; background: ${item.color}; border-radius: 50%; margin-right: 8px;"></span>
            <span>${item.seriesName}: ${formatBytes(item.value)}</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: ['上传', '下载'],
      top: 16,
      right: 24,
      itemGap: 24,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: {
        fontSize: 13,
        color: '#6b7280',
        fontWeight: 500
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '6%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dashboardData.transferTrend.uploadLabels || [],
      boundaryGap: false,
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#666',
        formatter: (value) => {
          if (value === 0) return '0'
          if (value < 1024) return value + ' B'
          if (value < 1024 * 1024) return (value / 1024).toFixed(1) + ' KB'
          if (value < 1024 * 1024 * 1024) return (value / 1024 / 1024).toFixed(1) + ' MB'
          return (value / 1024 / 1024 / 1024).toFixed(1) + ' GB'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [
      {
        name: '上传',
        type: 'line',
        smooth: true,
        data: dashboardData.transferTrend.uploadValues || [],
        lineStyle: {
          width: 3,
          color: '#3b82f6'
        },
        itemStyle: {
          color: '#3b82f6'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.25)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ])
        }
      },
      {
        name: '下载',
        type: 'line',
        smooth: true,
        data: dashboardData.transferTrend.downloadValues || [],
        lineStyle: {
          width: 3,
          color: '#10b981'
        },
        itemStyle: {
          color: '#10b981'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.25)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.05)' }
          ])
        }
      }
    ]
  }
  
  transferChart.setOption(option)
  
  // 确保图表正确填充容器
  setTimeout(() => {
    transferChart?.resize()
  }, 100)
}

/**
 * 初始化用户分布嵌套环形图
 */
const initUserChart = () => {
  if (!userChartRef.value || !dashboardData.userStats) return
  
  if (userChart) {
    userChart.dispose()
  }
  
  userChart = echarts.init(userChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    legend: {
      orient: 'horizontal',
      top: '5%',
      left: 'center',
      itemGap: 28,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: {
        fontSize: 13,
        color: '#6b7280',
        fontWeight: 500
      },
      data: ['管理员', '普通用户', '启用', '禁用']
    },
    series: [
      // 外环：角色分布
      {
        name: '角色分布',
        type: 'pie',
        radius: ['55%', '75%'],
        center: ['50%', '55%'],
        avoidLabelOverlap: false,
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        emphasis: {
          label: {
            show: false
          }
        },
        legendHoverLink: true,
        data: [
          {
            value: dashboardData.userStats.adminCount || 0,
            name: '管理员',
            itemStyle: { 
              color: '#3b82f6',
              borderColor: '#fff',
              borderWidth: 2
            }
          },
          {
            value: dashboardData.userStats.userCount || 0,
            name: '普通用户',
            itemStyle: { 
              color: '#10b981',
              borderColor: '#fff',
              borderWidth: 2
            }
          }
        ]
      },
      // 内环：状态分布
      {
        name: '状态分布',
        type: 'pie',
        radius: ['35%', '50%'],
        center: ['50%', '55%'],
        avoidLabelOverlap: false,
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        emphasis: {
          label: {
            show: false
          }
        },
        legendHoverLink: true,
        data: [
          {
            value: dashboardData.userStats.enabledUsers || 0,
            name: '启用',
            itemStyle: { 
              color: '#06b6d4',
              borderColor: '#fff',
              borderWidth: 2
            }
          },
          {
            value: dashboardData.userStats.disabledUsers || 0,
            name: '禁用',
            itemStyle: { 
              color: '#f87171',
              borderColor: '#fff',
              borderWidth: 2
            }
          }
        ]
      }
    ]
  }
  
  userChart.setOption(option)
  
  // 确保图表正确填充容器
  setTimeout(() => {
    userChart?.resize()
  }, 100)
}

/**
 * 初始化存储分类图表
 */
const initStorageChart = () => {
  if (!storageChartRef.value || !dashboardData.storageStats) return
  
  if (storageChart) {
    storageChart.dispose()
  }
  
  storageChart = echarts.init(storageChartRef.value)
  
  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 B'
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
    if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + ' MB'
    return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
  }
  
  // 先准备数据数组 - 采用柔和现代主义配色方案
  const storageData = [
    { 
      value: dashboardData.storageStats.imageSize || 0, 
      name: '图片',
      itemStyle: { 
        color: '#6B9BD1', // 柔和天蓝
        borderColor: '#fff',
        borderWidth: 2
      }
    },
    { 
      value: dashboardData.storageStats.videoSize || 0, 
      name: '视频',
      itemStyle: { 
        color: '#87C98D', // 薄荷绿
        borderColor: '#fff',
        borderWidth: 2
      }
    },
    { 
      value: dashboardData.storageStats.audioSize || 0, 
      name: '音频',
      itemStyle: { 
        color: '#B599D6', // 淡紫
        borderColor: '#fff',
        borderWidth: 2
      }
    },
    { 
      value: dashboardData.storageStats.docSize || 0, 
      name: '文档',
      itemStyle: { 
        color: '#F9C079', // 琥珀
        borderColor: '#fff',
        borderWidth: 2
      }
    },
    { 
      value: dashboardData.storageStats.archiveSize || 0, 
      name: '压缩包',
      itemStyle: { 
        color: '#E895B7', // 粉玫瑰
        borderColor: '#fff',
        borderWidth: 2
      }
    },
    { 
      value: dashboardData.storageStats.codeSize || 0, 
      name: '代码',
      itemStyle: { 
        color: '#6FCCCD', // 青绿松石
        borderColor: '#fff',
        borderWidth: 2
      }
    },
    { 
      value: dashboardData.storageStats.otherSize || 0, 
      name: '其他',
      itemStyle: { 
        color: '#A5A5A5', // 柔和中灰
        borderColor: '#fff',
        borderWidth: 2
      }
    }
  ] // 显示所有类型，包括值为0的
  
  const option = {
    animation: true,
    animationType: 'expansion',
    animationDuration: 1000,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}: ${formatBytes(params.value)} (${params.percent}%)`
      },
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(0, 0, 0, 0.06)',
      borderWidth: 1,
      padding: [12, 16],
      textStyle: {
        color: '#374151',
        fontSize: 13,
        fontWeight: 500
      },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); border-radius: 8px;'
    },
    legend: {
      orient: 'horizontal',
      top: '5%',
      left: 'center',
      itemGap: 28,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: {
        fontSize: 13,
        color: '#6b7280',
        fontWeight: 500
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['50%', '55%'],
        avoidLabelOverlap: false,
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        emphasis: {
          label: {
            show: false
          },
          itemStyle: {
            shadowBlur: 16,
            shadowOffsetX: 0,
            shadowOffsetY: 4,
            shadowColor: 'rgba(0, 0, 0, 0.12)'
          },
          scaleSize: 8
        },
        itemStyle: {
          borderRadius: 4
        },
        data: storageData
      }
    ]
  }
  
  storageChart.setOption(option)
  
  // 确保图表正确填充容器
  setTimeout(() => {
    storageChart?.resize()
  }, 100)
}

/**
 * 销毁图表
 */
const disposeCharts = () => {
  if (transferChart) {
    transferChart.dispose()
    transferChart = null
  }
  if (storageChart) {
    storageChart.dispose()
    storageChart = null
  }
  if (userChart) {
    userChart.dispose()
    userChart = null
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadDashboardData()
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

// 页面卸载时销毁图表
onUnmounted(() => {
  disposeCharts()
  window.removeEventListener('resize', handleResize)
})

// 处理窗口大小变化
const handleResize = () => {
  transferChart?.resize()
  storageChart?.resize()
  userChart?.resize()
}
</script>

<style scoped>
.dashboard-page {
  padding: 24px;
  width: 100%;
}

/* KPI行 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.kpi-card {
  background: var(--art-bg);
  border: 1px solid var(--art-border-color);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  position: relative;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.kpi-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-icon-wrap svg {
  color: white;
}

.kpi-content {
  flex: 1;
}

.kpi-card .value {
  font-family: var(--art-font-display);
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--art-text);
  margin-bottom: 4px;
  letter-spacing: -0.02em;
}

.kpi-card .label {
  font-size: 0.8125rem;
  color: var(--art-text-gray-500);
}

.kpi-card .change {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 0.75rem;
  font-weight: 500;
}

.kpi-card .change.up {
  color: rgb(var(--art-success));
}

.kpi-card .change.down {
  color: rgb(var(--art-danger));
}

/* 主布局 */
.grid-main-right {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
}

/* 统计行（用户概述+存储统计左右布局） */
.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card {
  background: var(--art-bg);
  border: 1px solid var(--art-border-color);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
  transition: box-shadow 0.25s ease;
}

.card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

/* 传输趋势卡片 */
.transfer-trend-card {
  margin-bottom: 20px;
}

.transfer-chart {
  width: 100%;
  height: 450px;
  min-height: 350px;
}

/* 快捷操作卡片 */
.quick-actions-card {
  margin-bottom: 16px;
  min-height: 280px;
}

.quick-actions-body {
  padding: 12px;
  min-height: 220px;
}

/* 最近动态卡片 */
.recent-activity-card {
  min-height: 400px;
}

.recent-activity-body {
  padding-top: 8px;
  min-height: 340px;
  max-height: 600px;
  overflow-y: auto;
}

/* 自定义滚动条样式 */
.recent-activity-body::-webkit-scrollbar {
  width: 6px;
}

.recent-activity-body::-webkit-scrollbar-track {
  background: transparent;
}

.recent-activity-body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
  transition: background 0.2s;
}

.recent-activity-body::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--art-border-color);
  font-family: var(--art-font-display);
  font-weight: 700;
  font-size: 0.9375rem;
  color: var(--art-text);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-body {
  padding: 20px;
}

.chart-placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 用户分布图表 */
.user-distribution-chart {
  display: flex;
  flex-direction: column;
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--art-text);
  text-decoration: none;
  transition: background 0.2s;
}

.action-item:hover {
  background: var(--art-fill-light);
}

.action-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-text {
  font-weight: 500;
  font-size: 0.875rem;
}

/* 最近动态 */
.activity-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.activity-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--art-border-color);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgb(var(--art-primary));
  flex-shrink: 0;
  margin-top: 6px;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-desc {
  color: var(--art-text-secondary);
  font-size: 0.8125rem;
  margin-bottom: 4px;
  word-wrap: break-word;
}

.activity-time {
  color: var(--art-text-gray-500);
  font-size: 0.75rem;
}

/* 响应式 */
@media (max-width: 1400px) {
  .transfer-chart {
    height: 400px;
  }
  
  .quick-actions-card {
    min-height: 260px;
  }
  
  .quick-actions-body {
    min-height: 200px;
  }
  
  .recent-activity-card {
    min-height: 360px;
  }
  
  .recent-activity-body {
    min-height: 300px;
  }
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .transfer-chart {
    height: 380px;
  }
}

@media (max-width: 900px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .grid-main-right {
    grid-template-columns: 1fr;
  }
  
  .transfer-chart {
    height: 350px;
    min-height: 300px;
  }
  
  .quick-actions-card {
    min-height: auto;
  }
  
  .quick-actions-body {
    min-height: auto;
  }
  
  .recent-activity-card {
    min-height: 300px;
  }
  
  .recent-activity-body {
    min-height: 240px;
    max-height: 400px;
  }
}

@media (max-width: 640px) {
  .transfer-chart {
    height: 300px;
    min-height: 280px;
  }
  
  .recent-activity-card {
    min-height: 260px;
  }
  
  .recent-activity-body {
    min-height: 200px;
    max-height: 350px;
  }
}
</style>
