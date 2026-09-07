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

const isFullBleedRoute = computed(() => route.name === 'route-map' || route.name === 'holy-land-tour')

const isMobileMenuOpen = ref(false)
const showLoginModal = ref(false)

const showProfilePanel = ref(false)

const userName = computed(() => userStore.displayName)
const userAvatar = computed(() => userStore.displayAvatar)

// 当前路由名称，用于高亮导航项
const currentRouteName = computed(() => route.name)

// 导航项配置
const navItems = [
  { name: 'home', label: '行程定制', icon: '🏠', path: '/' },
  { name: 'route-map', label: '3D 智能导览', icon: '🗺️', path: '/route-map' },
  { name: 'ai-consult', label: 'AI 伴游助手', icon: '💬', path: '/ai-consult' },
  { name: 'admin', label: '管理工作台', icon: '⚙️', path: '/admin' },
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

const handleUnauthorized = () => {
  userStore.logout()
  showLoginModal.value = true
}

const handleOpenProfile = () => {
  showProfilePanel.value = true
}

const handleOpenLogin = () => {
  showLoginModal.value = true
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isMobileMenuOpen.value) {
    closeMobileMenu()
  }
}

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('auth:unauthorized', handleUnauthorized)
  window.removeEventListener('auth:open-profile', handleOpenProfile)
  window.removeEventListener('auth:open-login', handleOpenLogin)
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
  if (city) {
    await holyTourStore.fetchCitySpots(city, userId)
  }
}

const handleLoginSuccess = () => {
  showLoginModal.value = false
  void preloadHolyTour()
}

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
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('auth:unauthorized', handleUnauthorized)
  window.addEventListener('auth:open-profile', handleOpenProfile)
  window.addEventListener('auth:open-login', handleOpenLogin)
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
        <router-link to="/" class="logo" title="灵境导览 · 衡阳文旅智能体">
          <span class="logo-icon">🧭</span>
          <span class="logo-text">灵境导览</span>
          <span class="logo-tag">衡阳文旅智能体</span>
        </router-link>

        <!-- 桌面端导航项 -->
        <ul class="nav-links">
          <li v-for="item in navItems" :key="item.name">
            <router-link
              :to="item.path"
              class="nav-item"
              :class="{ active: isActiveNav(item.name) }"
            >
              {{ item.label }}
            </router-link>
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
          <button class="mobile-menu-btn" @click="toggleMobileMenu" aria-label="打开菜单">
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
              <router-link to="/" class="logo" @click="closeMobileMenu" title="灵境导览 · 衡阳文旅智能体">
                <span class="logo-icon">🧭</span>
                <span class="logo-text">灵境导览</span>
                <span class="logo-tag">衡阳文旅智能体</span>
              </router-link>
              <button class="close-btn" aria-label="关闭菜单" @click="closeMobileMenu">✕</button>
            </div>
            <ul class="mobile-nav-links">
              <li v-for="item in navItems" :key="item.name">
                <router-link
                  :to="item.path"
                  class="mobile-nav-item"
                  :class="{ active: isActiveNav(item.name) }"
                  @click="closeMobileMenu"
                >
                  {{ item.icon }} {{ item.label }}
                </router-link>
              </li>
              <li>
                <a :href="githubUrl" target="_blank" rel="noopener" class="mobile-nav-item" @click="closeMobileMenu">
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
  background: radial-gradient(circle at 50% 20%, var(--color-primary-muted, rgba(6, 182, 212, 0.08)) 0%, var(--surface-ground, #070B14) 75%);
}

/* ========== 层级3：玻璃态导航栏 ========== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: calc(var(--navbar-height) + var(--safe-area-top));
  padding-top: var(--safe-area-top);
  padding-left: var(--safe-area-left);
  padding-right: var(--safe-area-right);
  box-sizing: border-box;
  z-index: var(--z-navbar);
  background: var(--surface-card-glass, rgba(15, 23, 42, 0.85));
  backdrop-filter: blur(var(--blur-amount));
  -webkit-backdrop-filter: blur(var(--blur-amount));
  border-bottom: 1px solid var(--border-subtle);
  transition: background var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.navbar:hover {
  background: var(--surface-card, #0F172A);
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
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  text-decoration: none;
  cursor: pointer;
  min-height: 44px;
}

.logo-icon {
  font-size: 28px;
  transition: transform var(--transition-normal);
}

.logo:hover .logo-icon {
  transform: rotate(15deg) scale(1.1);
}

.logo-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: var(--border-radius-full);
  background: var(--color-primary-muted);
  color: var(--color-primary);
  border: 1px solid var(--border-default);
  font-weight: 600;
  letter-spacing: 0.04em;
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
  display: inline-flex;
  align-items: center;
  padding: 0 16px;
  min-height: 44px;
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
  bottom: 6px;
  left: 50%;
  width: 0;
  height: 2px;
  background: var(--color-primary);
  transition: all var(--transition-normal);
  transform: translateX(-50%);
}

.nav-item:hover,
.nav-item.active {
  color: var(--text-primary);
}

.nav-item:hover::after,
.nav-item.active::after {
  width: 70%;
}

/* GitHub 链接样式 */
.github-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  min-height: 44px;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--border-subtle);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.github-link:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: var(--border-default);
  color: var(--text-primary);
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
  padding: 4px 12px 4px 4px;
  min-height: 44px;
  border-radius: var(--border-radius-full);
  border: 1px solid var(--border-default);
  background: var(--surface-card-glass, rgba(15, 23, 42, 0.75));
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-trigger:hover {
  border-color: var(--color-primary);
  box-shadow: 0 0 12px var(--color-primary-glow);
  transform: translateY(-1px);
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 2px solid var(--color-primary);
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
  color: var(--color-primary);
  font-weight: 500;
}

