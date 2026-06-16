/**
 * 公共工具函数
 * 提取自各组件的重复工具函数
 */

/**
 * 安全转换为字符串
 */
export const toSafeString = (value: unknown): string => {
  if (typeof value === 'string') return value.trim()
  if (typeof value === 'number') return String(value)
  if (typeof value === 'bigint') return value.toString()
  return ''
}

/**
 * 安全转换为数字
 */
export const toSafeNumber = (value: unknown): number => {
  if (typeof value === 'number') return Number.isFinite(value) ? value : Number.NaN
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : Number.NaN
  }
  return Number.NaN
}

/**
 * 标准化城市名称（去除"市"、"区"等后缀）
 */
export const normalizeCityKey = (city: string): string => {
  const cleaned = city.trim().replace(/\s+/g, '')
  if (!cleaned) return ''

  const match = cleaned.match(/^(.+?)(?:市|地区|盟|自治州|特别行政区|区|县)/)
  if (match?.[1]) {
    return match[1]
  }

  return cleaned
}

/**
 * 标准化城市关键词（添加"市"后缀如果需要）
 */
export const normalizeCityKeyword = (keyword: string): string => {
  const text = keyword.trim()
  if (!text) return text
  if (text.endsWith('市') || text.endsWith('区') || text.endsWith('县')) return text
  if (text.length <= 4) return `${text}市`
  return text
}

/**
 * 检查是否为空字符串
 */
export const isBlank = (value: unknown): boolean => {
  if (value === null || value === undefined) return true
  if (typeof value === 'string') return value.trim().length === 0
  return false
}

/**
 * 检查是否为有效手机号
 */
export const isValidPhone = (phone: string): boolean => {
  return /^1[3-9]\d{9}$/.test(phone)
}

/**
 * 检查是否为有效邮箱
 */
export const isValidEmail = (email: string): boolean => {
  return /^[\w.+-]+@[\w-]+\.[\w.]+$/.test(email)
}

/**
 * 生成随机验证码
 */
export const generateCode = (length = 6): string => {
  return String(Math.floor(Math.random() * Math.pow(10, length))).padStart(length, '0')
}

/**
 * 格式化手机号（脱敏）
 */
export const maskPhone = (phone: string): string => {
  if (!phone || phone.length < 7) return phone
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

/**
 * 格式化邮箱（脱敏）
 */
export const maskEmail = (email: string): string => {
  if (!email) return email
  const atIndex = email.indexOf('@')
  if (atIndex < 0) return email
  if (atIndex <= 2) return `***${email.slice(atIndex)}`
  return `${email.slice(0, 2)}***${email.slice(atIndex)}`
}

/**
 * 检查是否为十六进制颜色
 */
export const isHexColor = (value: string): boolean => {
  return /^#([\da-f]{3}|[\da-f]{6})$/i.test(value)
}

/**
 * 标准化字符串列表（去重、限制数量）
 */
export const normalizeStringList = (items: unknown[], limit = 5): string[] => {
  const result: string[] = []
  const seen = new Set<string>()

  for (const item of items) {
    const str = toSafeString(item)
    if (!str || seen.has(str)) continue
    seen.add(str)
    result.push(str)
    if (result.length >= limit) break
  }

  return result
}
