<template>
  <!-- 移动文件-选择目标路径 -->
  <el-dialog
    v-model="visible"
    title="选择目标路径"
    :width="dialogWidth"
    :close-on-click-modal="false"
    @open="handleDialogOpen"
    :fullscreen="isMobile"
  >
    <div class="move-dialog-content">
      <!-- 选择的目标路径 -->
      <div class="target-path">
        <span class="label">目标路径：</span>
        <el-input
          class="content"
          v-model="targetPath"
          readonly
          size="small"
          placeholder="请选择目标文件夹"
        />
      </div>
      
      <!-- 文件目录树 -->
      <div class="tree-wrapper" v-loading="loading">
        <el-tree
          ref="treeRef"
          :data="fileTree"
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
              <el-button
                class="add-folder-btn"
                type="primary"
                link
                size="small"
                @click.stop="handleAddFolder(data)"
              >
                新建文件夹
              </el-button>
            </span>
          </template>
        </el-tree>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="confirmLoading" @click="handleConfirm">
        确定
      </el-button>
    </template>
    
    <!-- 新建文件夹对话框 -->
    <el-dialog
      v-model="addFolderVisible"
      title="新建文件夹"
      :width="isMobile ? '90%' : '400px'"
      append-to-body
    >
      <el-form ref="addFolderFormRef" :model="addFolderForm" :rules="addFolderRules">
        <el-form-item label="文件夹名称" prop="folderName">
          <el-input v-model="addFolderForm.folderName" placeholder="请输入文件夹名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addFolderVisible = false">取消</el-button>
        <el-button type="primary" :loading="addFolderLoading" @click="confirmAddFolder">
          确定
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { Folder } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getFolderTree, createFolder } from '@/api/folderApi'

const props = defineProps({
  modelValue: { type: Boolean, required: true }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 响应式
const screenWidth = ref(window.innerWidth)
const updateScreenWidth = () => { screenWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', updateScreenWidth))
onUnmounted(() => window.removeEventListener('resize', updateScreenWidth))
const isMobile = computed(() => screenWidth.value < 768)
const dialogWidth = computed(() => screenWidth.value < 768 ? '100%' : '500px')

// 树形组件配置
const treeRef = ref(null)
const treeProps = {
  children: 'children',
  label: 'label'
}

// 数据
const loading = ref(false)
const confirmLoading = ref(false)
const fileTree = ref([])
const defaultExpandedKeys = ref([0])
const targetPath = ref('/')
const selectedFolderId = ref(0)

// 新建文件夹
const addFolderVisible = ref(false)
const addFolderLoading = ref(false)
const addFolderFormRef = ref(null)
const addFolderForm = ref({ folderName: '' })
const addFolderRules = {
  folderName: [{ required: true, message: '请输入文件夹名称', trigger: 'blur' }]
}
const addFolderParentId = ref(0)

// 对话框打开时初始化
const handleDialogOpen = () => {
  targetPath.value = '/'
  selectedFolderId.value = 0
  initFileTree()
}

// 初始化文件夹树
const initFileTree = async () => {
  loading.value = true
  try {
    const tree = await getFolderTree()
    // 后端返回的就是完整的树结构
    fileTree.value = tree ? [tree] : []
    defaultExpandedKeys.value = [0]
  } catch (error) {
    ElMessage.error('加载文件夹失败')
    fileTree.value = []
  } finally {
    loading.value = false
  }
}

// 节点点击
const handleNodeClick = (data) => {
  targetPath.value = data.path || '/'
  selectedFolderId.value = data.id
  
  // 高亮选中节点
  nextTick(() => {
    treeRef.value?.setCurrentKey(data.id)
  })
}

// 新建文件夹按钮点击
const handleAddFolder = (data) => {
  addFolderParentId.value = data.id
  addFolderForm.value.folderName = ''
  addFolderVisible.value = true
}

// 确认新建文件夹
const confirmAddFolder = async () => {
  try {
    await addFolderFormRef.value.validate()
    addFolderLoading.value = true
    
    await createFolder({
      folderName: addFolderForm.value.folderName,
      parentId: addFolderParentId.value
    })
    
    ElMessage.success('文件夹创建成功')
    addFolderVisible.value = false
    
    // 刷新树
    await initFileTree()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '创建文件夹失败')
    }
  } finally {
    addFolderLoading.value = false
  }
}

// 确认移动
const handleConfirm = () => {
  // 传递完整的文件夹信息（ID和路径）
  emit('confirm', selectedFolderId.value, targetPath.value)
  visible.value = false
}
</script>

<style lang="scss" scoped>
.move-dialog-content {
  .target-path {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    
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
    border: 1px solid var(--art-border-color);
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
          background-color: rgb(var(--art-hoverColor));
          
          .add-folder-btn {
            display: inline-flex;
          }
        }
      }
      
      .el-tree-node.is-current > .el-tree-node__content {
        background-color: rgb(var(--art-bg-primary));
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
      padding-right: 8px;
      
      .folder-icon {
        font-size: 18px;
        color: #e6a23c;
        margin-right: 6px;
      }
      
      .label {
        flex: 1;
      }
      
      .add-folder-btn {
        display: none;
        font-size: 12px;
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .move-dialog-content {
    .target-path {
      flex-direction: column;
      align-items: flex-start;
      gap: 6px;
      
      .label {
        width: auto;
        font-size: 13px;
      }
      
      .content {
        width: 100%;
      }
    }
    
    .tree-wrapper {
      height: calc(100vh - 220px);
      min-height: 200px;
      
      .custom-tree-node {
        font-size: 13px;
        
        .add-folder-btn {
          display: inline-flex;
          font-size: 11px;
        }
      }
    }
  }
}
</style>
