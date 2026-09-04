alter table customer_notifications
drop constraint chk_customer_notifications_type;

alter table customer_notifications
add constraint chk_customer_notifications_type check (
    notification_type in (
        'ORDER_RECEIVED',
        'ORDER_ACCEPTED',
        'ORDER_READY',
        'ORDER_CANCELLED',
        'ORDER_REFUNDED',
        'BOOKING_REQUEST_RECEIVED',
        'BOOKING_CONFIRMED',
        'BOOKING_DECLINED',
        'BOOKING_CANCELLED',
        'RESTAURANT_NEW_ORDER',
        'RESTAURANT_NEW_BOOKING',
        'MANUAL_MESSAGE'
    )
);

create table payment_refunds (
    id bigserial primary key,
    order_id bigint not null references orders(id) on delete restrict,
    payment_attempt_id bigint not null references payment_attempts(id) on delete restrict,
    provider varchar(30) not null check (provider in ('STRIPE')),
    stripe_refund_id varchar(255) unique,
    amount_pence integer not null check (amount_pence >= 0),
    currency varchar(3) not null check (currency = 'GBP'),
    status varchar(30) not null check (
        status in ('CREATED', 'PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'REQUIRES_ACTION')
    ),
    reason varchar(40) not null check (
        reason in ('CUSTOMER_REQUESTED', 'DUPLICATE', 'FRAUDULENT', 'OTHER')
    ),
    note text,
    failure_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    completed_at timestamptz
);

create index idx_payment_refunds_order_id
    on payment_refunds (order_id);

create index idx_payment_refunds_payment_attempt_id
    on payment_refunds (payment_attempt_id);

create index idx_payment_refunds_status
    on payment_refunds (status);

create index idx_payment_refunds_created_at
    on payment_refunds (created_at);

create unique index ux_payment_refunds_order_active_refund
    on payment_refunds (order_id)
    where status in ('CREATED', 'PENDING', 'REQUIRES_ACTION');

create unique index ux_payment_refunds_order_succeeded_refund
    on payment_refunds (order_id)
    where status = 'SUCCEEDED';

create trigger set_payment_refunds_updated_at
before update on payment_refunds
for each row
execute function set_updated_at();
