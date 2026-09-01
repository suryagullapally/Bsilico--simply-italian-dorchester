alter table orders
    add column delivery_fee_pence integer,
    add column total_pence integer;

update orders
set delivery_fee_pence = 0
where delivery_fee_pence is null;

update orders
set total_pence = subtotal_pence
where total_pence is null;

alter table orders
    alter column delivery_fee_pence set not null,
    alter column total_pence set not null,
    add constraint chk_orders_delivery_fee_pence_non_negative check (delivery_fee_pence >= 0),
    add constraint chk_orders_total_pence_non_negative check (total_pence >= 0);

create table fulfilment_settings (
    id bigserial primary key,
    collection_enabled boolean not null default true,
    delivery_enabled boolean not null default false,
    minimum_delivery_order_pence integer check (
        minimum_delivery_order_pence is null or minimum_delivery_order_pence >= 0
    ),
    delivery_fee_pence integer check (
        delivery_fee_pence is null or delivery_fee_pence >= 0
    ),
    free_delivery_threshold_pence integer check (
        free_delivery_threshold_pence is null or free_delivery_threshold_pence >= 0
    ),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

insert into fulfilment_settings (
    collection_enabled,
    delivery_enabled,
    minimum_delivery_order_pence,
    delivery_fee_pence,
    free_delivery_threshold_pence
)
values (true, false, null, null, null);

create table delivery_postcode_rules (
    id bigserial primary key,
    postcode_pattern varchar(20) not null unique,
    active boolean not null default true,
    display_order integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_delivery_postcode_rules_active_display_order
    on delivery_postcode_rules (active, display_order);

create trigger set_fulfilment_settings_updated_at
before update on fulfilment_settings
for each row
execute function set_updated_at();

create trigger set_delivery_postcode_rules_updated_at
before update on delivery_postcode_rules
for each row
execute function set_updated_at();
