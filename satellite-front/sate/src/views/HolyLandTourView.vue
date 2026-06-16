<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useHolyTourStore } from '@/store/holyTour'
import { useUserStore } from '@/store/user'
import { loadAmap } from '@/utils/amapLoader'
import { normalizeCityKeyword } from '@/utils/common'

type LngLatTuple = [number, number]

type RecommendedSpot = {
  name: string
  summary: string
  story?: string
  query?: string
  province?: string
  city?: string
  lng?: number
  lat?: number
  imageUrl?: string
}

type AMapModule = {
  Map: new (...args: any[]) => any
  Marker: new (...args: any[]) => any
  Geolocation: new (...args: any[]) => any
  Geocoder: new (...args: any[]) => any
  Driving: new (...args: any[]) => any
  Polyline: new (...args: any[]) => any
  Pixel: new (...args: any[]) => any
}

type DrivingRestResponse = {
  status?: string
  info?: string
  infocode?: string
  route?: {
    paths?: Array<{
      steps?: Array<{
        polyline?: string
      }>
    }>
  }
}

// 高德Key从后端获取，不再使用前端环境变量
const defaultCenter: LngLatTuple = [116.397428, 39.90923]
const QUICK_CITIES = ['北京', '上海', '广州', '深圳', '杭州', '成都', '重庆', '西安', '武汉', '南京']
const DEFAULT_THEME_COLOR = '#00e5ff'

const RECOMMENDED_SPOTS: Record<string, RecommendedSpot[]> = {
  北京: [
    { name: '故宫博物院', summary: '皇家建筑群与中轴线核心地标。', query: '北京市东城区故宫博物院' },
    { name: '天坛公园', summary: '明清祭天建筑，适合慢游打卡。', query: '北京市东城区天坛公园' },
    { name: '颐和园', summary: '湖山园林结合的经典路线。', query: '北京市海淀区颐和园' },
  ],
  上海: [
    { name: '外滩', summary: '黄浦江沿岸夜景与万国建筑群。', query: '上海市黄浦区外滩' },
    { name: '豫园', summary: '江南古典园林与老城厢文化。', query: '上海市黄浦区豫园' },
    { name: '上海博物馆', summary: '综合性博物馆，文物密度高。', query: '上海市黄浦区上海博物馆' },
  ],
  广州: [
    { name: '陈家祠', summary: '岭南建筑工艺代表。', query: '广州市荔湾区陈家祠' },
    { name: '沙面', summary: '近代建筑风貌街区。', query: '广州市荔湾区沙面' },
    { name: '广州塔', summary: '珠江两岸夜游核心地标。', query: '广州市海珠区广州塔' },
  ],
  深圳: [
    { name: '深圳湾公园', summary: '海岸线城市景观与骑行路线。', query: '深圳市南山区深圳湾公园' },
    { name: '华侨城创意园', summary: '文创街区与展馆集群。', query: '深圳市南山区华侨城创意园' },
    { name: '莲花山公园', summary: '城市中心观景制高点。', query: '深圳市福田区莲花山公园' },
  ],
  杭州: [
    { name: '西湖', summary: '环湖景点密集，适合步行与骑行。', query: '杭州市西湖风景名胜区' },
    { name: '灵隐寺', summary: '杭州代表性人文景点。', query: '杭州市西湖区灵隐寺' },
    { name: '河坊街', summary: '老街市井与本地小吃集中。', query: '杭州市上城区河坊街' },
  ],
  成都: [
    { name: '武侯祠', summary: '三国文化核心景点。', query: '成都市武侯区武侯祠' },
    { name: '锦里', summary: '夜游体验和民俗氛围突出。', query: '成都市武侯区锦里古街' },
    { name: '都江堰', summary: '世界级水利工程遗产。', query: '成都市都江堰景区' },
  ],
  重庆: [
    { name: '洪崖洞', summary: '山城夜景地标，适合夜间游览。', query: '重庆市渝中区洪崖洞' },
    { name: '磁器口古镇', summary: '老街区与在地风味集中。', query: '重庆市沙坪坝区磁器口古镇' },
    { name: '解放碑', summary: '核心商圈与城市中心节点。', query: '重庆市渝中区解放碑' },
  ],
  西安: [
    { name: '秦始皇兵马俑', summary: '西安代表性历史遗址。', query: '西安市临潼区秦始皇兵马俑博物馆' },
    { name: '西安城墙', summary: '完整古城防体系，可骑行。', query: '西安市碑林区西安城墙' },
    { name: '大雁塔', summary: '唐文化核心地标。', query: '西安市雁塔区大雁塔' },
  ],
  武汉: [
    { name: '黄鹤楼', summary: '武汉城市名片与登高视野点。', query: '武汉市武昌区黄鹤楼' },
    { name: '东湖', summary: '超大城市湖泊景区。', query: '武汉市武昌区东湖风景区' },
    { name: '户部巷', summary: '本地小吃与夜市氛围。', query: '武汉市武昌区户部巷' },
  ],
  南京: [
    { name: '中山陵', summary: '南京历史文化主轴景区。', query: '南京市玄武区中山陵景区' },
    { name: '夫子庙', summary: '秦淮河夜游与老城文化。', query: '南京市秦淮区夫子庙' },
    { name: '明孝陵', summary: '明代皇家陵寝遗址。', query: '南京市玄武区明孝陵景区' },
  ],
}