/* 登录按钮 */
.btn-login {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  min-height: 44px;
  background: transparent;
  border: 1px solid var(--color-primary);
  border-radius: var(--border-radius-sm);
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.btn-login:hover {
  background: var(--color-primary);
  color: var(--text-inverse);
  box-shadow: 0 0 16px var(--color-primary-glow);
}

/* ========== 手机端汉堡菜单按钮 ========== */
.mobile-menu-btn {
  display: none;
  background: transparent;
  border: none;
  padding: 8px;
  min-width: 44px;
  min-height: 44px;
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
  inset: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  z-index: var(--z-mobile-menu);
  backdrop-filter: blur(4px);
  overscroll-behavior: contain;
}

.mobile-menu {
  position: absolute;
  top: 0;
  right: 0;
  width: 280px;
  height: 100%;
  background: var(--surface-modal, #0F172A);
  color: var(--text-primary);
  border-left: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  padding: 20px;
  padding-top: calc(20px + var(--safe-area-top));
  padding-bottom: calc(20px + var(--safe-area-bottom));
  padding-left: calc(20px + var(--safe-area-left));
  padding-right: calc(20px + var(--safe-area-right));
  box-sizing: border-box;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.4);
  overscroll-behavior: contain;
}

.mobile-menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 22px;
  cursor: pointer;
  min-width: 44px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--border-radius-xs);
  transition: background var(--transition-fast);
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
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
  padding: 12px 16px;
  min-height: 48px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 16px;
  font-weight: 500;
  border-radius: var(--border-radius-sm);
  transition: all 0.2s ease;
}

.mobile-nav-item:hover,
.mobile-nav-item.active {
  color: var(--text-primary);
  background: var(--color-primary-muted);
}

.mobile-menu-footer {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
}

.btn-logout {
  width: 100%;
  padding: 12px;
  min-height: 44px;
  background: transparent;
  border: 1px solid var(--color-danger);
  border-radius: var(--border-radius-sm);
  color: var(--color-danger);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-logout:hover {
  background: var(--color-danger);
  color: var(--text-primary, #ffffff);
}

.btn-login-mobile {
  width: 100%;
  padding: 12px;
  min-height: 44px;
  background: transparent;
  border: 1px solid var(--color-primary);
  border-radius: var(--border-radius-sm);
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-login-mobile:hover {
  background: var(--color-primary);
  color: var(--text-inverse);
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
  padding-top: calc(var(--navbar-height) + var(--safe-area-top));
  padding-bottom: var(--safe-area-bottom);
  padding-left: var(--safe-area-left);
  padding-right: var(--safe-area-right);
  box-sizing: border-box;
}

.content-container {
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: 40px 24px;
}

.content-wrapper-fullbleed {
  min-height: calc(100vh - var(--navbar-height) - var(--safe-area-top));
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
    padding-top: calc(16px + var(--safe-area-top));
    padding-bottom: calc(16px + var(--safe-area-bottom));
    padding-left: calc(12px + var(--safe-area-left));
    padding-right: calc(12px + var(--safe-area-right));
  }

  .mobile-menu {
    width: 100%;
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
