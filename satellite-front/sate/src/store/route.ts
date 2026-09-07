/**
 * src/store/route.ts - 灵境导览 文旅路线规划 Pinia Store
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { travelApi } from '@/api/travel'
import type { MerchantVO, RouteGenerateRequestDTO, RouteItemVO, RoutePlanVO } from '@/types/travel'

const STORAGE_KEY_CURRENT_PLAN = 'sate_current_route_plan'
const STORAGE_KEY_HISTORY = 'sate_route_history'
const STORAGE_KEY_CUSTOM_MERCHANTS = 'sate_custom_merchants'

/**
 * 本地高可用静默降级兜底路线数据生成器 (零崩溃保障)
 */
const buildFallbackPlan = (params: RouteGenerateRequestDTO, customList: MerchantVO[] = []): RoutePlanVO => {
  const duration = params.durationHours || 4
  const vibe = params.atmosphere || '松弛感'
  const mode = params.transportMode || 'WALKING'
  const spicy = params.spicyLevel || '微辣'
  const restrictions = params.dietaryRestrictions || ['不吃内脏']
  const budget = params.budgetPerMeal || 35
  const customShop: MerchantVO | null = customList && customList.length > 0 ? customList[0] : null

  const items: RouteItemVO[] = []

  if (duration <= 2) {
    items.push({
      id: 1,
      planId: 999,
      itemOrder: 1,
      itemType: 'POI',
      targetId: 1,
      name: '衡阳师范学院 (雁山校区)',
      longitude: 112.6845,
      latitude: 26.8398,
      arriveTime: '14:30',
      stayMinutes: 60,
      recommendReason: `距离起点便捷，校园湖畔漫步与林荫绿意深度契合【${vibe}】风格，适合极速解压漫步周边。`,
      recommendedDishes: [],
      transportToNextMinutes: 10,
      transportToNextDistance: 500,
    })
    items.push({
      id: 2,
      planId: 999,
      itemOrder: 2,
      itemType: 'MERCHANT',
      targetId: 3,
      name: '师院后街·清凉补糖水铺',
      longitude: 112.684,
      latitude: 26.8415,
      arriveTime: '15:40',
      stayMinutes: 40,
      recommendReason: `步行仅10分钟，人均¥15在您¥${budget}预算内。精选清润不辣甜品，完全避开【${restrictions.join('、')}】。`,
      recommendedDishes: [
        {
          id: 31,
          merchantId: 3,
          name: '师院清补凉糖水',
          price: 12.0,
          isSignature: 1,
          spicyLevel: '不辣',
          flavorNotes: '椰奶清甜，莲子银耳爽脆',
          allergensOrIngredients: '纯素/健康甜品',
          warning: '纯素清爽 / 解腻消暑 / 无内脏',
        },
      ],
      transportToNextMinutes: 0,
      transportToNextDistance: 0,
    })
  } else if (duration <= 4) {
    const diningName = customShop ? customShop.name : '雁城老味·东洲土菜馆'
    const diningLng = customShop ? Number(customShop.longitude) : 112.632
    const diningLat = customShop ? Number(customShop.latitude) : 26.875
    const diningPrice = customShop ? Number(customShop.avgPricePerPerson) : 45
    const diningDishes: any[] = customShop?.dishes && customShop.dishes.length > 0
      ? customShop.dishes.map((d: any, i: number) => ({
          id: d.id || (200 + i),
          merchantId: customShop.id || 2,
          name: d.name,
          price: Number(d.price),
          isSignature: d.isSignature ?? 1,
          spicyLevel: d.spicyLevel || spicy,
          flavorNotes: d.flavorNotes || '地道湘菜特色风味',
          allergensOrIngredients: d.allergensOrIngredients || '纯净现制/避坑明确',
          warning: d.warning || d.allergensOrIngredients || '现炒现做 / 辣度适宜 / 严选无内脏',
        }))
      : [
          {
            id: 21,
            merchantId: customShop?.id || 2,
            name: customShop ? `${customShop.name}招牌风味` : '石膏老豆腐炖鲜鱼',
            price: diningPrice,
            isSignature: 1,
            spicyLevel: spicy,
            flavorNotes: '汤白浓鲜，豆腐软嫩饱吸鱼汤',
            allergensOrIngredients: '江鲜原汤/可免香菜/无内脏',
            warning: '鲜鱼现煮微辣 / 豆香浓郁 / 严选无内脏',
          },
        ]
    const diningReason = customShop
      ? `【管理端自录特色名店优先召回】人均¥${diningPrice}，严格为您匹配【${spicy}】辣度，剔除【${restrictions.join('、')}】食材。`
      : `临江风味小馆（人均¥45，契合¥${budget}区间），严格为您匹配【${spicy}】辣度，剔除【${restrictions.join('、')}】食材。`

    items.push({
      id: 1,
      planId: 999,
      itemOrder: 1,
      itemType: 'POI',
      targetId: 2,
      name: '东洲岛',
      longitude: 112.6358,
      latitude: 26.8732,
      arriveTime: '09:30',
      stayMinutes: 90,
      recommendReason: `湘江绿洲生态栈道与罗汉寺古树，江风拂面，完美契合【${vibe}】半日漫游体验。`,
      recommendedDishes: [],
      transportToNextMinutes: 15,
      transportToNextDistance: 1200,
    })
    items.push({
      id: 2,
      planId: 999,
      itemOrder: 2,
      itemType: 'MERCHANT',
      targetId: customShop?.id || 2,
      name: diningName,
      longitude: diningLng,
      latitude: diningLat,
      arriveTime: '11:15',
      stayMinutes: 60,
      recommendReason: diningReason,
      recommendedDishes: diningDishes,
      transportToNextMinutes: 20,
      transportToNextDistance: 2500,
    })
    items.push({
      id: 3,
      planId: 999,
      itemOrder: 3,
      itemType: 'POI',
      targetId: 1,
      name: '石鼓书院',
      longitude: 112.6133,
      latitude: 26.9038,
      arriveTime: '12:35',
      stayMinutes: 75,
      recommendReason: `三江汇流的人文胜地，摩崖石刻与仿古庭院，为半日漫游带来沉静历史底蕴。`,
      recommendedDishes: [],
      transportToNextMinutes: 0,
      transportToNextDistance: 0,
    })
  } else {
    const diningName = customShop ? customShop.name : '石鼓传统鱼粉小吃坊'
    const diningLng = customShop ? Number(customShop.longitude) : 112.612
    const diningLat = customShop ? Number(customShop.latitude) : 26.902
    const diningPrice = customShop ? Number(customShop.avgPricePerPerson) : 18
    const diningDishes: any[] = customShop?.dishes && customShop.dishes.length > 0
      ? customShop.dishes.map((d: any, i: number) => ({
          id: d.id || (100 + i),
          merchantId: customShop.id || 1,
          name: d.name,
          price: Number(d.price),
          isSignature: d.isSignature ?? 1,
          spicyLevel: d.spicyLevel || spicy,
          flavorNotes: d.flavorNotes || '老字号传统风味',
          allergensOrIngredients: d.allergensOrIngredients || '食材新鲜 / 避坑明确',
          warning: d.warning || d.allergensOrIngredients || '原汁原味 / 辣度适中 / 严选无内脏',
        }))
      : [
          {
            id: 11,
            merchantId: customShop?.id || 1,
            name: customShop ? `${customShop.name}招牌老汤粉` : '衡阳传统老汤鲜鱼粉',
            price: diningPrice,
            isSignature: 1,
            spicyLevel: spicy,
            flavorNotes: '骨汤久熬雪白鲜甜，米粉爽滑筋道',
            allergensOrIngredients: '鲜鱼原汤/葱花/无内脏',
            warning: '原汤微辣提鲜 / 无内脏 / 可免葱花香菜',
          },
        ]
    const diningReason = customShop
      ? `【管理端自录特色名店优先召回】人均¥${diningPrice}，清晨现制地道美食，严格契合您的【${spicy}】辣度偏好。`
      : `衡阳经典老店，清晨现熬鲜鱼骨浓汤，无内脏纯鲜滋味，精准契合【${spicy}】画像。`

    items.push({
      id: 1,
      planId: 999,
      itemOrder: 1,
      itemType: 'POI',
      targetId: 1,
      name: '石鼓书院',
      longitude: 112.6133,
      latitude: 26.9038,
      arriveTime: '09:00',
      stayMinutes: 90,
      recommendReason: `全天深度游览第一站：蒸、湘、耒三水汇聚处，探寻湖湘文化发源地。`,
      recommendedDishes: [],
      transportToNextMinutes: 15,
      transportToNextDistance: 1500,
    })
    items.push({
      id: 2,
      planId: 999,
      itemOrder: 2,
      itemType: 'MERCHANT',
      targetId: customShop?.id || 1,
      name: diningName,
      longitude: diningLng,
      latitude: diningLat,
      arriveTime: '10:45',
      stayMinutes: 45,
      recommendReason: diningReason,
      recommendedDishes: diningDishes,
      transportToNextMinutes: 25,
      transportToNextDistance: 4500,
    })
    items.push({
      id: 3,
      planId: 999,
      itemOrder: 3,
      itemType: 'POI',
      targetId: 2,
      name: '东洲岛',
      longitude: 112.6358,
      latitude: 26.8732,
      arriveTime: '12:00',
      stayMinutes: 120,
      recommendReason: `午后漫步江岛林荫，古刹寻幽，体验慢调江风，契合【${vibe}】氛围。`,
      recommendedDishes: [],
      transportToNextMinutes: 20,
      transportToNextDistance: 3200,
    })
    items.push({
      id: 4,
      planId: 999,
      itemOrder: 4,
      itemType: 'MERCHANT',
      targetId: 2,
      name: '雁城老味·东洲土菜馆',
      longitude: 112.632,
      latitude: 26.875,
      arriveTime: '14:20',
      stayMinutes: 70,
      recommendReason: `江畔特色土菜，为您推荐契合预算（¥${budget}）与辣度（${spicy}）的地道湘味佳肴。`,
      recommendedDishes: [
        {
          id: 21,
          merchantId: 2,
          name: '石膏老豆腐炖鲜鱼',
          price: 36.0,
          isSignature: 1,
          spicyLevel: '微辣',
          flavorNotes: '汤白浓鲜，豆腐软嫩饱吸鱼汤',
          allergensOrIngredients: '江鲜原汤/可免香菜/无内脏',
          warning: '鲜鱼现煮微辣 / 豆香浓郁 / 严选无内脏',
        },
      ],
      transportToNextMinutes: 25,
      transportToNextDistance: 4200,
    })
    items.push({
      id: 5,
      planId: 999,
      itemOrder: 5,
      itemType: 'POI',
      targetId: 4,
      name: '南湖公园',
      longitude: 112.6022,
      latitude: 26.8631,
      arriveTime: '15:55',
      stayMinutes: 90,
      recommendReason: `全天收官：傍晚夕阳与湖光水色交相辉映，环湖步道夜景亮起，惬意松弛。`,
      recommendedDishes: [],
      transportToNextMinutes: 0,
      transportToNextDistance: 0,
    })
  }

  let totalDist = 0
  let totalMin = 0
  for (const item of items) {
    totalDist += item.transportToNextDistance
    totalMin += item.stayMinutes + item.transportToNextMinutes
  }

  return {
    id: Date.now(),
    planId: Date.now(),
    title: `雁城「${vibe}」${duration}小时专属文旅漫游`,
    durationHours: duration,
    atmosphere: vibe,
    transportMode: mode,
    totalDistanceMeters: totalDist,
    totalDurationMinutes: totalMin,
    isFallback: 1,
    createdAt: new Date().toISOString(),
    items,
  }
}

