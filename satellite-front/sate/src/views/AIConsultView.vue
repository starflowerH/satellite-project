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
        <button class="close-btn" type="button" aria-label="关闭" @click="closePanel">×</button>
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
          v-model="draft"
          class="chat-input"
          rows="1"
          maxlength="300"
          :placeholder="selectedAgentId ? '输入问题...' : '请先选择 Agent'"
          :disabled="isSending || isAgentLoading || !selectedAgentId"
          @keydown="handleKeydown"
        />
        <button class="send-button" :disabled="!canSend || !selectedAgentId" @click="sendMessage">
          {{ isSending ? '...' : '发送' }}
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
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* ========== 紧凑 Header ========== */
.consult-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
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
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  max-width: 140px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(34, 197, 94, 0.2);
  background: rgba(34, 197, 94, 0.06);
  color: #16A34A;
  font-size: 12px;
  white-space: nowrap;
}

.status-chip--offline {
  border-color: rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.06);
  color: #DC2626;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22C55E;
}

.status-chip--offline .status-dot {
  background: #EF4444;
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;
  flex-shrink: 0;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.08);
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
  background: var(--accent-cyan);
  border-radius: 16px 16px 4px 16px;
  color: #fff;
}

.message-bubble--ai {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 16px 16px 16px 4px;
  color: var(--text-primary);
}

.message-time {
  font-size: 11px;
  color: var(--text-secondary);
  opacity: 0.6;
  padding: 0 4px;
}

.thinking-label {
  margin-left: 8px;
  font-size: 13px;
}

.thinking-dots {
  display: inline-flex;
  gap: 4px;
}

.thinking-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent-cyan);
  animation: dot-pulse 1.1s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) { animation-delay: 0.15s; }
.thinking-dots i:nth-child(3) { animation-delay: 0.3s; }

/* ========== 紧凑输入区 ========== */
.input-area {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.95);
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  min-height: 38px;
  max-height: 100px;
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  font-family: inherit;
}

.chat-input:focus {
  border-color: var(--accent-cyan);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.chat-input::placeholder {
  color: var(--text-secondary);
  opacity: 0.5;
}

.send-button {
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--accent-cyan), #60A5FA);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-button:not(:disabled):hover {
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
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
    max-width: 100px;
    font-size: 12px;
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
    font-size: 16px; /* 防止iOS缩放 */
  }
}
</style>
