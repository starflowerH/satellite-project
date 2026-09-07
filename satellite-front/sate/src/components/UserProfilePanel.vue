<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useUserStore } from '@/store/user'
import type { SpicyLevel, UserProfileVO } from '@/types/travel'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const userStore = useUserStore()

const isSaving = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error'>('success')
let toastTimer: ReturnType<typeof setTimeout> | null = null

// 本地草稿表单状态
const form = ref<UserProfileVO>({
  userId: 0,
  spicyLevel: '微辣',
  flavorPref: '咸鲜',
  dietaryRestrictions: ['不吃内脏'],
  travelPace: '松弛',
  budgetPerMeal: 35,
})

// 辣度等级定义
const SPICY_OPTIONS: { level: SpicyLevel; label: string; desc: string; icon: string }[] = [
  { level: '不辣', label: '不辣', desc: '清淡鲜美 · 原汁原味', icon: '🟢' },
  { level: '微辣', label: '微辣', desc: '微辛提味 · 点缀鲜香', icon: '🟡' },
  { level: '中辣', label: '中辣', desc: '地道湘味 · 鲜辣开胃', icon: '🟠' },
  { level: '重辣', label: '重辣', desc: '衡阳黄贡椒 · 爆炒香辣', icon: '🔴' },
]

// 常见忌口标签选项
const DIETARY_OPTIONS: { id: string; label: string; icon: string }[] = [
  { id: '不吃内脏', label: '不吃内脏', icon: '🥩' },
  { id: '免香菜', label: '免香菜', icon: '🌿' },
  { id: '不吃海鲜', label: '不吃海鲜', icon: '🐟' },
  { id: '清真饮食', label: '清真饮食', icon: '🕌' },
  { id: '免葱姜蒜', label: '免葱姜蒜', icon: '🧄' },
  { id: '纯素食', label: '纯素食', icon: '🥗' },
]

// 预算预设快捷锚点
const BUDGET_PRESETS = [
  { value: 20, label: '¥20 (特色小吃)' },
  { value: 35, label: '¥35 (地道简餐)' },
  { value: 60, label: '¥60 (特色土菜)' },
  { value: 100, label: '¥100 (丰盛宴席)' },
]

// 动态预算档位说明
const budgetTier = computed(() => {
  const b = form.value.budgetPerMeal || 35
  if (b < 35) {
    return { name: '平价实惠', desc: '主推衡阳鲜鱼粉、校园后街糖水与特色小吃', color: 'tier-green' }
  }
  if (b <= 80) {
    return { name: '品质小资', desc: '推荐东洲土菜馆老豆腐鱼鲜、招牌农家一碗香等舒适聚餐', color: 'tier-cyan' }
  }
  return { name: '丰盛宴饮', desc: '解锁衡东土头碗、特色江鲜与地道全景宴客体验', color: 'tier-gold' }
})

// 同步 Store 状态至本地草稿
const syncFromStore = () => {
  const current = userStore.travelProfile
  form.value = {
    userId: current.userId ?? 0,
    spicyLevel: current.spicyLevel || '微辣',
    flavorPref: current.flavorPref || '咸鲜',
    dietaryRestrictions: [...(current.dietaryRestrictions || ['不吃内脏'])],
    travelPace: current.travelPace || '松弛',
    budgetPerMeal: current.budgetPerMeal || 35,
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      syncFromStore()
      toastMessage.value = ''
      // 在打开时重新获取远端画像
      void userStore.fetchTravelProfile()
    }
  },
  { immediate: true },
)

const selectSpicy = (level: SpicyLevel) => {
  form.value.spicyLevel = level
}

const toggleRestriction = (tag: string) => {
  const list = form.value.dietaryRestrictions
  const index = list.indexOf(tag)
  if (index >= 0) {
    list.splice(index, 1)
  } else {
    list.push(tag)
  }
}

const isRestrictionSelected = (tag: string): boolean => {
  return form.value.dietaryRestrictions.includes(tag)
}

const setPresetBudget = (val: number) => {
  form.value.budgetPerMeal = val
}

const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = msg
  toastType.value = type
  toastTimer = setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

const handleSave = async () => {
  isSaving.value = true
  try {
    await userStore.updateTravelProfile(form.value)
    showToast('✨ 偏好配置已保存并同步至智能体', 'success')
    setTimeout(() => {
      emit('close')
    }, 600)
  } catch (error) {
    console.error('保存画像偏好失败:', error)
    showToast('保存异常，请稍后重试', 'error')
  } finally {
    isSaving.value = false
  }
}

