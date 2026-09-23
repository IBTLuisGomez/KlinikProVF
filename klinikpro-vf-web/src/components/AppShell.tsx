import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { Role } from '../lib/auth'

interface NavItem {
  to: string
  label: string
  icon: string
  /** Si se omite, visible para cualquier rol autenticado. */
  roles?: Role[]
}

const NAV_CLINICA: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: 'grid_view', roles: ['ADMIN', 'COORDINADOR'] },
  { to: '/pacientes', label: 'Pacientes e Historias', icon: 'patient_list' },
  { to: '/agenda', label: 'Agenda de Turnos', icon: 'calendar_today' },
  { to: '/caja', label: 'Caja', icon: 'point_of_sale' },
  { to: '/finanzas', label: 'Finanzas', icon: 'account_balance' },
  { to: '/facturacion', label: 'Facturación y Recetas', icon: 'receipt_long' },
]

const NAV_ADMIN: NavItem[] = [
  { to: '/configuracion', label: 'Configuración', icon: 'settings' },
  { to: '/ayuda', label: 'Ayuda', icon: 'help' },
]

function NavGroup({
  title,
  items,
  role,
  onNavigate,
}: {
  title: string
  items: NavItem[]
  role: Role
  onNavigate: () => void
}) {
  const visible = items.filter((item) => !item.roles || item.roles.includes(role))
  if (visible.length === 0) return null

  return (
    <div className="mb-space-md">
      <div className="px-space-md mb-space-xs">
        <span className="px-space-sm font-label-sm text-label-sm text-on-surface-variant uppercase font-semibold tracking-wider">
          {title}
        </span>
      </div>
      <nav className="flex flex-col gap-space-xs px-space-md">
        {visible.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex items-center gap-space-md px-space-md py-space-sm rounded-lg transition-colors ${
                isActive
                  ? 'bg-primary text-on-primary shadow-sm font-medium'
                  : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
              }`
            }
          >
            <span className="material-symbols-outlined text-[20px]">{item.icon}</span>
            <span className="font-label-lg text-label-lg">{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

export function AppShell() {
  const { session, logout } = useAuth()
  const [navOpen, setNavOpen] = useState(false)

  if (!session) return null

  const closeNav = () => setNavOpen(false)

  return (
    <div className="min-h-screen bg-surface">
      {/* Fondo oscuro detrás del drawer en móvil */}
      {navOpen && (
        <div
          className="fixed inset-0 bg-black/40 z-40 lg:hidden"
          onClick={closeNav}
          aria-hidden="true"
        />
      )}

      <aside
        className={`fixed left-0 top-0 h-full w-72 bg-surface-container-lowest shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-50 flex flex-col justify-between py-space-md transition-transform duration-200 ease-out lg:translate-x-0 ${
          navOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex flex-col overflow-y-auto">
          <div className="px-space-lg py-space-sm mb-space-md flex items-center justify-between">
            <span className="font-headline-md text-headline-md text-primary font-bold">KlinikPro</span>
            <button
              onClick={closeNav}
              className="lg:hidden p-1.5 rounded-lg text-on-surface-variant hover:bg-surface-container"
              aria-label="Cerrar menú"
            >
              <span className="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>
          <NavGroup title="Gestión Clínica" items={NAV_CLINICA} role={session.role} onNavigate={closeNav} />
          <NavGroup title="Administración" items={NAV_ADMIN} role={session.role} onNavigate={closeNav} />
        </div>
        <div className="px-space-md pt-space-sm">
          <div className="bg-surface-container-low rounded-lg p-space-sm flex items-center justify-between">
            <div className="flex flex-col overflow-hidden">
              <span className="font-label-md text-label-md text-on-surface font-semibold truncate">
                {session.name}
              </span>
              <span className="font-label-sm text-label-sm text-on-surface-variant">{session.role}</span>
            </div>
            <button
              onClick={logout}
              title="Cerrar sesión"
              className="p-1.5 rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-error transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">logout</span>
            </button>
          </div>
        </div>
      </aside>

      <div className="lg:pl-72">
        <header className="fixed top-0 left-0 right-0 lg:left-72 h-16 bg-surface/90 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-30 flex items-center justify-between px-space-lg">
          <button
            onClick={() => setNavOpen(true)}
            className="lg:hidden p-1.5 rounded-lg text-on-surface-variant hover:bg-surface-container"
            aria-label="Abrir menú"
          >
            <span className="material-symbols-outlined text-[24px]">menu</span>
          </button>
          <div className="flex-1" />
          <div className="flex items-center gap-space-sm">
            <div className="w-8 h-8 rounded-full bg-primary text-on-primary flex items-center justify-center font-label-md text-label-md font-bold">
              {session.name.slice(0, 1).toUpperCase()}
            </div>
          </div>
        </header>
        <main className="pt-16 min-h-screen px-space-md py-space-md lg:px-space-lg lg:py-space-lg">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
