-- ============================================================
-- FASE 5.1 (7/9) — Tipo "comisión" en conciliación bancaria
-- El prototipo distingue depósitos, cargos y comisiones bancarias; el
-- backend solo tenía deposito|cargo.
-- ============================================================

alter table bank_movements
    drop constraint chk_bank_movements_kind;

alter table bank_movements
    add constraint chk_bank_movements_kind check (kind in ('deposito', 'cargo', 'comision'));
