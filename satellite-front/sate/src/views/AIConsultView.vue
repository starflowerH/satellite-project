<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { agentApi, type AgentVO } from '@/api/agent'
import { useUserStore } from '@/store/user'

type MessageRole = 'user' | 'ai'
type NoticeType = 'info' | 'error'

interface ChatMessage {
  id: number
  role: MessageRole
  content: string
  timestamp: string
}

const THINKING_PLACEHOLDER = '正在思考...'

const formatTime = (date = new Date()) =>
  date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })

const router = useRouter()
const userStore = useUserStore()
const { currentUserId } = storeToRefs(userStore)

const draft = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const nextMessageId = ref(2)
const isAgentLoading = ref(false)
const isSending = ref(false)
const agents = ref<AgentVO[]>([])
const selectedAgentId = ref('')
const noticeText = ref('')
const noticeType = ref<NoticeType>('info')

const messages = ref<ChatMessage[]>([
  {
    id: 1,
    role: 'ai',
    content: '欢迎接入峡谷卫星 AI 指挥台。正在为你连接后端战术 Agent，请稍候。',
    timestamp: formatTime()
  }
])

const canSend = computed(() => {
  return draft.value.trim().length > 0 && !isSending.value && !isAgentLoading.value
})

const selectedAgentName = computed(() => {
  return agents.value.find((agent) => agent.agentId === selectedAgentId.value)?.name || '未连接 Agent'
})

const statusText = computed(() => {
  if (isAgentLoading.value) return '连接中'
  if (selectedAgentId.value) return '在线'
  return '待连接'
})

const isThinkingMessage = (message: ChatMessage) => {
  return message.role === 'ai' && message.content === THINKING_PLACEHOLDER
}

const setNotice = (text: string, type: NoticeType = 'error') => {
  noticeText.value = text
  noticeType.value = type
}

const clearNotice = () => {
  noticeText.value = ''
  noticeType.value = 'info'
}

const resolveUserId = (): string | null => {
  const userId = String(currentUserId.value || '').trim()

  if (!userId) {
    return null
  }

  return userId
}

const replaceAiMessage = async (messageId: number, content: string) => {
  const index = messages.value.findIndex((message) => message.id === messageId)

  if (index === -1) {
    messages.value.push({
      id: nextMessageId.value++,
      role: 'ai',
      content,
      timestamp: formatTime()
    })
  } else {
    messages.value[index] = {
      ...messages.value[index],
      content,
      timestamp: formatTime()
    }
  }

  await scrollToBottom()
}

const updateWelcomeMessage = (content: string) => {
  if (!messages.value.length) {
    messages.value.push({
      id: nextMessageId.value++,
      role: 'ai',
      content,
      timestamp: formatTime()
    })
    return
  }

  messages.value[0] = {
    ...messages.value[0],
    content,
    timestamp: formatTime()
  }
}

const scrollToBottom = async (behavior: ScrollBehavior = 'smooth') => {
  await nextTick()

  if (!messageListRef.value) {
    return
  }

  messageListRef.value.scrollTo({
    top: messageListRef.value.scrollHeight,
    behavior
  })
}

const initAgents = async () => {
  clearNotice()
  isAgentLoading.value = true

  try {
    await userStore.bootstrapSession()

    const userId = resolveUserId()
    if (!userId) {
      setNotice('请先登录后再使用 AI 指挥终端。')
      updateWelcomeMessage('未检测到有效登录身份，暂时无法连接战术 Agent。请先登录后再试。')
      if (import.meta.env.DEV) {
        console.debug('[AIConsultView] initAgents.invalidUserId', {
          currentUserId: currentUserId.value,
        })
      }
      return
    }

    const agentList = await agentApi.fetchAvailableAgents(userId)
    if (import.meta.env.DEV) {
      console.debug('[AIConsultView] initAgents.agentList', {
        userId,
        count: agentList.length,
        agentList,
      })
    }
    agents.value = agentList

    if (!agentList.length) {
      setNotice('当前暂无可用 Agent，请稍后刷新页面。')
      updateWelcomeMessage('战术频道暂未部署可用 Agent，请稍后再进入指挥终端。')
      return
    }

    selectedAgentId.value = agentList[0].agentId
    updateWelcomeMessage(`欢迎接入峡谷卫星 AI 指挥台。当前已连接 ${agentList[0].name}，你可以直接开始提问。`)
  } catch (error) {
    console.error('初始化 Agent 列表失败:', error)
    setNotice('Agent 初始化失败，请稍后重试。')
    updateWelcomeMessage('战术频道连接失败，请稍后刷新重试。')
  } finally {
    isAgentLoading.value = false
    await scrollToBottom('auto')
  }
}

