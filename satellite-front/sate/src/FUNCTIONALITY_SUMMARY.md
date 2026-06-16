# 峡谷卫星功能总结与待实现方案

本文档基于当前 `src` 目录源码整理，覆盖现有完整功能、半实现功能、未实现功能、实现方式、交互效果和后续推荐实现路径。

最后扫描时间：2026-04-20

## 1. 项目整体定位

`峡谷卫星` 当前是一个基于 `Vue 3 + TypeScript + Vite` 的前端应用，整体视觉方向是电竞、卫星指挥台、玻璃拟态、地图文旅路线规划。

项目当前核心由四条主线组成：

1. 用户账号与资料体系。
2. 首页功能入口与每日 AI 卡片。
3. AI 全能咨询，对接后端 Agent 列表与 Agent 调用接口。
4. 圣地巡游，对接高德地图和“圣地巡游”智能体生成城市景点路线。

当前已具备较完整前端交互闭环的是：登录/注册/重置密码、本命英雄选择、个人资料编辑、AI Agent 聊天、圣地巡游地图路线规划。未完成或只有入口的是：赛事追踪、帮选英雄、对局分析、社区、独立登录页、真实每日 AI 内容生成。

## 2. 技术栈与运行结构

### 2.1 技术栈

| 类别 | 当前使用 | 说明 |
| --- | --- | --- |
| 构建工具 | Vite | 开发服务器、构建、代理配置。 |
| 框架 | Vue 3 | 使用 `<script setup lang="ts">` 写法。 |
| 类型系统 | TypeScript | `strict`、`noUnusedLocals`、`noUnusedParameters` 等严格配置已开启。 |
| 状态管理 | Pinia | 用户状态和圣地巡游状态分别独立管理。 |
| 路由 | vue-router | 当前注册首页、AI 咨询、圣地巡游。 |
| HTTP | axios | `src/utils/request.ts` 做统一请求封装。 |
| 地图 | 高德地图 JS API | 圣地巡游页面使用 JSAPI、地理编码、定位、驾车路线规划。 |
| 第三方定位 | 浏览器 Geolocation + Nominatim | 个人资料面板定位城市时使用。 |

### 2.2 入口关系

| 文件 | 作用 |
| --- | --- |
| `src/main.ts` | 应用入口，加载全局变量样式，创建 Vue app，挂载 Pinia 和 router。 |
| `src/App.vue` | 根组件，只渲染 `<router-view />`。 |
| `src/router/index.ts` | 路由表，使用 `MainLayout` 作为主布局容器。 |
| `src/layout/MainLayout.vue` | 主布局，包含视频背景、导航栏、用户区域、登录弹窗、个人资料弹窗和二级路由出口。 |
| `src/styles/variables.css` | 全局视觉变量，包括颜色、玻璃效果、层级、导航高度等。 |
| `src/style.css` | 全局样式。 |

### 2.3 当前路由表

当前已经注册：

| 路径 | 页面 | 状态 |
| --- | --- | --- |
| `/` | `HomeView.vue` | 已实现。 |
| `/ai-consult` | `AIConsultView.vue` | 已实现基础 Agent 聊天。 |
| `/holy-land-tour` | `HolyLandTourView.vue` | 已实现地图路线规划与智能体景点推荐。 |

当前入口存在但路由未注册：

| 入口路径 | 来源 | 当前问题 |
| --- | --- | --- |
| `/matches` | 首页“赛事追踪”卡片 | 路由表未注册，点击后没有对应页面。 |
| `/hero-pick` | 首页“帮选英雄”卡片 | 路由表未注册，点击后没有对应页面。 |
| `/login` | 请求封装 401 跳转 | 当前没有独立登录页，401 会跳到不存在路径。 |

## 3. 已实现功能总览

| 模块 | 功能 | 完成度 | 主要文件 |
| --- | --- | --- | --- |
| 主布局 | 视频背景、玻璃导航、登录入口、用户资料入口 | 已实现 | `layout/MainLayout.vue` |
| 首页 | 每日 AI 卡片、四个功能入口卡片 | 部分实现 | `views/HomeView.vue`、`components/DailyAICard.vue` |
| 登录弹窗 | 密码登录、手机验证码登录、邮箱验证码登录、注册、重置密码 | 已实现前端流程 | `components/LoginModal.vue`、`store/user.ts`、`api/auth.ts` |
| 本命英雄初始化 | 登录后强制选择 1-5 个本命英雄 | 已实现 | `components/LoginModal.vue`、`components/MainHeroSelector.vue`、`api/hero.ts` |
| 个人资料 | 查看资料、编辑网名/头像/签名/城市/本命英雄、退出登录 | 已实现前端流程 | `components/UserProfilePanel.vue`、`store/user.ts` |
| 用户状态 | token、本地会话、用户资料、本命英雄、城市缓存 | 已实现 | `store/user.ts` |
| AI 咨询 | 拉取 Agent 列表、选择 Agent、发送消息、展示回复 | 已实现基础版 | `views/AIConsultView.vue`、`api/agent.ts` |
| 圣地巡游 | 高德地图、定位、起终点路线规划、城市快捷选择、景点推荐 | 已实现基础版 | `views/HolyLandTourView.vue`、`store/holyTour.ts` |
| 圣地巡游智能体 | 自动匹配“圣地巡游/圣地巡礼/文旅”Agent，生成 JSON 景点路线 | 已实现前端接入 | `store/holyTour.ts`、`api/agent.ts` |
| 地图加载 | 动态加载高德 JSAPI，缓存加载 Promise | 已实现 | `utils/amapLoader.ts` |
| 城市定位 | 浏览器定位 + 逆地理编码得到城市 | 已实现 | `utils/geolocation.ts` |
| API 请求 | axios 统一 baseURL、token 注入、错误处理 | 已实现基础版 | `utils/request.ts` |

## 4. 主布局功能详情

### 4.1 功能描述

主布局负责整个应用的沉浸式外壳：

1. 全屏视频背景。
2. 深色渐变遮罩，保证内容可读性。
3. 固定顶部导航栏。
4. Logo 点击回首页。
5. 登录状态下显示头像、昵称和“查看资料”。
6. 未登录状态下显示登录按钮。
7. 全局挂载登录弹窗和个人资料弹窗。
8. 登录后预加载圣地巡游智能体和当前城市景点路线。

### 4.2 实现方式

主要文件：`src/layout/MainLayout.vue`

核心实现：

