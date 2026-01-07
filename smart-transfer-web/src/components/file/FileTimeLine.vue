<template>
  <div class="file-timeline-wrapper" v-loading="loading" element-loading-text="文件加载中……">
    <!-- 空状态 -->
    <el-empty v-if="groupedList.length === 0" description="暂无图片" />
    
    <!-- 时间线列表 -->
    <div class="timeline-container" v-else>
      <div class="timeline-group" v-for="group in groupedList" :key="group.date">
        <!-- 日期标题 -->
        <div class="group-header">
          <el-checkbox
            v-model="group.checked"
            :indeterminate="group.indeterminate"
            @change="handleGroupCheck(group)"
          />
          <span class="date-text">{{ group.date }}</span>
          <span class="count-text">（{{ group.items.length }}张）</span>
        </div>
        
        <!-- 图片网格 -->
        <div class="image-grid">
          <div
            class="image-item"
            :class="{ selected: item._checked }"
            v-for="item in group.items"
            :key="item.id"
            @click="handleItemClick(item)"
            @contextmenu.prevent="handleContextMenu(item, $event)"
          >
            <!-- 选择框 -->
            <div class="checkbox-wrapper" @click.stop>
              <el-checkbox v-model="item._checked" @change="updateGroupCheck(group)" />
            </div>
            
            <!-- 图片 -->
            <img 
              :src="getImageUrl(item)" 
              class="image-preview"
              :alt="item.fileName"
              @error="handleImageError"
            />
          </div>
        </div>
      </div>
    </div>
    
    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewUrls"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
    
    <!-- 右键菜单 -->
    <div
      v-if="contextMenuVisible"
      class="right-menu-list"
      :style="{ left: contextMenuPos.x + 'px', top: contextMenuPos.y + 'px' }"
    >
      <div class="right-menu-item" @click="handleMenuDownload">
        <el-icon><Download /></el-icon>
        下载
      </div>
      <div class="right-menu-item" @click="handleMenuPreview">
        <el-icon><View /></el-icon>
        预览
      </div>
      <div class="right-menu-item" @click="handleMenuMove">
        <el-icon><FolderOpened /></el-icon>
        移动到
      </div>
      <div class="right-menu-item danger" @click="handleMenuDelete">
        <el-icon><Delete /></el-icon>
        删除
      </div>
    </div>
    
    <!-- 移动文件对话框 -->
    <MoveFileDialog v-model="moveDialogVisible" @confirm="confirmMove" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, View, FolderOpened, Delete } from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import { moveFile, deleteFile, getPreviewUrl } from '@/api/fileApi'

const props = defineProps({
  fileType: { type: Number, required: true },
  filePath: { type: String, required: true },
  fileList: { type: Array, required: true },
  loading: { type: Boolean, required: true }
})

const emit = defineEmits(['refresh', 'selection-change'])

// 按日期分组
const groupedList = computed(() => {
  const groups = {}
  
  props.fileList.forEach(item => {
    // 只显示图片
    const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
    if (!imageExts.includes((item.extendName || '').toLowerCase())) return
    
    const date = formatDate(item.createTime || item.updateTime)
    if (!groups[date]) {
      groups[date] = {
        date,
        items: [],
        checked: false,
        indeterminate: false
      }
    }
    groups[date].items.push({ ...item, _checked: false })
  })
  
  return Object.values(groups).sort((a, b) => b.date.localeCompare(a.date))
})

// 选中的项
const selectedItems = computed(() => {
  const items = []
  groupedList.value.forEach(group => {
    group.items.forEach(item => {
      if (item._checked) items.push(item)
    })
  })
  return items
})

// 图片预览
const previewVisible = ref(false)
const previewIndex = ref(0)
const previewUrls = computed(() => {
  const urls = []
  props.fileList.forEach(item => {
    const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
    if (imageExts.includes((item.extendName || '').toLowerCase())) {
      urls.push(getImageUrl(item))
    }
  })
  return urls
})

// 右键菜单
const contextMenuVisible = ref(false)
const contextMenuPos = ref({ x: 0, y: 0 })
const contextMenuRow = ref(null)

// 移动
const moveDialogVisible = ref(false)

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '未知日期'
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

// 获取图片URL
const getImageUrl = (item) => {
  return getPreviewUrl(item.id)
}

