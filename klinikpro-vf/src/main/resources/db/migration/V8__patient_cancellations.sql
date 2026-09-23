-- ============================================================
-- FASE 5.1 (4/9) — Cancelaciones repetidas + baja automática del paciente
-- ============================================================

alter table patients
    add column late_cancellation_count integer not null default 0,
    add column blocked boolean not null default false;