1. 使用 `useRouter` 和 `useRoute` 控制导航和判断当前页面。
2. 使用 `useUserStore` 读取登录态、显示名、头像。
3. 使用 `useHolyTourStore` 在登录后提前初始化“圣地巡游”Agent。
4. 使用 `showLoginModal` 控制登录弹窗显示。
5. 使用 `showProfilePanel` 控制个人资料面板显示。
6. 使用 `watch` 监听 `isAuthenticated` 和 `needsHeroSelection`，如果用户已登录但没有选择本命英雄，则强制打开登录弹窗并进入本命英雄选择流程。
7. `onMounted` 时执行 `userStore.bootstrapSession()`，从本地 token、userId、session flag 恢复会话。

### 4.3 当前效果

用户进入页面后会看到沉浸式视频背景和玻璃导航。未登录时可以点击“登录”打开弹窗；登录后导航栏显示头像、昵称和查看资料入口。进入圣地巡游之前，布局会尝试提前拉取智能体数据，减少圣地巡游页面等待时间。

### 4.4 当前不足

1. 顶部导航的“对局分析”“AI 助手”“社区”大部分只是静态 `<a>`，没有完整路由行为。
2. 当前导航高亮写死在首页项上，没有根据当前路由动态高亮。
3. 401 请求错误跳转到 `/login`，但主布局实际使用的是登录弹窗。

### 4.5 推荐实现方法

1. 建立统一导航配置数组，例如：

```ts
const navItems = [
  { label: '首页', path: '/' },
  { label: '对局分析', path: '/match-analysis' },
  { label: 'AI 助手', path: '/ai-consult' },
  { label: '社区', path: '/community' },
]
```

2. 使用 `route.path` 或 `route.name` 判断当前 active 状态。
3. 对未完成页面临时接入占位页，避免点击后空白。
4. 将 axios 的 401 处理改为事件通知或状态清理，不直接跳不存在的 `/login`。

## 5. 首页功能详情

### 5.1 功能描述

首页包含两个主要区域：

1. 每日 AI 卡片。
2. 四个功能入口卡片。

四个入口分别是：

| 入口 | 路径 | 当前状态 |
| --- | --- | --- |
| AI 全能咨询 | `/ai-consult` | 可用。 |
| 赛事追踪 | `/matches` | 只有入口，页面未实现。 |
| 帮选英雄 | `/hero-pick` | 只有入口，页面未实现。 |
| 圣地巡游 | `/holy-land-tour` | 可用。 |

### 5.2 实现方式

主要文件：

1. `src/views/HomeView.vue`
2. `src/components/DailyAICard.vue`

首页使用 `isCardVisible` 控制每日 AI 卡片是否显示。卡片关闭后，首页布局从“中央卡片 + 左右按钮组”切换到“四个功能卡片底部排列”。

`DailyAICard` 使用本地 reactive mock 数据生成当天日期、年份、月份、星期，并展示固定节气、英雄、台词和背景图。

### 5.3 当前效果

用户进入首页后，中央偏下显示每日 AI 卡片，左右两侧显示功能入口。关闭每日 AI 卡片后，理论上功能入口会收拢为底部四宫格。

### 5.4 当前不足

1. `DailyAICard` 数据是 mock，不是后端或 AI 生成。
2. `solarTerm` 固定为“清明”，不会根据真实日期计算。
3. 英雄名、英雄称号、台词和背景图都是固定数据。
4. `/matches` 和 `/hero-pick` 没有路由页面。
5. 折叠布局 CSS 选择器疑似写错：当前 `.layout-collapsed .home-interactive-area` 表示子元素选择，但实际 class 在同一个元素上，应该使用 `.home-interactive-area.layout-collapsed`。

### 5.5 推荐实现方法

#### 每日 AI 卡片真实化

新增接口：

```ts
GET /daily-card/today
```

推荐返回：

```json
{
  "code": "200",
  "message": "success",
  "data": {
    "date": "20",
    "year": 2026,
    "month": "04",
    "weekday": "周一",
    "solarTerm": "谷雨",
    "heroName": "李白",
    "heroTitle": "青莲剑仙",
    "quote": "大河之剑天上来！",
    "bgUrl": "https://...",
    "heroImgUrl": "https://..."
  }
}
```

前端新增：

1. `src/api/daily.ts`，封装 `getDailyCard()`。
2. `src/store/daily.ts`，缓存当天卡片，避免重复请求。
3. `DailyAICard.vue` 改为接收 props 或直接读取 store。
4. 请求失败时保留本地 fallback 数据。

预期效果：每日卡片根据日期和用户资料变化，例如根据本命英雄生成当天推荐语和战术建议。

#### 首页入口补齐

1. 新建 `MatchAnalysisView.vue` 或 `MatchesView.vue`。
2. 新建 `HeroPickView.vue`。
3. 在 `router/index.ts` 注册 `/matches` 和 `/hero-pick`。
4. 如暂时不开发完整功能，先做占位页，说明“建设中”，避免空路由。

## 6. 登录、注册、重置密码功能详情

### 6.1 功能描述

登录弹窗支持四种模式：

| 模式 | 当前能力 |
| --- | --- |
| 登录 | 支持账号密码、手机验证码、邮箱验证码。 |
| 注册 | 支持手机号注册、邮箱注册，注册成功后自动尝试密码登录。 |
| 重置密码 | 支持手机号验证码重置密码。 |
| 本命英雄设置 | 登录后如果没有本命英雄，强制选择 1-5 个英雄。 |

### 6.2 实现方式

主要文件：

1. `src/components/LoginModal.vue`
2. `src/store/user.ts`
3. `src/api/auth.ts`
4. `src/api/hero.ts`
5. `src/components/MainHeroSelector.vue`

核心流程：

1. `LoginModal.vue` 维护 `currentMode`，可切换 `login`、`register`、`reset`、`heroSetup`。
2. 登录方式由 `currentLoginMethod` 控制，支持 `password`、`phoneCode`、`emailCode`。
3. 注册方式由 `currentRegisterMethod` 控制，支持 `phone` 和 `email`。
4. 获取验证码时调用 `authApi.sendPhoneCode()` 或 `authApi.sendEmailCode()`。
5. 发送验证码成功后启动 60 秒倒计时。
6. 登录成功后调用 `userStore` 保存 token、用户信息和会话状态。
7. 如果 `userStore.needsHeroSelection` 为 true，则进入本命英雄设置。
8. 本命英雄保存时，将英雄名称映射成后端 heroId，再调用 `heroApi.saveMyHeroes()`。

