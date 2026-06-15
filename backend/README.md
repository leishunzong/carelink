# 护联 - 后端服务

## 项目简介

**护联**是一个基于地理位置(LBS)的居家养老护理撮合平台，致力于为需要护理服务的家庭快速匹配附近的优质护工。

### 核心业务流程

1. **用户下单** - 用户指定服务地点和需求
2. **智能召回** - 系统通过Redis GEO召回附近护工
3. **权重排序** - 基于距离、好评率、工龄等多维度评分
4. **高并发抢单** - 护工通过分布式锁机制抢单
5. **服务履约** - 提供服务并完成评价反馈

## 技术栈

### 核心框架
- **Java**: JDK 17
- **Spring Boot**: 2.7.18
- **MyBatis Plus**: 3.5.5

### 数据存储
- **数据库**: MySQL 5.7
- **缓存**: Redis 7.4 (支持GEO地理位置功能)
- **连接池**: Druid 1.2.20

### 工具组件
- **接口文档**: Knife4j 3.0.3 + Swagger2
- **JSON处理**: FastJson2 2.0.45
- **工具类**: Hutool 5.8.24
- **JWT认证**: JJWT 0.11.5

### 部署环境
- **Web服务器**: Nginx 1.26
- **容器化**: Docker

## 项目结构

```
care-link/
├── sql/
│   ├── 01-schema.sql                         # 数据库表结构脚本
│   ├── 02-seed.sql                           # 演示数据脚本
│   └── 03-rag-seed.sql                       # RAG 知识库演示数据
├── src/
│   ├── main/
│   │   ├── java/com/caregiver/carelink/
│   │   │   ├── CareLinkApplication.java      # 启动类
│   │   │   ├── common/                       # 通用模块
│   │   │   │   ├── base/                     # 基础类
│   │   │   │   │   └── BaseEntity.java       # 实体基类
│   │   │   │   ├── constant/                 # 常量定义
│   │   │   │   │   ├── Constants.java        # 通用常量
│   │   │   │   │   └── RedisKeyConstants.java # Redis键常量
│   │   │   │   ├── exception/                # 异常处理
│   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   └── result/                   # 统一返回结果
│   │   │   │       ├── Result.java
│   │   │   │       ├── ResultCode.java
│   │   │   │       └── PageResult.java
│   │   │   ├── config/                       # 配置类
│   │   │   │   ├── MybatisPlusConfig.java    # MyBatis Plus配置
│   │   │   │   ├── MetaObjectHandlerConfig.java # 字段自动填充
│   │   │   │   ├── RedisConfig.java          # Redis配置
│   │   │   │   ├── Knife4jConfig.java        # 接口文档配置
│   │   │   │   └── WebMvcConfig.java         # Web配置
│   │   │   ├── entity/                       # 实体类
│   │   │   │   ├── User.java                 # 用户实体
│   │   │   │   ├── Admin.java                # 管理员实体
│   │   │   │   ├── Caregiver.java            # 护工实体
│   │   │   │   ├── CaregiverSkill.java       # 护工技能实体
│   │   │   │   ├── CaregiverStats.java       # 护工统计实体
│   │   │   │   ├── ServiceAddress.java       # 服务地址实体
│   │   │   │   └── ServiceSubject.java       # 服务对象实体
│   │   │   ├── mapper/                       # Mapper接口
│   │   │   ├── service/                      # 服务层
│   │   │   │   └── impl/                     # 服务实现层
│   │   │   ├── controller/                   # 控制器
│   │   │   │   └── HealthController.java     # 健康检查
│   │   │   ├── dto/                          # 数据传输对象
│   │   │   ├── vo/                           # 视图对象
│   │   │   └── utils/                        # 工具类
│   │   │       ├── RedisUtils.java           # Redis工具类
│   │   │       └── JwtUtils.java             # JWT工具类
│   │   └── resources/
│   │       ├── application.yml               # 主配置文件
│   │       ├── application-dev.yml           # 开发环境配置
│   │       ├── application-prod.yml          # 生产环境配置
│   │       ├── logback-spring.xml            # 日志配置
│   │       └── mapper/                       # MyBatis XML文件
│   └── test/
└── pom.xml                                   # Maven配置文件
```

