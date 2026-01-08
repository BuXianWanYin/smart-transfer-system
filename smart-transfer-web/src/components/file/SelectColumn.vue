<template>
  <div class="select-column">
    <div class="title">显示列设置</div>
    <el-checkbox-group v-model="selectedColumns" @change="handleChange">
      <el-checkbox
        v-for="item in columnOptions"
        :key="item.value"
        :value="item.value"
        :disabled="item.disabled"
      >
        {{ item.label }}
      </el-checkbox>
    </el-checkbox-group>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => ['extendName', 'fileSize', 'updateTime']
  },
  fileType: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

// 列选项配置
const columnOptions = ref([
  { value: 'extendName', label: '类型', disabled: false },
  { value: 'fileSize', label: '大小', disabled: false },
  { value: 'updateTime', label: '修改日期', disabled: false },
  { value: 'deleteTime', label: '删除日期', disabled: false }
])

// 选中的列
const selectedColumns = ref([])

// 根据文件类型更新选项
const updateOptions = () => {
  columnOptions.value = columnOptions.value.map(item => {
    if (item.value === 'deleteTime') {
      // 只有回收站显示删除日期
      return { ...item, disabled: props.fileType !== 6 }
    }
    if (item.value === 'updateTime') {
      // 回收站不显示修改日期
      return { ...item, disabled: props.fileType === 6 }
    }
    return item
  })
}

// 处理变更
const handleChange = (values) => {
  emit('update:modelValue', values)
  emit('change', values)
  
  // 保存到本地存储
  localStorage.setItem('file_table_columns', JSON.stringify(values))
}

// 初始化
onMounted(() => {
  // 从本地存储加载
  const saved = localStorage.getItem('file_table_columns')
  if (saved) {
    try {
      selectedColumns.value = JSON.parse(saved)
    } catch {
      selectedColumns.value = [...props.modelValue]
    }
  } else {
    selectedColumns.value = [...props.modelValue]
  }
  
  updateOptions()
  emit('update:modelValue', selectedColumns.value)
})

// 监听 fileType 变化
watch(() => props.fileType, () => {
  updateOptions()
})

// 监听 modelValue 变化
watch(() => props.modelValue, (val) => {
  if (JSON.stringify(val) !== JSON.stringify(selectedColumns.value)) {
    selectedColumns.value = [...val]
  }
}, { deep: true })
</script>

<style lang="scss" scoped>
.select-column {
  .title {
    font-size: 14px;
    color: #606266;
    margin-bottom: 12px;
    font-weight: 500;
  }
  
  :deep(.el-checkbox-group) {
    display: flex;
    flex-direction: column;
    gap: 8px;
    
    .el-checkbox {
      margin-right: 0;
      height: auto;
      
      &.is-disabled {
        opacity: 0.5;
      }
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .select-column {
    .title {
      font-size: 13px;
      margin-bottom: 10px;
    }
    
    :deep(.el-checkbox-group) {
      gap: 6px;
      
      .el-checkbox__label {
        font-size: 13px;
      }
    }
  }
}
</style>

