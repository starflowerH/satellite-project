# 峡谷卫星 - 项目结构文档

> 本文档详细描述《峡谷卫星》项目的目录结构、页面分区功能对应的文件位置，以及图片资源的引用位置。

---

## 📂 项目目录结构

```
sate/
├── public/                     # 静态公共资源
└── src/
    ├── assets/                 # 本地静态资源
    │   ├── hero.png            # 英雄素材占位图
    │   ├── vue.svg             # Vue Logo
    │   └── vite.svg            # Vite Logo
    │
    ├── components/             # 公共组件
    │   ├── HelloWorld.vue      # 示例组件（未使用）
    │   ├── LoginModal.vue       # 🔐 登录弹窗组件
    │   └── DailyAICard.vue      # 🎴 每日 AI 卡片组件
    │
    ├── layout/                 # 布局组件
    │   └── MainLayout.vue      # 🏠 主布局框架
    │
    ├── router/                # 路由配置
    │   └── index.ts            # 路由定义
    │
    ├── store/                 # 状态管理
    │   └── user.ts             # 👤 用户状态管理
    │
    ├── styles/                # 全局样式
    │   └── variables.css       # 🎨 CSS 变量定义
    │
    ├── views/                # 页面视图
    │   ├── HomeView.vue       # 🏠 首页
    │   └── HolyLandTourView.vue  # 🗺️ 圣地巡游页
    │
    ├── App.vue               # 根组件
    ├── main.ts              # 应用入口
    ├── index.ts             # 模块导出
    └── style.css            # 全局样式
```

---

## 🗂️ 页面分区与文件对应关系

### 1. 主布局框架 (`MainLayout.vue`)

**文件路径**: `src/layout/MainLayout.vue`

| 页面分区 | 功能描述 | 对应代码区域 |
|---------|---------|-------------|
| 视频背景层 | 全屏沉浸式视频背景 | `<video class="video-bg">` |
| 渐变遮罩层 | 提升文字可读性的暗色渐变 | `<div class="gradient-overlay">` |
| 导航栏 (Navbar) | Logo、导航链接、用户区域 | `<nav class="navbar">` |
| 用户区域 | 登录按钮 / 用户头像+名称+退出 | `.user-area` |
| 内容插槽 | 渲染子路由页面 | `<router-view />` |
| 登录弹窗 | 登录 Modal 容器 | `<LoginModal />` |

---

### 2. 首页 (`HomeView.vue`)

**文件路径**: `src/views/HomeView.vue`

| 页面分区 | 功能描述 | 对应代码区域 |
|---------|---------|-------------|
| 每日 AI 卡片 | 独立组件，集成自 `DailyAICard.vue` | `<DailyAICard />` |
| 功能导航面板 | 四入口玻璃卡片网格 | `.features-section` |
| AI 全能咨询卡片 | 第一个功能入口 | `.feature-card` (index 0) |
| 帮选英雄卡片 | 第二个功能入口 | `.feature-card` (index 1) |
| 赛事追踪卡片 | 第三个功能入口 | `.feature-card` (index 2) |
| 圣地巡游卡片 | 第四个功能入口 | `.feature-card` (index 3) |

---

### 3. 每日 AI 卡片 (`DailyAICard.vue`)

**文件路径**: `src/components/DailyAICard.vue`