const userStore = useUserStore()
const holyTourStore = useHolyTourStore()

const mapContainerRef = ref<HTMLDivElement | null>(null)
const isLoadingMap = ref(false)
const isLocating = ref(false)
const isPlanning = ref(false)
const errorMessage = ref('')

const currentCityLabel = ref(userStore.userInfo.city || '未定位')
const startInput = ref(userStore.userInfo.city || '')
const endInput = ref('')
const selectedCities = ref<string[]>([])
const selectedDestinationSpot = ref<RecommendedSpot | null>(null)
const selectedDestinationKeyword = ref('')

const locatedPoint = ref<LngLatTuple | null>(null)
const plannedStart = ref('')
const plannedEnd = ref('')
const autoPlannedRouteSignature = ref('')

let AMap: AMapModule | null = null
let mapInstance: any = null
let geolocationPlugin: any = null
let geocoder: any = null
let driving: any = null
let startMarker: any = null
let endMarker: any = null
let spotPreviewMarker: any = null
let fallbackPolyline: any = null

const normalizeCityKey = (keyword: string): string => {
  const text = keyword.trim().replace(/\s+/g, '')
  if (!text) return ''

  const quickMatch = QUICK_CITIES.find((city) => text === city || text === `${city}市` || text.includes(city))
  if (quickMatch) return quickMatch

  const matched = text.match(/^(.+?)(?:市|地区|盟|自治州|特别行政区|区|县)/)
  if (matched?.[1]) {
    return matched[1]
  }

  return text
}

const canPlanRoute = computed(() => !!startInput.value.trim() && !!endInput.value.trim() && !isPlanning.value)
const fallbackCityForSpots = computed(() => {
  const current = normalizeCityKey(currentCityLabel.value)
  if (current && current !== '未定位' && current !== '定位失败') {
    return current
  }

  return normalizeCityKey(userStore.userInfo.city || '')
})
const destinationCityForSpots = computed(() => {
  if (selectedCities.value.length >= 2) {
    return normalizeCityKey(selectedCities.value[1])
  }

  if (selectedCities.value.length === 1) {
    return normalizeCityKey(selectedCities.value[0])
  }

  const end = normalizeCityKey(endInput.value)
  if (end) return end

  return fallbackCityForSpots.value
})
const holyRoute = computed(() => holyTourStore.spotsByCity[destinationCityForSpots.value] || null)
const activeRoute = computed(() => holyTourStore.preferredRoute || holyRoute.value || null)
const recommendedSpots = computed<RecommendedSpot[]>(() => {
  if (activeRoute.value) {
    return activeRoute.value.spots.map((spot) => ({
      name: spot.name,
      summary: spot.summary || spot.story,
      story: spot.story,
      query: spot.query,
      province: spot.province,
      city: spot.city,
      lng: spot.lng,
      lat: spot.lat,
      imageUrl: spot.imageUrl,
    }))
  }

  return RECOMMENDED_SPOTS[destinationCityForSpots.value] || []
})
const dataSourceLabel = computed(() => (activeRoute.value ? '智能体' : '本地'))
const smartFallbackMessage = computed(() => (activeRoute.value ? '' : holyTourStore.error))
const activeRouteThemeColor = computed(() => activeRoute.value?.heroThemeColor || DEFAULT_THEME_COLOR)
const activeRouteTitle = computed(() => activeRoute.value?.routeName || `${destinationCityForSpots.value || '当前城市'}推荐景点`)
const activeHeroName = computed(() => activeRoute.value?.heroName || userStore.userInfo.mainHeroes[0] || '巡游英雄')
const activeStorySpot = computed(() => selectedDestinationSpot.value || recommendedSpots.value[0] || null)
const selectedSpotLabel = computed(() => selectedDestinationSpot.value?.name || '-')
const locatedStartLabel = computed(() => {
  const city = currentCityLabel.value.trim()
  if (!city || city === '未定位' || city === '定位失败') return ''
  return city
})
const loadingMaskText = computed(() => {
  if (isLoadingMap.value) return '地图加载中...'
  if (holyTourStore.loading) return '卫星信号扫描中...'
  if (isLocating.value) return '定位中...'
  return ''
})
const shouldShowLoadingMask = computed(() => isLoadingMap.value || holyTourStore.loading)

const buildSpotQuery = (spot: RecommendedSpot): string => {
  if (spot.query?.trim()) return spot.query.trim()

  const province = String(spot.province || '').trim()
  const city = normalizeCityKeyword(spot.city || destinationCityForSpots.value || endInput.value.trim())
  const segments = Array.from(new Set([province, city, spot.name].filter(Boolean)))
  if (!segments.length) return spot.name
  return segments.join('')
}

const resetSelectedDestinationSpot = () => {
  selectedDestinationSpot.value = null
  selectedDestinationKeyword.value = ''
}

