<template>
  <teleport to="body">
    <transition name="fade">
      <div class="code-preview-wrapper" v-show="visible" @keydown.s.ctrl.prevent="handleSave">
        <!-- 顶部工具栏 -->
        <div class="toolbar" v-if="visible">
          <div class="toolbar-left">
            <span class="file-name" :title="fileName">{{ fileName }}</span>
            <span class="unsaved-tip" v-if="isModified && !readOnly">（未保存）</span>
          </div>
          
          <div class="toolbar-center">
            <span class="preview-type">在线预览{{ readOnly ? '' : ' & 编辑' }}</span>
          </div>
          
          <div class="toolbar-right">
            <!-- 保存按钮 -->
            <el-tooltip v-if="!readOnly && isModified" content="保存 (Ctrl+S)" placement="bottom">
              <el-icon class="tool-btn save-btn" @click="handleSave"><Check /></el-icon>
            </el-tooltip>
            
            <!-- 下载 -->
            <el-tooltip content="下载文件" placement="bottom">
              <el-icon class="tool-btn" @click="handleDownload"><Download /></el-icon>
            </el-tooltip>
            
            <!-- 操作提示 -->
            <el-tooltip placement="bottom">
              <template #content>
                <div style="line-height: 1.8">
                  1. 按 Esc 键可退出查看<br />
                  2. Ctrl+S 可保存修改<br />
                  3. 支持在线编辑、保存、下载
                </div>
              </template>
              <el-icon class="tool-btn"><QuestionFilled /></el-icon>
            </el-tooltip>
            
            <!-- 关闭 -->
            <el-icon class="tool-btn close-btn" @click="handleClose"><Close /></el-icon>
          </div>
        </div>
        
        <!-- 编辑器区域 -->
        <div class="editor-wrapper">
          <!-- 工具栏 -->
          <div class="editor-toolbar">
            <el-checkbox v-model="lineWrapping" @change="handleOptionChange">自动换行</el-checkbox>
            
            <el-select v-model="fontSize" size="small" style="width: 90px" @change="handleFontSizeChange">
              <el-option v-for="size in fontSizeList" :key="size" :value="size" :label="`${size}px`" />
            </el-select>
            
            <el-select v-model="currentMode" size="small" style="width: 130px" filterable @change="handleModeChange">
              <el-option v-for="[key, val] in languageModes" :key="key" :value="val.mime" :label="val.language || key" />
            </el-select>
            
            <el-select v-model="currentTheme" size="small" style="width: 150px" filterable @change="handleThemeChange">
              <el-option value="default" label="default" />
              <el-option v-for="t in themeList" :key="t" :value="t" :label="t" />
            </el-select>
          </div>
          
          <!-- CodeMirror 编辑器容器 -->
          <div class="codemirror-container" :style="{ fontSize: fontSize + 'px' }" v-loading="loading">
            <div ref="editorRef" class="editor-area"></div>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Download, QuestionFilled, Close } from '@element-plus/icons-vue'
import { getPreviewUrl, getDownloadUrl } from '@/api/fileApi'

// 动态导入 CodeMirror
import CodeMirror from 'codemirror'
import 'codemirror/lib/codemirror.css'

// CodeMirror 语言模式
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/mode/css/css.js'
import 'codemirror/mode/xml/xml.js'
import 'codemirror/mode/htmlmixed/htmlmixed.js'
import 'codemirror/mode/clike/clike.js'
import 'codemirror/mode/python/python.js'
import 'codemirror/mode/sql/sql.js'
import 'codemirror/mode/shell/shell.js'
import 'codemirror/mode/go/go.js'
import 'codemirror/mode/php/php.js'
import 'codemirror/mode/vue/vue.js'
import 'codemirror/mode/yaml/yaml.js'
import 'codemirror/mode/markdown/markdown.js'

// CodeMirror 主题
import 'codemirror/theme/monokai.css'
import 'codemirror/theme/dracula.css'
import 'codemirror/theme/material.css'
import 'codemirror/theme/eclipse.css'
import 'codemirror/theme/idea.css'
import 'codemirror/theme/darcula.css'
import 'codemirror/theme/cobalt.css'
import 'codemirror/theme/ambiance.css'

// CodeMirror 折叠功能
import 'codemirror/addon/fold/foldcode.js'
import 'codemirror/addon/fold/foldgutter.js'
import 'codemirror/addon/fold/foldgutter.css'
import 'codemirror/addon/fold/brace-fold.js'
import 'codemirror/addon/fold/comment-fold.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  file: { type: Object, default: null },
  readOnly: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'close', 'save'])

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

// 编辑器状态
const editorRef = ref(null)
let cmInstance = null
const loading = ref(false)
const code = ref('')
const originalCode = ref('')

