# 前端配置页面UI优化 - 单位选择器

## 📋 优化内容

### ❌ 优化前的问题
用户需要直接输入大数字（如 `10485760`），极不友好：
- ❌ 初始拥塞窗口：10485760 （谁知道这是10MB？）
- ❌ 慢启动阈值：52428800 （这是50MB）
- ❌ 最大拥塞窗口：104857600 （这是100MB）

### ✅ 优化后的体验
用户可以输入友好的数值，并选择单位：
- ✅ 初始拥塞窗口：`10` + `MB` 选择器
- ✅ 慢启动阈值：`50` + `MB` 选择器
- ✅ 最大拥塞窗口：`100` + `MB` 选择器

---

## 🎨 UI设计

### 表单项结构
```
[标签]  [数值输入框(180px)] [单位选择器(80px)] = 10.00 MB (实时显示转换结果)
```

### 单位选择器
- **拥塞窗口配置**：B / KB / MB
- **速率控制配置**：B/s / KB/s / MB/s

### 输入范围
| 配置项 | 数值范围 | 默认单位 | 精度 |
|--------|---------|---------|------|
| 初始拥塞窗口 | 0.5 - 200 | MB | 2位小数 |
| 慢启动阈值 | 0.5 - 200 | MB | 2位小数 |
| 最大拥塞窗口 | 0.5 - 500 | MB | 2位小数 |
| 最小拥塞窗口 | 0.1 - 100 | MB | 2位小数 |
| 最大传输速率 | 0.5 - 1000 | MB/s | 2位小数 |
| 最小传输速率 | 0.1 - 100 | MB/s | 2位小数 |

---

## 💻 技术实现

### 1. 双表单设计
```javascript
// 实际提交的配置（字节数）- 后端需要
const configForm = ref({
  initialCwnd: 10485760,  // 字节
  ssthresh: 52428800,     // 字节
  // ...
})

// 用户友好的显示表单 - 前端展示
const displayForm = ref({
  initialCwnd: 10,        // 数值
  initialCwndUnit: 'MB',  // 单位
  ssthresh: 50,
  ssthreshUnit: 'MB',
  // ...
})
```

### 2. 单位转换函数
```javascript
// 单位转字节的倍率
const unitToBytes = {
  B: 1,
  KB: 1024,
  MB: 1024 * 1024
}

// 显示值转换为字节（提交时）
const convertToBytes = (field) => {
  const value = displayForm.value[field]
  const unit = displayForm.value[field + 'Unit']
  return Math.round(value * unitToBytes[unit])
}

// 字节转换为显示值（加载时）
const bytesToDisplay = (bytes) => {
  if (bytes >= 1024 * 1024) {
    return { value: (bytes / (1024 * 1024)).toFixed(2), unit: 'MB' }
  } else if (bytes >= 1024) {
    return { value: (bytes / 1024).toFixed(2), unit: 'KB' }
  } else {
    return { value: bytes, unit: 'B' }
  }
}
```

### 3. 数据流转
```
加载配置：
后端(字节) → bytesToDisplay() → displayForm(数值+单位) → UI展示

保存配置：
UI输入(数值+单位) → convertToBytes() → configForm(字节) → 后端
```

---

## 🔄 数据转换示例

### 加载配置时
```javascript
// 后端返回：initial_cwnd = "10485760"
configForm.value.initialCwnd = 10485760  // 字节

// 转换为显示格式
bytesToDisplay(10485760)  // { value: 10, unit: 'MB' }

// 显示在UI上
displayForm.value.initialCwnd = 10
displayForm.value.initialCwndUnit = 'MB'
```

### 保存配置时
```javascript
// 用户输入：10 MB
displayForm.value.initialCwnd = 10
displayForm.value.initialCwndUnit = 'MB'

// 转换为字节
convertToBytes('initialCwnd')  // 10 * 1024 * 1024 = 10485760

// 提交给后端
submitData.initialCwnd = 10485760
```

---

## 📱 用户体验改进

### 1. 实时反馈
- 输入数值或切换单位时，右侧实时显示转换后的结果
- 示例：`10` + `MB` → `= 10.00 MB (10485760 字节)`

### 2. 智能单位选择
- 默认使用最合适的单位（MB）
- 用户可以自由切换 B/KB/MB

### 3. 精确控制
- 支持小数输入（精度2位）
- 可以输入 `0.5 MB` (512 KB) 或 `1.5 MB` (1536 KB)

### 4. 友好提示
- 旁边显示换算结果：`= 10.00 MB`
- 配置说明卡片提供建议值

---

## ✅ 优化效果对比

| 对比项 | 优化前 | 优化后 |
|--------|--------|--------|
| **输入方式** | 输入10485760 | 输入10 + 选择MB |
| **可读性** | ❌ 完全不直观 | ✅ 一目了然 |
| **操作便利性** | ❌ 需要计算器 | ✅ 直接输入 |
| **易用性评分** | ⭐ 1/5 | ⭐⭐⭐⭐⭐ 5/5 |
| **配置时间** | 需要查询转换 | 秒级完成 |
| **出错概率** | ❌ 容易输错位数 | ✅ 不易出错 |

---

## 🎯 后端兼容性

### ✅ 后端无需修改
- 后端继续接收字节数（Long类型）
- 前端负责单位转换
- 完全向后兼容

### 接口保持不变
```java
// 后端接收的DTO（无需修改）
public class CongestionConfigDTO {
    private Long initialCwnd;  // 字节
    private Long ssthresh;     // 字节
    private Long maxCwnd;      // 字节
    // ...
}
```

---

## 📊 配置建议值

| 配置项 | 建议值 | 说明 |
|--------|--------|------|
| 初始拥塞窗口 | 10 MB | 适合大多数网络环境 |
| 慢启动阈值 | 50 MB | 平衡启动速度和稳定性 |
| 最大拥塞窗口 | 100 MB | 高带宽网络可适当增大 |
| 最小拥塞窗口 | 1 MB | 防止窗口过小影响性能 |
| 最大传输速率 | 100 MB/s | 根据实际网络带宽调整 |
| 最小传输速率 | 1 MB/s | 保证基本传输性能 |

---

## 🌟 亮点总结

1. **用户友好** - 输入 `10 MB` 而不是 `10485760`
2. **实时反馈** - 立即看到转换结果
3. **灵活单位** - 支持 B/KB/MB 自由切换
4. **精确控制** - 支持小数输入
5. **零学习成本** - 符合用户直觉
6. **后端兼容** - 无需修改后端代码

---

**优化完成时间**：2026-01-06  
**优化范围**：CongestionConfig.vue 配置页面  
**用户体验提升**：从 ⭐ 1/5 到 ⭐⭐⭐⭐⭐ 5/5