const handleClose = () => {
  emit('close')
}

// 全局键盘 Escape 支持
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && props.visible) {
    handleClose()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer)
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="props.visible" class="drawer-backdrop" @click.self="handleClose">
        <aside
          class="drawer-panel"
          role="dialog"
          aria-modal="true"
          aria-labelledby="drawer-title"
        >
          <!-- 抽屉头部 -->
          <header class="drawer-header">
            <div class="header-titles">
              <span class="header-badge">PREFERENCE PROFILE</span>
              <h2 id="drawer-title" class="header-title">出行与饮食偏好画像</h2>
              <p class="header-desc">
                根据您的口味与单餐预算，4阶段 Agent 自动过滤避坑菜品
              </p>
            </div>
            <button
              class="close-btn touch-target"
              type="button"
              aria-label="关闭偏好抽屉"
              @click="handleClose"
            >
              ×
            </button>
          </header>

          <!-- 抽屉主体内容 (弹性纵向滚动) -->
          <main class="drawer-body">
            <!-- 用户身份简要信息 -->
            <div class="profile-card">
              <img :src="userStore.displayAvatar" :alt="userStore.displayName" class="user-avatar" />
              <div class="user-meta">
                <div class="user-name-row">
                  <span class="user-name">{{ userStore.displayName }}</span>
                  <span class="badge-status">
                    <span class="status-dot" /> 偏好实时同步
                  </span>
                </div>
                <div class="user-city-row">
                  <span>📍 所在城市：{{ userStore.userInfo.city || '衡阳市' }}</span>
                </div>
              </div>
            </div>

            <!-- 配置模块 1：辣度等级单选 -->
            <section class="pref-section">
              <div class="section-title-row">
                <span class="section-icon">🌶️</span>
                <h3 class="section-title">辣度耐受等级</h3>
                <span class="active-badge">{{ form.spicyLevel }}</span>
              </div>
              <p class="section-hint">湖南衡阳菜系以鲜辣著称，智能体将严格依此匹配必点菜品：</p>

              <div class="spicy-grid" role="radiogroup" aria-label="辣度等级">
                <button
                  v-for="opt in SPICY_OPTIONS"
                  :key="opt.level"
                  type="button"
                  class="spicy-card touch-target"
                  :class="{ 'is-active': form.spicyLevel === opt.level }"
                  role="radio"
                  :aria-checked="form.spicyLevel === opt.level"
                  @click="selectSpicy(opt.level)"
                >
                  <span class="spicy-icon">{{ opt.icon }}</span>
                  <div class="spicy-info">
                    <span class="spicy-label">{{ opt.label }}</span>
                    <span class="spicy-desc">{{ opt.desc }}</span>
                  </div>
                  <span v-if="form.spicyLevel === opt.level" class="check-mark">✓</span>
                </button>
              </div>
            </section>

            <!-- 配置模块 2：常见忌口标签多选 -->
            <section class="pref-section">
              <div class="section-title-row">
                <span class="section-icon">🚫</span>
                <h3 class="section-title">常见饮食忌口</h3>
                <span class="active-count">已选 {{ form.dietaryRestrictions.length }} 项</span>
              </div>
              <p class="section-hint">
                命中忌口食材的菜品（如黄贡椒脆肚、假羊肉）将自动被剔除或警示：
              </p>

              <div class="restrictions-chips">
                <button
                  v-for="diet in DIETARY_OPTIONS"
                  :key="diet.id"
                  type="button"
                  class="diet-chip touch-target"
                  :class="{ 'is-selected': isRestrictionSelected(diet.id) }"
                  @click="toggleRestriction(diet.id)"
                >
                  <span class="chip-icon">{{ diet.icon }}</span>
                  <span class="chip-label">{{ diet.label }}</span>
                  <span v-if="isRestrictionSelected(diet.id)" class="chip-check">✓</span>
                </button>
              </div>
            </section>

            <!-- 配置模块 3：单餐人均预算滑块 -->
            <section class="pref-section">
              <div class="section-title-row">
                <span class="section-icon">💰</span>
                <h3 class="section-title">单餐人均预算</h3>
                <span class="budget-badge">¥{{ form.budgetPerMeal }} / 餐</span>
              </div>
              <p class="section-hint">动态推荐人均消费在预算以内的特色餐饮商户：</p>

              <!-- 滑块容器 -->
              <div class="slider-container">
                <div class="slider-labels">
                  <span class="slider-min">¥15</span>
                  <span class="slider-current">当前设置：¥{{ form.budgetPerMeal }}</span>
                  <span class="slider-max">¥150</span>
                </div>
                <input
                  v-model.number="form.budgetPerMeal"
                  type="range"
                  min="15"
                  max="150"
                  step="5"
                  class="budget-range"
                  aria-label="单餐人均预算滑块"
                />
              </div>

              <!-- 档位说明徽标 -->
              <div class="tier-card" :class="budgetTier.color">
                <div class="tier-head">
                  <span class="tier-badge">{{ budgetTier.name }}</span>
                  <span class="tier-price">（当前单餐期望约 ¥{{ form.budgetPerMeal }}）</span>
                </div>
                <p class="tier-desc">{{ budgetTier.desc }}</p>
              </div>

              <!-- 常见档位快捷点击 -->
              <div class="presets-row">
                <span class="presets-label">快速设定：</span>
                <div class="presets-chips">
                  <button
                    v-for="p in BUDGET_PRESETS"
                    :key="p.value"
                    type="button"
                    class="preset-chip touch-target"
                    :class="{ 'is-active': form.budgetPerMeal === p.value }"
                    @click="setPresetBudget(p.value)"
                  >
                    {{ p.label }}
                  </button>
                </div>
              </div>
            </section>
          </main>

          <!-- 抽屉底部操作栏 (固底防遮挡) -->
          <footer class="drawer-footer">
            <Transition name="fade">
              <div v-if="toastMessage" class="toast-feedback" :class="`toast--${toastType}`">
                {{ toastMessage }}
              </div>
            </Transition>

            <div class="action-buttons">
              <button
                type="button"
                class="btn-save touch-target"
                :disabled="isSaving"
                @click="handleSave"
              >
                <span v-if="isSaving" class="btn-spinner" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="spinner-icon">
                    <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="3" stroke-dasharray="31.4 31.4" />
                  </svg>
                </span>
                <span>{{ isSaving ? '正在同步画像...' : '💾 保存偏好配置' }}</span>
              </button>
              <button type="button" class="btn-cancel touch-target" @click="handleClose">
                取消
              </button>
            </div>
          </footer>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ========== 抽屉遮罩与基础容器 ========== */
