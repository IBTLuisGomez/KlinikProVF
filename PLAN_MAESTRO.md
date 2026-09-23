# PLAN MAESTRO — KlinikProVF

**Fecha:** 2026-09-22
**Autor del plan:** Claude (guía/copilot técnico), a partir de: código actual del repo, `LogicaAgenda.pdf`, `stitch_klinikpro_app_redesign/` y `ARQUITECTURA.md`/`HISTORIAL.md`.
**Objetivo:** llevar KlinikProVF de "fundaciones + pacientes" a producto completo (backend + frontend real) en fases verificables, cada una con un entregable que compila/corre y se puede probar.

---

## 0. Dónde estamos (punto de partida real)

**Hecho y verificado (`mvn clean compile` → BUILD SUCCESS):**
- Multitenant (`tenant_id`/`branch_id`) vía JWT stateless, roles `ADMIN/COORDINADOR/FISIO/RECEPCION`.
- Módulo Pacientes completo (CRUD, código autogenerado por sucursal, búsqueda) con autorización por rol (`@PreAuthorize` + `Roles.java`).
- Migraciones Flyway V1–V3 (tenancy, schema_core, auth). El schema_core (V2) **ya creó las tablas** de agenda/caja/finanzas pero con un modelo simplificado (heredado del prototipo HTML), que no alcanza para lo que pide `LogicaAgenda.pdf`.

**Deuda técnica abierta (del audit anterior, para no perderla de vista):**
- Secrets (`security.jwt.secret`, password de Postgres) hardcodeados en `application.yml` — mover a variables de entorno antes de producción.
- CORS permite el origin literal `"null"` (necesario mientras se abre HTML como `file://`; quitar cuando el frontend real sirva por HTTP).
- Sin tests de `AuthController`/`PatientService`.
- Borrado físico (hard delete) sin auditoría en Pacientes.

Estos puntos se resuelven en la **Fase 6 (Hardening)**, no bloquean el resto.

**Frontend existente:** `stitch_klinikpro_app_redesign/` son **mockups estáticos** (Tailwind vía CDN, sin JS de aplicación, sin llamadas a API) generados con Google Stitch — 9 pantallas desktop + 4 móviles, con un sistema de diseño documentado en `clinical_precision/DESIGN.md` ("Clinical Precision": paleta clínica azul/teal, tipografía Plus Jakarta Sans + Inter, radios y elevaciones definidos). Esto es **material de diseño**, no una app — hay que construir la SPA real usando estos mockups como spec visual.

---

## 1. Orden de fases

| Fase | Nombre | Entregable verificable |
|---|---|---|
| 1 | Agenda (backend) | `mvn clean compile` + Postman: crear/confirmar/cancelar/reprogramar una cita respetando todas las reglas de `LogicaAgenda.pdf` |
| 2 | Caja / POS (backend) | Abrir sesión de caja, cobrar con comisión, arqueo, cerrar caja — vía Postman |
| 3 | Finanzas + Tratamientos (backend) | CxC/CxP conectadas a Caja, ciclo de vida de tratamientos |
| 4 | Reportes (backend) | Endpoints de KPIs por sucursal/periodo |
| 5 | Frontend (SPA real) | App React corriendo en `localhost:5173`, login real, agenda funcional contra la API |
| 6 | Hardening / Producción | Checklist de `engineering:deploy-checklist` en verde, secrets fuera del repo |
| 7 | Comercialización (billing multi-tenant) | Fuera de alcance inmediato — se detalla al final solo como referencia |

Cada fase de backend (1–4) sigue el mismo patrón interno: **migración Flyway → entidades JPA → repos → servicios con las reglas de negocio → controllers con `@PreAuthorize` → tests**. Así cada fase es autocontenida y revisable como las de Pacientes.

---

## FASE 1 — Agenda (el módulo más grande; se divide en 3 sub-entregas)

### 1.1 Gap de modelo de datos (por qué hace falta una V4)

`LogicaAgenda.pdf` pide un modelo más rico que el V2 actual:

| Ya existe (V2) | Falta agregar |
|---|---|
| `specialists` (id, name, specialty texto) | `medicos` con `documento`, `color_agenda`; relación N:M real a especialidades (`medico_especialidad`) |
| `services` (id, name, minutes, price) | `especialidad_id` en servicios, `buffer_minutos`, relación N:M médico↔servicio (`medico_servicio`) |
| `appointments` (patient_id, service_id, attended_by texto, date, time, status) | `medico_id` (FK real, no texto), `consultorio_id`, `cita_origen_id`/`cita_nueva_id` (reprogramación), `motivo_cancelacion`, `cargo_cancelacion`, timestamps `confirmado_en/cancelado_en/iniciado_en/finalizado_en` |
| — (no existe) | `consultorios`, `horarios_medico` (día+hora por médico), `bloqueos` (vacaciones/reuniones), `lista_espera`, `auditoria_cita` |