const resolveCityFromResult = (result: any): string => {
  const city = String(result?.addressComponent?.city || '').trim()
  if (city) return city

  const district = String(result?.addressComponent?.district || '').trim()
  if (district) return district

  const province = String(result?.addressComponent?.province || '').trim()
  return province
}

const clearMarkers = () => {
  if (!mapInstance) return

  if (driving && typeof driving.clear === 'function') {
    driving.clear()
  }

  if (startMarker) {
    mapInstance.remove(startMarker)
    startMarker = null
  }

  if (endMarker) {
    mapInstance.remove(endMarker)
    endMarker = null
  }

  if (spotPreviewMarker) {
    mapInstance.remove(spotPreviewMarker)
    spotPreviewMarker = null
  }

  if (fallbackPolyline) {
    mapInstance.remove(fallbackPolyline)
    fallbackPolyline = null
  }
}

const ensureGeocoder = () => {
  if (!AMap || !mapInstance) return null

  if (!geocoder) {
    geocoder = new AMap.Geocoder({ city: '全国' })
  }

  return geocoder
}

const geocodeAddress = async (keyword: string): Promise<LngLatTuple> => {
  const instance = ensureGeocoder()
  if (!instance) {
    throw new Error('地理编码器未初始化')
  }

  const normalized = keyword.trim()
  if (!normalized) {
    throw new Error('地点不能为空')
  }

  return new Promise((resolve, reject) => {
    instance.getLocation(normalized, (status: string, result: any) => {
      if (status !== 'complete' || !result?.geocodes?.length) {
        reject(new Error(`无法解析地点：${normalized}`))
        return
      }

      const location = result.geocodes[0].location
      const lng = Number(location?.lng)
      const lat = Number(location?.lat)

      if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
        reject(new Error(`地点坐标无效：${normalized}`))
        return
      }

      resolve([lng, lat])
    })
  })
}

const withTimeout = async <T>(task: Promise<T>, timeoutMs: number, message: string): Promise<T> => {
  return await Promise.race([
    task,
    new Promise<T>((_, reject) => {
      window.setTimeout(() => reject(new Error(message)), timeoutMs)
    }),
  ])
}

const formatLngLat = (position: LngLatTuple): string => `${position[0]},${position[1]}`

const parseStepPolyline = (polylineText: string): LngLatTuple[] => {
  return polylineText
    .split(';')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => {
      const [lngText, latText] = item.split(',')
      const lng = Number(lngText)
      const lat = Number(latText)
      return [lng, lat] as LngLatTuple
    })
    .filter(([lng, lat]) => Number.isFinite(lng) && Number.isFinite(lat))
}

const drawRoutePolyline = (path: LngLatTuple[]) => {
  if (!AMap || !mapInstance || path.length < 2) return

  if (fallbackPolyline) {
    mapInstance.remove(fallbackPolyline)
    fallbackPolyline = null
  }

  fallbackPolyline = new AMap.Polyline({
    path,
    strokeColor: activeRouteThemeColor.value,
    strokeOpacity: 0.95,
    strokeWeight: 7,
    isOutline: true,
    outlineColor: 'rgba(255,255,255,0.22)',
    borderWeight: 2,
    lineJoin: 'round',
    lineCap: 'round',
  })

  mapInstance.add(fallbackPolyline)
  mapInstance.setFitView([startMarker, endMarker, fallbackPolyline])
}

const requestDrivingPathByRest = async (startPosition: LngLatTuple, endPosition: LngLatTuple): Promise<LngLatTuple[]> => {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), 25000)

  try {
    // 通过后端代理调用高德路线规划API
    const params = new URLSearchParams({
      origin: formatLngLat(startPosition),
      destination: formatLngLat(endPosition),
      strategy: '0',
    })

    const response = await fetch(`/api/amap/driving?${params}`, { signal: controller.signal })
    if (!response.ok) {
      throw new Error(`路线服务请求失败(${response.status})`)
    }

    const data = (await response.json()) as DrivingRestResponse
    if (data.status !== '1') {
      const info = data.info || '未知错误'
      const code = data.infocode ? `(${data.infocode})` : ''
      throw new Error(`高德路线服务失败：${info}${code}`)
    }

    const steps = data.route?.paths?.[0]?.steps || []
    const path: LngLatTuple[] = []

    steps.forEach((step) => {
      const points = parseStepPolyline(String(step.polyline || ''))
      points.forEach((point) => {
        const previous = path[path.length - 1]
        if (!previous || previous[0] !== point[0] || previous[1] !== point[1]) {
          path.push(point)
        }
      })
    })

    if (path.length < 2) {
      throw new Error('高德路线服务返回空路径')
    }

    return path
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('高德路线服务超时')
    }
    throw error
  } finally {
    window.clearTimeout(timer)
  }
}

const planRouteByService = async (startPosition: LngLatTuple, endPosition: LngLatTuple) => {
  // 优先使用后端代理调用高德路线规划
  try {
    const path = await requestDrivingPathByRest(startPosition, endPosition)
    drawRoutePolyline(path)
    return
  } catch (error) {
    console.warn('后端代理路线规划失败，尝试使用JS API:', error)
  }

  // 降级使用高德JS API
  if (!driving) {
    throw new Error('驾车规划服务未初始化')
  }

  await withTimeout(
    new Promise<void>((resolve, reject) => {
      driving.search(startPosition, endPosition, (status: string, result: any) => {
        if (status === 'complete') {
          resolve()
          return
        }

        reject(new Error(result?.info || '路线规划失败'))
      })
    }),
    30000,
    '高德驾车规划超时',
  )
}

