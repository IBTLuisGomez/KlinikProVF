import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'

interface Patient {
  id: string
  code: string
  firstName: string
  lastNameP?: string | null
  lastNameM?: string | null
  fullName: string
  phone?: string | null
  email?: string | null
  birthDate?: string | null
  notes?: string | null
  specialist?: string | null
  treater?: string | null
  insurer: boolean
  referred: boolean
  autoCreated: boolean
  lateCancellationCount: number
  blocked: boolean
}

interface PatientReq {
  code: string | null
  firstName: string
  lastNameP: string | null
  lastNameM: string | null
  phone: string | null
  email: string | null
  birthDate: string | null
  notes: string | null
  specialist: string | null
  treater: string | null
  insurer: boolean
  referred: boolean
}

interface Appointment {
  id: string
  startsAt: string
  status: string
}
interface Transaction {
  id: string
  folio: string
  date: string
  amount: number
}
interface Treatment {
  id: string
  status: string
  recommended: number
  used: number
  paid: number
}
interface BitacoraEntry {
  when: string
  type: 'cita' | 'cobro' | 'tratamiento'
  description: string
}
interface PatientHistoryReport {
  patient: Patient
  citas: Appointment[]
  cobros: Transaction[]
  tratamientos: Treatment[]
  bitacora: BitacoraEntry[]
}

const EMPTY_FORM: PatientReq = {
  code: null,
  firstName: '',
  lastNameP: null,
  lastNameM: null,
  phone: null,
  email: null,
  birthDate: null,
  notes: null,
  specialist: null,
  treater: null,
  insurer: false,
  referred: false,
}

const money = (v: number) => v.toLocaleString('es-MX', { style: 'currency', currency: 'MXN' })
const dateOnly = (v?: string | null) => (v ? new Date(v).toLocaleDateString('es-MX') : '—')

const BITACORA_ICON: Record<BitacoraEntry['type'], string> = {
  cita: 'event',
  cobro: 'payments',
  tratamiento: 'medical_information',
}

