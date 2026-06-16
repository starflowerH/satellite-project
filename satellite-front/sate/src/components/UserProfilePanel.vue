<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { heroApi } from '@/api/hero'
import type { HeroOption } from '@/api/hero'
import MainHeroSelector from '@/components/MainHeroSelector.vue'
import { useUserStore } from '@/store/user'
import { locateCurrentCity } from '@/utils/geolocation'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const HERO_LIMIT = 5

const userStore = useUserStore()

const isEditMode = ref(false)
const isSaving = ref(false)
const isHeroLoading = ref(false)
const isLocating = ref(false)
const heroOptions = ref<HeroOption[]>([])
const heroKeyword = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const editForm = ref({
  name: '',
  avatar: '',
  signature: '',
  city: '',
  mainHeroes: [] as string[],
})

const profile = computed(() => userStore.userInfo)

const normalizeHeroes = (heroes: string[]): string[] => {
  const result: string[] = []

  for (const hero of heroes.map((item) => item.trim()).filter(Boolean)) {
    if (result.includes(hero)) continue
    result.push(hero)
    if (result.length >= HERO_LIMIT) break
  }

  return result
}

const resolveHeroIds = (heroNames: string[]): string[] => {
  const uniqueNames = normalizeHeroes(heroNames)
  const ids: string[] = []
  const seen = new Set<string>()

  for (const name of uniqueNames) {
    const matched = heroOptions.value.find((hero) => hero.name === name)
    const heroId = matched?.heroId?.trim()
    if (!heroId || seen.has(heroId)) continue
    seen.add(heroId)
    ids.push(heroId)
  }

  return ids
}

const syncEditForm = () => {
  editForm.value = {
    name: profile.value.name,
    avatar: profile.value.avatar,
    signature: profile.value.signature,
    city: profile.value.city,
    mainHeroes: [...profile.value.mainHeroes],
  }
}

const loadHeroOptions = async (keyword = heroKeyword.value) => {
  if (isHeroLoading.value) return

  isHeroLoading.value = true

  try {
    heroOptions.value = await heroApi.listHeroes({ keyword })
  } catch (error) {
    console.error('获取英雄列表失败:', error)
    messageType.value = 'error'
    message.value = '英雄列表加载失败，请稍后重试'
  } finally {
    isHeroLoading.value = false
  }
}

const handleHeroSearchChange = (keyword: string) => {
  heroKeyword.value = keyword.trim()
  void loadHeroOptions(heroKeyword.value)
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) return

    isEditMode.value = false
    message.value = ''
    syncEditForm()

    void (async () => {
      await Promise.all([userStore.fetchUserProfile(), loadHeroOptions()])
      syncEditForm()
    })()
  },
)

const closePanel = () => {
  emit('close')
}

const startEdit = () => {
  message.value = ''
  isEditMode.value = true
  syncEditForm()
  void loadHeroOptions()
}

const cancelEdit = () => {
  message.value = ''
  isEditMode.value = false
  syncEditForm()
}

const handleHeroLimit = (limit: number) => {
  messageType.value = 'error'
  message.value = `本命英雄最多选择 ${limit} 位`
}

const handleLocateCity = async () => {
  if (isLocating.value) return

  isLocating.value = true
  message.value = ''

  try {
    const result = await locateCurrentCity()
    editForm.value.city = result.city
    messageType.value = 'success'
    message.value = `定位成功：${result.city}`
  } catch (error) {
    console.error('城市定位失败:', error)
    messageType.value = 'error'
    message.value = '定位失败，请检查定位权限后重试'
  } finally {
    isLocating.value = false
  }
}

