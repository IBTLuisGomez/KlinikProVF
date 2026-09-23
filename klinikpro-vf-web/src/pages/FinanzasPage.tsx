import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../context/AuthContext'

interface Receivable {
  id: string
  client: string
  concept: string
  amount: number
  dueDate?: string | null
  status: string
}
interface Payable {
  id: string
  supplier: string
  concept: string
  amount: number
  dueDate?: string | null
  status: string
  recurring: boolean
  frequency?: string | null
  reminderAt?: string | null
}
interface BankMovement {
  id: string
  date: string
  concept: string
  kind: string
  amount: number
  reconciled: boolean
}

const money = (v: number) => v.toLocaleString('es-MX', { style: 'currency', currency: 'MXN' })
const PAYMENT_METHODS = ['efectivo', 'debito', 'credito', 'transferencia']

type Tab = 'cxc' | 'cxp' | 'banco'

export function FinanzasPage() {
  const { session } = useAuth()
  const canLead = session ? ['ADMIN', 'COORDINADOR'].includes(session.role) : false
  const canFrontDesk = session ? ['ADMIN', 'COORDINADOR', 'RECEPCION'].includes(session.role) : false
  const [tab, setTab] = useState<Tab>('cxc')
  const [error, setError] = useState<string | null>(null)

  return (
    <div className="flex flex-col gap-space-lg">
      <div className="bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
        <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest">
          Gestión clínica
        </span>
        <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold tracking-tight mt-1">Finanzas</h1>
      </div>

      {error ? (
        <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm">
          {error}
        </div>
      ) : null}

      <div className="flex gap-space-xs bg-surface-container-lowest rounded-xl shadow-sm p-1 w-fit">
        {(['cxc', 'cxp'] as Tab[])
          .concat(canLead ? ['banco'] : [])
          .map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-space-md py-2 rounded-lg font-label-md text-label-md font-medium uppercase transition-colors ${
                tab === t ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              {t}
            </button>
          ))}
      </div>

      {tab === 'cxc' ? (
        <CxcTab canManage={canFrontDesk} canLead={canLead} onError={setError} />
      ) : tab === 'cxp' ? (
        <CxpTab canManage={canFrontDesk} canLead={canLead} onError={setError} />
      ) : (
        <BancoTab onError={setError} />
      )}
    </div>
  )
}

// ============================== CxC ==============================

