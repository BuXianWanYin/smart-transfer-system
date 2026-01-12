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
          <div class="input-with-unit">
            <el-input-number
              v-model="displayForm.initialCwnd"
              :min="0.5"
              :max="200"
              :step="0.5"
              :precision="2"
              style="width: 180px"
            />
            <el-select v-model="displayForm.initialCwndUnit" style="width: 80px; margin-left: 10px">
              <el-option label="B" value="B" />
              <el-option label="KB" value="KB" />
              <el-option label="MB" value="MB" />
            </el-select>
            <span class="form-hint">= {{ formatFileSize(convertToBytes('initialCwnd')) }}</span>
          </div>
        </el-form-item>
        
        <el-form-item label="慢启动阈值" prop="ssthresh">
          <div class="input-with-unit">
            <el-input-number
              v-model="displayForm.ssthresh"
              :min="0.5"
              :max="200"
              :step="0.5"
              :precision="2"
              style="width: 180px"
            />
            <el-select v-model="displayForm.ssthreshUnit" style="width: 80px; margin-left: 10px">
              <el-option label="B" value="B" />
              <el-option label="KB" value="KB" />
              <el-option label="MB" value="MB" />
            </el-select>
            <span class="form-hint">= {{ formatFileSize(convertToBytes('ssthresh')) }}</span>
          </div>
        </el-form-item>
        
        <el-form-item label="最大拥塞窗口" prop="maxCwnd">
          <div class="input-with-unit">
            <el-input-number
              v-model="displayForm.maxCwnd"
              :min="0.5"
              :max="500"
              :step="0.5"
              :precision="2"
              style="width: 180px"
            />
            <el-select v-model="displayForm.maxCwndUnit" style="width: 80px; margin-left: 10px">
              <el-option label="B" value="B" />
              <el-option label="KB" value="KB" />
              <el-option label="MB" value="MB" />
            </el-select>
            <span class="form-hint">= {{ formatFileSize(convertToBytes('maxCwnd')) }}</span>
          </div>
        </el-form-item>
        
        <el-form-item label="最小拥塞窗口" prop="minCwnd">
          <div class="input-with-unit">
            <el-input-number
              v-model="displayForm.minCwnd"
              :min="0.1"
              :max="100"
              :step="0.5"
              :precision="2"
              style="width: 180px"
            />
            <el-select v-model="displayForm.minCwndUnit" style="width: 80px; margin-left: 10px">
              <el-option label="B" value="B" />
              <el-option label="KB" value="KB" />
              <el-option label="MB" value="MB" />
            </el-select>
            <span class="form-hint">= {{ formatFileSize(convertToBytes('minCwnd')) }}</span>
          </div>
        </el-form-item>
        
        <el-divider content-position="left">速率控制配置</el-divider>
        
        <el-form-item label="最大传输速率" prop="maxRate">
          <div class="input-with-unit">
            <el-input-number
              v-model="displayForm.maxRate"
              :min="0.5"
              :max="1000"
              :step="0.5"
              :precision="2"
              style="width: 180px"
            />
            <el-select v-model="displayForm.maxRateUnit" style="width: 80px; margin-left: 10px">
              <el-option label="B/s" value="B" />
              <el-option label="KB/s" value="KB" />
              <el-option label="MB/s" value="MB" />
            </el-select>
            <span class="form-hint">= {{ formatSpeed(convertToBytes('maxRate')) }}</span>
          </div>
        </el-form-item>
        
        <el-form-item label="最小传输速率" prop="minRate">
          <div class="input-with-unit">
            <el-input-number
              v-model="displayForm.minRate"
              :min="0.1"
              :max="100"
              :step="0.5"
              :precision="2"
              style="width: 180px"
            />
            <el-select v-model="displayForm.minRateUnit" style="width: 80px; margin-left: 10px">
              <el-option label="B/s" value="B" />
              <el-option label="KB/s" value="KB" />
              <el-option label="MB/s" value="MB" />
            </el-select>
            <span class="form-hint">= {{ formatSpeed(convertToBytes('minRate')) }}</span>
          </div>
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

// 实际提交的配置（字节数）
const configForm = ref({
  algorithm: 'CUBIC',
  initialCwnd: 10485760,
  ssthresh: 52428800,
  maxCwnd: 104857600,
  minCwnd: 1048576,
  maxRate: 104857600,
  minRate: 1048576
})