**Decisión propuesta:** migración `V4__agenda.sql` que crea las tablas nuevas y **altera** `appointments` (renombrar mentalmente a "citas" en el dominio, aunque la tabla física puede conservar el nombre para no romper V2) agregando las columnas que faltan. No se borra nada de V2 — solo se extiende.

### 1.2 Máquina de estados (tal cual el PDF, sin adaptarla)

```
PROGRAMADA → CONFIRMADA → EN_ESPERA → EN_ATENCION → FINALIZADA
                                            ↓
                    CANCELADA / REPROGRAMADA / NO_ASISTIO / EXPIRADA
```
Transiciones permitidas exactamente como la tabla del PDF (sección "Transiciones permitidas"). Esto se implementa como una validación explícita en `CitaService` (no dejar que cualquier estado salte a cualquier estado).

### 1.3 Reglas de negocio a portar 1:1 desde el pseudocódigo del PDF

- `validarDisponibilidad(medicoId, consultorioId, fechaHoraInicio, duracionMinutos, excluirCitaId)`:
  horario activo ese día → sin bloqueo → sin solape con otra cita del médico → sin solape de consultorio (si aplica) → dentro de anticipación mín/máx.
- `crearCita(...)`: valida servicio activo, calcula fin = inicio + duración + buffer, corre `validarDisponibilidad`, valida que el médico ofrezca ese servicio, crea en estado `PROGRAMADA` (o `CONFIRMADA` si la config lo permite), deja hooks para notificación/recordatorio (fuera de alcance en esta fase — se deja el método `enviarConfirmacion` como no-op/log, WhatsApp/SMS es fase posterior).
- `cancelarCita(...)`: valida estado cancelable, valida plazo mínimo si cancela el paciente (política de cargo por cancelación tardía configurable), libera horario, notifica lista de espera (evento interno, no canal externo todavía).
- `reprogramarCita(...)`: reutiliza `crearCita`, cierra la original como `REPROGRAMADA` con `cita_nueva_id`.
- CU-05 a CU-09: registrar llegada (`EN_ESPERA`), iniciar/finalizar atención (`EN_ATENCION`→`FINALIZADA`), gestión de lista de espera, registrar no-show (manual + job automático), gestión de horarios/bloqueos.
- Job `@Scheduled` (cada 5–10 min) que recorre citas `CONFIRMADA`/`PROGRAMADA` vencidas + tolerancia → `NO_ASISTIO` automático (CU-08).

### 1.4 Endpoints (todos con `@PreAuthorize` usando `Roles.java` ya creado)

```
/api/medicos            CRUD           LEADERSHIP (alta/baja) · ANY (lectura)
/api/especialidades     CRUD           LEADERSHIP
/api/servicios          CRUD           LEADERSHIP · ANY (lectura)
/api/consultorios       CRUD           LEADERSHIP
/api/horarios-medico    CRUD           LEADERSHIP
/api/bloqueos           CRUD           LEADERSHIP
/api/citas              CRUD + acciones: /confirmar /cancelar /reprogramar
                         /llegada /iniciar-atencion /finalizar-atencion   FRONT_DESK (crear/editar) · ANY (lectura) · Médico solo sus propias (iniciar/finalizar)
/api/lista-espera       CRUD           FRONT_DESK
```

### 1.5 Tests (con el mismo patrón que `JwtServiceTest`, en `src/test`)

- Unitarios de `validarDisponibilidad`: caso feliz, solape de médico, solape de consultorio, fuera de horario, bloqueo activo, anticipación mín/máx.
- Unitarios de transición de estados (cada transición inválida debe lanzar error).
- Integración (`@SpringBootTest` + H2, perfil `test` que ya existe): flujo completo CU-01 → CU-06.

**Nota de alcance:** las reglas "particularidades por especialidad" (tablas 4.1–4.3 del PDF: duraciones recomendadas, overbooking, tolerancia) se implementan como **valores configurables por servicio/tenant**, no hardcodeados — el PDF los da como recomendación, no como regla dura.

---

## FASE 2 — Caja / POS

Usa las tablas V2 ya creadas (`cash_sessions`, `transactions`, `expenses`, `cash_counts`) — no requiere migración nueva salvo ajustes menores.

- `CashSessionService`: apertura (valida que no haya sesión abierta previa del día; si la hay, exige cerrarla primero — regla ya documentada en memoria del prototipo), cierre.
- `TransactionService`: folio automático `AÑO-MES3-NNN`, cobro con `items[]`/`payments[]` (mixto efectivo/débito/crédito/transferencia), comisión de tarjeta que **se suma** al total (la absorbe el cliente — regla explícita del prototipo v2), vínculo opcional a `treatments` (sesiones cubiertas).
- `ExpenseService`: folio de salida interno + folio ticket/factura proveedor, gasto "a crédito" → crea CxP (enlace con Fase 3).
- `CashCountService`: arqueo con diferencia calculada.
- Endpoints bajo `FRONT_DESK` para operar, `LEADERSHIP` para reabrir/forzar cierre.

