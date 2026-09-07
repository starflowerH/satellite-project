/**
 * src/api/travel.ts - 灵境导览 文旅路线、商户与用户画像 API
 */
import request from '@/utils/request'
import type { Result } from '@/api/auth'
import type {
  DishVO,
  MerchantVO,
  PoiVO,
  RouteGenerateRequestDTO,
  RoutePlanVO,
  UserProfileVO,
} from '@/types/travel'

export const generateRoute = (dto: RouteGenerateRequestDTO): Promise<Result<RoutePlanVO>> => {
  return request.post('/routes/generate', dto)
}

export const getRouteById = (id: number | string): Promise<Result<RoutePlanVO>> => {
  return request.get(`/routes/${id}`)
}

export const getLatestRoute = (): Promise<Result<RoutePlanVO>> => {
  return request.get('/routes/latest')
}

export const listRouteHistory = (userId?: number | string): Promise<Result<RoutePlanVO[]>> => {
  return request.get('/routes/history', {
    params: { userId: userId ?? 0 },
  })
}

export const getUserProfile = (userId?: number | string): Promise<Result<UserProfileVO>> => {
  return request.get('/user-profile', {
    params: { userId: userId ?? 0 },
  })
}

export const updateUserProfile = (dto: Partial<UserProfileVO>): Promise<Result<string>> => {
  return request.post('/user-profile/update', dto)
}

export const listMerchants = (params?: { category?: string; isCustomAdded?: number }): Promise<Result<MerchantVO[]>> => {
  return request.get('/merchants', { params })
}

export const getMerchantById = (id: number | string): Promise<Result<MerchantVO>> => {
  return request.get(`/merchants/${id}`)
}

export const createMerchant = (dto: Partial<MerchantVO>): Promise<Result<any>> => {
  return request.post('/merchants/create', dto)
}

export const deleteMerchant = (id: number | string): Promise<Result<string>> => {
  return request.delete(`/merchants/${id}`)
}

export const listDishes = (merchantId: number | string): Promise<Result<DishVO[]>> => {
  return request.get(`/dishes/merchant/${merchantId}`)
}

export const createDish = (dto: Partial<DishVO>): Promise<Result<any>> => {
  return request.post('/dishes/create', dto)
}

export const deleteDish = (id: number | string): Promise<Result<string>> => {
  return request.delete(`/dishes/${id}`)
}

export const listPois = (tag?: string): Promise<Result<PoiVO[]>> => {
  return request.get('/pois', {
    params: { tag: tag || undefined },
  })
}

export const travelApi = {
  generateRoute,
  getRouteById,
  getLatestRoute,
  listRouteHistory,
  getUserProfile,
  updateUserProfile,
  listMerchants,
  getMerchantById,
  createMerchant,
  deleteMerchant,
  listDishes,
  createDish,
  deleteDish,
  listPois,
}

export default travelApi

