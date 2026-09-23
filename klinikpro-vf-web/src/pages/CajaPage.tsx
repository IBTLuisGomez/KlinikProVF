import { useEffect, useMemo, useState } from 'react'
import { api, ApiError } from '../lib/api'

interface CashSession {
  id: string
  cashier?: string | null
  openedDate: string
  openAmount: number
  status: string
}
interface ItemLine {
  concept: string
  qty: number
  unitPrice: number
}
interface PaymentLine {
  method: string
  amount: number
  transferFolio: string | null
}
interface Transaction {
  id: string
  folio: string
  date: string
  patientName?: string | null
  amount: number
  commission: number
  method: string
  createdAt: string
}
interface Expense {
  id: string
  folioOut: string
  concept: string
  supplier?: string | null
  date: string
  amount: number
  onCredit: boolean
}
interface CashCount {
  id: string
  date: string
  responsible?: string | null
  baseAmount: number
  counted: number
  difference: number
}
interface PatientLite {
  id: string
  code: string
  fullName: string
}

const money = (v: number) => v.toLocaleString('es-MX', { style: 'currency', currency: 'MXN' })
const today = () => new Date().toISOString().slice(0, 10)
const PAYMENT_METHODS = ['efectivo', 'debito', 'credito', 'transferencia']
const DENOMINATIONS = ['1000', '500', '200', '100', '50', '20', '10', '5', '2', '1', '0.5']

type Tab = 'cobros' | 'gastos' | 'arqueo'

