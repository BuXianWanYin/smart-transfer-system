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
            <el-option label="TCP Reno 算法" value="RENO" />
            <el-option label="TCP Vegas 算法" value="VEGAS" />
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
        
        <el-divider content-position="left">自适应算法配置</el-divider>
        
        <el-form-item label="丢包率阈值" prop="lossRateThreshold">
          <el-input-number
            v-model="configForm.lossRateThreshold"
            :min="0.001"
            :max="0.1"
            :step="0.001"
            :precision="3"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（0.001-0.1，默认0.01即1%）</span>
        </el-form-item>
        
        <el-form-item label="RTT抖动阈值" prop="rttJitterThreshold">
          <el-input-number
            v-model="configForm.rttJitterThreshold"
            :min="10"
            :max="200"
            :step="5"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（毫秒，默认50ms）</span>
        </el-form-item>
        
        <el-form-item label="评估间隔" prop="evaluationInterval">
          <el-input-number
            v-model="configForm.evaluationInterval"
            :min="1000"
            :max="30000"
            :step="500"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（毫秒，默认5000ms）</span>
        </el-form-item>
        
        <el-form-item label="趋势窗口大小" prop="trendWindowSize">
          <el-input-number
            v-model="configForm.trendWindowSize"
            :min="3"
            :max="20"
            :step="1"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（评估窗口数量，默认5）</span>
        </el-form-item>
        
        <el-form-item label="趋势变化率阈值" prop="trendThreshold">
          <el-input-number
            v-model="configForm.trendThreshold"
            :min="0.05"
            :max="0.5"
            :step="0.01"
            :precision="2"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（0.05-0.5，默认0.1即10%）</span>
        </el-form-item>
        
        <el-form-item label="置信度阈值" prop="confidenceThreshold">
          <el-input-number
            v-model="configForm.confidenceThreshold"
            :min="0.05"
            :max="0.3"
            :step="0.01"
            :precision="2"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（0.05-0.3，默认0.1即10%）</span>
        </el-form-item>
        
        <el-form-item label="基准算法" prop="baselineAlgorithm">
          <el-select v-model="configForm.baselineAlgorithm" style="width: 200px">
            <el-option label="CUBIC" value="CUBIC" />
            <el-option label="Reno" value="Reno" />
            <el-option label="Vegas" value="Vegas" />
            <el-option label="BBR" value="BBR" />
          </el-select>
          <span class="form-hint" style="margin-left: 10px">（用于相对评分，默认CUBIC）</span>
        </el-form-item>
        
        <el-form-item label="预热RTT周期数" prop="warmupRttCount">
          <el-input-number
            v-model="configForm.warmupRttCount"
            :min="1"
            :max="10"
            :step="1"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（算法预热周期数，默认2）</span>
        </el-form-item>
        
        <el-form-item label="启用异常值过滤" prop="outlierFilterEnabled">
          <el-switch v-model="configForm.outlierFilterEnabled" />
          <span class="form-hint" style="margin-left: 10px">（是否启用RTT异常值过滤）</span>
        </el-form-item>
        
        <el-form-item label="回滚阈值" prop="rollbackThreshold">
          <el-input-number
            v-model="configForm.rollbackThreshold"
            :min="0.1"
            :max="0.5"
            :step="0.05"
            :precision="2"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（0.1-0.5，默认0.2即20%）</span>
        </el-form-item>
        
        <el-form-item label="最小切换间隔" prop="minSwitchInterval">
          <el-input-number
            v-model="configForm.minSwitchInterval"
            :min="5000"
            :max="60000"
            :step="1000"
            style="width: 200px"
          />
          <span class="form-hint" style="margin-left: 10px">（毫秒，默认10000ms）</span>
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
        <el-descriptions-item label="自适应算法配置">
          包含网络趋势分析、动态权重、算法预热、异常值过滤、回滚机制等优化功能
        </el-descriptions-item>
        <el-descriptions-item label="趋势窗口大小">
          用于网络趋势分析的评估窗口数量，建议5个窗口
        </el-descriptions-item>
        <el-descriptions-item label="置信度阈值">
          算法切换所需的最小得分差异，建议10%，避免频繁切换
        </el-descriptions-item>
        <el-descriptions-item label="回滚阈值">
          算法切换后性能下降超过此阈值时自动回滚，建议20%
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

// 实际提交的配置（从数据库加载，不设置默认值）
const configForm = ref({
  algorithm: null,
  initialCwnd: null,
  ssthresh: null,
  maxCwnd: null,
  minCwnd: null,
  // 自适应算法配置（从数据库加载）
  lossRateThreshold: null,
  rttJitterThreshold: null,
  evaluationInterval: null,
  trendWindowSize: null,
  trendThreshold: null,
  confidenceThreshold: null,
  baselineAlgorithm: null,
  warmupRttCount: null,
  outlierFilterEnabled: null,
  rollbackThreshold: null,
  minSwitchInterval: null
})

