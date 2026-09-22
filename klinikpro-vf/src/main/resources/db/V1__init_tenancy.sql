create table tenants (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    slug text not null unique,
    plan text not null default 'basic',
    created_at timestamptz not null default now()
);
create table branches (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references tenants(id) on delete cascade,
    name text not null,
    clinic_name text,
    active boolean not null default true,
    created_at timestamptz not null default now()
);
create index idx_branches_tenant on branches(tenant_id);