### 6.3 后端接口依赖

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/auth/code` | 发送手机验证码。 |
| `GET` | `/auth/email-code` | 发送邮箱验证码。 |
| `POST` | `/auth/register` | 注册。 |
| `POST` | `/auth/login/password` | 密码登录。 |
| `POST` | `/auth/login/code` | 手机验证码登录。 |
| `POST` | `/auth/login/email-code` | 邮箱验证码登录。 |
| `POST` | `/auth/reset-password` | 重置密码。 |
| `GET` | `/hero/list` | 获取英雄列表。 |
| `GET` | `/hero/my/list` | 获取用户已选本命英雄。 |
| `POST` | `/hero/my/save` | 保存本命英雄。 |

### 6.4 当前效果

用户可以在一个弹窗内完成登录、注册、重置密码。本命英雄没有设置时，系统会阻止用户关闭登录流程并引导选择英雄。选择器支持搜索、职业分组、已选计数、取消选择和最多 5 个限制。

### 6.5 当前不足

1. 手机号、邮箱格式校验较基础，目前主要校验非空和密码长度。
2. 重置密码只支持手机号，不支持邮箱找回。
3. 注册成功后的自动登录依赖密码登录接口，如果后端注册返回 token，当前没有优先使用注册返回 token。
4. 验证码倒计时组件内维护，弹窗关闭时需要确保计时器清理完整。
5. token 刷新机制未实现。
6. `session_authenticated` 可能让没有 token 的状态也被认为已认证，安全边界需要后端配合确认。

### 6.6 推荐实现方法

#### 表单校验增强

1. 手机号使用正则校验。
2. 邮箱使用正则校验。
3. 密码强度增加提示，例如长度、字母、数字、特殊字符。
4. 后端错误信息透传给用户，而不是统一显示“登录失败”。

#### token 刷新机制

新增接口：

```ts
POST /auth/refresh
```

前端改造：

1. 登录后同时保存 `access_token` 和 `refresh_token`。
2. axios 响应拦截器遇到 401 时尝试 refresh。
3. refresh 成功后重放原请求。
4. refresh 失败时清理用户状态并打开登录弹窗。

预期效果：用户长期使用时不会因为 access token 短期过期突然退出。

## 7. 本命英雄选择功能详情

### 7.1 功能描述

本命英雄选择是登录流程和个人资料编辑中的共用能力。用户可以从后端英雄列表中搜索英雄，并选择 1-5 个作为本命英雄。

### 7.2 实现方式

主要文件：`src/components/MainHeroSelector.vue`

组件设计：

1. 使用 `v-model` 接收和更新已选英雄名称数组。
2. `heroes` props 接收后端英雄列表。
3. `limit` props 控制最大选择数量，默认 5。
4. 内部维护 `keyword`，变化时向父组件 emit `search-change`。
5. 根据英雄职业 `profession` 分组展示。
6. 点击英雄卡片切换选择状态。
7. 超过上限时 emit `limit-exceeded`。

### 7.3 当前效果

用户可以快速搜索英雄名或职业，并在职业分组里选择。已选英雄以 chip 形式展示，点击 chip 可移除。达到上限后未选卡片进入禁用视觉状态。

### 7.4 当前不足

1. 组件当前用英雄名称作为选择值，保存时再由父组件映射 heroId。如果存在重名英雄，会有歧义。
2. 搜索每次输入都会触发父组件请求后端，当前没有 debounce。
3. 英雄头像、阵营、定位、熟练度等扩展信息没有展示。

### 7.5 推荐实现方法

1. 将 `v-model` 从 `string[]` 改为 `heroId[]`，展示时根据 id 查名称。
2. 在组件内部或父组件搜索处理处增加 300ms debounce。
3. `HeroOption` 扩展字段：`avatar`、`roles`、`difficulty`、`tags`。
4. 保存本命英雄时直接提交 heroId，不再通过名称反查。

预期效果：选择逻辑更稳定，后端数据变复杂后也不会因为重名或改名导致保存失败。

## 8. 个人资料功能详情

### 8.1 功能描述

登录后点击导航栏用户区域打开个人资料面板。面板支持：

1. 查看头像、昵称、签名、城市、本命英雄、手机号、邮箱。
2. 编辑昵称、头像 URL、个性签名、城市、本命英雄。
3. 自动定位城市。
4. 保存资料。
5. 退出登录。

### 8.2 实现方式

主要文件：

1. `src/components/UserProfilePanel.vue`
2. `src/store/user.ts`
3. `src/api/user.ts`
4. `src/utils/geolocation.ts`

核心流程：

1. 面板打开时执行 `userStore.fetchUserProfile()` 和 `loadHeroOptions()`。
2. `syncEditForm()` 将 store 里的用户资料复制到编辑表单。
3. 点击“编辑资料”进入编辑模式。
4. 保存时校验昵称非空、昵称长度、签名长度、本命英雄数量。
5. 调用 `userStore.updateUserProfile()` 更新基础资料。
6. 调用 `userStore.saveMainHeroes()` 保存本命英雄。
7. 城市字段当前通过 `userStore.setLocalCity()` 保存到本地状态和 localStorage。
8. 点击退出登录时清理 token、userId、city、session flag 和用户资料。

### 8.3 城市定位实现

主要文件：`src/utils/geolocation.ts`

流程：

1. 使用 `navigator.geolocation.getCurrentPosition()` 获取经纬度。
2. 调用 `https://nominatim.openstreetmap.org/reverse` 逆地理编码。
3. 从响应中提取 city、town、municipality、county、state 等字段。
4. 返回 `{ city, latitude, longitude }`。

### 8.4 当前效果

用户可以在个人中心完成资料维护。城市定位成功后会显示“定位成功：城市名”，并填入编辑表单。保存后，昵称、头像、签名、本命英雄会刷新到导航栏和个人资料展示中。

### 8.5 当前不足

1. 城市没有通过 `updateUserInfo` 提交给后端，目前只保存在前端本地。
2. 头像只支持 URL 输入，没有上传能力。
3. 手机号、邮箱只能展示，不能绑定、解绑或更换。
4. 定位使用 Nominatim 公网服务，生产环境可能遇到限流、跨域、合规和稳定性问题。
5. 保存基础资料和保存本命英雄是两个接口调用，缺少事务一致性。可能出现资料保存成功但英雄保存失败。

### 8.6 推荐实现方法

#### 城市持久化

后端 `UpdateUserDTO` 增加 `city` 字段。前端 `src/api/user.ts` 和 `src/store/user.ts` 同步扩展：

```ts
export interface UpdateUserDTO {
  id: string
  userId: string
  name: string
  avatar: string
  signature: string
  phone: string
  email: string
  city?: string
}
```

