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

> **Estado: código escrito, pendiente de `mvn clean compile` / `mvn test` de tu lado (2026-09-23).**
> Migración `V4__agenda.sql`, 10 entidades, 9 repos, 8 servicios, 8 controllers con `@PreAuthorize`,
> el job `NoShowScheduler` (CU-08 automático) y una prueba de integración (`AppointmentServiceIT`)
> cubriendo CU-01/02/03 + la regla de no-solapamiento. Ver detalle abajo, sigue siendo la referencia.

### 1.1 Gap de modelo de datos (por qué hace falta una V4)

`LogicaAgenda.pdf` pide un modelo más rico que el V2 actual:

| Ya existe (V2) | Falta agregar |
|---|---|
| `specialists` (id, name, specialty texto) | `medicos` con `documento`, `color_agenda`; relación N:M real a especialidades (`medico_especialidad`) |
| `services` (id, name, minutes, price) | `especialidad_id` en servicios, `buffer_minutos`, relación N:M médico↔servicio (`medico_servicio`) |
| `appointments` (patient_id, service_id, attended_by texto, date, time, status) | `medico_id` (FK real, no texto), `consultorio_id`, `cita_origen_id`/`cita_nueva_id` (reprogramación), `motivo_cancelacion`, `cargo_cancelacion`, timestamps `confirmado_en/cancelado_en/iniciado_en/finalizado_en` |
| — (no existe) | `consultorios`, `horarios_medico` (día+hora por médico), `bloqueos` (vacaciones/reuniones), `lista_espera`, `auditoria_cita` |

**Decisión tomada (implementada en `V4__agenda.sql`):** nombres en inglés para mantener consistencia con V1–V3 (`specialties`, `specialist_specialties`, `specialist_services`, `rooms`, `specialist_schedules`, `schedule_blocks`, `waitlist_entries`, `appointment_audit`); `specialists` se conserva (se le agregan columnas) en vez de crear una tabla `medicos` aparte; `appointments` se evolucionó agregando las columnas que faltaban y **quitando** las columnas del prototipo Ciclo 3/4 que no tenían código Java encima todavía (`date`/`time`/`patient_name`/`service_name`/`attended_by`/`supervised_by`/`series`/`relocate`/`cancels`/`changes`/`dropped`/`logs`) a favor de `starts_at`/`ends_at` (timestamptz) y las columnas de auditoría de estado. `patient_id` y `service_id` pasaron a `NOT NULL` con `ON DELETE RESTRICT` (antes `SET NULL`) — borrar un paciente/servicio con citas ligadas ahora falla con 409, no las deja huérfanas.

### 1.2 Máquina de estados (tal cual el PDF, sin adaptarla)

```
PROGRAMADA → CONFIRMADA → EN_ESPERA → EN_ATENCION → FINALIZADA
                                            ↓
                    CANCELADA / REPROGRAMADA / NO_ASISTIO / EXPIRADA
```
Implementado como `AppointmentStatus` (enum en inglés: `SCHEDULED, CONFIRMED, WAITING, IN_PROGRESS, COMPLETED, CANCELLED, RESCHEDULED, NO_SHOW, EXPIRED`), con validación explícita de transición en cada método de `AppointmentService` (no se deja saltar de cualquier estado a cualquier estado).

### 1.3 Reglas de negocio — portadas 1:1 desde el pseudocódigo del PDF, implementadas en `AppointmentService`

