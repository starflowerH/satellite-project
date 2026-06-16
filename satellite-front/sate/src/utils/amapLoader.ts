import request from '@/utils/request'

let amapLoadPromise: Promise<any> | null = null

type LoadAmapOptions = {
  key?: string
  version?: string
  plugins?: string[]
}

declare global {
  interface Window {
    AMap?: any
  }
}

const buildScriptUrl = ({ key, version = '2.0', plugins = [] }: { key: string; version?: string; plugins?: string[] }): string => {
  const url = new URL('https://webapi.amap.com/maps')
  url.searchParams.set('v', version)
  url.searchParams.set('key', key)

  if (plugins.length) {
    url.searchParams.set('plugin', plugins.join(','))
  }

  return url.toString()
}

/**
 * 从后端获取高德地图Key
 */
const fetchAmapKey = async (): Promise<string> => {
  try {
    const response: any = await request.get('/amap/config')
    // 后端返回格式: { code: 200, data: { key: "xxx" } }
    const key = response?.data?.key || response?.key
    if (key) {
      return key
    }
    throw new Error('后端未返回高德地图Key')
  } catch (error) {
    throw new Error('获取高德地图Key失败，请检查后端配置')
  }
}

export const loadAmap = async (options: LoadAmapOptions = {}): Promise<any> => {
  if (window.AMap) {
    return window.AMap
  }

  // 优先使用传入的key，否则从后端获取
  let key = options.key?.trim()
  if (!key) {
    key = await fetchAmapKey()
  }

  if (!key) {
    throw new Error('高德地图 Key 不能为空')
  }

  if (!amapLoadPromise) {
    amapLoadPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = buildScriptUrl({
        key,
        version: options.version,
        plugins: options.plugins,
      })
      script.async = true
      script.defer = true

      script.onload = () => {
        if (!window.AMap) {
          reject(new Error('高德地图脚本已加载，但 AMap 未挂载到 window'))
          return
        }
        resolve(window.AMap)
      }

      script.onerror = () => {
        reject(new Error('高德地图脚本加载失败'))
      }

      document.head.appendChild(script)
    }).catch((error) => {
      amapLoadPromise = null
      throw error
    })
  }

  return amapLoadPromise
}

export default loadAmap
