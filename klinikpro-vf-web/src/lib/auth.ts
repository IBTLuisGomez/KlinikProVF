// Sesión guardada en localStorage tras /api/auth/login o /api/auth/register.
// El backend ya devuelve el rol/nombre/tenant/branch en la respuesta del login,
// así que no hace falta decodificar el JWT en el cliente para eso.

export type Role = 'ADMIN' | 'COORDINADOR' | 'FISIO' | 'RECEPCION'

export interface Session {
  token: string
  role: Role
  name: string
  email: string
  tenantId: string
  branchId: string
}

const STORAGE_KEY = 'klinikpro.session'

export function saveSession(session: Session): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function getSession(): Session | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