- `validateAvailability(...)`: horario activo ese día (vía `SpecialistSchedule`) → sin bloqueo (`ScheduleBlockRepository.findOverlapping`) → sin solape con otra cita del médico (`AppointmentRepository.findOverlappingForSpecialist`) → sin solape de consultorio si aplica (`findOverlappingForRoom`) → dentro de anticipación mín/máx (`agenda.min-lead-minutes`/`agenda.max-lead-days` en `application.yml`, default 120 min / 60 días).
- `create(...)` (CU-01): valida servicio activo, que el médico ofrezca ese servicio, calcula fin = inicio + duración + buffer, corre `validateAvailability`, crea en `SCHEDULED`.
- `confirm(...)` (CU-02), `cancel(...)` (CU-03, con cargo por cancelación tardía si `byPatient=true` y quedan menos de `agenda.cancellation-min-hours` — default 24h), `reschedule(...)` (CU-04, cierra la original como `RESCHEDULED` y crea una nueva citada por `sourceAppointmentId`).
- `registerArrival` (CU-05 → `WAITING`), `startCare`/`finishCare` (CU-06 → `IN_PROGRESS`/`COMPLETED`, restringido a rol `CLINICAL` = ADMIN/COORDINADOR/FISIO, no RECEPCION).
- `markNoShow` manual (CU-08) + `runAutomaticNoShowSweep()` corrido cada 5 min por `NoShowScheduler` (`@Scheduled`, `agenda.no-show-check-interval-ms`) sobre citas `SCHEDULED`/`CONFIRMED` vencidas + tolerancia (`agenda.no-show-tolerance-minutes`, default 15 min).
- Cada cambio de estado queda en `appointment_audit` vía `AppointmentAuditRepository` (CU auditoría, sección 4 del PDF).
- CU-07 (Lista de Espera) y CU-09 (Horarios/Bloqueos) implementados como CRUD en `WaitlistService`/`SpecialistScheduleService`/`ScheduleBlockService` — sin el flujo de notificación por WhatsApp/SMS (eso es Fase 5+, cuando haya canal real).

**Nota de alcance:** las reglas "particularidades por especialidad" (tablas 4.1–4.3 del PDF: duraciones recomendadas, overbooking, tolerancia) quedaron como **valores configurables** (`application.yml` / por servicio), no hardcodeadas — el PDF las da como recomendación, no como regla dura. Overbooking y prioridad/urgencias (sección 4, "Sobrecupo", "Prioridad") no se implementaron en este corte — quedan anotadas para cuando el frontend de Agenda los necesite.

### 1.4 Endpoints implementados (todos con `@PreAuthorize` vía `Roles.java`)

```
/api/specialties          CRUD           LEADERSHIP (escritura) · ANY (lectura)
/api/rooms                CRUD           LEADERSHIP · ANY
/api/services              CRUD           LEADERSHIP · ANY
/api/specialists           CRUD (+deactivate) LEADERSHIP · ANY
/api/specialist-schedules  crear/listar/borrar LEADERSHIP · ANY (lectura)
/api/schedule-blocks       crear/listar/borrar LEADERSHIP · ANY (lectura)
/api/waitlist              CRUD + notify  FRONT_DESK
/api/appointments          crear, listar (rango/paciente/id)   FRONT_DESK (crear) · ANY (lectura)
  /{id}/confirm /cancel /reschedule /arrival /no-show          FRONT_DESK
  /{id}/start /{id}/finish                                     CLINICAL (médico)
```

### 1.5 Tests

- `AppointmentServiceIT` (`@SpringBootTest`, perfil `test`/H2): CU-01 crear cita, regla de no-solapamiento del médico (segunda cita choca), CU-02 confirmar, CU-03 cancelar, y que una cita cerrada no se puede volver a cancelar.
- **Pendiente para cuando corras con feedback real de compilador:** casos de bloqueo/fuera-de-horario/anticipación, CU-04 a CU-09 completos, y tests de `AuthController`/`PatientService` (deuda ya identificada en Fase 0).

---

## FASE 2 — Caja / POS

> **Estado: código escrito, pendiente de `mvn clean compile` / `mvn test` de tu lado (2026-09-22).**
> Migración `V5__caja.sql` (ajustes menores sobre V2, sin romper nada), paquete `caja` completo
> (4 entidades, 4 repos, DTOs, 4 servicios, 4 controllers) y un paquete `finance` mínimo
> (`Payable`/`Treatment`, solo lo que Caja necesita tocar — Fase 3 les agrega el ciclo completo).
> Prueba de integración `CajaServiceIT` cubriendo sesión/cobro mixto/gasto a crédito/arqueo.

### 2.1 Migración `V5__caja.sql` (ajustes menores, tal como decía el plan)