const drawFallbackLine = (startPosition: LngLatTuple, endPosition: LngLatTuple) => {
  if (!AMap || !mapInstance) return

  if (fallbackPolyline) {
    mapInstance.remove(fallbackPolyline)
    fallbackPolyline = null
  }

  fallbackPolyline = new AMap.Polyline({
    path: [startPosition, endPosition],
    strokeColor: activeRouteThemeColor.value,
    strokeOpacity: 0.92,
    strokeWeight: 6,
    isOutline: true,
    outlineColor: 'rgba(255,255,255,0.2)',
    borderWeight: 2,
    lineJoin: 'round',
    lineCap: 'round',
  })

  mapInstance.add(fallbackPolyline)
  mapInstance.setFitView([startMarker, endMarker, fallbackPolyline])
}

const locateCurrentPosition = () => {
  if (!AMap || !mapInstance) return

  isLocating.value = true

  geolocationPlugin = new AMap.Geolocation({
    enableHighAccuracy: true,
    timeout: 10000,
    zoomToAccuracy: true,
    showButton: false,
  })

  mapInstance.addControl(geolocationPlugin)
  geolocationPlugin.getCurrentPosition((status: string, result: any) => {
    isLocating.value = false

    if (status !== 'complete') {
      errorMessage.value = '定位失败，请检查浏览器定位权限'
      const fallbackCity = userStore.userInfo.city || '定位失败'
      currentCityLabel.value = fallbackCity
      if (!startInput.value.trim() && userStore.userInfo.city) {
        startInput.value = userStore.userInfo.city
      }
      return
    }

    const position = result?.position
    const lng = Number(position?.lng)
    const lat = Number(position?.lat)

    if (Number.isFinite(lng) && Number.isFinite(lat)) {
      locatedPoint.value = [lng, lat]
      mapInstance.setCenter([lng, lat])
      mapInstance.setZoom(12)
    }

    const city = resolveCityFromResult(result) || userStore.userInfo.city || '已定位'
    currentCityLabel.value = city
    userStore.setLocalCity(city)
    startInput.value = city
  })
}

const applySelectedCities = () => {
  if (!selectedCities.value.length) return

  resetSelectedDestinationSpot()

  if (selectedCities.value.length === 1) {
    endInput.value = selectedCities.value[0]
    if (!startInput.value.trim() && currentCityLabel.value && currentCityLabel.value !== '未定位') {
      startInput.value = currentCityLabel.value
    }
    return
  }

  startInput.value = selectedCities.value[0]
  endInput.value = selectedCities.value[1]
}

const toggleSelectCity = (city: string) => {
  const index = selectedCities.value.indexOf(city)

  if (index >= 0) {
    selectedCities.value.splice(index, 1)
    applySelectedCities()
    return
  }

  if (selectedCities.value.length >= 2) {
    errorMessage.value = '最多勾选两个地点（起点和终点）'
    return
  }

  selectedCities.value.push(city)
  applySelectedCities()
}

const fillStartWithLocatedCity = () => {
  if (!currentCityLabel.value || currentCityLabel.value === '未定位') {
    errorMessage.value = '当前还没有定位成功，请先等待自动定位'
    return
  }

  startInput.value = currentCityLabel.value
}

const resolveSpotPosition = async (spot: RecommendedSpot): Promise<LngLatTuple> => {
  if (Number.isFinite(spot.lng) && Number.isFinite(spot.lat)) {
    return [Number(spot.lng), Number(spot.lat)]
  }

  return await geocodeAddress(buildSpotQuery(spot))
}

const resolveKeywordPosition = async (keyword: string, timeoutMessage: string): Promise<LngLatTuple> => {
  const normalizedKeyword = normalizeCityKeyword(keyword)
  return await withTimeout(geocodeAddress(normalizedKeyword), 15000, timeoutMessage)
}

const resolveStartPosition = async (startText: string): Promise<LngLatTuple> => {
  if (locatedPoint.value && (startText === currentCityLabel.value || startText === userStore.userInfo.city)) {
    return locatedPoint.value
  }

  return await resolveKeywordPosition(startText, '起点解析超时')
}

const resolveEndPosition = async (endText: string): Promise<LngLatTuple> => {
  const selectedSpot = selectedDestinationSpot.value
  const selectedKeyword = selectedDestinationKeyword.value

  if (selectedSpot && selectedKeyword && endText === selectedKeyword) {
    if (Number.isFinite(selectedSpot.lng) && Number.isFinite(selectedSpot.lat)) {
      return [Number(selectedSpot.lng), Number(selectedSpot.lat)]
    }

    const candidateKeywords = Array.from(
      new Set([selectedSpot.query?.trim() || '', buildSpotQuery(selectedSpot).trim(), endText].filter(Boolean)),
    )

    let lastError: unknown = null
    for (const candidate of candidateKeywords) {
      try {
        return await resolveKeywordPosition(candidate, '终点解析超时')
      } catch (error) {
        lastError = error
      }
    }

    throw lastError instanceof Error ? lastError : new Error('终点解析超时')
  }

  return await resolveKeywordPosition(endText, '终点解析超时')
}

