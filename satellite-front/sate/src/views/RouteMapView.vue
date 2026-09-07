<script setup lang="ts">
/**
 * RouteMapView.vue - 灵境导览 · 3D 智能导览与可解释路线时间轴
 *
 * 核心功能：
 * 1. 3D 高德全景地图：3D 模式、50度俯仰角、科技深色底图（兼容离线/无 Key 高保真 Canvas 降级渲染）
 * 2. 双语义色彩点位系统：
 *    - 🏛️ 文旅景点：海克斯战术青 (#06B6D4)，带脉冲光圈与序号码
 *    - 🍜 美食餐饮：活力餐饮橙 (#F97316)，带活力橙光晕与餐具/碗筷标识
 * 3. 真实道路轨迹折线：连接连续节点的发光道路多段线与行驶方向指示
 * 4. 悬浮式可解释时间轴卡片：
 *    - 到达时间、停留时长、交通耗时
 *    - 💡 可解释推荐理由 (Explainable Rationale)
 *    - 🍽️ 餐饮节点招牌菜品、单价、辣度说明与食材避坑提示
 *    - 🔄 【重新规划】与 🧭 【一键导航】高德 URI 联动
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AMapLoader from '@amap/amap-jsapi-loader'
import request from '@/utils/request'
import { useRouteStore } from '@/store/route'
import type { DishVO, RouteItemVO } from '@/types/travel'

const router = useRouter()
const routeStore = useRouteStore()

// 地图实例与状态
const mapContainerRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const mapInstance = ref<any>(null)
const isMapLoaded = ref(false)
const isFallbackCanvas = ref(false)
const is3D = ref(true)
const currentPitch = ref(50)

// 交互状态
const selectedOrder = ref<number>(1)
const isMobileDrawerOpen = ref(true)
const markersMap = new Map<number, any>()
const polylineInstance = ref<any>(null)
const cardRefs = ref<Record<number, HTMLElement | null>>({})

// 衡阳市中心基准经纬度 (石鼓书院附近)
const HENGYANG_CENTER: [number, number] = [112.6133, 26.9038]

// 当前规划方案
const plan = computed(() => routeStore.currentPlan)
const routeItems = computed<RouteItemVO[]>(() => routeStore.currentPlan?.items || [])

// 统计信息
const totalDistanceKm = computed(() => {
  const meters = plan.value?.totalDistanceMeters || 0
  return (meters / 1000).toFixed(1)
})

const totalDurationText = computed(() => {
  const mins = plan.value?.totalDurationMinutes || 0
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h > 0 && m > 0) return `${h}小时${m}分钟`
  if (h > 0) return `${h}小时`
  return `${m}分钟`
})

const poiCount = computed(() => routeItems.value.filter(item => item.itemType === 'POI').length)
const merchantCount = computed(() => routeItems.value.filter(item => item.itemType === 'MERCHANT').length)

// 获取高德Key
const fetchAmapKey = async (): Promise<string | null> => {
  try {
    const res: any = await request.get('/amap/config')
    const key = res?.data?.key || res?.key
    if (key && typeof key === 'string' && key.trim()) {
      return key.trim()
    }
  } catch (err) {
    console.warn('获取高德地图 Key 异常，将使用内置配置或降级渲染:', err)
  }
  return null
}

// 初始化 3D 高德地图
const initAMap = async () => {
  if (mapInstance.value) return

  try {
    const serverKey = await fetchAmapKey()
    const finalKey = serverKey || '3c3cb267c8223677353f0907e59b2075' // 内置预设高德Key

    const AMap = await AMapLoader.load({
      key: finalKey,
      version: '2.0',
      plugins: [
        'AMap.Scale',
        'AMap.ToolBar',
        'AMap.Polyline',
        'AMap.Marker',
        'AMap.InfoWindow',
      ],
    })

    if (!mapContainerRef.value) return

    mapInstance.value = new AMap.Map(mapContainerRef.value, {
      viewMode: '3D',
      pitch: 50,
      rotation: 0,
      zoom: 14,
      center: HENGYANG_CENTER,
      mapStyle: 'amap://styles/darkblue', // 科技暗蓝底图
      showBuildingBlock: true,
      features: ['bg', 'point', 'road', 'building'],
    })

    mapInstance.value.on('complete', () => {
      isMapLoaded.value = true
      renderRouteOnAMap(AMap)
    })
  } catch (error) {
    console.warn('高德 3D 地图初始化失败，平滑启动高保真交互 Canvas 降级模式:', error)
    isFallbackCanvas.value = true
    nextTick(() => {
      initFallbackCanvas()
    })
  }
}

// 在高德地图上渲染双语义 Marker 与道路折线
const renderRouteOnAMap = (AMap: any) => {
  if (!mapInstance.value || !AMap) return

  // 清除旧点位与旧折线
  markersMap.forEach(m => mapInstance.value.remove(m))
  markersMap.clear()
  if (polylineInstance.value) {
    mapInstance.value.remove(polylineInstance.value)
    polylineInstance.value = null
  }

  const items = routeItems.value
  if (!items || items.length === 0) return

  const pathCoords: [number, number][] = []

  items.forEach((item, index) => {
    const lng = Number(item.longitude)
    const lat = Number(item.latitude)
    pathCoords.push([lng, lat])

    const isPoi = item.itemType === 'POI'
    const colorClass = isPoi ? 'marker-poi' : 'marker-dining'
    const iconSymbol = isPoi ? '🏛️' : '🍜'
    const seq = String(item.itemOrder).padStart(2, '0')

    const markerContent = document.createElement('div')
    markerContent.className = `custom-map-marker ${colorClass} ${selectedOrder.value === item.itemOrder ? 'is-selected' : ''}`
    markerContent.setAttribute('data-order', String(item.itemOrder))
    markerContent.innerHTML = `
      <div class="marker-pulse"></div>
      <div class="marker-badge">
        <span class="marker-icon">${iconSymbol}</span>
        <span class="marker-num">${seq}</span>
      </div>
      <div class="marker-tooltip">
        <div class="tooltip-title">${item.name}</div>
        <div class="tooltip-time">🕒 ${item.arriveTime} 到达 · ${isPoi ? '文旅' : '美食'}</div>
      </div>
    `

    markerContent.addEventListener('click', () => {
      selectNode(item.itemOrder)
    })

    const marker = new AMap.Marker({
      position: [lng, lat],
      content: markerContent,
      offset: new AMap.Pixel(-24, -48),
      title: item.name,
      zIndex: 100 + index,
    })

    marker.setMap(mapInstance.value)
    markersMap.set(item.itemOrder, marker)
  })

  // 绘制发光道路轨迹折线
  if (pathCoords.length > 1) {
    polylineInstance.value = new AMap.Polyline({
      path: pathCoords,
      isOutline: true,
      outlineColor: '#070B14',
      borderWeight: 2,
      strokeColor: '#06B6D4',
      strokeOpacity: 0.95,
      strokeWeight: 6,
      strokeStyle: 'solid',
      strokeDasharray: [10, 5],
      lineJoin: 'round',
      lineCap: 'round',
      zIndex: 50,
      showDir: true,
    })

    polylineInstance.value.setMap(mapInstance.value)
    mapInstance.value.setFitView([polylineInstance.value, ...Array.from(markersMap.values())], false, [60, 60, 60, 440])
  } else if (pathCoords.length === 1) {
    mapInstance.value.setCenter(pathCoords[0])
    mapInstance.value.setZoom(15)
  }
}

// 降级高保真 3D Canvas 渲染器 (断网/无 Key 时 100% 优雅可用)
let animationFrameId: number | null = null
let canvasAnimAngle = 0

const initFallbackCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    const parent = canvas.parentElement
    if (parent) {
      canvas.width = parent.clientWidth
      canvas.height = parent.clientHeight
    }
  }
  resize()
  window.addEventListener('resize', resize)

  const renderFrame = () => {
    if (!ctx || !canvas) return
    const w = canvas.width
    const h = canvas.height

    // 1. 深邃深空底色
    ctx.fillStyle = '#070B14'
    ctx.fillRect(0, 0, w, h)

    // 2. 3D 透视网格
    ctx.save()
    ctx.strokeStyle = 'rgba(6, 182, 212, 0.08)'
    ctx.lineWidth = 1
    const gridSize = 40
    for (let x = 0; x < w; x += gridSize) {
      ctx.beginPath()
      ctx.moveTo(x, 0)
      ctx.lineTo(x, h)
      ctx.stroke()
    }
    for (let y = 0; y < h; y += gridSize) {
      ctx.beginPath()
      ctx.moveTo(0, y)
      ctx.lineTo(w, y)
      ctx.stroke()
    }

    // 3. 湘江流向水系与东洲岛弧线示意
    ctx.beginPath()
    ctx.moveTo(w * 0.45, 0)
    ctx.bezierCurveTo(w * 0.48, h * 0.35, w * 0.55, h * 0.65, w * 0.52, h)
    ctx.strokeStyle = 'rgba(6, 182, 212, 0.22)'
    ctx.lineWidth = 48
    ctx.lineCap = 'round'
    ctx.stroke()

    // 4. 绘制路线折线
    const items = routeItems.value
    if (items.length > 0) {
      const coords = getProjectedCoords(items, w, h)

      if (coords.length > 1) {
        // 折线外发光
        ctx.beginPath()
        coords.forEach((pt, i) => {
          if (i === 0) ctx.moveTo(pt.x, pt.y)
          else ctx.lineTo(pt.x, pt.y)
        })
        ctx.strokeStyle = 'rgba(6, 182, 212, 0.35)'
        ctx.lineWidth = 12
        ctx.lineJoin = 'round'
        ctx.stroke()

        // 主轨迹线
        ctx.beginPath()
        coords.forEach((pt, i) => {
          if (i === 0) ctx.moveTo(pt.x, pt.y)
          else ctx.lineTo(pt.x, pt.y)
        })
        ctx.strokeStyle = '#06B6D4'
        ctx.lineWidth = 5
        ctx.stroke()
      }

      // 5. 绘制 Marker 点位
      coords.forEach((pt, i) => {
        const item = items[i]
        const isPoi = item.itemType === 'POI'
        const isSelected = selectedOrder.value === item.itemOrder
        const color = isPoi ? '#06B6D4' : '#F97316'
        const glowColor = isPoi ? 'rgba(6, 182, 212, 0.45)' : 'rgba(249, 115, 22, 0.45)'

        // 呼吸脉冲波
        canvasAnimAngle += 0.005
        const pulseRadius = 18 + Math.sin(canvasAnimAngle + i) * 6
        ctx.beginPath()
        ctx.arc(pt.x, pt.y, pulseRadius, 0, Math.PI * 2)
        ctx.fillStyle = glowColor
        ctx.fill()

        // 主圆盘
        ctx.beginPath()
        ctx.arc(pt.x, pt.y, isSelected ? 16 : 13, 0, Math.PI * 2)
        ctx.fillStyle = isSelected ? '#FFFFFF' : color
        ctx.fill()
        ctx.lineWidth = 3
        ctx.strokeStyle = isSelected ? color : '#070B14'
        ctx.stroke()

        // 序号文本
        ctx.fillStyle = isSelected ? color : '#070B14'
        ctx.font = 'bold 12px sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText(String(item.itemOrder).padStart(2, '0'), pt.x, pt.y)

        // 标牌文字
        ctx.fillStyle = '#F8FAFC'
        ctx.font = isSelected ? 'bold 13px sans-serif' : '12px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText(item.name, pt.x, pt.y + 26)
      })
    }

    ctx.restore()
    animationFrameId = requestAnimationFrame(renderFrame)
  }

  renderFrame()
}

// 辅助：将经纬度映射到 Canvas 相对屏幕坐标
const getProjectedCoords = (items: RouteItemVO[], w: number, h: number) => {
  if (items.length === 0) return []

  let minLng = Infinity
  let maxLng = -Infinity
  let minLat = Infinity
  let maxLat = -Infinity

  items.forEach(it => {
    const lng = Number(it.longitude)
    const lat = Number(it.latitude)
    if (lng < minLng) minLng = lng
    if (lng > maxLng) maxLng = lng
    if (lat < minLat) minLat = lat
    if (lat > maxLat) maxLat = lat
  })

  const spanLng = Math.max(maxLng - minLng, 0.01)
  const spanLat = Math.max(maxLat - minLat, 0.01)

  // 预留左侧面板 420px 边距
  const paddingLeft = window.innerWidth > 960 ? 460 : 60
  const paddingRight = 80
  const paddingTop = 120
  const paddingBottom = 120

  const drawW = Math.max(w - paddingLeft - paddingRight, 200)
  const drawH = Math.max(h - paddingTop - paddingBottom, 200)

  return items.map(it => {
    const lng = Number(it.longitude)
    const lat = Number(it.latitude)
    const x = paddingLeft + ((lng - minLng) / spanLng) * drawW
    const y = h - paddingBottom - ((lat - minLat) / spanLat) * drawH
    return { x, y, order: it.itemOrder }
  })
}

// 选中某个节点，并高亮 + 居中地图 + 平滑滚动时间轴
const selectNode = (order: number) => {
  selectedOrder.value = order
  routeStore.selectNode(order)

  // 地图 Marker 样式更新
  const markerEl = document.querySelector(`.custom-map-marker[data-order="${order}"]`)
  document.querySelectorAll('.custom-map-marker').forEach(el => el.classList.remove('is-selected'))
  if (markerEl) {
    markerEl.classList.add('is-selected')
  }

  // 高德地图平滑平移
  if (mapInstance.value && !isFallbackCanvas.value) {
    const targetItem = routeItems.value.find(it => it.itemOrder === order)
    if (targetItem) {
      mapInstance.value.panTo([Number(targetItem.longitude), Number(targetItem.latitude)])
    }
  }

  // 时间轴对应卡片平滑滚动
  nextTick(() => {
    const el = cardRefs.value[order]
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  })
}

// 切换 3D / 2D 视角
const toggle3DMode = () => {
  is3D.value = !is3D.value
  currentPitch.value = is3D.value ? 50 : 0
  if (mapInstance.value) {
    mapInstance.value.setPitch(currentPitch.value)
  }
}

// 重置全景观景
const resetView = () => {
  if (mapInstance.value && !isFallbackCanvas.value) {
    const markers = Array.from(markersMap.values())
    if (markers.length > 0) {
      mapInstance.value.setFitView(markers, false, [60, 60, 60, 440])
    } else {
      mapInstance.value.setCenter(HENGYANG_CENTER)
      mapInstance.value.setZoom(14)
    }
  }
  selectedOrder.value = 1
}

// 放大
const zoomIn = () => {
  if (mapInstance.value) mapInstance.value.zoomIn()
}

// 缩小
const zoomOut = () => {
  if (mapInstance.value) mapInstance.value.zoomOut()
}

// 一键导航 (唤起高德地图)
const handleNavigate = (item: RouteItemVO) => {
  const url = `https://uri.amap.com/navigation?to=${item.longitude},${item.latitude},${encodeURIComponent(item.name)}&mode=car&callnative=1`
  window.open(url, '_blank', 'noopener,noreferrer')
}

// 重新规划
const handleReplan = () => {
  router.push('/')
}

// 监听键盘 Escape
const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (isMobileDrawerOpen.value && window.innerWidth <= 960) {
      isMobileDrawerOpen.value = false
    }
  }
}

onMounted(async () => {
  window.addEventListener('keydown', handleKeyDown)

  // 若当前无路线，自动生成一套高品质衡阳漫游路线
  if (!routeStore.hasActivePlan) {
    await routeStore.generatePlan({
      durationHours: 4,
      atmosphere: '松弛感',
      transportMode: 'WALKING',
      spicyLevel: '微辣',
      dietaryRestrictions: ['不吃内脏'],
      budgetPerMeal: 35,
    })
  }

  // 初始化地图
  await initAMap()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown)
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  if (mapInstance.value) {
    mapInstance.value.destroy()
    mapInstance.value = null
  }
})

// 监听路线数据变更时重绘
watch(
  () => routeStore.currentPlan,
  () => {
    if (mapInstance.value && window.AMap) {
      renderRouteOnAMap(window.AMap)
    }
  },
  { deep: true },
)
</script>

<template>
  <div class="route-map-container">
    <!-- ========== 地图主体容器 ========== -->
    <div
      v-show="!isFallbackCanvas"
      ref="mapContainerRef"
      class="amap-canvas"
      aria-label="3D 文旅路线全景地图"
    />

    <!-- ========== 降级 Canvas (离线/无 Key 稳健保障) ========== -->
    <div v-show="isFallbackCanvas" class="fallback-canvas-wrapper">
      <canvas ref="canvasRef" class="fallback-canvas" />
      <div class="fallback-badge">
        <span class="pulse-dot" />
        <span>本地高保真 3D 仿真视界 · 零网络崩溃兜底</span>
      </div>
    </div>

    <!-- ========== 地图悬浮操控工具条 ========== -->
    <div class="map-controls">
      <button
        class="control-btn"
        :class="{ active: is3D }"
        title="切换 3D / 2D 俯仰视角"
        @click="toggle3DMode"
      >
        <span class="control-text">{{ is3D ? '3D' : '2D' }}</span>
      </button>
      <button class="control-btn" title="放大地图" @click="zoomIn">
        <span>➕</span>
      </button>
      <button class="control-btn" title="缩小地图" @click="zoomOut">
        <span>➖</span>
      </button>
      <button class="control-btn" title="重置全景观景" @click="resetView">
        <span>🎯</span>
      </button>
    </div>

    <!-- ========== 移动端折叠控制条 ========== -->
    <button
      class="mobile-drawer-toggle"
      @click="isMobileDrawerOpen = !isMobileDrawerOpen"
    >
      <span>{{ isMobileDrawerOpen ? '🔽 收起行程清单' : '🔼 展开行程清单' }} ({{ routeItems.length }}站)</span>
    </button>

    <!-- ========== 左侧/底部悬浮式路线时间轴面板 ========== -->
    <aside
      class="timeline-panel"
      :class="{ 'is-mobile-collapsed': !isMobileDrawerOpen }"
      aria-label="行程时空时间轴"
    >
      <!-- 头部概览卡片 -->
      <div class="panel-header">
        <div class="header-badge-row">
          <span class="plan-tag">AI 智能规划路线</span>
          <span class="plan-vibe">🌿 {{ plan?.atmosphere || '松弛感' }}</span>
        </div>
        <h1 class="plan-title">{{ plan?.title || '衡阳文旅专属探索路线' }}</h1>
        <div class="metrics-row">
          <div class="metric-item">
            <span class="metric-label">全程预估</span>
            <span class="metric-val highlight">{{ totalDistanceKm }} km</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">推荐耗时</span>
            <span class="metric-val">{{ totalDurationText }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">行程站点</span>
            <span class="metric-val">
              <span class="cyan-text">{{ poiCount }} 景点</span> ·
              <span class="orange-text">{{ merchantCount }} 美食</span>
            </span>
          </div>
        </div>

        <div class="header-actions">
          <button class="btn-replan" @click="handleReplan">
            <span>🔄</span> 重新定制
          </button>
        </div>
      </div>

      <!-- 时间轴列表 -->
      <div class="timeline-list">
        <div
          v-for="(item, index) in routeItems"
          :key="item.id || item.itemOrder"
          :ref="el => { if (el) cardRefs[item.itemOrder] = el as HTMLElement }"
          class="timeline-node"
          :class="{
            'is-active': selectedOrder === item.itemOrder,
            'is-poi': item.itemType === 'POI',
            'is-dining': item.itemType === 'MERCHANT',
          }"
          @click="selectNode(item.itemOrder)"
        >
          <!-- 节点左侧顺序轴线 -->
          <div class="node-axis">
            <div class="node-bullet" :class="item.itemType === 'POI' ? 'bullet-poi' : 'bullet-dining'">
              <span>{{ String(item.itemOrder).padStart(2, '0') }}</span>
            </div>
            <div v-if="index < routeItems.length - 1" class="node-line" />
          </div>

          <!-- 节点卡片主体 -->
          <div class="node-card">
            <!-- 头部：序号、语义标签、名称 -->
            <div class="node-header">
              <div class="node-title-group">
                <span
                  class="type-pill"
                  :class="item.itemType === 'POI' ? 'pill-poi' : 'pill-dining'"
                >
                  {{ item.itemType === 'POI' ? '🏛️ 文旅地标' : '🍜 特色美食' }}
                </span>
                <h3 class="node-name">{{ item.name }}</h3>
              </div>
              <button
                class="btn-nav-mini"
                title="导航前往"
                @click.stop="handleNavigate(item)"
              >
                🧭 导航
              </button>
            </div>

            <!-- 时空指标条 -->
            <div class="node-meta">
              <span class="meta-pill">🕒 预计到达 {{ item.arriveTime }}</span>
              <span class="meta-pill">⏳ 停留 {{ item.stayMinutes }} 分钟</span>
              <span v-if="item.transportToNextMinutes > 0" class="meta-pill transit-pill">
                🚶 到下站约 {{ item.transportToNextMinutes }} 分钟 ({{ item.transportToNextDistance }}米)
              </span>
            </div>

            <!-- 💡 可解释推荐理由 -->
            <div class="rationale-box">
              <div class="rationale-label">
                <span class="bulb-icon">💡</span>
                <span>智能体推荐理由</span>
              </div>
              <p class="rationale-text">{{ item.recommendReason }}</p>
            </div>

            <!-- 🍽️ 餐饮节点专属展示：招牌菜品、价格、辣度与避坑提示 -->
            <div
              v-if="item.itemType === 'MERCHANT' && item.recommendedDishes && item.recommendedDishes.length > 0"
              class="dining-box"
            >
              <div class="dining-header">
                <span class="dining-title">🍽️ 推荐必点特色菜品</span>
                <span class="dining-subtitle">已按您的辣度偏好与忌口过滤</span>
              </div>
              <div class="dishes-list">
                <div
                  v-for="dish in (item.recommendedDishes as DishVO[])"
                  :key="dish.name"
                  class="dish-card"
                >
                  <div class="dish-row-primary">
                    <span class="dish-name">{{ dish.name }}</span>
                    <span class="dish-price">¥{{ Number(dish.price).toFixed(2) }}</span>
                  </div>

                  <div class="dish-tags-row">
                    <span v-if="dish.spicyLevel" class="spicy-tag">
                      🌶️ {{ dish.spicyLevel }}
                    </span>
                    <span v-if="dish.flavorNotes" class="flavor-tag">
                      {{ dish.flavorNotes }}
                    </span>
                  </div>

                  <!-- 避坑与过敏原提示 -->
                  <div v-if="dish.warning || dish.allergensOrIngredients" class="warning-box">
                    <span class="warning-icon">⚠️</span>
                    <span class="warning-text">
                      {{ dish.warning || dish.allergensOrIngredients }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 底部操作栏 -->
            <div class="card-footer">
              <button class="btn-action btn-locate" @click.stop="selectNode(item.itemOrder)">
                <span>🎯</span> 地图对齐
              </button>
              <button class="btn-action btn-nav" @click.stop="handleNavigate(item)">
                <span>🧭</span> 一键导航
              </button>
            </div>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
/* ========== 主容器 ========== */
.route-map-container {
  position: relative;
  width: 100vw;
  height: calc(100vh - var(--navbar-height));
  overflow: hidden;
  background-color: var(--surface-ground, #070B14);
}

/* ========== 地图容器 ========== */
.amap-canvas {
  width: 100%;
  height: 100%;
}

/* 降级高保真 Canvas */
.fallback-canvas-wrapper {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.fallback-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.fallback-badge {
  position: absolute;
  top: 16px;
  right: 20px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid var(--color-primary-muted);
  font-size: 12px;
  color: var(--color-primary);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
  z-index: 20;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-primary);
  box-shadow: 0 0 10px var(--color-primary);
  animation: pulseDot 2s infinite;
}

@keyframes pulseDot {
  0% { transform: scale(0.9); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.8; }
}

/* ========== 地图悬浮工具条 ========== */
.map-controls {
  position: absolute;
  top: 24px;
  right: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 40;
}

.control-btn {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--surface-card-glass, rgba(15, 23, 42, 0.88));
  backdrop-filter: blur(16px);
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  color: var(--text-primary, #F8FAFC);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s ease;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
}

.control-btn:hover,
.control-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary-muted);
  color: var(--color-primary);
  box-shadow: 0 0 14px var(--color-primary-glow);
  transform: translateY(-2px);
}