export function CajaPage() {
  const [tab, setTab] = useState<Tab>('cobros')
  const [session, setSession] = useState<CashSession | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)
  const [openAmount, setOpenAmount] = useState('')
  const [busy, setBusy] = useState(false)

  async function loadSession() {
    try {
      const s = await api.get<CashSession | null>('/api/caja/sesiones/actual')
      setSession(s)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo consultar la sesión de caja')
    }
  }

  useEffect(() => {
    loadSession()
  }, [])

  async function openSession() {
    setBusy(true)
    setError(null)
    try {
      await api.post('/api/caja/sesiones/abrir', { openAmount: Number(openAmount || 0) })
      setOpenAmount('')
      await loadSession()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo abrir la caja')
    } finally {
      setBusy(false)
    }
  }

  async function closeSession() {
    if (!session) return
    const amount = window.prompt('Monto final contado para cerrar la caja:')
    if (amount === null) return
    setBusy(true)
    setError(null)
    try {
      await api.post(`/api/caja/sesiones/${session.id}/cerrar`, { closeAmount: Number(amount || 0) })
      await loadSession()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cerrar la caja')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-space-lg">
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-space-md bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
        <div>
          <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest">
            Gestión clínica
          </span>
          <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold tracking-tight mt-1">Caja</h1>
        </div>

        {session === undefined ? null : session && session.status === 'abierta' ? (
          <div className="flex items-center gap-space-md bg-secondary-fixed text-on-secondary-fixed-variant px-space-md py-space-sm rounded-lg">
            <span className="material-symbols-outlined text-[20px]">point_of_sale</span>
            <div className="flex flex-col">
              <span className="font-label-md text-label-md font-semibold">Caja abierta</span>
              <span className="font-label-sm text-label-sm">
                Apertura {money(session.openAmount)} · {session.openedDate}
              </span>
            </div>
            <button
              onClick={closeSession}
              disabled={busy}
              className="px-space-sm py-1.5 rounded-lg bg-on-secondary-fixed-variant text-secondary-fixed font-label-sm text-label-sm font-semibold disabled:opacity-60"
            >
              Cerrar caja
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-space-sm">
            <input
              type="number"
              placeholder="Monto de apertura"
              value={openAmount}
              onChange={(e) => setOpenAmount(e.target.value)}
              className="input w-48"
            />
            <button
              onClick={openSession}
              disabled={busy}
              className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
            >
              Abrir caja
            </button>
          </div>
        )}
      </div>

      {error ? (
        <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm">
          {error}
        </div>
      ) : null}

      <div className="flex gap-space-xs bg-surface-container-lowest rounded-xl shadow-sm p-1 w-fit">
        {(['cobros', 'gastos', 'arqueo'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-space-md py-2 rounded-lg font-label-md text-label-md font-medium capitalize transition-colors ${
              tab === t ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-surface-container-low'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === 'cobros' ? (
        <CobrosTab hasOpenSession={!!session && session.status === 'abierta'} onError={setError} />
      ) : tab === 'gastos' ? (
        <GastosTab onError={setError} />
      ) : (
        <ArqueoTab onError={setError} />
      )}
    </div>
  )
}

// ============================== COBROS ==============================

function CobrosTab({ hasOpenSession, onError }: { hasOpenSession: boolean; onError: (m: string | null) => void }) {
  const [date, setDate] = useState(today())
  const [rows, setRows] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)

  async function load(d: string) {
    setLoading(true)
    try {
      const data = await api.get<Transaction[]>(`/api/caja/transacciones?date=${d}`)
      setRows(data)
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar los cobros')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(date)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date])

  return (
    <div className="flex flex-col gap-space-md">
      <div className="flex items-center justify-between gap-space-md flex-wrap">
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="input w-48" />
        <button
          disabled={!hasOpenSession}
          title={hasOpenSession ? undefined : 'Abre la caja primero'}
          onClick={() => setFormOpen(true)}
          className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-50"
        >
          <span className="material-symbols-outlined text-[18px]">add_shopping_cart</span>
          Nuevo cobro
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-outline-variant/40">
              {['Folio', 'Paciente', 'Método', 'Comisión', 'Total', 'Hora'].map((h) => (
                <th key={h} className="px-space-md py-space-sm font-label-sm text-label-sm text-on-surface-variant">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((t) => (
              <tr key={t.id} className="border-b border-outline-variant/20 last:border-0">
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">{t.folio}</td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">
                  {t.patientName || '—'}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface capitalize">
                  {t.method}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {money(t.commission)}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface font-semibold">
                  {money(t.amount)}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {new Date(t.createdAt).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? (
          <p className="font-body-sm text-body-sm text-on-surface-variant px-space-md py-space-lg">
            Sin cobros registrados en esta fecha.
          </p>
        ) : null}
      </div>

      {formOpen ? (
        <NuevoCobroModal
          onClose={() => setFormOpen(false)}
          onSaved={() => {
            setFormOpen(false)
            load(date)
          }}
        />
      ) : null}
    </div>
  )
}

function NuevoCobroModal({
  onClose,
  onSaved,
}: {
  onClose: () => void
  onSaved: () => void
}) {
  const [patients, setPatients] = useState<PatientLite[]>([])
  const [patientQuery, setPatientQuery] = useState('')
  const [patientId, setPatientId] = useState('')
  const [patientLabel, setPatientLabel] = useState('')
  const [newPatientName, setNewPatientName] = useState('')
  const [items, setItems] = useState<ItemLine[]>([{ concept: '', qty: 1, unitPrice: 0 }])
  const [payments, setPayments] = useState<PaymentLine[]>([{ method: 'efectivo', amount: 0, transferFolio: null }])
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<PatientLite[]>('/api/patients')
      .then(setPatients)
      .catch(() => undefined)
  }, [])

  const subtotal = useMemo(() => items.reduce((s, i) => s + i.qty * i.unitPrice, 0), [items])
  const paymentsTotal = useMemo(() => payments.reduce((s, p) => s + (p.amount || 0), 0), [payments])
  const matches = useMemo(() => {
    if (patientQuery.trim().length < 2) return []
    const q = patientQuery.toLowerCase()
    return patients.filter((p) => p.fullName.toLowerCase().includes(q) || p.code.includes(q)).slice(0, 6)
  }, [patientQuery, patients])

  function updateItem(i: number, patch: Partial<ItemLine>) {
    setItems(items.map((it, idx) => (idx === i ? { ...it, ...patch } : it)))
  }
  function updatePayment(i: number, patch: Partial<PaymentLine>) {
    setPayments(payments.map((p, idx) => (idx === i ? { ...p, ...patch } : p)))
  }

  async function submit() {
    setFormError(null)
    if (items.some((i) => !i.concept || i.unitPrice < 0 || i.qty <= 0)) {
      setFormError('Revisa los conceptos: cantidad y precio deben ser válidos')
      return
    }
    setSaving(true)
    try {
      await api.post('/api/caja/transacciones', {
        patientId: patientId || null,
        newPatientName: !patientId && newPatientName ? newPatientName : null,
        items,
        payments,
        treatmentId: null,
        sessionsCovered: 0,
        notes: notes || null,
        date: null,
        commissionRateOverride: null,
      })
      onSaved()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo registrar el cobro')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
      <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-xl max-h-[90vh] overflow-y-auto">
        <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">Nuevo cobro</h3>

        {formError ? (
          <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
            {formError}
          </div>
        ) : null}

        <div className="flex flex-col gap-space-sm">
          <div className="flex flex-col gap-1 relative">
            <label className="font-label-sm text-label-sm text-on-surface-variant">Paciente (opcional)</label>
            <input
              value={patientId ? patientLabel : patientQuery}
              onChange={(e) => {
                setPatientId('')
                setPatientLabel('')
                setPatientQuery(e.target.value)
              }}
              placeholder="Buscar paciente, o deja vacío para cobro sin paciente…"
              className="input"
            />
            {!patientId && matches.length > 0 ? (
              <div className="absolute top-full mt-1 w-full bg-surface-container-lowest rounded-lg shadow-lg z-10 max-h-40 overflow-y-auto">
                {matches.map((p) => (
                  <button
                    type="button"
                    key={p.id}
                    onClick={() => {
                      setPatientId(p.id)
                      setPatientLabel(p.fullName)
                      setPatientQuery('')
                    }}
                    className="w-full text-left px-space-sm py-space-xs hover:bg-surface-container-low font-body-sm text-body-sm text-on-surface"
                  >
                    {p.fullName} <span className="text-on-surface-variant">· HC-{p.code}</span>
                  </button>
                ))}
              </div>
            ) : null}
            {!patientId && patientQuery.trim().length >= 2 && matches.length === 0 ? (
              <input
                value={newPatientName}
                onChange={(e) => setNewPatientName(e.target.value)}
                placeholder="No existe: nombre para alta automática (opcional)"
                className="input mt-1"
              />
            ) : null}
          </div>

          <div className="flex flex-col gap-space-xs">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm text-on-surface-variant">Conceptos</span>
              <button
                type="button"
                onClick={() => setItems([...items, { concept: '', qty: 1, unitPrice: 0 }])}
                className="font-label-sm text-label-sm text-primary"
              >
                + Agregar
              </button>
            </div>
            {items.map((it, i) => (
              <div key={i} className="grid grid-cols-[1fr_60px_100px_28px] gap-space-xs items-center">
                <input
                  value={it.concept}
                  onChange={(e) => updateItem(i, { concept: e.target.value })}
                  placeholder="Concepto"
                  className="input"
                />
                <input
                  type="number"
                  min={1}
                  value={it.qty}
                  onChange={(e) => updateItem(i, { qty: Number(e.target.value) })}
                  className="input"
                />
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  value={it.unitPrice}
                  onChange={(e) => updateItem(i, { unitPrice: Number(e.target.value) })}
                  className="input"
                />
                <button
                  type="button"
                  onClick={() => setItems(items.filter((_, idx) => idx !== i))}
                  disabled={items.length === 1}
                  className="text-on-surface-variant disabled:opacity-30"
                >
                  <span className="material-symbols-outlined text-[18px]">close</span>
                </button>
              </div>
            ))}
            <div className="text-right font-label-md text-label-md text-on-surface font-semibold">
              Subtotal: {money(subtotal)}
            </div>
          </div>

          <div className="flex flex-col gap-space-xs">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm text-on-surface-variant">Pagos (deben sumar el subtotal)</span>
              <button
                type="button"
                onClick={() => setPayments([...payments, { method: 'efectivo', amount: 0, transferFolio: null }])}
                className="font-label-sm text-label-sm text-primary"
              >
                + Agregar
              </button>
            </div>
            {payments.map((p, i) => (
              <div key={i} className="flex flex-col gap-space-xs">
                <div className="grid grid-cols-[120px_100px_1fr_28px] gap-space-xs items-center">
                  <select
                    value={p.method}
                    onChange={(e) => updatePayment(i, { method: e.target.value })}
                    className="input"
                  >
                    {PAYMENT_METHODS.map((m) => (
                      <option key={m} value={m}>
                        {m}
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    value={p.amount}
                    onChange={(e) => updatePayment(i, { amount: Number(e.target.value) })}
                    className="input"
                  />
                  {p.method === 'transferencia' ? (
                    <input
                      value={p.transferFolio ?? ''}
                      onChange={(e) => updatePayment(i, { transferFolio: e.target.value || null })}
                      placeholder="Folio bancario"
                      className="input"
                    />
                  ) : (
                    <span />
                  )}
                  <button
                    type="button"
                    onClick={() => setPayments(payments.filter((_, idx) => idx !== i))}
                    disabled={payments.length === 1}
                    className="text-on-surface-variant disabled:opacity-30"
                  >
                    <span className="material-symbols-outlined text-[18px]">close</span>
                  </button>
                </div>
              </div>
            ))}
            <div
              className={`text-right font-label-md text-label-md font-semibold ${
                Math.abs(paymentsTotal - subtotal) > 0.005 ? 'text-error' : 'text-on-surface'
              }`}
            >
              Pagos: {money(paymentsTotal)}
            </div>
          </div>

          <div className="flex flex-col gap-1">
            <label className="font-label-sm text-label-sm text-on-surface-variant">Notas</label>
            <input value={notes} onChange={(e) => setNotes(e.target.value)} className="input" />
          </div>

          <div className="flex justify-end gap-space-sm mt-space-sm">
            <button
              type="button"
              onClick={onClose}
              className="px-space-md py-2 rounded-lg font-label-md text-label-md text-on-surface-variant hover:bg-surface-container-low transition-colors"
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={submit}
              disabled={saving}
              className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all disabled:opacity-60"
            >
              {saving ? 'Guardando…' : 'Cobrar'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

// ============================== GASTOS ==============================

function GastosTab({ onError }: { onError: (m: string | null) => void }) {
  const [date, setDate] = useState(today())
  const [rows, setRows] = useState<Expense[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({
    ticketFolio: '',
    invoiceFolio: '',
    concept: '',
    supplier: '',
    amount: '',
    onCredit: false,
    dueDate: '',
  })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load(d: string) {
    setLoading(true)
    try {
      setRows(await api.get<Expense[]>(`/api/caja/gastos?date=${d}`))
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar los gastos')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(date)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date])

  async function submit() {
    if (!form.concept || !form.amount) {
      setFormError('Concepto y monto son obligatorios')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      await api.post('/api/caja/gastos', {
        ticketFolio: form.ticketFolio || null,
        invoiceFolio: form.invoiceFolio || null,
        concept: form.concept,
        supplier: form.supplier || null,
        date: null,
        amount: Number(form.amount),
        onCredit: form.onCredit,
        dueDate: form.onCredit && form.dueDate ? form.dueDate : null,
      })
      setFormOpen(false)
      setForm({ ticketFolio: '', invoiceFolio: '', concept: '', supplier: '', amount: '', onCredit: false, dueDate: '' })
      await load(date)
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo registrar el gasto')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <div className="flex items-center justify-between gap-space-md flex-wrap">
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="input w-48" />
        <button
          onClick={() => setFormOpen(true)}
          className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
        >
          <span className="material-symbols-outlined text-[18px]">receipt_long</span>
          Nuevo gasto
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-outline-variant/40">
              {['Folio', 'Concepto', 'Proveedor', 'Monto', 'Tipo'].map((h) => (
                <th key={h} className="px-space-md py-space-sm font-label-sm text-label-sm text-on-surface-variant">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((e) => (
              <tr key={e.id} className="border-b border-outline-variant/20 last:border-0">
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">{e.folioOut}</td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">{e.concept}</td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {e.supplier || '—'}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface font-semibold">
                  {money(e.amount)}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {e.onCredit ? 'A crédito' : 'Contado'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? (
          <p className="font-body-sm text-body-sm text-on-surface-variant px-space-md py-space-lg">
            Sin gastos registrados en esta fecha.
          </p>
        ) : null}
      </div>

      {formOpen ? (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
          <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-md">
            <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">Nuevo gasto</h3>
            {formError ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
                {formError}
              </div>
            ) : null}
            <div className="flex flex-col gap-space-sm">
              <input
                placeholder="Concepto *"
                value={form.concept}
                onChange={(e) => setForm({ ...form, concept: e.target.value })}
                className="input"
              />
              <input
                placeholder="Proveedor"
                value={form.supplier}
                onChange={(e) => setForm({ ...form, supplier: e.target.value })}
                className="input"
              />
              <div className="grid grid-cols-2 gap-space-sm">
                <input
                  placeholder="Folio de ticket"
                  value={form.ticketFolio}
                  onChange={(e) => setForm({ ...form, ticketFolio: e.target.value })}
                  className="input"
                />
                <input
                  placeholder="Folio de factura"
                  value={form.invoiceFolio}
                  onChange={(e) => setForm({ ...form, invoiceFolio: e.target.value })}
                  className="input"
                />
              </div>
              <input
                type="number"
                min={0}
                step="0.01"
                placeholder="Monto *"
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                className="input"
              />
              <label className="flex items-center gap-space-xs font-body-sm text-body-sm text-on-surface">
                <input
                  type="checkbox"
                  checked={form.onCredit}
                  onChange={(e) => setForm({ ...form, onCredit: e.target.checked })}
                />
                A crédito (genera una CxP)
              </label>
              {form.onCredit ? (
                <input
                  type="date"
                  value={form.dueDate}
                  onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                  className="input"
                />
              ) : null}
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
                  {saving ? 'Guardando…' : 'Registrar'}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

// ============================== ARQUEO ==============================

function ArqueoTab({ onError }: { onError: (m: string | null) => void }) {
  const [rows, setRows] = useState<CashCount[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [responsible, setResponsible] = useState('')
  const [denominations, setDenominations] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    try {
      setRows(await api.get<CashCount[]>('/api/caja/arqueos'))
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar los arqueos')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const total = useMemo(
    () => DENOMINATIONS.reduce((sum, d) => sum + Number(d) * Number(denominations[d] || 0), 0),
    [denominations],
  )

  async function submit() {
    setSaving(true)
    setFormError(null)
    try {
      const denoms = Object.fromEntries(
        Object.entries(denominations)
          .filter(([, v]) => Number(v) > 0)
          .map(([k, v]) => [k, Number(v)]),
      )
      await api.post('/api/caja/arqueos', {
        counted: total > 0 ? total : null,
        responsible: responsible || null,
        denominations: Object.keys(denoms).length > 0 ? denoms : null,
      })
      setFormOpen(false)
      setResponsible('')
      setDenominations({})
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo registrar el arqueo')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <div className="flex justify-end">
        <button
          onClick={() => setFormOpen(true)}
          className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
        >
          <span className="material-symbols-outlined text-[18px]">calculate</span>
          Nuevo arqueo
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-outline-variant/40">
              {['Fecha', 'Responsable', 'Base', 'Contado', 'Diferencia'].map((h) => (
                <th key={h} className="px-space-md py-space-sm font-label-sm text-label-sm text-on-surface-variant">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((c) => (
              <tr key={c.id} className="border-b border-outline-variant/20 last:border-0">
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">{c.date}</td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {c.responsible || '—'}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {money(c.baseAmount)}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface font-semibold">
                  {money(c.counted)}
                </td>
                <td
                  className={`px-space-md py-space-sm font-body-sm text-body-sm font-semibold ${
                    Math.abs(c.difference) < 0.005
                      ? 'text-on-surface-variant'
                      : c.difference < 0
                        ? 'text-error'
                        : 'text-on-surface'
                  }`}
                >
                  {money(c.difference)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? (
          <p className="font-body-sm text-body-sm text-on-surface-variant px-space-md py-space-lg">
            Sin arqueos registrados todavía.
          </p>
        ) : null}
      </div>

      {formOpen ? (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
          <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-md max-h-[90vh] overflow-y-auto">
            <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">Nuevo arqueo</h3>
            {formError ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
                {formError}
              </div>
            ) : null}
            <div className="flex flex-col gap-space-sm">
              <input
                placeholder="Responsable"
                value={responsible}
                onChange={(e) => setResponsible(e.target.value)}
                className="input"
              />
              <span className="font-label-sm text-label-sm text-on-surface-variant mt-space-xs">
                Desglose de billetes y monedas
              </span>
              <div className="grid grid-cols-3 gap-space-xs">
                {DENOMINATIONS.map((d) => (
                  <div key={d} className="flex flex-col gap-1">
                    <label className="font-label-sm text-label-sm text-on-surface-variant">${d}</label>
                    <input
                      type="number"
                      min={0}
                      value={denominations[d] ?? ''}
                      onChange={(e) => setDenominations({ ...denominations, [d]: e.target.value })}
                      className="input"
                    />
                  </div>
                ))}
              </div>
              <div className="text-right font-label-md text-label-md text-on-surface font-semibold">
                Total contado: {money(total)}
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
                  {saving ? 'Guardando…' : 'Registrar arqueo'}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
