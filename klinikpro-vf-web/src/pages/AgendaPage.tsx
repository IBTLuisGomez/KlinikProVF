import { useEffect, useMemo, useState } from 'react'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'

type AppointmentStatus =
  | 'SCHEDULED'
  | 'CONFIRMED'
  | 'WAITING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'RESCHEDULED'
  | 'NO_SHOW'
  | 'EXPIRED'

interface Appointment {
  id: string
  patientId: string
  serviceId: string
  specialistId: string
  roomId?: string | null
  startsAt: string
  endsAt: string
  status: AppointmentStatus
  seriesId?: string | null
  lateCancellationFee: boolean
}

interface Specialist {
  id: string
  name: string
  calendarColor?: string | null
  services: { id: string; name: string }[]
}
interface ServiceItem {
  id: string
  name: string
  minutes: number
  price: number
  countsAsSession: boolean
}
interface Room {
  id: string
  name: string
}
interface PatientLite {
  id: string
  code: string
  fullName: string
  phone?: string | null
}

const STATUS_LABEL: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Programada',
  CONFIRMED: 'Confirmada',
  WAITING: 'En espera',
  IN_PROGRESS: 'En atención',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
  RESCHEDULED: 'Reprogramada',
  NO_SHOW: 'No asistió',
  EXPIRED: 'Expirada',
}

const STATUS_TONE: Record<AppointmentStatus, string> = {
  SCHEDULED: 'bg-surface-container-low text-on-surface-variant',
  CONFIRMED: 'bg-primary-fixed text-primary',
  WAITING: 'bg-secondary-fixed text-on-secondary-fixed-variant',
  IN_PROGRESS: 'bg-secondary text-on-secondary',
  COMPLETED: 'bg-secondary-fixed text-on-secondary-fixed-variant',
  CANCELLED: 'bg-error-container text-on-error-container',
  RESCHEDULED: 'bg-surface-container-low text-on-surface-variant',
  NO_SHOW: 'bg-error-container text-on-error-container',
  EXPIRED: 'bg-surface-container-low text-on-surface-variant',
}

const ACTIVE_STATUSES: AppointmentStatus[] = ['SCHEDULED', 'CONFIRMED', 'WAITING', 'IN_PROGRESS']

function toDateInput(d: Date) {
  return d.toISOString().slice(0, 10)
}
function rangeForDay(dateStr: string): { from: string; to: string } {
  const start = new Date(`${dateStr}T00:00:00`)
  const end = new Date(start)
  end.setDate(end.getDate() + 1)
  return { from: start.toISOString(), to: end.toISOString() }
}
const timeLabel = (iso: string) =>
  new Date(iso).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })

const EMPTY_CREATE = {
  patientId: '',
  patientLabel: '',
  specialistId: '',
  serviceId: '',
  roomId: '',
  time: '09:00',
  isSeries: false,
  frequency: 'semanal',
  occurrences: 4,
}

