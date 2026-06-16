<script setup lang="ts">
/**
 * src/components/DailyAICard.vue - 每日 AI 制作卡片
 *
 * 基于数据驱动的沉浸式电竞风卡片组件
 * 尺寸由父组件控制，此组件仅负责内容展示和事件抛出
 */
import { reactive } from 'vue'

// Mock 数据
const cardData = reactive({
  date: new Date().getDate().toString().padStart(2, '0'),
  year: new Date().getFullYear(),
  month: (new Date().getMonth() + 1).toString().padStart(2, '0'),
  weekday: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][new Date().getDay()],
  solarTerm: '清明',
  heroName: '李白',
  heroTitle: '青莲剑仙',
  quote: '大河之剑天上来！',
  bgUrl: 'https://images.unsplash.com/photo-1534796636912-3b95b3ab5986?w=1200&q=80',
  heroImgUrl: 'https://api.dicebear.com/7.x/personas/svg?seed=swordsman'
})

// 抛出关闭事件
const emit = defineEmits<{
  (e: 'close'): void
}>()

const handleClose = () => {
  emit('close')
}
</script>

<template>
  <div class="daily-ai-card">
    <!-- 背景层 -->
    <div class="card-background">
      <img :src="cardData.bgUrl" alt="背景图" class="bg-image" />
      <div class="bg-gradient" />
    </div>

    <!-- 关闭按钮 -->
    <button class="close-btn" @click="handleClose" aria-label="关闭">
      ×
    </button>

    <!-- 内容展示区 -->
    <div class="card-content">
      <!-- 左侧：日期信息 -->
      <div class="date-section">
        <div class="date-main">
          <span class="date-day">{{ cardData.date }}</span>
        </div>
        <div class="date-meta">
          <span class="date-y-m">{{ cardData.year }}/{{ cardData.month }}</span>
          <span class="date-weekday">{{ cardData.weekday }}</span>
        </div>
      </div>

      <!-- 中间：节气标签 -->
      <div class="solar-term-badge">
        <span class="solar-term-icon">❀</span>
        <span class="solar-term-text">{{ cardData.solarTerm }}</span>
      </div>

      <!-- 右侧：英雄信息 -->
      <div class="hero-section">
        <div class="hero-avatar-wrapper">
          <img :src="cardData.heroImgUrl" :alt="cardData.heroName" class="hero-avatar" />
          <div class="hero-glow" />
        </div>
        <div class="hero-info">
          <h3 class="hero-name">{{ cardData.heroName }}</h3>
          <p class="hero-title">{{ cardData.heroTitle }}</p>
        </div>
      </div>
    </div>

    <!-- 底部：励志语录 -->
    <div class="quote-section">
      <div class="quote-line" />
      <p class="quote-text">{{ cardData.quote }}</p>
      <div class="quote-line" />
    </div>

    <!-- AI 标签 -->
    <div class="ai-badge">
      <span class="ai-badge-icon">◆</span>
      <span class="ai-badge-text">AI 今日推荐</span>
    </div>
  </div>
</template>

<style scoped>
/* ========== 外层容器 ========== */
.daily-ai-card {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* ========== 背景层 ========== */
.card-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.daily-ai-card:hover .bg-image {
  transform: scale(1.05);
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.5) 40%,
    rgba(255, 255, 255, 0.92) 100%
  );
}

/* ========== 关闭按钮 ========== */
.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 50%;
  color: rgba(0, 0, 0, 0.4);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  z-index: 10;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
}

/* ========== 内容展示区 ========== */
.card-content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 60px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 28px;
  z-index: 2;
}

/* 日期区域 */
.date-section {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.date-main {
  line-height: 1;
}

.date-day {
  font-size: 72px;
  font-weight: 900;
  color: var(--accent-cyan);
  letter-spacing: -4px;
  font-family: 'Inter', 'Segoe UI', system-ui, sans-serif;
}

.date-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.date-y-m,
.date-weekday {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

/* 节气标签 */
.solar-term-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.solar-term-icon {
  font-size: 16px;
  color: var(--accent-cyan);
}

.solar-term-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--accent-cyan);
  letter-spacing: 2px;
}

/* 英雄区域 */
.hero-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-avatar-wrapper {
  position: relative;
}

.hero-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid rgba(59, 130, 246, 0.3);
  object-fit: cover;
}

.hero-glow {
  display: none;
}

.hero-info {
  text-align: right;
}

.hero-name {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.hero-title {
  font-size: 13px;
  color: var(--accent-cyan);
  margin: 0;
  letter-spacing: 3px;
}

/* ========== 底部语录区 ========== */
.quote-section {
  position: absolute;
  bottom: 16px;
  left: 28px;
  right: 28px;
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 2;
}

.quote-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(
    to right,
    transparent,
    rgba(59, 130, 246, 0.2),
    transparent
  );
}

.quote-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  white-space: nowrap;
}

/* ========== AI 标签 ========== */
.ai-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--accent-cyan), #60A5FA);
  z-index: 10;
}

.ai-badge-icon {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.9);
}

.ai-badge-text {
  font-size: 11px;
  font-weight: 600;
  color: #FFFFFF;
  letter-spacing: 1px;
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .daily-ai-card {
    border-radius: 12px;
  }

  .date-day {
    font-size: 48px;
    letter-spacing: -2px;
  }

  .date-meta {
    gap: 8px;
  }

  .date-y-m,
  .date-weekday {
    font-size: 12px;
  }

  .card-content {
    flex-direction: column;
    gap: 14px;
    padding: 16px 18px;
  }

  .solar-term-badge {
    padding: 6px 12px;
    align-self: flex-start;
  }

  .solar-term-text {
    font-size: 13px;
  }

  .hero-section {
    align-self: flex-end;
    gap: 12px;
  }

  .hero-avatar {
    width: 56px;
    height: 56px;
  }

  .hero-glow {
    width: 70px;
    height: 70px;
  }

  .hero-name {
    font-size: 20px;
  }

  .hero-title {
    font-size: 11px;
    letter-spacing: 2px;
  }

  .quote-section {
    bottom: 12px;
    left: 18px;
    right: 18px;
    gap: 10px;
  }

  .quote-text {
    font-size: 13px;
    white-space: normal;
    text-align: center;
    line-height: 1.4;
  }

  .ai-badge {
    padding: 4px 10px;
    border-radius: 8px;
  }

  .ai-badge-text {
    font-size: 10px;
  }
}

@media (max-width: 480px) {
  .card-content {
    padding: 14px 14px;
    gap: 12px;
  }

  .date-day {
    font-size: 40px;
  }

  .hero-avatar {
    width: 48px;
    height: 48px;
  }

  .hero-name {
    font-size: 18px;
  }

  .hero-title {
    font-size: 10px;
  }

  .quote-section {
    bottom: 10px;
    left: 14px;
    right: 14px;
  }

  .quote-text {
    font-size: 12px;
  }
}
</style>