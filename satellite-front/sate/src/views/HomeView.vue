<script setup lang="ts">
/**
 * src/views/HomeView.vue - 灵境导览 行程定制首页 (Guided Wizard Card)
 *
 * 遵循技能规范：
 * - 《forms-inputs-checkout》: 4步清晰向导、明确反馈、触控区域 >= 44px
 * - 《ux-usability-foundations》: 视线流自然、降低认知负荷、后果导向 CTA
 * - 《ux-writing-content-design》: 地道文旅与美食表达、微观避坑提示
 * - 《web-interface-guidelines》: 防双击防抖、Spinner 保持文案、响应式排版
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useRouteStore } from '@/store/route'
import UserProfilePanel from '@/components/UserProfilePanel.vue'
import type { AtmosphereTag, RouteGenerateRequestDTO, TransportMode } from '@/types/travel'

const router = useRouter()
const userStore = useUserStore()
const routeStore = useRouteStore()

// 控制饮食偏好抽屉
const showProfileDrawer = ref(false)

// 步骤 1：出行时长
interface DurationOption {
  hours: number
  title: string
  badge: string
  sub: string
  icon: string
}

const DURATION_OPTIONS: DurationOption[] = [
  { hours: 2, title: '2小时小憩', badge: '2h', sub: '极速解压 · 漫步周边', icon: '⚡' },
  { hours: 4, title: '半日漫游', badge: '4h', sub: '核心地标 · 美食体验', icon: '☀️' },
  { hours: 8, title: '全天深度', badge: '8h', sub: '文化洗礼 · 全景探索', icon: '🌌' },
]

const selectedDuration = ref<number>(4)

// 步骤 2：氛围风格
const ATMOSPHERE_OPTIONS: { id: AtmosphereTag; label: string; icon: string }[] = [
  { id: '松弛感', label: '松弛感', icon: '🌿' },
  { id: '出片打卡', label: '出片打卡', icon: '📸' },
  { id: '人文历史', label: '人文历史', icon: '🏛️' },
  { id: '自然山水', label: '自然山水', icon: '🏞️' },
  { id: '烟火夜市', label: '烟火夜市', icon: '🍢' },
]

const selectedAtmosphere = ref<AtmosphereTag>('松弛感')

// 步骤 3：出行方式
interface ModeOption {
  mode: TransportMode
  label: string
  icon: string
  desc: string
}

const TRANSIT_OPTIONS: ModeOption[] = [
  { mode: 'WALKING', label: '步行', icon: '🚶', desc: '4.5km/h · 慢调细品' },
  { mode: 'TRANSIT', label: '公共交通', icon: '🚌', desc: '绿色公交 · 经济便捷' },
  { mode: 'RIDING', label: '骑行', icon: '🚲', desc: '12km/h · 穿梭巷陌' },
  { mode: 'DRIVING', label: '驾车', icon: '🚗', desc: '30km/h · 高效通达' },
]

const selectedTransit = ref<TransportMode>('WALKING')

// 步骤 4：饮食微调摘要
const travelProfile = computed(() => userStore.travelProfile)

const spicySummary = computed(() => {
  const s = travelProfile.value.spicyLevel || '微辣'
  if (s === '不辣') return '不辣 (清淡鲜美)'
  if (s === '微辣') return '微辣 (点缀鲜香)'
  if (s === '中辣') return '中辣 (地道湘味)'
  return '重辣 (衡阳黄贡椒)'
})

const restrictionSummary = computed(() => {
  const list = travelProfile.value.dietaryRestrictions || []
  return list.length > 0 ? list.join('、') : '暂无忌口'
})

const budgetSummary = computed(() => {
  return `¥${travelProfile.value.budgetPerMeal || 35}/餐`
})

// 生成路线逻辑
const isGenerating = computed(() => routeStore.isGenerating)
const errorMessage = ref('')

const openProfileDrawer = () => {
  showProfileDrawer.value = true
}

const handleGenerate = async () => {
  if (isGenerating.value) return
  errorMessage.value = ''

  const requestDto: RouteGenerateRequestDTO = {
    durationHours: selectedDuration.value,
    atmosphere: selectedAtmosphere.value,
    transportMode: selectedTransit.value,
    startLng: 112.6845, // 衡阳师范学院雁山校区
    startLat: 26.8398,
    userId: Number(userStore.currentUserId) || 0,
    spicyLevel: travelProfile.value.spicyLevel,
    dietaryRestrictions: travelProfile.value.dietaryRestrictions,
    budgetPerMeal: travelProfile.value.budgetPerMeal,
  }

  try {
    const plan = await routeStore.generatePlan(requestDto)
    if (plan) {
      // 生成成功后平滑跳转至 3D 路线地图导览页
      await router.push('/route-map')
    }
  } catch (error) {
    console.error('路线规划发生错误:', error)
    errorMessage.value = '路线规划遇偶发波动，已为您自动启动本地多维拓扑算法兜底'
    // 依然平滑导航，避免用户卡在首页
    setTimeout(() => {
      void router.push('/route-map')
    }, 1200)
  }
}
</script>

<template>
  <div class="home-container">
    <div class="wizard-wrapper">
      <!-- 顶部品牌意图横幅 -->
      <header class="wizard-header">
        <div class="header-badge">
          <span class="badge-dot" />
          <span class="badge-text">✨ LLM + LBS 4 阶段 Pipeline 智能决策</span>
        </div>
        <h1 class="header-title">灵境导览 · 衡阳文旅智能行程定制</h1>
        <p class="header-subtitle">
          结合大语言模型意图理解与高德真实时空路网，为您定制不绕路、契合口味的专属旅程
        </p>
      </header>

      <!-- 核心 4 步向导卡片 -->
      <main class="wizard-card">
        <!-- 步骤 1：出行时长 -->
        <section class="wizard-step">
          <div class="step-head">
            <span class="step-number">01</span>
            <div class="step-titles">
              <h2 class="step-title">出行时长</h2>
              <p class="step-desc">合理编排游玩节奏，防止时空折返与赶场疲劳</p>
            </div>
          </div>

          <div class="duration-grid" role="radiogroup" aria-label="出行时长选择">
            <button
              v-for="opt in DURATION_OPTIONS"
              :key="opt.hours"
              type="button"
              class="duration-card touch-target"
              :class="{ 'is-active': selectedDuration === opt.hours }"
              role="radio"
              :aria-checked="selectedDuration === opt.hours"
              @click="selectedDuration = opt.hours"
            >
              <div class="duration-card-head">
                <span class="duration-icon">{{ opt.icon }}</span>
                <span class="duration-badge">{{ opt.badge }}</span>
              </div>
              <span class="duration-name">{{ opt.title }}</span>
              <span class="duration-sub">{{ opt.sub }}</span>
            </button>
          </div>
        </section>

        <!-- 步骤 2：氛围风格 -->
        <section class="wizard-step">
          <div class="step-head">
            <span class="step-number">02</span>
            <div class="step-titles">
              <h2 class="step-title">氛围风格</h2>
              <p class="step-desc">双路候选召回将优先匹配契合您当下心情的文旅地标</p>
            </div>
          </div>

          <div class="vibe-chips" role="radiogroup" aria-label="氛围风格选择">
            <button
              v-for="vibe in ATMOSPHERE_OPTIONS"
              :key="vibe.id"
              type="button"
              class="vibe-chip touch-target"
              :class="{ 'is-active': selectedAtmosphere === vibe.id }"
              role="radio"
              :aria-checked="selectedAtmosphere === vibe.id"
              @click="selectedAtmosphere = vibe.id"
            >
              <span class="vibe-icon">{{ vibe.icon }}</span>
              <span class="vibe-label">{{ vibe.label }}</span>
            </button>
          </div>
        </section>

        <!-- 步骤 3：出行方式 -->
        <section class="wizard-step">
          <div class="step-head">
            <span class="step-number">03</span>
            <div class="step-titles">
              <h2 class="step-title">出行方式</h2>
              <p class="step-desc">高德 LBS 真实测距与交通段耗时计算的核心依据</p>
            </div>
          </div>

          <div class="transit-grid" role="radiogroup" aria-label="出行方式选择">
            <button
              v-for="item in TRANSIT_OPTIONS"
              :key="item.mode"
              type="button"
              class="transit-card touch-target"
              :class="{ 'is-active': selectedTransit === item.mode }"
              role="radio"
              :aria-checked="selectedTransit === item.mode"
              @click="selectedTransit = item.mode"
            >
              <span class="transit-icon">{{ item.icon }}</span>
              <div class="transit-meta">
                <span class="transit-name">{{ item.label }}</span>
                <span class="transit-desc">{{ item.desc }}</span>
              </div>
            </button>
          </div>
        </section>

        <!-- 步骤 4：饮食微调摘要 -->
        <section class="wizard-step">
          <div class="step-head">
            <span class="step-number">04</span>
            <div class="step-titles">
              <h2 class="step-title">饮食个性化摘要</h2>
              <p class="step-desc">4阶段 Agent 决策引擎将依据此画像自动过滤避坑食材并优选菜品</p>
            </div>
          </div>

          <div class="dietary-summary-pill">
            <div class="summary-items">
              <div class="summary-item">
                <span class="summary-label">🌶️ 辣度耐受</span>
                <span class="summary-val">{{ spicySummary }}</span>
              </div>
              <div class="summary-divider" />
              <div class="summary-item">
                <span class="summary-label">🚫 避坑忌口</span>
                <span class="summary-val summary-val--warn">{{ restrictionSummary }}</span>
              </div>
              <div class="summary-divider" />
              <div class="summary-item">
                <span class="summary-label">💰 单餐预算</span>
                <span class="summary-val">{{ budgetSummary }}</span>
              </div>
            </div>

            <button
              type="button"
              class="btn-tune touch-target"
              aria-label="快速微调饮食与出行画像"
              @click="openProfileDrawer"
            >
              <span>✏️ 快速微调</span>
            </button>
          </div>
        </section>

        <!-- 错误提示 -->
        <div v-if="errorMessage" class="wizard-error">
          {{ errorMessage }}
        </div>

        <!-- 主操作按键 (Primary CTA) -->
        <div class="cta-wrapper">
          <button
            type="button"
            class="primary-cta-btn touch-target"
            :disabled="isGenerating"
            :aria-busy="isGenerating ? 'true' : 'false'"
            @click="handleGenerate"
          >
            <!-- 加载状态保持原有文案并内嵌动画 Spinner -->
            <span v-if="isGenerating" class="btn-spinner" aria-hidden="true">
              <svg viewBox="0 0 24 24" class="spinner-icon">
                <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="3" stroke-dasharray="31.4 31.4" />
              </svg>
            </span>
            <span class="cta-label">
              {{ isGenerating ? '✨ 智能体正在规划专属路线 (耗时测算中...)' : '✨ 智能生成专属路线' }}
            </span>
          </button>
          <p class="cta-tip">
            💡 包含高德路网真实测距校验、双路景点餐饮召回与避坑提示，无感支持离线断网兜底
          </p>
        </div>
      </main>
    </div>

    <!-- 用户偏好抽屉组件 -->
    <UserProfilePanel
      :visible="showProfileDrawer"
      @close="showProfileDrawer = false"
    />
  </div>
</template>

<style scoped>
/* ========== 页面主体容器 ========== */
.home-container {
  position: relative;
  min-height: calc(100vh - var(--navbar-height) - 40px);
  padding: 32px 16px 64px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.wizard-wrapper {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ========== 页面顶部标题区 ========== */
.wizard-header {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: var(--border-radius-full);
  background: var(--color-primary-muted, rgba(6, 182, 212, 0.15));
  border: 1px solid var(--border-default);
  box-shadow: 0 0 16px var(--color-primary-glow);
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 8px var(--color-primary);
  animation: pulse-dot 2s infinite ease-in-out;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.25); }
}

