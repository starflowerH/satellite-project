<script setup lang="ts">
/**
 * AdminView.vue - 灵境导览 · 商户与特色菜品管理工作台 (Block 4)
 *
 * 核心功能：
 * 1. 新增特色商户：店名、分类、经纬度（带衡阳核心地标一键填充芯片）、人均消费、风味标签、营业时间、电话与地址
 * 2. 录入招牌菜品：关联商户、菜品名称、单价、辣度等级、口感风味、食材避坑与过敏原提示
 * 3. 实时商户与菜品可视化知识库：支持自录店铺标识、菜品标签展示、实时删除与快速体验推荐
 * 4. 端到端协同联动：录入数据即时同步至后端与 Pinia Store，前台路线规划能即时召回录入的新店铺与菜品
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { travelApi } from '@/api/travel'
import { useRouteStore } from '@/store/route'
import type { CreateDishDTO, CreateMerchantDTO, DishVO, MerchantVO } from '@/types/travel'

const router = useRouter()
const routeStore = useRouteStore()

// 状态
const merchantsList = ref<MerchantVO[]>([])
const isLoadingList = ref(false)
const isSubmittingMerchant = ref(false)
const isSubmittingDish = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'warning' | 'error'>('success')
const activeFilter = ref<'all' | 'custom'>('all')

// 衡阳核心地标预设芯片
const HENGYANG_LANDMARKS = [
  { name: '衡阳师院 (雁山校区)', lng: 112.6845, lat: 26.8398, address: '衡阳市珠晖区衡花路16号' },
  { name: '东洲岛', lng: 112.6358, lat: 26.8732, address: '衡阳市雁峰区湘江东洲岛' },
  { name: '石鼓书院', lng: 112.6133, lat: 26.9038, address: '衡阳市石鼓区草桥头' },
  { name: '南湖公园', lng: 112.6022, lat: 26.8631, address: '衡阳市雁峰区黄白路' },
  { name: '雨母山风景区', lng: 112.5218, lat: 26.8436, address: '衡阳市蒸湘区雨母山镇' },
]

// 预设风味标签
const PRESET_FLAVOR_TAGS = ['地道湘菜', '衡阳传统', '黄贡椒脆肚', '老汤鱼粉', '石磨豆腐', '微辣鲜香', '学生实惠', '老字号']

// 商户表单
const merchantForm = reactive<CreateMerchantDTO>({
  name: '',
  category: '地道湘菜',
  longitude: 112.6845,
  latitude: 26.8398,
  address: '衡阳市师院雁山校区周边特色街区',
  businessHours: '10:00-22:00',
  avgPricePerPerson: 38,
  flavorTags: '地道湘菜,学生实惠',
  phone: '0734-8889168',
  rating: 4.8,
  dishes: [],
})

// 菜品表单
const dishForm = reactive<CreateDishDTO>({
  merchantId: undefined,
  name: '',
  price: 36,
  isSignature: 1,
  spicyLevel: '微辣',
  flavorNotes: '衡阳传统风味，鲜嫩浓郁',
  allergensOrIngredients: '纯素/无内脏/豆制品/可免香菜',
  warning: '地道现炒 / 辣度温和 / 严格无内脏',
})

// 提示框
const showToast = (msg: string, type: 'success' | 'warning' | 'error' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  setTimeout(() => {
    toastMessage.value = ''
  }, 3500)
}

// 快速一键填充地标经纬度
const applyLandmark = (lm: typeof HENGYANG_LANDMARKS[0]) => {
  merchantForm.longitude = lm.lng
  merchantForm.latitude = lm.lat
  merchantForm.address = `${lm.address}周边美食商圈`
  showToast(`已一键填入【${lm.name}】GCJ-02 坐标: ${lm.lng}, ${lm.lat}`)
}

// 切换风味标签选中
const toggleFlavorTag = (tag: string) => {
  const currentTags = merchantForm.flavorTags ? merchantForm.flavorTags.split(',').map(s => s.trim()).filter(Boolean) : []
  const idx = currentTags.indexOf(tag)
  if (idx >= 0) {
    currentTags.splice(idx, 1)
  } else {
    currentTags.push(tag)
  }
  merchantForm.flavorTags = currentTags.join(',')
}

// 加载商户列表
const fetchMerchants = async () => {
  isLoadingList.value = true
  try {
    const res = await travelApi.listMerchants()
    if (res && res.data && Array.isArray(res.data)) {
      merchantsList.value = res.data
      // 同时同步给 Pinia store
      routeStore.loadCustomMerchants()
    }
  } catch (err) {
    console.warn('获取商户列表异常，降级加载本地商户:', err)
    merchantsList.value = routeStore.customMerchants
  } finally {
    isLoadingList.value = false
    // 默认绑定第一个商户到菜品表单
    if (!dishForm.merchantId && merchantsList.value.length > 0) {
      dishForm.merchantId = merchantsList.value[0].id
    }
  }
}

// 提交新增商户
const handleCreateMerchant = async () => {
  if (!merchantForm.name.trim()) {
    showToast('请输入店铺名称', 'warning')
    return
  }

  isSubmittingMerchant.value = true
  try {
    const payload: Partial<MerchantVO> = {
      name: merchantForm.name.trim(),
      category: merchantForm.category,
      longitude: Number(merchantForm.longitude),
      latitude: Number(merchantForm.latitude),
      address: merchantForm.address,
      businessHours: merchantForm.businessHours,
      avgPricePerPerson: Number(merchantForm.avgPricePerPerson),
      flavorTags: merchantForm.flavorTags ? merchantForm.flavorTags.split(',').map(s => s.trim()) : ['特色美食'],
      phone: merchantForm.phone,
      rating: 4.9,
      isCustomAdded: 1,
      dishes: [],
    }

    let createdId: number | null = null
    try {
      const res = await travelApi.createMerchant(payload)
      if (res && res.data) {
        createdId = typeof res.data === 'number' ? res.data : (res.data as any).id
      }
    } catch (apiErr) {
      console.warn('后端创建商户接口离线，通过本地 Pinia 知识库持久化:', apiErr)
    }

    const finalMerchant: MerchantVO = {
      ...payload,
      id: createdId || Date.now(),
      isCustomAdded: 1,
    } as MerchantVO

    // 存入 Pinia Store 与本地存储
    routeStore.addCustomMerchant(finalMerchant)

    // 更新当前页面列表
    const existingIdx = merchantsList.value.findIndex(m => m.name === finalMerchant.name)
    if (existingIdx >= 0) {
      merchantsList.value[existingIdx] = finalMerchant
    } else {
      merchantsList.value.unshift(finalMerchant)
    }

    dishForm.merchantId = finalMerchant.id
    showToast(`🎉 成功录入商户【${finalMerchant.name}】，已即时同步至 Agent 知识库！`)

    // 重置表单但保留常用默认值
    merchantForm.name = ''
  } catch (error) {
    showToast('录入商户失败，请重试', 'error')
  } finally {
    isSubmittingMerchant.value = false
  }
}

// 提交新增菜品
const handleCreateDish = async () => {
  if (!dishForm.merchantId) {
    showToast('请选择关联商户', 'warning')
    return
  }
  if (!dishForm.name.trim()) {
    showToast('请输入特色菜品名称', 'warning')
    return
  }

  isSubmittingDish.value = true
  try {
    const payload: Partial<DishVO> = {
      merchantId: dishForm.merchantId,
      name: dishForm.name.trim(),
      price: Number(dishForm.price),
      isSignature: dishForm.isSignature ? 1 : 0,
      spicyLevel: dishForm.spicyLevel,
      flavorNotes: dishForm.flavorNotes,
      allergensOrIngredients: dishForm.allergensOrIngredients,
      warning: dishForm.warning || dishForm.allergensOrIngredients,
    }

    let createdDishId: number | null = null
    try {
      const res = await travelApi.createDish(payload)
      if (res && res.data) {
        createdDishId = typeof res.data === 'number' ? res.data : (res.data as any).id
      }
    } catch (apiErr) {
      console.warn('后端添加菜品接口离线，通过本地 Pinia 知识库持久化:', apiErr)
    }

    const finalDish: DishVO = {
      ...payload,
      id: createdDishId || Date.now(),
    } as DishVO

    // 更新到内存与 store 对应商户
    const targetMerchant = merchantsList.value.find(m => m.id === dishForm.merchantId)
    if (targetMerchant) {
      if (!targetMerchant.dishes) targetMerchant.dishes = []
      targetMerchant.dishes.unshift(finalDish)
      routeStore.addCustomMerchant(targetMerchant)
    }

    showToast(`🍲 成功为【${targetMerchant?.name || '指定商户'}】录入招牌菜【${finalDish.name}】！`)
    dishForm.name = ''
  } catch (error) {
    showToast('录入菜品失败，请重试', 'error')
  } finally {
    isSubmittingDish.value = false
  }
}

// 删除商户
const handleDeleteMerchant = async (merchant: MerchantVO) => {
  if (!confirm(`确认删除商户【${merchant.name}】吗？`)) return

  try {
    if (merchant.id) {
      await travelApi.deleteMerchant(merchant.id).catch(() => {})
      routeStore.removeCustomMerchant(merchant.id)
    }
    merchantsList.value = merchantsList.value.filter(m => m.id !== merchant.id)
    showToast(`已删除商户【${merchant.name}】`)
  } catch (e) {
    showToast('删除失败', 'error')
  }
}

// 删除菜品
const handleDeleteDish = async (merchant: MerchantVO, dish: DishVO) => {
  if (!confirm(`确认删除菜品【${dish.name}】吗？`)) return

  try {
    if (dish.id) {
      await travelApi.deleteDish(dish.id).catch(() => {})
    }
    if (merchant.dishes) {
      merchant.dishes = merchant.dishes.filter(d => d.id !== dish.id)
      routeStore.addCustomMerchant(merchant)
    }
    showToast(`已移除招牌菜【${dish.name}】`)
  } catch (e) {
    showToast('删除菜品失败', 'error')
  }
}

// 立即体验：以该商户为核心生成路线并跳转
const handleExperienceRouteWithMerchant = async (merchant: MerchantVO) => {
  showToast(`🚀 正在通过 Agent 意图与双路召回管线，优先捕获【${merchant.name}】定制漫游路线...`)
  routeStore.addCustomMerchant(merchant)

  await routeStore.generatePlan({
    durationHours: 4,
    atmosphere: '松弛感',
    transportMode: 'WALKING',
    spicyLevel: '微辣',
    dietaryRestrictions: ['不吃内脏'],
    budgetPerMeal: merchant.avgPricePerPerson || 40,
  })

  router.push('/route-map')
}

// 过滤后的商户列表
const filteredMerchants = computed(() => {
  if (activeFilter.value === 'custom') {
    return merchantsList.value.filter(m => m.isCustomAdded === 1)
  }
  return merchantsList.value
})

const totalDishesCount = computed(() => {
  return merchantsList.value.reduce((acc, cur) => acc + (cur.dishes?.length || 0), 0)
})

onMounted(() => {
  fetchMerchants()
})
</script>

<template>
  <div class="admin-workbench">
    <!-- ========== 头部概览与指标 ========== -->
    <header class="workbench-header">
      <div class="header-content">
        <div class="header-tag-row">
          <span class="admin-badge">ADMIN CONSOLE</span>
          <span class="live-sync-badge">
            <span class="sync-dot" />
            4 阶段 Agent 决策引擎实时联动
          </span>
        </div>
        <h1 class="workbench-title">⚙️ 灵境导览 · 商户与特色菜品管理工作台</h1>
        <p class="workbench-desc">
          面向衡阳文旅大创答辩与运营验证设计的知识库管控中心。支持快速录入特色餐饮店铺与地道招牌菜品，录入后由 Agent 决策核心优先召回，在 3D 地图时间轴呈现完整的推荐理由与避坑说明。
        </p>

        <!-- 指标概览条 -->
        <div class="metrics-card-row">
          <div class="metric-card">
            <span class="metric-num cyan-num">{{ merchantsList.length }}</span>
            <span class="metric-title">知识库商户总数</span>
          </div>
          <div class="metric-card">
            <span class="metric-num orange-num">
              {{ merchantsList.filter(m => m.isCustomAdded === 1).length }}
            </span>
            <span class="metric-title">自录特色商户</span>
          </div>
          <div class="metric-card">
            <span class="metric-num gold-num">{{ totalDishesCount }}</span>
            <span class="metric-title">已录入招牌菜品</span>
          </div>
        </div>
      </div>
    </header>

    <!-- ========== 全局通知 Toast ========== -->
    <Transition name="toast-fade">
      <div v-if="toastMessage" class="workbench-toast" :class="`toast-${toastType}`">
        <span class="toast-icon">{{ toastType === 'success' ? '✅' : '⚠️' }}</span>
        <span>{{ toastMessage }}</span>
      </div>
    </Transition>

    <!-- ========== 主工作区栅格 ========== -->
    <div class="workbench-grid">
      <!-- 左栏：表单录入区 (Card 1 + Card 2) -->
      <section class="forms-column">
        <!-- ===== Card 1: 新增特色店铺 ===== -->
        <div class="form-card">
          <div class="card-header">
            <div class="card-title-group">
              <span class="card-step-badge">01</span>
              <div>
                <h2 class="card-title">🏪 新增特色店铺 (Add Merchant)</h2>
                <p class="card-subtitle">录入衡阳本地特色餐馆，支持核心地标一键填充坐标</p>
              </div>
            </div>
          </div>

          <form class="card-body" @submit.prevent="handleCreateMerchant">
            <!-- 店铺名称与分类 -->
            <div class="form-row-2">
              <div class="form-group">
                <label class="form-label" for="m-name">店铺名称 <span class="required-star">*</span></label>
                <input
                  id="m-name"
                  v-model="merchantForm.name"
                  type="text"
                  class="form-input"
                  placeholder="如：师院后街·辣妹子擂辣椒土菜馆"
                  required
                />
              </div>

              <div class="form-group">
                <label class="form-label" for="m-cat">店铺分类</label>
                <select id="m-cat" v-model="merchantForm.category" class="form-select">
                  <option value="地道湘菜">地道湘菜</option>
                  <option value="特色粉面">特色粉面</option>
                  <option value="甜品小吃">甜品小吃</option>
                  <option value="夜市烧烤">夜市烧烤</option>
                  <option value="家常小炒">家常小炒</option>
                </select>
              </div>
            </div>

            <!-- 经纬度与一键芯片 -->
            <div class="form-group">
              <label class="form-label">
                GCJ-02 空间经纬度
                <span class="label-hint">（点击下方地标一键填充衡阳真实坐标）</span>
              </label>

              <!-- 地标一键芯片 -->
              <div class="preset-chips-row">
                <button
                  v-for="lm in HENGYANG_LANDMARKS"
                  :key="lm.name"
                  type="button"
                  class="chip-btn"
                  @click="applyLandmark(lm)"
                >
                  📍 {{ lm.name }}
                </button>
              </div>

              <div class="form-row-2 coord-inputs">
                <div class="input-with-affix">
                  <span class="input-affix">经度 Lng</span>
                  <input
                    v-model.number="merchantForm.longitude"
                    type="number"
                    step="0.000001"
                    class="form-input affix-input"
                    required
                  />
                </div>
                <div class="input-with-affix">
                  <span class="input-affix">纬度 Lat</span>
                  <input
                    v-model.number="merchantForm.latitude"
                    type="number"
                    step="0.000001"
                    class="form-input affix-input"
                    required
                  />
                </div>
              </div>
            </div>

            <!-- 人均消费与营业时间 -->
            <div class="form-row-2">
              <div class="form-group">
                <label class="form-label" for="m-price">人均消费 (元)</label>
                <input
                  id="m-price"
                  v-model.number="merchantForm.avgPricePerPerson"
                  type="number"
                  min="5"
                  max="500"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label class="form-label" for="m-hours">营业时间</label>
                <input
                  id="m-hours"
                  v-model="merchantForm.businessHours"
                  type="text"
                  class="form-input"
                  placeholder="如：10:00-22:00"
                />
              </div>
            </div>

            <!-- 详细地址与电话 -->
            <div class="form-row-2">
              <div class="form-group">
                <label class="form-label" for="m-addr">详细地址</label>
                <input
                  id="m-addr"
                  v-model="merchantForm.address"
                  type="text"
                  class="form-input"
                  placeholder="如：衡阳市雁峰区环城南路"
                />
              </div>
              <div class="form-group">
                <label class="form-label" for="m-phone">联系电话</label>
                <input
                  id="m-phone"
                  v-model="merchantForm.phone"
                  type="text"
                  class="form-input"
                  placeholder="如：0734-8889168"
                />
              </div>
            </div>

            <!-- 风味标签 -->
            <div class="form-group">
              <label class="form-label">风味标签 (多选/自定义)</label>
              <div class="preset-chips-row">
                <button
                  v-for="tag in PRESET_FLAVOR_TAGS"
                  :key="tag"
                  type="button"
                  class="chip-toggle"
                  :class="{ active: merchantForm.flavorTags?.includes(tag) }"
                  @click="toggleFlavorTag(tag)"
                >
                  {{ tag }}
                </button>
              </div>
              <input
                v-model="merchantForm.flavorTags"
                type="text"
                class="form-input tags-input"
                placeholder="以英文逗号分隔，如：地道湘菜,豆腐,老字号"
              />
            </div>

            <!-- 提交按钮 -->
            <button
              type="submit"
              class="btn-submit btn-merchant-submit"
              :disabled="isSubmittingMerchant"
            >
              <span v-if="isSubmittingMerchant" class="btn-spinner" />
              <span>{{ isSubmittingMerchant ? '同步保存中...' : '💾 保存并同步商户至知识库' }}</span>
            </button>
          </form>
        </div>

        <!-- ===== Card 2: 录入招牌特色菜品 ===== -->
        <div class="form-card">
          <div class="card-header">
            <div class="card-title-group">
              <span class="card-step-badge step-orange">02</span>
              <div>
                <h2 class="card-title">🍽️ 录入招牌特色菜品 (Add Signature Dish)</h2>
                <p class="card-subtitle">录入店铺招牌美味、价格、辣度与避坑食材说明</p>
              </div>
            </div>
          </div>

          <form class="card-body" @submit.prevent="handleCreateDish">
            <!-- 关联商户下拉 -->
            <div class="form-group">
              <label class="form-label" for="d-merch">关联所属商户 <span class="required-star">*</span></label>
              <select id="d-merch" v-model="dishForm.merchantId" class="form-select" required>
                <option :value="undefined" disabled>请选择关联的餐饮店铺</option>
                <option
                  v-for="m in merchantsList"
                  :key="m.id"
                  :value="m.id"
                >
                  {{ m.name }} ({{ m.category }} · 人均¥{{ m.avgPricePerPerson }})
                </option>
              </select>
            </div>

            <!-- 菜品名称与单价 -->
            <div class="form-row-2">
              <div class="form-group">
                <label class="form-label" for="d-name">菜品名称 <span class="required-star">*</span></label>
                <input
                  id="d-name"
                  v-model="dishForm.name"
                  type="text"
                  class="form-input"
                  placeholder="如：石膏老豆腐炖鲜鱼 / 爆炒麻辣仔鸡"
                  required
                />
              </div>

              <div class="form-group">
                <label class="form-label" for="d-price">菜品单价 (元) <span class="required-star">*</span></label>
                <input
                  id="d-price"
                  v-model.number="dishForm.price"
                  type="number"
                  min="1"
                  step="0.5"
                  class="form-input"
                  required
                />
              </div>
            </div>

            <!-- 辣度等级单选组 -->
            <div class="form-group">
              <label class="form-label">辣度等级</label>
              <div class="spicy-radios-row">
                <label
                  v-for="lvl in ['不辣', '微辣', '中辣', '重辣']"
                  :key="lvl"
                  class="spicy-radio-item"
                  :class="{ active: dishForm.spicyLevel === lvl }"
                >
                  <input
                    v-model="dishForm.spicyLevel"
                    type="radio"
                    :value="lvl"
                    name="spicyLevel"
                    class="sr-only"
                  />
                  <span>{{ lvl === '不辣' ? '🟢' : lvl === '微辣' ? '🟡' : lvl === '中辣' ? '🟠' : '🔴' }} {{ lvl }}</span>
                </label>
              </div>
            </div>

            <!-- 风味口感与避坑/过敏原说明 -->
            <div class="form-group">
              <label class="form-label" for="d-flavor">风味口感描述</label>
              <input
                id="d-flavor"
                v-model="dishForm.flavorNotes"
                type="text"
                class="form-input"
                placeholder="如：汤白浓鲜，豆腐软嫩吸饱鱼汤；外酥里嫩"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="d-warning">
                食材避坑与忌口说明
                <span class="label-hint">（前台智能体据此严格避坑）</span>
              </label>
              <input
                id="d-warning"
                v-model="dishForm.allergensOrIngredients"
                type="text"
                class="form-input"
                placeholder="如：纯素/无内脏/免香菜可选/含猪肚内脏/清真"
              />
            </div>

            <!-- 招牌必点开关 -->
            <div class="form-group signature-switch-row">
              <label class="switch-label">
                <input
                  v-model="dishForm.isSignature"
                  type="checkbox"
                  :true-value="1"
                  :false-value="0"
                  class="switch-checkbox"
                />
                <span class="switch-ui" />
                <span class="switch-text">设为该店铺【招牌必点推荐】</span>
              </label>
            </div>

            <!-- 提交按钮 -->
            <button
              type="submit"
              class="btn-submit btn-dish-submit"
              :disabled="isSubmittingDish"
            >
              <span v-if="isSubmittingDish" class="btn-spinner" />
              <span>{{ isSubmittingDish ? '录入中...' : '🍽️ 录入特色菜品并绑定商户' }}</span>
            </button>
          </form>
        </div>
      </section>

      <!-- 右栏：Card 3 实时商户与招牌菜可视化列表 -->
      <section class="list-column">
        <div class="list-card">
          <!-- 卡片头部与筛选切换 -->
          <div class="list-header">
            <div>
              <h2 class="card-title">📚 实时商户与招牌菜知识库 (Knowledge Base)</h2>
              <p class="card-subtitle">
                共展示 {{ filteredMerchants.length }} 家已装载商户，录入的数据立即在此处与路线引擎生效
              </p>
            </div>

            <div class="filter-tabs">
              <button
                class="filter-tab-btn"
                :class="{ active: activeFilter === 'all' }"
                @click="activeFilter = 'all'"
              >
                全部商户 ({{ merchantsList.length }})
              </button>
              <button
                class="filter-tab-btn"
                :class="{ active: activeFilter === 'custom' }"
                @click="activeFilter = 'custom'"
              >
                仅自录商户 ({{ merchantsList.filter(m => m.isCustomAdded === 1).length }})
              </button>
            </div>
          </div>

          <!-- 商户卡片流 -->
          <div class="merchants-scroll-list">
            <div
              v-for="merchant in filteredMerchants"
              :key="merchant.id || merchant.name"
              class="merchant-item-card"
            >
              <!-- 商户卡片头 -->
              <div class="merchant-top-row">
                <div class="merchant-meta-group">
                  <div class="badge-tag-line">
                    <span class="merchant-category-badge">{{ merchant.category }}</span>
                    <span v-if="merchant.isCustomAdded === 1" class="custom-badge">自录特色商户</span>
                    <span class="rating-badge">⭐ {{ merchant.rating || '4.8' }}</span>
                  </div>
                  <h3 class="merchant-card-title">{{ merchant.name }}</h3>
                  <div class="address-text">📍 {{ merchant.address || '衡阳市核心文旅商圈' }}</div>
                </div>

                <div class="merchant-price-box">
                  <span class="price-val">¥{{ merchant.avgPricePerPerson }}</span>
                  <span class="price-unit">/人均</span>
                </div>
              </div>

              <!-- 经纬度与信息胶囊 -->
              <div class="merchant-info-capsules">
                <span class="capsule">🕒 {{ merchant.businessHours || '10:00-22:00' }}</span>
                <span class="capsule">📞 {{ merchant.phone || '0734-888xxxx' }}</span>
                <span class="capsule coord-capsule">
                  🌐 {{ Number(merchant.longitude).toFixed(4) }}, {{ Number(merchant.latitude).toFixed(4) }}
                </span>
              </div>

              <!-- 招牌菜品列表 -->
              <div class="merchant-dishes-section">
                <div class="dishes-section-label">
                  <span>🍲 招牌特色菜品 ({{ merchant.dishes?.length || 0 }}道)</span>
                </div>

                <div v-if="merchant.dishes && merchant.dishes.length > 0" class="dish-chips-container">
                  <div
                    v-for="dish in merchant.dishes"
                    :key="dish.id || dish.name"
                    class="dish-chip-item"
                  >
                    <div class="dish-chip-main">
                      <span class="chip-dish-name">{{ dish.name }}</span>
                      <span class="chip-dish-price">¥{{ Number(dish.price).toFixed(1) }}</span>
                      <span v-if="dish.spicyLevel" class="chip-spicy">🌶️ {{ dish.spicyLevel }}</span>
                    </div>

                    <div v-if="dish.warning || dish.allergensOrIngredients" class="chip-warning">
                      ⚠️ {{ dish.warning || dish.allergensOrIngredients }}
                    </div>

                    <button
                      type="button"
                      class="btn-delete-dish"
                      title="移除此菜品"
                      @click="handleDeleteDish(merchant, dish)"
                    >
                      ✕
                    </button>
                  </div>
                </div>

                <div v-else class="empty-dishes-hint">
                  <span>暂无录入菜品，可通过左侧“02 录入招牌特色菜品”为其添加</span>
                </div>
              </div>

              <!-- 底部操作按钮 -->
              <div class="merchant-card-actions">
                <button
                  type="button"
                  class="btn-action-exp"
                  title="以此店铺为重点，立即通过 Agent 规划漫游路线并跳转到 3D 地图"
                  @click="handleExperienceRouteWithMerchant(merchant)"
                >
                  <span>✨ 立即以此店生成推荐路线</span>
                </button>

                <button
                  type="button"
                  class="btn-action-del"
                  title="删除该商户"
                  @click="handleDeleteMerchant(merchant)"
                >
                  <span>🗑️ 删除</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* ========== 主工作区容器 ========== */