export function AgendaPage() {
  const { session } = useAuth()
  const canFrontDesk = session ? ['ADMIN', 'COORDINADOR', 'RECEPCION'].includes(session.role) : false
  const canClinical = session ? ['ADMIN', 'COORDINADOR', 'FISIO'].includes(session.role) : false

  const [date, setDate] = useState(() => toDateInput(new Date()))
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [specialists, setSpecialists] = useState<Specialist[]>([])
  const [services, setServices] = useState<ServiceItem[]>([])
  const [rooms, setRooms] = useState<Room[]>([])
  const [patients, setPatients] = useState<PatientLite[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actingId, setActingId] = useState<string | null>(null)

  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(EMPTY_CREATE)
  const [patientQuery, setPatientQuery] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [seriesResult, setSeriesResult] = useState<{ created: number; skipped: string[] } | null>(null)

  async function loadCatalogs() {
    try {
      const [sp, sv, rm, pt] = await Promise.all([
        api.get<Specialist[]>('/api/specialists'),
        api.get<ServiceItem[]>('/api/services'),
        api.get<Room[]>('/api/rooms'),
        api.get<PatientLite[]>('/api/patients'),
      ])
      setSpecialists(sp)
      setServices(sv)
      setRooms(rm)
      setPatients(pt)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudieron cargar los catálogos de Agenda')
    }
  }

  async function loadDay(d: string) {
    setLoading(true)
    setError(null)
    try {
      const { from, to } = rangeForDay(d)
      const data = await api.get<Appointment[]>(
        `/api/appointments?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      )
      setAppointments(data.sort((a, b) => a.startsAt.localeCompare(b.startsAt)))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cargar la agenda del día')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCatalogs()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  useEffect(() => {
    loadDay(date)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date])

  const specialistById = useMemo(() => new Map(specialists.map((s) => [s.id, s])), [specialists])
  const serviceById = useMemo(() => new Map(services.map((s) => [s.id, s])), [services])
  const roomById = useMemo(() => new Map(rooms.map((r) => [r.id, r])), [rooms])
  const patientById = useMemo(() => new Map(patients.map((p) => [p.id, p])), [patients])

  const grouped = useMemo(() => {
    const bySpecialist = new Map<string, Appointment[]>()
    for (const a of appointments) {
      const key = a.specialistId
      if (!bySpecialist.has(key)) bySpecialist.set(key, [])
      bySpecialist.get(key)!.push(a)
    }
    return [...bySpecialist.entries()].sort(([a], [b]) =>
      (specialistById.get(a)?.name ?? '').localeCompare(specialistById.get(b)?.name ?? ''),
    )
  }, [appointments, specialistById])

  const patientMatches = useMemo(() => {
    if (patientQuery.trim().length < 2) return []
    const q = patientQuery.toLowerCase()
    return patients.filter((p) => p.fullName.toLowerCase().includes(q) || p.code.includes(q)).slice(0, 6)
  }, [patientQuery, patients])

  const specialistServices = useMemo(() => {
    const sp = specialistById.get(form.specialistId)
    if (!sp) return services
    const ids = new Set(sp.services.map((s) => s.id))
    return services.filter((s) => ids.has(s.id))
  }, [form.specialistId, specialistById, services])

  function shiftDay(delta: number) {
    const d = new Date(`${date}T00:00:00`)
    d.setDate(d.getDate() + delta)
    setDate(toDateInput(d))
  }

  async function runAction(fn: () => Promise<unknown>, id: string) {
    setActingId(id)
    setError(null)
    try {
      await fn()
      await loadDay(date)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo completar la acción')
    } finally {
      setActingId(null)
    }
  }

  function openCreate() {
    setForm(EMPTY_CREATE)
    setPatientQuery('')
    setFormError(null)
    setSeriesResult(null)
    setFormOpen(true)
  }

  async function submitCreate() {
    if (!form.patientId || !form.specialistId || !form.serviceId) {
      setFormError('Paciente, médico y servicio son obligatorios')
      return
    }
    const startsAt = new Date(`${date}T${form.time}:00`).toISOString()
    setSaving(true)
    setFormError(null)
    try {
      if (form.isSeries) {
        const res = await api.post<{ seriesId: string; created: Appointment[]; skipped: string[] }>(
          '/api/appointments/series',
          {
            patientId: form.patientId,
            specialistId: form.specialistId,
            serviceId: form.serviceId,
            roomId: form.roomId || null,
            startsAt,
            frequency: form.frequency,
            occurrences: form.occurrences,
          },
        )
        setSeriesResult({ created: res.created.length, skipped: res.skipped })
        await loadDay(date)
      } else {
        await api.post('/api/appointments', {
          patientId: form.patientId,
          specialistId: form.specialistId,
          serviceId: form.serviceId,
          roomId: form.roomId || null,
          startsAt,
        })
        setFormOpen(false)
        await loadDay(date)
      }
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo agendar la cita')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex flex-col gap-space-lg">
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-space-md bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
        <div>
          <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest">
            Gestión clínica
          </span>
          <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold tracking-tight mt-1">
            Agenda de Turnos
          </h1>
        </div>
        <div className="flex items-center gap-space-sm flex-wrap">
          <div className="flex items-center gap-space-xs bg-surface-container-low rounded-lg px-space-xs py-1">
            <button
              onClick={() => shiftDay(-1)}
              className="p-1.5 rounded-lg hover:bg-surface-container text-on-surface-variant"
            >
              <span className="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="bg-transparent font-body-sm text-body-sm text-on-surface focus:outline-none"
            />
            <button
              onClick={() => shiftDay(1)}
              className="p-1.5 rounded-lg hover:bg-surface-container text-on-surface-variant"
            >
              <span className="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
          <button
            onClick={() => setDate(toDateInput(new Date()))}
            className="px-space-sm py-1.5 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
          >
            Hoy
          </button>
          {canFrontDesk ? (
            <button
              onClick={openCreate}
              className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
            >
              <span className="material-symbols-outlined text-[18px]">event_available</span>
              Nueva cita
            </button>
          ) : null}
        </div>
      </div>

      {error ? (
        <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm">
          {error}
        </div>
      ) : null}

      {loading ? (
        <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-xl font-body-md text-body-md text-on-surface-variant">
          Cargando agenda…
        </div>
      ) : grouped.length === 0 ? (
        <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-xl text-center font-body-md text-body-md text-on-surface-variant">
          Sin citas agendadas para este día.
        </div>
      ) : (
        <div className="flex flex-col gap-space-md">
          {grouped.map(([specialistId, items]) => (
            <div key={specialistId} className="bg-surface-container-lowest rounded-xl shadow-sm p-space-lg">
              <h2 className="font-headline-sm text-headline-sm text-on-surface font-semibold mb-space-md">
                {specialistById.get(specialistId)?.name ?? 'Médico'}
              </h2>
              <div className="flex flex-col gap-space-sm">
                {items.map((a) => {
                  const service = serviceById.get(a.serviceId)
                  const patient = patientById.get(a.patientId)
                  const room = a.roomId ? roomById.get(a.roomId) : null
                  const busy = actingId === a.id
                  return (
                    <div
                      key={a.id}
                      className="flex flex-col sm:flex-row sm:items-center justify-between gap-space-sm border border-outline-variant/40 rounded-lg px-space-md py-space-sm"
                    >
                      <div className="flex items-center gap-space-md">
                        <span className="font-label-lg text-label-lg font-semibold text-on-surface w-14">
                          {timeLabel(a.startsAt)}
                        </span>
                        <div className="flex flex-col">
                          <span className="font-body-md text-body-md text-on-surface font-medium">
                            {patient?.fullName ?? 'Paciente'}
                          </span>
                          <span className="font-label-sm text-label-sm text-on-surface-variant">
                            {service?.name ?? 'Servicio'} {room ? `· ${room.name}` : ''}
                            {a.seriesId ? ' · serie' : ''}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-space-xs flex-wrap">
                        <span
                          className={`px-space-sm py-1 rounded-full font-label-sm text-label-sm ${STATUS_TONE[a.status]}`}
                        >
                          {STATUS_LABEL[a.status]}
                        </span>
                        {canFrontDesk && a.status === 'SCHEDULED' ? (
                          <ActionButton
                            label="Confirmar"
                            busy={busy}
                            onClick={() => runAction(() => api.post(`/api/appointments/${a.id}/confirm`), a.id)}
                          />
                        ) : null}
                        {canFrontDesk && (a.status === 'SCHEDULED' || a.status === 'CONFIRMED') ? (
                          <ActionButton
                            label="Llegó"
                            busy={busy}
                            onClick={() => runAction(() => api.post(`/api/appointments/${a.id}/arrival`), a.id)}
                          />
                        ) : null}
                        {canClinical && a.status === 'WAITING' ? (
                          <ActionButton
                            label="Iniciar"
                            busy={busy}
                            onClick={() => runAction(() => api.post(`/api/appointments/${a.id}/start`), a.id)}
                          />
                        ) : null}
                        {canClinical && a.status === 'IN_PROGRESS' ? (
                          <ActionButton
                            label="Finalizar"
                            busy={busy}
                            onClick={() => runAction(() => api.post(`/api/appointments/${a.id}/finish`), a.id)}
                          />
                        ) : null}
                        {canFrontDesk && ACTIVE_STATUSES.includes(a.status) ? (
                          <ActionButton
                            label="No asistió"
                            busy={busy}
                            onClick={() => runAction(() => api.post(`/api/appointments/${a.id}/no-show`), a.id)}
                          />
                        ) : null}
                        {canFrontDesk && (a.status === 'SCHEDULED' || a.status === 'CONFIRMED' || a.status === 'WAITING') ? (
                          <ActionButton
                            label="Cancelar"
                            tone="error"
                            busy={busy}
                            onClick={() =>
                              runAction(
                                () =>
                                  api.post(`/api/appointments/${a.id}/cancel?byPatient=true`, {
                                    reason: window.prompt('Motivo de la cancelación (opcional)') || null,
                                  }),
                                a.id,
                              )
                            }
                          />
                        ) : null}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          ))}
        </div>
      )}

      {formOpen ? (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
          <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">
              Nueva cita — {new Date(`${date}T00:00:00`).toLocaleDateString('es-MX')}
            </h3>

            {formError ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
                {formError}
              </div>
            ) : null}

            {seriesResult ? (
              <div className="rounded-lg bg-secondary-fixed text-on-secondary-fixed-variant px-4 py-3 font-body-sm text-body-sm mb-space-md">
                Se agendaron {seriesResult.created} cita(s) de la serie.
                {seriesResult.skipped.length > 0 ? (
                  <ul className="mt-1 list-disc list-inside">
                    {seriesResult.skipped.map((s, i) => (
                      <li key={i}>{s}</li>
                    ))}
                  </ul>
                ) : null}
              </div>
            ) : null}

            <form
              className="flex flex-col gap-space-sm"
              onSubmit={(e) => {
                e.preventDefault()
                submitCreate()
              }}
            >
              <div className="flex flex-col gap-1 relative">
                <label className="font-label-sm text-label-sm text-on-surface-variant">Paciente *</label>
                <input
                  value={form.patientId ? form.patientLabel : patientQuery}
                  onChange={(e) => {
                    setForm({ ...form, patientId: '', patientLabel: '' })
                    setPatientQuery(e.target.value)
                  }}
                  placeholder="Buscar por nombre o código…"
                  className="input"
                />
                {!form.patientId && patientMatches.length > 0 ? (
                  <div className="absolute top-full mt-1 w-full bg-surface-container-lowest rounded-lg shadow-lg z-10 max-h-48 overflow-y-auto">
                    {patientMatches.map((p) => (
                      <button
                        type="button"
                        key={p.id}
                        onClick={() => {
                          setForm({ ...form, patientId: p.id, patientLabel: p.fullName })
                          setPatientQuery('')
                        }}
                        className="w-full text-left px-space-sm py-space-xs hover:bg-surface-container-low font-body-sm text-body-sm text-on-surface"
                      >
                        {p.fullName} <span className="text-on-surface-variant">· HC-{p.code}</span>
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>

              <div className="grid grid-cols-2 gap-space-sm">
                <div className="flex flex-col gap-1">
                  <label className="font-label-sm text-label-sm text-on-surface-variant">Médico *</label>
                  <select
                    value={form.specialistId}
                    onChange={(e) => setForm({ ...form, specialistId: e.target.value, serviceId: '' })}
                    className="input"
                  >
                    <option value="">Selecciona…</option>
                    {specialists.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="flex flex-col gap-1">
                  <label className="font-label-sm text-label-sm text-on-surface-variant">Servicio *</label>
                  <select
                    value={form.serviceId}
                    onChange={(e) => setForm({ ...form, serviceId: e.target.value })}
                    className="input"
                  >
                    <option value="">Selecciona…</option>
                    {specialistServices.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} ({s.minutes} min)
                      </option>
                    ))}
                  </select>
                </div>
                <div className="flex flex-col gap-1">
                  <label className="font-label-sm text-label-sm text-on-surface-variant">Consultorio</label>
                  <select
                    value={form.roomId}
                    onChange={(e) => setForm({ ...form, roomId: e.target.value })}
                    className="input"
                  >
                    <option value="">Sin asignar</option>
                    {rooms.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="flex flex-col gap-1">
                  <label className="font-label-sm text-label-sm text-on-surface-variant">Hora *</label>
                  <input
                    type="time"
                    value={form.time}
                    onChange={(e) => setForm({ ...form, time: e.target.value })}
                    className="input"
                  />
                </div>
              </div>

              <label className="flex items-center gap-space-xs font-body-sm text-body-sm text-on-surface mt-space-xs">
                <input
                  type="checkbox"
                  checked={form.isSeries}
                  onChange={(e) => setForm({ ...form, isSeries: e.target.checked })}
                />
                Repetir como serie recurrente
              </label>

              {form.isSeries ? (
                <div className="grid grid-cols-2 gap-space-sm">
                  <div className="flex flex-col gap-1">
                    <label className="font-label-sm text-label-sm text-on-surface-variant">Frecuencia</label>
                    <select
                      value={form.frequency}
                      onChange={(e) => setForm({ ...form, frequency: e.target.value })}
                      className="input"
                    >
                      <option value="semanal">Semanal</option>
                      <option value="quincenal">Quincenal</option>
                      <option value="mensual">Mensual</option>
                    </select>
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="font-label-sm text-label-sm text-on-surface-variant">Número de citas</label>
                    <input
                      type="number"
                      min={1}
                      max={52}
                      value={form.occurrences}
                      onChange={(e) => setForm({ ...form, occurrences: Number(e.target.value) })}
                      className="input"
                    />
                  </div>
                </div>
              ) : null}

              <div className="flex justify-end gap-space-sm mt-space-md">
                <button
                  type="button"
                  onClick={() => setFormOpen(false)}
                  className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
                >
                  Cerrar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
                >
                  {saving ? 'Guardando…' : form.isSeries ? 'Agendar serie' : 'Agendar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function ActionButton({
  label,
  onClick,
  busy,
  tone = 'default',
}: {
  label: string
  onClick: () => void
  busy: boolean
  tone?: 'default' | 'error'
}) {
  return (
    <button
      disabled={busy}
      onClick={onClick}
      className={`px-space-sm py-1 rounded-lg font-label-sm text-label-sm font-medium transition-colors disabled:opacity-50 ${
        tone === 'error'
          ? 'text-error hover:bg-error-container'
          : 'text-primary hover:bg-primary-fixed'
      }`}
    >
      {busy ? '…' : label}
    </button>
  )
}