// 用户友好的显示表单
const displayForm = ref({
  initialCwnd: 10,
  initialCwndUnit: 'MB',
  ssthresh: 50,
  ssthreshUnit: 'MB',
  maxCwnd: 100,
  maxCwndUnit: 'MB',
  minCwnd: 1,
  minCwndUnit: 'MB',
  maxRate: 100,
  maxRateUnit: 'MB',
  minRate: 1,
  minRateUnit: 'MB'
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

// 单位转换：将数值和单位转换为字节
const unitToBytes = {
  B: 1,
  KB: 1024,
  MB: 1024 * 1024
}

// 将字节转换为友好显示格式
const bytesToDisplay = (bytes) => {
  if (!bytes || bytes === 0) return { value: 0, unit: 'MB' }
  
  if (bytes >= 1024 * 1024) {
    return { value: Number((bytes / (1024 * 1024)).toFixed(2)), unit: 'MB' }
  } else if (bytes >= 1024) {
    return { value: Number((bytes / 1024).toFixed(2)), unit: 'KB' }
  } else {
    return { value: bytes, unit: 'B' }
  }
}

// 将显示表单的值转换为字节
const convertToBytes = (field) => {
  const value = displayForm.value[field]
  const unit = displayForm.value[field + 'Unit']
  return Math.round(value * unitToBytes[unit])
}

onMounted(() => {
  loadConfig()
})

const loadConfig = async () => {
  try {
    const res = await getCongestionConfig()
    const config = res
    
    // 后端返回短横线格式：initial-cwnd
    configForm.value = {
      algorithm: config.algorithm || 'CUBIC',
      initialCwnd: Number(config['initial-cwnd']) || 10485760,
      ssthresh: Number(config.ssthresh) || 52428800,
      maxCwnd: Number(config['max-cwnd']) || 104857600,
      minCwnd: Number(config['min-cwnd']) || 1048576,
      maxRate: Number(config['max-rate']) || 104857600,
      minRate: Number(config['min-rate']) || 1048576
    }
    
    // 转换为用户友好的显示格式
    const initialCwndDisplay = bytesToDisplay(configForm.value.initialCwnd)
    const ssthreshDisplay = bytesToDisplay(configForm.value.ssthresh)
    const maxCwndDisplay = bytesToDisplay(configForm.value.maxCwnd)
    const minCwndDisplay = bytesToDisplay(configForm.value.minCwnd)
    const maxRateDisplay = bytesToDisplay(configForm.value.maxRate)
    const minRateDisplay = bytesToDisplay(configForm.value.minRate)
    
    displayForm.value = {
      initialCwnd: initialCwndDisplay.value,
      initialCwndUnit: initialCwndDisplay.unit,
      ssthresh: ssthreshDisplay.value,
      ssthreshUnit: ssthreshDisplay.unit,
      maxCwnd: maxCwndDisplay.value,
      maxCwndUnit: maxCwndDisplay.unit,
      minCwnd: minCwndDisplay.value,
      minCwndUnit: minCwndDisplay.unit,
      maxRate: maxRateDisplay.value,
      maxRateUnit: maxRateDisplay.unit,
      minRate: minRateDisplay.value,
      minRateUnit: minRateDisplay.unit
    }
    
    configStore.updateCongestionConfig(configForm.value)
    ElMessage.success('配置加载成功')
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    
    // 将显示表单的值转换为字节数
    const submitData = {
      algorithm: configForm.value.algorithm,
      initialCwnd: convertToBytes('initialCwnd'),
      ssthresh: convertToBytes('ssthresh'),
      maxCwnd: convertToBytes('maxCwnd'),
      minCwnd: convertToBytes('minCwnd'),
      maxRate: convertToBytes('maxRate'),
      minRate: convertToBytes('minRate')
    }
    
    await updateCongestionConfig(submitData)
    
    // 更新configForm为新的字节值
    configForm.value = submitData
    configStore.updateCongestionConfig(submitData)
    
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
  flex-wrap: wrap;
  gap: 10px;
}

.input-with-unit {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.form-hint {
  margin-left: 15px;
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

/* 平板适配 */
@media (max-width: 1024px) {
  .congestion-config-page {
    padding: 16px;
  }
  
  :deep(.el-form-item__label) {
    width: 120px !important;
  }
  
  .input-with-unit {
    :deep(.el-input-number) {
      width: 140px !important;
    }
  }
  
  .form-hint {
    font-size: 12px;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .congestion-config-page {
    padding: 12px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    
    span {
      font-size: 16px;
      margin-bottom: 8px;
    }
  }
  
  :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left !important;
    padding-bottom: 4px !important;
  }
  
  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
  
  .input-with-unit {
    flex-direction: column;
    align-items: flex-start;
    width: 100%;
    
    :deep(.el-input-number) {
      width: 100% !important;
    }
    
    :deep(.el-select) {
      width: 100% !important;
      margin-left: 0 !important;
      margin-top: 8px;
    }
  }
  
  .form-hint {
    margin-left: 0;
    margin-top: 4px;
    font-size: 12px;
  }
  
  :deep(.el-descriptions) {
    .el-descriptions-item__label {
      width: 100px;
      min-width: 100px;
    }
  }
}
</style>

