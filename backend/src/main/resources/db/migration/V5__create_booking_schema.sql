create table bookings (
    id bigserial primary key,
    booking_reference varchar(32) not null unique,
    status varchar(20) not null check (status in (
        'REQUESTED',
        'CONFIRMED',
        'DECLINED',
        'CANCELLED',
        'COMPLETED',
        'NO_SHOW'
    )),
    booking_date date not null,
    booking_time time not null,
    party_size integer not null check (party_size >= 1),
    first_name varchar(120) not null,
    last_name varchar(120) not null,
    phone varchar(60) not null,
    email varchar(254) not null,
    special_requests text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_bookings_booking_reference
    on bookings (booking_reference);

create index idx_bookings_booking_date
    on bookings (booking_date);

create index idx_bookings_status
    on bookings (status);

create index idx_bookings_future_sort
    on bookings (booking_date, booking_time);

create trigger set_bookings_updated_at
before update on bookings
for each row
execute function set_updated_at();