Sin romper las tablas de V2, se agregó: `opened_by`/`closed_by` (UUID, auditoría) y el check `status in ('abierta','cerrada')` en `cash_sessions`, más un **índice único parcial** `uq_cash_sessions_one_open` (`branch_id` donde `status='abierta'`) que impone a nivel de BD la regla de "una sola sesión abierta por sucursal a la vez" (no solo en el service); `created_by`/`treatment_id`/`cash_session_id` en `transactions`; `created_by`/`payable_id`/`cash_session_id` en `expenses`; `created_by`/`cash_session_id` en `cash_counts`.

**Nota:** `transactions` ya traía `commission` (numeric) desde V2 — no se duplicó esa columna.

### 2.2 `CashSessionService`

- `open`: rechaza si ya hay una sesión `abierta` en la sucursal (`IllegalArgumentException` → 400; además reforzado por el índice único de la migración). `cashier` se resuelve del usuario autenticado (`UserRepository.fullName`), `openedBy` guarda el UUID.
- `close`: solo si está `abierta`; cerrar dos veces falla igual que abrir dos veces.
- `reopen` (**solo `LEADERSHIP`**): reabre una sesión cerrada por error, validando que no haya otra ya abierta.
- `GET /api/caja/sesiones/actual`: la sesión abierta de la sucursal (o null).

### 2.3 `TransactionService` — cobro (POS)

- **Folio automático** `AÑO-MES3-NNN` (ej. `2026-SEP-001`), consecutivo por sucursal+mes.
- Exige una `CashSession` abierta (si no hay, 400 — "ábrela antes de cobrar").
- `items[]`/`payments[]` se guardan como **jsonb** en las columnas ya existentes de V2, usando el soporte nativo de JSON de Hibernate (`@JdbcTypeCode(SqlTypes.JSON)`, sin dependencias nuevas) — **primer uso de jsonb en el proyecto**, marcado explícitamente en el código porque si algo del mapeo falla al compilar/testear en tu máquina, es el sospechoso #1.
- **Decisión de diseño sobre la comisión** (el PDF/prototipo no la detalla al nivel de fórmula, así que quedó documentada aquí para que la valides): `payments[]` reparte el **subtotal** entre métodos (lo que efectivamente se vendió), no el total final — evita una ecuación circular (comisión que depende de un monto que ya incluye la comisión). La comisión (`caja.card-commission-rate`, default 3.5%) se calcula sobre la porción asignada a `debito`/`credito` y se **suma aparte** al total (`amount` = subtotal + comisión), que es el monto que efectivamente absorbe el cliente. Ejemplo: subtotal $1000 (600 efectivo + 400 tarjeta) → comisión $14.00 → total $1014.00.
- Vínculo opcional a `treatments`: si viene `treatmentId` + `sessionsCovered`, incrementa `Treatment.paid` (tope en `recommended` si está definido) — el ciclo completo de estados del tratamiento es Fase 3.
- Sin endpoint de cancelación/borrado de transacciones — se dejó como bitácora inmutable (buena práctica contable); si necesitas corregir un cobro, Fase 3 puede agregar una nota de crédito en vez de editar el registro.

### 2.4 `ExpenseService`

- Folio interno `EGR-AÑO-MES3-NNN`, separado de `ticketFolio`/`invoiceFolio` (los del proveedor, ya existían en V2).
- Gasto de **contado**: exige `CashSession` abierta (sale dinero de la caja física ahora).
- Gasto **a crédito** (`onCredit=true`): NO exige sesión abierta — crea una `Payable` (`payables`, tabla V2) con estado `Pendiente` y la enlaza (`expense.payableId`). Fase 3 (`PayableService`) se encarga de pagarla, lo que en ese momento sí generará el gasto/salida de caja real.

### 2.5 `CashCountService` — arqueo

- Efectivo esperado = `openAmount` de la sesión abierta + suma de pagos `efectivo` del día (de `transactions`) − suma de gastos de contado del día (de `expenses`, `onCredit=false`). Tarjeta/transferencia no cuentan (no son efectivo físico).
- `difference` = contado − esperado. El desglose (`baseAmount`/`cashIn`/`cashOut`/`expected`) queda en `detail` (jsonb) para mostrarlo en el frontend.

