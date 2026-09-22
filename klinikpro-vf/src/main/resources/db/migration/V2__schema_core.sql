-- ============ CATÁLOGOS ============
create table services (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    name text not null,
    minutes int not null default 30,
    price numeric(12, 2) not null default 0,
    sessions int not null default 0,
    -- paquete: sesiones que otorga
    counts_as_session boolean not null default false,
    active boolean not null default true,
    created_at timestamptz not null default now()
);
create index idx_services_branch on services(branch_id);
create table specialists (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    name text not null,
    specialty text,
    active boolean not null default true,
    created_at timestamptz not null default now()
);
create index idx_specialists_branch on specialists(branch_id);
-- ============ PACIENTES ============
create table patients (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    code text not null,
    -- ID visible de 4 dígitos
    first_name text not null,
    last_name_p text,
    last_name_m text,
    full_name text not null,
    phone text,
    email text,
    birth_date date,
    notes text,
    specialist text,
    treater text,
    insurer boolean not null default false,
    referred boolean not null default false,
    auto_created boolean not null default false,
    created_at timestamptz not null default now(),
    unique (branch_id, code)
);
create index idx_patients_branch on patients(branch_id);
-- ============ AGENDA ============
create table appointments (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    patient_id uuid references patients(id) on delete
    set null,
        patient_name text,
        phone text,
        service_id uuid references services(id) on delete
    set null,
        service_name text,
        attended_by text,
        supervised_by text,
        date date not null,
        time time not null,
        status text not null default 'Pendiente',
        -- Pendiente|Completada|Cancelada
        series boolean not null default false,
        relocate boolean not null default false,
        cancels int not null default 0,
        changes int not null default 0,
        dropped boolean not null default false,
        -- dada de baja
        logs jsonb not null default '[]',
        created_at timestamptz not null default now()
);
create index idx_appts_branch_date on appointments(branch_id, date);
create index idx_appts_patient on appointments(patient_id);
-- ============ CAJA ============
create table cash_sessions (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    cashier text not null,
    opened_date date not null,
    opened_time time,
    open_amount numeric(12, 2) not null default 0,
    closed_date date,
    closed_time time,
    close_amount numeric(12, 2),
    status text not null default 'abierta',
    -- abierta|cerrada
    created_at timestamptz not null default now()
);
create index idx_cashsessions_branch on cash_sessions(branch_id);
create table transactions (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    folio text not null,
    patient_id uuid references patients(id) on delete
    set null,
        patient_name text,
        date date not null,
        amount numeric(12, 2) not null default 0,
        commission numeric(12, 2) not null default 0,
        method text,
        items jsonb not null default '[]',
        payments jsonb not null default '[]',
        sessions_covered int not null default 0,
        notes text,
        created_at timestamptz not null default now(),
        unique (branch_id, folio)
);
create index idx_tx_branch_date on transactions(branch_id, date);
create table expenses (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    folio_out text not null,
    ticket_folio text,
    invoice_folio text,
    concept text,
    supplier text,
    date date not null,
    amount numeric(12, 2) not null default 0,
    on_credit boolean not null default false,
    created_at timestamptz not null default now(),
    unique (branch_id, folio_out)
);
create index idx_exp_branch_date on expenses(branch_id, date);
create table cash_counts (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    date date not null,
    responsible text,
    base_amount numeric(12, 2) not null default 0,
    counted numeric(12, 2) not null default 0,
    difference numeric(12, 2) not null default 0,
    detail jsonb not null default '{}',
    created_at timestamptz not null default now()
);
create index idx_cashcounts_branch on cash_counts(branch_id);
-- ============ FINANZAS ============
create table receivables (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    client text,
    concept text,
    amount numeric(12, 2) not null default 0,
    due_date date,
    status text not null default 'Pendiente',
    -- Pendiente|Pagado
    paid_tx_id uuid,
    created_at timestamptz not null default now()
);
create index idx_cxc_branch on receivables(branch_id);
create table payables (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    supplier text,
    concept text,
    amount numeric(12, 2) not null default 0,
    due_date date,
    status text not null default 'Pendiente',
    recurring boolean not null default false,
    frequency text,
    -- mensual|quincenal|semanal|anual
    reminder_at timestamptz,
    paid_expense_id uuid,
    created_at timestamptz not null default now()
);
create index idx_cxp_branch on payables(branch_id);
create table bank_movements (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    date date,
    concept text,
    kind text,
    -- deposito|cargo
    amount numeric(12, 2) not null default 0,
    reconciled boolean not null default false,
    created_at timestamptz not null default now()
);
create index idx_bank_branch on bank_movements(branch_id);
-- ============ TRATAMIENTOS (feature de plan) ============
create table treatments (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    patient_id uuid not null references patients(id) on delete cascade,
    service_id uuid references services(id) on delete
    set null,
        recommended int not null default 0,
        paid int not null default 0,
        used int not null default 0,
        status text not null default 'activo',
        -- activo|en_revision|por_cobrar|pendiente_cierre|finalizado|cerrado
        notes text,
        start_date date,
        end_date date,
        close_date date,
        created_at timestamptz not null default now()
);
create index idx_treat_branch on treatments(branch_id);
create index idx_treat_patient on treatments(patient_id);