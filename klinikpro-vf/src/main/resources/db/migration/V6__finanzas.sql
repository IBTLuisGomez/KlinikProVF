-- ============================================================
-- FASE 3 — Finanzas + Tratamientos
-- Ajustes menores sobre receivables/payables/bank_movements/treatments
-- (tablas ya creadas en V2): columnas de auditoría, FKs reales sobre los
-- vínculos que ya existían como uuid suelto, y checks de status/kind.
-- ============================================================

-- ---------- receivables (CxC) ----------
alter table receivables
    add column created_by uuid references users(id) on delete set null;

alter table receivables
    add constraint receivables_paid_tx_fk foreign key (paid_tx_id) references transactions(id) on delete set null;

alter table receivables
    add constraint chk_receivables_status check (status in ('Pendiente', 'Pagado'));

-- ---------- payables (CxP) ----------
alter table payables
    add column created_by uuid references users(id) on delete set null;

alter table payables
    add constraint payables_paid_expense_fk foreign key (paid_expense_id) references expenses(id) on delete set null;

alter table payables
    add constraint chk_payables_status check (status in ('Pendiente', 'Pagado'));

-- ---------- bank_movements ----------
alter table bank_movements
    add column created_by uuid references users(id) on delete set null;

alter table bank_movements
    add constraint chk_bank_movements_kind check (kind in ('deposito', 'cargo'));

-- ---------- treatments ----------
alter table treatments
    add column created_by uuid references users(id) on delete set null;

alter table treatments
    add constraint chk_treatments_status
        check (status in ('activo', 'en_revision', 'por_cobrar', 'pendiente_cierre', 'finalizado', 'cerrado'));
