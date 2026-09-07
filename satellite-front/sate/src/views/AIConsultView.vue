<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
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
const chatInputRef = ref<HTMLTextAreaElement | null>(null)
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
    content: '欢迎接入灵境导览 AI 伴游助手。正在为你连接后端文旅智能体，请稍候。',
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
      updateWelcomeMessage('未检测到有效登录身份，暂时无法连接文旅伴游智能体。请先登录后再试。')
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
      setNotice('当前暂无可用智能体，请稍后刷新页面。')
      updateWelcomeMessage('文旅伴游智能体正在部署中，请稍后再试。')
      return
    }

    selectedAgentId.value = agentList[0].agentId
    updateWelcomeMessage(`欢迎接入灵境导览 AI 伴游助手。当前已连接 ${agentList[0].name}，你可以直接咨询衡阳文旅与特色美食。`)
  } catch (error) {
    console.error('初始化 Agent 列表失败:', error)
    setNotice('智能体初始化失败，请稍后重试。')
    updateWelcomeMessage('智能体服务连接异常，请稍后刷新重试。')
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
    await replaceAiMessage(thinkingId, '抱歉，智能体服务暂时波动，请稍后重新发送。')
  } finally {
    isSending.value = false
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.isComposing || event.keyCode === 229) {
    return
  }

  if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey) {
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

const handleGlobalKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    closePanel()
  }
}

onMounted(async () => {
  window.addEventListener('keydown', handleGlobalKeydown)
  void initAgents()
  await nextTick()
  chatInputRef.value?.focus()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<template>
  <div class="ai-consult-view">
    <section class="consult-shell">
      <!-- 精简 Header：一行布局 -->
      <header class="consult-header">
        <div class="header-left">
          <h1 class="header-title">🛰️ AI 助手</h1>
          <select v-model="selectedAgentId" class="agent-select" :disabled="isAgentLoading || !agents.length">
            <option value="" disabled>选择 Agent</option>
            <option v-for="agent in agents" :key="agent.agentId" :value="agent.agentId">
              {{ agent.name }}
            </option>
          </select>
          <div class="status-chip" :class="{ 'status-chip--offline': !selectedAgentId && !isAgentLoading }">
            <span class="status-dot" />
            <span>{{ statusText }}</span>
          </div>
        </div>
        <button class="close-btn touch-target" type="button" aria-label="关闭" @click="closePanel">×</button>
      </header>

      <div v-if="noticeText" class="notice-banner" :class="`notice-banner--${noticeType}`">
        {{ noticeText }}
      </div>

      <!-- 消息列表：占据主要空间 -->
      <div ref="messageListRef" class="message-list">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="`message-row--${message.role}`"
        >
          <div v-if="message.role === 'ai'" class="ai-avatar" aria-hidden="true">AI</div>

          <div class="message-content">
            <div class="message-bubble" :class="`message-bubble--${message.role}`">
              <template v-if="isThinkingMessage(message)">
                <span class="thinking-dots" aria-hidden="true">
                  <i /><i /><i />
                </span>
                <span class="thinking-label">{{ selectedAgentName }} 思考中...</span>
              </template>
              <template v-else>
                {{ message.content }}
              </template>
            </div>
            <span class="message-time">{{ message.timestamp }}</span>
          </div>
        </article>
      </div>

      <!-- 紧凑输入区 -->
      <footer class="input-area">
        <textarea
          ref="chatInputRef"
          v-model="draft"
          class="chat-input"
          rows="1"
          maxlength="300"
          :placeholder="selectedAgentId ? '输入问题...' : '请先选择 Agent'"
          :disabled="isSending || isAgentLoading || !selectedAgentId"
          @keydown="handleKeydown"
        />
        <button class="send-button touch-target" :disabled="!canSend || !selectedAgentId" @click="sendMessage">
          <span v-if="isSending" class="btn-spinner" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="spinner-icon"><circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="3" stroke-dasharray="31.4 31.4" /></svg>
          </span>
          <span>发送</span>
        </button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
/* ========== 整体布局：撑满可用空间 ========== */
.ai-consult-view {
  height: calc(100vh - var(--navbar-height));
  background: transparent;
  padding: 12px 16px;
  padding-bottom: calc(12px + var(--safe-area-bottom));
}

.consult-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 900px;
  margin: 0 auto;
  background: var(--surface-card-glass, rgba(15, 23, 42, 0.85));
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

/* ========== 紧凑 Header ========== */
.consult-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--surface-card, rgba(15, 23, 42, 0.9));
  gap: 12px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}

