create table customer_notifications (
    id bigserial primary key,
    channel varchar(20) not null,
    notification_type varchar(60) not null,
    order_id bigint references orders (id),
    booking_id bigint references bookings (id),
    recipient_email varchar(254) not null,
    recipient_name varchar(240),
    subject varchar(240) not null,
    body_text text not null,
    body_html text,
    status varchar(20) not null default 'PENDING',
    attempt_count integer not null default 0,
    last_error text,
    deduplication_key varchar(220) unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    sent_at timestamptz,
    constraint chk_customer_notifications_channel check (channel in ('EMAIL')),
    constraint chk_customer_notifications_status check (status in ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    constraint chk_customer_notifications_type check (
        notification_type in (
            'ORDER_RECEIVED',
            'ORDER_ACCEPTED',
            'ORDER_READY',
            'ORDER_CANCELLED',
            'BOOKING_REQUEST_RECEIVED',
            'BOOKING_CONFIRMED',
            'BOOKING_DECLINED',
            'BOOKING_CANCELLED',
            'MANUAL_MESSAGE'
        )
    ),
    constraint chk_customer_notifications_attempt_count_non_negative check (attempt_count >= 0),
    constraint chk_customer_notifications_single_context check (
        (order_id is not null and booking_id is null)
        or
        (order_id is null and booking_id is not null)
    )
);

create index idx_customer_notifications_status_created_at
    on customer_notifications (status, created_at);

create index idx_customer_notifications_order_id
    on customer_notifications (order_id);

create index idx_customer_notifications_booking_id
    on customer_notifications (booking_id);

create trigger set_customer_notifications_updated_at
before update on customer_notifications
for each row
execute function set_updated_at();