| 页面分区 | 功能描述 | 对应代码区域 |
|---------|---------|-------------|
| 外层容器 | 相对定位，高度 220px，圆角 16px | `.daily-ai-card` |
| 背景层 | 绝对定位，背景图+渐变叠加 | `.card-background` |
| 背景图片 | 风景/英雄图，Hover 时 scale(1.05) | `.bg-image` |
| 背景渐变 | linear-gradient 透明到深空黑 | `.bg-gradient` |
| 关闭按钮 | 右上角 ×，Hover 变警戒红发光 | `.close-btn` |
| 日期主区域 | 超大号日期数字（72px）| `.date-section` |
| 日期数字 | 带 text-shadow 发光效果 | `.date-day` |
| 日期元信息 | 年月 + 星期 | `.date-meta` |
| 节气标签 | 霓虹蓝发光边框 | `.solar-term-badge` |
| 英雄区域 | 头像 + 名称 + 称号 | `.hero-section` |
| 英雄头像 | 圆形头像，带光晕效果 | `.hero-avatar` |
| 底部语录 | 带 text-shadow 全息投影质感 | `.quote-section` |
| AI 标签 | 左上角红色标签 | `.ai-badge` |
| 入场动画 | fade-slide 向上滑入 | `<Transition name="fade-slide">` |

---

### 4. 登录弹窗 (`LoginModal.vue`)

**文件路径**: `src/components/LoginModal.vue`

| 页面分区 | 功能描述 | 对应代码区域 |
|---------|---------|-------------|
| 模态遮罩 | 背景模糊遮罩 | `.modal-overlay` |
| 弹窗容器 | 玻璃质感卡片 | `.modal-container` |
| 关闭按钮 | 右上角 × 按钮 | `.close-btn` |
| Logo 区域 | 项目 Logo 和标题 | `.modal-header` |
| 手机号输入 | 手机号表单项 | `.input-group` (index 0) |
| 密码输入 | 密码表单项 | `.input-group` (index 1) |
| 错误提示 | 登录失败提示 | `.error-message` |
| 登录按钮 | 提交登录 | `.login-btn` |
| 提示文字 | 测试账号提示 | `.hint-text` |

---

### 5. 圣地巡游页 (`HolyLandTourView.vue`)

**文件路径**: `src/views/HolyLandTourView.vue`

| 页面分区 | 功能描述 | 对应代码区域 |
|---------|---------|-------------|
| 页面标题 | 圣地巡游大标题 | `.page-header` |
| 路线网格 | 瀑布流卡片布局 | `.routes-grid` |
| 路线卡片 | 单条旅游路线 | `.route-card` |
| 城市图片 | 打卡城市背景图 | `.route-image-wrapper` |
| 英雄标签 | 英雄头像+名称 | `.hero-tag` |
| 天数标签 | 行程天数 | `.days-badge` |
| 路线信息 | 城市、标题、描述 | `.route-content` |
| 亮点标签 | 景点亮点标签组 | `.highlights` |
| 规划按钮 | 一键规划行程按钮 | `.plan-btn` |
| 提示卡片 | 底部使用提示 | `.tips-section` |

---

### 6. 状态管理 (`user.ts`)

**文件路径**: `src/store/user.ts`

| 功能 | 描述 |
|-----|------|
| `User` 接口 | 用户信息类型定义 |
| `useUserStore` | Pinia Store 实例 |
| `user` 状态 | 用户信息 (username, avatar, isLoggedin) |
| `login()` | 登录动作 (Mock) |
| `logout()` | 登出动作 |

---

### 7. 路由配置 (`router/index.ts`)

**文件路径**: `src/router/index.ts`

| 路由路径 | 页面组件 | 描述 |
|---------|---------|------|
| `/` | `HomeView.vue` | 首页 |
| `/holy-land-tour` | `HolyLandTourView.vue` | 圣地巡游页 |

---

### 8. 样式变量 (`variables.css`)

**文件路径**: `src/styles/variables.css`

