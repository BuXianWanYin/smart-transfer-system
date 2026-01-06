<template>
  <div class="congestion-config-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>拥塞控制配置</span>
          <div>
            <el-button @click="loadConfig">刷新</el-button>
            <el-button type="primary" @click="handleSave">保存配置</el-button>
          </div>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="configForm"
        :rules="rules"
        label-width="150px"
      >
        <el-form-item label="算法选择" prop="algorithm">
          <el-select v-model="configForm.algorithm" placeholder="请选择算法">
            <el-option label="CUBIC 算法" value="CUBIC" />
            <el-option label="BBR 算法" value="BBR" />
            <el-option label="自适应算法" value="ADAPTIVE" />
          </el-select>
        </el-form-item>
        
        <el-divider content-position="left">拥塞窗口配置</el-divider>
        
        <el-form-item label="初始拥塞窗口" prop="initialCwnd">
          <el-input-number
            v-model="configForm.initialCwnd"
            :min="1048576"
            :max="104857600"
            :step="1048576"
          />
          <span class="form-hint">{{ formatFileSize(configForm.initialCwnd) }}</span>
        </el-form-item>
        
        <el-form-item label="慢启动阈值" prop="ssthresh">
          <el-input-number
            v-model="configForm.ssthresh"
            :min="1048576"
            :max="104857600"
            :step="1048576"
          />
          <span class="form-hint">{{ formatFileSize(configForm.ssthresh) }}</span>
        </el-form-item>
        
        <el-form-item label="最大拥塞窗口" prop="maxCwnd">
          <el-input-number
            v-model="configForm.maxCwnd"
            :min="1048576"
            :max="209715200"
            :step="1048576"
          />
          <span class="form-hint">{{ formatFileSize(configForm.maxCwnd) }}</span>
        </el-form-item>
        
        <el-form-item label="最小拥塞窗口" prop="minCwnd">
          <el-input-number
            v-model="configForm.minCwnd"
            :min="524288"
            :max="10485760"
            :step="524288"
          />
          <span class="form-hint">{{ formatFileSize(configForm.minCwnd) }}</span>
        </el-form-item>
        
        <el-divider content-position="left">速率控制配置</el-divider>
        
        <el-form-item label="最大传输速率" prop="maxRate">
          <el-input-number
            v-model="configForm.maxRate"
            :min="1048576"
            :max="1073741824"
            :step="1048576"
          />
          <span class="form-hint">{{ formatSpeed(configForm.maxRate) }}</span>
        </el-form-item>
        
        <el-form-item label="最小传输速率" prop="minRate">
          <el-input-number
            v-model="configForm.minRate"
            :min="524288"
            :max="10485760"
            :step="524288"
          />
          <span class="form-hint">{{ formatSpeed(configForm.minRate) }}</span>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 配置说明 -->
    <el-card style="margin-top: 20px">
      <template #header>配置说明</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="CUBIC 算法">
          Linux内核默认算法，适用于高带宽长距离网络，具有良好的公平性
        </el-descriptions-item>
        <el-descriptions-item label="BBR 算法">
          Google开发的算法，基于带宽和RTT建模，适用于高丢包率网络
        </el-descriptions-item>
        <el-descriptions-item label="自适应算法">
          根据网络质量自动切换CUBIC和BBR，适应不同网络环境
        </el-descriptions-item>
        <el-descriptions-item label="初始拥塞窗口">
          传输开始时的拥塞窗口大小，建议10MB
        </el-descriptions-item>
        <el-descriptions-item label="慢启动阈值">
          从慢启动切换到拥塞避免的阈值，建议50MB
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCongestionConfig, updateCongestionConfig } from '@/api/configApi'
import { useConfigStore } from '@/store/configStore'
import { formatFileSize, formatSpeed } from '@/utils/file'

const configStore = useConfigStore()
const formRef = ref()
const configForm = ref({
  algorithm: 'CUBIC',
  initialCwnd: 10485760,
  ssthresh: 52428800,
  maxCwnd: 104857600,
  minCwnd: 1048576,
  maxRate: 104857600,
  minRate: 1048576
})

const rules = {
  algorithm: [
    { required: true, message: '请选择算法', trigger: 'change' }
  ],
  initialCwnd: [
    { required: true, message: '请输入初始拥塞窗口', trigger: 'blur' }
  ],
  ssthresh: [
    { required: true, message: '请输入慢启动阈值', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadConfig()
})

const loadConfig = async () => {
  try {
    const res = await getCongestionConfig()
    const config = res.data
    
    // 将字符串转换为数字
    configForm.value = {
      algorithm: config.algorithm || 'CUBIC',
      initialCwnd: Number(config['initial-cwnd']) || 10485760,
      ssthresh: Number(config.ssthresh) || 52428800,
      maxCwnd: Number(config['max-cwnd']) || 104857600,
      minCwnd: Number(config['min-cwnd']) || 1048576,
      maxRate: Number(config['max-rate']) || 104857600,
      minRate: Number(config['min-rate']) || 1048576
    }
    
    configStore.updateCongestionConfig(configForm.value)
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    
    await updateCongestionConfig(configForm.value)
    configStore.updateCongestionConfig(configForm.value)
    ElMessage.success('配置保存成功')
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存配置失败')
    }
  }
}
</script>

<style scoped>
.congestion-config-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 14px;
}
</style>

