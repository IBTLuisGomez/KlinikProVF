-- ============================================================
-- FASE 5.1 (5/9) — Series de citas recurrentes
-- ============================================================

alter table appointments
    add column series_id uuid;

create index idx_appointments_series on appointments (series_id);
