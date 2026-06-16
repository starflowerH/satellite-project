export interface CityLocation {
  city: string
  latitude: number
  longitude: number
}

interface ReverseGeoResponse {
  address?: {
    city?: string
    town?: string
    county?: string
    state_district?: string
    state?: string
    municipality?: string
  }
}

const REVERSE_GEO_ENDPOINT = 'https://nominatim.openstreetmap.org/reverse'

const getCurrentPosition = (): Promise<GeolocationPosition> => {
  if (!navigator.geolocation) {
    throw new Error('当前浏览器不支持定位')
  }

  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 12000,
      maximumAge: 0,
    })
  })
}

const reverseGeocodeCity = async (latitude: number, longitude: number): Promise<string> => {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), 8000)

  try {
    const url = new URL(REVERSE_GEO_ENDPOINT)
    url.searchParams.set('format', 'jsonv2')
    url.searchParams.set('lat', String(latitude))
    url.searchParams.set('lon', String(longitude))
    url.searchParams.set('accept-language', 'zh-CN')

    const response = await fetch(url.toString(), {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal: controller.signal,
    })

    if (!response.ok) {
      throw new Error(`逆地理编码失败(${response.status})`)
    }

    const data = (await response.json()) as ReverseGeoResponse
    const address = data.address || {}
    const city =
      address.city ||
      address.town ||
      address.municipality ||
      address.county ||
      address.state_district ||
      address.state ||
      ''

    if (!city) {
      throw new Error('未解析到城市信息')
    }

    return city.trim()
  } finally {
    window.clearTimeout(timer)
  }
}

export const locateCurrentCity = async (): Promise<CityLocation> => {
  const position = await getCurrentPosition()
  const latitude = position.coords.latitude
  const longitude = position.coords.longitude
  const city = await reverseGeocodeCity(latitude, longitude)

  return {
    city,
    latitude,
    longitude,
  }
}

export default locateCurrentCity