保存时将 `city` 一起传给 `/user/update`。成功后由后端返回的用户资料覆盖前端本地状态。

#### 头像上传

新增接口：

```ts
POST /upload/avatar
```

前端实现：

1. `UserProfilePanel.vue` 增加文件选择。
2. 校验图片类型、大小和尺寸。
3. 使用 `FormData` 上传。
4. 上传成功后把返回 URL 写入 `editForm.avatar`。
5. 用户点击保存时再更新资料。

预期效果：用户不需要自己找图片 URL，资料编辑体验更完整。

## 9. 用户状态管理详情

### 9.1 功能描述

`useUserStore` 是用户侧核心状态中心，负责：

1. 登录 token。
2. 本地 session 状态。
3. userId 持久化。
4. 城市持久化。
5. 用户资料。
6. 本命英雄。
7. 登录、注册、重置密码、退出登录。
8. 启动时恢复会话。

### 9.2 实现方式

主要文件：`src/store/user.ts`

本地存储 key：

| key | 作用 |
| --- | --- |
| `access_token` | 保存 JWT 或 access token。 |
| `user_id` | 保存当前用户 ID。 |
| `session_authenticated` | 保存当前浏览器会话认证标记。 |
| `user_city` | 保存当前城市。 |

核心 computed：

| computed | 作用 |
| --- | --- |
| `isAuthenticated` | 有 token 或 session flag 即认为已登录。 |
| `currentUserId` | 当前用户 ID，优先从 `userInfo` 取，其次从 localStorage 取。 |
| `displayName` | 显示名称，按 name、phone、email、默认昵称兜底。 |
| `displayAvatar` | 显示头像，没有头像时使用 dicebear 根据名称生成。 |
| `needsHeroSelection` | 已登录、资料已加载、mainHeroes 为空时要求选择本命英雄。 |
| `user` | 兼容旧结构的用户展示对象。 |

### 9.3 数据兼容处理

`userStore` 对后端返回结构做了较多兼容：

1. 登录响应可能直接是 token 字符串。
2. token 可能在 `token`、`accessToken`、`access_token`、`jwt` 等字段。
3. 用户资料可能在 `data`、`data.data`、`userInfo`、`user`、`profile`。
4. userId 可能叫 `userId`、`id`、`uid`、`user_id`。
5. 本命英雄可能是字符串数组、逗号分隔字符串或对象数组。

### 9.4 当前效果

后端返回结构即使不完全稳定，前端也尽量能解析出 token、用户信息和本命英雄，减少接口联调阶段的阻塞。

### 9.5 当前不足

1. 宽松兼容有利于联调，但长期会掩盖接口协议不稳定的问题。
2. `isAuthenticated` 只依赖本地状态，不验证 token 是否真实有效。
3. 多标签页登录/退出没有同步监听 `storage` 事件。
4. 没有 refresh token 和自动续期。
5. 没有统一用户权限模型。

### 9.6 推荐实现方法

1. 在后端接口稳定后，收敛兼容逻辑，定义明确 `Result<T>` 协议。
2. `bootstrapSession()` 时增加 `/auth/me` 或 `/user/session` 校验。
3. 监听 `window.addEventListener('storage', ...)`，实现多标签页同步退出。
4. 增加 `roles`、`permissions` 字段，支持后续权限控制。

## 10. API 请求封装详情

### 10.1 功能描述

`src/utils/request.ts` 统一封装 axios：

1. 默认 `baseURL` 为 `/api`。
2. 请求超时 20 秒。
3. 默认 JSON 请求头。
4. 请求拦截器自动读取 `localStorage.access_token` 并写入 `Authorization: Bearer <token>`。
5. 响应拦截器直接返回 `response.data`。
6. 对 401、403、404、500 做 console 错误输出。

### 10.2 Vite 代理关系

`vite.config.ts` 配置 `/api` 代理：

1. 前端请求 `/api/auth/code`。
2. Vite 转发到 `VITE_API_BASE_URL/auth/code`。
3. 代理 rewrite 会移除 `/api` 前缀。
4. 代理请求增加 `ngrok-skip-browser-warning: true`，用于绕过 ngrok 浏览器提示页。

### 10.3 当前效果

开发环境下，前端可以统一请求 `/api/...`，避免在业务代码中硬编码后端域名。token 会自动注入，业务 API 文件只需要关注接口路径和参数。

### 10.4 当前不足

1. 401 直接 `window.location.href = '/login'`，但项目没有 `/login` 页面。
2. 响应拦截器只返回 `response.data`，但某些 API 代码又在做多层 data 解包，说明后端协议尚未收敛。
3. `utils/request.ts` 底部还有示例 `api` 对象，包含 `match`、`hero` 等旧示例方法，当前看不是主要业务路径，容易造成混淆。
4. `analyze` 和 `recommend` 使用 `any` 类型。

### 10.5 推荐实现方法

1. 删除或迁移 `utils/request.ts` 里的示例 `api` 对象，业务 API 全部放到 `src/api/*.ts`。
2. 增加统一错误类型：

```ts
export interface ApiErrorPayload {
  status?: number
  code?: string
  message: string
  data?: unknown
}
```

3. 401 时不要跳不存在页面，而是：

```ts
window.dispatchEvent(new CustomEvent('auth:unauthorized'))
```

4. `MainLayout.vue` 监听该事件，调用 `userStore.logout()` 并打开登录弹窗。
5. 后端统一返回：

```ts
interface Result<T> {
  code: string
  message: string
  data: T
}
```

## 11. AI 全能咨询功能详情

### 11.1 功能描述

AI 全能咨询是一个基础聊天面板，登录用户可以连接后端 Agent 并发送问题。

主要能力：

1. 页面打开后恢复用户会话。
2. 校验当前用户 ID。
3. 请求后端可用 Agent 列表。
4. 默认选择第一个 Agent。
5. 展示连接状态。
6. 支持输入消息。
7. Enter 发送，Shift + Enter 换行。
8. 发送时追加用户消息和“正在思考...”占位消息。
9. 后端返回后替换占位消息。
10. 调用失败时显示错误提示。

### 11.2 实现方式

主要文件：

1. `src/views/AIConsultView.vue`
2. `src/api/agent.ts`
3. `src/store/user.ts`

核心流程：

1. `onMounted` 调用 `initAgents()`。
2. `initAgents()` 先执行 `userStore.bootstrapSession()`。
3. 如果没有 userId，提示用户先登录。
4. 调用 `agentApi.fetchAvailableAgents(userId)` 获取 Agent 列表。
5. 将第一个 Agent 设为 `selectedAgentId`。
6. 用户发送消息时调用 `agentApi.executeAgentUse({ agentId, userId, content })`。
7. `agentApi` 会从 `content`、`reply`、`response`、`answer`、`message` 等字段中提取文本。

