import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'

interface Specialty {
  id: string
  name: string
  description?: string | null
}
interface ServiceItem {
  id: string
  name: string
  minutes: number
  price: number
  sessions: number
  countsAsSession: boolean
  bufferMinutes: number
  specialtyId?: string | null
}
interface Room {
  id: string
  name: string
  location?: string | null
}
interface Specialist {
  id: string
  name: string
  documentId?: string | null
  email?: string | null
  phone?: string | null
  calendarColor?: string | null
  specialties: { id: string; name: string }[]
  services: { id: string; name: string }[]
}
interface Branch {
  id: string
  name: string
  clinicName?: string | null
  active: boolean
  defaultCashier?: string | null
  defaultSupervisor?: string | null
}

type Tab = 'sucursales' | 'especialidades' | 'servicios' | 'especialistas' | 'consultorios'

export function ConfiguracionPage() {
  const { session } = useAuth()
  const canLead = session ? ['ADMIN', 'COORDINADOR'].includes(session.role) : false
  const isAdmin = session?.role === 'ADMIN'
  const [tab, setTab] = useState<Tab>('sucursales')
  const [error, setError] = useState<string | null>(null)

  const TABS: { key: Tab; label: string }[] = [
    { key: 'sucursales', label: 'Sucursales' },
    { key: 'especialidades', label: 'Especialidades' },
    { key: 'servicios', label: 'Servicios' },
    { key: 'especialistas', label: 'Especialistas' },
    { key: 'consultorios', label: 'Consultorios' },
  ]

  return (
    <div className="flex flex-col gap-space-lg">
      <div className="bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
        <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest">
          Administración
        </span>
        <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold tracking-tight mt-1">
          Configuración y Catálogos
        </h1>
      </div>

      {error ? (
        <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm">
          {error}
        </div>
      ) : null}

      <div className="flex gap-space-xs bg-surface-container-lowest rounded-xl shadow-sm p-1 w-fit flex-wrap">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-space-md py-2 rounded-lg font-label-md text-label-md font-medium transition-colors ${
              tab === t.key ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-surface-container-low'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'sucursales' ? (
        <SucursalesTab canEdit={canLead} isAdmin={isAdmin} onError={setError} />
      ) : tab === 'especialidades' ? (
        <EspecialidadesTab canEdit={canLead} onError={setError} />
      ) : tab === 'servicios' ? (
        <ServiciosTab canEdit={canLead} onError={setError} />
      ) : tab === 'especialistas' ? (
        <EspecialistasTab canEdit={canLead} onError={setError} />
      ) : (
        <ConsultoriosTab canEdit={canLead} onError={setError} />
      )}
    </div>
  )
}

function Table<T extends { id: string }>({
  columns,
  rows,
  renderActions,
  empty,
}: {
  columns: { key: string; label: string; render: (row: T) => React.ReactNode }[]
  rows: T[]
  renderActions?: (row: T) => React.ReactNode
  empty: string
}) {
  return (
    <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="border-b border-outline-variant/40">
            {columns.map((c) => (
              <th key={c.key} className="px-space-md py-space-sm font-label-sm text-label-sm text-on-surface-variant">
                {c.label}
              </th>
            ))}
            {renderActions ? <th /> : null}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} className="border-b border-outline-variant/20 last:border-0">
              {columns.map((c) => (
                <td key={c.key} className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">
                  {c.render(row)}
                </td>
              ))}
              {renderActions ? <td className="px-space-md py-space-sm">{renderActions(row)}</td> : null}
            </tr>
          ))}
        </tbody>
      </table>
      {rows.length === 0 ? (
        <p className="font-body-sm text-body-sm text-on-surface-variant px-space-md py-space-lg">{empty}</p>
      ) : null}
    </div>
  )
}

function SectionHeader({ canEdit, onCreate }: { canEdit: boolean; onCreate: () => void }) {
  if (!canEdit) return null
  return (
    <div className="flex justify-end">
      <button
        onClick={onCreate}
        className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
      >
        <span className="material-symbols-outlined text-[18px]">add</span>
        Nuevo
      </button>
    </div>
  )
}

function Modal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
      <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-md max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-space-md">
          <h3 className="font-headline-md text-headline-md text-on-surface font-bold">{title}</h3>
          <button onClick={onClose} className="text-on-surface-variant">
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