// 用户友好的显示表单（从数据库加载后填充）
const displayForm = ref({
  initialCwnd: null,
  initialCwndUnit: 'MB',
  ssthresh: null,
  ssthreshUnit: 'MB',
  maxCwnd: null,
  maxCwndUnit: 'MB',
  minCwnd: null,
  minCwndUnit: 'MB'
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
  if (value == null) return null
  const unit = displayForm.value[field + 'Unit']
  return Math.round(value * unitToBytes[unit])
}

onMounted(() => {
  loadConfig()
})

const loadConfig = async () => {
  try {
    const res = await getCongestionConfig()
    const config = res.data || res
    
    // 后端返回短横线格式：initial-cwnd，直接从数据库加载，不设置默认值
    configForm.value = {
      algorithm: config.algorithm || null,
      initialCwnd: config['initial-cwnd'] ? Number(config['initial-cwnd']) : null,
      ssthresh: config.ssthresh ? Number(config.ssthresh) : null,
      maxCwnd: config['max-cwnd'] ? Number(config['max-cwnd']) : null,
      minCwnd: config['min-cwnd'] ? Number(config['min-cwnd']) : null,
      // 自适应算法配置（从数据库加载）
      lossRateThreshold: config['loss-rate-threshold'] ? Number(config['loss-rate-threshold']) : null,
      rttJitterThreshold: config['rtt-jitter-threshold'] ? Number(config['rtt-jitter-threshold']) : null,
      evaluationInterval: config['evaluation-interval'] ? Number(config['evaluation-interval']) : null,
      trendWindowSize: config['trend-window-size'] ? Number(config['trend-window-size']) : null,
      trendThreshold: config['trend-threshold'] ? Number(config['trend-threshold']) : null,
      confidenceThreshold: config['confidence-threshold'] ? Number(config['confidence-threshold']) : null,
      baselineAlgorithm: config['baseline-algorithm'] || null,
      warmupRttCount: config['warmup-rtt-count'] ? Number(config['warmup-rtt-count']) : null,
      outlierFilterEnabled: config['outlier-filter-enabled'] !== undefined ? 
        (config['outlier-filter-enabled'] === 'true' || config['outlier-filter-enabled'] === true) : null,
      rollbackThreshold: config['rollback-threshold'] ? Number(config['rollback-threshold']) : null,
      minSwitchInterval: config['min-switch-interval'] ? Number(config['min-switch-interval']) : null
    }
    
    // 转换为用户友好的显示格式（只有非null值才转换）
    if (configForm.value.initialCwnd != null) {
      const initialCwndDisplay = bytesToDisplay(configForm.value.initialCwnd)
      displayForm.value.initialCwnd = initialCwndDisplay.value
      displayForm.value.initialCwndUnit = initialCwndDisplay.unit
    }
    if (configForm.value.ssthresh != null) {
      const ssthreshDisplay = bytesToDisplay(configForm.value.ssthresh)
      displayForm.value.ssthresh = ssthreshDisplay.value
      displayForm.value.ssthreshUnit = ssthreshDisplay.unit
    }
    if (configForm.value.maxCwnd != null) {
      const maxCwndDisplay = bytesToDisplay(configForm.value.maxCwnd)
      displayForm.value.maxCwnd = maxCwndDisplay.value
      displayForm.value.maxCwndUnit = maxCwndDisplay.unit
    }
    if (configForm.value.minCwnd != null) {
      const minCwndDisplay = bytesToDisplay(configForm.value.minCwnd)
      displayForm.value.minCwnd = minCwndDisplay.value
      displayForm.value.minCwndUnit = minCwndDisplay.unit
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
    
    // 将显示表单的值转换为字节数（只提交非null的配置项）
    const submitData = {}
    
    if (configForm.value.algorithm != null) {
      submitData.algorithm = configForm.value.algorithm
    }
    
    const initialCwndBytes = convertToBytes('initialCwnd')
    if (initialCwndBytes != null) submitData['initial-cwnd'] = initialCwndBytes
    
    const ssthreshBytes = convertToBytes('ssthresh')
    if (ssthreshBytes != null) submitData.ssthresh = ssthreshBytes
    
    const maxCwndBytes = convertToBytes('maxCwnd')
    if (maxCwndBytes != null) submitData['max-cwnd'] = maxCwndBytes
    
    const minCwndBytes = convertToBytes('minCwnd')
    if (minCwndBytes != null) submitData['min-cwnd'] = minCwndBytes
    
    // 自适应算法配置（使用短横线格式，只提交非null值）
    if (configForm.value.lossRateThreshold != null) {
      submitData['loss-rate-threshold'] = configForm.value.lossRateThreshold
    }
    if (configForm.value.rttJitterThreshold != null) {
      submitData['rtt-jitter-threshold'] = configForm.value.rttJitterThreshold
    }
    if (configForm.value.evaluationInterval != null) {
      submitData['evaluation-interval'] = configForm.value.evaluationInterval
    }
    if (configForm.value.trendWindowSize != null) {
      submitData['trend-window-size'] = configForm.value.trendWindowSize
    }
    if (configForm.value.trendThreshold != null) {
      submitData['trend-threshold'] = configForm.value.trendThreshold
    }
    if (configForm.value.confidenceThreshold != null) {
      submitData['confidence-threshold'] = configForm.value.confidenceThreshold
    }
    if (configForm.value.baselineAlgorithm != null) {
      submitData['baseline-algorithm'] = configForm.value.baselineAlgorithm
    }
    if (configForm.value.warmupRttCount != null) {
      submitData['warmup-rtt-count'] = configForm.value.warmupRttCount
    }
    if (configForm.value.outlierFilterEnabled != null) {
      submitData['outlier-filter-enabled'] = configForm.value.outlierFilterEnabled
    }
    if (configForm.value.rollbackThreshold != null) {
      submitData['rollback-threshold'] = configForm.value.rollbackThreshold
    }
    if (configForm.value.minSwitchInterval != null) {
      submitData['min-switch-interval'] = configForm.value.minSwitchInterval
    }
    
    await updateCongestionConfig(submitData)
    
    // 重新加载配置（从数据库获取最新值）
    await loadConfig()
    
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

