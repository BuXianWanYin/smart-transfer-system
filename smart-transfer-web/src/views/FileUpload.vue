<template>
  <div class="file-upload-page page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-title">文件上传</div>
      <div class="page-description">支持大文件分片上传、断点续传和秒传功能</div>
    </div>
    
    <!-- 上传统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-title">当前算法</div>
          <div class="stat-card-value text-primary">{{ currentAlgorithm || '-' }}</div>
          <div class="stat-card-trend">拥塞控制算法</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="border-left-color: rgb(var(--art-info))">
          <div class="stat-card-title">上传中</div>
          <div class="stat-card-value text-info">{{ uploadingCount }}</div>
          <div class="stat-card-trend">个文件正在上传</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="border-left-color: rgb(var(--art-success))">
          <div class="stat-card-title">已完成</div>
          <div class="stat-card-value text-success">{{ completedCount }}</div>
          <div class="stat-card-trend">个文件上传成功</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="border-left-color: rgb(var(--art-danger))">
          <div class="stat-card-title">失败</div>
          <div class="stat-card-value text-danger">{{ failedCount }}</div>
          <div class="stat-card-trend">个文件上传失败</div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 上传组件 -->
    <el-card class="upload-card">
      <FileUploader />
    </el-card>
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
.stat-cards {
  margin-bottom: 20px;
}

.upload-card {
  margin-top: 20px;
}
</style>