---

## FASE 3 — Finanzas + Tratamientos

- `ReceivableService`/`PayableService`: pagar una CxC crea un ingreso en Caja (`origenCxc`), pagar una CxP crea un gasto (`origenCxp`); ambos reversibles. CxP recurrente genera la siguiente ocurrencia al pagar.
- `TreatmentService`: ciclo `activo → en_revision/por_cobrar → pendiente_cierre → finalizado`, disparado por sesiones usadas/pagadas (regla ya documentada: `usadas ≥ pagadas` → `POR_COBRAR`).
- `BankMovementService`: conciliación manual simple (marcar `reconciled`).

---

## FASE 4 — Reportes

- Endpoints de solo lectura, agregando sobre lo ya persistido (JPQL con `SUM`/`GROUP BY`, sin librería externa):
  `GET /api/reportes/dashboard?desde=&hasta=` (KPIs: ingresos, gastos, utilidad, margen, citas, pacientes, CxC/CxP pendientes),
  `GET /api/reportes/pacientes/{id}` (historial + bitácora),
  `GET /api/reportes/pacientes` (general).
- El PDF/impresión con membrete se resuelve en el **frontend** (igual que el prototipo: vista imprimible vía `window.print()`), el backend solo entrega los datos.

---

## FASE 5 — Frontend real (SPA)

**Decisión técnica:** React + Vite + TypeScript + Tailwind (coincide con tu stack declarado). Los 13 mockups de Stitch se migran a componentes reales; no se copian los `code.html` tal cual (son estáticos y sin componentización) — se usan como **spec visual pixel-a-pixel** y el `DESIGN.md` se traduce directo a `tailwind.config.js` (mismos tokens de color/tipografía/radios/elevación).

1. **Setup del proyecto** (`klinikpro-vf-web/`, carpeta hermana de `klinikpro-vf/`): Vite + React + TS + Tailwind + React Router + cliente HTTP con interceptor de JWT (guarda token de `/api/auth/login`, lo adjunta en cada request, redirige a login en 401).
2. **Design system compartido**: theme de Tailwind generado desde `DESIGN.md` (colores, `headline-*`/`body-*`/`label-*` como utilidades, radios, sombras de elevación 0–3).
3. **Páginas** (una por mockup, en este orden porque cada una depende de la anterior estando ya wireada):
   Login → Dashboard → Pacientes → Agenda (la más compleja: calendario, slots de disponibilidad, los 9 flujos CU-01..CU-09) → Caja → Finanzas → Configuración/Catálogos → Facturación/Recetas → Ayuda.
   Versión móvil: mismas páginas con layout responsive (el PDF de diseño ya define breakpoints: 4 columnas <768px) — **no** se construyen como app separada, es CSS responsive sobre los mismos componentes, salvo que decidas lo contrario.
4. **Autorización en UI**: ocultar/deshabilitar acciones según el rol del JWT decodificado (espejo del `@PreAuthorize` del backend — la seguridad real vive en el backend, esto es solo UX).
5. **Docker**: agregar el frontend a `docker-compose.yml` (o dejarlo en dev server aparte — a decidir cuando lleguemos ahí).

---

## FASE 6 — Hardening / Producción

Uso directo de la skill `engineering:deploy-checklist` cuando lleguemos aquí. Puntos ya identificados:
- `security.jwt.secret` y credenciales de DB → variables de entorno / `.env` no comiteado.
- Quitar `"null"` de `app.cors.allowed-origins` una vez el frontend sirva por HTTP real.
- Soft-delete + auditoría en Pacientes/Citas (ya hay `auditoria_cita` de la Fase 1 — extender el patrón).
- Rate limiting en `/api/auth/login` (fuerza bruta).
- Backups de Postgres, healthchecks, logs estructurados.

---

## FASE 7 — Comercialización (referencia, no se detalla todavía)

Billing por tenant/plan, feature flags (tratamientos/cupos/logo por plan, como ya se decidió en el diseño de VF1), portal de alta de nuevas clínicas. Se retoma cuando 1–6 estén cerradas.

---

## Cómo lo vamos a trabajar

- Cada fase se sub-divide en PRs chicos (una migración + su módulo a la vez), igual que hicimos con Roles/PreAuthorize — así cada entrega es un `BUILD SUCCESS` tuyo en PowerShell antes de seguir.
- Yo preparo migración + entidades + servicio + controller + tests; tú corres `mvn clean compile` (y luego `mvn test` cuando montemos Docker/Postgres) y revisas antes del commit.
- Este documento se actualiza (no se reescribe) según avancemos — cada fase cerrada se marca aquí y se refleja en `HISTORIAL.md`.

**Siguiente paso concreto:** arrancar Fase 1.1 — migración `V4__agenda.sql` + entidades `Medico`, `Especialidad`, `Consultorio`, `HorarioMedico`, `Bloqueo`. ¿Empiezo?
