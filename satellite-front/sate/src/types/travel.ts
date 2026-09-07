/**
 * src/types/travel.ts - 灵境导览 文旅领域类型定义
 */

/** 辣度等级定义 */
export type SpicyLevel = '不辣' | '微辣' | '中辣' | '重辣'

/** 出行交通方式 */
export type TransportMode = 'WALKING' | 'TRANSIT' | 'RIDING' | 'DRIVING'

/** 核心氛围标签 */
export type AtmosphereTag = '松弛感' | '出片打卡' | '人文历史' | '自然山水' | '烟火夜市'

/**
 * 个人出行与饮食画像 VO / DTO
 */
export interface UserProfileVO {
  userId?: number | string
  spicyLevel: string
  flavorPref?: string
  dietaryRestrictions: string[]
  travelPace?: string
  budgetPerMeal: number
}

/**
 * 菜品明细 VO
 */
export interface DishVO {
  id?: number
  merchantId?: number
  name: string
  price: number
  isSignature?: number
  spicyLevel?: string
  flavorNotes?: string
  allergensOrIngredients?: string
  warning?: string
}

/**
 * 美食商户 / 店铺 VO
 */
export interface MerchantVO {
  id?: number
  amapPoiId?: string
  name: string
  category: string
  longitude: number
  latitude: number
  address?: string
  businessHours?: string
  avgPricePerPerson: number
  flavorTags?: string[]
  isCustomAdded?: number
  phone?: string
  rating?: number
  isActive?: number
  dishes?: DishVO[]
}

/**
 * 文旅景点 POI VO
 */
export interface PoiVO {
  id?: number
  name: string
  alias?: string
  category?: string
  longitude: number
  latitude: number
  address?: string
  openHours?: string
  suggestedDurationMinutes?: number
  ticketPrice?: number
  atmosphereTags?: string[]
  description?: string
  coverImage?: string
  isActive?: number
}

/**
 * 路线时空节点明细 VO
 */
export interface RouteItemVO {
  id?: number
  planId?: number
  itemOrder: number
  /** 节点类型：POI(文旅景点) / MERCHANT(美食餐饮) */
  itemType: 'POI' | 'MERCHANT'
  targetId?: number
  name: string
  longitude: number
  latitude: number
  arriveTime: string
  stayMinutes: number
  recommendReason: string
  recommendedDishes?: DishVO[]
  transportToNextMinutes: number
  transportToNextDistance: number
}

/**
 * 路线规划方案 VO
 */
export interface RoutePlanVO {
  id?: number
  planId?: number
  userId?: number
  title: string
  durationHours: number
  atmosphere: string
  transportMode: string
  totalDistanceMeters: number
  totalDurationMinutes: number
  isFallback: number
  createdAt?: string
  items: RouteItemVO[]
}

/**
 * 路线智能生成请求 DTO
 */
export interface RouteGenerateRequestDTO {
  durationHours: number
  atmosphere: string
  transportMode: string
  startLng?: number
  startLat?: number
  userId?: number | string
  spicyLevel?: string
  dietaryRestrictions?: string[]
  budgetPerMeal?: number
}

/**
 * 管理端录入商户 DTO
 */
export interface CreateMerchantDTO {
  name: string
  category: string
  longitude: number
  latitude: number
  address?: string
  businessHours?: string
  avgPricePerPerson: number
  flavorTags?: string
  phone?: string
  rating?: number
  dishes?: CreateDishDTO[]
}

/**
 * 管理端录入菜品 DTO
 */
export interface CreateDishDTO {
  merchantId?: number
  name: string
  price: number
  isSignature?: number
  spicyLevel?: string
  flavorNotes?: string
  allergensOrIngredients?: string
  warning?: string
}

