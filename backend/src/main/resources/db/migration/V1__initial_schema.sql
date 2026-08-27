create table app_metadata (
    id bigserial primary key,
    key varchar(120) not null unique,
    value text not null,
    created_at timestamptz not null default now()
);

insert into app_metadata (key, value)
values ('schema.baseline', 'Basilico backend initial schema');