const sendMessage = async () => {
  const content = draft.value.trim()

  if (!content) {
    return
  }

  const userId = resolveUserId()
  if (!userId) {
    setNotice('请先登录后再使用 AI 指挥终端。')
    return
  }

  const agentId = selectedAgentId.value.trim()
  if (!agentId) {
    setNotice('当前未连接可用 Agent，请稍后刷新页面。')
    return
  }

  clearNotice()

  messages.value.push({
    id: nextMessageId.value++,
    role: 'user',
    content,
    timestamp: formatTime()
  })

  draft.value = ''
  await scrollToBottom()

  const thinkingId = nextMessageId.value++
  messages.value.push({
    id: thinkingId,
    role: 'ai',
    content: THINKING_PLACEHOLDER,
    timestamp: formatTime()
  })

  isSending.value = true
  await scrollToBottom()

  try {
    const replyText = await agentApi.executeAgentUse({
      agentId,
      userId,
      content,
    })

    if (!replyText) {
      console.error('[AIConsultView] emptyReplyFallbackTriggered', {
        userId,
        agentId,
        contentLength: content.length,
        contentPreview: content.slice(0, 80),
      })
    }

    await replaceAiMessage(
      thinkingId,
      replyText || '当前 Agent 未返回有效内容，请稍后重试。'
    )
  } catch (error) {
    console.error('调用 Agent 失败:', error)
    setNotice('AI 服务调用失败，请稍后重试。')
    await replaceAiMessage(thinkingId, '抱歉，战术频道暂时波动，请稍后重新发送。')
  } finally {
    isSending.value = false
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void sendMessage()
  }
}

const closePanel = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push('/')
}

onMounted(() => {
  void initAgents()
})
</script>

<template>
  <div class="ai-consult-view">
    <section class="consult-shell">
      <header class="consult-header">
        <div>
          <p class="eyebrow">AI Command Link</p>
          <h1>峡谷卫星 指挥终端</h1>
          <p class="subtitle">沉浸式对话面板，用于阵容分析、版本理解与实战咨询。</p>
        </div>

        <div class="header-actions">
          <div class="agent-control">
            <span class="agent-label">策略体</span>
            <select v-model="selectedAgentId" class="agent-select" :disabled="isAgentLoading || !agents.length">
              <option value="" disabled>请选择 Agent</option>
              <option v-for="agent in agents" :key="agent.agentId" :value="agent.agentId">
                {{ agent.name }}
              </option>
            </select>
          </div>

          <div class="status-chip" :class="{ 'status-chip--offline': !selectedAgentId && !isAgentLoading }">
            <span class="status-dot" />
            <span>{{ statusText }}</span>
          </div>

          <button class="close-btn" type="button" aria-label="关闭咨询窗口" @click="closePanel">
            ×
          </button>
        </div>
      </header>

      <div v-if="noticeText" class="notice-banner" :class="`notice-banner--${noticeType}`">
        {{ noticeText }}
      </div>

      <div ref="messageListRef" class="message-list">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="`message-row--${message.role}`"
        >
          <div v-if="message.role === 'ai'" class="ai-avatar" aria-hidden="true">
            <span class="avatar-ring avatar-ring--outer" />
            <span class="avatar-ring avatar-ring--inner" />
            <span class="avatar-core">SAT</span>
          </div>

          <div class="message-content">
            <div class="message-bubble" :class="`message-bubble--${message.role}`">
              <template v-if="isThinkingMessage(message)">
                <span class="thinking-label">{{ selectedAgentName }} 正在思考</span>
                <span class="thinking-dots" aria-hidden="true">
                  <i />
                  <i />
                  <i />
                </span>
              </template>
              <template v-else>
                {{ message.content }}
              </template>
            </div>

            <span class="message-time">{{ message.timestamp }}</span>
          </div>
        </article>
      </div>

      <footer class="input-area">
        <div class="input-shell">
          <textarea
            v-model="draft"
            class="chat-input"
            rows="1"
            maxlength="300"
            :placeholder="selectedAgentId ? '输入你的问题，例如：帮我分析当前阵容后期团战怎么打' : '请先连接可用 Agent 后再发送问题'"
            :disabled="isSending || isAgentLoading || !selectedAgentId"
            @keydown="handleKeydown"
          />

          <button class="send-button" :disabled="!canSend || !selectedAgentId" @click="sendMessage">
            {{ isSending ? '发送中' : '发送' }}
          </button>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.ai-consult-view {
  min-height: calc(100vh - var(--navbar-height) - 80px);
  display: flex;
  justify-content: center;
  align-items: center;
  background: transparent;
}