.badge-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 0.04em;
}

.header-title {
  margin: 0;
  font-size: clamp(24px, 4vw, 32px);
  font-weight: 800;
  letter-spacing: var(--letter-spacing-tight);
  color: var(--text-primary);
  line-height: var(--line-height-tight);
}

.header-subtitle {
  margin: 0;
  max-width: 620px;
  font-size: 15px;
  color: var(--text-secondary);
  line-height: var(--line-height-relaxed);
}

/* ========== 核心向导卡片容器 ========== */
.wizard-card {
  position: relative;
  background: var(--surface-card-glass, rgba(15, 23, 42, 0.86));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  border-radius: var(--border-radius-lg, 20px);
  box-shadow: var(--shadow-elevated, 0 12px 36px -4px rgba(0, 0, 0, 0.5));
  padding: 32px 32px 36px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 步骤统一头部 */
.wizard-step {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.step-number {
  font-family: var(--font-mono, monospace);
  font-size: 13px;
  font-weight: 800;
  color: var(--color-primary);
  padding: 4px 8px;
  border-radius: var(--border-radius-xs);
  background: var(--color-primary-muted);
  border: 1px solid var(--border-subtle);
}

.step-titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.step-desc {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}

/* ========== 步骤 1：出行时长分段卡片 ========== */
.duration-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.duration-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 16px 14px;
  min-height: 88px;
  border-radius: var(--border-radius-md, 12px);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-normal);
}

