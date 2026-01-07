<template>
  <el-dialog
    v-model="visible"
    title="移动到"
    width="500px"
    :close-on-click-modal="false"
    @open="loadFolders"
  >
    <div class="move-dialog-content">
      <!-- 面包屑 -->
      <div class="folder-breadcrumb">
        <span class="breadcrumb-item" @click="navigateToRoot">
          <el-icon><HomeFilled /></el-icon>
          全部文件
        </span>
        <template v-for="(folder, index) in currentPath" :key="folder.id">
          <span class="separator">/</span>
          <span 
            class="breadcrumb-item"
            :class="{ 'is-current': index === currentPath.length - 1 }"
            @click="navigateTo(index)"
          >
            {{ folder.folderName }}
          </span>
        </template>
      </div>
      
      <!-- 文件夹列表 -->
      <div class="folder-list" v-loading="loading">
        <el-empty v-if="folderList.length === 0" description="暂无文件夹" />
        <div
          class="folder-item"
          v-for="folder in folderList"
          :key="folder.id"
          @click="selectFolder(folder)"
          @dblclick="enterFolder(folder)"
        >
          <el-icon class="folder-icon"><Folder /></el-icon>
          <span class="folder-name">{{ folder.folderName }}</span>
        </div>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirmMove">
        移动到此处
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { HomeFilled, Folder } from '@element-plus/icons-vue'
import { getFolderList } from '@/api/folderApi'

const props = defineProps({
  modelValue: { type: Boolean, required: true }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 文件夹列表
const folderList = ref([])
const loading = ref(false)

// 当前路径
const currentPath = ref([])
const currentFolderId = computed(() => {
  if (currentPath.value.length === 0) return 0
  return currentPath.value[currentPath.value.length - 1].id
})

// 加载文件夹
const loadFolders = async () => {
  loading.value = true
  try {
    const res = await getFolderList({ parentId: currentFolderId.value })
    // http工具自动解包 res.data，所以 res 直接就是文件夹数组
    folderList.value = Array.isArray(res) ? res : []
  } catch (error) {
    folderList.value = []
  } finally {
    loading.value = false
  }
}

// 导航到根目录
const navigateToRoot = () => {
  currentPath.value = []
  loadFolders()
}

// 导航到指定层级
const navigateTo = (index) => {
  currentPath.value = currentPath.value.slice(0, index + 1)
  loadFolders()
}

// 选择文件夹
const selectFolder = (folder) => {
  // 单击选中高亮（可选实现）
}

// 进入文件夹
const enterFolder = (folder) => {
  currentPath.value.push(folder)
  loadFolders()
}

// 确认移动
const confirmMove = () => {
  emit('confirm', currentFolderId.value)
  visible.value = false
}

// 监听显示状态
watch(visible, (val) => {
  if (val) {
    currentPath.value = []
    loadFolders()
  }
})
</script>

<style lang="scss" scoped>
.move-dialog-content {
  .folder-breadcrumb {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    padding: 8px 12px;
    background: #f5f7fa;
    border-radius: 4px;
    margin-bottom: 12px;
    
    .breadcrumb-item {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: #606266;
      cursor: pointer;
      
      &:hover:not(.is-current) {
        color: var(--el-color-primary);
      }
      
      &.is-current {
        color: #303133;
        font-weight: 500;
        cursor: default;
      }
    }
    
    .separator {
      color: #909399;
    }
  }
  
  .folder-list {
    height: 300px;
    overflow-y: auto;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    
    .folder-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      cursor: pointer;
      transition: background 0.2s;
      
      &:hover {
        background: #f5f7fa;
      }
      
      .folder-icon {
        font-size: 24px;
        color: #e6a23c;
      }
      
      .folder-name {
        color: #303133;
      }
    }
  }
}
</style>
