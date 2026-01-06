<template>
  <div class="file-upload-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件上传</span>
          <el-tag v-if="currentAlgorithm" type="primary">
            当前算法: {{ currentAlgorithm }}
          </el-tag>
        </div>
      </template>
      
      <FileUploader />
    </el-card>
    
    <!-- 上传统计 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-statistic title="上传中" :value="uploadingCount">
          <template #suffix>个</template>
        </el-statistic>
      </el-col>
      <el-col :span="8">
        <el-statistic title="已完成" :value="completedCount" value-style="color: #67c23a">
          <template #suffix>个</template>
        </el-statistic>
      </el-col>
      <el-col :span="8">
        <el-statistic title="失败" :value="failedCount" value-style="color: #f56c6c">
          <template #suffix>个</template>
        </el-statistic>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useFileStore } from '@/store/fileStore'
import { useCongestionStore } from '@/store/congestionStore'
import FileUploader from '@/components/FileUploader.vue'

const fileStore = useFileStore()
const congestionStore = useCongestionStore()

const currentAlgorithm = computed(() => congestionStore.currentAlgorithm)
const uploadingCount = computed(() => fileStore.uploadingFiles.length)
const completedCount = computed(() => fileStore.completedFiles.length)
const failedCount = computed(() => fileStore.failedFiles.length)
</script>

<style scoped>
.file-upload-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

