create table menu_categories (
    id bigserial primary key,
    slug varchar(120) not null unique,
    name varchar(160) not null,
    display_order integer not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table menu_items (
    id bigserial primary key,
    category_id bigint not null references menu_categories(id),
    slug varchar(160) not null unique,
    name varchar(220) not null,
    description text,
    price_pence integer not null check (price_pence >= 0),
    image_path varchar(500),
    product_type varchar(40) not null check (product_type in ('STANDARD', 'PIZZA')),
    available boolean not null default true,
    active boolean not null default true,
    featured boolean not null default false,
    customizable boolean not null default false,
    display_order integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table menu_item_dietary_tags (
    menu_item_id bigint not null references menu_items(id) on delete cascade,
    tag varchar(2) not null check (tag in ('V', 'GF', 'VE')),
    primary key (menu_item_id, tag)
);

create table menu_customizers (
    id bigserial primary key,
    category_id bigint not null references menu_categories(id),
    slug varchar(160) not null unique,
    name varchar(220) not null,
    base_price_pence integer not null check (base_price_pence >= 0),
    extra_topping_price_pence integer not null check (extra_topping_price_pence >= 0),
    active boolean not null default true,
    display_order integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table pizza_toppings (
    id bigserial primary key,
    customizer_id bigint not null references menu_customizers(id) on delete cascade,
    name varchar(180) not null,
    price_override_pence integer check (price_override_pence is null or price_override_pence >= 0),
    available boolean not null default true,
    display_order integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (customizer_id, name)
);

create index idx_menu_items_category_display_order
    on menu_items (category_id, display_order);

create index idx_menu_items_public_order
    on menu_items (active, category_id, display_order);

create index idx_menu_customizers_category_display_order
    on menu_customizers (category_id, display_order);

create index idx_pizza_toppings_customizer_display_order
    on pizza_toppings (customizer_id, display_order);

create or replace function set_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create trigger set_menu_categories_updated_at
before update on menu_categories
for each row
execute function set_updated_at();

create trigger set_menu_items_updated_at
before update on menu_items
for each row
execute function set_updated_at();

create trigger set_menu_customizers_updated_at
before update on menu_customizers
for each row
execute function set_updated_at();

create trigger set_pizza_toppings_updated_at
before update on pizza_toppings
for each row
execute function set_updated_at();
