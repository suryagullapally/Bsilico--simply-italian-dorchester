create table orders (
    id bigserial primary key,
    order_reference varchar(32) not null unique,
    fulfilment_type varchar(20) not null check (fulfilment_type in ('DELIVERY', 'COLLECTION')),
    status varchar(30) not null check (status in (
        'PENDING_PAYMENT',
        'NEW',
        'ACCEPTED',
        'PREPARING',
        'READY',
        'COMPLETED',
        'CANCELLED'
    )),
    payment_status varchar(20) not null check (payment_status in ('UNPAID', 'PAID', 'FAILED', 'REFUNDED')),
    customer_first_name varchar(120) not null,
    customer_last_name varchar(120) not null,
    customer_phone varchar(60) not null,
    customer_email varchar(254) not null,
    delivery_address_line1 varchar(220),
    delivery_address_line2 varchar(220),
    delivery_city varchar(120),
    delivery_postcode varchar(20),
    timing_type varchar(20) not null check (timing_type in ('ASAP', 'SCHEDULED')),
    requested_date date,
    requested_time time,
    order_notes text,
    subtotal_pence integer not null check (subtotal_pence >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table order_items (
    id bigserial primary key,
    order_id bigint not null references orders(id) on delete cascade,
    menu_item_id bigint references menu_items(id),
    customizer_id bigint references menu_customizers(id),
    item_type varchar(30) not null check (item_type in ('MENU_ITEM', 'CUSTOM_PIZZA')),
    product_name_snapshot varchar(220) not null,
    product_slug_snapshot varchar(160) not null,
    unit_price_pence integer not null check (unit_price_pence >= 0),
    quantity integer not null check (quantity >= 1),
    line_total_pence integer not null check (line_total_pence >= 0),
    created_at timestamptz not null default now(),
    check (
        (item_type = 'MENU_ITEM' and menu_item_id is not null and customizer_id is null)
        or
        (item_type = 'CUSTOM_PIZZA' and menu_item_id is null and customizer_id is not null)
    )
);

create table order_item_toppings (
    id bigserial primary key,
    order_item_id bigint not null references order_items(id) on delete cascade,
    pizza_topping_id bigint references pizza_toppings(id),
    topping_name_snapshot varchar(180) not null,
    price_pence_snapshot integer not null check (price_pence_snapshot >= 0),
    created_at timestamptz not null default now()
);

create index idx_orders_order_reference
    on orders (order_reference);

create index idx_orders_status
    on orders (status);

create index idx_orders_payment_status
    on orders (payment_status);

create index idx_orders_created_at
    on orders (created_at);

create index idx_order_items_order_id
    on order_items (order_id);

create index idx_order_item_toppings_order_item_id
    on order_item_toppings (order_item_id);

create trigger set_orders_updated_at
before update on orders
for each row
execute function set_updated_at();
