import { defineStore } from 'pinia'
import { ref } from 'vue'
import { agentApi } from '@/api/agent'
import { useUserStore } from '@/store/user'
import { toSafeString, toSafeNumber, normalizeCityKey, isHexColor } from '@/utils/common'

const HOLY_AGENT_MATCHER = /(圣地巡游|圣地巡礼|文旅)/i
const DEFAULT_THEME_COLOR = '#00ffee'
const MIN_SPOT_COUNT = 3
const MAX_SPOT_COUNT = 6

export interface HolyTourSpot {
  name: string
  province?: string
  city: string
  lng: number
  lat: number
  story: string
  summary: string
  imageUrl?: string
  query?: string
}

export interface HolyTourRouteData {
  city: string
  routeName: string
  heroName: string
  heroThemeColor: string
  spots: HolyTourSpot[]
}

const extractHeroNameFromRouteName = (routeName: string): string => {
  const normalized = routeName.trim()
  if (!normalized) return ''

  const [heroName] = normalized.split(/[·•｜|]/)
  return heroName?.trim() || ''
}

const extractCodeFenceCandidates = (text: string): string[] => {
  const candidates: string[] = []
  const regex = /```(?:json)?\s*([\s\S]*?)```/gi

  let match: RegExpExecArray | null = regex.exec(text)
  while (match) {
    const candidate = match[1]?.trim()
    if (candidate) {
      candidates.push(candidate)
    }
    match = regex.exec(text)
  }

  return candidates
}

const extractBalancedJsonCandidate = (text: string, startIndex: number): string => {
  const opening = text[startIndex]
  const stack: string[] = [opening]

  let inString = false
  let escaped = false

  for (let index = startIndex + 1; index < text.length; index += 1) {
    const char = text[index]

    if (inString) {
      if (escaped) {
        escaped = false
        continue
      }

      if (char === '\\') {
        escaped = true
        continue
      }

      if (char === '"') {
        inString = false
      }

      continue
    }

    if (char === '"') {
      inString = true
      continue
    }

    if (char === '{' || char === '[') {
      stack.push(char)
      continue
    }

    if (char === '}' || char === ']') {
      const last = stack[stack.length - 1]
      if ((char === '}' && last === '{') || (char === ']' && last === '[')) {
        stack.pop()
        if (!stack.length) {
          return text.slice(startIndex, index + 1)
        }
      }
    }
  }

  return ''
}

const extractInlineJsonCandidates = (text: string): string[] => {
  const candidates: string[] = []

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    if (char !== '{' && char !== '[') continue

    const candidate = extractBalancedJsonCandidate(text, index)
    if (candidate) {
      candidates.push(candidate.trim())
      index += candidate.length - 1
    }
  }

  return candidates
}