## 核心数据表

### 用户端
- **user** - 用户表（C端用户）
- **service_subject** - 服务对象表（老人信息）
- **service_address** - 服务地址表

### 护工端
- **caregiver** - 护工基础表（含经纬度坐标）
- **caregiver_skill** - 护工技能表
- **caregiver_stats** - 护工绩效统计表

### 管理端
- **admin** - 管理员表

## 核心功能模块

### 📍 LBS地理位置服务
- 基于Redis GEO实现护工位置存储和检索
- 支持按距离、评分等多维度排序
- 实时更新护工位置信息

### 🔐 身份认证与权限
- JWT Token认证
- 用户、护工、管理员多角色支持
- 手机号+密码登录

### ⚡ 高并发抢单机制
- Redis分布式锁防止超抢
- MySQL乐观锁version控制
- 订单状态流转管理

### 📊 数据统计分析
- 护工绩效统计（订单数、好评率等）
- 实时更新统计数据

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 5.7+
- Redis 7.4+ (或支持GEO命令的Redis版本)

### 数据库配置

1. 创建数据库并执行初始化脚本：

```bash
mysql -u root -p < sql/01-schema.sql
mysql -u root -p < sql/02-seed.sql
mysql -u root -p < sql/03-rag-seed.sql
```

2. 修改配置文件 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/care-link?...
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
```

### 运行项目

#### 方式一：使用Maven

```bash
mvn clean package
mvn spring-boot:run
```

#### 方式二：使用IDE

直接运行 `CareLinkApplication.java` 主类

### 访问服务

启动成功后访问：

- 📖 **Knife4j文档**: http://localhost:8080/api/doc.html
- 💚 **健康检查**: http://localhost:8080/api/health
- 📊 **Druid监控**: http://localhost:8080/api/druid （用户名/密码：admin/admin）

## Redis GEO使用示例

### 存储护工位置

```bash
# 将护工位置添加到Redis
GEOADD caregiver:locations 116.404 39.915 caregiver:1001
GEOADD caregiver:locations 116.405 39.916 caregiver:1002
```

### 查询附近护工

```bash
# 搜索指定坐标5公里内的护工（返回坐标和距离）
GEOSEARCH caregiver:locations FROMLONLAT 116.404 39.915 BYRADIUS 5 km WITHDIST WITHCOORD ASC
```

## 权重评分算法

```java
Score = (1/(1+距离) × 0.4) 
      + (好评率 × 0.3) 
      + (工龄 × 0.2) 
      + (技能匹配度 × 0.1)
```

## 开发规范

1. **实体类**: 继承 `BaseEntity`，自动具有创建时间、更新时间等字段
2. **Mapper**: 继承 `BaseMapper<T>`，自动具有CRUD方法
3. **Service**: 继承 `IService<T>`，实现类继承 `ServiceImpl<M, T>`
4. **Controller**: 统一使用 `Result<T>` 封装返回结果
5. **异常处理**: 业务异常抛出 `BusinessException`

## API开发示例

```java
@Api(tags = "护工管理")
@RestController
@RequestMapping("/caregiver")
public class CaregiverController {
    
    @Autowired
    private CaregiverService caregiverService;
    
    @ApiOperation("根据ID查询护工")
    @GetMapping("/{id}")
    public Result<Caregiver> getById(@PathVariable Long id) {
        return Result.success(caregiverService.getById(id));
    }
}
```

## 后续开发计划

- [ ] 用户注册登录功能
- [ ] 护工注册登录功能
- [ ] LBS地理位置召回功能
- [ ] 智能匹配排序算法
- [ ] 高并发抢单机制
- [ ] 订单管理系统
- [ ] 评价反馈系统
- [ ] AI护理咨询助手（基于LangChain4j）

## 联系方式

- **项目名称**: 护联
- **仓库地址**: https://gitee.com/zong-leishun/care-link

## 许可证

Apache 2.0
