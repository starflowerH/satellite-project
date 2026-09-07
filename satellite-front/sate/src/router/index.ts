/**
 * router/index.ts - 灵境导览路由配置
 *
 * 引入 MainLayout 作为顶层布局容器
 * - / : 首页 4 步向导卡片定制 (HomeView)
 * - /route-map : 3D 智能导览与时间轴卡片 (RouteMapView / HolyLandTourView)
 * - /ai-consult : AI 伴游助手 (AIConsultView)
 * - /admin : 商户与招牌菜品管理工作台 (AdminView)
 */
import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import HomeView from '@/views/HomeView.vue'
import RouteMapView from '@/views/RouteMapView.vue'
import AIConsultView from '@/views/AIConsultView.vue'
import AdminView from '@/views/AdminView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView,
        },
        {
          path: 'route-map',
          name: 'route-map',
          component: RouteMapView,
        },
        {
          path: 'holy-land-tour',
          redirect: '/route-map',
        },
        {
          path: 'ai-consult',
          name: 'ai-consult',
          component: AIConsultView,
        },
        {
          path: 'admin',
          name: 'admin',
          component: AdminView,
        },
        {
          path: ':pathMatch(.*)*',
          name: 'not-found',
          redirect: '/',
        },
      ],
    },
  ],
})

export default router