.drawer-backdrop {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(3, 7, 18, 0.68);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  justify-content: flex-end;
  z-index: 1200;
}

.drawer-panel {
  position: relative;
  width: min(480px, 100vw);
  height: 100%;
  background: var(--surface-card, #0F172A);
  border-left: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  box-shadow: -12px 0 36px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ========== 抽屉头部 ========== */
.drawer-header {
  flex-shrink: 0;
  padding: 24px 24px 18px;
  border-bottom: 1px solid var(--border-subtle, rgba(148, 163, 184, 0.12));
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: rgba(15, 23, 42, 0.95);
}

.header-badge {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--color-primary, #06B6D4);
  font-weight: 700;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary, #F8FAFC);
}

.header-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary, #94A3B8);
  line-height: 1.4;
}

.close-btn {
  width: 44px;
  height: 44px;
  min-width: 44px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--border-radius-sm, 8px);
  border: 1px solid var(--border-subtle);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-secondary);
  font-size: 24px;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.15);
  color: var(--color-danger, #EF4444);
  border-color: rgba(239, 68, 68, 0.3);
}

/* ========== 抽屉主体 ========== */
.drawer-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 用户画像卡片 */
.profile-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--border-radius-md, 12px);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-subtle);
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid var(--color-primary);
  flex-shrink: 0;
}

.user-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.badge-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--color-success, #10B981);
  background: rgba(16, 185, 129, 0.12);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
}

.user-city-row {
  font-size: 13px;
  color: var(--text-muted);
}

/* 配置模块通用 */
.pref-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 18px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.active-badge,
.active-count,
.budget-badge {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--color-primary-muted, rgba(6, 182, 212, 0.15));
  color: var(--color-primary, #06B6D4);
  border: 1px solid var(--border-default);
}

.section-hint {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.4;
}

/* 辣度选项网格 */
.spicy-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.spicy-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  min-height: 52px;
  border-radius: var(--border-radius-sm, 8px);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-fast);
}

