/**
 * router/index.ts - 峡谷卫星路由配置
 *
 * 引入 MainLayout 作为顶层布局容器
 * HomeView 作为首页内容
 * HolyLandTourView 作为圣地巡游页面
 */
import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import HomeView from '@/views/HomeView.vue'
import HolyLandTourView from '@/views/HolyLandTourView.vue'
import AIConsultView from '@/views/AIConsultView.vue'

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
          component: HomeView
        },
        {
          path: 'holy-land-tour',
          name: 'holy-land-tour',
          component: HolyLandTourView
        },
        {
          path: 'ai-consult',
          name: 'ai-consult',
          component: AIConsultView
        }
      ]
    }
  ]
})

export default router