### 11.3 后端接口依赖

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/agent/list` | 根据 userId 获取可用 Agent。 |
| `POST` | `/agent/use` | 使用指定 Agent 处理用户输入。 |

### 11.4 当前效果

用户进入 AI 咨询页后，页面会显示战术指挥台风格的聊天界面。连接成功后欢迎语会显示当前 Agent 名称。用户发送问题后会看到“正在思考...”，后端返回后替换成实际回复。

### 11.5 当前不足

1. 不支持流式输出，回复只能一次性展示。
2. 不支持 Markdown 渲染、代码块、高亮或富文本。
3. 不保存聊天历史，刷新页面后消息丢失。
4. 不支持切换 Agent 后清空上下文或建立独立会话。
5. 没有会话 ID，后端如果要保留上下文只能自己推断。
6. 空回复只做兜底提示，没有进一步重试策略。
7. 未登录时只提示，不会自动打开登录弹窗。

### 11.6 推荐实现方法

#### 流式输出

后端新增 SSE 接口：

```ts
POST /agent/use/stream
```

前端实现：

1. 新增 `agentApi.streamAgentUse()`。
2. 使用 `fetch` 读取 `ReadableStream`。
3. 每接收一段 token，就更新当前 AI 消息内容。
4. 增加“停止生成”按钮，使用 `AbortController` 中断。

预期效果：用户能看到 AI 逐字输出，体验更接近真实聊天产品。

#### 聊天历史

推荐数据结构：

```ts
interface ChatSession {
  id: string
  userId: string
  agentId: string
  title: string
  messages: ChatMessage[]
  createdAt: string
  updatedAt: string
}
```

实现方案：

1. 短期：用 localStorage 保存最近 20 条消息。
2. 中期：新增后端 `/chat/session`、`/chat/message` 接口。
3. 长期：支持多会话列表、搜索、删除、重命名。

#### Markdown 渲染

1. 安装 `markdown-it` 或 `marked`。
2. 使用 `dompurify` 做 XSS 清洗。
3. 对代码块接入 `highlight.js` 或 `shiki`。

预期效果：AI 可以返回结构化攻略、表格、代码、路线清单，阅读体验更好。

## 12. Agent API 兼容层详情

### 12.1 功能描述

`src/api/agent.ts` 负责对后端 Agent 数据做兼容处理。

主要能力：

1. 获取可用 Agent。
2. 规范化 Agent ID、名称和描述。
3. 调用 Agent。
4. 从不同字段中提取 Agent 回复文本。
5. 开发环境输出 debug 日志。
6. 对超长数字 ID 给出 warning，建议后端用字符串返回 Long ID。

### 12.2 当前效果

即使后端返回字段名不是完全统一，如 `agentId`、`agent_id`、`id`、`appId`，前端也能尽量识别。Agent 回复如果叫 `reply`、`response`、`answer`、`message`，也可以被提取。

### 12.3 当前不足

1. 兼容字段过多，说明接口协议需要固定。
2. 只提取纯文本，不支持结构化消息、引用、工具调用结果。
3. 没有请求取消和重试。
4. 没有业务错误码处理。

### 12.4 推荐实现方法

定义稳定协议：

```ts
interface AgentVO {
  agentId: string
  name: string
  description: string
  avatar?: string
  capabilities?: string[]
}

interface AgentUseResponse {
  messageId: string
  content: string
  usage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}
```

稳定后逐步移除多字段兼容逻辑，让联调错误更早暴露。

## 13. 圣地巡游功能详情

### 13.1 功能描述

圣地巡游是当前最复杂的业务页面，核心目标是把“城市文旅推荐”和“地图路线规划”结合起来。

当前能力：

1. 加载高德地图深色 3D 地图。
2. 浏览器定位当前城市和经纬度。
3. 起点和终点输入。
4. 快速选择城市，最多选择两个，分别作为起点和终点。
5. 规划起点到终点驾车路线。
6. 高德路线服务慢或失败时降级为直线预览。
7. 根据终点城市展示推荐景点。
8. 登录后自动查找“圣地巡游/圣地巡礼/文旅”相关 Agent。
9. 调用智能体生成城市路线 JSON。
10. 智能体数据成功时优先显示智能体推荐景点。
11. 智能体失败时回退本地推荐景点。
12. 点击景点后预览景点点位，并自动填充终点和触发路线规划。

### 13.2 页面结构

主要文件：`src/views/HolyLandTourView.vue`

页面由三层组成：

1. 全屏地图层 `.map-layer`。
2. 左侧路线规划面板 `.planner-panel`。
3. 右侧推荐景点面板 `.spots-panel`。
4. 地图或智能体加载时显示 `.loading-mask`。

### 13.3 地图实现方式

依赖文件：

1. `src/utils/amapLoader.ts`
2. `src/views/HolyLandTourView.vue`

流程：

1. 读取环境变量 `VITE_AMAP_KEY`。
2. 调用 `loadAmap()` 动态注入高德 JSAPI script。
3. 加载插件：`AMap.Geolocation`、`AMap.Geocoder`、`AMap.Driving`、`AMap.Polyline`、`AMap.ToolBar`、`AMap.Scale`。
4. 创建地图实例，启用 3D、深色地图样式、缩放控件和比例尺。
5. 初始化 `Geocoder` 和 `Driving`。
6. 自动调用 `locateCurrentPosition()` 获取当前城市。

### 13.4 路线规划实现方式

路线规划有两条路径：

#### 优先路径：高德 REST 驾车路线

条件：配置了 `VITE_AMAP_ROUTE_KEY`。

流程：

1. 将起点、终点解析为经纬度。
2. 请求 `https://restapi.amap.com/v3/direction/driving`。
3. 解析返回 steps 中的 polyline。
4. 合并成路线点数组。
5. 使用 `AMap.Polyline` 绘制路线。
6. 调用 `setFitView` 自动适配起终点和路线。

#### 备用路径：高德 JSAPI Driving 插件

条件：未配置 `VITE_AMAP_ROUTE_KEY`。

流程：

1. 使用 `AMap.Driving({ map, autoFitView: true })`。
2. 调用 `driving.search(startPosition, endPosition)`。
3. 成功后由高德插件绘制路线。
4. 超时或失败后进入直线预览。

#### 降级路径：直线预览

条件：路线服务超时、失败或不可用。

