# 护联护工端小程序

基于微信小程序原生开发的护工端应用，对接护联后端服务。

## 项目结构

```
miniprogram/
├── pages/                      # 页面目录
│   ├── login/                 # 登录页
│   ├── register/              # 注册页
│   ├── settlement/            # 入驻申请页
│   ├── home/                  # 首页(接单中心)
│   ├── service/               # 服务中心
│   ├── order/                 # 订单列表
│   ├── order-detail/          # 订单详情
│   ├── profile/               # 个人中心
│   ├── edit-profile/          # 编辑个人信息
│   ├── change-password/       # 修改密码
│   ├── skill-list/            # 技能列表
│   ├── skill-management/      # 技能管理
│   ├── package-list/          # 服务包列表
│   ├── package-detail/        # 服务包详情
│   ├── package-management/    # 服务包管理
│   └── review-management/     # 评价管理
├── utils/                      # 工具类
│   ├── request.js             # 网络请求封装
│   ├── api.js                 # API接口定义
│   ├── constants.js           # 常量定义
│   └── util.js                # 通用工具函数
├── assets/                     # 静态资源
│   └── icons/                 # TabBar图标
├── app.js                      # 小程序入口
├── app.json                    # 全局配置
├── app.wxss                    # 全局样式
├── project.config.json         # 项目配置
└── sitemap.json               # 站点地图

## 功能模块

### 1. 认证模块
- ✅ 护工登录
- ✅ 护工注册
- ⏳ 入驻申请(三步流程)
- ⏳ 修改密码

### 2. 首页(接单中心)
- ✅ 工作状态切换(接单中/服务中/休息中)
- ✅ 位置更新
- ✅ 接单池展示
- ✅ 抢单/接单功能

### 3. 服务中心
- ⏳ 技能申请与管理
- ⏳ 服务包开通与管理
- ⏳ 评价查看

### 4. 订单管理
- ⏳ 订单列表(分状态筛选)
- ⏳ 订单详情
- ⏳ 上门打卡
- ⏳ 完成服务

### 5. 个人中心
- ⏳ 个人信息展示
- ⏳ 编辑个人信息
- ⏳ 修改密码
- ⏳ 统计数据展示

## 技术栈

- **框架**: 微信小程序原生框架
- **语言**: JavaScript (ES6+)
- **UI**: 原生WXML + WXSS
- **网络**: wx.request 封装
- **状态管理**: 全局 App 实例 + 本地存储

## 后端接口

后端服务地址：`http://localhost:8080/api`

### 主要接口

#### 认证相关
- `POST /caregiver/login` - 登录
- `POST /caregiver/register` - 注册
- `PUT /caregiver/password` - 修改密码

#### 护工信息
- `GET /caregiver/info` - 获取个人信息
- `PUT /caregiver/info` - 更新个人信息
- `POST /caregiver/settle` - 提交入驻申请
- `POST /caregiver/location` - 更新位置
- `POST /caregiver/work-state` - 更新工作状态

#### 技能管理
- `GET /skill` - 获取技能字典
- `GET /caregiver/skill/list` - 获取我的技能
- `POST /caregiver/skill` - 申请技能
- `DELETE /caregiver/skill/:id` - 删除技能

#### 服务包管理
- `GET /package/page` - 服务包列表
- `GET /package/:id` - 服务包详情
- `GET /caregiver/package/my` - 我的服务包
- `POST /caregiver/package` - 开通服务包
- `DELETE /caregiver/package/:id` - 取消服务包

#### 订单管理
- `GET /order/caregiver/page` - 我的订单列表
- `GET /order/caregiver/:id` - 订单详情
- `POST /order/caregiver/grab/:id` - 抢单
- `POST /order/caregiver/start/:id` - 上门打卡
- `POST /order/caregiver/finish/:id` - 完成服务

#### 评价管理
- `GET /review/caregiver/my-list` - 我的评价列表

## 开发指南

### 环境准备

1. 安装微信开发者工具
2. 导入项目
3. 配置 AppID (在 `project.config.json` 中)

### 本地开发

1. 打开微信开发者工具
2. 选择 `miniprogram` 目录作为项目根目录
3. 点击编译运行

### 配置说明

#### 修改后端地址

在 `app.js` 中修改 `baseURL`:

```javascript
globalData: {
  baseURL: 'http://your-api-server.com/api'
}
```

#### TabBar 图标

需要准备4组图标(普通态+选中态),放置在 `assets/icons/` 目录:
- home.png / home-active.png
- service.png / service-active.png
- order.png / order-active.png
- profile.png / profile-active.png

尺寸建议: 81px × 81px (1:1)

### 权限配置

小程序需要以下权限:
- ✅ 获取用户位置信息
- ✅ 选择位置
- ⏳ 相机(上传证件照)
- ⏳ 相册(上传图片)

在 `app.json` 的 `permission` 和 `requiredPrivateInfos` 中配置。

## API 调用示例

### 登录
```javascript
const api = require('../../utils/api')

const res = await api.auth.login({
  username: '13800138000',
  password: '123456'
})

// 保存token和用户信息
app.login(res.token, res)
```

### 获取个人信息
```javascript
const userInfo = await api.caregiver.getInfo()
```

### 抢单
```javascript
await api.order.grab(orderId)
```

## 状态说明

### 工作状态
- `1` - 接单中(可接新单)
- `2` - 服务中(不可接新单,自动切换)
- `3` - 休息中(不可接新单)

### 订单状态
- `1` - 待支付
- `2` - 待接单
- `3` - 待上门
- `4` - 服务中
- `5` - 待确认
- `6` - 已完成
- `7` - 已取消
- `8` - 已关闭

### 审核状态
- `0` - 待审核
- `1` - 已通过
- `2` - 已拒绝

## 待完成功能

由于时间和篇幅限制，以下页面需要继续完善：

1. ⏳ 入驻申请页面(三步流程)
2. ⏳ 服务中心页面
3. ⏳ 订单列表页面
4. ⏳ 订单详情页面
5. ⏳ 个人中心页面
6. ⏳ 编辑个人信息页面
7. ⏳ 技能相关页面
8. ⏳ 服务包相关页面
9. ⏳ 评价管理页面

这些页面的开发思路与已完成的登录、注册、首页类似:
- WXML 负责结构
- WXSS 负责样式
- JS 负责逻辑
- JSON 负责配置

参考原型设计和后端接口文档即可完成。

## 注意事项

1. **token管理**: 所有需要认证的接口都会自动添加 `Authorization` 请求头
2. **错误处理**: 401 错误会自动跳转登录页
3. **位置权限**: 首次使用需要用户授权位置信息
4. **图片上传**: 使用 `wx.uploadFile` API
5. **网络超时**: 默认60秒超时

## 联系方式

- 后端接口文档: http://localhost:8080/api/doc.html
- 数据库地址: 通过后端 `DB_URL` 环境变量配置
- Redis地址: 通过后端 `REDIS_HOST` / `REDIS_PORT` 环境变量配置

## License

Copyright © 2026 CareLink
