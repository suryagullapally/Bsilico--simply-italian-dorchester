create table admin_users (
    id bigserial primary key,
    email varchar(320) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(160) not null,
    role varchar(40) not null check (role in ('OWNER', 'MANAGER', 'STAFF')),
    active boolean not null default true,
    last_login_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index ux_admin_users_email_lower
    on admin_users (lower(email));

create index idx_admin_users_active
    on admin_users (active);

create trigger set_admin_users_updated_at
before update on admin_users
for each row
execute function set_updated_at();