// 图片加载失败
const handleImageError = (e) => {
  e.target.src = '/icons/image.svg'
}

// 点击图片
const handleItemClick = (item) => {
  item._checked = !item._checked
  // 更新分组选中状态
  groupedList.value.forEach(group => {
    updateGroupCheck(group)
  })
  emit('selection-change', selectedItems.value)
}

// 分组全选/取消
const handleGroupCheck = (group) => {
  group.items.forEach(item => {
    item._checked = group.checked
  })
  group.indeterminate = false
  emit('selection-change', selectedItems.value)
}

// 更新分组选中状态
const updateGroupCheck = (group) => {
  const checkedCount = group.items.filter(item => item._checked).length
  group.checked = checkedCount === group.items.length
  group.indeterminate = checkedCount > 0 && checkedCount < group.items.length
  emit('selection-change', selectedItems.value)
}

// 右键菜单
const handleContextMenu = (item, event) => {
  contextMenuRow.value = item
  contextMenuPos.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

// 关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false
  contextMenuRow.value = null
}

// 右键菜单操作
const handleMenuDownload = () => {
  if (contextMenuRow.value) {
    window.open(`/api/file/download/${contextMenuRow.value.id}`)
  }
  closeContextMenu()
}

const handleMenuPreview = () => {
  if (contextMenuRow.value) {
    // 找到当前图片在所有图片中的索引
    let index = 0
    props.fileList.forEach((item, i) => {
      if (item.id === contextMenuRow.value.id) {
        index = i
      }
    })
    previewIndex.value = index
    previewVisible.value = true
  }
  closeContextMenu()
}

const handleMenuMove = () => {
  moveDialogVisible.value = true
  closeContextMenu()
}

const handleMenuDelete = async () => {
  const row = contextMenuRow.value
  closeContextMenu()
  
  try {
    await ElMessageBox.confirm(
      `确定要删除 "${row.fileName}" 吗？`,
      '确认删除',
      { type: 'warning' }
    )
    
    await deleteFile(row.id)
    
    ElMessage.success('删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 确认移动
const confirmMove = async (targetFolderId) => {
  try {
    await moveFile({
      id: contextMenuRow.value.id,
      targetFolderId
    })
    ElMessage.success('移动成功')
    emit('refresh')
  } catch (error) {
    ElMessage.error('移动失败')
  }
}

// 点击其他地方关闭菜单
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})
</script>

<style lang="scss" scoped>
.file-timeline-wrapper {
  flex: 1;
  overflow: auto;
  padding: 16px;
  
  .timeline-container {
    .timeline-group {
      margin-bottom: 24px;
      
      .group-header {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 1px solid #ebeef5;
        
        .date-text {
          margin-left: 8px;
          font-size: 16px;
          font-weight: 500;
          color: #303133;
        }
        
        .count-text {
          font-size: 14px;
          color: #909399;
        }
      }
      
      .image-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
        gap: 8px;
        
        .image-item {
          position: relative;
          width: 100%;
          padding-top: 100%;
          border-radius: 4px;
          overflow: hidden;
          cursor: pointer;
          transition: all 0.2s;
          
          &:hover {
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
          }
          
          &.selected {
            box-shadow: 0 0 0 2px var(--el-color-primary);
          }
          
          .checkbox-wrapper {
            position: absolute;
            top: 4px;
            left: 4px;
            z-index: 10;
            opacity: 0;
            transition: opacity 0.2s;
            
            :deep(.el-checkbox__inner) {
              background: rgba(255, 255, 255, 0.9);
            }
          }
          
          &:hover .checkbox-wrapper,
          &.selected .checkbox-wrapper {
            opacity: 1;
          }
          
          .image-preview {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            object-fit: cover;
          }
        }
      }
    }
  }
}

// 右键菜单
.right-menu-list {
  position: fixed;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 9999;
  padding: 4px 0;
  
  .right-menu-item {
    padding: 0 16px;
    height: 36px;
    line-height: 36px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    color: #606266;
    
    &:hover {
      background: #ecf5ff;
      color: var(--el-color-primary);
    }
    
    &.danger {
      color: var(--el-color-danger);
      
      &:hover {
        background: #fef0f0;
      }
    }
  }
}
</style>
