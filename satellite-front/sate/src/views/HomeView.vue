<script setup lang="ts">
/**
 * src/views/HomeView.vue - 峡谷卫星首页
 *
 * 核心功能：
 * 1. 每日 AI 卡片（可关闭，父组件控制状态）
 * 2. 动态功能入口布局（绝对定位 + 动态 Class 切换）
 */
import { ref } from 'vue'
import DailyAICard from '@/components/DailyAICard.vue'

// 卡片可见性状态
const isCardVisible = ref(true)

// 关闭卡片
const handleCardClose = () => {
  isCardVisible.value = false
}
</script>

<template>
  <div class="home-view">
    <!-- ========== 主体交互区域（相对定位） ========== -->
    <div 
      class="home-interactive-area"
      :class="{ 'layout-expanded': isCardVisible, 'layout-collapsed': !isCardVisible }"
    >
      <!-- 每日 AI 卡片 - 绝对独立，单独受控 -->
      <Transition name="card-fade">
        <div v-if="isCardVisible" class="daily-card-positioner">
          <div class="daily-card-container">
            <DailyAICard @close="handleCardClose" />
          </div>
        </div>
      </Transition>

      <!-- ========== 功能入口卡片 ========== -->
      <div class="left-btn-group">
        <!-- AI 全能咨询 -->
        <router-link
          to="/ai-consult"
          class="feature-card glass-card"
          :class="{ 'feature-card-active': $route.name === 'ai-consult' }"
        >
          <div class="feature-icon">🤖</div>
          <div class="feature-content">
            <h3 class="feature-title">AI 全能咨询</h3>
            <p class="feature-desc">智能问答，秒级响应</p>
          </div>
          <div class="feature-glow feature-glow-cyan" />
          <div class="feature-arrow">→</div>
        </router-link>

        <!-- 本命英雄 -->
        <div
          class="feature-card glass-card feature-card-disabled"
          title="功能开发中"
        >
          <div class="feature-icon">⚔️</div>
          <div class="feature-content">
            <h3 class="feature-title">帮选英雄</h3>
            <p class="feature-desc">测试你的本命英雄</p>
          </div>
          <div class="feature-badge">开发中</div>
        </div>
      </div>

      <div class="right-btn-group">
        <!-- 圣地巡游 -->
        <router-link
          to="/holy-land-tour"
          class="feature-card glass-card"
          :class="{ 'feature-card-active': $route.name === 'holy-land-tour' }"
        >
          <div class="feature-icon">🗺️</div>
          <div class="feature-content">
            <h3 class="feature-title">圣地巡游</h3>
            <p class="feature-desc">文旅路线规划</p>
          </div>
          <div class="feature-glow feature-glow-red" />
          <div class="feature-arrow">→</div>
        </router-link>

        <!-- 赛事追踪 -->
        <div
          class="feature-card glass-card feature-card-disabled"
          title="功能开发中"
        >
          <div class="feature-icon">📊</div>
          <div class="feature-content">
            <h3 class="feature-title">赛事追踪</h3>
            <p class="feature-desc">查询成绩与路径</p>
          </div>
          <div class="feature-badge">开发中</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-view {
  padding: 0;
  min-height: calc(100vh - var(--navbar-height));
}

/* ========== 主体交互区域（强制撑满全屏，视口级布局） ========== */
.home-interactive-area {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw; /* 强制视口宽度 */
  height: 100vh;
  pointer-events: none; /* 防止遮挡底层视频点击 */
  transition: all 0.6s cubic-bezier(0.25, 1, 0.5, 1);
}

/* ========== 每日 AI 卡片定位器 - 恢复大气尺寸与绝对居中 ========== */
.daily-card-positioner {
  pointer-events: auto; /* 恢复点击权限 */
  position: absolute;
  bottom: 12vh;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  width: 45vw; /* 占据中间大片区域 */
  min-width: 600px; /* 防止缩太小导致内部挤压 */
  max-width: 900px;
  transition: all 0.6s cubic-bezier(0.25, 1, 0.5, 1);
}

.daily-card-container {
  width: 100%;
  height: 260px;
  padding: 0 40px;
  box-sizing: border-box;
}

