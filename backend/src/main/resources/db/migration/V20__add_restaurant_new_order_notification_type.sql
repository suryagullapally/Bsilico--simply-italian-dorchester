alter table customer_notifications
drop constraint chk_customer_notifications_type;

alter table customer_notifications
add constraint chk_customer_notifications_type check (
    notification_type in (
        'ORDER_RECEIVED',
        'ORDER_ACCEPTED',
        'ORDER_READY',
        'ORDER_CANCELLED',
        'BOOKING_REQUEST_RECEIVED',
        'BOOKING_CONFIRMED',
        'BOOKING_DECLINED',
        'BOOKING_CANCELLED',
        'RESTAURANT_NEW_ORDER',
        'MANUAL_MESSAGE'
    )
);