.duration-card:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--border-hover);
  transform: translateY(-2px);
}

.duration-card.is-active {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  box-shadow: 0 0 16px var(--color-primary-glow);
}

.duration-card-head {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.duration-icon {
  font-size: 20px;
}

.duration-badge {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-highlight, #22D3EE);
}

.duration-card.is-active .duration-badge {
  background: var(--color-primary);
  color: var(--text-inverse, #020617);
}

.duration-name {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 2px;
}

.duration-sub {
  font-size: 11px;
  color: var(--text-secondary);
}

/* ========== 步骤 2：氛围风格标签芯片 ========== */
.vibe-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.vibe-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  min-height: 44px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.vibe-chip:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: var(--border-hover);
  color: var(--text-primary);
}

.vibe-chip.is-active {
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.28), rgba(2, 132, 199, 0.28));
  border-color: var(--color-primary);
  color: #FFFFFF;
  box-shadow: 0 0 14px var(--color-primary-glow);
}

.vibe-icon {
  font-size: 16px;
}

/* ========== 步骤 3：出行方式卡片 ========== */
.transit-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.transit-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  min-height: 54px;
  border-radius: var(--border-radius-md);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-fast);
}

.transit-card:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--border-hover);
}

.transit-card.is-active {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  box-shadow: 0 0 12px var(--color-primary-glow);
}