const parseJsonCandidate = (text: string): unknown | null => {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

const normalizeSpot = (raw: unknown, fallbackCity: string): HolyTourSpot | null => {
  if (!raw || typeof raw !== 'object') return null

  const source = raw as Record<string, unknown>
  const name = toSafeString(source.name)
  const province = toSafeString(source.province ?? source.state ?? source.region)
  const city = normalizeCityKey(toSafeString(source.city) || fallbackCity)
  const story = toSafeString(source.story ?? source.summary ?? source.description)
  const summary = toSafeString(source.summary ?? source.story ?? source.description) || story
  const imageUrl = toSafeString(source.imageUrl ?? source.image ?? source.cover ?? source.poster)
  const query = toSafeString(source.query)
  const lng = toSafeNumber(source.lng)
  const lat = toSafeNumber(source.lat)

  if (!name || !city || !story || !Number.isFinite(lng) || !Number.isFinite(lat)) {
    return null
  }

  return {
    name,
    province: province || undefined,
    city,
    lng,
    lat,
    story,
    summary,
    imageUrl: imageUrl || undefined,
    query: query || undefined,
  }
}

const normalizeRouteObject = (raw: unknown): HolyTourRouteData | null => {
  if (!raw || typeof raw !== 'object') return null

  const source = raw as Record<string, unknown>
  const routeCity = normalizeCityKey(toSafeString(source.city))
  const rawSpots = Array.isArray(source.spots) ? source.spots : []
  const spots = rawSpots
    .map((spot) => normalizeSpot(spot, routeCity))
    .filter((spot): spot is HolyTourSpot => !!spot)
    .slice(0, MAX_SPOT_COUNT)

  if (spots.length < MIN_SPOT_COUNT) {
    return null
  }

  const city = normalizeCityKey(routeCity || spots[0]?.city || '')
  if (!city) {
    return null
  }

  const routeName = toSafeString(source.routeName) || `${city}·圣地巡游`
  const heroName =
    toSafeString(source.heroName ?? source.hero ?? source.roleName) ||
    extractHeroNameFromRouteName(routeName) ||
    toSafeString(useUserStore().userInfo.mainHeroes[0]) ||
    '未命名英雄'
  const heroThemeColor = toSafeString(source.heroThemeColor)

  return {
    city,
    routeName,
    heroName,
    heroThemeColor: isHexColor(heroThemeColor) ? heroThemeColor : DEFAULT_THEME_COLOR,
    spots,
  }
}

const normalizeRoutePayload = (payload: unknown): HolyTourRouteData | null => {
  if (Array.isArray(payload)) {
    for (const item of payload) {
      const normalized = normalizeRoutePayload(item)
      if (normalized) {
        return normalized
      }
    }
    return null
  }

  if (!payload || typeof payload !== 'object') {
    return null
  }

  const source = payload as Record<string, unknown>
  const nestedArray = source.data ?? source.routes ?? source.items ?? source.list

  if (Array.isArray(nestedArray)) {
    const normalized = normalizeRoutePayload(nestedArray)
    if (normalized) {
      return normalized
    }
  }

  return normalizeRouteObject(source)
}

export const useHolyTourStore = defineStore('holyTour', () => {
  const agentId = ref('')
  const spotsByCity = ref<Record<string, HolyTourRouteData>>({})
  const heroRoutes = ref<Record<string, HolyTourRouteData>>({})
  const preferredHero = ref('')
  const preferredRoute = ref<HolyTourRouteData | null>(null)
  const loading = ref(false)
  const error = ref('')

  const buildHolyTourPrompt = (heroName: string, city = ''): string => {
    const normalizedHeroName = toSafeString(heroName) || '未设置'
    const normalizedCity = normalizeCityKey(city)

    return [
      '你是“圣地巡游”智能体，请只返回 JSON，不要返回解释文本、markdown 或代码块。',
      `用户本命英雄第一位：${normalizedHeroName}`,
      normalizedCity ? `用户当前定位城市：${normalizedCity}` : '',
      '请围绕该英雄生成一条可巡游的现实征途路线，第一站要能作为用户进入地图后的默认终点。',
      '请输出单个 JSON 对象，或输出 JSON 数组（如果是数组，第一组必须是主推荐路线）。',
      '固定 JSON 协议如下：',
      '{',
      '  "routeName": "韩信·淮阴侯的征途",',
      '  "heroName": "韩信",',
      '  "heroThemeColor": "#FFB90F",',
      '  "city": "淮安市",',
      '  "spots": [',
      '    {',
      '      "name": "淮阴区",',
      '      "province": "江苏省",',
      '      "city": "淮安市",',
      '      "lng": 119.15,',
      '      "lat": 33.50,',
      '      "story": "这里是英雄羁绊故事",',
      '      "summary": "可选摘要，没有可省略",',
      '      "imageUrl": "https://...",',
      '      "query": "江苏省淮安市淮阴区"',
      '    }',
      '  ]',
      '}',
      '约束：',
      '1. routeName、heroThemeColor、spots 必填；',
      '2. spots 必填数组，长度 3-6；',
      '3. 每项 name、city、lng、lat、story 必填；province、summary、imageUrl、query 选填；',
      '4. 返回内容必须是合法 JSON，不要附带任何解释文本。',
    ].filter(Boolean).join('\n')
  }

  const bootstrapHolyAgent = async (userId: string): Promise<string> => {
    const normalizedUserId = toSafeString(userId)
    if (!normalizedUserId) {
      error.value = '未获取到当前登录用户，暂时无法初始化圣地巡游智能体'
      return ''
    }

    if (agentId.value) {
      return agentId.value
    }

    loading.value = true
    error.value = ''

    try {
      const agents = await agentApi.fetchAvailableAgents(normalizedUserId)
      const matched = agents.find((agent) => HOLY_AGENT_MATCHER.test(`${agent.name} ${agent.description || ''}`))

      if (!matched?.agentId) {
        error.value = '未找到“圣地巡游”相关智能体，将回退到本地推荐数据'
        return ''
      }

      agentId.value = matched.agentId
      return matched.agentId
    } catch (err) {
      console.error('初始化圣地巡游智能体失败:', err)
      error.value = '圣地巡游智能体初始化失败，将回退到本地推荐数据'
      return ''
    } finally {
      loading.value = false
    }
  }

  const parseAgentJson = (text: string): HolyTourRouteData | null => {
    const rawText = text.trim()
    if (!rawText) {
      return null
    }

    const candidates = Array.from(
      new Set([
        rawText,
        ...extractCodeFenceCandidates(rawText),
        ...extractInlineJsonCandidates(rawText),
      ].map((item) => item.trim()).filter(Boolean)),
    )

    for (const candidate of candidates) {
      const parsed = parseJsonCandidate(candidate)
      if (!parsed) continue

      const normalized = normalizeRoutePayload(parsed)
      if (normalized) {
        return normalized
      }
    }

    return null
  }

  const cacheRouteByCities = (route: HolyTourRouteData, aliasKeys: string[] = []) => {
    const merged = { ...spotsByCity.value }

    aliasKeys.forEach((key) => {
      const normalizedKey = normalizeCityKey(key)
      if (normalizedKey) {
        merged[normalizedKey] = route
      }
    })

    route.spots.forEach((spot) => {
      const normalizedKey = normalizeCityKey(spot.city)
      if (normalizedKey) {
        merged[normalizedKey] = route
      }
    })

    const routeCityKey = normalizeCityKey(route.city)
    if (routeCityKey) {
      merged[routeCityKey] = route
    }

    spotsByCity.value = merged
  }

  const fetchHeroRoute = async (heroName: string, userId: string, city = ''): Promise<HolyTourRouteData | null> => {
    const normalizedHeroName = toSafeString(heroName)
    const normalizedUserId = toSafeString(userId)
    const normalizedCity = normalizeCityKey(city)

    if (!normalizedHeroName) {
      error.value = '未获取到本命英雄第一位，无法请求圣地巡游路线'
      return null
    }

    if (!normalizedUserId) {
      error.value = '未获取到当前登录用户，无法请求智能体数据'
      return null
    }

    if (heroRoutes.value[normalizedHeroName]) {
      preferredHero.value = normalizedHeroName
      preferredRoute.value = heroRoutes.value[normalizedHeroName]
      error.value = ''
      return heroRoutes.value[normalizedHeroName]
    }

    loading.value = true
    error.value = ''

    try {
      const resolvedAgentId = agentId.value || (await bootstrapHolyAgent(normalizedUserId))
      if (!resolvedAgentId) {
        return null
      }

      const replyText = await agentApi.executeAgentUse({
        agentId: resolvedAgentId,
        userId: normalizedUserId,
        content: buildHolyTourPrompt(normalizedHeroName, normalizedCity),
      })

      const parsed = parseAgentJson(replyText)
      if (!parsed) {
        error.value = '智能体未返回可解析的英雄巡游 JSON，已回退本地推荐数据'
        return null
      }

      const route: HolyTourRouteData = {
        ...parsed,
        heroName: parsed.heroName || normalizedHeroName,
      }

      heroRoutes.value = {
        ...heroRoutes.value,
        [normalizedHeroName]: route,
      }
      preferredHero.value = normalizedHeroName
      preferredRoute.value = route
      cacheRouteByCities(route, normalizedCity ? [normalizedCity] : [])
      error.value = ''
      return route
    } catch (err) {
      console.error('获取英雄巡游推荐失败:', err)
      error.value = '智能体英雄路线生成失败，当前已回退到本地推荐数据'
      return null
    } finally {
      loading.value = false
    }
  }

  const fetchPrimaryHeroRoute = async (userId: string, city = ''): Promise<HolyTourRouteData | null> => {
    const userStore = useUserStore()
    const primaryHero = toSafeString(userStore.userInfo.mainHeroes[0])

    if (!primaryHero) {
      error.value = '当前用户还没有设置本命英雄，暂时无法预加载英雄巡游路线'
      return null
    }

    return await fetchHeroRoute(primaryHero, userId, city)
  }

  const fetchCitySpots = async (city: string, userId: string): Promise<HolyTourRouteData | null> => {
    const normalizedCity = normalizeCityKey(city)
    const normalizedUserId = toSafeString(userId)
    const userStore = useUserStore()
    const primaryHero = toSafeString(userStore.userInfo.mainHeroes[0])

    if (!normalizedCity) {
      error.value = '城市不能为空，无法获取圣地巡游推荐'
      return null
    }

    if (!normalizedUserId) {
      error.value = '未获取到当前登录用户，无法请求智能体数据'
      return null
    }

    if (spotsByCity.value[normalizedCity]) {
      error.value = ''
      return spotsByCity.value[normalizedCity]
    }

    loading.value = true
    error.value = ''

    try {
      const resolvedAgentId = agentId.value || (await bootstrapHolyAgent(normalizedUserId))
      if (!resolvedAgentId) {
        return null
      }

      const replyText = await agentApi.executeAgentUse({
        agentId: resolvedAgentId,
        userId: normalizedUserId,
        content: buildHolyTourPrompt(primaryHero || '未设置', normalizedCity),
      })

      const parsed = parseAgentJson(replyText)
      if (!parsed) {
        error.value = '智能体未返回可解析的圣地巡游 JSON，已回退本地推荐数据'
        return null
      }

      cacheRouteByCities(parsed, [normalizedCity])
      if (!preferredRoute.value && primaryHero) {
        preferredHero.value = primaryHero
        preferredRoute.value = parsed
      }
      error.value = ''
      return parsed
    } catch (err) {
      console.error('获取圣地巡游推荐失败:', err)
      error.value = '智能体路线生成失败，当前已回退到本地推荐数据'
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    agentId,
    spotsByCity,
    heroRoutes,
    preferredHero,
    preferredRoute,
    loading,
    error,
    bootstrapHolyAgent,
    fetchHeroRoute,
    fetchPrimaryHeroRoute,
    fetchCitySpots,
    parseAgentJson,
  }
})