/* ========== 功能卡片（固定尺寸 + 绝对定位） ========== */
.feature-card {
  position: absolute;
  width: 260px;
  height: 130px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  border-radius: var(--border-radius);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(var(--blur-amount));
  -webkit-backdrop-filter: blur(var(--blur-amount));
  border: 1px solid var(--glass-border);
  text-decoration: none;
  z-index: 50 !important;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.6s cubic-bezier(0.25, 1, 0.5, 1);
}

/* 玻璃卡片 Hover 效果 */
.feature-card:hover:not(.feature-card-disabled) {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

/* 激活状态 */
.feature-card-active {
  border-color: var(--accent-cyan) !important;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.15);
}

/* 禁用状态 */
.feature-card-disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.feature-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  color: var(--text-secondary);
  font-size: 10px;
}

/* ========== 左右按钮组（视口级绝对贴边） ========== */
.left-btn-group,
.right-btn-group {
  position: absolute;
  bottom: 12vh;
  width: 280px;
  z-index: 50;
  display: flex;
  flex-direction: column;
  gap: 24px;
  pointer-events: auto; /* 恢复点击权限 */
}

.left-btn-group {
  left: 40px; /* 真正的贴近左屏幕 */
}

.right-btn-group {
  right: 40px; /* 真正的贴近右屏幕 */
}

/* 按钮组内部卡片样式 */
.left-btn-group .feature-card,
.right-btn-group .feature-card {
  position: relative;
  width: 100%;
  height: 130px;
  margin: 0;
}

/* ========== 布局状态 A：卡片展开（左右按钮组 + 中央卡片） ========== */
.layout-expanded .left-btn-group,
.layout-expanded .right-btn-group {
  display: flex;
}

.layout-expanded .daily-card-positioner {
  display: block;
}

/* ========== 布局状态 B：卡片关闭（底部水平聚集） ========== */
.layout-collapsed .daily-card-positioner {
  display: none;
}

.layout-collapsed .home-interactive-area {
  display: grid !important;
  grid-template-columns: repeat(4, 260px);
  justify-content: center;
  align-items: flex-end;
  gap: 24px;
  padding-bottom: 12vh;
}

.layout-collapsed .left-btn-group,
.layout-collapsed .right-btn-group {
  display: contents; /* 让子元素直接成为 grid 项目 */
}

.layout-collapsed .feature-card {
  position: relative !important;
  width: 260px !important;
  height: 130px !important;
  left: auto !important;
  right: auto !important;
  bottom: auto !important;
  transform: none !important;
  grid-row: 1; /* 所有卡片都在第一行 */
}

/* 为每个卡片指定 grid 列位置 */
.layout-collapsed .feature-1 { grid-column: 1; }
.layout-collapsed .feature-2 { grid-column: 2; }
.layout-collapsed .feature-3 { grid-column: 3; }
.layout-collapsed .feature-4 { grid-column: 4; }

/* 底部模式 Hover 效果 */
.layout-collapsed .feature-card:hover {
  transform: translateY(-8px) scale(1.03) !important;
}

/* ========== 功能卡片内容样式 ========== */
.feature-icon {
  font-size: 36px;
  flex-shrink: 0;
}

.feature-content {
  flex: 1;
}

.feature-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.feature-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.3;
}

.feature-arrow {
  font-size: 20px;
  color: var(--text-secondary);
  opacity: 0;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.feature-card:hover .feature-arrow {
  opacity: 1;
  color: var(--accent-cyan);
  transform: translateX(4px);
}

/* ========== 发光边框效果 ========== */
.feature-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  opacity: 0.8;
  transition: all 0.3s ease;
}

.feature-card:hover .feature-glow {
  opacity: 1;
  height: 4px;
}

.feature-glow-cyan {
  background: linear-gradient(90deg, transparent, var(--accent-cyan), transparent);
  opacity: 0.6;
}

.feature-glow-red {
  background: linear-gradient(90deg, transparent, var(--accent-red), transparent);
  opacity: 0.6;
}

/* ========== 卡片过渡动画 ========== */
.card-fade-enter-active {
  transition: all 0.5s cubic-bezier(0.25, 1, 0.5, 1);
}

.card-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.55, 0, 1, 0.45);
}

.card-fade-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(30px) scale(0.95);
}

.card-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px) scale(0.9);
}

