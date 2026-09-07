# Original User Request

## 2026-09-07T05:18:33Z

对“峡谷卫星 (Satellite)”全栈项目开展全方位深度审查，并在审查结束后基于《Web Interface Guidelines》与《Frontend Design》设计规范，对前端（Vue 3 + TypeScript + Vite）进行整体视觉设计、配色、排版字体、按键触控与响应式布局的专项重构优化。

Working directory: C:\Users\29602\Desktop\sate
Integrity mode: development

参考设计规范来源：
1. [Vercel Web Interface Guidelines](https://github.com/vercel-labs/web-interface-guidelines)（触控目标 ≥ 44px、移动端输入框 ≥ 16px、`:focus-visible` 焦点环、表单与加载状态无障碍、无死区、防止意外缩放）
2. [Anthropic Frontend Design](https://github.com/anthropics/claude-code/tree/main/plugins/frontend-design)（定制化品牌视觉语言、拒绝 AI 生成式俗套渐变/卡片模板、严谨字体层级、克制动效与明确信息架构）

## Requirements

### R1. 全栈架构、安全与代码质量全面审查
深入审查 `satellite-end`（Spring Boot 3.2.4）与 `satellite-front`（Vue 3 + TypeScript）全链路代码，涵盖：
1. **安全与合规审计**：排查敏感密钥硬编码（Redis、MariaDB、高德地图、邮件服务等）、明文回退逻辑、JWT 鉴权拦截漏洞、未授权接口及越权访问隐患；
2. **后端代码质量**：排查 Spring Boot 控制器/服务层异常处理、事务一致性、MyBatis SQL 注入风险、不安全随机数与工具函数重复定义；
3. **前端架构与可用性**：排查 Vue 3 组件生命周期泄漏、Pinia 状态持久化、死链与未定义路由、冗余文件与未引用废弃模块；
4. **输出物**：生成完整的结构化项目审查报告（Markdown 形式存入项目根目录）。

### R2. 视觉设计系统与配色重构 (Design Token)
结合产品“峡谷卫星/游戏辅助与圣地巡游”属性与《Frontend Design》设计原则：
1. **重构色彩体系**：摆脱千篇一律的通用模板感，确立具有电竞/科技轻未来感的高级调色板（定义语义化 Primary、Surface、Accent、Border、Muted 色阶及暗色/亮色协调对比度）；
2. **字体与排版阶梯**：建立清晰的字号、字重与行高层级（H1/H2/H3/Body/Caption），优化中英文字符渲染与段落最大宽度（line-length < 80 字符），严禁无序滥用全大写或随意渐变文字；
3. **空间与层次系统**：规范 4px/8px 网格间距尺度、统一容器圆角、玻璃态/微投影与边界分割线层次。

### R3. 前端界面、布局与按键交互全面优化
针对核心页面与组件进行精细化打磨（遵循《Web Interface Guidelines》）：
1. **导航与整体框架 (`MainLayout.vue`)**：优化桌面端与移动端响应式切换、导航高亮状态、折叠菜单动画与安全区适配（`env(safe-area-inset-*)`）；
2. **首页视界 (`HomeView.vue`)**：重构 Hero 区域视觉张力、每日 AI 卡片 (`DailyAICard.vue`)、英雄选择与快捷入口，改善图文对比度与卡片悬停反馈；
3. **圣地巡游与地图交互 (`HolyLandTourView.vue`)**：优化高德地图容器布局、点位列表浮层、路径指引与筛选控制栏的排版组织；
4. **AI 咨询对局助手 (`AIConsultView.vue`)**：优化聊天气泡流式布局、输入框交互（自动聚焦、回车发送、Ctrl+Enter 换行、禁用防抖与加载中骨架）；
5. **模态窗与表单体验 (`LoginModal.vue`, `UserProfilePanel.vue`)**：完善触控目标尺寸（所有交互元素最小点击区 ≥ 44×44px）、表单回车提交、加载中保持原始文案与转圈指示、清晰内联错误提示、关闭按钮可访问性。

### R4. 构建验证与工程健康度
1. 确保前端 TypeScript 类型检查 (`vue-tsc -b`) 与 Vite 编译构建 (`npm run build`) 100% 成功通过，零编译错误；
2. 保持对后端已定义 RESTful API 契约的向后兼容，不破坏现有功能链路。

## Acceptance Criteria

### 审计验收标准
- [ ] 产出系统全维度审计分析报告文档（详细列出漏洞风险等级、成因分析与改进建议）
- [ ] 标记并修复前端已暴露的逻辑死链、重复工具函数与模板残留

### 前端 UI/UX 验收标准
- [ ] 所有交互按钮、输入框、链接的点击热区均满足 ≥ 44×44px（移动端友好）
- [ ] 移动端输入框 `font-size ≥ 16px`，防止 iOS Safari 自动缩放
- [ ] 键盘交互支持：所有可交互元素具备清晰可见的 `:focus-visible` 焦点环，按 Escape 支持关闭弹窗
- [ ] 按钮在提交加载时保持文案并展示 Spinner，防止二次重复点击
- [ ] 核心页面（MainLayout、HomeView、HolyLandTourView、AIConsultView、LoginModal）完成配色与排版重构，视觉层级鲜明、无遮挡破损
- [ ] 运行 `npm run build` 成功完成打包，TypeScript 零类型错误

## 2026-09-07T14:59:28Z

对“灵境导览 (基于LLM与LBS的个性化文旅导览智能体)”开展模块化、分阶段的深度重构与工程落地。全流程严格遵循全新配置的专业规范库（《design-systems-frontend-architecture》、《forms-inputs-checkout》、《interaction-patterns-components》、《ui-visual-composition》、《ux-writing-content-design》、《web-interface-guidelines》与《accessibility-inclusive-design》），确保输出高工程标准、高人机工学可用性与当代高品质美学。

Working directory: C:\Users\29602\Desktop\sate
Integrity mode: development

---

## Modular Requirements (分模块实施要求)

### Block 1: 数据模型与后端 Agent 决策核心 (Data Domain & Backend Agent Engine)
1. **文旅领域模型与种子数据**：
   - 部署 `schema_travel.sql`，建立文旅景点表 (`t_poi`)、商户表 (`t_merchant`)、特色菜品表 (`t_dish`)、用户画像表 (`t_user_profile`)、路线方案表 (`t_route_plan`) 与节点表 (`t_route_item`)；
   - 导入衡阳师院周边及衡阳核心地标（石鼓书院、东洲岛、南岳衡山、南湖公园等）景点与特色美食/菜品种子数据；
2. **4 阶段 Pipeline Agent 工作流服务 (`AgentWorkflowService.java`)**：
   - **意图与画像解析**：提取出行时长、氛围标签、交通方式，结合用户辣度、忌口与预算；
   - **双路候选召回**：景点初筛 + 餐饮初筛（优先召回管理录入特色店，结合高德周边美食实时数据）；
   - **高德 LBS 时空真实路径校验**：调用高德驾车/步行 API 进行距离与耗时校验，剔除空间幻觉与不可行节点；
   - **个性化可解释合成**：为每个节点输出推荐理由（“为什么推荐这里”）及推荐必点菜品、避坑/忌口提示；
   - **高可用静默降级**：大模型超时/未配置时自动切换至本地多维加权拓扑贪心算法，确保零崩溃；
3. **商户与路线 RESTful API**：提供路线规划、商户与菜品增删改查、用户偏好更新接口；
4. **清理历史游戏代码**：彻底移除后端所有王者荣耀英雄相关控制器、Mapper 与 Pojo。

### Block 2: 首页向导与用户饮食画像系统 (Guided Wizard & Preference Profile)
*遵循技能：《forms-inputs-checkout》+《ux-usability-foundations》+《ux-writing-content-design》*
1. **重构 `HomeView.vue` 为轻量向导表单卡片 (Guided Wizard Card)**：
   - **步骤 1：出行时长**（2小时小憩、半日漫游、全天深度）；
   - **步骤 2：氛围风格**（松弛感、出片打卡、人文历史、自然山水、烟火夜市）；
   - **步骤 3：出行方式**（步行、公共交通、骑行、驾车）；
   - **步骤 4：饮食微调摘要**（显示当前辣度与忌口，支持快速一键唤起微调）；
   - **主操作按键**：清晰后果导向的大号按键“✨ 智能生成专属路线”，加载时保持文案并展示 Spinner；
2. **升级 `UserProfilePanel.vue` 为个性化出行与饮食画像抽屉**：
   - 辣度等级单选、常见忌口标签多选（不吃内脏、免香菜、不吃海鲜、清真等）、单餐人均预算滑块；
3. **清理前端游戏残留**：彻底移除 `MainHeroSelector.vue` 与游戏素材。

### Block 3: 3D 地图智能导览与可解释时间轴 (3D Map & Explainable Timeline)
*遵循技能：《interaction-patterns-components》+《web-interface-guidelines》+《ui-visual-composition》*
1. **重构升级 `RouteMapView.vue`**：
   - 3D 高德地图全景沉浸，以不同语义色彩区分文旅景点（海克斯青）与美食餐饮（活力橙）；
   - 基于高德路径规划真实绘制两点间道路轨迹折线；
2. **左侧/底部悬浮式路线时间轴卡片**：
   - 标注：到达时间、建议停留时长、交通段耗时；
   - 呈现**可解释推荐理由**（“距上个景点仅1.2km，江风与晚霞绝佳”）；
   - 餐饮节点醒目呈现：**推荐必点特色菜品、单价、辣度说明与食材避坑提示**；
   - 提供【重新规划】与【一键导航】操作。

### Block 4: 管理工作台与端到端协同联动 (Admin Console & Live Integration)
*遵循技能：《information-architecture-navigation》+《design-systems-frontend-architecture》*
1. **全新构建管理工作台 (`AdminView.vue`)**：
   - 专为大创答辩演示设计，支持可视化录入新店铺（店名、坐标、人均、风味）与招牌菜品（价格、辣度、避坑食材）；
2. **顶栏导航架构升级 (`MainLayout.vue`)**：
   - 品牌名正式更名为：“**灵境导览 · 衡阳文旅智能体**”；
   - 清晰设置 4 大导航节点：🏠 行程定制 (`/`)、🗺️ 3D 智能导览 (`/route-map`)、💬 AI 伴游助手 (`/ai-consult`)、⚙️ 管理工作台 (`/admin`)；
3. **端到端联动验证**：管理端录入新店后，前台用户重新生成路线能够即时精准捕获该店并推荐进餐饮节点。

### Block 5: 全局工程验证、类型安全与多维度质量门禁 (Global Verification & Quality Gate)
*遵循技能：《webapp-testing》+《accessibility-inclusive-design》*
1. **构建与类型验证**：前端执行 `npm run build` (`vue-tsc -b && vite build`) 100% 成功，零编译与类型报错；
2. **人机工程与无障碍核查**：全站触控目标 ≥ 44 × 44px、移动端输入框字体 ≥ 16px、全局 `:focus-visible` 焦点环、支持 Escape 关闭弹窗。

---

## Acceptance Criteria (分阶段验收标准)

### Block 1 后端与领域验收
- [ ] 数据库表结构与衡阳文旅/美食种子数据就绪，无报错；
- [ ] 后端 Agent 工作流具备意图分析、双路召回、高德测距校验与本地规则静默降级；
- [ ] 清理后端原有 `Hero` 相关的冗余代码。

### Block 2 & 3 前端界面与交互验收
- [ ] 首页完成向导表单卡片重构，视线流清晰，交互友好；
- [ ] 个人中心完成饮食偏好（辣度/忌口/预算）配置抽屉；
- [ ] 3D 地图页完成路线轨迹渲染与时间轴卡片展开，清晰展示推荐菜品与避坑理由；
- [ ] 彻底移除历史游戏/英雄选择组件。

### Block 4 管理端与联动验收
- [ ] 顶栏导航清晰集成【管理工作台】入口；
- [ ] 管理工作台支持新增特色店铺与菜品，且能成功联动到前台智能推荐中。

### Block 5 质量与工程验收
- [ ] `npm run build` 成功完成打包，TypeScript 零错误；
- [ ] 满足 Web Interface Guidelines 触控（≥ 44px）与无障碍规范。