.consult-shell {
  position: relative;
  width: min(70vw, 1080px);
  height: min(78vh, 860px);
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 28px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  animation: panel-in 0.35s ease-out;
}

.consult-header {
  position: relative;
  z-index: 1;
  padding: 14px 22px 10px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.agent-control {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.agent-label {
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(153, 194, 226, 0.68);
}

.agent-select {
  min-width: 164px;
  height: 36px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-primary);
  outline: none;
}

.agent-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.eyebrow {
  margin: 0 0 4px;
  font-family: 'Orbitron', 'Rajdhani', sans-serif;
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--accent-cyan);
}

.consult-header h1 {
  margin: 0;
  font-family: 'Orbitron', 'Rajdhani', sans-serif;
  font-size: clamp(18px, 1.8vw, 28px);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-primary);
}

.subtitle {
  margin: 3px 0 0;
  max-width: 420px;
  font-size: 12px;
  line-height: 1.45;
  color: var(--text-secondary);
}

.status-chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 11px;
  border-radius: 999px;
  border: 1px solid rgba(34, 197, 94, 0.2);
  background: rgba(34, 197, 94, 0.06);
  color: #16A34A;
  font-size: 12px;
}

.status-chip--offline {
  border-color: rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.06);
  color: #DC2626;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22C55E;
}

.status-chip--offline .status-dot {
  background: #EF4444;
}

.close-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.04);
  color: var(--text-secondary);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.close-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(0, 0, 0, 0.12);
  background: rgba(0, 0, 0, 0.06);
}

.notice-banner {
  position: relative;
  z-index: 1;
  margin: 10px 22px 0;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.5;
}

.notice-banner--info {
  color: #1E40AF;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.notice-banner--error {
  color: #DC2626;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.12);
}

.message-list {
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 18px 22px 14px;
  overflow-y: auto;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.15) transparent;
}

.message-list::-webkit-scrollbar {
  width: 5px;
}

.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 999px;
}