### 2.6 Endpoints (todos con `@PreAuthorize`)

```
/api/caja/sesiones          GET (ANY) · /actual (ANY) · /{id} (ANY)
  /abrir /{id}/cerrar                                    FRONT_DESK
  /{id}/reabrir                                           LEADERSHIP
/api/caja/transacciones      GET ?date= (ANY) · /{id} (ANY) · POST         FRONT_DESK
/api/caja/gastos              GET ?date= (ANY) · /{id} (ANY) · POST         FRONT_DESK
/api/caja/arqueos             GET (ANY) · POST                              FRONT_DESK
```

**Nota:** no se implementó un endpoint separado de "forzar cierre" — `FRONT_DESK` ya incluye `ADMIN`/`COORDINADOR` (liderazgo), así que el cierre normal cubre ese caso; `reabrir` es la única acción exclusiva de `LEADERSHIP` en este módulo.

### 2.7 Tests

- `CajaServiceIT` (`@SpringBootTest`, perfil `test`/H2): abrir/cerrar sesión (+ rechazo de doble apertura/cierre), cobro mixto efectivo+tarjeta verificando la comisión exacta, cobro rechazado sin sesión abierta, gasto a crédito generando CxP, arqueo con diferencia calculada.
- **Riesgo a vigilar en el compile/test real:** es el primer uso de jsonb (`items`/`payments` en `Transaction`, `detail` en `CashCount`) — si Hibernate/H2 tienen algún problema con `@JdbcTypeCode(SqlTypes.JSON)`, va a salir aquí primero.

---

## FASE 3 — Finanzas + Tratamientos

> **Estado: código escrito, pendiente de `mvn clean compile` / `mvn test` de tu lado (2026-09-22).**
> Migración `V6__finanzas.sql`, 2 entidades nuevas (`Receivable`, `BankMovement`) + las 2 que ya
> existían mínimas desde Fase 2 (`Payable`, `Treatment`) ahora completas, 4 servicios, 4 controllers.
> `TransactionService`/`ExpenseService` (Caja, Fase 2) se ampliaron con un método cada uno para que
> pagar una CxC/CxP genere el movimiento real en Caja — dependencia cruzada `finance` ↔ `caja` a
> propósito (ver nota de arquitectura abajo). Prueba de integración `FinanceServiceIT`.

### 3.1 `TreatmentService` — máquina de estados

Implementa exactamente la regla documentada del prototipo — **"usadas ≥ pagadas → POR_COBRAR"** — con dos matices que no estaban explícitos y que decidí para que la regla tuviera sentido en los bordes:

- Con `used=0` y `paid=0` (tratamiento recién creado) la comparación `0 ≥ 0` también sería cierta; se exige `used > 0` para no marcar "por_cobrar" algo que nadie ha usado todavía.
- Si además `used ≥ recommended` (paquete completo consumido), eso pesa más que "falta cobrar" → pasa a `pendiente_cierre` en vez de `por_cobrar`.
- `en_revision` y el cierre final (`finalizado`) son **manuales** — el PDF no da una señal automática para dispararlos. `close()` es acción de `LEADERSHIP`.
- `registerPayment(id, sesiones)` es llamado por `TransactionService` al cobrar en Caja (Fase 2) — **se refactorizó** `TransactionService` para que ya no toque `Treatment` directamente (antes hacía `tr.setPaid(...)` a mano sin recalcular status); ahora delega en `TreatmentService`, así el status siempre queda consistente sin importar desde dónde se pague.
- `registerUsage` queda expuesto para uso manual — **Agenda no lo dispara todavía** (no se tocó `AppointmentService` de Fase 1 para no arriesgar código que aún no confirmas que compila). Cuando quieras, `AppointmentService.finishCare` puede llamar `TreatmentService.registerUsage` si el servicio de la cita cuenta como sesión (`Service.countsAsSession`) — queda anotado como siguiente paso, no implementado en este corte.

### 3.2 `ReceivableService` / `PayableService` — CxC/CxP con movimiento real en Caja