const saveProfile = async () => {
  const name = editForm.value.name.trim()
  const avatar = editForm.value.avatar.trim()
  const signature = editForm.value.signature.trim()
  const city = editForm.value.city.trim()
  const mainHeroes = normalizeHeroes(editForm.value.mainHeroes)
  const userId = userStore.currentUserId

  if (!userId) {
    console.error('错误：未获取到当前登录用户ID')
    messageType.value = 'error'
    message.value = '保存失败：未获取到用户ID，请重新登录后重试'
    return
  }

  if (!name) {
    messageType.value = 'error'
    message.value = '网名不能为空'
    return
  }

  if (name.length > 24) {
    messageType.value = 'error'
    message.value = '网名长度不能超过 24 个字符'
    return
  }

  if (signature.length > 80) {
    messageType.value = 'error'
    message.value = '个性签名不能超过 80 个字符'
    return
  }

  if (!mainHeroes.length) {
    messageType.value = 'error'
    message.value = '请至少选择 1 位本命英雄'
    return
  }

  if (mainHeroes.length > HERO_LIMIT) {
    messageType.value = 'error'
    message.value = `本命英雄最多选择 ${HERO_LIMIT} 位`
    return
  }

  const heroIds = resolveHeroIds(mainHeroes)
  if (heroIds.length !== mainHeroes.length) {
    messageType.value = 'error'
    message.value = '部分英雄缺少有效 ID，请重新搜索后再保存'
    return
  }

  isSaving.value = true

  try {
    const profileSuccess = await userStore.updateUserProfile({
      userId,
      name,
      avatar,
      signature,
    })

    if (!profileSuccess) {
      messageType.value = 'error'
      message.value = '资料保存失败，请稍后重试'
      return
    }

    const heroSuccess = await userStore.saveMainHeroes(heroIds)

    if (!heroSuccess) {
      messageType.value = 'error'
      message.value = '本命英雄保存失败，请稍后重试'
      return
    }

    if (city) {
      userStore.setLocalCity(city)
    }

    messageType.value = 'success'
    message.value = '修改成功'
    isEditMode.value = false
    syncEditForm()
  } finally {
    isSaving.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="profile-fade">
      <div v-if="props.visible" class="profile-overlay" @click.self="closePanel">
        <section class="profile-panel" :class="{ 'profile-panel--edit': isEditMode }">
          <header class="panel-header">
            <div class="title-box">
              <p class="label">Pilot Profile</p>
              <h3>个人资料中心</h3>
            </div>
            <button class="icon-btn" type="button" @click="closePanel">×</button>
          </header>

          <div class="profile-main">
            <div class="avatar-wrap">
              <img :src="userStore.displayAvatar" :alt="userStore.displayName" class="avatar" />
            </div>

            <template v-if="!isEditMode">
              <div class="info-grid">
                <div class="info-item">
                  <span class="item-label">网名</span>
                  <span class="item-value">{{ userStore.displayName }}</span>
                </div>
                <div class="info-item">
                  <span class="item-label">个性签名</span>
                  <span class="item-value item-sign">{{ profile.signature || '这个人很神秘，什么都没留下。' }}</span>
                </div>
                <div class="info-item">
                  <span class="item-label">所在城市</span>
                  <span class="item-value">{{ profile.city || '未定位' }}</span>
                </div>
                <div class="info-item">
                  <span class="item-label">本命英雄</span>
                  <div v-if="profile.mainHeroes.length" class="hero-badges">
                    <span v-for="hero in profile.mainHeroes" :key="hero" class="hero-badge">{{ hero }}</span>
                  </div>
                  <span v-else class="item-value">暂未设置</span>
                </div>
                <div class="info-item">
                  <span class="item-label">手机号</span>
                  <span class="item-value">{{ profile.phone || '未绑定' }}</span>
                </div>
                <div class="info-item">
                  <span class="item-label">邮箱</span>
                  <span class="item-value">{{ profile.email || '未绑定' }}</span>
                </div>
              </div>

              <div class="action-row">
                <button class="action-btn action-btn--primary" type="button" @click="startEdit">编辑资料</button>
                <button class="action-btn action-btn--danger" type="button" @click="handleLogout">退出登录</button>
              </div>
            </template>

            <template v-else>
              <div class="form-grid">
                <label class="field-label" for="name">网名</label>
                <input id="name" v-model="editForm.name" type="text" class="field-input" placeholder="请输入你的网名" maxlength="24" />

                <label class="field-label" for="avatar">头像 URL</label>
                <input id="avatar" v-model="editForm.avatar" type="text" class="field-input" placeholder="https://..." />

                <label class="field-label" for="signature">个性签名</label>
                <textarea
                  id="signature"
                  v-model="editForm.signature"
                  class="field-input field-textarea"
                  rows="3"
                  maxlength="80"
                  placeholder="写点你的战场宣言..."
                />

                <label class="field-label" for="city">所在城市</label>
                <div class="city-row">
                  <input id="city" v-model="editForm.city" type="text" class="field-input" placeholder="点击右侧按钮自动定位" />
                  <button class="locate-btn" type="button" :disabled="isLocating" @click="handleLocateCity">
                    {{ isLocating ? '定位中...' : '自动定位' }}
                  </button>
                </div>

                <label class="field-label">本命英雄</label>
                <div class="hero-editor">
                  <MainHeroSelector
                    v-model="editForm.mainHeroes"
                    :heroes="heroOptions"
                    :loading="isHeroLoading"
                    :limit="HERO_LIMIT"
                    @limit-exceeded="handleHeroLimit"
                    @search-change="handleHeroSearchChange"
                  />
                  <p class="field-tip">可搜索并勾选本命英雄，至少选择 1 位，最多 5 位。</p>
                </div>
              </div>

              <p v-if="message" class="feedback" :class="`feedback--${messageType}`">{{ message }}</p>

              <div class="action-row edit-action-row">
                <button class="action-btn action-btn--primary" type="button" :disabled="isSaving" @click="saveProfile">
                  {{ isSaving ? '保存中...' : '保存修改' }}
                </button>
                <button class="action-btn" type="button" :disabled="isSaving" @click="cancelEdit">取消</button>
              </div>
            </template>

            <p v-if="message && !isEditMode" class="feedback" :class="`feedback--${messageType}`">{{ message }}</p>
          </div>

          <span class="neon-border neon-border--cyan" aria-hidden="true" />
          <span class="neon-border neon-border--blue" aria-hidden="true" />
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.profile-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(1, 6, 14, 0.55);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  z-index: 1200;
}

.profile-panel {
  position: relative;
  width: min(720px, calc(100vw - 32px));
  max-height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  background: linear-gradient(160deg, rgba(11, 16, 28, 0.82), rgba(8, 12, 22, 0.92));
  border: 1px solid rgba(96, 149, 217, 0.24);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.52),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(127, 175, 226, 0.15);
}