.agent-select {
  height: 44px;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid var(--border-default);
  background: var(--surface-overlay, rgba(30, 41, 59, 0.85));
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  max-width: 160px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.agent-select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-muted);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(16, 185, 129, 0.3);
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success, #10B981);
  font-size: 12px;
  white-space: nowrap;
}

.status-chip--offline {
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger, #EF4444);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success, #10B981);
}

.status-chip--offline .status-dot {
  background: var(--color-danger, #EF4444);
}

.close-btn {
  width: 44px;
  height: 44px;
  min-width: 44px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-secondary);
  font-size: 20px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--color-danger-bg, rgba(239, 68, 68, 0.15));
  border-color: var(--color-danger, #EF4444);
  color: var(--color-danger, #EF4444);
}

/* ========== 通知条 ========== */
.notice-banner {
  margin: 0;
  padding: 8px 16px;
  font-size: 13px;
  flex-shrink: 0;
}

.notice-banner--info {
  color: #1E40AF;
  background: rgba(59, 130, 246, 0.08);
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.notice-banner--error {
  color: #DC2626;
  background: rgba(239, 68, 68, 0.06);
  border-bottom: 1px solid rgba(239, 68, 68, 0.1);
}

/* ========== 消息列表：占据主要空间 ========== */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.12) transparent;
}

.message-list::-webkit-scrollbar {
  width: 4px;
}

.message-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 999px;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
  gap: 10px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-row--ai {
  justify-content: flex-start;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-cyan), #60A5FA);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}

.message-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-row--user .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 10px 14px;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-bubble--user {
  background: var(--color-primary);
  border-radius: 16px 16px 4px 16px;
  color: var(--text-inverse, #020617);
  box-shadow: 0 2px 12px var(--color-primary-glow);
}

.message-bubble--ai {
  background: var(--surface-overlay, rgba(30, 41, 59, 0.85));
  border: 1px solid var(--border-subtle);
  border-radius: 16px 16px 16px 4px;
  color: var(--text-primary);
}

.message-time {
  font-size: 11px;
  color: var(--text-secondary);
  opacity: 0.7;
  padding: 0 4px;
}

.thinking-label {
  margin-left: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.thinking-dots {
  display: inline-flex;
  gap: 4px;
}

.thinking-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: dot-pulse 1.1s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) { animation-delay: 0.15s; }
.thinking-dots i:nth-child(3) { animation-delay: 0.3s; }

/* ========== 紧凑输入区 ========== */
.input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-subtle);
  background: var(--surface-card, rgba(15, 23, 42, 0.95));
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  min-height: 44px;
  max-height: 120px;
  padding: 10px 14px;
  border: 1px solid var(--border-default);
  border-radius: 12px;
  background: var(--surface-ground, rgba(7, 11, 20, 0.85));
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  outline: none;
  font-family: inherit;
  transition: all var(--transition-fast);
}

.chat-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-muted);
}

.chat-input::placeholder {
  color: var(--text-muted);
  opacity: 0.7;
}

.send-button {
  height: 44px;
  min-height: 44px;
  min-width: 80px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: var(--color-primary);
  color: var(--text-inverse, #020617);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 0 16px var(--color-primary-glow);
  transition: all var(--transition-fast);
}

.send-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}

.send-button:not(:disabled):hover {
  background: var(--color-primary-hover);
  box-shadow: 0 4px 20px var(--color-primary-glow);
}

@keyframes dot-pulse {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.35; }
  40% { transform: scale(1); opacity: 1; }
}

/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .ai-consult-view {
    padding: 0;
  }

  .consult-shell {
    border-radius: 0;
    border: none;
    max-width: none;
  }

  .consult-header {
    padding: 8px 12px;
  }

  .header-title {
    font-size: 14px;
  }

  .agent-select {
    max-width: 120px;
    height: 44px;
    min-height: 44px;
    font-size: 13px;
  }

  .status-chip {
    padding: 3px 8px;
    font-size: 11px;
  }

  .message-list {
    padding: 12px;
  }

  .message-content {
    max-width: 85%;
  }

  .message-bubble {
    font-size: 13px;
    padding: 8px 12px;
  }

  .input-area {
    padding: 8px 12px calc(8px + var(--safe-area-bottom));
  }

  .chat-input {
    min-height: 44px;
    font-size: 16px; /* 防止iOS缩放 */
  }

  .send-button {
    height: 44px;
    min-height: 44px;
  }
}
</style>