No es un cambio de status nada más: pagar una CxC llama a `TransactionService.createFromReceivablePayment` (nuevo método en Fase 2) que crea una `Transaction` de verdad — mismo folio automático, misma comisión de tarjeta si el método es débito/crédito, exige sesión de caja abierta igual que un cobro normal. Pagar una CxP llama a `ExpenseService.createFromPayablePayment` (idem, con `Expense`).

- **Reversión** (`reverse`, solo `LEADERSHIP`): borra el `Transaction`/`Expense` generado y regresa la CxC/CxP a `Pendiente`. Esto es una excepción deliberada a la regla de "Caja es una bitácora inmutable" que dejé en Fase 2 (ahí no expuse borrar transacciones) — aquí sí se borra, pero solo el registro creado por *este* flujo de pago específico, solo `LEADERSHIP`, y solo para corregir un pago mal registrado (no un endpoint genérico de borrado). Lo dejo marcado por si prefieres el enfoque contable estricto (nunca borrar, solo asiento de reversa) — es un cambio de una función, avísame.
- **CxP recurrente**: al pagar, genera de una vez la siguiente ocurrencia (`Pendiente`, misma `frequency`, `dueDate` corrida: `+1 mes`/`+15 días`/`+1 semana`/`+1 año` según `mensual|quincenal|semanal|anual`). La reversión de un pago **no** borra esa ocurrencia ya generada — queda como obligación futura independiente (documentado como límite conocido, no bloqueante).

**Nota de arquitectura:** `finance` ahora depende de `caja` (para crear `Transaction`/`Expense`) y `caja` ya dependía de `finance` desde Fase 2 (para `Treatment`/`Payable`) — es una dependencia cruzada entre paquetes, no un ciclo de beans de Spring (ningún constructor se necesita circularmente), así que compila y arranca sin problema, pero es la clase de cosa que si el proyecto crece mucho más conviene aplanar. Anotado para Fase 6 (hardening), no urgente ahora.

### 3.3 `BankMovementService`

CRUD simple + `reconcile(id)` (marca `reconciled=true`). Sin conciliación automática contra un estado de cuenta importado — eso sería una fase aparte si algún día importas archivos del banco.

### 3.4 Migración `V6__finanzas.sql`

Ajustes menores sobre tablas de V2 que ya existían (`receivables`, `payables`, `bank_movements`, `treatments`): columna `created_by` en las 4, **FK real** sobre `receivables.paid_tx_id → transactions(id)` y `payables.paid_expense_id → expenses(id)` (antes eran `uuid` sueltos sin integridad referencial), y checks de `status`/`kind`.

### 3.5 Endpoints

```
/api/tratamientos            GET ?patientId= (ANY) · /{id} (ANY) · POST         FRONT_DESK
  /{id}/uso                                                                      CLINICAL
  /{id}/cerrar                                                                   LEADERSHIP
/api/cxc                      GET (ANY) · /{id} (ANY) · POST · /{id}/pagar       FRONT_DESK
  /{id}/revertir                                                                 LEADERSHIP
/api/cxp                      GET (ANY) · /{id} (ANY) · POST · /{id}/pagar       FRONT_DESK
  /{id}/revertir                                                                 LEADERSHIP
/api/banco                    GET · POST · /{id}/conciliar                       LEADERSHIP
```

### 3.6 Tests

- `FinanceServiceIT`: el ciclo completo de un tratamiento (pago→uso→"por_cobrar"→pago que lo alcanza→uso que agota el paquete→"pendiente_cierre"→cierre→rechazo de cerrar dos veces), pagar una CxC (rechazado sin sesión abierta, genera `Transaction`, reversión borra esa transacción), pagar una CxP recurrente (genera `Expense` + la siguiente ocurrencia con la fecha corrida, reversión), y conciliación de un movimiento bancario.

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
- Soft-delete + auditoría en Pacientes/Citas (ya hay `appointment_audit` de la Fase 1 — extender el patrón a Pacientes).
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

**Siguiente paso concreto:** corre `mvn clean compile` y luego `mvn test` (usa el perfil `test`/H2, no necesita Docker) sobre lo que acabo de escribir para Fase 1. Si algo no compila, pega el error y lo arreglamos ahí mismo.