.admin-workbench {
  min-height: calc(100vh - var(--navbar-height));
  padding: 32px 24px 60px;
  max-width: 1440px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* ========== 头部概览区 ========== */
.workbench-header {
  margin-bottom: 28px;
}

.header-tag-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.admin-badge {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: var(--color-primary, #06B6D4);
  background: var(--color-primary-muted, rgba(6, 182, 212, 0.15));
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(6, 182, 212, 0.3);
}

.live-sync-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #10B981;
  background: rgba(16, 185, 129, 0.12);
  padding: 3px 12px;
  border-radius: 999px;
  border: 1px solid rgba(16, 185, 129, 0.25);
  font-weight: 600;
}

.sync-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 8px #10B981;
}

.workbench-title {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary, #F8FAFC);
  letter-spacing: -0.01em;
}

.workbench-desc {
  margin: 0 0 20px;
  max-width: 860px;
  font-size: 14px;
  color: var(--text-secondary, #94A3B8);
  line-height: 1.6;
}

/* 统计卡片条 */
.metrics-card-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  max-width: 680px;
}

.metric-card {
  background: var(--surface-card-glass, rgba(15, 23, 42, 0.85));
  backdrop-filter: blur(16px);
  border: 1px solid var(--border-subtle, rgba(148, 163, 184, 0.12));
  border-radius: 12px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

.metric-num {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.2;
}

.cyan-num { color: var(--color-primary, #06B6D4); }
.orange-num { color: var(--color-dining, #F97316); }
.gold-num { color: var(--color-accent-gold, #F59E0B); }

.metric-title {
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

/* ========== 全局通知 Toast ========== */
.workbench-toast {
  position: fixed;
  top: 80px;
  right: 24px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border-radius: 12px;
  background: var(--surface-modal, #0B1120);
  backdrop-filter: blur(16px);
  border: 1px solid var(--border-default);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.6);
  z-index: 300;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #F8FAFC);
}

.toast-success {
  border-color: #10B981;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.25);
}

.toast-warning {
  border-color: #F59E0B;
}

.toast-error {
  border-color: #EF4444;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-16px);
}

/* ========== 主工作区栅格 ========== */
.workbench-grid {
  display: grid;
  grid-template-columns: 520px 1fr;
  gap: 28px;
  align-items: start;
}

/* 左栏：表单列 */
.forms-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-card,
.list-card {
  background: var(--surface-card-glass, rgba(15, 23, 42, 0.85));
  backdrop-filter: blur(16px);
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.45);
}

.card-header {
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle, rgba(148, 163, 184, 0.12));
}

.card-title-group {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.card-step-badge {
  font-size: 13px;
  font-weight: 800;
  color: var(--color-primary, #06B6D4);
  background: var(--color-primary-muted);
  border: 1px solid rgba(6, 182, 212, 0.3);
  padding: 4px 8px;
  border-radius: 8px;
}

.step-orange {
  color: var(--color-dining, #F97316);
  background: rgba(249, 115, 22, 0.15);
  border-color: rgba(249, 115, 22, 0.3);
}

.card-title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary, #F8FAFC);
}

.card-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

/* 表单组件 */
.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary, #94A3B8);
  display: flex;
  align-items: center;
  gap: 4px;
}

.required-star {
  color: #EF4444;
}

.label-hint {
  font-size: 11px;
  color: var(--color-primary, #06B6D4);
  font-weight: 400;
}

.form-input,
.form-select {
  width: 100%;
  min-height: 44px;
  box-sizing: border-box;
  padding: 0 14px;
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.65);
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  color: var(--text-primary, #F8FAFC);
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-input:focus,
.form-select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-muted);
  outline: none;
}

/* 经纬度输入与地标芯片 */
.preset-chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.chip-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: var(--text-secondary, #94A3B8);
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  min-height: 32px;
}

.chip-btn:hover {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.chip-toggle {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(148, 163, 184, 0.15);
  color: var(--text-secondary, #94A3B8);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip-toggle.active {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

.input-with-affix {
  position: relative;
  display: flex;
  align-items: center;
}

.input-affix {
  position: absolute;
  left: 10px;
  font-size: 11px;
  color: var(--text-muted, #64748B);
  font-weight: 600;
  pointer-events: none;
}

.affix-input {
  padding-left: 68px;
}

/* 辣度单选组 */
.spicy-radios-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.spicy-radio-item {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.spicy-radio-item.active {
  background: rgba(249, 115, 22, 0.15);
  border-color: var(--color-dining, #F97316);
  color: var(--color-dining, #F97316);
  box-shadow: 0 0 12px rgba(249, 115, 22, 0.25);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

/* 开关控件 */
.signature-switch-row {
  margin-top: 4px;
}

.switch-label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.switch-checkbox {
  position: absolute;
  opacity: 0;
}

.switch-ui {
  position: relative;
  width: 44px;
  height: 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.15);
  transition: background 0.2s ease;
}

.switch-ui::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #FFFFFF;
  transition: transform 0.2s ease;
}

.switch-checkbox:checked + .switch-ui {
  background: var(--color-dining, #F97316);
}

.switch-checkbox:checked + .switch-ui::after {
  transform: translateX(20px);
}

.switch-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 提交按键 */
.btn-submit {
  width: 100%;
  min-height: 48px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.4);
}

.btn-merchant-submit {
  background: linear-gradient(135deg, #0891B2 0%, #06B6D4 100%);
  color: #FFFFFF;
}

.btn-merchant-submit:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(6, 182, 212, 0.45);
  transform: translateY(-1px);
}

.btn-dish-submit {
  background: linear-gradient(135deg, #EA580C 0%, #F97316 100%);
  color: #FFFFFF;
}

.btn-dish-submit:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(249, 115, 22, 0.45);
  transform: translateY(-1px);
}

.btn-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #FFFFFF;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== 右栏：知识库列表 ========== */
.list-column {
  min-width: 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.filter-tab-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  min-height: 38px;
  transition: all 0.2s ease;
}

.filter-tab-btn.active {
  background: var(--color-primary-muted);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.merchants-scroll-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 900px;
  overflow-y: auto;
  padding-right: 6px;
  scrollbar-width: thin;
}

/* 商户卡片 */
.merchant-item-card {
  background: rgba(2, 6, 23, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 14px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.2s ease;
}

.merchant-item-card:hover {
  border-color: rgba(6, 182, 212, 0.4);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.merchant-top-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.badge-tag-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.merchant-category-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(6, 182, 212, 0.15);
  color: var(--color-primary);
  border: 1px solid rgba(6, 182, 212, 0.3);
}

.custom-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(249, 115, 22, 0.15);
  color: var(--color-dining);
  border: 1px solid rgba(249, 115, 22, 0.3);
}

.rating-badge {
  font-size: 11px;
  font-weight: 600;
  color: #F59E0B;
}

.merchant-card-title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.address-text {
  font-size: 12px;
  color: var(--text-secondary);
}

.merchant-price-box {
  text-align: right;
  flex-shrink: 0;
}

.price-val {
  font-size: 18px;
  font-weight: 800;
  color: var(--color-dining);
}

.price-unit {
  font-size: 11px;
  color: var(--text-muted);
}

/* 胶囊信息条 */
.merchant-info-capsules {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.capsule {
  font-size: 11px;
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.04);
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.coord-capsule {
  font-family: var(--font-mono, monospace);
  color: var(--color-primary);
}

/* 菜品展示 */
.merchant-dishes-section {
  background: rgba(15, 23, 42, 0.6);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.dishes-section-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-dining);
  margin-bottom: 8px;
}

.dish-chips-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dish-chip-item {
  position: relative;
  background: rgba(2, 6, 23, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 8px;
  padding: 8px 32px 8px 10px;
}

.dish-chip-main {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 2px;
}

.chip-dish-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.chip-dish-price {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-dining);
}

.chip-spicy {
  font-size: 11px;
  color: #EF4444;
}

.chip-warning {
  font-size: 11px;
  color: #FCD34D;
}

.btn-delete-dish {
  position: absolute;
  right: 6px;
  top: 6px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.15s ease;
}

.btn-delete-dish:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #EF4444;
}

.empty-dishes-hint {
  font-size: 12px;
  color: var(--text-muted);
  font-style: italic;
  padding: 4px 0;
}

/* 操作按键栏 */
.merchant-card-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.btn-action-exp {
  flex: 1;
  min-height: 38px;
  background: var(--color-primary-muted);
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.btn-action-exp:hover {
  background: var(--color-primary);
  color: var(--text-inverse);
  box-shadow: 0 0 16px var(--color-primary-glow);
}

.btn-action-del {
  padding: 0 16px;
  min-height: 38px;
  background: transparent;
  border: 1px solid var(--border-default);
  color: var(--text-muted);
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-del:hover {
  border-color: #EF4444;
  color: #EF4444;
  background: rgba(239, 68, 68, 0.1);
}

/* ========== 响应式与移动端适配 ========== */
@media (max-width: 1080px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .admin-workbench {
    padding: 20px 14px 40px;
  }

  .form-row-2 {
    grid-template-columns: 1fr;
  }

  .form-input,
  .form-select {
    font-size: 16px !important; /* 防止移动端聚焦缩放 */
  }

  .spicy-radios-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
