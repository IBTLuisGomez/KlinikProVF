interface KpiCardProps {
  label: string
  value: string
  suffix?: string
  icon: string
  tone?: 'primary' | 'secondary' | 'error'
}

const TONE_CLASSES: Record<NonNullable<KpiCardProps['tone']>, string> = {
  primary: 'bg-primary-fixed text-primary',
  secondary: 'bg-secondary-fixed text-on-secondary-fixed-variant',
  error: 'bg-error-container text-on-error-container',
}

export function KpiCard({ label, value, suffix, icon, tone = 'primary' }: KpiCardProps) {
  return (
    <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col justify-between hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between mb-space-xs">
        <span className="font-label-md text-label-md text-on-surface-variant font-medium">{label}</span>
        <span className={`p-space-xs rounded-lg flex items-center justify-center ${TONE_CLASSES[tone]}`}>
          <span className="material-symbols-outlined text-[18px]">{icon}</span>
        </span>
      </div>
      <div className="flex items-baseline gap-space-xs">
        <span className="font-headline-lg text-headline-lg text-on-surface font-bold">{value}</span>
        {suffix ? <span className="font-label-md text-label-md text-on-surface-variant">{suffix}</span> : null}
      </div>
    </div>
  )
}
