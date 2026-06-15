# CareLink 护联

CareLink 是一个基于 LBS 的居家养老护理撮合系统，包含后端服务、管理后台、用户端微信小程序和护工端微信小程序。

## 项目结构

```text
.
├── backend/                 # Spring Boot 后端服务
├── admin-web/               # Vue 3 管理后台
├── user-miniprogram/        # 用户端微信小程序
├── caregiver-miniprogram/   # 护工端微信小程序
├── .github/workflows/       # GitHub Actions CI
├── docker-compose.yml       # 本地 MySQL / Redis
└── .env.example             # 环境变量示例
```

## 技术栈

- 后端：Java 17、Spring Boot 2.7、MyBatis Plus、MySQL、Redis GEO、JWT、WebSocket、LangChain4j
- 管理端：Vue 3、Vite、TypeScript、Element Plus、Pinia
- 小程序：微信原生小程序、Vant Weapp

## 快速启动

1. 准备环境变量：

```bash
cp .env.example .env
```

2. 启动本地依赖：

```bash
docker compose up -d mysql redis
```

3. 初始化数据库：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -pcarelink < backend/sql/init.sql
mysql -h 127.0.0.1 -P 3306 -u root -pcarelink care-link < backend/sql/init_data.sql
mysql -h 127.0.0.1 -P 3306 -u root -pcarelink care-link < backend/sql/rag_document_init.sql
```

4. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

5. 启动管理后台：

```bash
cd admin-web
pnpm install
pnpm dev
```

后端默认地址为 `http://localhost:8080/api`，接口文档为 `http://localhost:8080/api/doc.html`。

## 演示账号

执行 `backend/sql/init_data.sql` 后可使用以下账号体验：

| 端 | 用户名 | 密码 |
| --- | --- | --- |
| 管理后台 | `admin` | `admin123` |
| 用户端 | `test1` / `test2` / `test3` | `123456` |
| 护工端 | `cg001` 到 `cg010` | `123456` |

## 常用命令

```bash
# 后端打包
cd backend && mvn -B -DskipTests package

# 管理后台构建
cd admin-web && pnpm install && pnpm build

# 管理后台类型检查
cd admin-web && pnpm typecheck
```

## 小程序开发

- 用户端：用微信开发者工具打开 `user-miniprogram`
- 护工端：用微信开发者工具打开 `caregiver-miniprogram/miniprogram`

默认小程序后端地址为 `http://localhost:8080/api`，可在各自 `app.js` 中调整。

## 开源安全说明

仓库不包含生产数据库、Redis、COS 或 AI 服务密钥。真实凭据请通过环境变量或本地 `.env` 管理，不要提交到 Git。

如果你曾经在公开仓库或可共享文件里提交过真实密钥，请先在对应云服务侧轮换密钥，再继续开源。

## License

This project is licensed under the MIT License. See [LICENSE](./LICENSE).
