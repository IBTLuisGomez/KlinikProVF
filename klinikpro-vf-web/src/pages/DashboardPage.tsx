import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import { KpiCard } from '../components/KpiCard'

interface DashboardReport {
  desde: string
  hasta: string
  ingresos: number
  gastos: number
  utilidad: number
  margenPorcentaje: number
  totalCitas: number
  citasPorEstado: Record<string, number>
  pacientesNuevos: number
  cxcPendiente: number
  cxpPendiente: number
}

const money = (v: number) => v.toLocaleString('es-MX', { style: 'currency', currency: 'MXN' })

export function DashboardPage() {
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [report, setReport] = useState<DashboardReport | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load(params?: { desde?: string; hasta?: string }) {
    setLoading(true)
    setError(null)
    try {
      const qs = new URLSearchParams()
      if (params?.desde) qs.set('desde', params.desde)
      if (params?.hasta) qs.set('hasta', params.hasta)
      const query = qs.toString()
      const data = await api.get<DashboardReport>(`/api/reportes/dashboard${query ? `?${query}` : ''}`)
      setReport(data)
      setDesde(data.desde)
      setHasta(data.hasta)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cargar el dashboard')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="flex flex-col gap-space-lg">
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-space-md bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
        <div>
          <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest">
            Panel general
          </span>
          <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold tracking-tight mt-1">
            Dashboard
          </h1>
        </div>
        <form
          className="flex flex-wrap items-end gap-space-sm"
          onSubmit={(e) => {
            e.preventDefault()
            load({ desde, hasta })
          }}
        >
          <div className="flex flex-col">
            <label className="font-label-sm text-label-sm text-on-surface-variant mb-1">Desde</label>
            <input
              type="date"
              value={desde}
              onChange={(e) => setDesde(e.target.value)}
              className="px-space-sm py-1.5 rounded-lg bg-surface-container-low text-on-surface font-body-sm text-body-sm focus:outline-none"
            />
          </div>
          <div className="flex flex-col">
            <label className="font-label-sm text-label-sm text-on-surface-variant mb-1">Hasta</label>
            <input
              type="date"
              value={hasta}
              onChange={(e) => setHasta(e.target.value)}
              className="px-space-sm py-1.5 rounded-lg bg-surface-container-low text-on-surface font-body-sm text-body-sm focus:outline-none"
            />
          </div>
          <button
            type="submit"
            className="px-space-md py-2 rounded-lg bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-sm hover:shadow-md transition-all"
          >
            Aplicar
          </button>
        </form>
      </div>

      {error ? (
        <div className="rounded-lg bg-error-container text-on-error-container px-4 py-3 font-body-sm text-body-sm">
          {error}
        </div>
      ) : null}

      {loading || !report ? (
        <div className="font-body-md text-body-md text-on-surface-variant">Cargando…</div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-space-md">
            <KpiCard label="Ingresos" value={money(report.ingresos)} icon="payments" tone="secondary" />
            <KpiCard label="Gastos" value={money(report.gastos)} icon="shopping_cart" tone="error" />
            <KpiCard label="Utilidad" value={money(report.utilidad)} icon="trending_up" tone="primary" />
            <KpiCard label="Margen" value={`${report.margenPorcentaje}%`} icon="percent" tone="secondary" />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-space-md">
            <KpiCard label="Citas en el periodo" value={String(report.totalCitas)} icon="calendar_today" />
            <KpiCard label="Pacientes nuevos" value={String(report.pacientesNuevos)} icon="group_add" />
            <KpiCard label="CxC pendiente" value={money(report.cxcPendiente)} icon="request_quote" tone="secondary" />
            <KpiCard label="CxP pendiente" value={money(report.cxpPendiente)} icon="request_quote" tone="error" />
          </div>

          <div className="bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
            <h2 className="font-headline-sm text-headline-sm text-on-surface font-semibold mb-space-md">
              Citas por estado
            </h2>
            {Object.keys(report.citasPorEstado).length === 0 ? (
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Sin citas registradas en este periodo.
              </p>
            ) : (
              <div className="flex flex-wrap gap-space-sm">
                {Object.entries(report.citasPorEstado).map(([status, count]) => (
                  <span
                    key={status}
                    className="inline-flex items-center gap-space-xs px-space-md py-1.5 rounded-full bg-surface-container-low font-label-md text-label-md text-on-surface"
                  >
                    <span className="font-semibold">{count}</span> {status}
                  </span>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
