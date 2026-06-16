import request from '@/utils/request'
import type { Result } from '@/api/auth'

export interface AgentVO {
  agentId: string
  name: string
  description?: string
}

export interface UseAgentDTO {
  agentId: string
  userId: string
  content: string
}

export interface AgentUserResponseVO {
  content?: string
  reply?: string
  response?: string
  answer?: string
  message?: string
}

const isDev = import.meta.env.DEV

const debugAgent = (label: string, payload?: unknown) => {
  if (!isDev) return
  if (payload === undefined) {
    console.debug(`[agentApi] ${label}`)
    return
  }
  console.debug(`[agentApi] ${label}`, payload)
}

const unwrapData = <T>(response: unknown): T | null => {
  if (response === null || response === undefined) return null

  if (typeof response === 'object' && 'data' in (response as Record<string, unknown>)) {
    const data = (response as Result<T>).data
    if (data && typeof data === 'object' && 'data' in (data as Record<string, unknown>)) {
      return ((data as Record<string, unknown>).data as T) ?? null
    }
    return data ?? null
  }

  return response as T
}

const toSafeIdString = (value: unknown, label: string): string => {
  if (typeof value === 'string') {
    return value.trim()
  }

  if (typeof value === 'number') {
    if (!Number.isSafeInteger(value)) {
      console.warn(`[agentApi] ${label} 使用了不安全整数，建议后端将 Long/超长 ID 序列化为字符串返回`, value)
    }
    return Number.isFinite(value) ? String(value) : ''
  }

  if (typeof value === 'bigint') {
    return value.toString()
  }

  return ''
}

const normalizeAgent = (raw: unknown, index: number): AgentVO => {
  const source = (raw ?? {}) as Record<string, unknown>
  return {
    agentId: toSafeIdString(
      source.agentId ?? source.agent_id ?? source.id ?? source.appId ?? source.app_id ?? index + 1,
      'agentId',
    ),
    name: String(source.name ?? source.agentName ?? source.title ?? `Agent ${index + 1}`),
    description: source.description ? String(source.description) : undefined,
  }
}

export const fetchAvailableAgents = async (userId: string): Promise<AgentVO[]> => {
  debugAgent('fetchAvailableAgents.request', { userId })

  const response = await request.get('/agent/list', {
    params: { userId },
  })
  debugAgent('fetchAvailableAgents.response.raw', response)

  const data = unwrapData<unknown[]>(response) ?? []
  debugAgent('fetchAvailableAgents.response.unwrapped', data)

  if (!Array.isArray(data)) {
    debugAgent('fetchAvailableAgents.response.nonArray', data)
    return []
  }

  const normalized = data.map(normalizeAgent).filter((agent) => !!agent.agentId)
  debugAgent('fetchAvailableAgents.response.normalized', normalized)

  return normalized
}


export const extractAgentReplyText = (payload: AgentUserResponseVO | string | null | undefined): string => {
  if (!payload) return ''
  if (typeof payload === 'string') return payload.trim()

  const content = payload.content ?? payload.reply ?? payload.response ?? payload.answer ?? payload.message ?? ''
  return String(content).trim()
}

const logEmptyReplyDiagnostic = (params: {
  requestData: UseAgentDTO
  rawResponse: unknown
  unwrappedPayload: AgentUserResponseVO | string | null
}) => {
  const { requestData, rawResponse, unwrappedPayload } = params
  const candidateFields =
    unwrappedPayload && typeof unwrappedPayload === 'object'
      ? {
          content: (unwrappedPayload as AgentUserResponseVO).content,
          reply: (unwrappedPayload as AgentUserResponseVO).reply,
          response: (unwrappedPayload as AgentUserResponseVO).response,
          answer: (unwrappedPayload as AgentUserResponseVO).answer,
          message: (unwrappedPayload as AgentUserResponseVO).message,
        }
      : null

  console.error('[agentApi] executeAgentUse.emptyReply: Agent未返回有效文本', {
    requestData,
    rawResponse,
    unwrappedPayload,
    candidateFields,
    hint: '请检查后端返回是否包含 data.content / data.reply / data.response / data.answer / data.message',
  })
}

export const executeAgentUse = async (data: UseAgentDTO): Promise<string> => {
  debugAgent('executeAgentUse.request', data)

  const response = await request.post('/agent/use', data)
  debugAgent('executeAgentUse.response.raw', response)

  const payload = unwrapData<AgentUserResponseVO>(response)
  debugAgent('executeAgentUse.response.unwrapped', payload)

  const reply = extractAgentReplyText(payload)
  debugAgent('executeAgentUse.response.reply', reply)

  if (!reply) {
    logEmptyReplyDiagnostic({
      requestData: data,
      rawResponse: response,
      unwrappedPayload: payload,
    })
  }

  return reply
}

export const agentApi = {
  fetchAvailableAgents,
  executeAgentUse,
}

export default agentApi