/* ========== 响应式适配 ========== */
@media (max-width: 1280px) {
  .left-btn-group,
  .right-btn-group {
    width: 260px;
  }

  .left-btn-group {
    left: 30px;
  }

  .right-btn-group {
    right: 30px;
  }

  .daily-card-positioner {
    width: 50vw;
    min-width: 550px;
    max-width: 800px;
  }

  .daily-card-container {
    height: 220px;
    padding: 0 30px;
  }

  .feature-card {
    height: 110px;
    padding: 0 18px;
  }
}

@media (max-width: 1024px) {
  .left-btn-group,
  .right-btn-group {
    width: 240px;
  }

  .left-btn-group {
    left: 20px;
  }

  .right-btn-group {
    right: 20px;
  }

  .daily-card-positioner {
    width: 55vw;
    min-width: 480px;
    max-width: 700px;
  }

  .daily-card-container {
    height: 200px;
    padding: 0 20px;
  }

  .feature-card {
    height: 90px;
    padding: 0 14px;
  }

  .feature-icon {
    font-size: 28px;
  }

  .feature-title {
    font-size: 14px;
  }

  .feature-desc {
    font-size: 11px;
  }
}

/* ========== 平板和小屏幕（≤768px） ========== */
@media (max-width: 768px) {
  .home-interactive-area {
    position: relative;
    min-height: calc(100vh - var(--navbar-height));
    height: auto;
    width: 100%;
    padding: 20px 16px;
    padding-bottom: calc(20px + var(--safe-area-bottom));
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .left-btn-group,
  .right-btn-group {
    position: relative;
    width: 100%;
    left: auto;
    right: auto;
    bottom: auto;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  /* 卡片展开状态 */
  .layout-expanded .left-btn-group,
  .layout-expanded .right-btn-group {
    display: grid;
  }

  .layout-expanded .daily-card-positioner {
    order: -1;
  }

  /* 卡片关闭状态 */
  .layout-collapsed .home-interactive-area {
    display: flex !important;
    flex-direction: column;
    gap: 16px;
  }

  .layout-collapsed .left-btn-group,
  .layout-collapsed .right-btn-group {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .layout-collapsed .feature-card {
    width: 100% !important;
    min-height: 100px !important;
    height: auto !important;
    grid-column: auto !important;
  }

  .daily-card-container {
    width: 100%;
    height: auto;
    min-height: 180px;
    padding: 0 16px;
  }

  .daily-card-positioner {
    position: relative;
    bottom: auto;
    left: auto;
    right: auto;
    width: 100%;
    min-width: 0;
    max-width: none;
    margin: 0 auto;
  }

  .feature-card {
    position: relative !important;
    width: 100% !important;
    min-height: 100px !important;
    height: auto !important;
    left: auto !important;
    right: auto !important;
    bottom: auto !important;
    transform: none !important;
    padding: 16px 18px;
    gap: 14px;
    border-radius: 16px;
  }

  .feature-icon {
    font-size: 36px;
  }

  .feature-title {
    font-size: 16px;
  }

  .feature-desc {
    font-size: 13px;
  }

  .feature-arrow {
    opacity: 0.6;
    font-size: 20px;
  }

  .feature-card:active {
    transform: scale(0.97) !important;
    transition-duration: 0.1s;
    background: rgba(59, 130, 246, 0.08);
  }

  .card-fade-enter-from,
  .card-fade-leave-to {
    transform: translateY(20px) scale(0.95);
  }
}

/* ========== 手机竖屏（≤520px） ========== */
@media (max-width: 520px) {
  .home-interactive-area {
    padding: 16px 12px;
    padding-bottom: calc(16px + var(--safe-area-bottom));
    gap: 14px;
  }

  .left-btn-group,
  .right-btn-group {
    gap: 10px;
  }

  .layout-collapsed .left-btn-group,
  .layout-collapsed .right-btn-group {
    gap: 10px;
  }

  .feature-card {
    min-height: 90px !important;
    padding: 14px 14px;
    gap: 12px;
  }

  .feature-icon {
    font-size: 32px;
  }

  .feature-title {
    font-size: 15px;
  }

  .feature-desc {
    font-size: 12px;
  }

  .daily-card-container {
    padding: 0 12px;
    min-height: 160px;
  }
}

/* ========== 超小屏幕（≤380px） ========== */
@media (max-width: 380px) {
  .left-btn-group,
  .right-btn-group {
    grid-template-columns: 1fr;
  }

  .feature-card {
    min-height: 80px !important;
  }

  .feature-icon {
    font-size: 28px;
  }
}
</style>