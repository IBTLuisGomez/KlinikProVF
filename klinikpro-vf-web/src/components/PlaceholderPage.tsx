interface PlaceholderPageProps {
  title: string
  icon: string
  description: string
}

/**
 * Página "próximamente" para las secciones de Fase 5 que todavía no se
 * construyen en esta sub-entrega (setup + design system + Login + Dashboard).
 * Ya están en el routing/menú para que la navegación completa se pueda probar
 * desde ya; el contenido real de cada una llega en la siguiente sub-entrega.
 */
export function PlaceholderPage({ title, icon, description }: PlaceholderPageProps) {
  return (
    <div className="flex flex-col items-center justify-center text-center bg-surface-container-lowest rounded-xl shadow-sm p-space-xl min-h-[60vh]">
      <span className="material-symbols-outlined text-primary text-[48px] mb-space-md">{icon}</span>
      <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold mb-space-sm">{title}</h1>
      <p className="font-body-md text-body-md text-on-surface-variant max-w-md">{description}</p>
      <span className="mt-space-md inline-flex items-center px-space-md py-1.5 rounded-full bg-surface-container-low font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
        Próximamente
      </span>
    </div>
  )
}
