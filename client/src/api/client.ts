import { ref } from 'vue'

const TOKEN_KEY = 'agora.access-token'
const USERNAME_KEY = 'agora.username'

export type ApiEnvelope<T> = { data: T }
export type LoginResponse = { accessToken: string; tokenType: string; expireAt: string }
export type ReplyView = { id: number; floorNo: number; userId: number; replyToFloor: number | null; content: string; status: string; version: number }
export type ReplyPage = { topicId: number; afterFloor: number; nextCursor: number | null; hasNext: boolean; onlyAuthor: boolean; replies: ReplyView[] }

export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly code?: string) { super(message); this.name = 'ApiError' }
}

function readStorage(key: string) { try { return localStorage.getItem(key) } catch { return null } }
export const sessionUsername = ref(readStorage(USERNAME_KEY))
export function getToken() { return readStorage(TOKEN_KEY) }
export function getUsername() { return sessionUsername.value }
export function clearSession() {
  try { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USERNAME_KEY) } catch { /* Session state still clears in memory. */ }
  sessionUsername.value = null
}

export async function request<T>(path: string, options: { method?: string; body?: unknown; auth?: boolean } = {}): Promise<T> {
  const headers = new Headers({ Accept: 'application/json' })
  if (options.body !== undefined) headers.set('Content-Type', 'application/json')
  if (options.auth) {
    const token = getToken()
    if (token) headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(path, { method: options.method || 'GET', headers, body: options.body === undefined ? undefined : JSON.stringify(options.body) })
  } catch {
    throw new ApiError('无法连接服务端，请确认后端已启动。', 0)
  }

  const payload = await response.json().catch(() => null) as (ApiEnvelope<T> & { detail?: string; code?: string }) | null
  if (!response.ok) throw new ApiError(payload?.detail || `请求失败（${response.status}）`, response.status, payload?.code)
  if (!payload || !('data' in payload)) throw new ApiError('服务端返回格式不符合预期。', response.status)
  return payload.data
}

export async function login(username: string, password: string) {
  const result = await request<LoginResponse>('/api/identity/login', { method: 'POST', body: { username, password } })
  try { localStorage.setItem(TOKEN_KEY, result.accessToken); localStorage.setItem(USERNAME_KEY, username) }
  catch { throw new ApiError('浏览器无法保存登录状态，请检查本地存储设置。', 0) }
  sessionUsername.value = username
  return result
}

export async function register(username: string, password: string) {
  return request<{ username: string }>('/api/identity/register', { method: 'POST', body: { username, password } })
}

export async function logout() {
  try { await request<null>('/api/identity/logout', { method: 'POST', auth: true }) }
  finally { clearSession() }
}

export function formatApiError(error: unknown) {
  if (!(error instanceof ApiError)) return '请求失败，请稍后重试。'
  if (error.status === 429) return '今日发帖或回复额度已用完，请稍后再试。'
  if (error.status === 401) return '登录状态已失效，请重新登录。'
  if (error.status === 400) return error.message || '提交内容不符合要求。'
  return error.message
}