流程：

1. 仍尝试解析起点、终点坐标。
2. 使用 `AMap.Polyline` 连接两个点。
3. 错误提示说明已切换为直线预览。

### 13.5 智能体路线推荐实现方式

依赖文件：

1. `src/store/holyTour.ts`
2. `src/api/agent.ts`
3. `src/views/HolyLandTourView.vue`

流程：

1. 页面根据终点城市计算 `destinationCityForSpots`。
2. `watch(destinationCityForSpots)` 触发 `fetchSmartSpotsIfNeeded(city)`。
3. 如果用户已登录且有 userId，则调用 `holyTourStore.bootstrapHolyAgent(userId)`。
4. `bootstrapHolyAgent()` 拉取 Agent 列表，并用正则 `/(圣地巡游|圣地巡礼|文旅)/i` 匹配目标 Agent。
5. 找到 Agent 后调用 `holyTourStore.fetchCitySpots(city, userId)`。
6. `fetchCitySpots()` 构造严格 JSON prompt，要求智能体只返回 JSON。
7. 返回文本进入 `parseAgentJson()`。
8. `parseAgentJson()` 支持解析纯 JSON、markdown 代码块中的 JSON、正文中内嵌的 JSON。
9. `normalizeRoutePayload()` 将对象或数组统一转换成 `HolyTourRouteData`。
10. 只接受至少 3 个景点、最多 6 个景点、每个景点必须有 name、city、lng、lat、story、summary、imageUrl 的数据。
11. 成功后写入 `spotsByCity` 缓存。
12. 页面推荐区优先使用智能体数据，否则使用本地 `RECOMMENDED_SPOTS`。

### 13.6 智能体 JSON 协议

当前前端 prompt 要求智能体返回：

```json
{
  "routeName": "路线名称",
  "heroThemeColor": "#00ffee",
  "city": "城市名",
  "spots": [
    {
      "name": "景点名",
      "city": "城市名",
      "lng": 108.97,
      "lat": 34.22,
      "story": "景点故事",
      "summary": "推荐摘要",
      "imageUrl": "https://...",
      "query": "可选的更完整检索词"
    }
  ]
}
```

### 13.7 本地 fallback 景点

当前本地内置城市：

1. 北京
2. 上海
3. 广州
4. 深圳
5. 杭州
6. 成都
7. 重庆
8. 西安
9. 武汉
10. 南京

每个城市有 3 个推荐景点，提供 name、summary、query。

### 13.8 当前效果

用户进入圣地巡游页面后，左侧可以输入起终点或快速选择城市，右侧显示推荐景点。点击推荐景点会在地图上预览点位，并将该景点作为终点进行路线规划。登录状态下，如果后端存在匹配的“圣地巡游”智能体，推荐区会显示智能体生成的路线名称、主题色和景点；否则显示本地推荐。

### 13.9 当前不足

1. 智能体路线只做单城市景点推荐，没有真正生成多日行程、时间安排或交通建议。
2. 路线规划是起点到单一终点，不是多个景点之间的串联路线。
3. 本地 fallback 景点没有经纬度和图片，点击时需要地理编码解析，速度和准确性不如直接坐标。
4. 没有 POI 搜索建议，用户输入终点时容易因为地址不精确导致解析失败。
5. 没有保存路线、分享路线、收藏景点功能。
6. 没有地图路线详情面板，如距离、耗时、路线步骤。
7. 没有步行、骑行、公交路线模式。
8. 没有根据用户本命英雄明显改变推荐结果，虽然 prompt 已传入本命英雄。
9. 智能体返回强依赖 JSON 格式，异常输出只能回退本地数据。
10. 高德 Key 暴露在前端环境变量中，浏览器端 Key 需要做好域名白名单限制。

### 13.10 推荐实现方法

#### 多景点串联路线

目标：点击“生成巡游路线”后，将推荐景点按顺序连成完整路线。

实现步骤：

1. 将 `selectedDestinationSpot` 扩展为 `selectedRouteSpots: RecommendedSpot[]`。
2. 给右侧景点增加多选状态。
3. 每个景点解析成经纬度。
4. 依次调用路线规划服务：起点 -> 景点 A -> 景点 B -> 景点 C。
5. 将每段路线 polyline 合并绘制，或者分别绘制不同颜色。
6. 左侧状态框展示总距离、总耗时、途经点数量。

预期效果：圣地巡游不再只是“到某个景点”，而是真正形成文旅路线。

#### 行程详情生成

新增按钮：“生成一日巡游计划”。

前端 prompt 增加：

```text
请按上午、午餐、下午、傍晚、夜间输出一日路线，包含每站预计停留时长、推荐理由、注意事项。
```

推荐后端返回：

```ts
interface TourPlan {
  title: string
  city: string
  days: Array<{
    day: number
    sections: Array<{
      timeRange: string
      title: string
      description: string
      spotName?: string
      tips?: string[]
    }>
  }>
}
```

预期效果：用户不只看到地图点位，还能拿到可执行的旅游计划。

#### POI 搜索自动补全

使用高德 `AMap.AutoComplete`：

1. 加载插件 `AMap.AutoComplete`。
2. 起点和终点输入框输入时触发搜索。
3. 展示候选 POI 名称、地址、城市。
4. 用户选择后直接拿到经纬度，不再依赖纯文本 geocode。

预期效果：减少路线规划失败率，输入体验更接近地图 App。

#### 路线保存与分享

新增数据结构：

```ts
interface SavedTourRoute {
  id: string
  userId: string
  city: string
  routeName: string
  spots: RecommendedSpot[]
  start: string
  end: string
  createdAt: string
}
```

接口：

```ts
POST /tour-route/save
GET /tour-route/list?userId=xxx
DELETE /tour-route/{id}
```

前端新增：

1. “保存路线”按钮。
2. “我的路线”抽屉。
3. “复制分享链接”按钮。

## 14. 地图加载工具详情

### 14.1 功能描述

`src/utils/amapLoader.ts` 负责动态加载高德地图 JSAPI。

### 14.2 实现方式

1. 使用模块级变量 `amapLoadPromise` 缓存加载过程。
2. 如果 `window.AMap` 已存在，直接返回。
3. 如果正在加载，复用同一个 Promise。
4. 根据 key、version、plugins 拼接 script URL。
5. script 加载成功后检查 `window.AMap` 是否挂载。
6. 加载失败时清空缓存，允许下次重试。

### 14.3 当前效果

避免多个组件或重复进入页面时重复注入高德 script，提高稳定性。

### 14.4 当前不足

