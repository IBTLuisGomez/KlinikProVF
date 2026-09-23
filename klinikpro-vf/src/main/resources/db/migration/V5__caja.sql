-- ============================================================
-- FASE 2 — Caja / POS
-- Ajustes menores sobre las tablas de Caja ya creadas en V2:
-- columnas de auditoría (quién abrió/cerró/creó cada registro),
-- vínculos opcionales a treatments (sesiones cubiertas) y a
-- payables (gasto a crédito → CxP), y la regla de "solo una
-- sesión de caja abierta a la vez por sucursal" a nivel de BD.
-- ============================================================

-- ---------- cash_sessions ----------
alter table cash_sessions
    add column opened_by uuid references users(id) on delete set null,
    add column closed_by uuid references users(id) on delete set null;

alter table cash_sessions
    add constraint chk_cash_sessions_status check (status in ('abierta', 'cerrada'));

-- Solo puede existir una sesión abierta por sucursal a la vez.
create unique index uq_cash_sessions_one_open
    on cash_sessions (branch_id)
    where status = 'abierta';

-- ---------- transactions ----------
alter table transactions
    add column created_by uuid references users(id) on delete set null,
    add column treatment_id uuid references treatments(id) on delete set null,
    add column cash_session_id uuid references cash_sessions(id) on delete set null;

create index idx_tx_treatment on transactions(treatment_id);
create index idx_tx_cash_session on transactions(cash_session_id);

-- ---------- expenses ----------
alter table expenses
    add column created_by uuid references users(id) on delete set null,
    add column payable_id uuid references payables(id) on delete set null,
    add column cash_session_id uuid references cash_sessions(id) on delete set null;

create index idx_exp_payable on expenses(payable_id);

-- ---------- cash_counts ----------
alter table cash_counts
    add column created_by uuid references users(id) on delete set null,
    add column cash_session_id uuid references cash_sessions(id) on delete set null;
