# klinikpro-vf-web

Frontend (SPA) de KlinikPro — Fase 5 del plan maestro. React + Vite + TypeScript + Tailwind,
diseño tomado 1:1 de `stitch_klinikpro_app_redesign/clinical_precision/DESIGN.md`.

## Requisitos

- Node.js 18+ (recomendado 20 LTS)
- El backend (`klinikpro-vf/`) corriendo en `http://localhost:8081` (o ajusta `VITE_API_BASE_URL`)

## Arrancar en desarrollo

```bash
cd klinikpro-vf-web
npm install
cp .env.example .env   # ajusta VITE_API_BASE_URL si tu backend no está en :8081
npm run dev
```

Abre `http://localhost:5173` — ese origen ya está en la whitelist de CORS del backend
(`app.cors.allowed-origins` en `application.yml`).

## Estado de esta sub-entrega (2026-09-22)

Completo: setup del proyecto, design system (`tailwind.config.js` desde `DESIGN.md`), cliente
HTTP con interceptor de JWT (`src/lib/api.ts` — adjunta el token, redirige a `/login` en 401),
`AuthContext` + `ProtectedRoute` (espejo de roles en UI, la seguridad real vive en el backend),
`AppShell` (sidebar + header, navegación completa a las 8 páginas), **Login** y **Dashboard**
(wireado a `GET /api/reportes/dashboard`, con selector de rango de fechas).

Pendiente (próxima sub-entrega): las 6 páginas restantes están en el routing como "Próximamente"
(`PlaceholderPage`) — Pacientes, Agenda (la más grande: calendario + CU-01..CU-09), Caja,
Finanzas, Configuración/Catálogos, Facturación (esta última sin backend propio todavía — no
estaba en el alcance original). También falta: versión móvil responsive, y agregar el frontend
a `docker-compose.yml`.

## Decisión de contenido (no solo de código)

El mockup de login original (`klinikpro_inicio_de_sesi_n_cl_nico/code.html`) traía un selector
de "rol clínico" que no corresponde a ningún campo real (el rol lo asigna el backend según la
cuenta, no se elige al iniciar sesión), opciones de 2FA/Smart Card/biometría que el backend no
implementa, y certificaciones/cifras inventadas (HIPAA, ISO 27001, "142 camas UCI activas",
99.98% disponibilidad). Se adaptó el diseño visual (colores, tipografía, layout) pero se quitó
ese contenido — no correspondía a lo que el producto realmente hace ni es cierto.