const previewRecommendedSpot = async (spot: RecommendedSpot) => {
  if (!AMap || !mapInstance) return

  try {
    const position = await resolveSpotPosition(spot)

    if (spotPreviewMarker) {
      mapInstance.remove(spotPreviewMarker)
      spotPreviewMarker = null
    }

    spotPreviewMarker = new AMap.Marker({
      position,
      title: `推荐景点：${spot.name}`,
      label: {
        content: '景',
        direction: 'top',
      },
      offset: new AMap.Pixel(-12, -12),
    })

    mapInstance.add(spotPreviewMarker)
    mapInstance.setZoomAndCenter(13, position)
  } catch (error) {
    console.error('景点预览失败:', error)
    errorMessage.value = `无法定位景点：${spot.name}`
  }
}

const chooseSpotAsDestination = (spot: RecommendedSpot) => {
  const keyword = buildSpotQuery(spot).trim()
  if (!keyword) {
    errorMessage.value = '请先选择终点城市后再选景点'
    return
  }

  selectedDestinationSpot.value = spot
  selectedDestinationKeyword.value = keyword
  endInput.value = keyword
  errorMessage.value = ''
  void previewRecommendedSpot(spot)

  if (startInput.value.trim() && !isPlanning.value) {
    void planStartToEnd()
  }
}

const autoPlanPreferredRoute = () => {
  const firstSpot = recommendedSpots.value[0]
  const locatedCity = currentCityLabel.value.trim()

  if (!holyTourStore.preferredRoute || !firstSpot || !locatedPoint.value || !locatedCity || isLocating.value) {
    return
  }

  const routeSignature = `${holyTourStore.preferredRoute.routeName}:${firstSpot.name}:${locatedCity}`
  if (autoPlannedRouteSignature.value === routeSignature) {
    return
  }

  autoPlannedRouteSignature.value = routeSignature
  startInput.value = locatedCity
  chooseSpotAsDestination(firstSpot)
}

const planStartToEnd = async () => {
  if (!AMap || !mapInstance) return
  if (isPlanning.value) return

  const startText = startInput.value.trim() || userStore.userInfo.city || currentCityLabel.value
  const endText = endInput.value.trim()

  if (!startText || !endText) {
    errorMessage.value = '请先输入起点和终点'
    return
  }

  if (startText === endText) {
    errorMessage.value = '起点和终点不能相同'
    return
  }

  startInput.value = startText

  errorMessage.value = ''
  isPlanning.value = true

  try {
    const startPosition = await resolveStartPosition(startText)
    const endPosition = await resolveEndPosition(endText)

    clearMarkers()

    startMarker = new AMap.Marker({
      position: startPosition,
      title: `起点：${startText}`,
      label: {
        content: '起',
        direction: 'top',
      },
      offset: new AMap.Pixel(-12, -12),
    })

    endMarker = new AMap.Marker({
      position: endPosition,
      title: `终点：${endText}`,
      label: {
        content: '终',
        direction: 'top',
      },
      offset: new AMap.Pixel(-12, -12),
    })

    mapInstance.add([startMarker, endMarker])

    await planRouteByService(startPosition, endPosition)

    plannedStart.value = startText
    plannedEnd.value = endText
  } catch (error) {
    console.error('起终点路线规划失败:', error)
    const rawMessage = error instanceof Error ? error.message : ''
    const timeoutMessage = rawMessage.includes('终点解析超时')
      ? '终点解析超时，请输入更完整的地名（如“南京市新街口”）后重试'
      : rawMessage.includes('起点解析超时')
        ? '起点解析超时，请输入更完整的地名后重试'
        : rawMessage.includes('高德路线服务超时')
          ? '高德路线服务超时，正在切换为直线预览'
          : rawMessage.includes('高德驾车规划超时')
            ? '高德驾车规划超时，正在切换为直线预览'
            : ''
    const serviceMessage =
      rawMessage.includes('高德路线服务失败') || rawMessage.includes('路线服务请求失败')
        ? rawMessage
        : rawMessage.includes('路线规划失败') || rawMessage.includes('驾车规划服务未初始化')
          ? rawMessage
          : ''

    try {
      const startPosition = await resolveStartPosition(startText)
      const endPosition = await resolveEndPosition(endText)
      drawFallbackLine(startPosition, endPosition)
      plannedStart.value = startText
      plannedEnd.value = endText
      errorMessage.value = timeoutMessage || serviceMessage || '高德驾车规划响应慢，已切换为直线预览模式'
    } catch (fallbackError) {
      console.error('降级直线预览失败:', fallbackError)
      errorMessage.value = timeoutMessage || serviceMessage || '路线规划失败，请确认起点和终点名称是否可识别'
    }
  } finally {
    isPlanning.value = false
  }
}

