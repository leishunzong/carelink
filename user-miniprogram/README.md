# 护联 Care-Link 用户端微信小程序

基于 Figma 设计稿与后端 API 文档搭建的用户端小程序，先完成界面结构、样式与交互，接口为 mock，后续在 `utils/request.js` 与各页 `load*` 方法中对接 `backend/care-link`。

## 目录结构

```
carelink-user/
├── app.js / app.json / app.wxss   # 入口与全局配置
├── project.config.json            # 微信开发者工具项目配置
├── sitemap.json
├── utils/
│   ├── request.js                 # 请求封装（Bearer token、baseUrl）
│   └── constant.js                # 枚举：订单状态、服务包分类等
├── pages/
│   ├── index/                     # 服务 Tab：城市、搜索、轮播、六宫格、附近守护者、热卖服务包
│   ├── login/                     # 登录
│   ├── register/                  # 注册
│   ├── caregiver/index/           # 护工 Tab：搜索、分类、筛选、列表
│   ├── chat/                      # 对话 Tab：智能助手、快捷入口
│   ├── package/list/              # 服务包列表（分类、搜索）
│   ├── package/detail/            # 服务包详情（预约入口）
│   ├── caregiver/detail/          # 护工详情（预约此护工）
│   ├── order/list/                # 我的订单（从「我的」进入）
│   ├── order/detail/              # 订单详情
│   ├── order/create/              # 预约下单
│   └── my/                        # 我的 Tab：头像、我的订单/地址/服务对象/评价/接单入驻、退出
└── assets/                        # 默认图等静态资源
```

## 运行方式

1. 用 **微信开发者工具** 打开本目录（`fronted/carelink-user`）。
2. 未配置 AppID 时可选「测试号」或「游客模式」。
3. 当前为 mock 数据，登录任意账号即可进入；Tab：服务、护工、对话、我的。

## 与后端对接

- **Base URL**：在 `app.js` 的 `globalData.baseUrl` 中填写（如 `https://your-api.com`）。
- **接口文档**：`backend/care-link/docs/API接口文档.md`。
- **认证**：登录后把返回的 `token` 存到 `wx.setStorageSync('token')` 与 `app.globalData.token`，`request.js` 会在需认证请求头中自动加 `Authorization: Bearer <token>`。
- **主要对接点**：
  - 登录/注册：`pages/login/login.js`、`pages/register/register.js` → POST /user/login、/user/register
  - 首页热门词：`pages/index/index.js` → GET /package/hot-keywords
  - 服务包：`pages/package/list/list.js`、`pages/package/detail/detail.js` → /package/page、/package/search、/package/{id}
  - 护工：`pages/caregiver/index/index.js`、`pages/caregiver/detail/detail.js` → POST /caregiver/search、GET /caregiver/public/{id}/detail
  - 订单：`pages/order/list/list.js`、`pages/order/detail/detail.js`、`pages/order/create/create.js` → /order/user/*

## 设计说明

- **设计参考**：`prototype/prototype-user/` 下的 React 原型与 `CURSOR_HANDOFF.md`。主题色蓝色 #3B82F6，4 个 Tab：服务、护工、对话、我的；登录/注册为蓝渐变 + 白卡表单。
- 未登录可浏览首页、服务包、护工；订单与「我的」内需登录能力已预留；订单从「我的」→ 我的订单 进入。

## 与原型对齐说明（编译效果不一致时参考）

以下已按原型做过对齐，若仍不一致可重点核对：

1. **全局**（`app.wxss`）  
   - 已去除微信 `button` 默认边框（`button::after { border: none }`）和默认内边距，避免按钮多一圈边框、与原型不一致。

2. **首页**（`pages/index/index.wxss`）  
   - 顶栏：与原型一致使用 `padding: 32rpx`（对应 px-4 py-4）。  
   - 各区块：与原型一致使用 `padding: 40rpx 32rpx`（对应 py-5 px-4）。  
   - 六宫格：与原型一致使用 `gap: 32rpx`、分类项圆角 `24rpx`。

3. **登录/注册**  
   - 背景为 `linear-gradient(135deg, #3B82F6, #2563eb)`，与原型 `from-blue-500 to-blue-600` 一致。  
   - 页面设为 `overflow: visible`，装饰圆在边缘可露出，更接近原型视觉效果。

4. **我的**（`pages/my/my.wxss`）  
   - 头部与原型一致使用 `padding: 96rpx 40rpx 64rpx`（对应 pt-12 pb-8）。

5. **单位**  
   - 原型为 px（如 16px、20px），小程序用 rpx（约 2rpx = 1px @ 375pt）。不同设备上 rpx 会等比缩放，与 Web 固定 px 会有轻微差异，属正常。