.title-box .label {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(120, 226, 255, 0.82);
}

.title-box h3 {
  margin: 6px 0 0;
  font-size: 22px;
  color: #eef6ff;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.03);
  color: #c9d9ee;
  font-size: 22px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.icon-btn:hover {
  border-color: rgba(0, 229, 255, 0.45);
  box-shadow: 0 0 18px rgba(0, 229, 255, 0.2);
}

.profile-main {
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 229, 255, 0.45) rgba(8, 17, 32, 0.35);
}

.profile-main::-webkit-scrollbar {
  width: 8px;
}

.profile-main::-webkit-scrollbar-track {
  background: rgba(8, 17, 32, 0.35);
}

.profile-main::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(0, 229, 255, 0.36);
}

.profile-panel--edit .profile-main {
  padding-top: 16px;
}

.profile-panel--edit .avatar-wrap {
  margin-bottom: 12px;
}

.profile-panel--edit .avatar {
  width: 64px;
  height: 64px;
}

.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.avatar {
  width: 82px;
  height: 82px;
  border-radius: 50%;
  border: 2px solid rgba(0, 229, 255, 0.6);
  box-shadow:
    0 0 22px rgba(0, 229, 255, 0.28),
    0 0 36px rgba(0, 119, 255, 0.22);
}

.info-grid,
.form-grid {
  display: grid;
  gap: 12px;
}

.profile-panel--edit .form-grid {
  gap: 10px;
}

.info-item {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(123, 160, 201, 0.18);
  background: rgba(12, 20, 35, 0.56);
}

.item-label {
  display: block;
  font-size: 12px;
  color: rgba(157, 184, 212, 0.78);
  margin-bottom: 6px;
}

.item-value {
  color: #ecf6ff;
  font-size: 14px;
  word-break: break-all;
}

.item-sign {
  white-space: pre-wrap;
}

.hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(0, 229, 255, 0.12);
  border: 1px solid rgba(0, 229, 255, 0.2);
  color: #d9fbff;
  font-size: 13px;
}

.field-label {
  font-size: 13px;
  color: rgba(174, 201, 230, 0.8);
}

