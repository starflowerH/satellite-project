# 定位系统功能文档

## 1. 目标与范围

当前项目实现了两条“城市定位”能力，目标是让用户在前端可看到并保存自己的当前城市：

1. 资料面板手动触发定位（浏览器原生定位 + 逆地理解析）。
2. 圣地巡游页面自动触发定位（高德地图定位插件）。

定位结果统一写入 `userStore.userInfo.city`，并落地到本地缓存 `localStorage.user_city`。

## 2. 代码实现总览

### 2.1 核心模块

- `src/utils/geolocation.ts`
  - 基于 `navigator.geolocation.getCurrentPosition` 获取经纬度。
  - 调用 Nominatim 逆地理接口 `https://nominatim.openstreetmap.org/reverse` 解析城市。
  - 导出 `locateCurrentCity()`，返回 `{ city, latitude, longitude }`。

- `src/utils/amapLoader.ts`
  - 动态注入高德地图 JS 脚本（避免静态 import 第三方 loader 解析问题）。
  - 按 `key/version/plugins` 生成脚本地址。
  - 缓存加载 Promise，避免重复注入脚本。

- `src/store/user.ts`
  - 新增城市持久化键：`CITY_KEY = 'user_city'`。
  - `createEmptyUserInfo()` 初始化时读取本地城市。
  - `mergeUserInfo()` 合并用户信息时同步持久化城市。
  - `patchUserInfoFromRaw()` 支持从后端字段回填城市：`city/cityName/location/region`。
  - 暴露 `setLocalCity(city)` 供页面写入城市。

- `src/components/UserProfilePanel.vue`
  - 编辑态新增“自动定位”按钮。
  - 调用 `locateCurrentCity()` 后填充 `editForm.city`。
  - 点击“保存修改”时调用 `userStore.setLocalCity(city)`。

- `src/views/HolyLandTourView.vue`
  - 页面初始化时加载高德地图 SDK。
  - 调用 `AMap.Geolocation` 定位并解析 `city/district/province`。
  - 将结果展示在状态栏，并写入 `userStore.setLocalCity(city)`。

### 2.2 类型与数据模型

- `src/api/user.ts`
  - `UserInfoVO` 已包含 `city: string` 字段。

## 3. 定位数据流

### 3.1 资料面板定位链路

1. 用户打开资料面板，进入编辑态。
2. 点击“自动定位”。
3. 浏览器弹出定位权限。
4. `locateCurrentCity()` 获取坐标并逆地理解析出城市。
5. 城市写入 `editForm.city`。
6. 用户点击“保存修改”，调用 `userStore.setLocalCity(city)`。
7. Store 更新 `userInfo.city` 并写入 `localStorage.user_city`。

### 3.2 圣地巡游自动定位链路

1. 进入路由 `/holy-land-tour`。
2. 页面 `onMounted` 调用 `initMap()`。
3. `loadAmap()` 动态加载高德地图脚本。
4. 初始化地图后调用 `locateCurrentPosition()`。
5. `AMap.Geolocation` 返回定位结果。
6. 从 `addressComponent` 取 `city -> district -> province`。
7. 更新 `currentCityLabel` 并调用 `userStore.setLocalCity(city)`。

## 4. 使用说明

## 4.1 前置条件

1. 浏览器允许地理定位权限。
2. 页面运行在可用环境（建议 `localhost` 或 HTTPS）。
3. 若使用圣地巡游地图定位，需要配置高德 Key。

## 4.2 环境变量

在 `.env.development` 添加：

```bash
VITE_AMAP_KEY=你的高德Web端Key
VITE_AMAP_ROUTE_KEY=你的高德路径规划Web服务Key
```

说明：
- `VITE_AMAP_KEY` 用于地图 SDK 加载和定位插件。
- `VITE_AMAP_ROUTE_KEY` 用于路径规划 Web 服务（REST）。
- 未配置 `VITE_AMAP_KEY` 时，圣地巡游页面无法加载地图。

## 4.3 用户操作路径

- 路径 A（资料面板）
  1. 登录后点击右上角头像。
  2. 点击“编辑资料”。
  3. 在“所在城市”右侧点击“自动定位”。
  4. 成功后点击“保存修改”。

- 路径 B（圣地巡游）
  1. 进入 `/holy-land-tour` 页面。
  2. 页面自动定位并在底部“定位城市”显示结果。

## 5. 持久化与回填规则

1. 本地持久化键：`localStorage.user_city`。
2. 刷新后：`createEmptyUserInfo()` 会优先回填本地城市。
3. 若后端返回用户信息里含城市字段，Store 会合并覆盖本地值。
4. 退出登录会清理城市缓存。

## 6. 错误处理与降级策略

- 浏览器不支持定位：
  - 抛出“当前浏览器不支持定位”。

- 用户拒绝定位权限 / 定位超时：
  - 资料面板显示“定位失败，请检查定位权限后重试”。
  - 圣地巡游回退到 `userStore.userInfo.city` 或“定位失败”。

- 高德 Key 未配置：
  - 页面提示“未配置 VITE_AMAP_KEY，无法加载高德地图”。
  - 仍展示默认演示路线，不阻塞页面。

- 逆地理解析失败：
  - `locateCurrentCity()` 抛错，页面提示失败信息。

## 7. 已知限制

1. 资料面板定位依赖第三方逆地理服务（Nominatim），网络受限时会失败。
2. 城市当前主要是前端本地写入；`updateUserProfile` 未携带 `city` 到后端（若后端需要持久化城市，需补接口字段和请求体）。
3. `HolyLandTourView.vue` 中仍有历史中文乱码文本，不影响定位逻辑，但影响部分文案显示。

## 8. 联调建议

1. 前端先在 `.env.development` 配置 `VITE_AMAP_KEY`，验证 `/holy-land-tour` 自动定位。
2. 在资料面板执行“自动定位 -> 保存修改 -> 刷新页面”，检查城市是否持久化。
3. 若后端要持久化城市，新增 `city` 字段入参后，再将 `updateUserProfile` 请求体同步扩展。

## 9. 快速自测清单

1. 首次登录后，资料页城市显示是否为“未定位”。
2. 点击“自动定位”后，是否出现浏览器权限弹窗。
3. 定位成功后，城市是否写入输入框与展示区。
4. 保存后刷新页面，城市是否仍在。
5. 进入 `/holy-land-tour` 后，底部“定位城市”是否更新。
6. 关闭定位权限时，是否有明确错误提示且页面不崩溃。
