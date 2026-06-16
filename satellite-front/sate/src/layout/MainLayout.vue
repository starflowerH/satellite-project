<script setup lang="ts">
/**
 * MainLayout.vue - 峡谷卫星沉浸式主框架
 *
 * 层级结构（z-index 从低到高）：
 * 1. video-bg (z-index: 0)   - 全屏视频背景层
 * 2. gradient-overlay (z-index: 1) - 深色渐变遮罩，提升上方文字可读性
 * 3. navbar (z-index: 100)  - 玻璃态导航栏
 * 4. content (z-index: 10)  - 路由内容插槽
 */
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/store/user'
import { useHolyTourStore } from '@/store/holyTour'
import LoginModal from '@/components/LoginModal.vue'
import UserProfilePanel from '@/components/UserProfilePanel.vue'


const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const holyTourStore = useHolyTourStore()

const isFullBleedRoute = computed(() => route.name === 'holy-land-tour')

const isMobileMenuOpen = ref(false)
const showLoginModal = ref(false)

const showProfilePanel = ref(false)

const userName = computed(() => userStore.displayName)
const userAvatar = computed(() => userStore.displayAvatar)

// 当前路由名称，用于高亮导航项
const currentRouteName = computed(() => route.name)

// 导航项配置
const navItems = [
  { name: 'home', label: '首页', icon: '🏠', path: '/' },
  { name: 'ai-consult', label: 'AI 助手', icon: '🤖', path: '/ai-consult' },
  { name: 'holy-land-tour', label: '圣地巡游', icon: '🗺️', path: '/holy-land-tour' },
]

const githubUrl = 'https://github.com/starflowerH/satellite-project'

// 检查导航项是否激活
const isActiveNav = (name: string) => currentRouteName.value === name

const handleLoginClick = () => {
  showLoginModal.value = true
}

const handleAvatarClick = () => {
  showProfilePanel.value = true
}

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false
}

