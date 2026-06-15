# 护联管理系统 (CareLink-Web)

护工服务管理后台系统，基于 Vue 3 + Element Plus 构建。

## 技术栈

- **前端框架**: Vue 3.5 + TypeScript
- **构建工具**: Vite 7.3
- **UI 框架**: Element Plus 2.13
- **路由管理**: Vue Router 4
- **状态管理**: Pinia 3
- **HTTP 请求**: Axios
- **日期处理**: dayjs
- **样式预处理**: Sass

## 功能模块

### 1. 审核管理
- ✅ 入驻审核：护工入驻申请审核
- ✅ 技能审核：护工技能证书审核

### 2. 护工管理
- ✅ 护工档案：护工基本信息查询
- ✅ 状态跟踪：工作状态、审核状态管理

### 3. 订单管理
- ✅ 订单查询：订单全生命周期管理
- ✅ 订单详情：订单状态、费用等信息

### 4. 评价系统
- ✅ 标签管理：评价标签的增删改
- ✅ 用户评价：用户评价查看

### 5. 服务管理
- ✅ 技能管理：技能分类和审核配置
- ✅ 服务包管理：服务套餐配置

### 6. 知识库
- ✅ 文档管理：知识文档的上传和管理

## 快速开始

### 安装依赖

\`\`\`bash
pnpm install
\`\`\`

### 开发环境运行

\`\`\`bash
pnpm dev
\`\`\`

访问 http://localhost:5173

### 构建生产版本

\`\`\`bash
pnpm build
\`\`\`

### 预览生产构建

\`\`\`bash
pnpm preview
\`\`\`

## 默认登录账号

- 用户名: `admin`
- 密码: `123456`

## 项目结构

\`\`\`
carelink-web/
├── src/
│   ├── api/              # API 接口
│   ├── assets/           # 静态资源
│   ├── layout/           # 布局组件
│   ├── router/           # 路由配置
│   ├── store/            # Pinia 状态管理
│   ├── styles/           # 全局样式
│   ├── types/            # TypeScript 类型
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   │   ├── login/                # 登录页
│   │   ├── admission-review/     # 入驻审核
│   │   ├── skill-review/         # 技能审核
│   │   ├── nurse-management/     # 护工管理
│   │   ├── order-management/     # 订单管理
│   │   ├── tag-management/       # 标签管理
│   │   ├── user-review/          # 用户评价
│   │   ├── skill-management/     # 技能管理
│   │   ├── service-package/      # 服务包管理
│   │   └── knowledge-base/       # 知识库管理
│   ├── App.vue
│   └── main.ts
├── public/               # 公共静态资源
├── index.html
├── vite.config.ts        # Vite 配置
├── tsconfig.json         # TypeScript 配置
└── package.json
\`\`\`

## 后端接口对接

当前使用模拟数据，后续需要对接真实后端 API：

1. 修改 `vite.config.ts` 中的 `proxy.target` 为后端地址
2. 在 `src/api/` 目录下创建对应的 API 接口文件
3. 替换页面中的模拟数据为真实 API 调用

## 注意事项

- 本项目仅包含前端代码
- `backend/` 和 `prototype-web/` 目录为参考资料，不会上传到 Git
- 开发时注意保持与原型设计的一致性

## License

ISC