// ============================== Sucursales ==============================

function SucursalesTab({
  canEdit,
  isAdmin,
  onError,
}: {
  canEdit: boolean
  isAdmin: boolean
  onError: (m: string | null) => void
}) {
  const [rows, setRows] = useState<Branch[]>([])
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Branch | null>(null)
  const [form, setForm] = useState({ name: '', clinicName: '', defaultCashier: '', defaultSupervisor: '' })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    try {
      setRows(await api.get<Branch[]>('/api/sucursales'))
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar las sucursales')
    }
  }
  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setEditing(null)
    setForm({ name: '', clinicName: '', defaultCashier: '', defaultSupervisor: '' })
    setFormError(null)
    setFormOpen(true)
  }
  function openEdit(b: Branch) {
    setEditing(b)
    setForm({
      name: b.name,
      clinicName: b.clinicName ?? '',
      defaultCashier: b.defaultCashier ?? '',
      defaultSupervisor: b.defaultSupervisor ?? '',
    })
    setFormError(null)
    setFormOpen(true)
  }

  async function submit() {
    if (!form.name) {
      setFormError('El nombre es obligatorio')
      return
    }
    setSaving(true)
    setFormError(null)
    const body = {
      name: form.name,
      clinicName: form.clinicName || null,
      schedule: null,
      logo: null,
      defaultCashier: form.defaultCashier || null,
      defaultSupervisor: form.defaultSupervisor || null,
    }
    try {
      if (editing) await api.put(`/api/sucursales/${editing.id}`, body)
      else await api.post('/api/sucursales', body)
      setFormOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar la sucursal')
    } finally {
      setSaving(false)
    }
  }

  async function toggle(b: Branch) {
    try {
      await api.post(`/api/sucursales/${b.id}/${b.active ? 'desactivar' : 'activar'}`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo cambiar el estado de la sucursal')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <SectionHeader canEdit={isAdmin} onCreate={openCreate} />
      <Table<Branch>
        rows={rows}
        empty="Sin sucursales registradas."
        columns={[
          { key: 'name', label: 'Nombre', render: (b) => b.name },
          { key: 'clinicName', label: 'Clínica', render: (b) => b.clinicName || '—' },
          { key: 'cashier', label: 'Cajero por defecto', render: (b) => b.defaultCashier || '—' },
          {
            key: 'active',
            label: 'Estado',
            render: (b) => (
              <span
                className={`px-space-sm py-1 rounded-full font-label-sm text-label-sm ${
                  b.active ? 'bg-secondary-fixed text-on-secondary-fixed-variant' : 'bg-error-container text-on-error-container'
                }`}
              >
                {b.active ? 'Activa' : 'Inactiva'}
              </span>
            ),
          },
        ]}
        renderActions={
          canEdit
            ? (b) => (
                <div className="flex items-center gap-space-sm">
                  <button onClick={() => openEdit(b)} className="font-label-sm text-label-sm text-primary">
                    Editar
                  </button>
                  {isAdmin ? (
                    <button
                      onClick={() => toggle(b)}
                      className={`font-label-sm text-label-sm ${b.active ? 'text-error' : 'text-primary'}`}
                    >
                      {b.active ? 'Desactivar' : 'Activar'}
                    </button>
                  ) : null}
                </div>
              )
            : undefined
        }
      />

      {formOpen ? (
        <Modal title={editing ? 'Editar sucursal' : 'Nueva sucursal'} onClose={() => setFormOpen(false)}>
          {formError ? (
            <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
              {formError}
            </div>
          ) : null}
          <div className="flex flex-col gap-space-sm">
            <input
              placeholder="Nombre *"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="input"
            />
            <input
              placeholder="Nombre de la clínica"
              value={form.clinicName}
              onChange={(e) => setForm({ ...form, clinicName: e.target.value })}
              className="input"
            />
            <input
              placeholder="Cajero por defecto"
              value={form.defaultCashier}
              onChange={(e) => setForm({ ...form, defaultCashier: e.target.value })}
              className="input"
            />
            <input
              placeholder="Supervisor por defecto"
              value={form.defaultSupervisor}
              onChange={(e) => setForm({ ...form, defaultSupervisor: e.target.value })}
              className="input"
            />
            <div className="flex justify-end gap-space-sm mt-space-sm">
              <button
                onClick={() => setFormOpen(false)}
                className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={submit}
                disabled={saving}
                className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
              >
                {saving ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </div>
        </Modal>
      ) : null}
    </div>
  )
}

// ============================== Especialidades ==============================

function EspecialidadesTab({ canEdit, onError }: { canEdit: boolean; onError: (m: string | null) => void }) {
  const [rows, setRows] = useState<Specialty[]>([])
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Specialty | null>(null)
  const [form, setForm] = useState({ name: '', description: '' })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    try {
      setRows(await api.get<Specialty[]>('/api/specialties'))
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar las especialidades')
    }
  }
  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setEditing(null)
    setForm({ name: '', description: '' })
    setFormError(null)
    setFormOpen(true)
  }
  function openEdit(s: Specialty) {
    setEditing(s)
    setForm({ name: s.name, description: s.description ?? '' })
    setFormError(null)
    setFormOpen(true)
  }

  async function submit() {
    if (!form.name) {
      setFormError('El nombre es obligatorio')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      const body = { name: form.name, description: form.description || null }
      if (editing) await api.put(`/api/specialties/${editing.id}`, body)
      else await api.post('/api/specialties', body)
      setFormOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar la especialidad')
    } finally {
      setSaving(false)
    }
  }

  async function remove(s: Specialty) {
    if (!window.confirm(`¿Eliminar "${s.name}"?`)) return
    try {
      await api.delete(`/api/specialties/${s.id}`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo eliminar')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <SectionHeader canEdit={canEdit} onCreate={openCreate} />
      <Table<Specialty>
        rows={rows}
        empty="Sin especialidades registradas."
        columns={[
          { key: 'name', label: 'Nombre', render: (s) => s.name },
          { key: 'description', label: 'Descripción', render: (s) => s.description || '—' },
        ]}
        renderActions={
          canEdit
            ? (s) => (
                <div className="flex items-center gap-space-sm">
                  <button onClick={() => openEdit(s)} className="font-label-sm text-label-sm text-primary">
                    Editar
                  </button>
                  <button onClick={() => remove(s)} className="font-label-sm text-label-sm text-error">
                    Eliminar
                  </button>
                </div>
              )
            : undefined
        }
      />

      {formOpen ? (
        <Modal title={editing ? 'Editar especialidad' : 'Nueva especialidad'} onClose={() => setFormOpen(false)}>
          {formError ? (
            <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
              {formError}
            </div>
          ) : null}
          <div className="flex flex-col gap-space-sm">
            <input
              placeholder="Nombre *"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="input"
            />
            <input
              placeholder="Descripción"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="input"
            />
            <div className="flex justify-end gap-space-sm mt-space-sm">
              <button
                onClick={() => setFormOpen(false)}
                className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={submit}
                disabled={saving}
                className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
              >
                {saving ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </div>
        </Modal>
      ) : null}
    </div>
  )
}

// ============================== Servicios ==============================

function ServiciosTab({ canEdit, onError }: { canEdit: boolean; onError: (m: string | null) => void }) {
  const [rows, setRows] = useState<ServiceItem[]>([])
  const [specialties, setSpecialties] = useState<Specialty[]>([])
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<ServiceItem | null>(null)
  const [form, setForm] = useState({
    name: '',
    minutes: '30',
    price: '0',
    sessions: '1',
    countsAsSession: true,
    bufferMinutes: '0',
    specialtyId: '',
  })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    try {
      const [sv, sp] = await Promise.all([
        api.get<ServiceItem[]>('/api/services'),
        api.get<Specialty[]>('/api/specialties'),
      ])
      setRows(sv)
      setSpecialties(sp)
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar los servicios')
    }
  }
  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setEditing(null)
    setForm({ name: '', minutes: '30', price: '0', sessions: '1', countsAsSession: true, bufferMinutes: '0', specialtyId: '' })
    setFormError(null)
    setFormOpen(true)
  }
  function openEdit(s: ServiceItem) {
    setEditing(s)
    setForm({
      name: s.name,
      minutes: String(s.minutes),
      price: String(s.price),
      sessions: String(s.sessions),
      countsAsSession: s.countsAsSession,
      bufferMinutes: String(s.bufferMinutes),
      specialtyId: s.specialtyId ?? '',
    })
    setFormError(null)
    setFormOpen(true)
  }

  async function submit() {
    if (!form.name) {
      setFormError('El nombre es obligatorio')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      const body = {
        name: form.name,
        minutes: Number(form.minutes),
        price: Number(form.price),
        sessions: Number(form.sessions),
        countsAsSession: form.countsAsSession,
        bufferMinutes: Number(form.bufferMinutes),
        specialtyId: form.specialtyId || null,
      }
      if (editing) await api.put(`/api/services/${editing.id}`, body)
      else await api.post('/api/services', body)
      setFormOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar el servicio')
    } finally {
      setSaving(false)
    }
  }

  async function remove(s: ServiceItem) {
    if (!window.confirm(`¿Eliminar "${s.name}"?`)) return
    try {
      await api.delete(`/api/services/${s.id}`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo eliminar')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <SectionHeader canEdit={canEdit} onCreate={openCreate} />
      <Table<ServiceItem>
        rows={rows}
        empty="Sin servicios registrados."
        columns={[
          { key: 'name', label: 'Nombre', render: (s) => s.name },
          { key: 'minutes', label: 'Duración', render: (s) => `${s.minutes} min` },
          { key: 'price', label: 'Precio', render: (s) => s.price.toLocaleString('es-MX', { style: 'currency', currency: 'MXN' }) },
          { key: 'session', label: '¿Cuenta sesión?', render: (s) => (s.countsAsSession ? 'Sí' : 'No') },
        ]}
        renderActions={
          canEdit
            ? (s) => (
                <div className="flex items-center gap-space-sm">
                  <button onClick={() => openEdit(s)} className="font-label-sm text-label-sm text-primary">
                    Editar
                  </button>
                  <button onClick={() => remove(s)} className="font-label-sm text-label-sm text-error">
                    Eliminar
                  </button>
                </div>
              )
            : undefined
        }
      />

      {formOpen ? (
        <Modal title={editing ? 'Editar servicio' : 'Nuevo servicio'} onClose={() => setFormOpen(false)}>
          {formError ? (
            <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
              {formError}
            </div>
          ) : null}
          <div className="flex flex-col gap-space-sm">
            <input
              placeholder="Nombre *"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="input"
            />
            <select
              value={form.specialtyId}
              onChange={(e) => setForm({ ...form, specialtyId: e.target.value })}
              className="input"
            >
              <option value="">Sin especialidad</option>
              {specialties.map((sp) => (
                <option key={sp.id} value={sp.id}>
                  {sp.name}
                </option>
              ))}
            </select>
            <div className="grid grid-cols-2 gap-space-sm">
              <input
                type="number"
                min={1}
                placeholder="Minutos"
                value={form.minutes}
                onChange={(e) => setForm({ ...form, minutes: e.target.value })}
                className="input"
              />
              <input
                type="number"
                min={0}
                step="0.01"
                placeholder="Precio"
                value={form.price}
                onChange={(e) => setForm({ ...form, price: e.target.value })}
                className="input"
              />
              <input
                type="number"
                min={0}
                placeholder="Buffer (min)"
                value={form.bufferMinutes}
                onChange={(e) => setForm({ ...form, bufferMinutes: e.target.value })}
                className="input"
              />
              <input
                type="number"
                min={0}
                placeholder="Sesiones sugeridas"
                value={form.sessions}
                onChange={(e) => setForm({ ...form, sessions: e.target.value })}
                className="input"
              />
            </div>
            <label className="flex items-center gap-space-xs font-body-sm text-body-sm text-on-surface">
              <input
                type="checkbox"
                checked={form.countsAsSession}
                onChange={(e) => setForm({ ...form, countsAsSession: e.target.checked })}
              />
              Cuenta como sesión de un tratamiento
            </label>
            <div className="flex justify-end gap-space-sm mt-space-sm">
              <button
                onClick={() => setFormOpen(false)}
                className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={submit}
                disabled={saving}
                className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
              >
                {saving ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </div>
        </Modal>
      ) : null}
    </div>
  )
}

// ============================== Especialistas ==============================

function EspecialistasTab({ canEdit, onError }: { canEdit: boolean; onError: (m: string | null) => void }) {
  const [rows, setRows] = useState<Specialist[]>([])
  const [specialties, setSpecialties] = useState<Specialty[]>([])
  const [services, setServices] = useState<ServiceItem[]>([])
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Specialist | null>(null)
  const [form, setForm] = useState({
    name: '',
    documentId: '',
    email: '',
    phone: '',
    calendarColor: '#00436a',
    specialtyIds: [] as string[],
    serviceIds: [] as string[],
  })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    try {
      const [sp, spec, sv] = await Promise.all([
        api.get<Specialist[]>('/api/specialists'),
        api.get<Specialty[]>('/api/specialties'),
        api.get<ServiceItem[]>('/api/services'),
      ])
      setRows(sp)
      setSpecialties(spec)
      setServices(sv)
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar los especialistas')
    }
  }
  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setEditing(null)
    setForm({ name: '', documentId: '', email: '', phone: '', calendarColor: '#00436a', specialtyIds: [], serviceIds: [] })
    setFormError(null)
    setFormOpen(true)
  }
  function openEdit(s: Specialist) {
    setEditing(s)
    setForm({
      name: s.name,
      documentId: s.documentId ?? '',
      email: s.email ?? '',
      phone: s.phone ?? '',
      calendarColor: s.calendarColor ?? '#00436a',
      specialtyIds: s.specialties.map((x) => x.id),
      serviceIds: s.services.map((x) => x.id),
    })
    setFormError(null)
    setFormOpen(true)
  }

  function toggleId(list: string[], id: string) {
    return list.includes(id) ? list.filter((x) => x !== id) : [...list, id]
  }

  async function submit() {
    if (!form.name) {
      setFormError('El nombre es obligatorio')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      const body = {
        name: form.name,
        documentId: form.documentId || null,
        email: form.email || null,
        phone: form.phone || null,
        calendarColor: form.calendarColor || null,
        specialtyIds: form.specialtyIds,
        serviceIds: form.serviceIds,
      }
      if (editing) await api.put(`/api/specialists/${editing.id}`, body)
      else await api.post('/api/specialists', body)
      setFormOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar el especialista')
    } finally {
      setSaving(false)
    }
  }

  async function remove(s: Specialist) {
    if (!window.confirm(`¿Dar de baja a "${s.name}"?`)) return
    try {
      await api.delete(`/api/specialists/${s.id}`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo dar de baja')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <SectionHeader canEdit={canEdit} onCreate={openCreate} />
      <Table<Specialist>
        rows={rows}
        empty="Sin especialistas registrados."
        columns={[
          { key: 'name', label: 'Nombre', render: (s) => s.name },
          { key: 'specialties', label: 'Especialidades', render: (s) => s.specialties.map((x) => x.name).join(', ') || '—' },
          { key: 'phone', label: 'Teléfono', render: (s) => s.phone || '—' },
        ]}
        renderActions={
          canEdit
            ? (s) => (
                <div className="flex items-center gap-space-sm">
                  <button onClick={() => openEdit(s)} className="font-label-sm text-label-sm text-primary">
                    Editar
                  </button>
                  <button onClick={() => remove(s)} className="font-label-sm text-label-sm text-error">
                    Dar de baja
                  </button>
                </div>
              )
            : undefined
        }
      />

      {formOpen ? (
        <Modal title={editing ? 'Editar especialista' : 'Nuevo especialista'} onClose={() => setFormOpen(false)}>
          {formError ? (
            <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
              {formError}
            </div>
          ) : null}
          <div className="flex flex-col gap-space-sm">
            <input
              placeholder="Nombre *"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="input"
            />
            <div className="grid grid-cols-2 gap-space-sm">
              <input
                placeholder="Cédula / documento"
                value={form.documentId}
                onChange={(e) => setForm({ ...form, documentId: e.target.value })}
                className="input"
              />
              <input
                placeholder="Teléfono"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                className="input"
              />
            </div>
            <input
              placeholder="Email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="input"
            />
            <div className="flex flex-col gap-1">
              <label className="font-label-sm text-label-sm text-on-surface-variant">Especialidades</label>
              <div className="flex flex-wrap gap-space-xs">
                {specialties.map((sp) => (
                  <label
                    key={sp.id}
                    className="flex items-center gap-1 px-space-sm py-1 rounded-full bg-surface-container-low font-label-sm text-label-sm text-on-surface"
                  >
                    <input
                      type="checkbox"
                      checked={form.specialtyIds.includes(sp.id)}
                      onChange={() => setForm({ ...form, specialtyIds: toggleId(form.specialtyIds, sp.id) })}
                    />
                    {sp.name}
                  </label>
                ))}
              </div>
            </div>
            <div className="flex flex-col gap-1">
              <label className="font-label-sm text-label-sm text-on-surface-variant">Servicios que ofrece</label>
              <div className="flex flex-wrap gap-space-xs">
                {services.map((sv) => (
                  <label
                    key={sv.id}
                    className="flex items-center gap-1 px-space-sm py-1 rounded-full bg-surface-container-low font-label-sm text-label-sm text-on-surface"
                  >
                    <input
                      type="checkbox"
                      checked={form.serviceIds.includes(sv.id)}
                      onChange={() => setForm({ ...form, serviceIds: toggleId(form.serviceIds, sv.id) })}
                    />
                    {sv.name}
                  </label>
                ))}
              </div>
            </div>
            <div className="flex justify-end gap-space-sm mt-space-sm">
              <button
                onClick={() => setFormOpen(false)}
                className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={submit}
                disabled={saving}
                className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
              >
                {saving ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </div>
        </Modal>
      ) : null}
    </div>
  )
}

// ============================== Consultorios ==============================

function ConsultoriosTab({ canEdit, onError }: { canEdit: boolean; onError: (m: string | null) => void }) {
  const [rows, setRows] = useState<Room[]>([])
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Room | null>(null)
  const [form, setForm] = useState({ name: '', location: '' })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    try {
      setRows(await api.get<Room[]>('/api/rooms'))
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar los consultorios')
    }
  }
  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setEditing(null)
    setForm({ name: '', location: '' })
    setFormError(null)
    setFormOpen(true)
  }
  function openEdit(r: Room) {
    setEditing(r)
    setForm({ name: r.name, location: r.location ?? '' })
    setFormError(null)
    setFormOpen(true)
  }

  async function submit() {
    if (!form.name) {
      setFormError('El nombre es obligatorio')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      const body = { name: form.name, location: form.location || null }
      if (editing) await api.put(`/api/rooms/${editing.id}`, body)
      else await api.post('/api/rooms', body)
      setFormOpen(false)
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar el consultorio')
    } finally {
      setSaving(false)
    }
  }

  async function remove(r: Room) {
    if (!window.confirm(`¿Eliminar "${r.name}"?`)) return
    try {
      await api.delete(`/api/rooms/${r.id}`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo eliminar')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <SectionHeader canEdit={canEdit} onCreate={openCreate} />
      <Table<Room>
        rows={rows}
        empty="Sin consultorios registrados."
        columns={[
          { key: 'name', label: 'Nombre', render: (r) => r.name },
          { key: 'location', label: 'Ubicación', render: (r) => r.location || '—' },
        ]}
        renderActions={
          canEdit
            ? (r) => (
                <div className="flex items-center gap-space-sm">
                  <button onClick={() => openEdit(r)} className="font-label-sm text-label-sm text-primary">
                    Editar
                  </button>
                  <button onClick={() => remove(r)} className="font-label-sm text-label-sm text-error">
                    Eliminar
                  </button>
                </div>
              )
            : undefined
        }
      />

      {formOpen ? (
        <Modal title={editing ? 'Editar consultorio' : 'Nuevo consultorio'} onClose={() => setFormOpen(false)}>
          {formError ? (
            <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
              {formError}
            </div>
          ) : null}
          <div className="flex flex-col gap-space-sm">
            <input
              placeholder="Nombre *"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="input"
            />
            <input
              placeholder="Ubicación"
              value={form.location}
              onChange={(e) => setForm({ ...form, location: e.target.value })}
              className="input"
            />
            <div className="flex justify-end gap-space-sm mt-space-sm">
              <button
                onClick={() => setFormOpen(false)}
                className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={submit}
                disabled={saving}
                className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
              >
                {saving ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </div>
        </Modal>
      ) : null}
    </div>
  )
}
