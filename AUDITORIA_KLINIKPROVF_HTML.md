# Auditoría: ¿KlinikProVF (backend + frontend) cubre toda la lógica de `KlinikProVF.html`?

**Fecha:** 2026-09-23
**Método:** lectura completa del prototipo `KlinikProVF.html` (v2.5, ~6,300 líneas, IndexedDB + vanilla JS) — paneles, formularios y las ~140 funciones de negocio — contra el código real de `klinikpro-vf/` (Spring Boot, Fases 1-4) y el estado actual de `klinikpro-vf-web/` (Fase 5, en curso). No se modificó código; esto es solo diagnóstico.

**Contexto importante:** `KlinikProVF.html` es tu prototipo funcional completo (offline, un solo archivo, IndexedDB) — es una fuente de verdad de negocio mucho más rica y específica que los mockups de Stitch. El backend actual, sin embargo, no es una copia 1:1 de ese prototipo: en varios módulos (sobre todo Agenda) se implementó una lógica **más rigurosa y distinta** basada en `LogicaAgenda.pdf` (casos de uso CU-01..CU-09). Eso no es necesariamente un error, pero sí significa que hay dos "fuentes de verdad" que no siempre coinciden, y varias decisiones de diseño quedaron sin resolver explícitamente. Abajo separo: **✅ capturado**, **⚠️ capturado con diferencias de diseño** (requieren tu decisión) y **❌ ausente**.

---

## 1. Hallazgo más importante: Sucursales (multi-branch) no tiene API

En el prototipo, "sucursal" es el eje central: todo (pacientes, citas, caja, finanzas, catálogos) se filtra por `sucursalId`, y desde Ajustes puedes crear sucursales, cambiar la activa, y configurar por sucursal: nombre, horarios por día de la semana, cupos por hora, responsable de caja, responsable de fisioterapia, y logo para recibos.

En el backend real:
- La tabla `branches` **sí existe** en la base de datos (`V1__init_tenancy.sql`: `id, tenant_id, name, clinic_name, active, created_at`).
- **No existe ninguna entidad JPA, repositorio, servicio ni controlador** para sucursales. No hay forma, desde la aplicación, de crear una sucursal nueva, renombrarla, o configurar su horario/cupos/logo/responsables — solo se podría hacer con SQL directo.
- Tampoco existen en el esquema las columnas para horario semanal, cupos por hora, logo o responsables — el modelo de datos de sucursal quedó más simple que en el prototipo.

**Por qué importa:** todo lo demás (especialmente Agenda) depende de esta configuración por sucursal en el prototipo. Si vas a operar con una sola sucursal por ahora, esto puede esperar; si planeas vender KlinikPro a clínicas con varias sucursales pronto, es el hueco más grande de todos.

---

## 2. Agenda — mismo dominio, dos modelos distintos

El backend (`AppointmentService`, ~CU-01 a CU-09) es más riguroso que el prototipo en varios sentidos, pero el **modelo de capacidad es fundamentalmente distinto**:

| | Prototipo (`KlinikProVF.html`) | Backend real |
|---|---|---|
| Unidad de disponibilidad | "Cupos por hora" de la **sucursal** (N pacientes en paralelo, sin importar especialista) | Calendario **por especialista** (`SpecialistSchedule`), sin concepto de cupos — 1 especialista, 1 cita a la vez |
| Choque de horario | Aviso blando ("¿agendar de todos modos?") — no bloquea | Bloqueo duro (excepción) si el especialista ya tiene cita, bloqueo de sala, o fuera de su horario |
| Anticipación mínima/máxima | No existe | Sí: `agenda.min-lead-minutes` (120) / `agenda.max-lead-days` (60) |
| Cancelación tardía | No existe | Sí: cargo por cancelación tardía si faltan <24h (`agenda.cancellation-min-hours`) |
| No-show | No existe | Sí, manual y automático (`NoShowScheduler`, barrido por tolerancia) |
| Reprogramar | Cambia la misma fila + contador `cambios` + `logs[]` | Crea una cita nueva y enlaza la vieja (`sourceAppointmentId`/`rescheduledToId`) — historial inmutable, más trazable |

Estas diferencias son en general **mejoras** (más control clínico real), pero es una decisión de producto distinta a la del prototipo. Vale la pena que confirmes cuál quieres para la versión final, porque afecta directamente el diseño de la página de Agenda del frontend (que aún no se ha construido).