.spicy-card:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--border-hover);
}

.spicy-card.is-active {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  box-shadow: 0 0 12px var(--color-primary-glow);
}

.spicy-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.spicy-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.spicy-label {
  font-size: 14px;
  font-weight: 700;
}

.spicy-desc {
  font-size: 11px;
  color: var(--text-muted);
}

.check-mark {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 15px;
}

/* 忌口标签芯片 */
.restrictions-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.diet-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  min-height: 44px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.diet-chip:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
}

.diet-chip.is-selected {
  background: rgba(245, 158, 11, 0.15);
  border-color: var(--color-warning, #F59E0B);
  color: #FDE68A;
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.25);
  font-weight: 600;
}

.chip-check {
  font-size: 13px;
  color: var(--color-warning);
}

/* 预算滑块容器 */
.slider-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: rgba(255, 255, 255, 0.02);
  padding: 12px 14px;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--border-subtle);
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
}

.slider-current {
  color: var(--color-primary);
  font-weight: 600;
}

.budget-range {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.15);
  outline: none;
  cursor: pointer;
  accent-color: var(--color-primary);
}

/* 档位说明卡片 */
.tier-card {
  padding: 10px 14px;
  border-radius: var(--border-radius-sm);
  border-left: 3px solid;
  background: rgba(255, 255, 255, 0.02);
}

.tier-card.tier-green {
  border-color: var(--color-success);
  background: rgba(16, 185, 129, 0.06);
}

.tier-card.tier-cyan {
  border-color: var(--color-primary);
  background: rgba(6, 182, 212, 0.06);
}

.tier-card.tier-gold {
  border-color: var(--color-accent-gold);
  background: rgba(245, 158, 11, 0.06);
}

.tier-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.tier-badge {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
}

.tier-price {
  font-size: 11px;
  color: var(--text-muted);
}

.tier-desc {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.4;
}

/* 预算预设点 */
.presets-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.presets-label {
  font-size: 12px;
  color: var(--text-muted);
}

.presets-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preset-chip {
  padding: 4px 10px;
  min-height: 36px;
  border-radius: var(--border-radius-xs);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.preset-chip:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
}

.preset-chip.is-active {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

/* ========== 抽屉底部操作栏 ========== */
.drawer-footer {
  flex-shrink: 0;
  padding: 16px 24px 24px;
  border-top: 1px solid var(--border-subtle);
  background: rgba(15, 23, 42, 0.95);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toast-feedback {
  padding: 10px 14px;
  border-radius: var(--border-radius-sm);
  font-size: 13px;
  text-align: center;
  font-weight: 500;
}

.toast--success {
  background: rgba(16, 185, 129, 0.15);
  color: #34D399;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.toast--error {
  background: rgba(239, 68, 68, 0.15);
  color: #F87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.btn-save {
  flex: 2;
  min-height: 48px;
  border-radius: var(--border-radius-sm);
  border: none;
  background: linear-gradient(135deg, var(--color-primary, #06B6D4), #0284C7);
  color: #FFFFFF;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px var(--color-primary-glow);
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.1);
  box-shadow: 0 6px 20px var(--color-primary-glow);
}

.btn-save:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-cancel {
  flex: 1;
  min-height: 48px;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--border-default);
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}

/* ========== 过渡动画 ========== */
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.28s ease;
}

.drawer-enter-active .drawer-panel,
.drawer-leave-active .drawer-panel {
  transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .drawer-panel,
.drawer-leave-to .drawer-panel {
  transform: translateX(100%);
}

/* ========== 移动端响应式 (<= 768px: 底部抽屉模式) ========== */
@media (max-width: 768px) {
  .drawer-backdrop {
    align-items: flex-end;
  }

  .drawer-panel {
    width: 100vw;
    height: 88vh;
    border-radius: 20px 20px 0 0;
    border-left: none;
    border-top: 1px solid var(--border-default);
  }

  .drawer-enter-from .drawer-panel,
  .drawer-leave-to .drawer-panel {
    transform: translateY(100%);
  }

  .drawer-header {
    padding: 20px 20px 14px;
  }

  .drawer-body {
    padding: 16px 20px;
    gap: 20px;
  }

  .drawer-footer {
    padding: 14px 20px calc(20px + var(--safe-area-bottom));
  }

  .spicy-grid {
    grid-template-columns: 1fr;
  }
}
</style>