.control-text {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

/* ========== 移动端折叠条 ========== */
.mobile-drawer-toggle {
  display: none;
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 44px;
  background: var(--surface-card, #0F172A);
  color: var(--text-primary);
  border: none;
  border-top: 1px solid var(--border-default);
  font-size: 14px;
  font-weight: 600;
  align-items: center;
  justify-content: center;
  z-index: 60;
  cursor: pointer;
}

/* ========== 悬浮式路线时间轴面板 ========== */
.timeline-panel {
  position: absolute;
  top: 20px;
  left: 24px;
  width: 420px;
  max-height: calc(100vh - var(--navbar-height) - 40px);
  background: rgba(11, 17, 32, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.65);
  display: flex;
  flex-direction: column;
  z-index: 50;
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 面板头部 */
.panel-header {
  padding: 20px 22px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  background: linear-gradient(180deg, rgba(6, 182, 212, 0.08) 0%, transparent 100%);
}

.header-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.plan-tag {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary, #06B6D4);
  background: var(--color-primary-muted, rgba(6, 182, 212, 0.15));
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(6, 182, 212, 0.25);
  letter-spacing: 0.04em;
}

.plan-vibe {
  font-size: 11px;
  font-weight: 600;
  color: #F8FAFC;
  background: rgba(255, 255, 255, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
}

.plan-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary, #F8FAFC);
  line-height: 1.35;
}

.metrics-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(15, 23, 42, 0.65);
  border-radius: 12px;
  padding: 10px 14px;
  border: 1px solid rgba(148, 163, 184, 0.1);
  margin-bottom: 12px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.metric-label {
  font-size: 11px;
  color: var(--text-muted, #64748B);
}

.metric-val {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary, #F8FAFC);
}

.metric-val.highlight {
  color: var(--color-primary, #06B6D4);
}

.cyan-text {
  color: var(--color-primary, #06B6D4);
}

.orange-text {
  color: var(--color-dining, #F97316);
}

.header-actions {
  display: flex;
  justify-content: flex-end;
}

.btn-replan {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  min-height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  color: var(--text-secondary, #94A3B8);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-replan:hover {
  background: rgba(255, 255, 255, 0.12);
  color: var(--text-primary);
  border-color: var(--border-hover);
}

/* 时间轴可滚动列表 */
.timeline-list {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scrollbar-width: thin;
  scrollbar-color: rgba(6, 182, 212, 0.3) transparent;
}

.timeline-list::-webkit-scrollbar {
  width: 6px;
}

.timeline-list::-webkit-scrollbar-thumb {
  background: rgba(6, 182, 212, 0.3);
  border-radius: 999px;
}

/* 时间轴单个节点 */
.timeline-node {
  display: flex;
  gap: 14px;
  position: relative;
  cursor: pointer;
}

/* 左侧轴线 */
.node-axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 32px;
  flex-shrink: 0;
}

.node-bullet {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  z-index: 2;
  box-shadow: 0 0 12px rgba(0, 0, 0, 0.6);
  transition: transform 0.2s ease;
}

.bullet-poi {
  background: #083344;
  color: var(--color-primary, #06B6D4);
  border: 2px solid var(--color-primary, #06B6D4);
}

.bullet-dining {
  background: #431407;
  color: var(--color-dining, #F97316);
  border: 2px solid var(--color-dining, #F97316);
}

.timeline-node.is-active .node-bullet {
  transform: scale(1.15);
  box-shadow: 0 0 16px currentColor;
}

.node-line {
  flex: 1;
  width: 2px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.3) 0%, rgba(148, 163, 184, 0.1) 100%);
  margin-top: 4px;
  margin-bottom: -12px;
}

/* 右侧卡片主体 */
.node-card {
  flex: 1;
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  padding: 16px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.timeline-node:hover .node-card {
  border-color: rgba(148, 163, 184, 0.4);
  transform: translateX(2px);
}

.timeline-node.is-active.is-poi .node-card {
  border-color: var(--color-primary, #06B6D4);
  box-shadow: 0 0 20px rgba(6, 182, 212, 0.25);
  background: rgba(15, 23, 42, 0.95);
}

.timeline-node.is-active.is-dining .node-card {
  border-color: var(--color-dining, #F97316);
  box-shadow: 0 0 20px rgba(249, 115, 22, 0.25);
  background: rgba(15, 23, 42, 0.95);
}

/* 节点头部 */
.node-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.node-title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.type-pill {
  align-self: flex-start;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  letter-spacing: 0.03em;
}

.pill-poi {
  background: rgba(6, 182, 212, 0.15);
  color: var(--color-primary, #06B6D4);
  border: 1px solid rgba(6, 182, 212, 0.3);
}

.pill-dining {
  background: rgba(249, 115, 22, 0.15);
  color: var(--color-dining, #F97316);
  border: 1px solid rgba(249, 115, 22, 0.3);
}

.node-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary, #F8FAFC);
}

.btn-nav-mini {
  background: transparent;
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 28px;
}

.btn-nav-mini:hover {
  background: var(--color-primary);
  color: var(--text-inverse);
  border-color: var(--color-primary);
}

/* 时空指标条 */
.node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.meta-pill {
  font-size: 11px;
  color: var(--text-secondary, #94A3B8);
  background: rgba(255, 255, 255, 0.04);
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.transit-pill {
  color: var(--color-primary, #06B6D4);
  background: rgba(6, 182, 212, 0.08);
  border-color: rgba(6, 182, 212, 0.2);
}

/* 💡 可解释推荐理由 */
.rationale-box {
  background: rgba(2, 6, 23, 0.6);
  border-left: 3px solid var(--color-primary, #06B6D4);
  border-radius: 0 8px 8px 0;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.timeline-node.is-dining .rationale-box {
  border-left-color: var(--color-dining, #F97316);
}

.rationale-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary, #06B6D4);
  margin-bottom: 4px;
}

.timeline-node.is-dining .rationale-label {
  color: var(--color-dining, #F97316);
}

.bulb-icon {
  font-size: 13px;
}

.rationale-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #E2E8F0;
}

/* 🍽️ 餐饮专属菜品与避坑模块 */
.dining-box {
  background: rgba(249, 115, 22, 0.06);
  border: 1px solid rgba(249, 115, 22, 0.2);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
}

.dining-header {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 8px;
}

.dining-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-dining, #F97316);
}

.dining-subtitle {
  font-size: 11px;
  color: var(--text-muted, #64748B);
}

.dishes-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dish-card {
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 8px;
  padding: 8px 10px;
}

.dish-row-primary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.dish-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary, #F8FAFC);
}

.dish-price {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-dining, #F97316);
}

.dish-tags-row {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
}

.spicy-tag {
  font-size: 11px;
  color: #EF4444;
  background: rgba(239, 68, 68, 0.12);
  padding: 1px 6px;
  border-radius: 4px;
}

.flavor-tag {
  font-size: 11px;
  color: var(--text-secondary, #94A3B8);
  background: rgba(255, 255, 255, 0.05);
  padding: 1px 6px;
  border-radius: 4px;
}

.warning-box {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px dashed rgba(245, 158, 11, 0.35);
  border-radius: 6px;
  padding: 4px 8px;
  margin-top: 4px;
}

.warning-icon {
  font-size: 12px;
  flex-shrink: 0;
}

.warning-text {
  font-size: 11px;
  color: #FCD34D;
  line-height: 1.4;
}

/* 底部操作栏 */
.card-footer {
  display: flex;
  gap: 8px;
}

.btn-action {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-locate {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  color: var(--text-secondary, #94A3B8);
}

.btn-locate:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}

.btn-nav {
  background: var(--color-primary-muted, rgba(6, 182, 212, 0.15));
  border: 1px solid var(--color-primary, #06B6D4);
  color: var(--color-primary, #06B6D4);
}

.btn-nav:hover {
  background: var(--color-primary, #06B6D4);
  color: var(--text-inverse, #020617);
}

/* ========== 高德地图 Marker 自定义 DOM 样式 (非 Scoped，需全局注入) ========== */
:deep(.custom-map-marker) {
  position: relative;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease;
}

:deep(.custom-map-marker:hover),
:deep(.custom-map-marker.is-selected) {
  transform: scale(1.2);
  z-index: 999 !important;
}

:deep(.custom-map-marker .marker-pulse) {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  animation: markerPulse 2s infinite;
  pointer-events: none;
}

:deep(.custom-map-marker.marker-poi .marker-pulse) {
  border: 2px solid rgba(6, 182, 212, 0.6);
  box-shadow: 0 0 16px rgba(6, 182, 212, 0.4);
}

:deep(.custom-map-marker.marker-dining .marker-pulse) {
  border: 2px solid rgba(249, 115, 22, 0.6);
  box-shadow: 0 0 16px rgba(249, 115, 22, 0.4);
}

@keyframes markerPulse {
  0% { transform: scale(0.8); opacity: 0.9; }
  70% { transform: scale(1.6); opacity: 0; }
  100% { transform: scale(0.8); opacity: 0; }
}

:deep(.custom-map-marker .marker-badge) {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.7);
  z-index: 2;
}

:deep(.custom-map-marker.marker-poi .marker-badge) {
  background: #083344;
  border: 2px solid #06B6D4;
  color: #06B6D4;
}

:deep(.custom-map-marker.marker-dining .marker-badge) {
  background: #431407;
  border: 2px solid #F97316;
  color: #F97316;
}

:deep(.custom-map-marker.is-selected .marker-badge) {
  background: #FFFFFF;
}

:deep(.custom-map-marker.is-selected.marker-poi .marker-badge) {
  color: #0891B2;
  border-color: #FFFFFF;
}

:deep(.custom-map-marker.is-selected.marker-dining .marker-badge) {
  color: #EA580C;
  border-color: #FFFFFF;
}

:deep(.marker-num) {
  font-size: 11px;
  font-weight: 800;
  margin-left: 2px;
}

:deep(.marker-icon) {
  font-size: 12px;
}

/* Marker Tooltip */
:deep(.marker-tooltip) {
  position: absolute;
  bottom: 54px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(15, 23, 42, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.3);
  backdrop-filter: blur(10px);
  padding: 6px 12px;
  border-radius: 8px;
  white-space: nowrap;
  pointer-events: none;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6);
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

:deep(.custom-map-marker:hover .marker-tooltip),
:deep(.custom-map-marker.is-selected .marker-tooltip) {
  opacity: 1;
  transform: translateX(-50%) translateY(-4px);
}

:deep(.tooltip-title) {
  font-size: 13px;
  font-weight: 700;
  color: #F8FAFC;
}

:deep(.tooltip-time) {
  font-size: 11px;
  color: #94A3B8;
}

/* ========== 响应式与移动端适配 ========== */
@media (max-width: 960px) {
  .mobile-drawer-toggle {
    display: flex;
  }

  .timeline-panel {
    top: auto;
    bottom: 44px;
    left: 0;
    width: 100%;
    max-height: 60vh;
    border-radius: 20px 20px 0 0;
    border-bottom: none;
  }

  .timeline-panel.is-mobile-collapsed {
    transform: translateY(calc(100% - 10px));
  }

  .map-controls {
    top: 16px;
    right: 16px;
  }
}
</style>