.message-row {
  display: flex;
  margin-bottom: 22px;
  gap: 14px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-row--ai {
  justify-content: flex-start;
}

.ai-avatar {
  position: relative;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-ring,
.avatar-core {
  position: absolute;
  border-radius: 50%;
}

.avatar-ring--outer {
  inset: 0;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.avatar-ring--inner {
  inset: 7px;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.avatar-core {
  inset: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent-cyan), #60A5FA);
  color: #fff;
  font-family: 'Orbitron', 'Rajdhani', sans-serif;
  font-size: 10px;
  letter-spacing: 0.2em;
  text-indent: 0.2em;
}

.message-content {
  max-width: min(70%, 680px);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-row--user .message-content {
  align-items: flex-end;
}

.message-row--ai .message-content {
  align-items: flex-start;
}

.message-bubble {
  padding: 14px 18px;
  line-height: 1.75;
  font-size: 15px;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-bubble--user {
  background: var(--accent-cyan);
  border: none;
  border-radius: 20px 20px 8px 20px;
  color: #fff;
}

.message-bubble--ai {
  padding-left: 0;
  background: transparent;
  border: none;
  color: var(--text-primary);
}

.message-time {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: rgba(138, 162, 186, 0.65);
}

.thinking-label {
  display: inline-block;
  margin-right: 10px;
}

.thinking-dots {
  display: inline-flex;
  gap: 6px;
  vertical-align: middle;
}

.thinking-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-cyan);
  animation: dot-pulse 1.1s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

.input-area {
  position: relative;
  z-index: 1;
  padding: 10px 18px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.6) 30%, rgba(255, 255, 255, 0.9));
}

.input-shell {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 14px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.chat-input {
  flex: 1;
  min-height: 52px;
  max-height: 140px;
  resize: none;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.7;
  font-family: 'Rajdhani', 'Microsoft YaHei', sans-serif;
}

.chat-input::placeholder {
  color: var(--text-secondary);
  opacity: 0.6;
}

.chat-input:disabled {
  cursor: not-allowed;
  opacity: 0.78;
}

.send-button {
  min-width: 108px;
  height: 52px;
  border: none;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.06);
  color: var(--text-secondary);
  font-family: 'Orbitron', 'Rajdhani', sans-serif;
  font-size: 14px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.send-button:not(:disabled) {
  background: linear-gradient(135deg, var(--accent-cyan), #60A5FA);
  color: #fff;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.25);
}

.send-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.3);
}

@keyframes dot-pulse {
  0%,
  80%,
  100% {
    transform: scale(0.7);
    opacity: 0.35;
  }

  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes panel-in {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ========== 响应式适配 ========== */
@media (max-width: 1024px) {
  .ai-consult-view {
    min-height: calc(100vh - var(--navbar-height) - 64px);
    padding: 16px;
  }

  .consult-shell {
    width: 100%;
    height: min(82vh, 820px);
    border-radius: 24px;
  }

  .consult-header,
  .message-list {
    padding-left: 18px;
    padding-right: 18px;
  }

  .input-area {
    padding: 10px 16px 14px;
  }
}

@media (max-width: 768px) {
  .ai-consult-view {
    min-height: calc(100vh - var(--navbar-height));
    padding: 0;
    align-items: stretch;
  }

  .consult-shell {
    width: 100%;
    height: calc(100vh - var(--navbar-height));
    border-radius: 0;
    border: none;
  }

  .consult-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 14px 16px 10px;
  }

  .consult-header h1 {
    font-size: 20px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
  }

  .agent-control {
    width: 100%;
  }

  .agent-select {
    width: 100%;
    min-width: 0;
  }

  .subtitle {
    max-width: none;
    font-size: 12px;
  }

  .message-list {
    padding: 12px 14px 10px;
  }

  .message-row {
    margin-bottom: 16px;
    gap: 10px;
  }

  .ai-avatar {
    width: 36px;
    height: 36px;
  }

  .avatar-core {
    font-size: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  .message-bubble {
    font-size: 14px;
    padding: 12px 14px;
  }

  .input-area {
    padding: 8px 12px calc(12px + var(--safe-area-bottom));
  }

  .input-shell {
    flex-direction: column;
    align-items: stretch;
    padding: 10px;
    gap: 10px;
  }

  .chat-input {
    min-height: 44px;
    font-size: 16px; /* 防止iOS缩放 */
  }

  .send-button {
    width: 100%;
    height: 48px;
    min-width: 0;
  }
}

@media (max-width: 520px) {
  .consult-header {
    padding: 12px 12px 8px;
  }

  .consult-header h1 {
    font-size: 18px;
  }

  .eyebrow {
    font-size: 9px;
  }

  .message-list {
    padding: 10px 12px 8px;
  }

  .message-content {
    max-width: 90%;
  }

  .message-bubble {
    font-size: 13px;
    padding: 10px 12px;
  }

  .input-area {
    padding: 6px 10px calc(10px + var(--safe-area-bottom));
  }
}
</style>
