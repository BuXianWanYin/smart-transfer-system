<template>
  <div class="file-timeline-wrapper" v-loading="loading" element-loading-text="文件加载中……">
    <!-- 空状态 -->
    <el-empty v-if="groupedList.length === 0" description="暂无数据">
      <template #image>
        <el-icon :size="80" color="#c0c4cc">
          <FolderOpened />
        </el-icon>
      </template>
    </el-empty>
    
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
      <div class="right-menu-item" @click="handleMenuRename">
        <el-icon><Edit /></el-icon>
        重命名
      </div>
      <div class="right-menu-item" @click="handleMenuCopy">
        <el-icon><CopyDocument /></el-icon>
        复制到
      </div>
      <div class="right-menu-item" @click="handleMenuMove">
        <el-icon><FolderOpened /></el-icon>
        移动到
      </div>
      <div class="right-menu-item" @click="handleMenuDetail">
        <el-icon><InfoFilled /></el-icon>
        详情
      </div>
      <div class="right-menu-item danger" @click="handleMenuDelete">
        <el-icon><Delete /></el-icon>
        删除
      </div>
    </div>
    
    <!-- 重命名对话框 -->
    <el-dialog v-model="renameVisible" title="重命名" width="400px" append-to-body>
      <el-form ref="renameFormRef" :model="renameForm" :rules="renameRules">
        <el-form-item prop="fileName">
          <el-input v-model="renameForm.fileName" placeholder="请输入新名称" @keyup.enter="confirmRename" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename" :loading="renameLoading">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 移动文件对话框 -->
    <MoveFileDialog v-model="moveDialogVisible" @confirm="confirmMove" />
    
    <!-- 复制文件对话框 -->
    <CopyFileDialog v-model="copyDialogVisible" :files="copyFileData ? [copyFileData] : []" @success="handleRefresh" />
    
    <!-- 文件详情弹窗 -->
    <FileDetailDialog
      v-model="detailDialogVisible"
      :file="detailFileData"
      :file-type="fileType"
      @preview="handleItemClick"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, View, FolderOpened, Delete, Edit, CopyDocument, InfoFilled } from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import CopyFileDialog from './CopyFileDialog.vue'
import FileDetailDialog from './FileDetailDialog.vue'
import { moveFile, deleteFile, renameFile, getPreviewUrl } from '@/api/fileApi'
import { deleteFolder } from '@/api/folderApi'
import { useTransferStore } from '@/store/transferStore'

const router = useRouter()
const transferStore = useTransferStore()

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
const moveFileData = ref(null)

// 复制
const copyDialogVisible = ref(false)
const copyFileData = ref(null)

// 详情
const detailDialogVisible = ref(false)
const detailFileData = ref(null)

// 重命名
const renameVisible = ref(false)
const renameLoading = ref(false)
const renameFormRef = ref(null)
const renameForm = ref({ fileName: '' })
const renameFileData = ref(null)
const renameRules = {
  fileName: [
    { required: true, message: '请输入文件名', trigger: 'blur' },
    { min: 1, max: 255, message: '文件名长度在 1 到 255 个字符', trigger: 'blur' }
  ]
}

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

// 右键菜单操作 - 添加到下载列表
const handleMenuDownload = () => {
  if (contextMenuRow.value) {
    const file = contextMenuRow.value
    // 添加到传输列表
    transferStore.addDownloadTask({
      fileId: file.id,
      fileName: file.fileName,
      fileSize: file.fileSize,
      fileHash: file.fileHash
    })
    ElMessage.success(`已添加 "${file.fileName}" 到下载列表`)
    // 跳转到传输中心
    router.push({ name: 'TransferCenter' })
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
  moveFileData.value = { ...contextMenuRow.value }
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
    
    if (row.isDir === 1) {
      // 删除文件夹
      await deleteFolder(row.id)
    } else {
      // 删除文件
      await deleteFile(row.id)
    }
    
    ElMessage.success('删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 重命名
const handleMenuRename = () => {
  renameFileData.value = { ...contextMenuRow.value }
  renameForm.value.fileName = contextMenuRow.value.fileName
  renameVisible.value = true
  closeContextMenu()
}

// 确认重命名
const confirmRename = async () => {
  if (!renameFormRef.value) return
  
  await renameFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    renameLoading.value = true
    try {
      await renameFile({
        id: renameFileData.value.id,
        fileName: renameForm.value.fileName
      })
      ElMessage.success('重命名成功')
      renameVisible.value = false
      emit('refresh')
    } catch (error) {
      console.error('重命名失败', error)
      ElMessage.error('重命名失败')
    } finally {
      renameLoading.value = false
      renameFileData.value = null
    }
  })
}

// 复制到
const handleMenuCopy = () => {
  copyFileData.value = { ...contextMenuRow.value }
  copyDialogVisible.value = true
  closeContextMenu()
}

// 详情
const handleMenuDetail = () => {
  detailFileData.value = { ...contextMenuRow.value }
  detailDialogVisible.value = true
  closeContextMenu()
}

// 刷新
const handleRefresh = () => {
  emit('refresh')
}

// 确认移动
const confirmMove = async (targetFolderId) => {
  if (!moveFileData.value) {
    ElMessage.error('请选择要移动的文件')
    return
  }
  try {
    await moveFile({
      id: moveFileData.value.id,
      targetFolderId
    })
    ElMessage.success('移动成功')
    emit('refresh')
  } catch (error) {
    console.error('移动失败', error)
    ElMessage.error('移动失败')
  } finally {
    moveFileData.value = null
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

/* 平板适配 */
@media (max-width: 1024px) {
  .file-timeline-wrapper {
    padding: 12px;
    
    .timeline-container {
      .timeline-group {
        .image-grid {
          grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
          gap: 6px;
        }
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .file-timeline-wrapper {
    padding: 10px;
    
    .timeline-container {
      .timeline-group {
        margin-bottom: 16px;
        
        .group-header {
          margin-bottom: 8px;
          
          .date-text {
            font-size: 14px;
          }
          
          .count-text {
            font-size: 12px;
          }
        }
        
        .image-grid {
          grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
          gap: 4px;
          
          .image-item {
            .checkbox-wrapper {
              opacity: 1;
            }
          }
        }
      }
    }
  }
  
  .right-menu-list {
    .right-menu-item {
      height: 40px;
      line-height: 40px;
      font-size: 14px;
    }
  }
}
</style>
