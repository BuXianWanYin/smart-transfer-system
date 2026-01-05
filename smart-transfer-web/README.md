# 基于Java+Vue的TCP拥塞控制优化大文件传输工具 - 前端项目

## 项目简介

基于Java+Vue的TCP拥塞控制优化大文件传输工具前端项目，采用 Vue3 + JavaScript + Element Plus + Vite 构建。

## 技术栈

- **Vue 3.5** - 渐进式 JavaScript 框架
- **JavaScript** - 开发语言（非 TypeScript）
- **Element Plus 2.9** - UI 组件库
- **Vue Router 4.4** - 路由管理
- **Pinia 2.3** - 状态管理
- **Axios 1.7** - HTTP 请求
- **Vite 5.0** - 构建工具
- **CSS** - 样式（原生 CSS，不使用预处理器）

## 项目结构

```
smart-transfer-web/
├── public/                  # 静态资源
├── src/
│   ├── api/                # API 接口
│   ├── assets/             # 资源文件
│   │   ├── images/        # 图片
│   │   └── styles/        # 样式（CSS）
│   ├── components/         # 公共组件
│   ├── composables/        # 组合式函数
│   ├── directives/         # 自定义指令
│   ├── router/             # 路由配置
│   ├── store/              # 状态管理
│   ├── utils/              # 工具函数
│   ├── views/              # 页面组件
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── .eslintrc.cjs          # ESLint 配置
├── .prettierrc            # Prettier 配置
├── .gitignore             # Git 忽略文件
├── index.html             # HTML 模板
├── package.json           # 项目配置
├── vite.config.js         # Vite 配置
└── README.md              # 项目说明
```

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

### 代码检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

## 开发规范

请参考项目根目录的 `.cursor/rules/project-development-rules.md` 文件。

### 核心规范

1. **使用 JavaScript**：项目使用 JavaScript 而非 TypeScript
2. **组件命名**：PascalCase（如 `UserList.vue`）
3. **文件命名**：
   - API 文件：`xxxApi.js`
   - 工具函数：`xxx.js`
   - 组合式函数：`useXxx.js`
4. **注释规范**：使用 JSDoc 注释
5. **代码风格**：遵循 ESLint 和 Prettier 配置

## 接口配置

后端接口地址在 `vite.config.js` 中配置：

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8081',
    changeOrigin: true
  }
}
```

## 环境变量

开发环境：默认 http://localhost:8081
生产环境：需要在部署时配置

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 许可证

私有项目

