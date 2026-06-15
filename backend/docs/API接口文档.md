# 护联 Care-Link 后端接口文档

> 供前端联调与 Figma 设计参考。基础路径请以实际部署为准（如 `https://api.example.com`）。

---

## 一、通用说明

### 1.1 基础路径与 Content-Type

- **Base URL**：由部署环境决定（如 `/api` 或直接域名）
- **Content-Type**：`application/json`（除文件上传为 `multipart/form-data`）

### 1.2 认证方式

- **用户端**：登录后请求头携带 `Authorization: Bearer <token>`（`userType=user`）
- **护工端**：登录后请求头携带 `Authorization: Bearer <token>`（`userType=caregiver`）
- **管理员端**：登录后请求头携带 `Authorization: Bearer <token>`（`userType=admin`）

未标注「需认证」的接口为公开接口，可不带 token。

### 1.3 统一响应结构

```json
{
  "code": 200,
  "message": "成功",
  "data": { ... }
}
```

- `code`：200 表示成功，其他为业务/系统错误码
- `message`：提示信息
- `data`：业务数据，部分接口无数据时为 `null`

### 1.4 分页响应结构

分页接口的 `data` 为：

```json
{
  "records": [ ... ],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

- `records`：当前页数据列表
- `total`：总记录数
- `size`：每页条数
- `current`：当前页码
- `pages`：总页数

---

## 二、枚举与字典（设计/前端可共用）

### 2.1 订单状态 order.status

| 值 | 含义     |
|----|----------|
| 1  | 待支付   |
| 2  | 待接单   |
| 3  | 待上门   |
| 4  | 服务中   |
| 5  | 待确认   |
| 6  | 已完成   |
| 7  | 已取消   |
| 8  | 已关闭   |

### 2.2 订单类型 orderType

| 值 | 含义     |
|----|----------|
| 1  | 系统匹配 |
| 2  | 定向预约 |

### 2.3 服务包类型 category（服务包/订单筛选）

| 值 | 含义     |
|----|----------|
| 1  | 居家陪护 |
| 2  | 医院陪护 |
| 3  | 周期护理 |
| 4  | 家政服务 |
| 5  | 陪诊服务 |
| 6  | 母婴护理 |

### 2.4 计费方式 billingMethod

| 值 | 含义   |
|----|--------|
| 1  | 按月   |
| 2  | 按天   |
| 3  | 按小时 |
| 4  | 按次   |

### 2.5 性别 gender

| 值 | 含义   |
|----|--------|
| 0  | 未知/不限 |
| 1  | 男     |
| 2  | 女     |

### 2.6 护工工作状态 workState

| 值 | 含义   |
|----|--------|
| 1  | 接单中 |
| 2  | 服务中 |
| 3  | 休息中 |

### 2.7 护工审核状态 verifyStatus

| 值 | 含义   |
|----|--------|
| 0  | 待审   |
| 1  | 通过   |
| 2  | 拒绝   |

### 2.8 评价类型 review.type

| 值 | 含义   |
|----|--------|
| 1  | 好评   |
| 2  | 差评   |

### 2.9 评价标签类型 tag.type

| 值 | 含义     |
|----|----------|
| 1  | 好评标签 |
| 2  | 差评标签 |

### 2.10 AI 消息角色 role

| 值 | 含义   |
|----|--------|
| 1  | 用户   |
| 2  | AI     |
| 3  | 系统   |

### 2.11 技能分类 skillType（技能字典）

| 值 | 含义           |
|----|----------------|
| 1  | 临床医疗护理   |
| 2  | 基础生活照料   |
| 3  | 康复训练与介护 |
| 4  | 失智专项护理   |
| 5  | 居家安全与应急 |
| 6  | 精神慰藉与社交 |

---

## 三、用户端接口

### 3.1 用户管理 `/user`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /user/register | 否 | 用户注册 |
| POST | /user/login | 否 | 用户登录 |
| GET | /user/info | 用户 | 获取当前用户信息 |
| PUT | /user/info | 用户 | 修改当前用户信息 |
| POST | /user/city | 用户 | 设置我的城市 |
| PUT | /user/password | 用户 | 修改密码 |

**POST /user/register**  
请求体示例：

```json
{
  "username": "13800138000",
  "password": "xxx",
  "nickname": "张三",
  "phone": "13800138000"
}
```

**POST /user/login**  
请求体：`{ "username": "13800138000", "password": "xxx" }`  
响应 data：`{ "token": "xxx", "userType": "user" }`

**PUT /user/info**  
请求体示例：`{ "nickname": "张三", "avatar": "https://...", "phone": "13800138000" }`

**POST /user/city**  
请求体：`{ "cityCode": "110100", "cityName": "北京市" }`

**PUT /user/password**  
请求体：`{ "oldPassword": "xxx", "newPassword": "yyy" }`

**GET /user/info** 响应 data（UserInfoVO）：

- id, username, nickname, avatar, phone, cityCode, cityName, status, createTime

---

### 3.2 服务地址 `/user/address`（需用户认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/address | 新增服务地址 |
| PUT | /user/address/{addressId} | 修改服务地址 |
| DELETE | /user/address/{addressId} | 删除服务地址 |
| GET | /user/address/list | 服务地址列表 |
| GET | /user/address/{addressId} | 服务地址详情 |

请求体（新增/修改）：contactName(必填), contactPhone(必填), address(必填), doorNumber(选填), longitude, latitude, isDefault(0/1)。列表/详情返回同上字段。

---

### 3.3 服务对象 `/user/subject`（需用户认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/subject | 新增服务对象 |
| PUT | /user/subject/{subjectId} | 修改服务对象 |
| DELETE | /user/subject/{subjectId} | 删除服务对象 |
| GET | /user/subject/list | 服务对象列表 |
| GET | /user/subject/{subjectId} | 服务对象详情 |

请求体（新增/修改）：name, relationship, birthday, gender, height, weight, intellectStatus, selfCareAbility, medicalHistory, remarks, isDefault 等。

---

### 3.4 服务包（用户/护工均可调用，仅上架）`/package`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /package/page | 否 | 分页查询上架服务包 |
| GET | /package/search | 否 | 关键词搜索服务包（FULLTEXT） |
| GET | /package/hot-keywords | 否 | 热门/推荐搜索关键词（按销量，供搜索框） |
| GET | /package/{id} | 否 | 根据 ID 查服务包详情 |

**GET /package/page**  
Query：category（可选）, current（默认 1）, size（默认 10）  
响应 data：分页，每项为 ServicePackageVO。

**GET /package/search**  
Query：keyword（可选）, category（可选）, current, size  
响应 data：分页，每项为 ServicePackageVO。keyword 为空时等价于按分类分页列表。

**GET /package/hot-keywords**  
Query：limit（可选，默认 10，最大 20）  
响应 data：字符串数组，为按销量排序的上架服务包名称，用作搜索框热门/推荐关键词。

**GET /package/{id}** 响应 data（ServicePackageVO）：

- id, name, category, coverImage, description（简介）, detail（详情/具体服务内容）, sales  
- allowMonth, allowDay, allowHour, allowTimes  
- priceMonth, priceDay, priceHour, priceTimes  
- mandatorySkillIds, status, createTime, updateTime  

---

### 3.5 护工查询（用户端）`/caregiver`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /caregiver/search | 否 | 分页搜索护工（条件、排序） |
| GET | /caregiver/nearby | 否 | 附近护工列表（按距离排序） |
| GET | /caregiver/public/{caregiverId}/detail | 否 | 护工详情聚合（基础 + 技能 + 服务包 + 统计；评价用单独分页接口） |
| GET | /caregiver/public/{caregiverId} | 否 | 护工基础信息 |
| GET | /caregiver/public/{caregiverId}/skills | 否 | 护工技能列表 |
| GET | /caregiver/public/{caregiverId}/packages | 否 | 护工可提供的服务包列表 |

**POST /caregiver/search**  
请求体（CaregiverSearchDTO）：  
- cityCode（必填）, nameKeyword（护工名模糊）, gender, minAge, maxAge, minWorkYears, maxWorkYears, education  
- packageCategory（服务包类型，筛能提供该服务的护工）  
- sortField：orderCount | goodReviewRate | workYears | createTime  
- sortOrder：ASC | DESC，默认 DESC  
- page, size（默认 10，最大 50）  

响应 data：分页，每项为 CaregiverInfoVO（id, username, phone, realName, avatar, gender, birthday, nativePlace, education, workYears, verifyStatus, workState, cityCode, cityName, residentAddress, longitude, latitude, createTime）。

**GET /caregiver/nearby**  
Query：cityCode（必填）, longitude（必填）, latitude（必填）, limit（可选，默认 20，最大 50）  
响应 data：数组，每项为 NearbyCaregiverVO（id, realName, avatar, workYears, distanceKm, orderCount, goodReviewRate, averageRating）。

**GET /caregiver/public/{caregiverId}/detail**  
服务端采用异步编排并行查询，一次返回以下四类数据（无需分页）。评价数据较多，请使用 **GET /review/user/caregiver/{caregiverId}** 分页查询。  
响应 data（CaregiverDetailVO）：  
- `basicInfo`：CaregiverInfoVO  
- `skills`：CaregiverSkillVO[]  
- `packages`：ServicePackageVO[]（仅上架、护工已准入的服务包）  
- `stats`：CaregiverStatsVO（订单数、评价数、好评率、平均星级、标签统计等）  

**GET /caregiver/public/{caregiverId}**  
响应 data：CaregiverInfoVO。  

**GET /caregiver/public/{caregiverId}/skills**  
响应 data：CaregiverSkillVO[]。  

**GET /caregiver/public/{caregiverId}/packages**  
响应 data：ServicePackageVO[]（仅上架、护工已准入的服务包）。 

---

### 3.6 订单（用户端）`/order`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /order/user/match/create | 用户 | 创建匹配订单 |
| POST | /order/user/direct/create | 用户 | 创建定向预约订单 |
| POST | /order/user/pay | 用户 | 支付订单 |
| POST | /order/user/cancel/{orderId} | 用户 | 取消订单 |
| GET | /order/user/page | 用户 | 用户订单分页 |
| GET | /order/user/{orderId} | 用户 | 订单详情 |
| POST | /order/user/complete/{orderId} | 用户 | 确认完成 |

**POST /order/user/match/create**  
请求体（MatchOrderCreateDTO）：contactName, contactPhone, clientName, clientGender, clientAge, …；**address**（必填）, **doorNumber**（选填）, longitude, latitude, cityCode, matchingRadius；packageId, packageName, billingMethod, unitPrice, buyQuantity, totalAmount；reqGender, reqWorkYears, reqNativePlace, specialRemark；expectStartTime。

**POST /order/user/direct/create**  
请求体（DirectOrderCreateDTO）：caregiverId（必填）；其余与匹配订单类似（无 matchingRadius、reqGender 等）。

**POST /order/user/pay**  
请求体：`{ "orderId": 1 }`

**POST /order/user/cancel/{orderId}**  
请求体可选：`{ "cancelReason": "xxx" }`

**GET /order/user/page**  
Query：status（可选）, category（可选）, current, size  
响应 data：分页，每项为 OrderListItemVO（订单列表卡片字段）。  
OrderListItemVO（用户/护工订单列表）：id, orderNo, orderType, status, packageId, packageName, caregiverName（护工姓名，已派单时有值）, address, doorNumber, detailAddress, billingMethod, buyQuantity, totalAmount, expectStartTime（预约上门时间）, createTime。

**GET /order/user/{orderId}** 响应 data：OrderDetailVO（订单详情，含订单号、状态、联系人、服务对象、地址、服务包、价格、时间等；已派单时含 caregiverName、caregiverPhone）。

**POST /order/user/complete/{orderId}** 无请求体。

---

### 3.7 评价（用户端）`/review`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /review/user/create | 用户 | 创建评价 |
| GET | /review/user/my-list | 用户 | 我发布的评价列表 |
| GET | /review/user/caregiver/{caregiverId} | 否 | 某护工的评价列表（分页） |

**POST /review/user/create**  
请求体（ReviewCreateDTO）：orderId, caregiverId, content, type（1 好评/2 差评）, stars（1–5 星，必填）, isAnonymous（0/1）, tagIds（标签 ID 数组）。

**GET /review/user/my-list**  
Query：page（默认 1）, size（默认 10）  
响应 data：分页，每项为 ReviewVO。

**GET /review/user/caregiver/{caregiverId}**  
Query：page, size  
响应 data：分页，每项为 ReviewVO。

**ReviewVO**：id, orderNo, caregiverId, caregiverName, serviceDate, **nickname**（评价人昵称，匿名时为「匿名用户」）, avatar, content, type, stars, isAnonymous, tags（标签名数组）, createTime。

---

### 3.8 统计数据（用户端）`/stats`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /stats/user/caregiver/{caregiverId} | 否 | 护工统计信息 |
| GET | /stats/user/caregiver/{caregiverId}/tags | 否 | 护工评价标签统计 |

**GET /stats/user/caregiver/{caregiverId}** 响应 data（CaregiverStatsVO）：  
caregiverId, orderCount, reviewCount, starCount, goodReviewRate, averageRating, cancelCount, tagStats（TagCountVO[]：tagId, tagName, tagType, count）。

**GET /stats/user/caregiver/{caregiverId}/tags** 响应 data：TagCountVO 数组。

---

### 3.9 AI 智能助手 `/ai`（需用户认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /ai/chat | AI 对话（同步） |
| POST | /ai/chat/stream | AI 对话（流式 SSE） |
| GET | /ai/conversations | 我的会话列表（分页） |
| GET | /ai/conversation/{conversationId}/messages | 指定会话消息列表（分页） |
| PUT | /ai/conversation/{conversationId}/title | 重命名会话标题 |
| DELETE | /ai/conversation/{conversationId} | 删除单个会话 |
| DELETE | /ai/conversations/clear | 清空我的所有会话 |
| PUT | /ai/conversation/{conversationId}/pin | 设置/取消置顶 |
| PUT | /ai/conversation/{conversationId}/favorite | 设置/取消收藏 |

**POST /ai/chat**  
请求体：`{ "message": "用户输入", "conversationId": "可选，不传则新建" }`  
响应 data：`{ "reply": "AI 回复全文", "conversationId": "xxx" }`

**POST /ai/chat/stream**  
请求体同上；响应为 SSE 流，事件名 `token` 为逐段内容，`done` 为结束（data 为 conversationId）。

**GET /ai/conversations**  
Query：current（默认 1）, size（默认 10）  
响应 data：分页，每项为 AiConversationVO（id, conversationId, title, lastQuestion, lastAnswer, messageCount, isPinned, isFavorite, updateTime）。

**GET /ai/conversation/{conversationId}/messages**  
Query：current, size（默认 50）  
响应 data：分页，每项为 AiMessageVO（id, role, content, seq, createTime）。

**PUT /ai/conversation/{conversationId}/title**  
Query：title（新标题）

**PUT /ai/conversation/{conversationId}/pin**  
Query：pinned（true/false）

**PUT /ai/conversation/{conversationId}/favorite**  
Query：favorite（true/false）

---

### 3.10 文件上传 `/file`（需登录，用户/护工均可）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /file/upload/image | 是 | 上传图片 |

**POST /file/upload/image**  
表单：file（图片文件，≤5MB）  
响应 data：`{ "url": "https://..." }`

---

### 3.11 评价标签（公开）`/tag`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /tag/list | 否 | 所有评价标签 |
| GET | /tag/list/{type} | 否 | 按类型：1 好评 2 差评 |

响应 data：ReviewTag 数组（id, name, type, sort, createTime, updateTime）。

---

### 3.12 技能字典（公开）`/skill`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /skill/list | 否 | 技能列表（不分页，可选按 skillType 筛选） |
| GET | /skill/page | 否 | 技能列表分页，支持按分类、技能名检索 |
| GET | /skill/search | 否 | 按关键词搜索技能（技能名、技能描述全文检索） |

**GET /skill/list**  
Query：skillType（可选，见 2.11）  
响应 data：SkillDict 数组（id, skillName, skillType, description, needAudit 等）。

**GET /skill/page**  
Query：skillType（可选）, skillName（技能名，模糊）, current（页码，默认 1）, size（每页数量，默认 10）  
响应 data：分页结构，records 为 SkillDict 数组。

**GET /skill/search**  
Query：keyword（搜索关键词，匹配技能名、技能描述；空则返回空数组）  
响应 data：SkillDict 数组。

---

### 3.13 健康检查 `/health`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /health | 否 | 健康检查 |

响应 data：`{ "status": "UP", "timestamp": "...", "message": "护联系统运行正常" }`

---

## 四、护工端接口

### 4.1 护工认证与基础 `/caregiver`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /caregiver/register | 否 | 护工注册（仅用户名、密码、手机号） |
| POST | /caregiver/login | 否 | 护工登录 |
| POST | /caregiver/settle | 护工 | 入驻（补全姓名、头像、性别等基本信息并提交审核材料） |
| GET | /caregiver/info | 护工 | 我的信息 |
| PUT | /caregiver/info | 护工 | 修改我的信息 |
| POST | /caregiver/location | 护工 | 更新接单位置（经纬度） |
| POST | /caregiver/work-state | 护工 | 切换工作状态（1 接单中 3 休息中） |
| PUT | /caregiver/password | 护工 | 修改密码 |

**POST /caregiver/register** 请求体：username, password, phone（仅此三项；姓名、头像等基础信息在入驻时补全）。

**POST /caregiver/settle** 请求体（CaregiverSettleDTO）：realName（必填）, avatar（实拍照正面免冠素颜）, gender, birthday, nativePlace, education, ethnicity, zodiac, workYears, cityCode, cityName, residentAddress, longitude, latitude；审核材料：idCardFrontUrl, idCardBackUrl, qualificationCertUrl, otherMaterialUrls。

**POST /caregiver/login** 响应 data：`{ "token": "xxx", "userType": "caregiver" }`

**POST /caregiver/location** Query：longitude, latitude（必填）

**POST /caregiver/work-state** Query：workState（1 或 3）

---

### 4.2 护工技能 `/caregiver/skill`（需护工认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /caregiver/skill | 新增技能 |
| DELETE | /caregiver/skill/{skillId} | 删除技能（按技能字典 ID） |
| GET | /caregiver/skill/list | 我的技能列表 |

**POST /caregiver/skill** 请求体：skillId（技能字典 ID）, certImage（可选）。

---

### 4.3 护工服务包 `/caregiver/package`（需护工认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /caregiver/package/my | 我开通的服务包列表 |
| GET | /caregiver/package/list | 可开通的服务包分页（仅上架） |
| POST | /caregiver/package | 添加服务包（开通接单） |
| DELETE | /caregiver/package/{packageId} | 取消服务包 |

**GET /caregiver/package/my** 响应 data：CaregiverMyPackageVO 数组（仅含基本信息+准入时间：id, name, category, coverImage, description, bindTime）。

**POST /caregiver/package** 请求体：`{ "packageId": 1 }`

---

### 4.4 护工订单 `/order`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /order/caregiver/page | 护工 | 护工订单分页 |
| GET | /order/caregiver/{orderId} | 护工 | 订单详情 |
| POST | /order/caregiver/grab/{orderId} | 护工 | 抢单 |
| POST | /order/caregiver/start/{orderId} | 护工 | 上门打卡/开始服务 |
| POST | /order/caregiver/finish/{orderId} | 护工 | 结束服务 |

**订单推送（WebSocket）** 护工端订阅 `/topic/order/{caregiverId}` 接收新订单推送，推送体为 OrderPushVO，含 **orderType**（1 系统匹配 2 定向预约），前端可根据 orderType 渲染不同按钮（如「抢单」/「接单」等）。

**GET /order/caregiver/page** Query：status, category, current, size  
响应 data：分页，每项为 OrderListItemVO（同上，含 caregiverName、detailAddress）。  

**POST /order/caregiver/start/{orderId}** 请求体：`{ "longitude": 116.xxx, "latitude": 39.xxx }`（距服务地址 500m 内可打卡）

---

### 4.5 护工评价与统计 `/review`、`/stats`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /review/caregiver/my-list | 护工：我的被评价列表（分页） |
| GET | /stats/caregiver/my | 护工：我的统计信息 |
| GET | /stats/caregiver/my/tags | 护工：我的标签统计 |

Query 分页：page, size。

---

## 五、管理员端接口 `/admin`

> 除登录外均需管理员 token。

### 5.1 登录与护工审核

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /admin/login | 否 | 管理员登录 |
| GET | /admin/stats | 管理员 | 查询统计数据（护工/订单/待审核/用户/评价/服务包/技能/评价标签/知识库总数，异步并行） |
| GET | /admin/caregiver/settle-list | 管理员 | 护工入驻申请列表（分页，可按姓名、手机号检索） |
| POST | /admin/caregiver/settle/audit | 管理员 | 入驻审核（通过/拒绝） |
| GET | /admin/caregiver/skill-apply-list | 管理员 | 护工技能申请列表（分页，可按姓名、手机号、技能名检索） |
| POST | /admin/caregiver/skill-apply/audit | 管理员 | 技能申请审核 |

**GET /admin/stats** 无需请求体。响应 data 为 AdminStatsVO：caregiverTotal（护工总数）, orderTotal（订单总数）, pendingSettleTotal（待审核护工入驻数）, pendingSkillTotal（待审核技能数）, userTotal（用户总数）, reviewTotal（评价总数）, servicePackageTotal（服务包总数）, skillTotal（技能总数）, reviewTagTotal（评价标签总数）, ragDocumentTotal（知识库总数）, **todayRevenue**（当日营业额，元）, **totalRevenue**（总营业额，元）。营业额为已完成订单（status=6）的 total_amount 合计，当日按 finish_time 所在自然日。查询采用异步编排并行执行各 count 与营业额汇总。

**GET /admin/caregiver/settle-list** Query：realName（护工姓名，模糊）, phone（手机号，模糊）, current, size。响应 data 分页，每项为 CaregiverSettleApplyVO：id, realName, phone, **avatar**（头像）, **gender**（1男2女）, **birthday**（出生日期）, **ethnicity**（民族）, **zodiac**（属相）, **nativePlace**（籍贯）, **education**（学历）, **workYears**（从业年限）, cityName, **residentAddress**（居住地址）, verifyStatus, createTime, updateTime, verifyMaterials（审核材料：materialType、materialTypeName、fileUrl、sortOrder）。

**GET /admin/caregiver/skill-apply-list** Query：caregiverName（护工姓名，模糊）, caregiverPhone（手机号，模糊）, skillName（技能名称，模糊）, current, size。

**POST /admin/login** 请求体：LoginDTO；响应 data：`{ "token": "xxx", "userType": "admin" }`

**POST /admin/caregiver/settle/audit** 请求体：caregiverId, passed（true/false）, rejectReason（拒绝时）

**POST /admin/caregiver/skill-apply/audit** 请求体：caregiverSkillId, passed, rejectReason

---

### 5.2 分页查询（管理员）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /admin/review/page | 管理员 | 分页查询评价列表（用户名/护工名/订单号检索，按创建时间倒序） |
| GET | /admin/caregiver/page | 管理员 | 分页查询护工列表（护工名/性别/手机号/年龄/学历/从业年限/服务城市名/工作状态检索，按创建时间倒序） |
| GET | /admin/order/page | 管理员 | 分页查询订单列表（订单号/订单类型/订单状态/护工名/联系人姓名/地址检索，按创建时间倒序） |

**GET /admin/review/page** Query：**nickname**（用户昵称，模糊，检索评价人昵称）, caregiverName（护工名，模糊）, orderNo（订单号，模糊）, current, size。响应 data：分页，records 为 ReviewVO 数组（nickname 为评价人昵称）。

**GET /admin/caregiver/page** Query：realName（护工名，模糊）, phone（手机号，模糊）, gender（1男2女）, minAge, maxAge, education（学历，模糊）, workYears（从业年限）, cityName（服务城市名，模糊）, workState（1接单中 2服务中 3休息中）, current, size。响应 data：分页，records 为 CaregiverInfoVO 数组。

**GET /admin/order/page** Query：orderNo（订单号，模糊）, orderType（1系统匹配 2定向预约）, status（订单状态）, caregiverName（护工名，模糊）, contactName（联系人姓名，模糊）, cityName（地址，模糊，检索订单 address）, current, size。响应 data：分页，records 为 **AdminOrderListItemVO** 数组（管理端专用）。AdminOrderListItemVO 含：id, orderNo, orderType, status, packageId, packageName, caregiverName, **nickname**（下单用户昵称）, address, doorNumber, detailAddress, billingMethod, buyQuantity, **unitPrice**（单价）, totalAmount, **expectStartTime**（预约上门时间）, **realStartTime**（实际上门/开始时间）, **finishTime**（结束时间）, createTime。

---

### 5.3 评价标签与技能字典（管理员）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/tag | 新增评价标签 |
| PUT | /admin/tag/{tagId} | 修改评价标签 |
| POST | /admin/skill | 新增技能 |
| PUT | /admin/skill/{skillId} | 修改技能 |
| DELETE | /admin/skill/{skillId} | 删除技能 |

---

### 5.4 服务包管理（管理员）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/package/page | 分页（可筛 category、status） |
| POST | /admin/package | 新增服务包 |
| PUT | /admin/package/{id} | 修改服务包 |
| DELETE | /admin/package/{id} | 删除服务包 |
| PUT | /admin/package/{id}/on-shelf | 上架服务包（status=1） |
| PUT | /admin/package/{id}/off-shelf | 下架服务包（status=0） |

服务包 DTO 含：name, coverImage, category, description（简介）, detail（详情，富文本）, sales, allowMonth/Day/Hour/Times, priceMonth/Day/Hour/Times, mandatorySkillIds, status 等。

---

### 5.5 RAG 知识库（管理员）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/rag/document | 上传文档（.txt/.md） |
| POST | /admin/rag/document/text | 直接提交标题+正文 |
| GET | /admin/rag/documents | 文档列表（含正文 content，便于管理端查看/编辑） |
| DELETE | /admin/rag/document/{id} | 删除文档（仅移除该文档向量，不重建整个库） |

上传文档：表单 file + 可选 title。  
直接提交：Query title, content。  
文档列表返回：id, title, fileName, content, createTime, updateTime。

---


---

*文档版本与后端代码同步，若有增删接口请以实际代码与 Swagger 为准。*
