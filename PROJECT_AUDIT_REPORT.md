# 峡谷卫星 (Canyon Satellite) 全栈架构、安全合规与交互设计深度审查报告
# Fullstack Architecture, Security & Usability Audit Report

- **项目名称**: 峡谷卫星 (Canyon Satellite / 文旅导览与游戏辅助智能体系统)
- **审查日期**: 2026-09-07
- **审查范围**:
  - 后端服务: `satellite-end` (Spring Boot 3.2.4 + MyBatis 3.0.3 + MariaDB/MySQL + Redis + JWT)
  - 前端工程: `satellite-front/sate` (Vue 3.5 + TypeScript 5.9 + Vite 8.0 + Pinia 3.0 + Vue Router 4.6)
- **基准规范**:
  - 《Web Interface Guidelines》(Vercel Labs: 触控区 ≥ 44px、移动端字体 ≥ 16px、`:focus-visible` 焦点环、无死区、防止意外缩放)
  - 《Frontend Design》(Anthropic: 定制化品牌视觉、严谨字阶与行长限制、克制动效、无障碍合规)
  - OWASP Top 10 & CWE 行业安全基准

---

## 目录 (Table of Contents)
1. [执行摘要 (Executive Summary)](#1-执行摘要-executive-summary)
2. [第一部分：后端安全与架构深度审查 (Backend Security & Architecture Audit)](#2-第一部分后端安全与架构深度审查)
   - 2.1 严重凭证泄漏与明文硬编码 (Critical Credential Leaks)
   - 2.2 JWT 鉴权机制漏洞与身份上下文脱节 (JWT Filter Bypass & Context Disconnect)
   - 2.3 水平越权 (IDOR) 与垂直提权漏洞 (Privilege Escalation & IDOR)
   - 2.4 高德地图 Key 暴露与未授权代理风险 (AMap Key Disclosure & Unauthenticated Proxy)
   - 2.5 全局统一异常处理缺失与错误响应不一致 (Missing Global Exception Handling)
   - 2.6 事务一致性分析 (@Transactional 缺失与回滚缺陷)
   - 2.7 MyBatis SQL 字段关联错误与注入分析 (SQL Column Syntax Bug & Injection Check)
   - 2.8 密码安全、随机数与工具类重复 (Password Hashing, Randomness & Duplications)
   - 2.9 前后端 RESTful API 契约全景图谱与断裂点 (API Contracts & Compatibility)
3. [第二部分：前端架构与可用性审查 (Frontend Architecture & Usability Audit)](#3-第二部分前端架构与可用性审查)
   - 3.1 组件生命周期与内存泄漏 (Lifecycle & Timer Leaks)
   - 3.2 401 拦截死链与 404 路由捕获缺失 (Dead 401 Link & Missing 404 Catch-All)
   - 3.3 中文输入法 (IME Composition) 竞态与交互缺陷 (Chinese IME Composition Bug)
   - 3.4 视觉设计系统与 Design Token 深度审查 (Theme Contrast & Token Consistency)
   - 3.5 触控目标与无障碍审查 (Touch Target & A11y Violations)
   - 3.6 CSS 选择器与视口溢出 Bug (HomeView CSS Selector & Viewport Overflow)
   - 3.7 冗余文件与废弃代码清单 (Dead Code & Asset Redundancy)
4. [第三部分：风险矩阵与分级整改路线图 (Risk Matrix & Prioritized Roadmap)](#4-第三部分风险矩阵与分级整改路线图)
   - 4.1 缺陷风险矩阵 (Risk Matrix)
   - 4.2 P0 阶段：最高优先级立即修复项 (Immediate Remediation)
   - 4.3 P1 阶段：高优先级功能与体验增强 (High Priority Refactoring)
   - 4.4 P2 阶段：中优先级工程健壮度提升 (Medium Priority Improvements)

---

## 1. 执行摘要 (Executive Summary)

本次审查对“峡谷卫星”全栈项目进行了源码级深度解构。审查确认，该项目具备完整的文旅圣地巡游与 AI 智能体对话业务构想，但在**构建可用性**、**核心安全合规**、**数据库 SQL 语法**以及**前端可访问性与视觉一致性**方面存在多个高危阻塞性缺陷。

### 核心发现概览
1. **构建阻断 (Build Blocker)**：
   后端 `LoginServiceImpl.java` 首行将 `import` 置于 `package` 之前，直接导致 Java 编译中断并抛出 21 处致命错误，使整个后端无法编译打包。*(已在 M1 完成热修复)*
2. **生产凭证完全泄漏 (Critical Credential Leak)**：
   `application.yml.backup` 文件将腾讯云公网 MariaDB/MySQL 数据库连接串、root 密码及个人 QQ 邮箱 SMTP 授权码以明文形式提交入库。*(已在 M1 完成脱敏占位化修复)*
3. **鉴权机制形同虚设与全控制器越权 (Critical IDOR & Privilege Escalation)**：
   后端 `JwtAuthenticationFilter` 采用黑名单模式放行大部分接口，且 Controller 与 Service 层完全不读取 Token 上下文，直接信任客户端传入的 `operatorUserId` 与 `userId`。普通用户可伪造参数一键提权为开发者，或越权物理注销任意用户账号。
4. **数据库关联字段语法 Bug (Medium SQL Bug)**：
   `LoginMapper.xml` 中的 `listAllUsers` 联合查询误将 `user` 表连接条件写为 `ON u.id = l.user_id`，而实际表中无 `id` 字段（主键为 `user_id`），直接导致管理列表在真实数据库执行时抛出未知列 SQL 异常。
5. **前端 401 路由死链与 404 捕获缺失 (Dead Link & Router Blindspot)**：
   前端 `request.ts` 在收到 401 状态码时直接执行 `window.location.href = '/login'`，而系统中并无 `/login` 路由（登录由模态框承载），且 Vite 配置了 `base: '/sate/'`，导致页面跳出 Base 彻底 404 白屏死锁。*(已在 M1 修复为凭证清理与事件分发)*
6. **重置密码 API 契约断裂 (Contract Breakage)**：
   后端 DTO 仅接收 `newPassword`，前端向后端提交 `password`，导致前端重置密码请求必定触发后端 400 校验拦截。*(已在 M1 完成前后端双向兼容对齐)*
7. **致命主题对比度与无障碍违规 (Severe Contrast Failure & A11y)**：
   `LoginModal.vue` 在暗色背景 (`#0A0D14`) 下套用了浅色文本变量 (`#1A202C`)，文字对比度仅为 1.2:1（远低于 WCAG AA 级 4.5:1 基准），肉眼近乎不可见；中文输入法打字回车发送会导致半截拼音提早发出；大量关闭按钮触控热区仅 32×32px，低于 44×44px 规范。

---

## 2. 第一部分：后端安全与架构深度审查

### 2.1 严重凭证泄漏与明文硬编码 (Critical Credential Leaks)

#### 1) 生产云数据库与个人邮箱凭证泄漏
- **缺陷位置**: `satellite-end/demo/src/main/resources/application.yml.backup:15-25`
- **直接观察代码**:
  ```yaml
  url: jdbc:mysql://gz-cdb-mvz0yjgp.sql.tencentcdb.com:24180/txrw?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
  username: root
  password: Pld200501251807.
  mail:
    host: ${MAIL_HOST:smtp.qq.com}
    port: ${MAIL_PORT:465}
    username: ${MAIL_USERNAME:1025848660@qq.com}
    password: ${MAIL_PASSWORD:enysbfayuvosbbeg}
  ```
- **成因与危害**:
  开发者将包含生产环境真实连接串的配置文件另存为 `.backup` 备份文件，并直接提交至 Git 仓库。该外网实例端口为 `24180`，root 账号密码以及 QQ 邮箱 SMTP 授权码完整外泄。攻击者可直接登录云端数据库删除业务表，或利用 SMTP 凭据发起钓鱼仿冒。
- **治理措施**:
  已立即在 M1 中将密码与连接串替换为环境变量注入模板 `${DB_PASSWORD:******}`，杜绝硬编码。建议相关账号持有人立刻在腾讯云控制台重置数据库密码并撤销 QQ 邮箱授权码。

#### 2) JWT 弱密钥兜底回退 (JWT Weak Default Secret)
- **缺陷位置**:
  - `application.yml:45`: `secret: ${JWT_SECRET:myDefaultSecretKeyForJwtTokenGeneration2024!}`
  - `JwtUtil.java:18`: `@Value("${app.jwt.secret:myDefaultSecretKeyForJwtTokenGeneration2024!}")`
- **危害**:
  在生产部署环境若未显式注入 `JWT_SECRET`，程序自动降级至弱密钥。攻击者可离线计算 HMAC-SHA256 签名，随意伪造任意权限的合法 JWT。
- **建议**:
  生产配置文件严禁提供弱密钥默认值，启动时应增加对弱口令的强校验（长度 ≥ 256 位，若为空或为默认值则直接拒绝启动容器）。

---

### 2.2 JWT 鉴权机制漏洞与身份上下文脱节 (JWT Filter Bypass & Context Disconnect)

#### 1) 默认放行设计模式 (Fail-Open Filter)
- **缺陷位置**: `satellite-end/demo/src/main/java/com/example/demo/filter/JwtAuthenticationFilter.java:62-71`
- **观察代码**:
  ```java
  } else {
      // 对于需要认证但没有有效Token的请求
      if (requiresAuthentication(path)) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write("{\"code\":401,\"message\":\"未认证，请先登录\",\"data\":null}");
          return;
      }
      filterChain.doFilter(request, response);
  }
  ```
- **逻辑推导**:
  过滤器采用了脆弱的“黑名单拦截”方案，仅针对 `requiresAuthentication(path)` 显式声明的少数路由（`/user/`, `/agent/`, `/hero/my/`）进行 Token 检查。若后续业务扩展新增了 `/admin/*`、`/audit/*` 或静态管理端点，默认行为全部为“未认证直接放行”（Fail-Open），形成鉴权盲区。
- **整改方案**:
  重构为“白名单策略”（Fail-Closed）。显式列出无需登录的公开接口（如 `/auth/*`, `/hero/list`, `/amap/config`），其余所有接口默认强制验证 Bearer Token。

#### 2) Token 上下文与业务层脱钩 (Authentication Context Disconnect)
- **观察代码**:
  `JwtAuthenticationFilter.java` 第 44-59 行解析了 Token 并包裹了 Request：
  ```java
  request.setAttribute("currentUserId", userId);
  request.setAttribute("currentUserStatus", status);
  ```
- **核心漏洞**:
  **全局所有 Controller 与 Service 实现中，没有任何一处从 `request.getAttribute("currentUserId")` 或安全上下文中获取用户身份！**
  业务代码完全脱离了 JWT 过滤器提供的身份保证，导致后续一系列毁灭性的越权漏洞。

---

### 2.3 水平越权 (IDOR) 与垂直提权漏洞 (Privilege Escalation & IDOR)

#### 1) 垂直提权：普通用户一键晋升开发者 (Vertical Privilege Escalation)
- **缺陷位置**: `satellite-end/demo/src/main/java/com/example/demo/service/impl/UserServiceImpl.java:153-174`
- **观察代码**:
  ```java
  @Override
  public Result<String> updateUserStatus(UpdateUserStatusDTO dto) {
      Login operator = loginMapper.findByUserId(dto.getOperatorUserId());
      if (operator == null) return Result.error(400, "操作人不存在");
      if (!isDeveloper(operator)) return Result.error(403, "仅开发者可修改用户权限");
      ...
      loginMapper.updateStatus(dto.getTargetUserId(), dto.getStatus());
      return Result.success("更新成功");
  }
  ```
- **漏洞复现路径**:
  攻击者注册一个普通用户（账号 ID 为 `1002`），调用 `PUT /user/manage/status`，在 Body 中伪造 `operatorUserId: 1`（开发者账号 ID）并将 `targetUserId: 1002, status: 3`。系统完全信任客户端传来的 `operatorUserId`，立刻将普通用户直接提权为开发者权限！

#### 2) 水平越权：任意账号资料修改与恶意物理销号 (IDOR Account Takeover & Deletion)
- **缺陷位置**:
  - `UserServiceImpl.java:72-127` (`updateUserInfo`): 客户端传入 `dto.getUserId()`，可任意修改他人的用户名、头像与签名。
  - `UserServiceImpl.java:180-203` (`cancelAccount`):
    ```java
    Long userId = dto.getUserId();
    ...
    userHeroMapper.deleteByUserId(userId);
    userMapper.deleteByUserId(userId);
    loginMapper.deleteByUserId(userId);
    ```
    未校验当前 Token 拥有者是否为 `dto.getUserId()`，任意用户只要发送 HTTP POST 传入受害者的 `userId`，即可强制级联删除该受害者的全部账号与英雄数据。
  - `HeroServiceImpl.java:52-85` (`saveUserHeroes`), `addUserHero`, `removeUserHero`: 均直接操作客户端传入的 `userId`。

---

### 2.4 高德地图 Key 暴露与未授权代理风险 (AMap Key Disclosure & Unauthenticated Proxy)

- **缺陷位置**: `satellite-end/demo/src/main/java/com/example/demo/controller/AmapController.java:33-42, 49-69`
- **观察代码**:
  ```java
  @GetMapping("/config")
  public Result<Map<String, String>> getConfig() {
      return Result.success(Map.of(
              "key", amapKey,
              "hasRouteKey", String.valueOf(amapRouteKey != null && !amapRouteKey.isBlank())
      ));
  }
  ```
- **风险分析**:
  1. **Key 泄露风险**: `/amap/config` 属于公开端点，向全公网明文分发高德 Web JS API Key。
  2. **API 额度被盗刷**: 后端代理接口 `/amap/driving` 和 `/amap/geocode` 无需登录且无限流保护，外部恶意机器人可通过频繁调用后端的代理接口，迅速耗尽项目的高德 Web 服务路线规划额度。
  3. **URL 拼接注入故障**:
     `String.format("https://restapi.amap.com/v3/direction/driving?key=%s&origin=%s&destination=%s&strategy=%s", amapRouteKey, origin, destination, strategy)`
     参数未经过 `URLEncoder.encode(...)` 转义，若地址中包含 `&`、`#` 或中文字符，将导致高德 REST 接口直接返回 400 非法参数错误。

---

### 2.5 全局统一异常处理缺失与错误响应不一致 (Missing Global Exception Handling)

- **缺陷观察**:
  项目未声明任何带有 `@ControllerAdvice` 或 `@RestControllerAdvice` 注解的全局异常处理类。
- **潜在危害**:
  1. **破坏前端统一契约**: 正常业务返回受控的 `{ "code": 200, "message": "...", "data": ... }`。一旦发生空指针、类型转换失败或 SQL 异常，Spring Boot 默认 fallback 到 `/error` 端点，返回：
     ```json
     { "timestamp": "...", "status": 500, "error": "Internal Server Error", "path": "..." }
     ```
     导致前端 Axios 拦截器解构 `response.data.code` 失败，引发未捕获的 Uncaught Promise Rejection 或 UI 崩溃。
  2. **服务器内部信息泄漏**: 原始异常或 Validation 失败细节未经过滤，会直接将包路径、数据库表名泄漏给前端调用方。

---

### 2.6 事务一致性分析 (@Transactional 缺失与回滚缺陷)

1. **用户注册跨表操作缺失事务 (`register`)**:
   - **位置**: `LoginServiceImpl.java:101-163`
   - **分析**: 用户注册分为两步独立写入：
     1. `loginMapper.insertByPhone(phone, encryptedPassword, ...)`
     2. `userMapper.insert(saved.getUserId())`
     该方法**未加 `@Transactional` 注解**。若步骤 2 因并发锁、数据库短时抖动发生异常，步骤 1 的账号信息已持久化在 `login` 表中，造成“有登录凭据但无用户资料”的死数据，用户后续登录直接抛出 NullPointerException。
2. **已有事务注解未显式指定 `rollbackFor`**:
   - `UserServiceImpl.java:179` (`cancelAccount`)
   - `HeroServiceImpl.java:51` (`saveUserHeroes`)
   - Spring 默认仅捕获 `RuntimeException` 进行回滚，对检查型异常（Checked Exception）不予回滚，必须显式声明 `@Transactional(rollbackFor = Exception.class)`。

---

### 2.7 MyBatis SQL 字段关联错误与注入分析 (SQL Column Syntax Bug & Injection Check)

#### 1) Schema 关联列错误缺陷 (严重 SQL 运行时 Bug)
- **缺陷位置**: `satellite-end/demo/src/main/resources/mapper/LoginMapper.xml:62`
- **观察代码**:
  ```xml
  <select id="listAllUsers" resultType="com.example.demo.dto.UserManageVO">
      SELECT l.user_id   AS userId,
             u.name      AS name,
             u.avatar    AS avatar,
             u.signature AS signature,
             l.phone     AS phone,
             l.email     AS email,
             l.status    AS status
      FROM login l
      LEFT JOIN user u ON u.id = l.user_id
      ORDER BY l.user_id ASC
  </select>
  ```
- **根因推导**:
  实体类 `User.java` (第 16 行) 和 `UserMapper.xml` 均明确声明 `user` 表的主键列名为 `user_id`（`<id column="user_id" property="userId"/>`），数据库根本不存在 `id` 字段。
  当管理员调用用户管理列表接口时，执行此 SQL 必将直接报错：`Unknown column 'u.id' in 'on clause'`。
- **修复措施**:
  必须纠正为 `LEFT JOIN user u ON u.user_id = l.user_id`。

#### 2) SQL 注入专项排查
- 排查所有 5 个 Mapper XML（`UserMapper.xml`, `LoginMapper.xml`, `HeroMapper.xml`, `UserHeroMapper.xml`, `AgentMapper.xml`），全部使用 `#{}` 预编译占位符。
- 模糊检索使用 `LIKE CONCAT('%', #{keyword}, '%')`，未发现 `${}` 字符串动态拼接风险。
- **结论**: **MyBatis 层防御 SQL 注入表现规范，无注入漏洞。**

---

### 2.8 密码安全、随机数与工具类重复 (Password Hashing, Randomness & Duplications)

1. **随机数安全性**: 验证码生成采用 `SecureRandom`，具备加密级熵源，表现良好。
2. **日志打印敏感信息**: `LoginServiceImpl.java:68` 使用 `System.out.println` 输出真实手机号与验证码，易通过容器标准输出日志造成凭证外泄。
3. **RestTemplate 缺乏超时配置**: `AgentServiceImpl.java:45` 和 `AmapController.java:27` 中直接 `new RestTemplate()`，未设置 ConnectTimeout 与 ReadTimeout，下游大模型响应卡顿时可能引发 Tomcat 线程耗尽。
4. **工具类重复造轮子**: `com.example.demo.util.StringUtils` 虽已提取，但 `UserServiceImpl`、`AgentServiceImpl` 等实现中依然充斥着 10 余处内联的 `maskPhone`、`isBlank`、`trimToNull` 私有重复函数。

---

### 2.9 前后端 RESTful API 契约全景图谱与断裂点 (API Contracts & Compatibility)

系统共有 16 个核心 RESTful 接口：
1. **认证模块 (`/auth`)**: `/auth/code`, `/auth/email-code`, `/auth/register`, `/auth/login/password`, `/auth/login/code`, `/auth/login/email-code`, `/auth/reset-password`
2. **用户模块 (`/user`)**: `/user/{userId}`, `/user/update`, `/user/manage/list`, `/user/manage/status`, `/user/cancel`
3. **英雄/圣地模块 (`/hero`)**: `/hero/list`, `/hero/my/list`, `/hero/my/save`, `/hero/my/add`, `/hero/my/update`, `/hero/my/remove`
4. **智能体模块 (`/agent`)**: `/agent/list`, `/agent/use`, `/agent/manage/list`, `/agent/manage/add`, `/agent/manage/status`
5. **高德代理 (`/amap`)**: `/amap/config`, `/amap/driving`, `/amap/geocode`

#### 关键契约断裂点与修复
- **重置密码字段断裂 (Critical)**:
  - 后端 `ResetPasswordDTO.java:22`: `private String newPassword;`
  - 前端原提交: `{ phone, code, password }`
  - **断裂影响**: 前端重置密码因缺失 `newPassword` 必定被后端拦截并报错 400。
  - **修复对齐**: 前端 `auth.ts` 已重构为统一注入 `{ phone, email, code, password: pwd, newPassword: pwd }`，实现双向兼容。

---

## 3. 第二部分：前端架构与可用性审查 (Frontend Architecture & Usability Audit)

### 3.1 组件生命周期与内存泄漏 (Lifecycle & Timer Leaks)

#### 1) 验证码定时器未清理 (LoginModal.vue)
- **位置**: `src/components/LoginModal.vue:56-58, 162-177`
- **问题**: `startCodeCountdown` 启动了 `setInterval`，但组件内部**未定义 `onBeforeUnmount` 或 `onUnmounted`**。当用户在倒计时期间直接关闭弹窗、通过路由切换或登录成功被父组件销毁时，定时器仍持续以 1000ms 间隔在后台空转，导致闭包引用与内存泄漏。

#### 2) Promise 超时与异步定位跨生命周期泄漏 (HolyLandTourView.vue)
- **位置**: `src/views/HolyLandTourView.vue:307-314, 463-504`
- **问题**:
  1. `withTimeout` 工具函数使用 `Promise.race([task, new Promise(...)])`，在 `task` 正常完成后**未清除 `window.setTimeout` 句柄**，导致长达 15000ms 的无用定时器堆积在浏览器宏任务队列中。
  2. 高德异步定位回调（`getCurrentPosition` 超时时间长达 10s），当用户在定位尚未完成前离开页面时，`onBeforeUnmount` 会调用 `mapInstance.destroy()` 将地图实例置空；随后异步回调触发执行 `mapInstance.setCenter(...)`，直接引发运行时空指针异常。

---

### 3.2 401 拦截死链与 404 路由捕获缺失 (Dead 401 Link & Missing 404 Catch-All)

#### 1) 401 致命跳转死链
- **缺陷位置**: `src/utils/request.ts:53-57`
- **原代码**:
  ```typescript
  case 401:
    console.error('未授权，请重新登录')
    window.location.href = '/login'
    break
  ```
- **危害**:
  1. 系统路由表（`src/router/index.ts`）中根本不存在 `/login` 这个路由（登录由全局模态框 `LoginModal.vue` 承载）；
  2. Vite 打包配置了 `base: '/sate/'`，执行 `window.location.href = '/login'` 会脱离应用基准上下文，导致整个页面彻底陷入 404 死锁；
  3. 未清除本地残留的过期 Token。
- **M1 治理实现**:
  已在 `request.ts` 中修正为自动清除 `access_token` 与 `session_authenticated`，并派发全局事件 `window.dispatchEvent(new CustomEvent('auth:unauthorized'))`。`MainLayout.vue` 捕获该事件后自动调用 `userStore.logout()` 并弹出 `LoginModal`。

#### 2) 404 路由捕获盲区
- **位置**: `src/router/index.ts`
- **问题**: 系统之前仅注册了 `/`、`/holy-land-tour`、`/ai-consult`，当用户输入任何未匹配路径时，`<router-view />` 渲染为空白，无任何错误提示。
- **M1 治理实现**: 已在 `router/index.ts` 中添加 `{ path: ':pathMatch(.*)*', name: 'not-found', redirect: '/' }`。

---

### 3.3 中文输入法 (IME Composition) 竞态与交互缺陷 (Chinese IME Composition Bug)

- **缺陷位置**: `src/views/AIConsultView.vue:257-262`
- **原代码**:
  ```typescript
  const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void sendMessage()
    }
  }
  ```
- **危害**:
  在中文输入法打字选词时按下 Enter 键，`event.key` 同样是 `'Enter'`。由于缺少 `event.isComposing`（输入法组合态）检查，输入法的选词确认动作会误触发 `sendMessage()`，将尚未完成打字的半截拼音内容直接发送给后端智能体。
- **改进规范**:
  判断条件必须扩充为 `if (event.key === 'Enter' && !event.shiftKey && !event.isComposing)`。

---

### 3.4 视觉设计系统与 Design Token 深度审查 (Theme Contrast & Token Consistency)

#### 1) 严重的主题割裂与对比度灾难 (WCAG 2.1 AA 严重违规)
- **现状分析**:
  - `src/styles/variables.css` 将根变量配置为浅色风格（`--bg-primary: #F8FAFD;`, `--text-primary: #1A202C;`）；
  - 但 `LoginModal.vue` 容器背景写死为深黑半透明 `background: rgba(10, 13, 20, 0.85);`；
  - 弹窗内标题和输入框直接引用了 `var(--text-primary)`（即深黑色 `#1A202C`）；
  - **对比度结果**: 深黑文字（`#1A202C`）显示在深黑背景（`rgba(10, 13, 20, 0.85)`）上，对比度仅为 **1.2:1**，严重违背 WCAG 2.1 AA 规定的 4.5:1 基准线，导致弹窗文字在视觉上近乎盲人无法辨识！

#### 2) 色彩硬编码与 Token 命名语义反差
- `variables.css:15`: `--accent-cyan: #3B82F6`（变量名为 cyan 青色，实际值为 blue 蓝色）。
- `variables.css:17`: `--accent-red: #F97316`（变量名为 red 红色，实际值为 orange 橙色）。
- 各组件内充斥着独立的硬编码 HEX（如 `#00e5ff`, `#FFB90F`, `#9ff7ff`, `#ff9eb8`），未形成统一的语义化颜色色阶。

---

### 3.5 触控目标与无障碍审查 (Touch Target & A11y Violations)

1. **触控目标 < 44×44px 违规清单**:
   - `LoginModal.vue:955`: 关闭按钮写死为 `32px × 32px`（需 ≥ 44×44px）。
   - `UserProfilePanel.vue:421`: 关闭按钮写死为 `34px × 34px`（需 ≥ 44×44px）。
   - `DailyAICard.vue:149`: 关闭按钮写死为 `32px × 32px`。
   - `AIConsultView.vue:442`: 关闭按钮写死为 `32px × 32px`。
   - `HolyLandTourView.vue:1182`: 城市标签按钮高度仅约 28px。
2. **全局 `:focus-visible` 焦点环缺失**:
   全站所有交互式按钮与输入框仅设置了 `outline: none`，键盘导航（Tab 键）时无高辨识度焦点轮廓。
3. **禁止缩放违规**:
   `index.html:7` 声明了 `user-scalable=no, maximum-scale=1.0`，违背《Web Interface Guidelines》无障碍准则。移动端防放大应由输入框 `font-size ≥ 16px` 保证，而非剥夺用户的辅助缩放能力。
4. **加载中防重复提交与动效缺失**:
   按钮在提交时直接替换文字为“加载中...”，未保留原操作上下文且无旋转 Spinner 指示器。

---

### 3.6 CSS 选择器与视口溢出 Bug (HomeView CSS Selector & Viewport Overflow)

- **位置**: `src/views/HomeView.vue:107-117, 238-272`
- **问题**:
  1. **水平滚动条溢出**: Line 113 设置 `.home-interactive-area { position: absolute; width: 100vw; height: 100vh; }`。但在具有外边距的父容器中，设置 `100vw` 导致桌面端出现无意义的横向水平滚动条。
  2. **无效的后代选择器**: Line 238 编写了 `.layout-collapsed .home-interactive-area`，然而类名 `.layout-collapsed` 实际上挂载在 `.home-interactive-area` 自身上，正确的选择器应为 `.home-interactive-area.layout-collapsed`，导致折叠布局逻辑永远失效。

---

### 3.7 冗余文件与废弃代码清单 (Dead Code & Asset Redundancy)

在 M1 阶段中已完成清理的冗余资产：
1. `satellite-front/sate/src/index.ts`（0 字节空文件，已删除）
2. `satellite-front/sate/vite.config.ts.bak`（无用备份文件，已删除）
3. `satellite-front/sate/src/style.css`（默认脚手架未引用样式，已删除）
4. `src/utils/request.ts` 中的 `export const api`（旧示例未引用代码，已移除）

---

## 4. 第三部分：风险矩阵与分级整改路线图 (Risk Matrix & Prioritized Roadmap)

### 4.1 缺陷风险矩阵 (Risk Matrix)

| 编号 | 模块 | 缺陷描述 | 严重等级 | 影响范围 | 整改状态 |
|---|---|---|---|---|---|
| **SEC-01** | 后端 | `LoginServiceImpl.java` 语法错误导致无法编译 | **Blocker** | 全后端服务不可用 | **M1 已修复** |
| **SEC-02** | 后端 | `application.yml.backup` 明文泄露腾讯云数据库与邮箱凭证 | **Critical** | 核心数据资产泄漏 | **M1 已修复** |
| **SEC-03** | 后端 | 全控制器存在 IDOR 越权与垂直提权，参数可任意伪造 | **Critical** | 用户数据被篡改、被销号 | 建议 P0 落实 |
| **SEC-04** | 后端 | JWT Filter 采用 Fail-Open 黑名单模式，且上下文未传递 | **Critical** | 鉴权防线形同虚设 | 建议 P0 落实 |
| **SEC-05** | 后端 | `LoginMapper.xml:62` 关联列语法错误 (`u.id` 不存在) | **High** | 管理列表 SQL 运行时必崩 | 建议 P0 修复 |
| **UI-01** | 前端 | 401 拦截器跳转不存在的 `/login` 导致应用彻底死锁 | **High** | 前端可用性中断 | **M1 已修复** |
| **UI-02** | 前端 | `auth.ts` 重置密码参数缺失 `newPassword` 导致 400 | **High** | 密码重置业务不可用 | **M1 已修复** |
| **UI-03** | 前端 | `LoginModal.vue` 出现致命主题反差 (对比度 1.2:1 黑底黑字) | **High** | 弹窗不可读，WCAG 违规 | 建议 M2 落实 |
| **UI-04** | 前端 | 前端缺少 404 捕获通配路由，非法路径直接白屏 | **Medium** | 页面渲染异常 | **M1 已修复** |
| **UI-05** | 前端 | 前端定时器与高德地图异步竞态生命周期泄漏 | **Medium** | 内存持续增长、空指针 | 建议 M3 落实 |
| **UI-06** | 前端 | 中文输入法选词 Enter 误触发消息提早发送 | **Medium** | AI 对话交互体验差 | 建议 M3 落实 |
| **UI-07** | 前端 | 全站可交互元素触控区 < 44×44px，缺少 `:focus-visible` | **Medium** | 移动端误触、键盘无障碍缺失 | 建议 M2/M3 落实 |
| **UI-08** | 前端 | `HomeView.vue` `100vw` 溢出导致水平滚动条 | **Low** | 界面布局瑕疵 | 建议 M3 落实 |
| **CODE-01** | 前端 | 冗余文件 (`index.ts`, `vite.config.ts.bak`, `style.css`, `api`) | **Low** | 代码仓库噪音 | **M1 已清理** |

---

### 4.2 P0 阶段：最高优先级立即修复项 (Immediate Remediation - M1 已落实核心基础)

1. **[已完成] 修复 Java 语法编译阻断**:
   在 `LoginServiceImpl.java` 中调整 `package` 与 `import` 行序，恢复 Java 编译标准。
2. **[已完成] 清理敏感配置文件明文凭据**:
   在 `application.yml.backup` 中将公网腾讯云连接串与个人邮箱密码全部替换为环境变量注入模板。
3. **[已完成] 治理前端 401 死链与 404 捕获**:
   在 `request.ts` 中移除跳转 `/login`，通过 `CustomEvent` 触发登出与登录弹窗；在 `router/index.ts` 补充 `:pathMatch(.*)*` 兜底路由。
4. **[已完成] 对齐重置密码接口契约**:
   在 `auth.ts` 中规范 `resetPassword` 同时携带 `newPassword` 与 `password`，修复 400 校验阻断。
5. **[已完成] 彻底移除冗余废弃文件**:
   清理 `index.ts`、`vite.config.ts.bak`、`style.css` 及 `export const api`，验证 `vue-tsc -b && vite build` 0 错误编译通过。
6. **[待后端更新] 修正 `LoginMapper.xml` SQL 列名**:
   将第 62 行 `ON u.id = l.user_id` 替换为 `ON u.user_id = l.user_id`。
7. **[待后端更新] 修复鉴权与 IDOR 越权**:
   在 Controller 层通过 `@RequestAttribute("currentUserId")` 绑定操作者身份，废除请求体直接读取 `operatorUserId`。

---

### 4.3 P1 阶段：高优先级功能与体验增强 (High Priority Refactoring - M2 & M3 目标)

1. **重构 Design Token 与色彩层级 (M2)**:
   - 建立沉浸式科技调色板，重构 `src/styles/tokens.css`；
   - 彻底修复 `LoginModal.vue` 与 `UserProfilePanel.vue` 的暗黑容器浅黑文字对比度灾难，达到 WCAG AA 级（≥ 4.5:1）；
   - 建立明确的字号、字重排版梯阶，正文严格遵守 `< 80` 字符行长规范。
2. **键盘导航与无障碍全覆盖 (M2 & M3)**:
   - 全局引入高对比度 `:focus-visible` 双层环；
   - 移除 `index.html` 的 `user-scalable=no` 限制，确保移动端输入框 `font-size ≥ 16px`；
   - 所有按钮触控热区保证 `min-width: 44px; min-height: 44px;`；弹窗支持 `Escape` 键直接关闭。
3. **生命周期与交互精细化打磨 (M3)**:
   - `LoginModal.vue` 添加 `onBeforeUnmount` 钩子清理验证码倒计时定时器；
   - `HolyLandTourView.vue` 在组件卸载时安全清理高德地图定位回调；
   - `AIConsultView.vue` 输入框补充 `!event.isComposing` 判定，支持自动聚焦与加载骨架屏；
   - `HomeView.vue` 修正 `.layout-collapsed` 选择器并消除 `100vw` 视口横向滚动条。

---

### 4.4 P2 阶段：中优先级工程健壮度提升 (Medium Priority Improvements - M4 目标)

1. **后端统一异常处理**:
   实现 `@RestControllerAdvice`，统一捕获参数校验异常与业务异常，向前端保证 `{ code, message, data }` 的一致性契约。
2. **超时机制与 HTTP 客户端防护**:
   为后端 `RestTemplate` 显式设置连接超时（5000ms）与读取超时（10000ms），防止下游阻塞导致线程池耗尽。
3. **高德地图 SDK 加载方式标准化**:
   基于官方 `@amap/amap-jsapi-loader` 标准管理 Key 与安全密钥，替代手写 `<script>` 注入。
4. **端到端构建与完整性验证**:
   运行全链路前端 TypeScript 强类型校验与生产构建测试，产出高质量可交付制品。