### Reglas específicas del prototipo que **no están** en el backend:
- ❌ **Serie de citas recurrentes** (elegir días de la semana + número de sesiones, crea todas de una vez). No existe ningún equivalente (`crear en lote`) en `AppointmentService`.
- ❌ **Baja automática del paciente tras 2 cancelaciones sin motivo** (`dadaDeBaja`). El backend no tiene este campo ni la regla.
- ❌ Contadores de `cambios`/`cancelaciones` visibles por cita (el backend lo resuelve distinto, con auditoría completa por evento — es un reemplazo razonable, pero no expone "cuántas veces se cambió esta cita" como dato directo).
- ❌ Enlace WhatsApp de confirmación (`wa.me`) — es una función de frontend, pendiente porque la página de Agenda aún no se construye.
- ❌ **"Seguimiento"**: pacientes con una sesión completada pero sin próxima cita agendada. No hay ningún endpoint ni query para esto (ni en `ReportService` ni en otro lado). Es una función completa del prototipo que no tiene ningún rastro en el backend.

### Tratamientos ↔ Agenda: la sesión consumida NO se descuenta todavía
Esto ya estaba documentado por ti/por mí antes, y lo confirmo: en el prototipo, completar una cita cuyo servicio es "cuenta como sesión" incrementa automáticamente `sesionesUsadas` del tratamiento activo del paciente. En el backend, `TreatmentService.registerUsage()` existe pero **nada lo llama** — el comentario en el código lo dice explícitamente ("Agenda no lo dispara todavía"). Sigue pendiente.

---

## 3. Tratamientos — máquina de estados con una diferencia real

El backend documenta explícitamente que interpretó el PDF de forma distinta al prototipo en dos puntos, y vale la pena que sepas exactamente cuáles:

- **`en_revision`** (pagó menos de lo recomendado): en el prototipo se marca **automáticamente** al registrar el pago. En el backend, el comentario dice que esta transición "no tiene señal automática documentada" y **la dejaron manual** — es decir, hoy el backend nunca calcula `en_revision` solo.
- **`pendiente_cierre`**: en el prototipo es **100% manual** (botón "Finalizar tratamiento"). En el backend se dispara **automáticamente** en cuanto `usadas >= recomendadas` — es un comportamiento que el prototipo no tiene.

Ninguna de las dos es "incorrecta", pero son decisiones distintas a las del `.html`, y como pediste específicamente fidelidad a ese archivo, te las señalo para que decidas cuál te sirve más.

- ⚠️ La regla "pacientes de aseguradora no pueden recibir paquete/promoción" (bloqueo duro en el prototipo) no encontré que esté validada en `TreatmentService.create()` — solo valida `patientId` y `recommended >= 0`, no revisa si el paciente tiene `insurer=true` antes de aceptar un `serviceId` de paquete. Vale la pena confirmarlo si es una regla que quieres mantener.

---

## 4. Caja — muy fiel en folios y estructura, con 4 diferencias que importan para la operación diaria

✅ Folio de cobro (`{AÑO}-{MES3}-{NNN}`), gating de "no se puede cobrar sin sesión de caja abierta", ítems múltiples por cobro, comisión de tarjeta sumada al total (la absorbe el cliente) — todo esto está capturado fielmente o mejorado.

- ⚠️ **Comisión de tarjeta fija vs. manual**: el prototipo permite que el cajero capture, *cobro por cobro*, si la comisión es % o monto fijo (útil si cambian de terminal o negocian tasas distintas para débito/crédito). El backend aplica una **tasa única global** por configuración (`caja.card-commission-rate`, hoy 3.5%) igual para débito y crédito, sin poder ajustarla por transacción. Si en la práctica usan más de un banco/terminal, esto puede quedar corto.
- ❌ **Sin "fecha de operación"**: el prototipo permite operar caja (cobros, gastos, arqueo) para cualquier fecha elegida — útil si el turno cruza medianoche o para correcciones retroactivas. El backend usa siempre `LocalDate.now()` al crear transacciones/gastos/arqueos; no hay forma de registrar "esto fue de ayer" sin tocar la hora del sistema.
- ❌ **Arqueo sin desglose de denominaciones**: el prototipo captura cuántos billetes/monedas de cada valor se contaron (auditoría real del conteo físico). El backend solo guarda el total contado (`counted`), no el desglose — se pierde el detalle de "cuántos billetes de $500 había".
- ❌ **Folio de transferencia por pago**: el prototipo guarda un folio de transferencia bancaria por cada línea de pago (para conciliar después). El `PaymentLine` del backend solo tiene `método` + `monto`, sin campo de folio/referencia.

---

## 5. Finanzas (CxC / CxP / Conciliación) — la parte más fiel de todo el sistema

✅ El patrón "marcar pagada una CxC/CxP crea el movimiento real de caja, y revertir borra ese movimiento" está capturado **exactamente** igual que en el prototipo (y es el mismo patrón que ya habíamos marcado para revisar más adelante con un enfoque contable más estricto). ✅ CxP recurrente con generación automática de la siguiente ocurrencia (misma lógica de frecuencia mensual/quincenal/semanal/anual). ✅ Campo de recordatorio (`reminderAt`) sí existe en la base de datos.

