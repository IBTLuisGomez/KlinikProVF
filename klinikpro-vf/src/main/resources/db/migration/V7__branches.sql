-- ============================================================
-- FASE 5.1 (1/9) — Sucursales como entidad administrable
-- La tabla "branches" existe desde V1 (id, tenant_id, name, clinic_name,
-- active) pero sin las columnas que la auditoría contra KlinikProVF.html
-- encontró que hacían falta: horario, logo para recibos y responsables
-- por defecto (cajero / supervisor de fisioterapia).
-- ============================================================

alter table branches
    add column schedule jsonb not null default '{}'::jsonb,
    add column logo text,
    add column default_cashier text,
    add column default_supervisor text;