/* 菜单打开时锁定 body 滚动，防止背景穿透 */
watch(isMobileMenuOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onUnmounted(() => {
  document.body.style.overflow = ''
})

const navigateTo = (path: string) => {
  router.push(path)
  closeMobileMenu()
}

const handleLogout = () => {
  userStore.logout()
  closeMobileMenu()
  navigateTo('/')
}

const preloadHolyTour = async () => {
  const userId = String(userStore.currentUserId || '').trim()
  if (!userId) return

  const resolvedAgentId = await holyTourStore.bootstrapHolyAgent(userId)
  if (!resolvedAgentId) return

  const city = String(userStore.userInfo.city || '').trim()
  const heroRoute = await holyTourStore.fetchPrimaryHeroRoute(userId, city)
  if (heroRoute) return

  if (city) {
    await holyTourStore.fetchCitySpots(city, userId)
  }
}


const handleLoginSuccess = () => {
  if (!userStore.needsHeroSelection) {
    showLoginModal.value = false
  }

  void preloadHolyTour()
}


watch(
  () => [userStore.isAuthenticated, userStore.needsHeroSelection] as const,
  ([authenticated, needsHeroSelection]) => {
    if (authenticated && needsHeroSelection) {
      showLoginModal.value = true
      return
    }

    if (authenticated && !needsHeroSelection) {
      showLoginModal.value = false
    }
  },
  { immediate: true },
)

watch(
  () => [route.name, userStore.isAuthenticated] as const,
  ([routeName, authenticated]) => {
    if (routeName === 'home' && authenticated) {
      void preloadHolyTour()
    }
  },
  { immediate: true },
)


onMounted(async () => {
  await userStore.bootstrapSession()

  if (userStore.isAuthenticated) {
    void preloadHolyTour()
  }
})

</script>

<template>
  <div class="app-wrapper">
    <!-- ========== 层级1：浅色背景层 ========== -->
    <div class="bg-layer">
      <img
        class="bg-image"
        src="https://images.unsplash.com/photo-1557683316-973673baf926?w=1920&q=80"
        alt=""
        loading="eager"
      />
      <div class="bg-tint" />
    </div>

    <!-- ========== 层级3：玻璃态导航栏 ========== -->
    <nav class="navbar">
      <div class="navbar-content">
        <!-- Logo 区域 -->
        <div class="logo" @click="navigateTo('/')">
          <span class="logo-icon">🛰️</span>
          <span class="logo-text">峡谷卫星</span>
        </div>

        <!-- 桌面端导航项 -->
        <ul class="nav-links">
          <li v-for="item in navItems" :key="item.name">
            <a
              href="#"
              class="nav-item"
              :class="{ active: isActiveNav(item.name) }"
              @click.prevent="navigateTo(item.path)"
            >
              {{ item.label }}
            </a>
          </li>
          <li>
            <a :href="githubUrl" target="_blank" rel="noopener" class="nav-item github-link">
              <svg class="github-icon" viewBox="0 0 16 16" width="18" height="18" fill="currentColor">
                <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
              </svg>
              <span class="github-label">GitHub</span>
            </a>
          </li>
        </ul>

        <!-- 用户区域 -->
        <div class="user-area">
          <button
            v-if="userStore.isAuthenticated"
            type="button"
            class="user-trigger"
            @click="handleAvatarClick"
          >
            <img :src="userAvatar" :alt="userName" class="user-avatar" />
            <span class="user-name">{{ userName }}</span>
            <span class="user-action">查看资料</span>
          </button>

          <button v-else class="btn-login" @click="handleLoginClick">登录</button>

          <!-- 手机端汉堡菜单按钮 -->
          <button class="mobile-menu-btn" @click="toggleMobileMenu" aria-label="菜单">
            <span class="hamburger" :class="{ 'is-active': isMobileMenuOpen }">
              <span></span>
              <span></span>
              <span></span>
            </span>
          </button>
        </div>
      </div>

      <!-- 手机端侧边菜单 -->
      <Transition name="slide-menu">
        <div v-if="isMobileMenuOpen" class="mobile-menu-overlay" @click="closeMobileMenu">
          <div class="mobile-menu" @click.stop>
            <div class="mobile-menu-header">
              <div class="logo" @click="navigateTo('/')">
                <span class="logo-icon">🛰️</span>
                <span class="logo-text">峡谷卫星</span>
              </div>
              <button class="close-btn" @click="closeMobileMenu">✕</button>
            </div>
            <ul class="mobile-nav-links">
              <li v-for="item in navItems" :key="item.name">
                <a
                  href="#"
                  class="mobile-nav-item"
                  :class="{ active: isActiveNav(item.name) }"
                  @click.prevent="navigateTo(item.path)"
                >
                  {{ item.icon }} {{ item.label }}
                </a>
              </li>
              <li>
                <a :href="githubUrl" target="_blank" rel="noopener" class="mobile-nav-item">
                  <svg class="github-icon" viewBox="0 0 16 16" width="20" height="20" fill="currentColor">
                    <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
                  </svg>
                  GitHub
                </a>
              </li>
            </ul>
            <div class="mobile-menu-footer">
              <button v-if="userStore.isAuthenticated" class="btn-logout" @click="handleLogout">
                退出登录
              </button>
              <button v-else class="btn-login-mobile" @click="handleLoginClick(); closeMobileMenu()">
                登录
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </nav>

    <!-- ========== 层级4：内容插槽层 ========== -->
    <main class="content-wrapper" :class="{ 'content-wrapper-fullbleed': isFullBleedRoute }">
      <div class="content-container" :class="{ 'content-container-fullbleed': isFullBleedRoute }">
        <router-view />
      </div>
    </main>


    <!-- 登录弹窗 -->
    <LoginModal :visible="showLoginModal" @close="showLoginModal = false" @success="handleLoginSuccess" />

    <!-- 个人资料弹窗 -->
    <UserProfilePanel :visible="showProfilePanel" @close="showProfilePanel = false" />
  </div>
</template>

<style scoped>
/* ========== 最外层容器 ========== */
.app-wrapper {
  position: relative;
  min-height: 100vh;
  width: 100%;
  overflow-x: hidden;
}

/* ========== 层级1：浅色背景 ========== */
.bg-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: var(--z-video-bg);
  pointer-events: none;
}

.bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bg-tint {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(248, 250, 253, 0.85) 0%,
    rgba(241, 245, 249, 0.75) 50%,
    rgba(248, 250, 253, 0.9) 100%
  );
}

/* ========== 层级3：玻璃态导航栏 ========== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: var(--navbar-height);
  z-index: var(--z-navbar);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(var(--blur-amount));
  -webkit-backdrop-filter: blur(var(--blur-amount));
  border-bottom: 1px solid var(--glass-border);
  transition: background var(--transition-normal);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.navbar:hover {
  background: rgba(255, 255, 255, 0.88);
}

.navbar-content {
  max-width: 100%;
  height: 100%;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Logo 样式 */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  cursor: pointer;
}

.logo-icon {
  font-size: 28px;
  transition: transform var(--transition-normal);
}

.logo:hover .logo-icon {
  transform: rotate(15deg) scale(1.1);
}

/* 导航链接 */
.nav-links {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 8px;
}

.nav-item {
  position: relative;
  padding: 8px 16px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: color var(--transition-normal);
}

.nav-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: var(--accent-cyan);
  transition: all var(--transition-normal);
  transform: translateX(-50%);
}

.nav-item:hover,
.nav-item.active {
  color: var(--text-primary);
}

.nav-item:hover::after,
.nav-item.active::after {
  width: 80%;
}

/* GitHub 链接样式 */
.github-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--glass-border);
  background: rgba(0, 0, 0, 0.04);
  transition: all var(--transition-fast);
}

.github-link:hover {
  background: rgba(0, 0, 0, 0.08);
  border-color: rgba(0, 0, 0, 0.12);
}

.github-link::after {
  display: none;
}

.github-icon {
  flex-shrink: 0;
}

.github-label {
  font-size: 13px;
  font-weight: 600;
}

/* 用户区域 */
.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
  border-radius: 999px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  background: rgba(255, 255, 255, 0.6);
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-trigger:hover {
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.12);
  transform: translateY(-1px);
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 2px solid var(--accent-cyan);
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-action {
  font-size: 12px;
  color: rgba(161, 199, 235, 0.82);
}

/* 登录按钮 */
.btn-login {
  padding: 8px 20px;
  background: transparent;
  border: 1px solid var(--accent-cyan);
  border-radius: 8px;
  color: var(--accent-cyan);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.btn-login:hover {
  background: var(--accent-cyan);
  color: #fff;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.25);
}

/* ========== 手机端汉堡菜单按钮 ========== */
.mobile-menu-btn {
  display: none;
  background: transparent;
  border: none;
  padding: 8px;
  cursor: pointer;
  z-index: 200;
}

.hamburger {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 24px;
}

.hamburger span {
  display: block;
  width: 100%;
  height: 2px;
  background: var(--text-primary);
  border-radius: 2px;
  transition: all 0.3s ease;
}

.hamburger.is-active span:nth-child(1) {
  transform: rotate(45deg) translate(5px, 5px);
}

.hamburger.is-active span:nth-child(2) {
  opacity: 0;
}

.hamburger.is-active span:nth-child(3) {
  transform: rotate(-45deg) translate(5px, -5px);
}

/* ========== 手机端侧边菜单 ========== */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  z-index: 150;
  backdrop-filter: blur(4px);
}

.mobile-menu {
  position: absolute;
  top: 0;
  right: 0;
  width: 280px;
  height: 100%;
  background: #fff;
  border-left: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  padding: 20px;
  box-sizing: border-box;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.08);
}

