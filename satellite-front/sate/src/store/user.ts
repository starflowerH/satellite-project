/**
 * src/store/user.ts - 用户状态管理
 * 对接真实后端API，支持 token 持久化与用户资料同步
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { ResetPasswordDTO } from '@/api/auth'
import { heroApi } from '@/api/hero'
import { userApi } from '@/api/user'
import type { UpdateUserDTO, UserInfoVO } from '@/api/user'
import { toSafeString as safeStr } from '@/utils/common'

const TOKEN_KEY = 'access_token'
const USER_ID_KEY = 'user_id'
const SESSION_AUTH_KEY = 'session_authenticated'
const CITY_KEY = 'user_city'
const HERO_LIMIT = 5

const createAvatar = (seed: string): string => {
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(seed || 'satellite')}`
}

// 使用公共工具函数的别名
const toSafeString = safeStr

const normalizeStringList = (value: unknown): string[] => {
  const rawItems = Array.isArray(value)
    ? value
    : typeof value === 'string'
      ? value.split(/[，,、|]/)
      : []

  const result: string[] = []

  for (const item of rawItems) {
    let text = ''

    if (typeof item === 'string' || typeof item === 'number') {
      text = toSafeString(item)
    } else if (item && typeof item === 'object') {
      const record = item as Record<string, unknown>
      text = toSafeString(record.name ?? record.heroName ?? record.title ?? record.label ?? record.value)
    }

    if (text && !result.includes(text)) {
      result.push(text)
    }

    if (result.length >= HERO_LIMIT) {
      break
    }
  }

  return result
}

const getPersistedUserId = (): string => {
  return localStorage.getItem(USER_ID_KEY) || ''
}

const getPersistedCity = (): string => {
  return localStorage.getItem(CITY_KEY) || ''
}

const createEmptyUserInfo = (): UserInfoVO => ({
  userId: getPersistedUserId(),
  name: '',
  avatar: '',
  signature: '',
  city: getPersistedCity(),
  phone: '',
  email: '',
  mainHeroes: [],
})

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const sessionAuthenticated = ref<boolean>(localStorage.getItem(SESSION_AUTH_KEY) === '1')
  const userInfo = ref<UserInfoVO>(createEmptyUserInfo())
  const profileResolved = ref(false)

  const isAuthenticated = computed(() => !!token.value || sessionAuthenticated.value)

  const currentUserId = computed(() => {
    return userInfo.value.userId || getPersistedUserId()
  })

  const displayName = computed(() => {
    return userInfo.value.name || userInfo.value.phone || userInfo.value.email || '未命名召唤师'
  })

  const displayAvatar = computed(() => {
    return userInfo.value.avatar || createAvatar(displayName.value)
  })

  const needsHeroSelection = computed(() => {
    return isAuthenticated.value && profileResolved.value && userInfo.value.mainHeroes.length === 0
  })

  const user = computed(() => ({
    username: displayName.value,
    avatar: displayAvatar.value,
    city: userInfo.value.city,
    phone: userInfo.value.phone,
    email: userInfo.value.email,
    mainHeroes: userInfo.value.mainHeroes,
    isLoggedin: isAuthenticated.value,
  }))

  const setSessionAuthenticated = (value: boolean) => {
    sessionAuthenticated.value = value
    if (value) {
      localStorage.setItem(SESSION_AUTH_KEY, '1')
      return
    }
    localStorage.removeItem(SESSION_AUTH_KEY)
  }

  const saveToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
    setSessionAuthenticated(true)
  }

  const clearToken = () => {
    token.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  const saveUserId = (userId: string) => {
    if (!userId) return
    userInfo.value.userId = userId
    localStorage.setItem(USER_ID_KEY, userId)
  }

  const clearUserId = () => {
    userInfo.value.userId = ''
    localStorage.removeItem(USER_ID_KEY)
  }

  const saveCity = (city: string) => {
    const normalized = city.trim()
    if (!normalized) return
    userInfo.value.city = normalized
    localStorage.setItem(CITY_KEY, normalized)
  }

  const clearCity = () => {
    userInfo.value.city = ''
    localStorage.removeItem(CITY_KEY)
  }

  const ensureUserId = (): string | null => {
    const userId = currentUserId.value
    if (!userId) {
      console.error('错误：未获取到当前登录用户ID')
      return null
    }
    return userId
  }

  const isRequestSuccess = (response: unknown): boolean => {
    if (response === null || response === undefined) return false
    if (typeof response === 'string') return true

    if (typeof response === 'object') {
      const code = toSafeString((response as Record<string, unknown>).code)
      if (!code) return true
      return ['200', '0', '20000', '201', '1'].includes(code)
    }

    return false
  }

  const extractBusinessData = (response: unknown): Record<string, unknown> | null => {
    if (!response || typeof response !== 'object') return null

    const root = response as Record<string, unknown>
    const rootData = root.data

    if (rootData && typeof rootData === 'object') {
      const nested = (rootData as Record<string, unknown>).data
      if (nested && typeof nested === 'object') {
        return nested as Record<string, unknown>
      }
      return rootData as Record<string, unknown>
    }

    return root
  }

  const toRecord = (value: unknown): Record<string, unknown> | null => {
    if (!value || typeof value !== 'object') return null
    return value as Record<string, unknown>
  }

  const extractTokenFromResponse = (response: unknown): string => {
    if (typeof response === 'string') return response.trim()
    if (!response || typeof response !== 'object') return ''

    const root = response as Record<string, unknown>
    const data = (root.data ?? {}) as Record<string, unknown>
    const nestedData = (data.data ?? {}) as Record<string, unknown>

    const tokenCandidates = [
      root.token,
      root.accessToken,
      root.access_token,
      root.jwt,
      data.token,
      data.accessToken,
      data.access_token,
      data.jwt,
      nestedData.token,
      nestedData.accessToken,
      nestedData.access_token,
      nestedData.jwt,
      typeof data === 'string' ? data : '',
    ]

    for (const candidate of tokenCandidates) {
      const tokenValue = toSafeString(candidate)
      if (tokenValue) return tokenValue
    }

    return ''
  }

  const mergeUserInfo = (payload: Partial<UserInfoVO>) => {
    const merged: UserInfoVO = {
      userId: payload.userId ?? userInfo.value.userId,
      name: payload.name ?? userInfo.value.name,
      avatar: payload.avatar ?? userInfo.value.avatar,
      signature: payload.signature ?? userInfo.value.signature,
      city: payload.city ?? userInfo.value.city,
      phone: payload.phone ?? userInfo.value.phone,
      email: payload.email ?? userInfo.value.email,
      mainHeroes: payload.mainHeroes ?? userInfo.value.mainHeroes,
    }

    userInfo.value = merged
    profileResolved.value = true

    if (merged.userId) {
      saveUserId(merged.userId)
    }
    if (merged.city) {
      saveCity(merged.city)
    }
  }

  const patchUserInfoFromRaw = (raw: Record<string, unknown> | null, fallback: Partial<UserInfoVO> = {}) => {
    const nestedUser = toRecord(raw?.userInfo) || toRecord(raw?.user) || toRecord(raw?.profile)
    const source = nestedUser || raw

    const userId = toSafeString(
      source?.userId ??
        source?.id ??
        source?.uid ??
        source?.user_id ??
        raw?.userId ??
        raw?.id ??
        raw?.uid ??
        raw?.user_id ??
        fallback.userId,
    )
    const name = toSafeString(source?.name ?? source?.username ?? source?.nickname ?? raw?.name ?? raw?.username ?? fallback.name)
    const avatar = toSafeString(source?.avatar ?? raw?.avatar ?? fallback.avatar)
    const signature = toSafeString(source?.signature ?? raw?.signature ?? fallback.signature)
    const city = toSafeString(
      source?.city ??
        source?.cityName ??
        source?.location ??
        source?.region ??
        raw?.city ??
        raw?.cityName ??
        raw?.location ??
        fallback.city ??
        userInfo.value.city ??
        getPersistedCity(),
    )
    const phone = toSafeString(source?.phone ?? raw?.phone ?? fallback.phone)
    const email = toSafeString(source?.email ?? raw?.email ?? fallback.email)

    mergeUserInfo({
      userId,
      name,
      avatar,
      signature,
      city,
      phone,
      email,
      mainHeroes: fallback.mainHeroes ?? userInfo.value.mainHeroes,
    })
  }

  const fetchMainHeroes = async (): Promise<boolean> => {
    const userId = ensureUserId()
    if (!userId) {
      return false
    }

    try {
      const list = await heroApi.listMyHeroes(userId)
      const mainHeroes = normalizeStringList(list.map((item) => item.heroName))
      mergeUserInfo({ mainHeroes })
      return true
    } catch (error) {
      console.error('获取本命英雄失败:', error)
      mergeUserInfo({ mainHeroes: [] })
      return false
    }
  }

  const fetchUserInfo = async (): Promise<boolean> => {
    const userId = ensureUserId()
    if (!userId) {
      return false
    }

    try {
      const response = await userApi.getUserInfo(userId)

      if (!isRequestSuccess(response)) {
        console.error('获取用户信息失败:', response)
        return false
      }

      const data = extractBusinessData(response)
      patchUserInfoFromRaw(data, { userId })
      return true
    } catch (error) {
      console.error('获取用户信息异常:', error)
      return false
    }
  }

  const fetchUserProfile = async (): Promise<boolean> => {
    const profileOk = await fetchUserInfo()
    if (!profileOk) {
      return false
    }

    await fetchMainHeroes()
    return true
  }

  const updateUserProfile = async (
    payload: Partial<Pick<UpdateUserDTO, 'name' | 'avatar' | 'signature'>> & { id?: string; userId?: string },
  ): Promise<boolean> => {
    const userId = payload.userId || payload.id || ensureUserId()
    if (!userId) {
      return false
    }

    try {
      const requestPayload: UpdateUserDTO = {
        id: userId,
        userId,
        name: payload.name ?? userInfo.value.name,
        avatar: payload.avatar ?? userInfo.value.avatar,
        signature: payload.signature ?? userInfo.value.signature,
        phone: userInfo.value.phone,
        email: userInfo.value.email,
      }

      const response = await userApi.updateUserInfo(requestPayload)

      if (!isRequestSuccess(response)) {
        console.error('更新用户信息失败:', response)
        return false
      }

      const data = extractBusinessData(response)
      patchUserInfoFromRaw(data, { ...requestPayload, city: userInfo.value.city, mainHeroes: userInfo.value.mainHeroes })
      return true
    } catch (error) {
      console.error('更新用户信息异常:', error)
      return false
    }
  }

  const saveMainHeroes = async (heroIds: string[]): Promise<boolean> => {
    const userId = ensureUserId()
    if (!userId) {
      return false
    }

    const normalizedHeroIds = normalizeStringList(heroIds)
    if (!normalizedHeroIds.length) {
      return false
    }

    try {
      const response = await heroApi.saveMyHeroes({
        userId,
        heroIds: normalizedHeroIds,
      })

      if (!isRequestSuccess(response)) {
        console.error('保存本命英雄失败:', response)
        return false
      }

      await fetchMainHeroes()
      return true
    } catch (error) {
      console.error('保存本命英雄异常:', error)
      return false
    }
  }


  const hydrateUserFromLogin = (response: unknown, fallback: Partial<UserInfoVO>) => {
    const data = extractBusinessData(response)
    patchUserInfoFromRaw(data, fallback)
  }

  const loginByPassword = async (account: string, password: string): Promise<boolean> => {
    try {
      const response = await authApi.loginByPassword({ phone: account, password })

      if (!isRequestSuccess(response)) {
        console.error('密码登录失败:', response)
        return false
      }

      const tokenValue = extractTokenFromResponse(response)
      if (tokenValue) {
        saveToken(tokenValue)
      }

      const isEmail = account.includes('@')
      hydrateUserFromLogin(response, {
        name: account,
        avatar: createAvatar(account),
        phone: isEmail ? '' : account,
        email: isEmail ? account : '',
        mainHeroes: [],
      })

      setSessionAuthenticated(true)
      await fetchUserProfile()
      return true
    } catch (error) {
      console.error('登录异常:', error)
      return false
    }
  }

  const loginByPhoneCode = async (phone: string, code: string): Promise<boolean> => {
    try {
      const response = await authApi.loginByPhoneCode({ phone, code })

      if (!isRequestSuccess(response)) {
        console.error('手机验证码登录失败:', response)
        return false
      }

      const tokenValue = extractTokenFromResponse(response)
      if (tokenValue) {
        saveToken(tokenValue)
      }

      hydrateUserFromLogin(response, {
        name: phone,
        avatar: createAvatar(phone),
        phone,
        mainHeroes: [],
      })

      setSessionAuthenticated(true)
      await fetchUserProfile()
      return true
    } catch (error) {
      console.error('登录异常:', error)
      return false
    }
  }

  const loginByEmailCode = async (email: string, code: string): Promise<boolean> => {
    try {
      const response = await authApi.loginByEmailCode({ email, code })

      if (!isRequestSuccess(response)) {
        console.error('邮箱验证码登录失败:', response)
        return false
      }

      const tokenValue = extractTokenFromResponse(response)
      if (tokenValue) {
        saveToken(tokenValue)
      }

      hydrateUserFromLogin(response, {
        name: email,
        avatar: createAvatar(email),
        email,
        mainHeroes: [],
      })

      setSessionAuthenticated(true)
      await fetchUserProfile()
      return true
    } catch (error) {
      console.error('登录异常:', error)
      return false
    }
  }

  const register = async (
    account: string,
    password: string,
    code: string,
    isEmail = false,
  ): Promise<boolean> => {
    try {
      const payload: { password: string; code: string; phone?: string; email?: string } = { password, code }
      if (isEmail) {
        payload.email = account
      } else {
        payload.phone = account
      }

      const response = await authApi.register(payload)

      if (!isRequestSuccess(response)) {
        console.error('注册失败:', response)
        return false
      }

      return true
    } catch (error) {
      console.error('注册异常:', error)
      return false
    }
  }

  const resetPassword = async (payload: ResetPasswordDTO): Promise<boolean> => {
    try {
      const response = await authApi.resetPassword(payload)

      if (!isRequestSuccess(response)) {
        console.error('重置密码失败:', response)
        return false
      }

      return true
    } catch (error) {
      console.error('重置密码异常:', error)
      return false
    }
  }

  const bootstrapSession = async (): Promise<void> => {
    const persistedUserId = getPersistedUserId()
    if (persistedUserId && !userInfo.value.userId) {
      userInfo.value.userId = persistedUserId
    }

    if (!token.value && !sessionAuthenticated.value) return
    await fetchUserProfile()
  }

  const logout = () => {
    clearToken()
    clearUserId()
    clearCity()
    setSessionAuthenticated(false)
    profileResolved.value = false
    userInfo.value = createEmptyUserInfo()
  }

  const setLocalCity = (city: string) => {
    if (!city.trim()) return
    mergeUserInfo({ city: city.trim() })
  }

  return {
    token,
    userInfo,
    user,
    isAuthenticated,
    currentUserId,
    displayName,
    displayAvatar,
    needsHeroSelection,

    saveToken,
    clearToken,

    fetchUserInfo,
    fetchMainHeroes,
    fetchUserProfile,
    updateUserProfile,
    saveMainHeroes,
    setLocalCity,
    bootstrapSession,

    loginByPassword,
    loginByPhoneCode,
    loginByEmailCode,
    register,
    resetPassword,
    logout,
  }
})