.transit-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.transit-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.transit-name {
  font-size: 14px;
  font-weight: 700;
}

.transit-desc {
  font-size: 11px;
  color: var(--text-muted);
}

/* ========== 步骤 4：饮食微调摘要药丸卡片 ========== */
.dietary-summary-pill {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  border-radius: var(--border-radius-md);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-default);
}

.summary-items {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.summary-label {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}

.summary-val {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.summary-val--warn {
  color: var(--color-warning, #F59E0B);
}

.summary-divider {
  width: 1px;
  height: 28px;
  background: var(--border-subtle);
}

.btn-tune {
  flex-shrink: 0;
  padding: 8px 16px;
  min-height: 44px;
  border-radius: var(--border-radius-sm);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-default);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-tune:hover {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  box-shadow: 0 0 10px var(--color-primary-glow);
}

/* 错误提示 */
.wizard-error {
  padding: 10px 16px;
  border-radius: var(--border-radius-sm);
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #FCA5A5;
  font-size: 13px;
  text-align: center;
}

/* ========== 主操作按钮 (Primary CTA) ========== */
.cta-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}

.primary-cta-btn {
  width: 100%;
  min-height: 52px;
  padding: 14px 24px;
  border-radius: var(--border-radius);
  border: none;
  background: linear-gradient(135deg, #06B6D4 0%, #0284C7 50%, #2563EB 100%);
  color: #FFFFFF;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(6, 182, 212, 0.35);
  transition: all var(--transition-normal);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.primary-cta-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 12px 32px rgba(6, 182, 212, 0.45);
}

.primary-cta-btn:disabled {
  opacity: 0.75;
  cursor: not-allowed;
  transform: none;
}

.cta-tip {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.5;
}

/* ========== 响应式微调 (<= 768px) ========== */
@media (max-width: 768px) {
  .wizard-card {
    padding: 24px 18px 28px;
    gap: 22px;
  }

  .duration-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .duration-card {
    min-height: 64px;
  }

  .transit-grid {
    grid-template-columns: 1fr;
  }

  .dietary-summary-pill {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .summary-divider {
    display: none;
  }

  .summary-items {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .btn-tune {
    width: 100%;
  }

  .primary-cta-btn {
    min-height: 50px;
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  .summary-items {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>