.mobile-menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--glass-border);
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
}

.mobile-nav-links {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 12px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.mobile-nav-item:hover,
.mobile-nav-item.active {
  color: var(--text-primary);
  background: rgba(59, 130, 246, 0.08);
}

.mobile-menu-footer {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid var(--glass-border);
}

.btn-logout {
  width: 100%;
  padding: 12px;
  background: transparent;
  border: 1px solid var(--accent-red);
  border-radius: 8px;
  color: var(--accent-red);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-logout:hover {
  background: var(--accent-red);
}

.btn-login-mobile {
  width: 100%;
  padding: 12px;
  background: transparent;
  border: 1px solid var(--accent-cyan);
  border-radius: 8px;
  color: var(--accent-cyan);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-login-mobile:hover {
  background: var(--accent-cyan);
  color: #fff;
}

/* 菜单动画 */
.slide-menu-enter-active,
.slide-menu-leave-active {
  transition: opacity 0.3s ease;
}

.slide-menu-enter-active .mobile-menu,
.slide-menu-leave-active .mobile-menu {
  transition: transform 0.3s ease;
}

.slide-menu-enter-from,
.slide-menu-leave-to {
  opacity: 0;
}

.slide-menu-enter-from .mobile-menu,
.slide-menu-leave-to .mobile-menu {
  transform: translateX(100%);
}

/* ========== 层级4：内容区 ========== */
.content-wrapper {
  position: relative;
  z-index: var(--z-content);
  min-height: 100vh;
  padding-top: var(--navbar-height);
}

.content-container {
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: 40px 24px;
}

.content-wrapper-fullbleed {
  min-height: calc(100vh - var(--navbar-height));
}

.content-container-fullbleed {
  max-width: none;
  margin: 0;
  padding: 0;
}


/* ========== 响应式适配 ========== */
@media (max-width: 960px) {
  .nav-links {
    display: none;
  }

  .navbar-content {
    padding: 0 16px;
  }

  .user-action {
    display: none;
  }

  .user-name {
    max-width: 100px;
  }

  .mobile-menu-btn {
    display: block;
  }
}

@media (max-width: 768px) {
  .navbar-content {
    padding: 0 14px;
  }

  .logo {
    font-size: 18px;
    gap: 8px;
  }

  .logo-icon {
    font-size: 26px;
  }

  .user-trigger {
    padding: 4px 8px 4px 4px;
    gap: 8px;
  }

  .user-avatar {
    width: 30px;
    height: 30px;
  }

  .user-name {
    font-size: 13px;
    max-width: 80px;
  }

  .btn-login {
    padding: 8px 16px;
    font-size: 14px;
  }

  .content-container {
    padding: 20px 16px;
  }

  .mobile-menu {
    width: 280px;
  }
}

@media (max-width: 520px) {
  .navbar {
    height: calc(var(--navbar-height) + var(--safe-area-top));
    padding-top: var(--safe-area-top);
  }

  .navbar-content {
    padding: 0 12px;
  }

  .logo-text {
    display: none;
  }

  .logo-icon {
    font-size: 28px;
  }

  .user-name {
    display: none;
  }

  .content-container {
    padding: 16px 12px;
    padding-bottom: calc(16px + var(--safe-area-bottom));
  }

  .mobile-menu {
    width: 100%;
    padding-top: calc(20px + var(--safe-area-top));
    padding-bottom: calc(20px + var(--safe-area-bottom));
  }

  .mobile-nav-item {
    padding: 18px 16px;
    font-size: 17px;
    min-height: 56px;
  }

  .close-btn {
    min-width: 48px;
    min-height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .btn-login {
    min-height: 44px;
    min-width: 70px;
  }

  .btn-logout {
    min-height: 48px;
    font-size: 15px;
  }
}
</style>
