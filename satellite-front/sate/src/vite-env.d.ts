/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_PORT?: string
  readonly VITE_PROXY_ENABLED?: string
  readonly VITE_AMAP_KEY?: string
  readonly VITE_AMAP_ROUTE_KEY?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
