/**
 * src/utils/request.ts - 统一请求封装
 * 
 * 说明：
 * 1. 所有 API 请求都会自动添加 /api 前缀
 * 2. 通过 Vite 代理转发到后端服务器
 * 3. 支持请求拦截器和响应拦截器
 * 4. 统一错误处理
 */

import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api', // 通过 Vite 代理转发到后端服务器
  timeout: 20000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json',
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    // 例如：添加认证 token
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 对响应数据做点什么
    return response.data
  },
  (error) => {
    // 对响应错误做点什么
    if (error.response) {
      // 服务器返回了错误状态码
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          // 未授权，跳转到登录页
          console.error('未授权，请重新登录')
          window.location.href = '/login'
          break
        case 403:
          console.error('禁止访问')
          break
        case 404:
          console.error('资源不存在')
          break
        case 500:
          console.error('服务器内部错误')
          break
        default:
          console.error(`请求错误 ${status}:`, data.message || '未知错误')
      }
    } else if (error.request) {
      // 请求发送成功，但没有收到响应
      console.error('网络错误，请检查网络连接')
    } else {
      // 请求配置出错
      console.error('请求配置错误:', error.message)
    }
    
    return Promise.reject(error)
  }
)

export default request

// 示例 API 函数
export const api = {
  // 用户相关
  user: {
    login: (data: { phone: string; password: string }) => 
      request.post('/user/login', data),
    logout: () => 
      request.post('/user/logout'),
    getProfile: () => 
      request.get('/user/profile'),
  },
  
  // 对局相关
  match: {
    getList: (params?: { page: string; size: string }) => 
      request.get('/matches', { params }),
    getDetail: (id: string) => 
      request.get(`/matches/${id}`),
    analyze: (data: any) => 
      request.post('/matches/analyze', data),
  },
  
  // 英雄相关
  hero: {
    getList: () => 
      request.get('/heroes'),
    recommend: (data: any) => 
      request.post('/heroes/recommend', data),
  },
}