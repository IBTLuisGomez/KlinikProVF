import { clearSession, getSession } from './auth'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081'

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

/**
 * Cliente HTTP único para todo el frontend: adjunta el JWT en cada request
 * (si hay sesión) y, si el backend responde 401 (token vencido/ inválido),
 * limpia la sesión y manda a /login — el "interceptor" que pide el plan.
 */
async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const session = getSession()
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (session?.token) headers.Authorization = `Bearer ${session.token}`

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401) {
    clearSession()
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    throw new ApiError(401, 'Sesión expirada')
  }

  if (!res.ok) {
    let message = `Error ${res.status}`
    try {
      const data = await res.json()
      message = data.error ?? data.message ?? message
    } catch {
      // el cuerpo no era JSON (o venía vacío) — nos quedamos con el mensaje genérico
    }
    throw new ApiError(res.status, message)
  }

  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  delete: <T>(path: string) => request<T>('DELETE', path),
}
