import request from '@/utils/request'
import type { Result } from '@/api/auth'
import { toSafeString } from '@/utils/common'

export interface HeroListQuery {
  keyword?: string
  gender?: string | number
  role?: string
}

export interface HeroOption {
  heroId: string
  name: string
  profession: string
  gender?: string
}

export interface UserHeroVO {
  id?: string
  heroId: string
  heroName: string
  role?: string
}

export interface SaveUserHeroesDTO {
  userId: string
  heroIds: string[]
}

const extractArray = (response: unknown): unknown[] => {
  if (Array.isArray(response)) return response
  if (!response || typeof response !== 'object') return []

  const root = response as Record<string, unknown>
  const data = root.data as Record<string, unknown> | undefined
  const candidates = [root.data, data?.data, data?.records, root.records, root.list, data?.list]

  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate
  }

  return []
}

const normalizeHero = (raw: unknown, index: number): HeroOption => {
  const source = (raw ?? {}) as Record<string, unknown>

  return {
    heroId: toSafeString(source.heroId ?? source.id ?? source.hero_id ?? source.value ?? index + 1),
    name: toSafeString(source.name ?? source.heroName ?? source.title ?? source.label),
    profession: toSafeString(
      source.profession ?? source.position ?? source.role ?? source.career ?? source.job ?? source.lane,
    ),
    gender: toSafeString(source.gender ?? source.sex),
  }
}

const normalizeUserHero = (raw: unknown): UserHeroVO => {
  const source = (raw ?? {}) as Record<string, unknown>

  return {
    id: toSafeString(source.id),
    heroId: toSafeString(source.heroId ?? source.id ?? source.hero_id),
    heroName: toSafeString(source.heroName ?? source.name ?? source.title ?? source.label),
    role: toSafeString(source.role ?? source.profession ?? source.position),
  }
}

const dedupeHeroes = (list: HeroOption[]) => {
  const result: HeroOption[] = []
  const seen = new Set<string>()

  for (const hero of list) {
    const key = hero.heroId || hero.name
    if (!key || seen.has(key)) continue
    seen.add(key)
    result.push(hero)
  }

  return result
}

export const listHeroes = async (query: HeroListQuery = {}): Promise<HeroOption[]> => {
  const response = await request.get('/hero/list', {
    params: {
      keyword: query.keyword?.trim() || undefined,
      gender: query.gender || undefined,
      role: query.role?.trim() || undefined,
    },
  })

  return dedupeHeroes(extractArray(response).map(normalizeHero).filter((hero) => !!hero.name))
}

export const listMyHeroes = async (userId: string): Promise<UserHeroVO[]> => {
  const response = await request.get('/hero/my/list', {
    params: { userId },
  })

  return extractArray(response)
    .map(normalizeUserHero)
    .filter((hero) => !!hero.heroId || !!hero.heroName)
}

export const saveMyHeroes = async (payload: SaveUserHeroesDTO): Promise<Result<string>> => {
  return request.post('/hero/my/save', payload)
}

export const heroApi = {
  listHeroes,
  listMyHeroes,
  saveMyHeroes,
}

export default heroApi