1. `window.AMap` 使用 `any` 类型。
2. 没有处理不同插件列表的二次加载问题。如果第一次加载插件较少，第二次请求更多插件时可能无法补齐。

### 14.5 推荐实现方法

1. 建立更完整的 AMap 类型声明。
2. 固定项目所需插件集合，统一一次加载。
3. 如果后续页面需要更多插件，集中维护 `AMAP_PLUGINS` 常量。

## 15. 环境变量和代理功能

### 15.1 当前环境变量

`.env.example` 中列出：

| 变量 | 作用 |
| --- | --- |
| `VITE_API_BASE_URL` | 后端 API 服务器地址。 |
| `VITE_PORT` | 前端开发服务器端口。 |
| `VITE_PROXY_ENABLED` | 是否启用代理。 |
| `VITE_WS_URL` | WebSocket 地址，当前未使用。 |
| `VITE_AUTH_TOKEN_KEY` | token key，当前未真正接入。 |
| `VITE_REFRESH_TOKEN_KEY` | refresh token key，当前未真正接入。 |
| `VITE_AMAP_KEY` | 高德 JSAPI Key。 |
| `VITE_AMAP_ROUTE_KEY` | 高德 WebService 路线 Key。 |

### 15.2 当前效果

开发时可以通过 Vite 代理把 `/api` 请求转发到后端。圣地巡游页面可以读取高德 Key 加载地图和路线服务。

### 15.3 风险

当前 `.env.development` 包含真实 ngrok 地址和高德 Key。如果未来引入 Git，需要确认 `.env.development` 被忽略，不要提交。即使前端 Key 本身会暴露给浏览器，也应该在高德控制台配置域名白名单，避免被滥用。

## 16. 当前未实现功能汇总

| 功能 | 当前状态 | 缺失内容 | 推荐优先级 |
| --- | --- | --- | --- |
| 赛事追踪 | 首页有入口，无页面 | 路由、页面、接口、数据展示都未实现 | 中 |
| 帮选英雄 | 首页有入口，无页面 | 路由、问卷/推荐逻辑、结果页未实现 | 高 |
| 对局分析 | 导航有入口文案，无页面 | 对局数据输入、分析接口、报告页面未实现 | 中 |
| 社区 | 导航有入口文案，无页面 | 帖子、评论、点赞、用户内容体系未实现 | 低 |
| 独立登录页 | 401 会跳 `/login`，但无页面 | 路由和页面未实现，当前是弹窗模式 | 高 |
| 每日 AI 卡片真实数据 | 当前 mock | 后端接口、AI 生成、节气计算未实现 | 中 |
| 聊天流式输出 | 当前非流式 | SSE/WebSocket/fetch stream 未实现 | 中 |
| 聊天历史 | 当前刷新丢失 | 会话存储、历史列表、删除/重命名未实现 | 中 |
| 多 Agent 会话 | 当前默认第一个 Agent | Agent 切换、上下文隔离、会话 ID 未实现 | 中 |
| 圣地巡游多点路线 | 当前单起点到单终点 | 多景点串联、总距离耗时、途经点顺序未实现 | 高 |
| 圣地巡游行程计划 | 当前只推荐景点 | 上午/下午/夜间计划、停留时长、注意事项未实现 | 高 |
| 路线保存/分享 | 当前无持久化 | 收藏路线、我的路线、分享链接未实现 | 中 |
| POI 自动补全 | 当前文本输入 | 高德 AutoComplete 未接入 | 中 |
| 用户头像上传 | 当前只支持 URL | 上传接口、文件选择、裁剪未实现 | 中 |
| 手机/邮箱换绑 | 当前只展示 | 验证旧账号、新账号验证码、换绑接口未实现 | 低 |
| token 刷新 | 当前无 refresh | refresh token、401 重试、请求重放未实现 | 高 |
| 多标签同步登录态 | 当前无监听 | storage 事件、跨标签退出同步未实现 | 低 |
| 权限模型 | 当前无 | role、permission、路由守卫未实现 | 低 |

## 17. 未实现功能详细方案

### 17.1 帮选英雄

#### 目标效果

用户通过答题或输入偏好，系统推荐适合的英雄，并解释推荐理由。推荐结果可以保存为本命英雄。

#### 推荐页面

新增文件：`src/views/HeroPickView.vue`

页面结构：

1. 顶部标题：帮选英雄。
2. 偏好问卷：位置、操作难度、打法风格、常玩模式、喜欢远程/近战、团队定位。
3. AI 推荐按钮。
4. 推荐结果卡片：英雄名称、职业、推荐理由、优缺点、上手建议。
5. 一键加入本命英雄。

#### 推荐接口

```ts
POST /hero/recommend
```

请求：

```ts
interface HeroRecommendRequest {
  userId: string
  preferredRoles: string[]
  difficulty: 'low' | 'medium' | 'high'
  playStyle: string[]
  mainHeroes: string[]
  extraText?: string
}
```

响应：

```ts
interface HeroRecommendResult {
  heroId: string
  name: string
  profession: string
  score: number
  reasons: string[]
  weaknesses: string[]
  beginnerTips: string[]
}
```

#### 前端实现步骤

1. 注册 `/hero-pick` 路由。
2. 新建 `src/api/heroRecommend.ts` 或复用 `src/api/hero.ts`。
3. 页面打开时读取 `userStore.userInfo.mainHeroes` 作为上下文。
4. 表单提交后调用推荐接口。
5. 展示推荐结果。
6. 支持点击“设为本命英雄”，调用 `heroApi.saveMyHeroes()`。

#### 预期效果

用户可以从“我喜欢什么打法”出发获得英雄推荐，而不是只能手动搜索英雄列表。

### 17.2 赛事追踪

#### 目标效果

用户可以查看赛事列表、比赛详情、战队战绩和赛程提醒。

#### 推荐页面

新增文件：`src/views/MatchesView.vue`

页面结构：

1. 赛事筛选：赛事名称、日期、战队。
2. 今日比赛列表。
3. 近期赛程时间线。
4. 比赛详情弹窗：队伍、比分、地图/局数、MVP、关键数据。
5. 收藏/提醒按钮。

#### 推荐接口

```ts
GET /matches?page=1&size=20&keyword=&date=
GET /matches/{id}
GET /teams/{id}/matches
POST /matches/{id}/subscribe
```

#### 前端实现步骤

1. 注册 `/matches` 路由。
2. 新建 `src/api/match.ts`。
3. 新建 `src/views/MatchesView.vue`。
4. 使用分页列表和日期筛选。
5. 比赛状态分为未开始、进行中、已结束。
6. 收藏提醒需要登录，未登录则打开登录弹窗。

