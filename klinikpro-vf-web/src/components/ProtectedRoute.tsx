import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { Role } from '../lib/auth'

/**
 * Espejo en UI del `@PreAuthorize` del backend: oculta/bloquea rutas según
 * el rol. La seguridad real la sigue haciendo el backend (esto es solo UX —
 * si alguien fuerza la URL sin el rol correcto, el backend igual responde 403).
 */
export function ProtectedRoute({ roles, fallback = '/pacientes' }: { roles?: Role[]; fallback?: string }) {
  const { session } = useAuth()

  if (!session) return <Navigate to="/login" replace />
  // Importante: el fallback NUNCA puede ser "/" cuando esta misma ruta protege "/" (Dashboard),
  // o un rol sin acceso quedaría en un loop de redirección infinito contra sí mismo.
  if (roles && !roles.includes(session.role)) return <Navigate to={fallback} replace />

  return <Outlet />
}