const fetchSmartSpotsIfNeeded = async (city: string) => {
  const normalizedCity = normalizeCityKey(city)
  const userId = String(userStore.currentUserId || '').trim()

  if (!normalizedCity || !userStore.isAuthenticated || !userId) {
    return
  }

  if (holyTourStore.preferredRoute) {
    return
  }

  if (holyTourStore.spotsByCity[normalizedCity]) {
    return
  }

  await holyTourStore.bootstrapHolyAgent(userId)
  await holyTourStore.fetchCitySpots(normalizedCity, userId)
}

const initMap = async () => {
  if (!mapContainerRef.value) return

  isLoadingMap.value = true
  errorMessage.value = ''

  try {
    // 从后端获取高德Key并加载地图
    AMap = (await loadAmap({
      version: '2.0',
      plugins: ['AMap.Geolocation', 'AMap.Geocoder', 'AMap.Driving', 'AMap.Polyline', 'AMap.ToolBar', 'AMap.Scale'],
    })) as AMapModule

    mapInstance = new AMap.Map(mapContainerRef.value, {
      viewMode: '3D',
      zoom: 11,
      center: defaultCenter,
      resizeEnable: true,
      mapStyle: 'amap://styles/dark',
      pitch: 22,
      zooms: [3, 20],
    })

    mapInstance.addControl(new (AMap as any).ToolBar())
    mapInstance.addControl(new (AMap as any).Scale())

    geocoder = new AMap.Geocoder({ city: '全国' })
    driving = new AMap.Driving({ map: mapInstance, autoFitView: true })

    locateCurrentPosition()
  } catch (error) {
    console.error('高德地图初始化失败:', error)
    errorMessage.value = '高德地图加载失败，请检查 Key 或网络状态'
  } finally {
    isLoadingMap.value = false
  }
}

watch(endInput, (value) => {
  if (!selectedDestinationSpot.value) return
  if (value.trim() !== selectedDestinationKeyword.value) {
    resetSelectedDestinationSpot()
  }
})

watch(
  () => [Boolean(locatedPoint.value), currentCityLabel.value, holyTourStore.preferredRoute?.routeName || '', recommendedSpots.value[0]?.name || ''] as const,
  ([located]) => {
    if (located) {
      autoPlanPreferredRoute()
    }
  },
  { immediate: true },
)

watch(
  destinationCityForSpots,
  (city) => {
    if (!city) return
    void fetchSmartSpotsIfNeeded(city)
  },
  { immediate: true },
)

onMounted(() => {
  if (!startInput.value.trim() && userStore.userInfo.city) {
    startInput.value = userStore.userInfo.city
    currentCityLabel.value = userStore.userInfo.city
  }

  if (destinationCityForSpots.value) {
    void fetchSmartSpotsIfNeeded(destinationCityForSpots.value)
  }

  void initMap()
})

onBeforeUnmount(() => {
  clearMarkers()

  if (driving && typeof driving.clear === 'function') {
    driving.clear()
  }

  if (mapInstance && typeof mapInstance.destroy === 'function') {
    mapInstance.destroy()
  }

  geolocationPlugin = null
  geocoder = null
  driving = null
  spotPreviewMarker = null
  fallbackPolyline = null
  mapInstance = null
  AMap = null
})
</script>