#### 预期效果

首页“赛事追踪”入口变成可用功能，用户能查询比赛赛程和结果。

### 17.3 对局分析

#### 目标效果

用户输入或上传对局数据，系统生成对局复盘报告，包括优势点、失误点、英雄选择、节奏建议。

#### 推荐页面

新增文件：`src/views/MatchAnalysisView.vue`

页面结构：

1. 对局 ID 输入。
2. 手动录入关键数据：英雄、位置、KDA、经济、参团率、伤害、承伤等。
3. 上传截图或 JSON。
4. 分析按钮。
5. 分析报告：评分、关键问题、下一步训练建议。

#### 推荐接口

```ts
POST /matches/analyze
```

请求：

```ts
interface MatchAnalyzeRequest {
  userId: string
  matchId?: string
  heroName?: string
  role?: string
  kda?: string
  gold?: number
  damage?: number
  takenDamage?: number
  participation?: number
  rawText?: string
}
```

响应：

```ts
interface MatchAnalyzeReport {
  score: number
  summary: string
  strengths: string[]
  mistakes: string[]
  trainingPlan: string[]
  recommendedHeroes?: string[]
}
```

#### 前端实现步骤

1. 注册 `/match-analysis` 路由。
2. 导航“对局分析”指向该路径。
3. 新建 API 封装。
4. 做表单校验和 loading 状态。
5. 报告使用分区卡片展示。

#### 预期效果

用户能获得个人化对局复盘，形成和 AI 咨询不同的结构化分析功能。

### 17.4 社区

#### 目标效果

用户可以发布路线、攻略、英雄心得和赛事讨论。

#### 推荐页面

新增文件：`src/views/CommunityView.vue`

基础功能：

1. 帖子列表。
2. 发帖。
3. 帖子详情。
4. 评论。
5. 点赞。
6. 收藏。

#### 推荐接口

```ts
GET /posts
POST /posts
GET /posts/{id}
POST /posts/{id}/comments
POST /posts/{id}/like
POST /posts/{id}/favorite
```

#### 实现建议

社区功能会引入内容审核、分页、用户关系和通知系统，复杂度较高。建议在核心工具功能稳定后再实现。

### 17.5 独立登录页或弹窗式 401 处理

当前请求 401 会跳 `/login`，但项目没有 `/login` 页面。推荐二选一。

#### 方案 A：继续弹窗式登录

1. 删除 `/login` 跳转。
2. axios 401 时派发事件：`auth:unauthorized`。
3. `MainLayout.vue` 监听事件，调用 `userStore.logout()` 并打开 `LoginModal`。
4. 登录成功后停留当前页面。

优点：符合当前产品交互，不打断用户上下文。

#### 方案 B：新增独立登录页

1. 新建 `src/views/LoginView.vue`。
2. 注册 `/login` 路由。
3. 抽离 `LoginModal` 内部表单为 `AuthForm.vue`，弹窗和页面复用同一表单。
4. 401 跳转 `/login?redirect=<currentPath>`。
5. 登录成功后跳回 redirect。

优点：更传统，适合后续做 OAuth、扫码登录、单独注册页。

当前项目已有弹窗交互，建议优先采用方案 A。

### 17.6 每日 AI 卡片

#### 目标效果

每日卡片根据日期、节气、用户本命英雄、近期行为生成个性化推荐。

#### 实现方案

1. 新增 `src/api/daily.ts`。
2. 新增 `GET /daily-card/today`。
3. 后端可用定时任务或实时 Agent 生成今日内容。
4. 前端失败时用本地 fallback。
5. 用户关闭卡片后，记录到 localStorage，只在当天隐藏。

本地隐藏 key：

```ts
const DAILY_CARD_CLOSED_KEY = `daily_card_closed_${yyyyMMdd}`
```

预期效果：用户每天进入首页看到不同内容，关闭后当天不再打扰。

## 18. 推荐开发优先级

### 第一优先级：修复当前明显断点

1. 注册或处理 `/matches`、`/hero-pick` 缺失路由。
2. 修复 401 跳转 `/login` 的问题。
3. 修复首页折叠布局选择器。
4. 清理或确认 `.env.development` 不会提交。

### 第二优先级：补齐产品闭环

1. 帮选英雄页面。
2. 圣地巡游多景点串联路线。
3. 个人资料城市后端持久化。
4. AI 咨询聊天历史。

### 第三优先级：增强体验

1. AI 流式输出。
2. POI 自动补全。
3. 每日 AI 卡片真实数据。
4. 路线保存和分享。

### 第四优先级：长期扩展

1. 赛事追踪。
2. 对局分析。
3. 社区。
4. 权限系统和通知系统。

## 19. 建议的目录演进

当前 `src` 可以逐步演进为：

```text
src/
├── api/
│   ├── agent.ts
│   ├── auth.ts
│   ├── daily.ts
│   ├── hero.ts
│   ├── match.ts
│   ├── tour.ts
│   └── user.ts
├── components/
│   ├── auth/
│   │   └── AuthForm.vue
│   ├── hero/
│   │   └── MainHeroSelector.vue
│   ├── map/
│   │   └── AmapContainer.vue
│   └── profile/
│       └── UserProfilePanel.vue
├── layout/
│   └── MainLayout.vue
├── router/
│   └── index.ts
├── store/
│   ├── daily.ts
│   ├── holyTour.ts
│   └── user.ts
├── styles/
│   └── variables.css
├── utils/
│   ├── amapLoader.ts
│   ├── geolocation.ts
│   └── request.ts
└── views/
    ├── AIConsultView.vue
    ├── CommunityView.vue
    ├── HeroPickView.vue
    ├── HolyLandTourView.vue
    ├── HomeView.vue
    ├── MatchAnalysisView.vue
    └── MatchesView.vue
```

## 20. 总结

当前项目不是空壳，已经具备用户系统、Agent 聊天和地图文旅路线规划这三类核心能力。最强的现有功能是圣地巡游，已经完成了地图加载、定位、地理编码、路线规划、智能体 JSON 推荐和本地 fallback。用户体系也比较完整，已经覆盖登录、注册、重置密码、本命英雄和个人资料。

当前主要问题是产品入口比实际页面多，导致首页和导航里有几个功能看起来可点击但没有落地。短期最应该做的是修复路由断点和 401 登录处理；中期应该补齐“帮选英雄”和“圣地巡游多点路线”，因为它们最贴合现有代码和产品方向；长期再扩展赛事、对局分析和社区。
