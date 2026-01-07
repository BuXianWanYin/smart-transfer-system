<template>
  <el-dialog
    v-model="visible"
    title="复制到"
    width="500px"
    :close-on-click-modal="false"
    @open="handleDialogOpen"
  >
    <div class="copy-dialog-content">
      <!-- 要复制的文件信息 -->
      <div class="file-info" v-if="files.length === 1">
        <el-icon class="file-icon">
          <Folder v-if="files[0]?.isDir === 1" />
          <Document v-else />
        </el-icon>
        <span class="file-name">{{ files[0]?.fileName || files[0]?.folderName }}</span>
      </div>
      <div class="file-info" v-else>
        <el-icon class="file-icon"><Files /></el-icon>
        <span class="file-name">已选择 {{ files.length }} 个文件</span>
      </div>
      
      <!-- 目标路径显示 -->
      <div class="target-path">
        <span class="label">目标路径：</span>
        <el-input
          class="content"
          v-model="selectedPath"
          readonly
          size="small"
          placeholder="请选择目标文件夹"
        />
      </div>
      
      <!-- 文件夹目录树 -->
      <div class="tree-wrapper" v-loading="loading">
        <el-tree
          ref="treeRef"
          :data="folderTree"
          :props="treeProps"
          :highlight-current="true"
          :expand-on-click-node="false"
          :default-expanded-keys="defaultExpandedKeys"
          node-key="id"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <el-icon class="folder-icon"><Folder /></el-icon>
              <span class="label">{{ node.label }}</span>
            </span>
          </template>
        </el-tree>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="copyLoading" @click="handleCopy">
        复制到此处
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Folder, Document, Files } from '@element-plus/icons-vue'
import { getFolderTree } from '@/api/folderApi'
import { copyFile, batchCopyFiles } from '@/api/fileApi'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  files: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

// 树形组件配置
const treeRef = ref(null)
const treeProps = {
  children: 'children',
  label: 'label'
}

// 数据
const loading = ref(false)
const copyLoading = ref(false)
const folderTree = ref([])
const defaultExpandedKeys = ref([0])
const selectedPath = ref('/')
const selectedFolderId = ref(0)

// 对话框打开时初始化
const handleDialogOpen = () => {
  selectedPath.value = '/'
  selectedFolderId.value = 0
  loadFolderTree()
}

// 加载文件夹树
const loadFolderTree = async () => {
  loading.value = true
  try {
    const tree = await getFolderTree()
    // 后端返回的就是完整的树结构
    folderTree.value = tree ? [tree] : []
    defaultExpandedKeys.value = [0]
  } catch (error) {
    ElMessage.error('加载文件夹失败')
    folderTree.value = []
  } finally {
    loading.value = false
  }
}

// 节点点击
const handleNodeClick = (data) => {
  selectedPath.value = data.path || '/'
  selectedFolderId.value = data.id
  
  // 高亮选中节点
  nextTick(() => {
    treeRef.value?.setCurrentKey(data.id)
  })
}

// 复制文件
const handleCopy = async () => {
  if (props.files.length === 0) {
    ElMessage.warning('请选择要复制的文件')
    return
  }
  
  copyLoading.value = true
  try {
    if (props.files.length === 1) {
      // 单个文件复制
      await copyFile({
        fileId: props.files[0].id,
        targetFolderId: selectedFolderId.value
      })
    } else {
      // 批量复制
      await batchCopyFiles({
        fileIds: props.files.map(f => f.id),
        targetFolderId: selectedFolderId.value
      })
    }
    
    ElMessage.success('复制成功')
    visible.value = false
    emit('success')
  } catch (error) {
    ElMessage.error(error.message || '复制失败')
  } finally {
    copyLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.copy-dialog-content {
  .file-info {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 6px;
    margin-bottom: 16px;
    
    .file-icon {
      font-size: 24px;
      color: #e6a23c;
    }
    
    .file-name {
      font-size: 14px;
      color: #303133;
    }
  }
  
  .target-path {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
    
    .label {
      width: 80px;
      color: #606266;
      flex-shrink: 0;
    }
    
    .content {
      flex: 1;
    }
  }
  
  .tree-wrapper {
    height: 300px;
    overflow: auto;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 8px;
    
    &::-webkit-scrollbar {
      width: 6px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #c0c4cc;
      border-radius: 3px;
    }
    
    :deep(.el-tree) {
      background: transparent;
      
      .el-tree-node__content {
        height: 36px;
        
        &:hover {
          background-color: #f5f7fa;
        }
      }
      
      .el-tree-node.is-current > .el-tree-node__content {
        background-color: #ecf5ff;
        color: var(--el-color-primary);
        
        .folder-icon {
          color: var(--el-color-primary);
        }
      }
    }
    
    .custom-tree-node {
      flex: 1;
      display: flex;
      align-items: center;
      font-size: 14px;
      
      .folder-icon {
        font-size: 18px;
        color: #e6a23c;
        margin-right: 6px;
      }
    }
  }
}
</style>

