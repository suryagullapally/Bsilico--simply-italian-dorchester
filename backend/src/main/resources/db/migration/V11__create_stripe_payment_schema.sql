create table payment_attempts (
    id bigserial primary key,
    order_id bigint not null references orders(id) on delete restrict,
    provider varchar(30) not null check (provider in ('STRIPE')),
    status varchar(30) not null check (status in ('CREATED', 'OPEN', 'PAID', 'FAILED', 'EXPIRED')),
    amount_pence integer not null check (amount_pence >= 0),
    currency varchar(3) not null check (currency = 'GBP'),
    stripe_checkout_session_id varchar(255) unique,
    stripe_payment_intent_id varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    completed_at timestamptz
);

create table stripe_webhook_events (
    id bigserial primary key,
    stripe_event_id varchar(255) not null unique,
    event_type varchar(120) not null,
    processed_at timestamptz not null default now()
);

create index idx_payment_attempts_order_id
    on payment_attempts (order_id);

create index idx_payment_attempts_status
    on payment_attempts (status);

create index idx_payment_attempts_created_at
    on payment_attempts (created_at);

create index idx_stripe_webhook_events_event_type
    on stripe_webhook_events (event_type);

create trigger set_payment_attempts_updated_at
before update on payment_attempts
for each row
execute function set_updated_at();
