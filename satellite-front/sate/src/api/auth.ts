/**
 * src/api/auth.ts - 认证相关 API 接口封装
 *
 * 统一返回结构使用泛型：Result<T>
 * 登录接口返回：Result<string>（JWT Token）
 */

import request from '@/utils/request'

// 通用响应结构
export interface Result<T = any> {
  code: string
  message: string
  data: T
}

// 登录返回信息
export interface LoginResponse {
  token: string
  userInfo: {
    username: string
    avatar?: string
    phone?: string
    email?: string
  }
}

/**
 * 发送手机验证码
 * @param phone 手机号
 */
export const sendPhoneCode = (phone: string): Promise<Result> => {
  return request.post('/auth/code', { phone })
}

/**
 * 发送邮箱验证码
 * @param email 邮箱地址
 */
export const sendEmailCode = (email: string): Promise<Result> => {
  return request.post('/auth/email-code', { email })
}

/**
 * 注册参数
 */
export interface RegisterDTO {
  phone?: string
  email?: string
  password: string
  code: string
}

export const register = (data: RegisterDTO): Promise<Result> => {
  return request.post('/auth/register', data)
}

/**
 * 手机号密码登录
 */
export interface PasswordLoginDTO {
  username?: string
  phone?: string
  password: string
}

export const loginByPassword = (data: PasswordLoginDTO): Promise<Result<string>> => {
  return request.post('/auth/login/password', data)
}

/**
 * 手机验证码登录
 */
export interface PhoneCodeLoginDTO {
  phone: string
  code: string
}

export const loginByPhoneCode = (data: PhoneCodeLoginDTO): Promise<Result<string>> => {
  return request.post('/auth/login/code', data)
}

/**
 * 邮箱验证码登录
 */
export interface EmailCodeLoginDTO {
  email: string
  code: string
}

export const loginByEmailCode = (data: EmailCodeLoginDTO): Promise<Result<string>> => {
  return request.post('/auth/login/email-code', data)
}

/**
 * 重置密码参数
 */
export interface ResetPasswordDTO {
  phone?: string
  email?: string
  code: string
  password?: string
  newPassword?: string
}

export const resetPassword = (data: ResetPasswordDTO): Promise<Result> => {
  const pwd = data.newPassword || data.password || ''
  const payload = {
    phone: data.phone,
    email: data.email,
    code: data.code,
    password: pwd,
    newPassword: pwd,
  }
  return request.post('/auth/reset-password', payload)
}

// Auth API 汇总导出
export const authApi = {
  sendPhoneCode,
  sendEmailCode,
  register,
  loginByPassword,
  loginByPhoneCode,
  loginByEmailCode,
  resetPassword
}

export default authApi