- ⚠️ El **recordatorio no tiene endpoint de consulta** ("¿qué recordatorios están vencidos ahora?"): el dato se guarda pero nadie lo expone filtrado; el banner del prototipo tendría que reconstruirse en el frontend trayendo todas las CxP y filtrando ahí (funciona, pero es menos eficiente que un endpoint dedicado).
- ⚠️ Conciliación bancaria: el prototipo tiene 3 tipos de partida (depósito +, cargo −, **comisión bancaria** −). El backend (`BankMovementService`) solo acepta `deposito` y `cargo` — "comisión bancaria" no es un tipo válido hoy.

---

## 6. Reportes — lo agregado existe, dos piezas completas faltan

✅ Dashboard (KPIs, citas por estado, pacientes nuevos, CxC/CxP) y bitácora por paciente están capturados, y de hecho la bitácora del backend es **mejor** que la del prototipo (combina citas + cobros + tratamientos en una sola línea de tiempo, no solo cambios/cancelaciones de cita).

- ❌ **"Seguimiento"** (ya mencionado en Agenda): no existe en ningún lado del backend.
- ❌ El **reporte operativo y financiero completo** (con membrete, desgloses por forma de pago / por especialista / por proveedor, arqueos del periodo, listas de CxC/CxP pendientes, tratamientos activos) que en el prototipo se arma para imprimir/exportar a PDF no tiene un endpoint agregador — pero esto probablemente no es grave: los datos crudos (transacciones, gastos, tratamientos, arqueos) sí están disponibles vía los endpoints CRUD existentes, así que se podría construir enteramente en el frontend cuando llegue el momento de esa pantalla.

---

## 7. Pacientes — muy fiel, con una simplificación de diseño

✅ Código de 4 dígitos autogenerado y único por sucursal, nombre obligatorio, `treater` (tratante) usa `specialist` como default si viene vacío — todo capturado.

- ⚠️ En el prototipo, "especialista"/"tratante" se guardan como texto **y** se resuelven a un ID real del catálogo de especialistas cuando el nombre coincide de forma única (`especialistaId`/`tratanteId`). En el backend, `Patient.specialist`/`Patient.treater` son **solo texto libre**, sin relación (FK) al catálogo real de especialistas. Esto significa que hoy no se puede confiar en "cuántos pacientes atiende el Dr. X" con una consulta relacional — solo por coincidencia de texto.
- ⚠️ El aviso de "posible duplicado" (mismo nombre o mismo teléfono, con opción de guardar de todos modos) es lógica de UX que en el prototipo vive en el cliente. El backend sí tiene `search()` disponible para que el frontend implemente el mismo aviso — no es un gap del backend, pero hay que recordar construirlo cuando se haga la página de Pacientes (aún pendiente).
- ❌ Alta automática de paciente desde un cobro rápido (si no coincide nombre/código, se crea un paciente con `autoCreado:true`) no está en `TransactionService.create()` — hoy si mandas un `patientId` que no existe, tira error; si no mandas `patientId`, el cobro simplemente queda sin paciente enlazado.

---

## 8. Funciones del prototipo que son del navegador y probablemente no se deban replicar igual

Estas dependen de APIs específicas de navegador (File System Access API) y de que todo viva en IndexedDB local — no tiene sentido portarlas literalmente a una arquitectura cliente-servidor, pero las menciono para que decidas si quieres un equivalente:

- Sincronización automática a CSV en una carpeta local del equipo (solo Chrome/Edge).
- Exportar/importar respaldo completo en `.json` y "borrar todos los datos" — con un backend real y Postgres, esto se resuelve con backups de base de datos del lado del servidor, no cliente.

---

## 9. Resumen priorizado — qué decidir primero

1. **Sucursales sin API** (§1): bloquea cualquier operación multi-sucursal real desde la app. Si por ahora solo operan una sucursal, esto es de baja urgencia; si no, es lo primero.
2. **Paradigma de Agenda** (§2): cupos-por-hora vs. calendario-por-especialista. Afecta directamente cómo se diseña la próxima página de Agenda del frontend — conviene decidirlo antes de construirla, no después.
3. **Sesión de tratamiento no se descuenta al completar una cita** (§2/§3): ya lo teníamos identificado; sigue siendo el enganche más importante entre Agenda y Finanzas que falta cablear.
4. **"Seguimiento" de pacientes** (§2/§6): funcionalidad completa del prototipo, ausente en el backend — fácil de agregar como una query nueva en `ReportService` cuando quieras.
5. Detalles de Caja (§4): fecha de operación, comisión manual por cobro, denominaciones del arqueo, folio de transferencia — ninguno es urgente individualmente, pero juntos son la diferencia entre "sirve para demo" y "sirve para operar caja todos los días como ya lo hacían con el prototipo".

No toqué código en esta revisión — es solo el mapa de diferencias. Dime en qué orden quieres que ataquemos esto y seguimos.
