import request from '@/utils/request'
import type { Result } from '@/api/auth'

export interface UserInfoVO {
  userId: string
  name: string
  avatar: string
  signature: string
  city: string
  phone: string
  email: string
  mainHeroes: string[]
}

export interface UpdateUserDTO {
  id: string
  userId: string
  name: string
  avatar: string
  signature: string
  phone: string
  email: string
}

export const getUserInfo = (userId: string): Promise<Result<UserInfoVO>> => {
  return request.get(`/user/${userId}`)
}

export const updateUserInfo = (payload: UpdateUserDTO): Promise<Result<UserInfoVO>> => {
  const id = payload.id || payload.userId
  const userId = payload.userId || payload.id

  return request.put('/user/update', {
    ...payload,
    id,
    userId,
  })
}

export const userApi = {
  getUserInfo,
  updateUserInfo,
}

export default userApi