// 编辑器选项
const lineWrapping = ref(true)
const fontSize = ref(14)
const currentMode = ref('text/javascript')
const currentTheme = ref('default')

// 字体大小列表
const fontSizeList = [12, 13, 14, 15, 16, 18, 20, 24]

// 主题列表
const themeList = ['monokai', 'dracula', 'material', 'eclipse', 'idea', 'darcula', 'cobalt', 'ambiance']

// 语言模式映射
const languageModes = new Map([
  ['js', { language: 'JavaScript', mime: 'text/javascript' }],
  ['ts', { language: 'TypeScript', mime: 'text/typescript' }],
  ['json', { language: 'JSON', mime: 'application/json' }],
  ['html', { language: 'HTML', mime: 'text/html' }],
  ['css', { language: 'CSS', mime: 'text/css' }],
  ['less', { language: 'Less', mime: 'text/x-less' }],
  ['scss', { language: 'SCSS', mime: 'text/x-scss' }],
  ['xml', { language: 'XML', mime: 'application/xml' }],
  ['java', { language: 'Java', mime: 'text/x-java' }],
  ['c', { language: 'C', mime: 'text/x-csrc' }],
  ['cpp', { language: 'C++', mime: 'text/x-c++src' }],
  ['py', { language: 'Python', mime: 'text/x-python' }],
  ['go', { language: 'Go', mime: 'text/x-go' }],
  ['php', { language: 'PHP', mime: 'text/x-php' }],
  ['sql', { language: 'SQL', mime: 'text/x-sql' }],
  ['sh', { language: 'Shell', mime: 'text/x-sh' }],
  ['vue', { language: 'Vue', mime: 'text/x-vue' }],
  ['md', { language: 'Markdown', mime: 'text/x-markdown' }],
  ['yml', { language: 'YAML', mime: 'text/x-yaml' }],
  ['yaml', { language: 'YAML', mime: 'text/x-yaml' }],
  ['txt', { language: 'Plain Text', mime: 'text/plain' }]
])

// 文件名
const fileName = computed(() => props.file?.fileName || '未知文件')

// 是否已修改
const isModified = computed(() => code.value !== originalCode.value)

// 根据文件扩展名获取语言模式
const getModeByExtension = (ext) => {
  if (!ext) return 'text/plain'
  const lower = ext.toLowerCase()
  const modeInfo = languageModes.get(lower)
  return modeInfo?.mime || 'text/plain'
}

// 创建 CodeMirror 实例
const createEditor = () => {
  if (!editorRef.value) return
  
  // 如果已存在实例，先销毁
  if (cmInstance) {
    cmInstance.toTextArea()
    cmInstance = null
  }
  
  cmInstance = CodeMirror(editorRef.value, {
    value: code.value,
    mode: currentMode.value,
    theme: currentTheme.value,
    lineNumbers: true,
    lineWrapping: lineWrapping.value,
    readOnly: props.readOnly,
    tabSize: 2,
    foldGutter: true,
    gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
    autoCloseBrackets: true,
    matchBrackets: true
  })
  
  // 监听内容变化
  cmInstance.on('change', (cm) => {
    code.value = cm.getValue()
  })
  
  // 刷新编辑器
  setTimeout(() => {
    cmInstance && cmInstance.refresh()
  }, 100)
}

// 销毁编辑器
const destroyEditor = () => {
  if (cmInstance) {
    cmInstance = null
  }
  if (editorRef.value) {
    editorRef.value.innerHTML = ''
  }
}

// 加载文件内容
const loadFileContent = async () => {
  if (!props.file) return
  
  loading.value = true
  try {
    const url = getPreviewUrl(props.file.id)
    const response = await fetch(url)
    const text = await response.text()
    
    code.value = text
    originalCode.value = text
    
    // 设置语言模式
    const ext = props.file.extendName
    currentMode.value = getModeByExtension(ext)
    
    // 创建编辑器
    await nextTick()
    createEditor()
    
  } catch (error) {
    ElMessage.error('加载文件内容失败')
  } finally {
    loading.value = false
  }
}

// 选项变更 - 换行
const handleOptionChange = () => {
  if (cmInstance) {
    cmInstance.setOption('lineWrapping', lineWrapping.value)
  }
}

// 字体大小变更
const handleFontSizeChange = () => {
  // 字体大小通过 CSS 控制，不需要重建编辑器
}

// 语言模式变更
const handleModeChange = () => {
  if (cmInstance) {
    cmInstance.setOption('mode', currentMode.value)
  }
}

// 主题变更
const handleThemeChange = () => {
  localStorage.setItem('code_preview_theme', currentTheme.value)
  if (cmInstance) {
    cmInstance.setOption('theme', currentTheme.value)
  }
}

