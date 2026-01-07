<template>
  <el-dialog
    v-model="visible"
    title="解压文件"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="unzip-dialog-content">
      <div class="file-info">
        <el-icon class="file-icon"><Files /></el-icon>
        <div class="file-details">
          <span class="file-name">{{ file?.fileName }}</span>
          <span class="file-size">{{ formatSize(file?.fileSize) }}</span>
        </div>
      </div>
      
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="解压方式" prop="unzipMode">
          <el-radio-group v-model="form.unzipMode">
            <el-radio :value="1">解压到当前文件夹</el-radio>
            <el-radio :value="2">解压到新文件夹</el-radio>
            <el-radio :value="3">解压到指定路径</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item
          v-if="form.unzipMode === 2"
          label="文件夹名称"
          prop="folderName"
        >
          <el-input v-model="form.folderName" placeholder="请输入文件夹名称" />
        </el-form-item>
        
        <el-form-item
          v-if="form.unzipMode === 3"
          label="目标路径"
          prop="targetPath"
        >
          <el-input v-model="form.targetPath" readonly placeholder="点击选择目标路径">
            <template #append>
              <el-button @click="selectTargetPath">选择</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleUnzip">
        开始解压
      </el-button>
    </template>
    
    <!-- 路径选择对话框 -->
    <MoveFileDialog
      v-model="selectPathVisible"
      @confirm="handlePathSelect"
    />
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Files } from '@element-plus/icons-vue'
import MoveFileDialog from './MoveFileDialog.vue'
import { unzipFile } from '@/api/fileApi'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  file: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const formRef = ref(null)
const loading = ref(false)
const selectPathVisible = ref(false)

const form = reactive({
  unzipMode: 1,
  folderName: '',
  targetPath: '/',
  targetFolderId: 0
})

const rules = {
  folderName: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' }
  ],
  targetPath: [
    { required: true, message: '请选择目标路径', trigger: 'change' }
  ]
}

// 格式化文件大小
const formatSize = (size) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 选择目标路径
const selectTargetPath = () => {
  selectPathVisible.value = true
}

// 处理路径选择
const handlePathSelect = (folderId) => {
  form.targetFolderId = folderId
  form.targetPath = folderId === 0 ? '/' : `文件夹ID: ${folderId}`
}

// 解压文件
const handleUnzip = async () => {
  if (!props.file) return
  
  // 验证表单
  if (form.unzipMode === 2 && !form.folderName) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  
  if (form.unzipMode === 3 && !form.targetPath) {
    ElMessage.warning('请选择目标路径')
    return
  }
  
  loading.value = true
  try {
    await unzipFile({
      fileId: props.file.id,
      unzipMode: form.unzipMode,
      folderName: form.unzipMode === 2 ? form.folderName : null,
      targetFolderId: form.unzipMode === 3 ? form.targetFolderId : null
    })
    
    ElMessage.success('解压成功')
    visible.value = false
    emit('success')
  } catch (error) {
    ElMessage.error(error.message || '解压失败')
  } finally {
    loading.value = false
  }
}

// 监听文件变化，设置默认文件夹名
watch(() => props.file, (file) => {
  if (file) {
    const name = file.fileName || ''
    const dotIndex = name.lastIndexOf('.')
    form.folderName = dotIndex > 0 ? name.substring(0, dotIndex) : name
  }
}, { immediate: true })

// 重置表单
watch(visible, (val) => {
  if (val) {
    form.unzipMode = 1
    form.targetPath = '/'
    form.targetFolderId = 0
  }
})
</script>

<style lang="scss" scoped>
.unzip-dialog-content {
  .file-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #fafbfc;
    border-radius: 8px;
    border: 1px solid #ebeef5;
    margin-bottom: 20px;
    
    .file-icon {
      font-size: 36px;
      color: #e6a23c;
    }
    
    .file-details {
      .file-name {
        display: block;
        font-size: 14px;
        color: #303133;
        margin-bottom: 4px;
      }
      
      .file-size {
        font-size: 12px;
        color: #909399;
      }
    }
  }
  
  :deep(.el-radio) {
    display: block;
    margin-bottom: 8px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}
</style>