<template>
  <div class="travel-view">
    <div ref="mapContainerRef" class="map-layer" />

    <section class="planner-panel" :style="{ '--route-theme-color': activeRouteThemeColor }">
      <div class="panel-head">
        <div>
          <p class="panel-tag">圣地巡游 · 路线规划</p>
          <h2>起终点路线规划</h2>
        </div>
        <span class="source-pill">数据来源：{{ dataSourceLabel }}</span>
      </div>
      <p class="panel-desc">登录后会自动定位“圣地巡游”智能体并尝试拉取城市路线 JSON。推荐区优先显示智能体结果，失败时自动回退本地数据。</p>

      <div class="field-block">
        <div class="field-label-row">
          <label for="start">起点</label>
          <span v-if="locatedStartLabel" class="located-pill">已定位：{{ locatedStartLabel }}</span>
        </div>
        <div class="input-row">
          <input id="start" v-model="startInput" type="text" placeholder="例如：杭州" />
          <button type="button" class="ghost-btn" @click="fillStartWithLocatedCity">用定位点</button>
        </div>
      </div>

      <div class="field-block">
        <label for="end">终点</label>
        <input id="end" v-model="endInput" type="text" placeholder="例如：南京 / 西安市大雁塔" />
      </div>

      <div class="field-block">
        <label>快速勾选地点（最多 2 个）</label>
        <div class="city-grid">
          <button
            v-for="city in QUICK_CITIES"
            :key="city"
            type="button"
            class="city-chip"
            :class="{ 'city-chip--active': selectedCities.includes(city) }"
            @click="toggleSelectCity(city)"
          >
            {{ city }}
          </button>
        </div>
      </div>

      <button type="button" class="primary-btn" :disabled="!canPlanRoute" @click="planStartToEnd">
        {{ isPlanning ? '规划中...' : '展示起点到终点' }}
      </button>

      <p v-if="errorMessage" class="feedback feedback--error">{{ errorMessage }}</p>
      <p v-if="smartFallbackMessage" class="feedback feedback--warn">{{ smartFallbackMessage }}</p>

      <div class="status-box">
        <p><span>定位城市：</span>{{ isLocating ? '定位中...' : currentCityLabel }}</p>
        <p><span>推荐城市：</span>{{ destinationCityForSpots || '等待输入终点城市' }}</p>
        <p><span>已选景点：</span>{{ selectedSpotLabel }}</p>
        <p><span>当前起点：</span>{{ plannedStart || startInput || '-' }}</p>
        <p><span>当前终点：</span>{{ plannedEnd || endInput || '-' }}</p>
      </div>
    </section>

    <section v-if="activeStorySpot" class="hero-story-card" :style="{ '--route-theme-color': activeRouteThemeColor }">
      <div class="hero-story-head">
        <div>
          <p class="panel-tag">英雄坐标注记</p>
          <h3>{{ activeHeroName }}</h3>
        </div>
        <span class="source-pill">本地定位：{{ currentCityLabel }}</span>
      </div>
      <p class="hero-story-place">
        {{ activeStorySpot.name }}
        <span v-if="activeStorySpot.province || activeStorySpot.city">
          · {{ [activeStorySpot.province, activeStorySpot.city].filter(Boolean).join(' / ') }}
        </span>
      </p>
      <p class="hero-story-text">{{ activeStorySpot.story || activeStorySpot.summary }}</p>
    </section>

    <aside class="spots-panel" :style="{ '--route-theme-color': activeRouteThemeColor }">
      <div class="spots-head">
        <div>
          <p class="panel-tag">圣地巡游推荐区</p>
          <h3>{{ activeRouteTitle }}</h3>
        </div>
        <span class="source-pill">{{ dataSourceLabel }}</span>
      </div>
      <p class="spots-desc">切换终点城市或进入页面时，会自动尝试调用“圣地巡游”智能体；点击任一景点可预览点位并填充终点进行路线规划。</p>

      <div v-if="recommendedSpots.length" class="spots-list">
        <button
          v-for="spot in recommendedSpots"
          :key="`${spot.city || destinationCityForSpots}-${spot.name}`"
          type="button"
          class="spot-item"
          @click="chooseSpotAsDestination(spot)"
        >
          <div class="spot-thumb">
            <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name" />
            <div v-else class="spot-thumb-placeholder">暂无图片</div>
          </div>
          <div class="spot-meta">
            <strong>{{ spot.name }}</strong>
            <span class="spot-city">{{ spot.city || destinationCityForSpots || '待定城市' }}</span>
            <span>{{ spot.summary }}</span>
          </div>
        </button>
      </div>
      <div v-else class="spots-empty">先填写终点城市，或从左侧快速勾选城市。</div>
    </aside>

    <div v-if="shouldShowLoadingMask" class="loading-mask">{{ loadingMaskText || '卫星信号扫描中...' }}</div>
  </div>
</template>

<style scoped>
.travel-view {
  position: relative;
  width: 100%;
  height: calc(100vh - var(--navbar-height));
  min-height: calc(100vh - var(--navbar-height));
  overflow: hidden;
  background: var(--bg-primary);
}

.map-layer {
  position: absolute;
  inset: 0;
}

.planner-panel,
.spots-panel,
.hero-story-card {
  position: absolute;
  z-index: 4;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(14px);
}

.planner-panel {
  top: 20px;
  left: 20px;
  width: min(440px, calc(100vw - 40px));
  padding: 18px;
}

.spots-panel {
  top: 20px;
  right: 20px;
  width: min(380px, calc(100vw - 40px));
  max-height: calc(100vh - 160px);
  overflow: auto;
  padding: 16px;
}

.hero-story-card {
  position: absolute;
  left: 20px;
  bottom: 20px;
  z-index: 4;
  width: min(420px, calc(100vw - 40px));
  padding: 16px 18px;
}

.hero-story-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.hero-story-head h3 {
  margin: 0;
  color: var(--accent-cyan);
  font-size: 22px;
}

.hero-story-place {
  margin: 14px 0 8px;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.45;
}