// 保存
const handleSave = () => {
  if (props.readOnly || !isModified.value) return
  
  emit('save', code.value)
  originalCode.value = code.value
  ElMessage.success('保存成功')
}

// 下载
const handleDownload = () => {
  if (props.file) {
    window.open(getDownloadUrl(props.file.id))
  }
}

// 关闭
const handleClose = () => {
  if (isModified.value && !props.readOnly) {
    if (!confirm('有未保存的更改，确定要关闭吗？')) {
      return
    }
  }
  visible.value = false
  emit('close')
}

// 键盘事件
const handleKeydown = (event) => {
  if (!visible.value) return
  
  if (event.key === 'Escape') {
    handleClose()
  }
}

// 监听显示状态
watch(visible, (val) => {
  if (val) {
    // 加载保存的主题
    const savedTheme = localStorage.getItem('code_preview_theme')
    if (savedTheme) {
      currentTheme.value = savedTheme
    }
    
    loadFileContent()
    document.addEventListener('keydown', handleKeydown)
    document.body.style.overflow = 'hidden'
  } else {
    document.removeEventListener('keydown', handleKeydown)
    document.body.style.overflow = ''
    destroyEditor()
    code.value = ''
    originalCode.value = ''
  }
})

// 组件销毁时清理
onBeforeUnmount(() => {
  destroyEditor()
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style lang="scss" scoped>
.code-preview-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  flex-direction: column;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

// 顶部工具栏
.toolbar {
  height: 48px;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  color: #fff;
  flex-shrink: 0;
  
  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    
    .file-name {
      max-width: 400px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 16px;
    }
    
    .unsaved-tip {
      color: #e6a23c;
      font-size: 14px;
    }
  }
  
  .toolbar-center {
    .preview-type {
      color: rgba(255, 255, 255, 0.7);
      font-size: 14px;
    }
  }
  
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    justify-content: flex-end;
    
    .tool-btn {
      font-size: 22px;
      cursor: pointer;
      padding: 10px;
      border-radius: 6px;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 42px;
      min-height: 42px;
      
      &:hover {
        background: rgba(255, 255, 255, 0.15);
        transform: scale(1.05);
      }
      
      &:active {
        transform: scale(0.95);
      }
      
      &.save-btn {
        color: #67c23a;
      }
      
      &.close-btn {
        margin-left: 8px;
        
        &:hover {
          background: rgba(255, 0, 0, 0.4);
        }
      }
    }
  }
}

// 编辑器区域
.editor-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin: 8px auto;
  width: 90vw;
  max-width: 1400px;
  overflow: hidden;
  
  .editor-toolbar {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 16px;
    background: #fff;
    border-radius: 8px 8px 0 0;
    border-bottom: 1px solid #ebeef5;
    
    :deep(.el-checkbox) {
      margin-right: 0;
    }
  }
  
  .codemirror-container {
    flex: 1;
    overflow: hidden;
    background: #fff;
    border-radius: 0 0 8px 8px;
    
    .editor-area {
      height: 100%;
      
      :deep(.CodeMirror) {
        height: 100%;
        font-family: 'Consolas', 'Monaco', 'Source Code Pro', monospace;
      }
    }
  }
}

/* 平板适配 */
@media (max-width: 1024px) {
  .toolbar {
    padding: 0 16px;
    
    .toolbar-left {
      .file-name {
        max-width: 280px;
      }
    }
  }
  
  .editor-wrapper {
    width: 94vw;
    margin: 6px auto;
    
    .editor-toolbar {
      gap: 12px;
      padding: 10px 12px;
      flex-wrap: wrap;
    }
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .toolbar {
    height: 44px;
    padding: 0 12px;
    flex-wrap: wrap;
    
    .toolbar-left {
      .file-name {
        max-width: 180px;
        font-size: 14px;
      }
      
      .unsaved-tip {
        font-size: 12px;
      }
    }
    
    .toolbar-center {
      display: none;
    }
    
    .toolbar-right {
      gap: 2px;
      
      .tool-btn {
        font-size: 18px;
        padding: 8px;
        min-width: 36px;
        min-height: 36px;
      }
    }
  }
  
  .editor-wrapper {
    width: 100%;
    margin: 4px 0;
    border-radius: 0;
    
    .editor-toolbar {
      gap: 8px;
      padding: 8px 12px;
      border-radius: 0;
      flex-wrap: wrap;
      
      :deep(.el-select) {
        width: 100px;
      }
    }
    
    .codemirror-container {
      border-radius: 0;
      
      .editor-area {
        :deep(.CodeMirror) {
          font-size: 13px;
        }
      }
    }
  }
}
</style>
