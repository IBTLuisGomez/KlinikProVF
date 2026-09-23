import { createContext, useContext, useState, type ReactNode } from 'react'
import { api } from '../lib/api'
import { clearSession, getSession, saveSession, type Session } from '../lib/auth'

interface AuthContextValue {
  session: Session | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => getSession())

  async function login(email: string, password: string) {
    const data = await api.post<Session>('/api/auth/login', { email, password })
    saveSession(data)
    setSession(data)
  }

  function logout() {
    clearSession()
    setSession(null)
  }

  return <AuthContext.Provider value={{ session, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
