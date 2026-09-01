alter table fulfilment_settings
    add column delivery_area_mode varchar(30) not null default 'POSTCODE_RULES',
    add column restaurant_postcode varchar(20),
    add column restaurant_latitude numeric(9, 6),
    add column restaurant_longitude numeric(9, 6),
    add column delivery_radius_miles numeric(5, 2),
    add column preparation_time_minutes integer,
    add column delivery_pricing_mode varchar(30) not null default 'FLAT_FEE',
    add column base_delivery_radius_miles numeric(5, 2),
    add column base_delivery_fee_pence integer,
    add column extra_mile_fee_pence integer;

alter table fulfilment_settings
    add constraint chk_fulfilment_delivery_area_mode
        check (delivery_area_mode in ('POSTCODE_RULES', 'RADIUS')),
    add constraint chk_fulfilment_delivery_pricing_mode
        check (delivery_pricing_mode in ('FLAT_FEE', 'RADIUS_BANDS')),
    add constraint chk_fulfilment_restaurant_latitude
        check (restaurant_latitude is null or restaurant_latitude between -90 and 90),
    add constraint chk_fulfilment_restaurant_longitude
        check (restaurant_longitude is null or restaurant_longitude between -180 and 180),
    add constraint chk_fulfilment_delivery_radius_miles
        check (delivery_radius_miles is null or delivery_radius_miles > 0),
    add constraint chk_fulfilment_preparation_time_minutes
        check (preparation_time_minutes is null or preparation_time_minutes >= 0),
    add constraint chk_fulfilment_base_delivery_radius_miles
        check (base_delivery_radius_miles is null or base_delivery_radius_miles >= 0),
    add constraint chk_fulfilment_base_delivery_fee_pence
        check (base_delivery_fee_pence is null or base_delivery_fee_pence >= 0),
    add constraint chk_fulfilment_extra_mile_fee_pence
        check (extra_mile_fee_pence is null or extra_mile_fee_pence >= 0);

update fulfilment_settings
set delivery_area_mode = 'RADIUS',
    restaurant_postcode = 'DT1 1TT',
    restaurant_latitude = 50.714050,
    restaurant_longitude = -2.438190,
    delivery_radius_miles = 6.00,
    preparation_time_minutes = 20,
    delivery_pricing_mode = 'RADIUS_BANDS',
    base_delivery_radius_miles = 3.00,
    base_delivery_fee_pence = 200,
    extra_mile_fee_pence = 100;

alter table orders
    add column delivery_distance_miles numeric(6, 2),
    add column delivery_preparation_minutes integer,
    add column delivery_travel_minutes integer,
    add column estimated_delivery_minutes integer;

alter table orders
    add constraint chk_orders_delivery_distance_miles
        check (delivery_distance_miles is null or delivery_distance_miles >= 0),
    add constraint chk_orders_delivery_preparation_minutes
        check (delivery_preparation_minutes is null or delivery_preparation_minutes >= 0),
    add constraint chk_orders_delivery_travel_minutes
        check (delivery_travel_minutes is null or delivery_travel_minutes >= 0),
    add constraint chk_orders_estimated_delivery_minutes
        check (estimated_delivery_minutes is null or estimated_delivery_minutes >= 0);
