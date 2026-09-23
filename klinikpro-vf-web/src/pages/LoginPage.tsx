import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../lib/api'

/**
 * Adaptado del mockup de Stitch (klinikpro_inicio_de_sesi_n_cl_nico) manteniendo
 * el diseño (panel izquierdo de marca + formulario a la derecha, mismos tokens
 * de color/tipografía). Se quitaron del mockup original: el selector de "rol
 * clínico" (el rol lo decide el backend según la cuenta, no se elige en el
 * login), las opciones de 2FA/Smart Card/biometría y las cifras/certificaciones
 * (HIPAA, ISO 27001, "142 camas UCI", 99.98% disponibilidad) — nada de eso
 * existe todavía en el backend ni es cierto sobre el producto, así que no
 * debía quedar en la pantalla real.
 */
export function LoginPage() {
  const { session, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (session) return <Navigate to="/" replace />

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo iniciar sesión')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="w-full min-h-screen flex items-center justify-center p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-12 min-h-[640px] rounded-xl overflow-hidden shadow-xl bg-surface-container-lowest">
        {/* Columna izquierda: marca */}
        <div className="lg:col-span-5 relative flex flex-col justify-between p-8 sm:p-10 lg:p-12 text-on-primary bg-gradient-to-br from-primary-container via-primary to-on-primary-fixed overflow-hidden">
          <div className="absolute -right-24 -top-24 w-96 h-96 rounded-full bg-primary-fixed-dim/10 blur-3xl pointer-events-none" />
          <div className="absolute -left-20 bottom-1/4 w-80 h-80 rounded-full bg-secondary-fixed/10 blur-2xl pointer-events-none" />

          <div className="relative z-10 space-y-6">
            <span className="font-headline-lg text-headline-lg font-bold">KlinikPro</span>
            <div>
              <h1 className="font-headline-xl text-headline-xl text-surface-container-lowest font-bold leading-tight">
                Gestión clínica y de fisioterapia
              </h1>
              <p className="mt-3 font-body-md text-body-md text-surface-container-highest/90 leading-relaxed">
                Agenda, caja, finanzas y expediente del paciente en un solo lugar para tu clínica.
              </p>
            </div>
          </div>

          <div className="relative z-10 pt-8 mt-6 text-surface-variant/70 font-label-sm text-label-sm">
            Cytohelix Systems
          </div>
        </div>

        {/* Columna derecha: formulario */}
        <div className="lg:col-span-7 p-6 sm:p-10 lg:p-12 flex flex-col justify-center bg-surface-container-lowest">
          <div className="mb-6">
            <h2 className="font-headline-lg text-headline-lg font-bold text-on-surface">Bienvenido de nuevo</h2>
            <p className="font-body-md text-body-md text-on-surface-variant mt-1">
              Ingresa tus credenciales para acceder a tu clínica.
            </p>
          </div>

          <form className="space-y-5" onSubmit={handleSubmit}>
            {error ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-2.5 font-body-sm text-body-sm">
                {error}
              </div>
            ) : null}

            <div>
              <label className="block font-label-md text-label-md font-semibold text-on-surface-variant mb-1.5" htmlFor="email">
                Correo electrónico
              </label>
              <div className="relative flex items-center">
                <div className="absolute left-3.5 flex items-center pointer-events-none text-outline">
                  <span className="material-symbols-outlined text-body-lg">badge</span>
                </div>
                <input
                  id="email"
                  type="email"
                  required
                  autoComplete="username"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="tu.correo@clinica.com"
                  className="w-full pl-11 pr-4 py-2.5 rounded-lg bg-surface-container-low text-on-surface placeholder:text-outline font-body-md text-body-md focus:bg-surface-container-lowest focus:outline-none focus:shadow-md transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block font-label-md text-label-md font-semibold text-on-surface-variant mb-1.5" htmlFor="password">
                Contraseña
              </label>
              <div className="relative flex items-center">
                <div className="absolute left-3.5 flex items-center pointer-events-none text-outline">
                  <span className="material-symbols-outlined text-body-lg">key</span>
                </div>
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-11 pr-11 py-2.5 rounded-lg bg-surface-container-low text-on-surface placeholder:text-outline font-body-md text-body-md focus:bg-surface-container-lowest focus:outline-none focus:shadow-md transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  title="Mostrar/ocultar contraseña"
                  className="absolute right-3 p-1 rounded text-outline hover:text-on-surface focus:outline-none"
                >
                  <span className="material-symbols-outlined text-body-lg">
                    {showPassword ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 rounded-lg bg-primary hover:bg-on-primary-fixed-variant text-on-primary font-label-lg text-label-lg flex items-center justify-center space-x-2 shadow-md hover:shadow-lg transition-all disabled:opacity-60"
            >
              <span className="material-symbols-outlined text-body-lg">login</span>
              <span>{loading ? 'Ingresando…' : 'Iniciar sesión'}</span>
            </button>
          </form>
        </div>
      </div>
    </main>
  )
}