function CxcTab({
  canManage,
  canLead,
  onError,
}: {
  canManage: boolean
  canLead: boolean
  onError: (m: string | null) => void
}) {
  const [rows, setRows] = useState<Receivable[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({ client: '', concept: '', amount: '', dueDate: '' })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    try {
      setRows(await api.get<Receivable[]>('/api/cxc'))
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar las cuentas por cobrar')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function submit() {
    if (!form.client || !form.concept || !form.amount) {
      setFormError('Cliente, concepto y monto son obligatorios')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      await api.post('/api/cxc', {
        client: form.client,
        concept: form.concept,
        amount: Number(form.amount),
        dueDate: form.dueDate || null,
      })
      setFormOpen(false)
      setForm({ client: '', concept: '', amount: '', dueDate: '' })
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo crear la cuenta por cobrar')
    } finally {
      setSaving(false)
    }
  }

  async function pay(r: Receivable) {
    const method = window.prompt(`Método de pago (${PAYMENT_METHODS.join('|')}):`, 'efectivo')
    if (!method) return
    try {
      await api.post(`/api/cxc/${r.id}/pagar`, { method })
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo cobrar la cuenta')
    }
  }

  async function reverse(r: Receivable) {
    if (!window.confirm('¿Revertir este pago?')) return
    try {
      await api.post(`/api/cxc/${r.id}/revertir`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo revertir')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      {canManage ? (
        <div className="flex justify-end">
          <button
            onClick={() => setFormOpen(true)}
            className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
          >
            <span className="material-symbols-outlined text-[18px]">request_quote</span>
            Nueva CxC
          </button>
        </div>
      ) : null}

      <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-outline-variant/40">
              {['Cliente', 'Concepto', 'Monto', 'Vence', 'Estado', ''].map((h) => (
                <th key={h} className="px-space-md py-space-sm font-label-sm text-label-sm text-on-surface-variant">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id} className="border-b border-outline-variant/20 last:border-0">
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">{r.client}</td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {r.concept}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface font-semibold">
                  {money(r.amount)}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {r.dueDate || '—'}
                </td>
                <td className="px-space-md py-space-sm">
                  <span
                    className={`px-space-sm py-1 rounded-full font-label-sm text-label-sm ${
                      r.status === 'Pendiente'
                        ? 'bg-surface-container-low text-on-surface-variant'
                        : 'bg-secondary-fixed text-on-secondary-fixed-variant'
                    }`}
                  >
                    {r.status}
                  </span>
                </td>
                <td className="px-space-md py-space-sm">
                  {canManage && r.status === 'Pendiente' ? (
                    <button onClick={() => pay(r)} className="font-label-sm text-label-sm text-primary">
                      Cobrar
                    </button>
                  ) : canLead && r.status !== 'Pendiente' ? (
                    <button onClick={() => reverse(r)} className="font-label-sm text-label-sm text-error">
                      Revertir
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? (
          <p className="font-body-sm text-body-sm text-on-surface-variant px-space-md py-space-lg">
            Sin cuentas por cobrar registradas.
          </p>
        ) : null}
      </div>

      {formOpen ? (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
          <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-md">
            <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">Nueva CxC</h3>
            {formError ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
                {formError}
              </div>
            ) : null}
            <div className="flex flex-col gap-space-sm">
              <input
                placeholder="Cliente *"
                value={form.client}
                onChange={(e) => setForm({ ...form, client: e.target.value })}
                className="input"
              />
              <input
                placeholder="Concepto *"
                value={form.concept}
                onChange={(e) => setForm({ ...form, concept: e.target.value })}
                className="input"
              />
              <input
                type="number"
                min={0}
                step="0.01"
                placeholder="Monto *"
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                className="input"
              />
              <input
                type="date"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
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
          </div>
        </div>
      ) : null}
    </div>
  )
}

// ============================== CxP ==============================

function CxpTab({
  canManage,
  canLead,
  onError,
}: {
  canManage: boolean
  canLead: boolean
  onError: (m: string | null) => void
}) {
  const [rows, setRows] = useState<Payable[]>([])
  const [reminders, setReminders] = useState<Payable[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({
    supplier: '',
    concept: '',
    amount: '',
    dueDate: '',
    recurring: false,
    frequency: 'mensual',
  })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    try {
      const [all, due] = await Promise.all([
        api.get<Payable[]>('/api/cxp'),
        api.get<Payable[]>('/api/cxp/recordatorios?dias=7'),
      ])
      setRows(all)
      setReminders(due)
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar las cuentas por pagar')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function submit() {
    if (!form.supplier || !form.concept || !form.amount) {
      setFormError('Proveedor, concepto y monto son obligatorios')
      return
    }
    if (form.recurring && !form.frequency) {
      setFormError('Una CxP recurrente necesita frecuencia')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      await api.post('/api/cxp', {
        supplier: form.supplier,
        concept: form.concept,
        amount: Number(form.amount),
        dueDate: form.dueDate || null,
        recurring: form.recurring,
        frequency: form.recurring ? form.frequency : null,
      })
      setFormOpen(false)
      setForm({ supplier: '', concept: '', amount: '', dueDate: '', recurring: false, frequency: 'mensual' })
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo crear la cuenta por pagar')
    } finally {
      setSaving(false)
    }
  }

  async function pay(p: Payable) {
    if (!window.confirm(`¿Pagar ${money(p.amount)} a ${p.supplier}? Se registrará como gasto en Caja.`)) return
    try {
      await api.post(`/api/cxp/${p.id}/pagar`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo pagar la cuenta')
    }
  }

  async function reverse(p: Payable) {
    if (!window.confirm('¿Revertir este pago?')) return
    try {
      await api.post(`/api/cxp/${p.id}/revertir`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo revertir')
    }
  }

  async function markReminded(p: Payable) {
    try {
      await api.post(`/api/cxp/${p.id}/recordatorio-enviado`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo marcar el recordatorio')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      {reminders.length > 0 ? (
        <div className="rounded-xl bg-error-container text-on-error-container px-space-lg py-space-md">
          <div className="flex items-center gap-space-sm mb-space-sm font-label-md text-label-md font-semibold">
            <span className="material-symbols-outlined text-[20px]">notifications_active</span>
            {reminders.length} cuenta(s) por pagar vencidas o por vencer en 7 días
          </div>
          <div className="flex flex-col gap-space-xs">
            {reminders.map((r) => (
              <div key={r.id} className="flex items-center justify-between gap-space-sm font-body-sm text-body-sm">
                <span>
                  {r.supplier} — {r.concept} · {money(r.amount)} · vence {r.dueDate || '—'}
                </span>
                <button onClick={() => markReminded(r)} className="font-label-sm text-label-sm underline shrink-0">
                  Ya avisé
                </button>
              </div>
            ))}
          </div>
        </div>
      ) : null}

      {canManage ? (
        <div className="flex justify-end">
          <button
            onClick={() => setFormOpen(true)}
            className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
          >
            <span className="material-symbols-outlined text-[18px]">request_quote</span>
            Nueva CxP
          </button>
        </div>
      ) : null}

      <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-outline-variant/40">
              {['Proveedor', 'Concepto', 'Monto', 'Vence', 'Estado', ''].map((h) => (
                <th key={h} className="px-space-md py-space-sm font-label-sm text-label-sm text-on-surface-variant">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id} className="border-b border-outline-variant/20 last:border-0">
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">{p.supplier}</td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {p.concept}
                  {p.recurring ? ` (${p.frequency})` : ''}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface font-semibold">
                  {money(p.amount)}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {p.dueDate || '—'}
                </td>
                <td className="px-space-md py-space-sm">
                  <span
                    className={`px-space-sm py-1 rounded-full font-label-sm text-label-sm ${
                      p.status === 'Pendiente'
                        ? 'bg-surface-container-low text-on-surface-variant'
                        : 'bg-secondary-fixed text-on-secondary-fixed-variant'
                    }`}
                  >
                    {p.status}
                  </span>
                </td>
                <td className="px-space-md py-space-sm">
                  {canManage && p.status === 'Pendiente' ? (
                    <button onClick={() => pay(p)} className="font-label-sm text-label-sm text-primary">
                      Pagar
                    </button>
                  ) : canLead && p.status !== 'Pendiente' ? (
                    <button onClick={() => reverse(p)} className="font-label-sm text-label-sm text-error">
                      Revertir
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? (
          <p className="font-body-sm text-body-sm text-on-surface-variant px-space-md py-space-lg">
            Sin cuentas por pagar registradas.
          </p>
        ) : null}
      </div>

      {formOpen ? (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
          <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-md">
            <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">Nueva CxP</h3>
            {formError ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
                {formError}
              </div>
            ) : null}
            <div className="flex flex-col gap-space-sm">
              <input
                placeholder="Proveedor *"
                value={form.supplier}
                onChange={(e) => setForm({ ...form, supplier: e.target.value })}
                className="input"
              />
              <input
                placeholder="Concepto *"
                value={form.concept}
                onChange={(e) => setForm({ ...form, concept: e.target.value })}
                className="input"
              />
              <input
                type="number"
                min={0}
                step="0.01"
                placeholder="Monto *"
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                className="input"
              />
              <input
                type="date"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                className="input"
              />
              <label className="flex items-center gap-space-xs font-body-sm text-body-sm text-on-surface">
                <input
                  type="checkbox"
                  checked={form.recurring}
                  onChange={(e) => setForm({ ...form, recurring: e.target.checked })}
                />
                Recurrente
              </label>
              {form.recurring ? (
                <select
                  value={form.frequency}
                  onChange={(e) => setForm({ ...form, frequency: e.target.value })}
                  className="input"
                >
                  <option value="semanal">Semanal</option>
                  <option value="quincenal">Quincenal</option>
                  <option value="mensual">Mensual</option>
                  <option value="anual">Anual</option>
                </select>
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
                  {saving ? 'Guardando…' : 'Guardar'}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

// ============================== Banco ==============================

function BancoTab({ onError }: { onError: (m: string | null) => void }) {
  const [rows, setRows] = useState<BankMovement[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({ date: '', concept: '', kind: 'deposito', amount: '' })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    try {
      setRows(await api.get<BankMovement[]>('/api/banco'))
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudieron cargar los movimientos bancarios')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function submit() {
    if (!form.concept || !form.amount) {
      setFormError('Concepto y monto son obligatorios')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      await api.post('/api/banco', {
        date: form.date || null,
        concept: form.concept,
        kind: form.kind,
        amount: Number(form.amount),
      })
      setFormOpen(false)
      setForm({ date: '', concept: '', kind: 'deposito', amount: '' })
      await load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo registrar el movimiento')
    } finally {
      setSaving(false)
    }
  }

  async function reconcile(m: BankMovement) {
    try {
      await api.post(`/api/banco/${m.id}/conciliar`)
      await load()
    } catch (err) {
      onError(err instanceof ApiError ? err.message : 'No se pudo conciliar')
    }
  }

  return (
    <div className="flex flex-col gap-space-md">
      <div className="flex justify-end">
        <button
          onClick={() => setFormOpen(true)}
          className="flex items-center gap-space-xs px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
        >
          <span className="material-symbols-outlined text-[18px]">account_balance</span>
          Nuevo movimiento
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-outline-variant/40">
              {['Fecha', 'Concepto', 'Tipo', 'Monto', 'Conciliado', ''].map((h) => (
                <th key={h} className="px-space-md py-space-sm font-label-sm text-label-sm text-on-surface-variant">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((m) => (
              <tr key={m.id} className="border-b border-outline-variant/20 last:border-0">
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface">{m.date}</td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant">
                  {m.concept}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface-variant capitalize">
                  {m.kind}
                </td>
                <td className="px-space-md py-space-sm font-body-sm text-body-sm text-on-surface font-semibold">
                  {money(m.amount)}
                </td>
                <td className="px-space-md py-space-sm">
                  <span
                    className={`px-space-sm py-1 rounded-full font-label-sm text-label-sm ${
                      m.reconciled
                        ? 'bg-secondary-fixed text-on-secondary-fixed-variant'
                        : 'bg-surface-container-low text-on-surface-variant'
                    }`}
                  >
                    {m.reconciled ? 'Sí' : 'No'}
                  </span>
                </td>
                <td className="px-space-md py-space-sm">
                  {!m.reconciled ? (
                    <button onClick={() => reconcile(m)} className="font-label-sm text-label-sm text-primary">
                      Conciliar
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? (
          <p className="font-body-sm text-body-sm text-on-surface-variant px-space-md py-space-lg">
            Sin movimientos bancarios registrados.
          </p>
        ) : null}
      </div>

      {formOpen ? (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-space-md">
          <div className="bg-surface-container-lowest rounded-xl shadow-lg p-space-lg w-full max-w-md">
            <h3 className="font-headline-md text-headline-md text-on-surface font-bold mb-space-md">
              Nuevo movimiento bancario
            </h3>
            {formError ? (
              <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm mb-space-md">
                {formError}
              </div>
            ) : null}
            <div className="flex flex-col gap-space-sm">
              <input
                type="date"
                value={form.date}
                onChange={(e) => setForm({ ...form, date: e.target.value })}
                className="input"
              />
              <input
                placeholder="Concepto *"
                value={form.concept}
                onChange={(e) => setForm({ ...form, concept: e.target.value })}
                className="input"
              />
              <select value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value })} className="input">
                <option value="deposito">Depósito</option>
                <option value="cargo">Cargo</option>
                <option value="comision">Comisión bancaria</option>
              </select>
              <input
                type="number"
                min={0}
                step="0.01"
                placeholder="Monto *"
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
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
          </div>
        </div>
      ) : null}
    </div>
  )
}
