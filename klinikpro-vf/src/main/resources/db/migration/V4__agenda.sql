-- ============================================================
-- FASE 1: AGENDA — catálogos, horarios, bloqueos, citas
-- Basado en LogicaAgenda.pdf. Tablas nuevas + evolución de
-- specialists/services/appointments (V2), que aún no tenían
-- código Java encima, así que se ajustan sin capa de compat.
-- ============================================================

-- ---------- ESPECIALIDADES ----------
create table specialties (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    name text not null,
    description text,
    created_at timestamptz not null default now(),
    unique (branch_id, name)
);
create index idx_specialties_branch on specialties(branch_id);

-- ---------- specialists (V2) -> se enriquece a "Médico" real ----------
alter table specialists add column document_id text;
alter table specialists add column email text;
alter table specialists add column phone text;
alter table specialists add column calendar_color text;
alter table specialists drop column specialty;

create table specialist_specialties (
    specialist_id uuid not null references specialists(id) on delete cascade,
    specialty_id uuid not null references specialties(id) on delete cascade,
    primary key (specialist_id, specialty_id)
);

-- ---------- services (V2): + especialidad + buffer ----------
alter table services add column specialty_id uuid references specialties(id) on delete set null;
alter table services add column buffer_minutes int not null default 0;

create table specialist_services (
    specialist_id uuid not null references specialists(id) on delete cascade,
    service_id uuid not null references services(id) on delete cascade,
    primary key (specialist_id, service_id)
);

-- ---------- CONSULTORIOS ----------
create table rooms (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    name text not null,
    location text,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (branch_id, name)
);
create index idx_rooms_branch on rooms(branch_id);

-- ---------- HORARIOS DEL MÉDICO ----------
create table specialist_schedules (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    specialist_id uuid not null references specialists(id) on delete cascade,
    day_of_week int not null,          -- 0=Domingo .. 6=Sábado
    start_time time not null,
    end_time time not null,
    room_id uuid references rooms(id) on delete set null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    check (day_of_week between 0 and 6),
    check (end_time > start_time)
);
create index idx_specialist_schedules_lookup on specialist_schedules(specialist_id, day_of_week);

-- ---------- BLOQUEOS ----------
create table schedule_blocks (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    specialist_id uuid references specialists(id) on delete cascade,   -- null = bloqueo global de la sucursal
    room_id uuid references rooms(id) on delete cascade,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    reason text,
    created_by uuid,
    created_at timestamptz not null default now(),
    check (ends_at > starts_at)
);
create index idx_schedule_blocks_specialist on schedule_blocks(specialist_id, starts_at);
create index idx_schedule_blocks_branch on schedule_blocks(branch_id, starts_at);

-- ---------- CITAS: evoluciona appointments (V2) ----------
alter table appointments drop column date;
alter table appointments drop column time;
alter table appointments drop column patient_name;
alter table appointments drop column service_name;
alter table appointments drop column attended_by;
alter table appointments drop column supervised_by;
alter table appointments drop column series;
alter table appointments drop column relocate;
alter table appointments drop column cancels;
alter table appointments drop column changes;
alter table appointments drop column dropped;
alter table appointments drop column logs;

alter table appointments drop constraint appointments_patient_id_fkey;
alter table appointments add constraint appointments_patient_id_fkey
    foreign key (patient_id) references patients(id) on delete restrict;
alter table appointments alter column patient_id set not null;

alter table appointments drop constraint appointments_service_id_fkey;
alter table appointments add constraint appointments_service_id_fkey
    foreign key (service_id) references services(id) on delete restrict;
alter table appointments alter column service_id set not null;

alter table appointments add column specialist_id uuid not null references specialists(id) on delete restrict;
alter table appointments add column room_id uuid references rooms(id) on delete set null;
alter table appointments add column starts_at timestamptz not null;
alter table appointments add column ends_at timestamptz not null;
alter table appointments add column source_appointment_id uuid references appointments(id) on delete set null;
alter table appointments add column rescheduled_to_id uuid references appointments(id) on delete set null;
alter table appointments add column cancellation_reason text;
alter table appointments add column late_cancellation_fee boolean not null default false;
alter table appointments add column arrived_at timestamptz;
alter table appointments add column confirmed_at timestamptz;
alter table appointments add column cancelled_at timestamptz;
alter table appointments add column started_at timestamptz;
alter table appointments add column finished_at timestamptz;
alter table appointments add column created_by uuid;

alter table appointments alter column status set default 'SCHEDULED';
alter table appointments add constraint appointments_status_check check (
    status in ('SCHEDULED','CONFIRMED','WAITING','IN_PROGRESS','COMPLETED','CANCELLED','RESCHEDULED','NO_SHOW','EXPIRED')
);

create index idx_appts_specialist_starts on appointments(specialist_id, starts_at);
create index idx_appts_room_starts on appointments(room_id, starts_at);

-- ---------- LISTA DE ESPERA ----------
create table waitlist_entries (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    branch_id uuid not null references branches(id) on delete cascade,
    patient_id uuid not null references patients(id) on delete cascade,
    specialist_id uuid references specialists(id) on delete cascade,
    service_id uuid references services(id) on delete set null,
    desired_date date,
    priority int not null default 0,
    notified boolean not null default false,
    created_at timestamptz not null default now(),
    expires_at timestamptz
);
create index idx_waitlist_branch on waitlist_entries(branch_id, desired_date);

-- ---------- AUDITORÍA DE CITAS ----------
create table appointment_audit (
    id uuid primary key default gen_random_uuid(),
    appointment_id uuid not null references appointments(id) on delete cascade,
    previous_status text,
    new_status text not null,
    user_id uuid,
    reason text,
    occurred_at timestamptz not null default now()
);
create index idx_appointment_audit_appt on appointment_audit(appointment_id);
