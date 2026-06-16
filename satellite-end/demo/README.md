# 🗺️ RoamAI — 基于LLM的个性化文旅智能导览系统

> 大创项目：基于LLM和LBS个性化文旅导览智能体应用研究与实现

一个融合大语言模型（MiMo）与地理位置服务的智能文旅导览后端系统，为游客提供AI驱动的景点讲解、路线推荐和个性化旅行建议。

## ✨ 核心功能

| 模块 | 功能 | 说明 |
|------|------|------|
| 🤖 AI智能导览 | 自然语言对话 | 接入MiMo大模型，支持景点问答、文化讲解、路线推荐 |
| 🏔️ 景点管理 | 景点CRUD + 搜索筛选 | 内置南岳衡山15个景点数据，支持按名称/类别筛选 |
| 👤 用户系统 | 注册/登录/个人信息 | 密码+邮箱验证码双登录，Gmail SMTP邮件服务 |
| ❤️ 收藏系统 | 本命景点收藏 | 用户可收藏/管理喜爱的景点 |
| ⚙️ Agent管理 | AI Agent配置 | 支持多Agent接入（MiMo/元器/通用），开发者后台管理 |

## 🛠️ 技术栈

```
后端框架: Spring Boot 3.x
ORM:      MyBatis + XML Mapper
数据库:   MariaDB (MySQL兼容)
缓存:     Redis
AI模型:   MiMo-v2.5-pro (小米大模型)
邮件:     Gmail SMTP (Spring Mail)
构建:     Maven
```

## 📁 项目结构

```
src/main/java/com/example/demo/
├── controller/          # REST API
│   ├── LoginController  # 认证：注册/登录/邮箱验证码/重置密码
│   ├── UserController   # 用户：信息查询/修改/注销
│   ├── HeroController   # 景点：列表/搜索/收藏管理
│   └── AgentController  # AI：Agent列表/对话调用/后台管理
├── service/             # 业务逻辑
│   └── impl/
│       ├── AgentServiceImpl   # AI调用核心（MiMo/元器/通用三协议）
│       ├── LoginServiceImpl   # 认证 + 邮箱验证码
│       └── HeroServiceImpl    # 景点 + 用户收藏
├── mapper/              # MyBatis接口
├── pojo/                # 实体类
├── dto/                 # 数据传输对象
├── common/              # 统一响应体 Result<T>
└── config/              # 跨域等配置
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- MariaDB / MySQL 10.x+
- Redis 6.x+

### 1. 创建数据库
```sql
CREATE DATABASE dormitory_autonomy_db CHARACTER SET utf8mb4;
```

### 2. 配置数据库连接
编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/dormitory_autonomy_db
    username: your_user
    password: your_password
```

### 3. 构建运行
```bash
mvn clean package -DskipTests
java -jar target/springboot-demo-0.0.1-SNAPSHOT.jar --server.port=8088
```

### 4. 测试接口
```bash
# 景点列表
curl http://localhost:8088/hero/list

# AI导览对话
curl -X POST http://localhost:8088/agent/use \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"agentConfigId":"1","message":"介绍一下南岳衡山"}'
```

## 📊 数据库设计

| 表名 | 说明 |
|------|------|
| `login` | 登录账号（手机号/邮箱/密码） |
| `user` | 用户资料（昵称/头像/签名） |
| `hero` | 景点信息（名称/类别） |
| `user_hero` | 用户收藏关联 |
| `t_agent_config` | AI Agent配置（provider/apiKey/baseUrl） |

## 🔌 API一览

### 认证 `/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 手机号注册（需验证码） |
| POST | `/auth/login/password` | 密码登录 |
| POST | `/auth/login/code` | 短信验证码登录 |
| POST | `/auth/login/email-code` | 邮箱验证码登录 |
| GET | `/auth/email-code?email=` | 发送邮箱验证码 |
| POST | `/auth/reset-password` | 重置密码 |

### 景点 `/hero`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/hero/list` | 景点列表（支持keyword/gender/role筛选） |
| GET | `/hero/my/list?userId=` | 我的收藏 |
| POST | `/hero/my/add` | 收藏景点 |
| POST | `/hero/my/remove` | 取消收藏 |

### AI导览 `/agent`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/agent/list?userId=` | 可用Agent列表 |
| POST | `/agent/use` | 与AI Agent对话 |
| GET | `/agent/manage/list` | 管理员：全部Agent配置 |
| POST | `/agent/manage/add` | 管理员：新增Agent |
| PUT | `/agent/manage/status` | 管理员：启用/禁用Agent |

## 📄 License

MIT License

---
> 🏫 衡阳师范学院 · 软件工程 · 大创项目