.field-input {
  width: 100%;
  border: 1px solid rgba(121, 170, 221, 0.22);
  border-radius: 12px;
  background: rgba(8, 17, 32, 0.65);
  color: #eef7ff;
  padding: 12px 14px;
  font-size: 14px;
  transition: all var(--transition-fast);
}

.field-input:focus {
  outline: none;
  border-color: rgba(0, 229, 255, 0.6);
  box-shadow: 0 0 0 3px rgba(0, 229, 255, 0.12);
}

.field-textarea {
  resize: vertical;
  min-height: 88px;
}

.city-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.locate-btn {
  height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(0, 229, 255, 0.4);
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.2), rgba(0, 119, 255, 0.2));
  color: #e8fbff;
  padding: 0 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.locate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hero-editor {
  display: grid;
  gap: 10px;
}

.field-tip {
  margin: 0;
  font-size: 12px;
  color: rgba(172, 199, 225, 0.76);
}

.action-row {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.edit-action-row {
  position: sticky;
  bottom: -20px;
  z-index: 2;
  margin: 14px -20px -20px;
  padding: 12px 20px 16px;
  background:
    linear-gradient(180deg, rgba(8, 12, 22, 0), rgba(8, 12, 22, 0.94) 24%),
    rgba(8, 12, 22, 0.94);
  border-top: 1px solid rgba(127, 175, 226, 0.14);
  backdrop-filter: blur(12px);
}

.action-btn {
  flex: 1;
  height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(114, 164, 219, 0.22);
  background: rgba(21, 30, 48, 0.48);
  color: #e2efff;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn--primary {
  border-color: rgba(0, 229, 255, 0.4);
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.2), rgba(0, 119, 255, 0.2));
}

.action-btn--danger {
  border-color: rgba(255, 51, 102, 0.35);
  color: #ff9eb8;
}

.feedback {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  text-align: center;
}

.feedback--success {
  color: #bff9ff;
  background: rgba(0, 229, 255, 0.12);
  border: 1px solid rgba(0, 229, 255, 0.26);
}

.feedback--error {
  color: #ffb9cd;
  background: rgba(255, 51, 102, 0.12);
  border: 1px solid rgba(255, 51, 102, 0.3);
}

.neon-border {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: 24px;
}

.neon-border--cyan {
  border: 1px solid rgba(0, 229, 255, 0.28);
  box-shadow: 0 0 28px rgba(0, 229, 255, 0.2);
  animation: neon-breath-cyan 2.8s ease-in-out infinite;
}

.neon-border--blue {
  border: 1px solid rgba(0, 119, 255, 0.26);
  box-shadow: 0 0 32px rgba(0, 119, 255, 0.22);
  animation: neon-breath-blue 3.2s ease-in-out infinite;
}

.profile-fade-enter-active,
.profile-fade-leave-active {
  transition: opacity 0.24s ease;
}

.profile-fade-enter-from,
.profile-fade-leave-to {
  opacity: 0;
}

@keyframes neon-breath-cyan {
  0%,
  100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.9;
  }
}

@keyframes neon-breath-blue {
  0%,
  100% {
    opacity: 0.28;
  }
  50% {
    opacity: 0.78;
  }
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .profile-overlay {
    padding: 16px;
  }

  .profile-panel {
    width: 100%;
    max-width: 480px;
    max-height: calc(100vh - 32px);
    border-radius: 20px;
  }

  .panel-header {
    padding: 16px 18px 14px;
  }

  .profile-main {
    padding: 18px;
  }

  .edit-action-row {
    bottom: -18px;
    margin: 14px -18px -18px;
    padding: 12px 18px 16px;
  }
}

@media (max-width: 560px) {
  .profile-overlay {
    padding: 0;
    align-items: flex-end;
  }

  .profile-panel {
    width: 100%;
    max-width: none;
    max-height: 90vh;
    border-radius: 20px 20px 0 0;
  }

  .panel-header {
    padding: 14px 16px 12px;
  }

  .profile-main {
    padding: 16px;
  }

  .edit-action-row {
    bottom: -16px;
    margin: 12px -16px -16px;
    padding: 12px 16px calc(14px + var(--safe-area-bottom));
  }

  .city-row {
    grid-template-columns: 1fr;
  }

  .action-row {
    flex-direction: column;
  }

  .action-btn {
    min-height: 48px;
  }
}
</style>
