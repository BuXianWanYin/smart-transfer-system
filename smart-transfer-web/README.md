# 基于Java+Vue的TCP拥塞控制优化大文件传输工具 - 前端

## 项目简介

这是一个基于 Vue 3 + Vite + Element Plus 开发的大文件传输前端应用，支持：

- 🚀 大文件分片上传
- ⏸️ 断点续传
- 📊 实时传输监控
- 🔧 拥塞控制算法配置
- 📈 可视化数据展示

## 技术栈

- **框架**: Vue 3.5.16
- **构建工具**: Vite 5.0
- **UI 组件库**: Element Plus 2.9.11
- **路由**: Vue Router 4.4.2
- **状态管理**: Pinia 2.3.1
- **HTTP 客户端**: Axios 1.7.5
- **图表**: ECharts 5.4.3
- **工具库**: dayjs, spark-md5, @vueuse/core

## 项目结构

```
smart-transfer-web/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口
│   │   ├── fileApi.js
│   │   ├── congestionApi.js
│   │   └── configApi.js
│   ├── assets/         # 资源文件
│   │   └── styles/     # 样式文件
│   ├── components/     # 公共组件
│   │   └── FileUploader.vue
│   ├── router/         # 路由配置
│   ├── store/          # Pinia 状态管理
│   │   ├── fileStore.js
│   │   ├── congestionStore.js
│   │   └── configStore.js
│   ├── utils/          # 工具函数
│   │   ├── file.js     # 文件操作工具
│   │   ├── format.js   # 格式化工具
│   │   └── http/       # HTTP 请求封装
│   ├── views/          # 页面组件
│   │   ├── FileUpload.vue
│   │   ├── FileList.vue
│   │   ├── CongestionMonitor.vue
│   │   └── CongestionConfig.vue
│   ├── App.vue         # 根组件
│   └── main.js         # 入口文件
├── .eslintrc.cjs       # ESLint 配置
├── .prettierrc         # Prettier 配置
├── .gitignore          # Git 忽略文件
├── jsconfig.json       # JavaScript 配置
├── vite.config.js      # Vite 配置
├── package.json        # 项目依赖
└── ENV_CONFIG.md       # 环境变量配置说明

## 快速开始

### 1. 环境要求

- Node.js >= 16.0.0
- npm >= 8.0.0

### 2. 安装依赖

```bash
npm install
```

### 3. 配置环境变量

在项目根目录创建 `.env.development` 文件：

```env
NODE_ENV=development
VITE_APP_TITLE=基于Java+Vue的TCP拥塞控制优化大文件传输工具
VITE_APP_PORT=3000
VITE_API_BASE_URL=/api
VITE_API_TIMEOUT=30000
VITE_SERVER_URL=http://localhost:8081
# 分片大小、最大文件大小从系统配置接口 /file/upload/config 获取
```

详细配置说明请查看 [ENV_CONFIG.md](./ENV_CONFIG.md)

### 4. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 5. 构建生产版本

```bash
npm run build
```

### 6. 预览生产构建

```bash
npm run preview
```

## 可用脚本

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 构建生产版本 |
| `npm run preview` | 预览生产构建 |
| `npm run lint` | 运行 ESLint 检查并自动修复 |
| `npm run format` | 使用 Prettier 格式化代码 |

## 核心功能

### 1. 文件上传

- 支持拖拽上传
- 自动分片处理
- 断点续传
- 秒传功能（文件 Hash 检测）
- 实时上传进度显示

### 2. 文件列表

- 文件浏览
- 文件下载
- 文件删除
- 文件详情查看

### 3. 拥塞监控

- 实时网络指标监控（RTT、带宽、丢包率等）
- 可视化图表展示
- 算法切换
- 历史数据查看

### 4. 系统配置

- 拥塞控制算法选择（CUBIC、BBR、自适应）
- 拥塞窗口参数配置
- 传输速率限制配置

## 配置说明

### Vite 配置

主要配置项（`vite.config.js`）：

- **端口**: 3000
- **自动打开浏览器**: 已启用
- **API 代理**: `/api` -> `http://localhost:8081`
- **路径别名**: `@` -> `src/`
- **自动导入**: Vue、Vue Router、Pinia API
- **组件自动注册**: Element Plus 组件按需加载

### ESLint 配置

- Vue 3 推荐规则
- 关闭多词组件名检查
- 开发环境允许 console
- 强制使用单引号、不使用分号

### Prettier 配置

- 不使用分号
- 使用单引号
- 行宽 100
- 缩进 2 空格

## 开发规范

### 命名规范

- **组件文件**: PascalCase，如 `FileUploader.vue`
- **工具函数文件**: camelCase，如 `fileApi.js`
- **常量**: UPPER_SNAKE_CASE，如 `MAX_FILE_SIZE`

### 代码规范

- 使用 Composition API
- 使用 `<script setup>` 语法
- 合理拆分组件
- 统一使用 Pinia 管理状态
- API 调用统一在 `api/` 目录管理

### Git 提交规范

```
feat: 新功能
fix: 修复问题
docs: 文档修改
style: 代码格式调整
refactor: 代码重构
perf: 性能优化
test: 测试相关
chore: 构建/工具链相关
```

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 常见问题

### 1. 无法连接后端服务

确认：
- 后端服务是否已启动（默认端口 8081）
- `.env.development` 中 `VITE_SERVER_URL` 配置是否正确
- 代理配置是否正确

### 2. 文件上传失败

检查：
- 文件大小是否超过限制
- 后端存储路径是否有写入权限
- 网络连接是否正常

### 3. 图表不显示

确认：
- ECharts 是否正确安装
- 数据格式是否正确
- 容器是否有高度

## 许可证

MIT License

## 联系方式

如有问题，请联系项目负责人。
