<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/store/user'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success'): void
}>()

const userStore = useUserStore()

type Mode = 'login' | 'register' | 'reset'
type LoginMethod = 'password' | 'phoneCode' | 'emailCode'
type RegisterMethod = 'phone' | 'email'

const currentMode = ref<Mode>('login')
const currentLoginMethod = ref<LoginMethod>('password')
const currentRegisterMethod = ref<RegisterMethod>('phone')

const formData = ref({
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
  loginAccount: '',
})

const resetForm = ref({
  phone: '',
  code: '',
  password: '',
  confirmPassword: '',
})

const isLoading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

const codeCountdown = ref(0)
const codeTimer = ref<ReturnType<typeof setInterval> | null>(null)

const modalTitle = computed(() => {
  if (currentMode.value === 'login') return '欢迎使用灵境导览'
  if (currentMode.value === 'register') return '加入灵境导览'
  return '重置密码'
})

const modalSubtitle = computed(() => {
  if (currentMode.value === 'login') return '登录以同步您的出行画像与专属路线'
  if (currentMode.value === 'register') return '注册新账号，开启专属衡阳文旅定制'
  return '通过手机验证码重置密码'
})

const codeButtonText = computed(() =>
  codeCountdown.value > 0 ? `${codeCountdown.value}s 后重试` : '获取验证码',
)

const isCodeButtonDisabled = computed(() => {
  if (codeCountdown.value > 0) return true

  if (currentMode.value === 'login') {
    if (currentLoginMethod.value === 'phoneCode') {
      return !formData.value.phone.trim()
    }

    if (currentLoginMethod.value === 'emailCode') {
      return !formData.value.email.trim()
    }
  }

  if (currentMode.value === 'register') {
    if (currentRegisterMethod.value === 'phone') {
      return !formData.value.phone.trim()
    }

    return !formData.value.email.trim()
  }

  if (currentMode.value === 'reset') {
    return !resetForm.value.phone.trim()
  }

  return false
})

const submitButtonLabel = computed(() => {
  if (currentMode.value === 'login') return '登录'
  if (currentMode.value === 'register') return '注册'
  return '确认重置'
})

const clearMessages = () => {
  errorMsg.value = ''
  successMsg.value = ''
}

const resetCodeTimer = () => {
  if (codeTimer.value) {
    clearInterval(codeTimer.value)
    codeTimer.value = null
  }
  codeCountdown.value = 0
}

const startCodeCountdown = () => {
  codeCountdown.value = 60

  if (codeTimer.value) {
    clearInterval(codeTimer.value)
  }

  codeTimer.value = setInterval(() => {
    codeCountdown.value -= 1

    if (codeCountdown.value <= 0 && codeTimer.value) {
      clearInterval(codeTimer.value)
      codeTimer.value = null
    }
  }, 1000)
}

const resetForms = () => {
  formData.value = {
    phone: '',
    email: '',
    password: '',
    confirmPassword: '',
    code: '',
    loginAccount: '',
  }

  resetForm.value = {
    phone: '',
    code: '',
    password: '',
    confirmPassword: '',
  }

  currentMode.value = 'login'
  currentLoginMethod.value = 'password'
  currentRegisterMethod.value = 'phone'
  isLoading.value = false
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) return
    clearMessages()
  },
)

const handleGetCode = async () => {
  clearMessages()

  try {
    let response

    if (currentMode.value === 'login' && currentLoginMethod.value === 'phoneCode') {
      const phone = formData.value.phone.trim()
      if (!phone) {
        errorMsg.value = '请先输入手机号'
        return
      }
      response = await authApi.sendPhoneCode(phone)
    } else if (currentMode.value === 'login' && currentLoginMethod.value === 'emailCode') {
      const email = formData.value.email.trim()
      if (!email) {
        errorMsg.value = '请先输入邮箱'
        return
      }
      response = await authApi.sendEmailCode(email)
    } else if (currentMode.value === 'register') {
      if (currentRegisterMethod.value === 'phone') {
        const phone = formData.value.phone.trim()
        if (!phone) {
          errorMsg.value = '请先输入手机号'
          return
        }
        response = await authApi.sendPhoneCode(phone)
      } else {
        const email = formData.value.email.trim()
        if (!email) {
          errorMsg.value = '请先输入邮箱'
          return
        }
        response = await authApi.sendEmailCode(email)
      }
    } else {
      const phone = resetForm.value.phone.trim()
      if (!phone) {
        errorMsg.value = '请先输入手机号'
        return
      }
      response = await authApi.sendPhoneCode(phone)
    }

    if (response?.code === '200' || response?.code === '0' || (response && !('code' in response))) {
      successMsg.value = '验证码已发送'
      startCodeCountdown()
      return
    }

    errorMsg.value = response?.message || '发送验证码失败'
  } catch (error) {
    errorMsg.value = '发送验证码异常'
    console.error(error)
  }
}