export function PacientesPage() {
  const { session } = useAuth()
  const canManage = session ? ['ADMIN', 'COORDINADOR', 'RECEPCION'].includes(session.role) : false
  const canLead = session ? ['ADMIN', 'COORDINADOR'].includes(session.role) : false

  const [patients, setPatients] = useState<Patient[]>([])
  const [q, setQ] = useState('')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [history, setHistory] = useState<PatientHistoryReport | null>(null)
  const [listLoading, setListLoading] = useState(true)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<PatientReq>(EMPTY_FORM)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function loadList(query?: string) {
    setListLoading(true)
    setError(null)
    try {
      const qs = query ? `?q=${encodeURIComponent(query)}` : ''
      const data = await api.get<Patient[]>(`/api/patients${qs}`)
      setPatients(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cargar la lista de pacientes')
    } finally {
      setListLoading(false)
    }
  }

  async function loadHistory(id: string) {
    setHistoryLoading(true)
    try {
      const data = await api.get<PatientHistoryReport>(`/api/reportes/pacientes/${id}`)
      setHistory(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cargar el expediente del paciente')
    } finally {
      setHistoryLoading(false)
    }
  }

  useEffect(() => {
    loadList()
  }, [])

  useEffect(() => {
    if (selectedId) loadHistory(selectedId)
    else setHistory(null)
  }, [selectedId])

  function openCreate() {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setFormError(null)
    setFormOpen(true)
  }

  function openEdit(p: Patient) {
    setEditingId(p.id)
    setForm({
      code: p.code,
      firstName: p.firstName,
      lastNameP: p.lastNameP ?? null,
      lastNameM: p.lastNameM ?? null,
      phone: p.phone ?? null,
      email: p.email ?? null,
      birthDate: p.birthDate ?? null,
      notes: p.notes ?? null,
      specialist: p.specialist ?? null,
      treater: p.treater ?? null,
      insurer: p.insurer,
      referred: p.referred,
    })
    setFormError(null)
    setFormOpen(true)
  }

  async function submitForm() {
    setSaving(true)
    setFormError(null)
    try {
      const saved = editingId
        ? await api.put<Patient>(`/api/patients/${editingId}`, form)
        : await api.post<Patient>('/api/patients', form)
      setFormOpen(false)
      await loadList(q)
      setSelectedId(saved.id)
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar el paciente')
    } finally {
      setSaving(false)
    }
  }

  async function removePatient(p: Patient) {
    if (!window.confirm(`¿Eliminar a ${p.fullName}? Esta acción no se puede deshacer.`)) return
    try {
      await api.delete(`/api/patients/${p.id}`)
      if (selectedId === p.id) setSelectedId(null)
      await loadList(q)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo eliminar el paciente')
    }
  }

  async function unblock(p: Patient) {
    try {
      await api.post(`/api/patients/${p.id}/desbloquear`)
      await loadList(q)
      if (selectedId === p.id) await loadHistory(p.id)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo desbloquear al paciente')
    }
  }

  const selected = history?.patient

  return (
    <div className="flex flex-col gap-space-lg">
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-space-md bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
        <div>
          <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest">
            Gestión clínica
          </span>
          <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold tracking-tight mt-1">
            Pacientes e Historias
          </h1>
        </div>
        {canManage ? (
          <button
            onClick={openCreate}
            className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
          >
            <span className="material-symbols-outlined text-[18px]">person_add</span>
            Nuevo paciente
          </button>
        ) : null}
      </div>

      {error ? (
        <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm">
          {error}
        </div>
      ) : null}

      <div className="grid grid-cols-1 xl:grid-cols-[360px_1fr] gap-space-lg items-start">
        {/* ---------- Lista ---------- */}
        <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-md flex flex-col gap-space-sm">
          <form
            className="relative"
            onSubmit={(e) => {
              e.preventDefault()
              loadList(q)
            }}
          >
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[18px]">
              search
            </span>
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Buscar por código, nombre o teléfono…"
              className="w-full pl-9 pr-3 py-2 rounded-lg bg-surface-container-low text-on-surface font-body-sm text-body-sm focus:outline-none"
            />
          </form>

          <span className="font-label-sm text-label-sm text-on-surface-variant">
            {listLoading ? 'Cargando…' : `${patients.length} paciente(s)`}
          </span>

          <div className="flex flex-col gap-space-xs max-h-[70vh] overflow-y-auto">
            {patients.map((p) => (
              <button
                key={p.id}
                onClick={() => setSelectedId(p.id)}
                className={`text-left px-space-sm py-space-sm rounded-lg transition-colors ${
                  selectedId === p.id
                    ? 'bg-primary text-on-primary'
                    : 'hover:bg-surface-container-low text-on-surface'
                }`}
              >
                <div className="flex items-center justify-between gap-space-xs">
                  <span className="font-label-lg text-label-lg font-semibold truncate">{p.fullName}</span>
                  <span
                    className={`font-label-sm text-label-sm shrink-0 ${
                      selectedId === p.id ? 'text-on-primary/80' : 'text-on-surface-variant'
                    }`}
                  >
                    HC-{p.code}
                  </span>
                </div>
                <div className="flex items-center gap-space-xs mt-1 flex-wrap">
                  {p.blocked ? (
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-error-container text-on-error-container font-label-sm text-label-sm">
                      Bloqueado
                    </span>
                  ) : null}
                  {p.insurer ? (
                    <span
                      className={`font-label-sm text-label-sm ${
                        selectedId === p.id ? 'text-on-primary/80' : 'text-on-surface-variant'
                      }`}
                    >
                      Aseguradora
                    </span>
                  ) : null}
                  {p.phone ? (
                    <span
                      className={`font-label-sm text-label-sm ${
                        selectedId === p.id ? 'text-on-primary/80' : 'text-on-surface-variant'
                      }`}
                    >
                      {p.phone}
                    </span>
                  ) : null}
                </div>
              </button>
            ))}
            {!listLoading && patients.length === 0 ? (
              <p className="font-body-sm text-body-sm text-on-surface-variant px-space-sm py-space-md">
                Sin pacientes que coincidan con la búsqueda.
              </p>
            ) : null}
          </div>
        </div>

        {/* ---------- Detalle ---------- */}
        <div className="flex flex-col gap-space-md">
          {!selectedId ? (
            <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-xl flex flex-col items-center justify-center text-center min-h-[40vh]">
              <span className="material-symbols-outlined text-primary text-[40px] mb-space-sm">patient_list</span>
              <p className="font-body-md text-body-md text-on-surface-variant">
                Selecciona un paciente de la lista para ver su expediente.
              </p>
            </div>
          ) : historyLoading || !selected ? (
            <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-xl font-body-md text-body-md text-on-surface-variant">
              Cargando expediente…
            </div>
          ) : (
            <>
              {selected.blocked ? (
                <div className="rounded-xl bg-error-container text-on-error-container px-space-lg py-space-md flex items-center justify-between gap-space-md">
                  <div className="flex items-center gap-space-sm">
                    <span className="material-symbols-outlined">block</span>
                    <span className="font-body-sm text-body-sm">
                      Paciente bloqueado por {selected.lateCancellationCount} cancelación(es) tardía(s) sin el aviso
                      mínimo. No se le pueden agendar nuevas citas hasta reactivarlo.
                    </span>
                  </div>
                  {canLead ? (
                    <button
                      onClick={() => unblock(selected)}
                      className="shrink-0 px-space-md py-1.5 rounded-lg bg-on-error-container text-error-container font-label-md text-label-md font-semibold"
                    >
                      Desbloquear
                    </button>
                  ) : null}
                </div>
              ) : null}

              <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-lg">
                <div className="flex items-start justify-between gap-space-md flex-wrap">
                  <div>
                    <div className="flex items-center gap-space-sm flex-wrap">
                      <h2 className="font-headline-md text-headline-md text-on-surface font-bold">
                        {selected.fullName}
                      </h2>
                      <span className="font-label-sm text-label-sm text-on-surface-variant">HC-{selected.code}</span>
                      {selected.autoCreated ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-surface-container-low font-label-sm text-label-sm text-on-surface-variant">
                          Alta automática
                        </span>
                      ) : null}
                      {selected.insurer ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-secondary-fixed text-on-secondary-fixed-variant font-label-sm text-label-sm">
                          Aseguradora
                        </span>
                      ) : null}
                      {selected.referred ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-primary-fixed text-primary font-label-sm text-label-sm">
                          Referido
                        </span>
                      ) : null}
                    </div>
                    <div className="flex flex-wrap gap-space-md mt-space-sm font-body-sm text-body-sm text-on-surface-variant">
                      <span>Tel: {selected.phone || '—'}</span>
                      <span>Email: {selected.email || '—'}</span>
                      <span>Nacimiento: {dateOnly(selected.birthDate)}</span>
                      {selected.specialist ? <span>Especialista: {selected.specialist}</span> : null}
                      {selected.treater ? <span>Tratante: {selected.treater}</span> : null}
                    </div>
                    {selected.notes ? (
                      <p className="font-body-sm text-body-sm text-on-surface-variant mt-space-sm max-w-xl">
                        {selected.notes}
                      </p>
                    ) : null}
                  </div>
                  {canManage ? (
                    <div className="flex items-center gap-space-xs shrink-0">
                      <button
                        onClick={() => openEdit(selected)}
                        className="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface transition-colors"
                        title="Editar"
                      >
                        <span className="material-symbols-outlined text-[20px]">edit</span>
                      </button>
                      {canLead ? (
                        <button
                          onClick={() => removePatient(selected)}
                          className="p-2 rounded-lg text-on-surface-variant hover:bg-error-container hover:text-on-error-container transition-colors"
                          title="Eliminar"
                        >
                          <span className="material-symbols-outlined text-[20px]">delete</span>
                        </button>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-space-md">
                <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm">
                  <span className="font-label-md text-label-md text-on-surface-variant">Citas totales</span>
                  <div className="font-headline-lg text-headline-lg text-on-surface font-bold mt-1">
                    {history!.citas.length}
                  </div>
                </div>
                <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm">
                  <span className="font-label-md text-label-md text-on-surface-variant">Cobrado</span>
                  <div className="font-headline-lg text-headline-lg text-on-surface font-bold mt-1">
                    {money(history!.cobros.reduce((sum, c) => sum + c.amount, 0))}
                  </div>
                </div>
                <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm">
                  <span className="font-label-md text-label-md text-on-surface-variant">Tratamientos</span>
                  <div className="font-headline-lg text-headline-lg text-on-surface font-bold mt-1">
                    {history!.tratamientos.length}
                  </div>
                </div>
              </div>

              <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-lg">
                <h3 className="font-headline-sm text-headline-sm text-on-surface font-semibold mb-space-md">
                  Bitácora
                </h3>
                {history!.bitacora.length === 0 ? (
                  <p className="font-body-sm text-body-sm text-on-surface-variant">
                    Sin movimientos registrados todavía.
                  </p>
                ) : (
                  <div className="flex flex-col gap-space-sm max-h-[50vh] overflow-y-auto">
                    {history!.bitacora.map((e, i) => (
                      <div key={i} className="flex items-start gap-space-sm">
                        <span className="material-symbols-outlined text-primary text-[18px] mt-0.5">
                          {BITACORA_ICON[e.type]}
                        </span>
                        <div className="flex flex-col">
                          <span className="font-body-sm text-body-sm text-on-surface">{e.description}</span>
                          <span className="font-label-sm text-label-sm text-on-surface-variant">
                            {new Date(e.when).toLocaleString('es-MX')}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {formOpen ? (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
          <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">
              {editingId ? 'Editar paciente' : 'Nuevo paciente'}
            </h3>

            {formError ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
                {formError}
              </div>
            ) : null}

            <form
              className="flex flex-col gap-space-sm"
              onSubmit={(e) => {
                e.preventDefault()
                submitForm()
              }}
            >
              <div className="grid grid-cols-2 gap-space-sm">
                <Field label="Nombre(s) *">
                  <input
                    required
                    value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                    className="input"
                  />
                </Field>
                <Field label="Código (opcional)">
                  <input
                    value={form.code ?? ''}
                    onChange={(e) => setForm({ ...form, code: e.target.value || null })}
                    className="input"
                  />
                </Field>
                <Field label="Apellido paterno">
                  <input
                    value={form.lastNameP ?? ''}
                    onChange={(e) => setForm({ ...form, lastNameP: e.target.value || null })}
                    className="input"
                  />
                </Field>
                <Field label="Apellido materno">
                  <input
                    value={form.lastNameM ?? ''}
                    onChange={(e) => setForm({ ...form, lastNameM: e.target.value || null })}
                    className="input"
                  />
                </Field>
                <Field label="Teléfono">
                  <input
                    value={form.phone ?? ''}
                    onChange={(e) => setForm({ ...form, phone: e.target.value || null })}
                    className="input"
                  />
                </Field>
                <Field label="Email">
                  <input
                    type="email"
                    value={form.email ?? ''}
                    onChange={(e) => setForm({ ...form, email: e.target.value || null })}
                    className="input"
                  />
                </Field>
                <Field label="Fecha de nacimiento">
                  <input
                    type="date"
                    value={form.birthDate ?? ''}
                    onChange={(e) => setForm({ ...form, birthDate: e.target.value || null })}
                    className="input"
                  />
                </Field>
                <Field label="Especialista">
                  <input
                    value={form.specialist ?? ''}
                    onChange={(e) => setForm({ ...form, specialist: e.target.value || null })}
                    className="input"
                  />
                </Field>
                <Field label="Tratante (si difiere del especialista)">
                  <input
                    value={form.treater ?? ''}
                    onChange={(e) => setForm({ ...form, treater: e.target.value || null })}
                    className="input"
                  />
                </Field>
              </div>

              <Field label="Notas">
                <textarea
                  value={form.notes ?? ''}
                  onChange={(e) => setForm({ ...form, notes: e.target.value || null })}
                  rows={2}
                  className="input resize-none"
                />
              </Field>

              <div className="flex items-center gap-space-lg mt-space-xs">
                <label className="flex items-center gap-space-xs font-body-sm text-body-sm text-on-surface">
                  <input
                    type="checkbox"
                    checked={form.insurer}
                    onChange={(e) => setForm({ ...form, insurer: e.target.checked })}
                  />
                  Paciente de aseguradora
                </label>
                <label className="flex items-center gap-space-xs font-body-sm text-body-sm text-on-surface">
                  <input
                    type="checkbox"
                    checked={form.referred}
                    onChange={(e) => setForm({ ...form, referred: e.target.checked })}
                  />
                  Referido
                </label>
              </div>

              <div className="flex justify-end gap-space-sm mt-space-md">
                <button
                  type="button"
                  onClick={() => setFormOpen(false)}
                  className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
                >
                  {saving ? 'Guardando…' : 'Guardar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="font-label-sm text-label-sm text-on-surface-variant">{label}</label>
      {children}
    </div>
  )
}