| 变量名 | 用途 |
|-------|------|
| `--bg-primary` | 主背景色 (#0A0D14) |
| `--text-primary` | 主要文字色 (#FFFFFF) |
| `--text-secondary` | 次要文字色 (#8BA1B8) |
| `--accent-cyan` | 霓虹蓝 (#00E5FF) |
| `--accent-red` | 警戒红 (#FF3366) |
| `--glass-bg` | 玻璃背景 (rgba 半透明) |
| `--glass-border` | 玻璃边框色 |
| `--blur-amount` | 模糊半径 (12px) |
| `--navbar-height` | 导航栏高度 (64px) |
| `--content-max-width` | 内容最大宽度 (1200px) |
| `--border-radius` | 圆角 (16px) |

---

## 🖼️ 图片资源位置

### 网络图片（CDN 引用）

项目中的图片主要通过网络 CDN 引用，无需本地存储：

| 用途 | 图片 URL | 所在文件 |
|-----|---------|---------|
| 视频背景海报 | `https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1920&q=80` | `MainLayout.vue` |
| 视频背景 MP4 | `https://assets.mixkit.co/videos/preview/mixkit-stars-in-space-1610-large.mp4` | `MainLayout.vue` |
| 每日卡片背景 | `https://images.unsplash.com/photo-1534796636912-3b95b3ab5986?w=1200&q=80` | `DailyAICard.vue` |
| 每日英雄头像 | `https://api.dicebear.com/7.x/personas/svg?seed=swordsman` | `DailyAICard.vue` |
| 英雄头像 (后羿) | `https://api.dicebear.com/7.x/personas/svg?seed=archer` | `HolyLandTourView.vue` |
| 英雄头像 (孙悟空) | `https://api.dicebear.com/7.x/personas/svg?seed=monkey` | `HolyLandTourView.vue` |
| 英雄头像 (王昭君) | `https://api.dicebear.com/7.x/personas/svg?seed=princess` | `HolyLandTourView.vue` |
| 英雄头像 (诸葛亮) | `https://api.dicebear.com/7.x/personas/svg?seed=scholar` | `HolyLandTourView.vue` |
| 英雄头像 (貂蝉) | `https://api.dicebear.com/7.x/personas/svg?seed=dancer` | `HolyLandTourView.vue` |
| 英雄头像 (关羽) | `https://api.dicebear.com/7.x/personas/svg?seed=warrior` | `HolyLandTourView.vue` |
| 西安城市图 | `https://images.unsplash.com/photo-1508804185872-d7badad00f7d?w=800&q=80` | `HolyLandTourView.vue` |
| 连云港城市图 | `https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=800&q=80` | `HolyLandTourView.vue` |
| 宜昌城市图 | `https://images.unsplash.com/photo-1530866495561-507c9faab2ed?w=800&q=80` | `HolyLandTourView.vue` |
| 成都城市图 | `https://images.unsplash.com/photo-1519614218869-4d4bf475cbf9?w=800&q=80` | `HolyLandTourView.vue` |
| 洛阳城市图 | `https://images.unsplash.com/photo-1544725176-7c40e5a71c5e?w=800&q=80` | `HolyLandTourView.vue` |
| 运城城市图 | `https://images.unsplash.com/photo-1568315663980-1e927a28b416?w=800&q=80` | `HolyLandTourView.vue` |

### 本地图片

| 文件名 | 路径 | 用途 |
|-------|------|------|
| `hero.png` | `src/assets/hero.png` | 英雄素材占位图 |
| `vue.svg` | `src/assets/vue.svg` | Vue Logo |
| `vite.svg` | `src/assets/vite.svg` | Vite Logo |

---

## 🔗 图片来源说明

| 来源 | 域名 | 用途 |
|-----|------|------|
| Unsplash | `images.unsplash.com` | 城市风景高清图片 |
| Mixkit | `assets.mixkit.co` | 星空视频背景 |
| DiceBear | `api.dicebear.com` | 动态生成用户/英雄头像 SVG |

---

## 📱 响应式断点

| 断点 | 屏幕宽度 | 适配内容 |
|-----|---------|---------|
| Desktop | > 1024px | 圣地巡游 3 列网格 |
| Tablet | 640px - 1024px | 圣地巡游 2 列网格 |
| Mobile | < 640px | 单列布局，功能卡片垂直堆叠 |

---

*文档更新时间: 2026-04-05 (v2 - 新增 DailyAICard 组件)*