const finishLoginFlow = () => {
  successMsg.value = '登录成功'
  setTimeout(() => {
    emit('success')
    handleClose()
  }, 300)
}

const handleLogin = async () => {
  clearMessages()

  const password = formData.value.password
  const account = formData.value.loginAccount.trim()
  const phone = formData.value.phone.trim()
  const email = formData.value.email.trim()
  const code = formData.value.code.trim()

  if (currentLoginMethod.value === 'password') {
    if (!account || !password) {
      errorMsg.value = '请输入账号和密码'
      return
    }
  } else if (currentLoginMethod.value === 'phoneCode') {
    if (!phone || !code) {
      errorMsg.value = '请输入手机号和验证码'
      return
    }
  } else if (!email || !code) {
    errorMsg.value = '请输入邮箱和验证码'
    return
  }

  isLoading.value = true

  try {
    let success = false

    if (currentLoginMethod.value === 'password') {
      success = await userStore.loginByPassword(account, password)
    } else if (currentLoginMethod.value === 'phoneCode') {
      success = await userStore.loginByPhoneCode(phone, code)
    } else {
      success = await userStore.loginByEmailCode(email, code)
    }

    if (!success) {
      errorMsg.value = '登录失败，请检查输入信息'
      return
    }

    await finishLoginFlow()
  } catch (error) {
    errorMsg.value = '登录异常，请稍后重试'
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

const handleRegister = async () => {
  clearMessages()

  const phone = formData.value.phone.trim()
  const email = formData.value.email.trim()
  const code = formData.value.code.trim()
  const password = formData.value.password
  const confirmPassword = formData.value.confirmPassword

  const isEmailMode = currentRegisterMethod.value === 'email'
  const account = isEmailMode ? email : phone

  if (!account || !code || !password || !confirmPassword) {
    errorMsg.value = '请填写所有必填字段'
    return
  }

  if (password !== confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  if (password.length < 6) {
    errorMsg.value = '密码长度至少 6 位'
    return
  }

  isLoading.value = true

  try {
    const registerSuccess = await userStore.register(account, password, code, isEmailMode)

    if (!registerSuccess) {
      errorMsg.value = '注册失败，请检查输入信息'
      return
    }

    successMsg.value = '注册成功，正在自动登录...'

    const loginSuccess = await userStore.loginByPassword(account, password)

    if (!loginSuccess) {
      errorMsg.value = '自动登录失败，请手动登录'
      successMsg.value = ''
      currentMode.value = 'login'
      currentLoginMethod.value = 'password'
      formData.value.loginAccount = account
      formData.value.password = password
      return
    }

    await finishLoginFlow()
  } catch (error) {
    errorMsg.value = '注册异常，请稍后重试'
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

const handleResetPassword = async () => {
  clearMessages()

  const phone = resetForm.value.phone.trim()
  const code = resetForm.value.code.trim()
  const password = resetForm.value.password
  const confirmPassword = resetForm.value.confirmPassword

  if (!phone || !code || !password || !confirmPassword) {
    errorMsg.value = '请填写所有必填字段'
    return
  }

  if (password !== confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  if (password.length < 6) {
    errorMsg.value = '密码长度至少 6 位'
    return
  }

  isLoading.value = true

  try {
    const success = await userStore.resetPassword({
      phone,
      code,
      password,
    })

    if (!success) {
      errorMsg.value = '密码重置失败，请检查输入信息'
      return
    }

    successMsg.value = '密码重置成功，请使用新密码登录'
    resetForm.value = {
      phone: '',
      code: '',
      password: '',
      confirmPassword: '',
    }

    setTimeout(() => {
      currentMode.value = 'login'
      currentLoginMethod.value = 'password'
      successMsg.value = ''
    }, 2000)
  } catch (error) {
    errorMsg.value = '密码重置异常，请稍后重试'
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

const handleSubmit = () => {
  if (currentMode.value === 'login') {
    void handleLogin()
    return
  }

  if (currentMode.value === 'register') {
    void handleRegister()
    return
  }

  void handleResetPassword()
}

const switchLoginMethod = (method: LoginMethod) => {
  currentLoginMethod.value = method
  clearMessages()
  formData.value.code = ''
}

const switchRegisterMethod = (method: RegisterMethod) => {
  currentRegisterMethod.value = method
  clearMessages()
  formData.value.code = ''
}

const switchToRegister = () => {
  currentMode.value = 'register'
  currentLoginMethod.value = 'password'
  currentRegisterMethod.value = 'phone'
  clearMessages()
}

const switchToReset = () => {
  currentMode.value = 'reset'
  clearMessages()
  resetForm.value = {
    phone: '',
    code: '',
    password: '',
    confirmPassword: '',
  }
}

const switchToLogin = () => {
  currentMode.value = 'login'
  currentLoginMethod.value = 'password'
  clearMessages()
}

const handleClose = () => {
  resetCodeTimer()
  clearMessages()
  resetForms()
  emit('close')
}

const handleGlobalKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && props.visible) {
    handleClose()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  resetCodeTimer()
  window.removeEventListener('keydown', handleGlobalKeydown)
})

watch(
  () => props.visible,
  (val) => {
    if (!val) {
      resetCodeTimer()
    }
  },
)
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="props.visible" class="modal-overlay" @click.self="handleClose()">
        <div class="modal-container">
          <button class="close-btn touch-target" type="button" aria-label="关闭" @click="handleClose()">×</button>

          <div class="modal-header">
            <div class="logo-icon">🛰️</div>
            <h2 class="modal-title">{{ modalTitle }}</h2>
            <p class="modal-subtitle">{{ modalSubtitle }}</p>
          </div>

          <div v-if="currentMode === 'login'" class="login-tabs">
            <button
              class="tab-btn"
              :class="{ active: currentLoginMethod === 'password' }"
              @click="switchLoginMethod('password')"
            >
              密码登录
            </button>
            <button
              class="tab-btn"
              :class="{ active: currentLoginMethod === 'phoneCode' }"
              @click="switchLoginMethod('phoneCode')"
            >
              手机验证码
            </button>
            <button
              class="tab-btn"
              :class="{ active: currentLoginMethod === 'emailCode' }"
              @click="switchLoginMethod('emailCode')"
            >
              邮箱登录
            </button>
          </div>

          <div v-if="currentMode === 'register'" class="login-tabs">
            <button
              class="tab-btn"
              :class="{ active: currentRegisterMethod === 'phone' }"
              @click="switchRegisterMethod('phone')"
            >
              手机注册
            </button>
            <button
              class="tab-btn"
              :class="{ active: currentRegisterMethod === 'email' }"
              @click="switchRegisterMethod('email')"
            >
              邮箱注册
            </button>
          </div>

          <form class="login-form" @submit.prevent="handleSubmit">
            <template v-if="currentMode === 'login'">
              <template v-if="currentLoginMethod === 'password'">
                <div class="input-group">
                  <label class="input-label">账号</label>
                  <input
                    v-model="formData.loginAccount"
                    type="text"
                    class="input-field"
                    placeholder="请输入手机号或邮箱"
                  />
                </div>

                <div class="input-group">
                  <label class="input-label">密码</label>
                  <input
                    v-model="formData.password"
                    type="password"
                    class="input-field"
                    placeholder="请输入密码"
                  />
                </div>
              </template>

              <template v-else-if="currentLoginMethod === 'phoneCode'">
                <div class="input-group">
                  <label class="input-label">手机号</label>
                  <input
                    v-model="formData.phone"
                    type="tel"
                    class="input-field"
                    placeholder="请输入手机号"
                    maxlength="11"
                  />
                </div>

                <div class="input-group">
                  <label class="input-label">验证码</label>
                  <div class="code-input-group">
                    <input
                      v-model="formData.code"
                      type="text"
                      class="input-field"
                      placeholder="请输入验证码"
                      maxlength="6"
                    />
                    <button
                      type="button"
                      class="code-btn"
                      :disabled="isCodeButtonDisabled"
                      @click="handleGetCode"
                    >
                      {{ codeButtonText }}
                    </button>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="input-group">
                  <label class="input-label">邮箱</label>
                  <input
                    v-model="formData.email"
                    type="email"
                    class="input-field"
                    placeholder="请输入邮箱"
                  />
                </div>

                <div class="input-group">
                  <label class="input-label">验证码</label>
                  <div class="code-input-group">
                    <input
                      v-model="formData.code"
                      type="text"
                      class="input-field"
                      placeholder="请输入验证码"
                      maxlength="6"
                    />
                    <button
                      type="button"
                      class="code-btn"
                      :disabled="isCodeButtonDisabled"
                      @click="handleGetCode"
                    >
                      {{ codeButtonText }}
                    </button>
                  </div>
                </div>
              </template>
            </template>

            <template v-else-if="currentMode === 'register'">
              <div class="input-group" v-if="currentRegisterMethod === 'phone'">
                <label class="input-label">手机号</label>
                <input
                  v-model="formData.phone"
                  type="tel"
                  class="input-field"
                  placeholder="请输入手机号"
                  maxlength="11"
                />
              </div>

              <div class="input-group" v-else>
                <label class="input-label">邮箱</label>
                <input
                  v-model="formData.email"
                  type="email"
                  class="input-field"
                  placeholder="请输入邮箱"
                />
              </div>

              <div class="input-group">
                <label class="input-label">验证码</label>
                <div class="code-input-group">
                  <input
                    v-model="formData.code"
                    type="text"
                    class="input-field"
                    placeholder="请输入验证码"
                    maxlength="6"
                  />
                  <button
                    type="button"
                    class="code-btn"
                    :disabled="isCodeButtonDisabled"
                    @click="handleGetCode"
                  >
                    {{ codeButtonText }}
                  </button>
                </div>
              </div>

              <div class="input-group">
                <label class="input-label">密码</label>
                <input
                  v-model="formData.password"
                  type="password"
                  class="input-field"
                  placeholder="请设置密码（至少 6 位）"
                />
              </div>

              <div class="input-group">
                <label class="input-label">确认密码</label>
                <input
                  v-model="formData.confirmPassword"
                  type="password"
                  class="input-field"
                  placeholder="请再次输入密码"
                />
              </div>
            </template>

            <template v-else-if="currentMode === 'reset'">
              <div class="input-group">
                <label class="input-label">手机号</label>
                <input
                  v-model="resetForm.phone"
                  type="tel"
                  class="input-field"
                  placeholder="请输入手机号"
                  maxlength="11"
                />
              </div>

              <div class="input-group">
                <label class="input-label">验证码</label>
                <div class="code-input-group">
                  <input
                    v-model="resetForm.code"
                    type="text"
                    class="input-field"
                    placeholder="请输入验证码"
                    maxlength="6"
                  />
                  <button
                    type="button"
                    class="code-btn"
                    :disabled="isCodeButtonDisabled"
                    @click="handleGetCode"
                  >
                    {{ codeButtonText }}
                  </button>
                </div>
              </div>

              <div class="input-group">
                <label class="input-label">新密码</label>
                <input
                  v-model="resetForm.password"
                  type="password"
                  class="input-field"
                  placeholder="请设置新密码（至少 6 位）"
                />
              </div>

              <div class="input-group">
                <label class="input-label">确认新密码</label>
                <input
                  v-model="resetForm.confirmPassword"
                  type="password"
                  class="input-field"
                  placeholder="请再次输入新密码"
                />
              </div>
            </template>

            <div v-if="errorMsg" class="error-message">{{ errorMsg }}</div>
            <div v-if="successMsg" class="success-message">{{ successMsg }}</div>

            <button type="submit" class="login-btn touch-target" :disabled="isLoading">
              <span v-if="isLoading" class="btn-spinner" aria-hidden="true">
                <svg viewBox="0 0 24 24" class="spinner-icon"><circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="3" stroke-dasharray="31.4 31.4" /></svg>
              </span>
              <span>{{ submitButtonLabel }}</span>
            </button>

            <div class="form-links">
              <template v-if="currentMode === 'login'">
                <button type="button" class="link-btn" @click="switchToRegister">
                  还没有账号？立即注册
                </button>
                <button type="button" class="link-btn" @click="switchToReset">
                  忘记密码？
                </button>
              </template>
              <template v-else>
                <button type="button" class="link-btn" @click="switchToLogin">
                  返回登录
                </button>
              </template>
            </div>
          </form>

          <div class="glow-effect glow-cyan" />
          <div class="glow-effect glow-red" />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  position: relative;
  width: min(460px, calc(100vw - 32px));
  padding: 48px 40px;
  border-radius: 24px;
  background: var(--surface-modal, #0B1120);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.5),
    inset 0 1px 1px rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.modal-container--wide {
  width: min(760px, calc(100vw - 32px));
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 20px;
  width: 44px;
  height: 44px;
  min-width: 44px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  color: var(--text-secondary);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.close-btn:hover {
  background: var(--color-danger-bg, rgba(239, 68, 68, 0.2));
  color: var(--color-danger, #EF4444);
}

.modal-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.modal-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.modal-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.login-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 8px;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 4px;
}

.tab-btn {
  padding: 12px 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.tab-btn.active {
  background: var(--color-primary, #06B6D4);
  color: var(--text-inverse, #020617);
  box-shadow: 0 0 20px var(--color-primary-glow, rgba(6, 182, 212, 0.35));
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.input-field {
  padding: 14px 18px;
  border-radius: 12px;
  border: 1px solid var(--border-default, rgba(148, 163, 184, 0.24));
  background: var(--surface-overlay, rgba(30, 41, 59, 0.7));
  color: var(--text-primary);
  font-size: 16px;
  transition: all var(--transition-normal);
}

.input-field::placeholder {
  color: var(--text-muted, rgba(148, 163, 184, 0.6));
}

.input-field:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-muted);
}

.code-input-group {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.code-input-group .input-field {
  flex: 1;
}

.code-btn {
  padding: 14px 20px;
  min-height: 44px;
  border-radius: 12px;
  border: 1px solid var(--color-primary);
  background: transparent;
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
  white-space: nowrap;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.code-btn:hover:not(:disabled) {
  background: var(--color-primary);
  color: var(--text-inverse, #020617);
  box-shadow: 0 0 16px var(--color-primary-glow);
}

.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  border-color: var(--text-muted);
  color: var(--text-muted);
}

.hero-setup-panel {
  display: grid;
  gap: 18px;
}

.hero-setup-head {
  padding: 18px 18px 14px;
  border-radius: 18px;
  background: var(--surface-overlay, rgba(15, 23, 42, 0.88));
  border: 1px solid var(--border-subtle);
}

.hero-setup-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--color-primary-muted);
  color: var(--color-primary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-setup-title {
  margin: 14px 0 8px;
  font-size: 22px;
  color: var(--text-primary);
}

.hero-setup-desc {
  margin: 0;
  line-height: 1.7;
  color: var(--text-secondary);
  font-size: 14px;
}

.error-message {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--color-danger-bg, rgba(239, 68, 68, 0.12));
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--color-danger, #EF4444);
  font-size: 13px;
  text-align: center;
}

.success-message {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--color-primary-muted, rgba(6, 182, 212, 0.15));
  border: 1px solid rgba(6, 182, 212, 0.3);
  color: var(--color-primary, #06B6D4);
  font-size: 13px;
  text-align: center;
}

.login-btn {
  margin-top: 8px;
  padding: 16px;
  min-height: 48px;
  border-radius: 12px;
  border: none;
  background: var(--color-primary, #06B6D4);
  color: var(--text-inverse, #020617);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-normal);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 0 16px var(--color-primary-glow);
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  background: var(--color-primary-hover, #22D3EE);
  box-shadow: 0 4px 24px var(--color-primary-glow);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-links {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 8px;
}

.link-btn {
  background: none;
  border: none;
  color: var(--accent-cyan);
  font-size: 13px;
  cursor: pointer;
  padding: 8px;
  transition: all var(--transition-normal);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.link-btn:hover {
  color: var(--text-primary);
  text-shadow: 0 0 10px var(--accent-cyan);
}

.glow-effect {
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
  pointer-events: none;
}

.glow-cyan {
  top: -80px;
  right: -80px;
  background: var(--accent-cyan);
}

.glow-red {
  bottom: -80px;
  left: -80px;
  background: var(--accent-red);
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.9) translateY(20px);
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .modal-container,
  .modal-container--wide {
    width: calc(100vw - 24px);
    padding: 36px 24px 24px;
    border-radius: 20px;
  }

  .modal-title {
    font-size: 24px;
  }

  .logo-icon {
    font-size: 40px;
  }

  .input-field {
    padding: 12px 16px;
    font-size: 16px; /* 防止iOS缩放 */
  }

  .login-btn {
    padding: 14px;
    font-size: 15px;
  }
}

@media (max-width: 520px) {
  .modal-overlay {
    align-items: flex-end;
  }

  .modal-container,
  .modal-container--wide {
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
    padding: 32px 20px calc(20px + var(--safe-area-bottom));
    border-radius: 20px 20px 0 0;
    transform: none;
  }

  .modal-enter-from .modal-container,
  .modal-leave-to .modal-container {
    transform: translateY(100%);
  }

  .login-tabs {
    grid-template-columns: repeat(3, 1fr);
    gap: 4px;
  }

  .tab-btn {
    padding: 10px 8px;
    font-size: 13px;
  }

  .code-input-group {
    flex-direction: column;
    gap: 8px;
  }

  .code-btn {
    padding: 12px;
  }

  .form-links {
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
}

@media (max-width: 380px) {
  .modal-container {
    padding: 28px 16px calc(16px + var(--safe-area-bottom));
  }

  .modal-title {
    font-size: 22px;
  }

  .login-tabs {
    grid-template-columns: 1fr;
  }
}
</style>