.hero-story-place span {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.hero-story-text {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.panel-head,
.spots-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-tag {
  margin: 0 0 8px;
  color: var(--accent-cyan);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.panel-head h2,
.spots-head h3 {
  margin: 0;
  color: var(--text-primary);
}

.panel-head h2 {
  font-size: 22px;
}

.spots-head h3 {
  font-size: 20px;
}

.source-pill {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.04);
  color: var(--text-secondary);
  font-size: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.panel-desc,
.spots-desc {
  margin: 8px 0 14px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.field-block {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.field-block label {
  font-size: 13px;
  color: var(--text-secondary);
}

.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.located-pill {
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 999px;
  padding: 4px 9px;
  background: rgba(59, 130, 246, 0.06);
  color: var(--accent-cyan);
  font-size: 12px;
}

.input-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

input {
  width: 100%;
  height: 40px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-primary);
  padding: 0 12px;
  font-size: 14px;
}

input:focus {
  outline: none;
  border-color: var(--accent-cyan);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.city-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.city-chip {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.6);
  color: var(--text-secondary);
  padding: 6px 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.city-chip:hover {
  border-color: rgba(59, 130, 246, 0.2);
  background: rgba(59, 130, 246, 0.04);
}

.city-chip--active {
  border-color: var(--accent-cyan);
  color: var(--accent-cyan);
  background: rgba(59, 130, 246, 0.08);
}

.primary-btn,
.ghost-btn {
  height: 40px;
  border-radius: 10px;
  cursor: pointer;
}

.primary-btn {
  width: 100%;
  border: none;
  background: linear-gradient(135deg, var(--accent-cyan), #60A5FA);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.25);
}

.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ghost-btn {
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.6);
  color: var(--text-secondary);
  padding: 0 12px;
}

.ghost-btn:hover {
  border-color: rgba(59, 130, 246, 0.2);
  color: var(--accent-cyan);
}

.feedback {
  margin: 12px 0 0;
  font-size: 13px;
}

.feedback--error {
  color: #DC2626;
}

.feedback--warn {
  color: #D97706;
}

.status-box {
  margin-top: 14px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.status-box p {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.7;
}

.status-box span {
  color: var(--text-secondary);
}

.spots-list {
  display: grid;
  gap: 12px;
}

.spot-item {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  text-align: left;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.6);
  color: var(--text-primary);
  padding: 10px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.spot-item:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.1);
}

.spot-thumb {
  width: 92px;
  height: 92px;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.04);
}

.spot-thumb img,
.spot-thumb-placeholder {
  width: 100%;
  height: 100%;
}

.spot-thumb img {
  display: block;
  object-fit: cover;
}

.spot-thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.06), rgba(139, 92, 246, 0.06));
  color: var(--text-secondary);
  font-size: 12px;
  text-align: center;
}

.spot-meta {
  display: grid;
  gap: 6px;
}

.spot-meta strong {
  font-size: 15px;
}

.spot-meta span {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.spot-city {
  color: var(--accent-cyan);
}

.spots-empty {
  font-size: 13px;
  color: var(--text-secondary);
  border: 1px dashed rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  padding: 12px;
}

.loading-mask {
  position: absolute;
  inset: 0;
  z-index: 6;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-primary);
  font-size: 18px;
  backdrop-filter: blur(4px);
}

/* ========== 平板适配（≤1024px） ========== */
@media (max-width: 1024px) {
  .planner-panel {
    width: min(380px, calc(100vw - 24px));
    padding: 16px;
  }

  .spots-panel {
    width: min(320px, calc(100vw - 24px));
    padding: 14px;
  }

  .hero-story-card {
    width: min(360px, calc(100vw - 24px));
    padding: 14px 16px;
  }
}

/* ========== 移动端适配（≤768px） ========== */
@media (max-width: 768px) {
  .travel-view {
    height: auto;
    min-height: calc(100vh - var(--navbar-height));
  }

  .map-layer {
    position: relative;
    height: 50vh;
    min-height: 300px;
  }

  .planner-panel {
    position: relative;
    left: 0;
    right: 0;
    top: 0;
    width: 100%;
    margin: 0;
    border-radius: 0 0 16px 16px;
    padding: 16px;
  }

  .input-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .spots-panel {
    position: relative;
    left: 0;
    right: 0;
    top: 0;
    width: 100%;
    max-height: none;
    margin: 12px 0 0;
    border-radius: 16px;
    padding: 14px;
  }

  .hero-story-card {
    position: relative;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    max-height: none;
    margin: 12px 0 0;
    border-radius: 16px;
    padding: 14px 16px;
  }

  .hero-story-head {
    flex-direction: column;
    gap: 8px;
  }

  .spot-item {
    grid-template-columns: 80px 1fr;
    gap: 10px;
    padding: 10px;
  }

  .spot-thumb {
    width: 80px;
    height: 80px;
  }

  .city-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 6px;
  }

  .city-chip {
    padding: 8px 6px;
    font-size: 12px;
    text-align: center;
  }

  .primary-btn {
    min-height: 48px;
  }

  .ghost-btn {
    min-height: 44px;
  }

  .status-box {
    padding: 10px;
  }

  .status-box p {
    font-size: 12px;
  }
}

/* ========== 手机竖屏（≤520px） ========== */
@media (max-width: 520px) {
  .map-layer {
    height: 40vh;
    min-height: 250px;
  }

  .planner-panel {
    padding: 14px 12px;
  }

  .panel-head h2 {
    font-size: 18px;
  }

  .panel-tag {
    font-size: 11px;
  }

  .spots-panel {
    padding: 12px;
  }

  .spots-head h3 {
    font-size: 16px;
  }

  .spot-item {
    grid-template-columns: 1fr;
  }

  .spot-thumb {
    width: 100%;
    height: 120px;
  }

  .city-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .city-chip {
    padding: 10px 8px;
    font-size: 13px;
  }

  .loading-mask {
    font-size: 14px;
  }
}

/* ========== 超小屏幕（≤380px） ========== */
@media (max-width: 380px) {
  .map-layer {
    height: 35vh;
    min-height: 200px;
  }

  .city-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