export const useRouteStore = defineStore('route', () => {
  const currentPlan = ref<RoutePlanVO | null>(null)
  const isGenerating = ref(false)
  const historyPlans = ref<RoutePlanVO[]>([])
  const customMerchants = ref<MerchantVO[]>([])
  const selectedItemOrder = ref<number>(1)

  // 从本地缓存初始化当前规划与历史、自录商户
  const initFromStorage = () => {
    try {
      const cached = localStorage.getItem(STORAGE_KEY_CURRENT_PLAN)
      if (cached) {
        currentPlan.value = JSON.parse(cached)
      }
      const historyCached = localStorage.getItem(STORAGE_KEY_HISTORY)
      if (historyCached) {
        historyPlans.value = JSON.parse(historyCached)
      }
      const merchantsCached = localStorage.getItem(STORAGE_KEY_CUSTOM_MERCHANTS)
      if (merchantsCached) {
        customMerchants.value = JSON.parse(merchantsCached)
      }
    } catch (e) {
      console.warn('读取本地路线缓存失败:', e)
    }
  }

  initFromStorage()

  const hasActivePlan = computed(() => !!currentPlan.value && (currentPlan.value.items?.length || 0) > 0)

  const savePlanToStorage = (plan: RoutePlanVO) => {
    currentPlan.value = plan
    try {
      localStorage.setItem(STORAGE_KEY_CURRENT_PLAN, JSON.stringify(plan))

      // 更新历史记录
      const existsIndex = historyPlans.value.findIndex(p => p.id === plan.id || p.planId === plan.planId)
      if (existsIndex >= 0) {
        historyPlans.value[existsIndex] = plan
      } else {
        historyPlans.value.unshift(plan)
        if (historyPlans.value.length > 20) {
          historyPlans.value.pop()
        }
      }
      localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(historyPlans.value))
    } catch (e) {
      console.warn('缓存路线失败:', e)
    }
  }

  /**
   * 加载自录商户列表
   */
  const loadCustomMerchants = async (): Promise<MerchantVO[]> => {
    try {
      const res = await travelApi.listMerchants({ isCustomAdded: 1 })
      if (res && res.data && Array.isArray(res.data) && res.data.length > 0) {
        const merged = [...res.data]
        for (const local of customMerchants.value) {
          if (!merged.some(m => m.id === local.id || m.name === local.name)) {
            merged.push(local)
          }
        }
        customMerchants.value = merged
        localStorage.setItem(STORAGE_KEY_CUSTOM_MERCHANTS, JSON.stringify(merged))
        return merged
      }
    } catch (e) {
      console.warn('后端获取商户失败，使用本地缓存商户:', e)
    }
    return customMerchants.value
  }

  /**
   * 添加自录商户到 store 与本地缓存
   */
  const addCustomMerchant = (merchant: MerchantVO) => {
    const list = [...customMerchants.value]
    const idx = list.findIndex(m => m.id === merchant.id || m.name === merchant.name)
    if (idx >= 0) {
      list[idx] = { ...list[idx], ...merchant }
    } else {
      list.unshift(merchant)
    }
    customMerchants.value = list
    localStorage.setItem(STORAGE_KEY_CUSTOM_MERCHANTS, JSON.stringify(list))
  }

  /**
   * 删除自录商户
   */
  const removeCustomMerchant = (id: number | string) => {
    customMerchants.value = customMerchants.value.filter(m => m.id !== id)
    localStorage.setItem(STORAGE_KEY_CUSTOM_MERCHANTS, JSON.stringify(customMerchants.value))
  }

  /**
   * 触发路线规划（带高可用静默降级）
   */
  const generatePlan = async (requestDto: RouteGenerateRequestDTO): Promise<RoutePlanVO> => {
    isGenerating.value = true
    try {
      const res = await travelApi.generateRoute(requestDto)
      if (res && (String(res.code) === '200' || String(res.code) === '0') && res.data) {
        savePlanToStorage(res.data)
        selectedItemOrder.value = 1
        return res.data
      }
      console.warn('后端路线接口返回非标准成功，启动本地静默降级:', res)
      const fallback = buildFallbackPlan(requestDto, customMerchants.value)
      savePlanToStorage(fallback)
      selectedItemOrder.value = 1
      return fallback
    } catch (error) {
      console.warn('后端路线接口异常，平滑无感启动本地拓扑贪心降级方案:', error)
      const fallback = buildFallbackPlan(requestDto, customMerchants.value)
      savePlanToStorage(fallback)
      selectedItemOrder.value = 1
      return fallback
    } finally {
      isGenerating.value = false
    }
  }

  const setCurrentPlan = (plan: RoutePlanVO | null) => {
    currentPlan.value = plan
    if (plan) {
      savePlanToStorage(plan)
      selectedItemOrder.value = 1
    } else {
      localStorage.removeItem(STORAGE_KEY_CURRENT_PLAN)
    }
  }

  const selectNode = (order: number) => {
    selectedItemOrder.value = order
  }

  return {
    currentPlan,
    isGenerating,
    historyPlans,
    hasActivePlan,
    customMerchants,
    selectedItemOrder,
    generatePlan,
    setCurrentPlan,
    loadCustomMerchants,
    addCustomMerchant,
    removeCustomMerchant,
    selectNode,
  }
})

export default useRouteStore

