create table users (
  id            uuid primary key default gen_random_uuid(),
  tenant_id     uuid not null references tenants(id) on delete cascade,
  branch_id     uuid references branches(id) on delete set null,
  email         text not null,
  password_hash text not null,
  full_name     text not null,
  role          text not null default 'RECEPCION',   -- ADMIN|FISIO|RECEPCION
  active        boolean not null default true,
  created_at    timestamptz not null default now(),
  unique (tenant_id, email)
);
create index idx_users_tenant on users(tenant_id);