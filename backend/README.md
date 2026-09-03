# Basilico Backend

Spring Boot foundation for the Basilico restaurant platform.

## Required Software

- Java 21
- Maven Wrapper, included as `./mvnw`
- Docker

You do not need a globally installed Maven if you use the wrapper.

## Local Environment

Configuration is read from environment variables. Copy the example file if you want local shell defaults:

```sh
cp .env.example .env
```

When running from the `backend` directory, Spring Boot also imports
`backend/.env` automatically through `spring.config.import`. You do not need to
run `source .env` before `./mvnw spring-boot:run`.

The local example values are:

```txt
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=basilico_db
DB_USER=basilico
DB_PASSWORD=change-me
DB_JDBC_PARAMETERS=
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_CONNECTION_TIMEOUT_MS=30000
DB_VALIDATION_TIMEOUT_MS=5000
DB_MAX_LIFETIME_MS=1800000
BASILICO_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001,http://localhost:3002
BASILICO_ADMIN_EMAIL=
BASILICO_ADMIN_PASSWORD=
BASILICO_ADMIN_DISPLAY_NAME=
ADMIN_SESSION_TIMEOUT=8h
ADMIN_SESSION_COOKIE_MAX_AGE=8h
SESSION_COOKIE_SECURE=false
SESSION_COOKIE_SAME_SITE=lax
RATE_LIMIT_ENABLED=true
RATE_LIMIT_WINDOW_SECONDS=60
RATE_LIMIT_ORDER_CREATE_LIMIT=120
RATE_LIMIT_BOOKING_CREATE_LIMIT=60
RATE_LIMIT_CHECKOUT_SESSION_LIMIT=120
RATE_LIMIT_DELIVERY_QUOTE_LIMIT=120
RATE_LIMIT_ADMIN_LOGIN_LIMIT=10
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
CUSTOMER_WEB_BASE_URL=http://localhost:3000
ADMIN_WEB_BASE_URL=http://localhost:3002
BASILICO_ALLOW_MANUAL_PAYMENT_STATUS=false
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM_EMAIL=orders@basilico.local
MAIL_FROM_NAME=Basilico - Simple Italian
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS=false
MAIL_MAX_ATTEMPTS=3
MAIL_BATCH_SIZE=10
MAIL_RETRY_TRIGGER_TOKEN=
MAIL_WORKER_ENABLED=true
MAIL_RETRY_DELAY_MS=30000
MAIL_INITIAL_DELAY_MS=5000
MAIL_HEALTH_ENABLED=false
BASILICO_ORDER_ALERT_EMAIL=
POSTCODES_IO_BASE_URL=https://api.postcodes.io
POSTCODES_IO_TIMEOUT_MS=2500
POSTCODES_IO_CACHE_TTL_SECONDS=86400
POSTCODES_IO_CACHE_MAX_ENTRIES=500
OPENROUTESERVICE_API_KEY=
OPENROUTESERVICE_BASE_URL=https://api.heigit.org/openrouteservice
OPENROUTESERVICE_TIMEOUT_MS=3500
OPENROUTESERVICE_CACHE_TTL_SECONDS=900
OPENROUTESERVICE_CACHE_MAX_ENTRIES=500
```

Do not commit a real `.env` file.

Real OS and deployment environment variables have higher precedence than the
local `.env` file, so production secrets can override local development values.

The definitive environment-variable list is documented in:

- `../docs/environment-variables.md`

Production should run with `SPRING_PROFILES_ACTIVE=prod`. The production
profile keeps secrets environment-driven, enables secure cookies by default,
expects explicit CORS origins, avoids Mailpit localhost defaults, and keeps
Hibernate schema management at `ddl-auto=validate`.

## Render Deployment Preparation

Render currently runs JVM applications most reliably through Docker because
Java is not one of Render's native language runtimes. The repository includes a
backend Docker image definition at:

- `Dockerfile`

The image uses Java 21, builds with the Maven wrapper, skips tests during image
build, and starts the packaged Spring Boot jar:

```sh
./mvnw -B -DskipTests clean package
java -jar /app/app.jar
```

The repository root includes `render.yaml` for a Render Blueprint containing:

- one Docker web service rooted at `backend`
- one Render PostgreSQL database
- `/api/health` as the service health check
- `SPRING_PROFILES_ACTIVE=prod`
- database values mapped into the existing `DB_*` variables
- secret/application values marked for dashboard configuration

Render supplies the runtime `PORT` variable automatically. Spring Boot reads
`PORT` first and falls back to `SERVER_PORT`, then `8080`, so local development
continues to use port `8080`.

Do not put Stripe, SMTP, admin or database secrets in `render.yaml`. Configure
them in Render environment variables or a secret manager.

If the production PostgreSQL connection requires TLS, set:

```txt
DB_JDBC_PARAMETERS=?sslmode=require
```

Only set provider-specific JDBC parameters that are required by the chosen
connection mode.

## Start PostgreSQL

```sh
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with a named Docker volume so local data survives container restarts. It also starts Mailpit for safe local email testing:

- SMTP: `localhost:1025`
- Web UI: http://localhost:8025

Local Mailpit catches development emails inside your machine. Do not configure
local testing to send email to real customers.

To check the container:

```sh
docker compose ps
```

## Run Spring Boot

```sh
./mvnw spring-boot:run
```

The application runs on port `8080`.

Health URLs:

- http://localhost:8080/api/health
- http://localhost:8080/actuator/health

## Admin Authentication

Admin APIs are protected by Spring Security using server-side sessions.
Admin-web signs in with email and password, then the browser receives an
HttpOnly `SESSION` cookie from the backend. Admin-web does not store
authentication tokens in `localStorage` or `sessionStorage`.

Spring Session JDBC stores admin sessions in the primary PostgreSQL database.
In production this means authenticated admin sessions survive Cloud Run
instance restarts, rolling deploys and scale-to-zero cold starts, as long as the
same Neon PostgreSQL database is attached.

Passwords are stored with BCrypt hashes only. Plaintext passwords are never
stored, logged or returned from the API.

CSRF protection is enabled for authenticated admin state-changing requests.
Admin clients should first request:

- `GET /api/admin/auth/csrf`

and then send the returned token in the returned header name for protected
`POST`, `PUT`, `PATCH` and `DELETE` requests.

Authentication endpoints:

- `GET /api/admin/auth/csrf`
- `POST /api/admin/auth/login`
- `GET /api/admin/auth/me`
- `POST /api/admin/auth/logout`

The first local owner account can be bootstrapped at application startup with:

```txt
BASILICO_ADMIN_EMAIL=
BASILICO_ADMIN_PASSWORD=
BASILICO_ADMIN_DISPLAY_NAME=
```

If those values are present and no user with that email exists, the backend
creates an active `OWNER` account. If the user already exists, the bootstrapper
does not create a duplicate and does not overwrite the password. Missing
bootstrap values allow the app to start normally, with a warning that no owner
account was created.

Initial roles are:

- `OWNER`
- `MANAGER`
- `STAFF`

For this phase, all active roles can access authenticated admin APIs. Finer
permissions, staff invitations and password management will be added later.

Local development uses non-secure cookies so `localhost` works. Production must
run behind HTTPS with secure session cookies and real secret management.

Production session cookies are configured by environment:

```txt
ADMIN_SESSION_TIMEOUT=365d
ADMIN_SESSION_COOKIE_MAX_AGE=365d
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=lax
```

`ADMIN_SESSION_TIMEOUT` controls inactive session expiry in the database.
`ADMIN_SESSION_COOKIE_MAX_AGE` controls how long the browser keeps the
persistent `SESSION` cookie. Keep both aligned for long-lived production admin
sessions. The `prod` profile defaults both values to `365d`; local development
keeps the shorter `8h` default.

Flyway migration `V19__create_spring_session_tables.sql` creates the
`SPRING_SESSION` and `SPRING_SESSION_ATTRIBUTES` tables used by Spring Session
JDBC. Schema initialization is disabled in Spring Session itself so Flyway
remains the only schema owner.

## Production CORS

CORS is centralized and environment-driven:

```txt
BASILICO_CORS_ALLOWED_ORIGINS=https://customer.example,https://admin.example
```

Local development defaults allow:

- `http://localhost:3000`
- `http://localhost:3001`
- `http://localhost:3002`

Do not use `*`, especially because admin requests use credentialed sessions.
Production deployments must set explicit HTTPS origins.

## Rate Limiting

The backend includes lightweight in-process fixed-window rate limiting for
abuse-sensitive public/admin endpoints:

- `POST /api/orders`
- `POST /api/bookings`
- `POST /api/payments/checkout-session`
- `POST /api/fulfilment/delivery-quote`
- `POST /api/admin/auth/login`

Stripe webhooks are not rate-limited by this filter so legitimate Stripe retry
delivery is not discarded.

Configuration:

```txt
RATE_LIMIT_ENABLED=true
RATE_LIMIT_WINDOW_SECONDS=60
RATE_LIMIT_ORDER_CREATE_LIMIT=120
RATE_LIMIT_BOOKING_CREATE_LIMIT=60
RATE_LIMIT_CHECKOUT_SESSION_LIMIT=120
RATE_LIMIT_DELIVERY_QUOTE_LIMIT=120
RATE_LIMIT_ADMIN_LOGIN_LIMIT=10
```

When a limit is reached, the API returns HTTP `429` with clean JSON and a
`Retry-After` header. This is an initial hardening layer, not a substitute for
edge/network-level rate limiting at deployment.

## Request Size and Input Safety

Customer notes, booking special requests, admin manual messages, menu
descriptions, names, email addresses, image paths and postcode inputs are
bounded with Jakarta Validation before they reach persistence. User-controlled
text is rendered as text in the frontend, and email HTML is generated from
escaped plain text. The API does not accept arbitrary admin-entered HTML for
manual customer messages.

## Public Menu API

The public menu API returns active menu categories and active menu items.
Items that are active but temporarily unavailable are still returned with `available: false` so the frontend can show a sold-out state later.

- `GET /api/menu`
- `GET /api/menu/items/{slug}`
- `GET /api/menu/customizers/create-your-own`

Prices are stored and exposed in integer pennies:

```json
{
  "pricePence": 899
}
```

The Create Your Own customizer exposes:

```json
{
  "basePricePence": 700,
  "extraToppingPricePence": 125
}
```

Frontend applications should format pence into GBP for display.

## Fulfilment Configuration

Fulfilment is configured in PostgreSQL and exposed through:

- `GET /api/fulfilment/options`
- `POST /api/fulfilment/check-delivery`
- `POST /api/fulfilment/delivery-quote`

The seeded Basilico configuration is:

- collection enabled
- delivery disabled
- delivery area mode: `RADIUS`
- restaurant postcode: `DT1 1TT`
- restaurant coordinate seed: latitude `50.714050`, longitude `-2.438190`
- delivery radius: 6.0 miles
- preparation time: 20 minutes
- delivery pricing mode: `RADIUS_BANDS`
- first 3 miles: 200 pence
- each started additional mile: 100 pence
- no minimum delivery order unless explicitly configured

Do not treat delivery as live until Basilico has completed local QA and
explicitly enables delivery in Admin.

Radius delivery uses straight-line Haversine distance from the configured
Basilico coordinate to the geocoded customer postcode or browser geolocation.
The authoritative 6-mile rule and fee calculation happen in Spring Boot.
Customer-web quotes are previews only; `POST /api/orders` revalidates the
delivery postcode and recalculates the fee before the order is stored.

Radius-band pricing uses started extra miles:

- `0-3.00` miles inclusive: £2.00
- `>3.00-4.00` miles: £3.00
- `>4.00-5.00` miles: £4.00
- `>5.00-6.00` miles: £5.00
- over `6.00` miles: delivery rejected

Postcode-prefix delivery rules are preserved for backwards compatibility and
future use, but they are not authoritative when `delivery_area_mode=RADIUS`.
Legacy flat-fee/free-delivery settings are also preserved but the
free-delivery threshold is not used by `RADIUS_BANDS`.

Postcode geocoding uses Postcodes.io from the backend. If a postcode cannot be
verified, delivery is rejected safely and collection remains available. Driving
time estimates use openrouteservice/HeiGIT from the backend when
`OPENROUTESERVICE_API_KEY` is configured. If that provider is unavailable or no
key is set, radius eligibility and delivery fee still work, and the ETA is
returned as unavailable rather than guessed.

Customer-facing ETA is:

```txt
20 minute preparation time + driving duration, rounded up to the next 5 minutes
```

Orders now store:

- `subtotalPence`: food subtotal from item snapshots
- `deliveryFeePence`: fulfilment fee snapshot, zero for collection
- `deliveryDistanceMiles`: rounded radius distance snapshot for delivery orders
- `deliveryPreparationMinutes`: preparation-time snapshot
- `deliveryTravelMinutes`: route-provider travel-time snapshot if available
- `estimatedDeliveryMinutes`: customer-facing delivery estimate if available
- `totalPence`: payable total used by Stripe

Stripe Checkout Sessions are created from `totalPence`. If a non-zero delivery
fee exists, Stripe receives a separate `Delivery` line item. Historical orders
migrated before fulfilment settings have `deliveryFeePence=0` and
`totalPence=subtotalPence`.

## Authenticated Admin Menu API

All `/api/admin/**` menu endpoints require an authenticated active admin
session and CSRF protection for mutations.

- `GET /api/admin/menu/items`
- `GET /api/admin/menu/items/{id}`
- `POST /api/admin/menu/items`
- `PUT /api/admin/menu/items/{id}`
- `PATCH /api/admin/menu/items/{id}/availability`
- `PATCH /api/admin/menu/items/{id}/active`
- `PATCH /api/admin/menu/items/{id}/featured`

Menu items are never hard-deleted through the API. Set `active` to `false` to archive an item and remove it from the public menu. Set `available` to `false` when a dish is temporarily sold out but should remain visible for future sold-out UI.

Slugs are editable on create only for this first admin API. Existing item slugs are intentionally immutable on `PUT` so public product URLs remain stable while order and admin history are still being designed.

## Public Orders API

`POST /api/orders` creates an online order ready for payment:

```json
{
  "fulfilmentType": "COLLECTION",
  "customer": {
    "firstName": "John",
    "lastName": "Smith",
    "phone": "07123456789",
    "email": "john@example.com"
  },
  "timing": {
    "type": "ASAP"
  },
  "notes": "No onions please",
  "items": [
    {
      "type": "MENU_ITEM",
      "menuItemId": 9,
      "quantity": 1
    }
  ]
}
```

Order timing is validated by the backend using Basilico's authoritative
restaurant schedule:

- timezone: `Europe/London`
- Monday, Wednesday, Thursday, Friday, Saturday and Sunday: `12:00-23:00`
- Tuesday: closed
- final order/request slot: `22:45`
- scheduled slots: 15-minute intervals

`ASAP` is accepted only while Basilico is inside the current order window in
`Europe/London`. Customers can still build a basket while closed, but they must
choose a valid scheduled slot. Today's scheduled slots that have already passed
are rejected by the backend even if a stale browser page submits them.

`GET /api/fulfilment/options` includes an `orderAvailability` block with the
current restaurant timezone, open/closed ordering state, next available slot and
date-specific scheduled slots for the customer checkout UI.

For delivery orders, include:

```json
{
  "deliveryAddress": {
    "line1": "41 Example Street",
    "line2": "",
    "city": "Dorchester",
    "postcode": "DT1 1AA"
  }
}
```

For Create Your Own pizzas, send database IDs only:

```json
{
  "type": "CUSTOM_PIZZA",
  "customizerId": 1,
  "toppingIds": [2, 5],
  "quantity": 1
}
```

The backend never trusts product prices, line totals or subtotals sent by the browser. It reloads the current menu item, customizer and topping records from PostgreSQL, calculates prices in integer pennies, and stores order snapshots for historical accuracy.

For delivery orders the backend also validates fulfilment settings, postcode
eligibility and delivery minimums, then calculates the delivery fee and final
payable total server-side. Browser-submitted fees or totals are ignored.

New online orders start as:

- `status`: `PENDING_PAYMENT`
- `paymentStatus`: `UNPAID`

Orders are not marked as paid by the browser. They remain pending payment until
Stripe confirms payment through the signed webhook endpoint.

Order statuses:

- `PENDING_PAYMENT`
- `NEW`
- `ACCEPTED`
- `PREPARING`
- `READY`
- `COMPLETED`
- `CANCELLED`

Payment statuses:

- `UNPAID`
- `PAID`
- `FAILED`
- `REFUNDED`

## Public Payments API

Stripe payments use Checkout Sessions with the embedded Payment Element flow.
The Java SDK exposes this as `ui_mode=elements`, which is the SDK enum used for
the current custom Checkout UI integration.

Public payment endpoints:

- `POST /api/payments/checkout-session`
- `GET /api/payments/checkout-session/{sessionId}/status`
- `POST /api/payments/stripe/webhook`

To start payment, the customer frontend sends only the order reference:

```json
{
  "orderReference": "BAS-20260827-A7K4P2"
}
```

The backend reloads the persisted order and item snapshots, verifies the stored
subtotal, and creates Stripe line items from server-side data. Browser-supplied
prices, totals or currency values are not accepted as authoritative input.

Payment attempts are stored in `payment_attempts`. The backend reuses an open
Stripe checkout session for the same order where possible, rather than creating
unnecessary duplicate sessions.

Stripe webhook events are stored in `stripe_webhook_events` using Stripe's event
ID as an idempotency key. Duplicate webhook deliveries are acknowledged without
reapplying state changes.

Webhook handling is authoritative:

- `checkout.session.completed` and `checkout.session.async_payment_succeeded`
  verify order reference, amount and `gbp`, mark the payment attempt `PAID`,
  store the payment intent ID, set order `paymentStatus=PAID`, and move
  `PENDING_PAYMENT` orders to `NEW`.
- Orders already in later kitchen states such as `ACCEPTED` or `PREPARING` are
  never downgraded back to `NEW`.
- `checkout.session.expired` marks only the payment attempt as `EXPIRED`; the
  order remains pending and unpaid.
- `checkout.session.async_payment_failed` marks the attempt `FAILED` and leaves
  the order not paid.

Stripe environment variables:

```txt
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
CUSTOMER_WEB_BASE_URL=
```

Do not commit real Stripe keys. Use Stripe test-mode keys locally. In
production, configure the Stripe webhook endpoint with the production
`STRIPE_WEBHOOK_SECRET` and serve the customer website over HTTPS. For the first
Vercel/Render staging pass, use Stripe test-mode keys and a deployed test-mode
webhook endpoint such as:

```txt
https://API_DOMAIN/api/payments/stripe/webhook
```

Replace the local Stripe CLI webhook secret with the Dashboard-generated secret
for that deployed endpoint.

## Authenticated Admin Orders API

All admin order endpoints require an authenticated active admin session. Status
and payment-status mutations also require CSRF protection.

- `GET /api/admin/orders`
- `GET /api/admin/orders?status=PENDING_PAYMENT`
- `GET /api/admin/orders?paymentStatus=UNPAID`
- `GET /api/admin/orders?fulfilmentType=COLLECTION`
- `GET /api/admin/orders/{id}`
- `PATCH /api/admin/orders/{id}/status`
- `PATCH /api/admin/orders/{id}/payment-status`

The payment-status patch endpoint is temporary development tooling and is
disabled by default. It returns a conflict unless:

```txt
BASILICO_ALLOW_MANUAL_PAYMENT_STATUS=true
```

Stripe webhooks are the authoritative payment path. Keep the manual payment
control disabled outside deliberate local development testing.

Orders keep snapshots of:

- product name and slug
- unit price
- selected custom pizza topping names
- topping prices

This means a historical order can still show the exact purchased item even if the live menu changes later.

## Customer Email Notifications

Transactional customer email uses Spring Boot Mail/Jakarta Mail through generic
SMTP settings. The backend is not tied to a single commercial provider; production
can use any suitable SMTP service once real credentials are configured.

SMTP variables:

```txt
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM_EMAIL=
MAIL_FROM_NAME=Basilico - Simple Italian
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_MAX_ATTEMPTS=3
MAIL_BATCH_SIZE=10
MAIL_RETRY_TRIGGER_TOKEN=
MAIL_WORKER_ENABLED=true
MAIL_HEALTH_ENABLED=false
BASILICO_ORDER_ALERT_EMAIL=
ADMIN_WEB_BASE_URL=https://admin.basilicodorchester.co.uk
```

Email delivery follows an outbox-style lifecycle:

1. A business change creates a `PENDING` row in `customer_notifications`.
2. The database transaction commits.
3. The delivery service sends the email through SMTP.
4. The notification becomes `SENT` or `FAILED`, with attempt count and a safe
   error message recorded.

Email failure does not rollback payments, orders or bookings. Failed messages
remain visible in Admin and can be retried.

For Cloud Run request-based production deployments, keep the in-process
scheduled worker disabled:

```txt
MAIL_WORKER_ENABLED=false
```

Then trigger retries externally with Google Cloud Scheduler:

```http
POST /api/internal/notifications/process-pending
X-Basilico-Retry-Token: <MAIL_RETRY_TRIGGER_TOKEN>
```

`MAIL_RETRY_TRIGGER_TOKEN` must be a long random secret. The endpoint returns a
small operational response such as `{"status":"processed","processedCount":0}`.
Missing or incorrect tokens return `401`; a missing server-side trigger token
fails closed with `503`. The token is never logged or returned.

Automatic notification triggers:

- `ORDER_RECEIVED`: only after Stripe webhook verification marks an order
  `PAID` and moves it to `NEW`.
- `ORDER_ACCEPTED`: when Admin changes an order to `ACCEPTED`.
- `ORDER_READY`: when Admin changes an order to `READY`.
- `ORDER_CANCELLED`: when Admin changes an order to `CANCELLED`.
- `RESTAURANT_NEW_ORDER`: internal restaurant alert only after Stripe webhook
  verification marks an order `PAID` and moves it to `NEW`.
- `BOOKING_REQUEST_RECEIVED`: after a public booking request is stored as
  `REQUESTED`.
- `BOOKING_CONFIRMED`, `BOOKING_DECLINED`, `BOOKING_CANCELLED`: when Admin
  changes booking status to those states.

Automatic messages use deterministic deduplication keys such as
`ORDER_RECEIVED:<order-id>` so webhook retries, repeated status saves and app
restarts do not send duplicate customer emails. Manual admin messages are
independent and are recorded separately.

The internal restaurant alert uses its own deduplication key,
`RESTAURANT_NEW_ORDER:<order-id>`, and is sent to `BASILICO_ORDER_ALERT_EMAIL`.
For Basilico production, set `BASILICO_ORDER_ALERT_EMAIL=basilico2912@gmail.com`.
If that variable is missing, payment and order status changes still succeed; the
configuration issue is logged and can be fixed without reversing the customer
payment.

Emails are plain text plus simple HTML multipart messages. They include Basilico
context, order or booking references, and customer-facing status truth. They do
not make allergen guarantees, refund promises or delivery-driver claims.

## Authenticated Admin Messages API

All message endpoints require an authenticated active admin session. Mutations
also require CSRF protection.

- `GET /api/admin/messages`
- `GET /api/admin/messages?status=FAILED`
- `GET /api/admin/messages?orderId=1`
- `GET /api/admin/messages?bookingId=1`
- `GET /api/admin/messages/{id}`
- `POST /api/admin/messages/email`
- `POST /api/admin/messages/{id}/retry`

Manual email messages must reference exactly one order or booking. The backend
resolves the stored customer email from that order/booking and does not accept
an arbitrary recipient address for this endpoint.

`POST /api/internal/notifications/process-pending` is not an admin browser API.
It is a server-to-server retry trigger for Cloud Scheduler, does not require an
admin session or CSRF token, and is protected by `MAIL_RETRY_TRIGGER_TOKEN`.

## Public Bookings API

`POST /api/bookings` creates a table-booking request:

```json
{
  "date": "2026-08-29",
  "time": "19:30",
  "partySize": 4,
  "firstName": "John",
  "lastName": "Smith",
  "phone": "07123456789",
  "email": "john@example.com",
  "specialRequests": "Birthday dinner"
}
```

Public booking submissions start as `REQUESTED`, not confirmed. Capacity and availability checking will be added later.

Basilico opening-hours validation currently allows Wednesday to Monday, 12:00 through 22:45 in 15-minute intervals. Tuesday bookings are rejected because the restaurant is closed.

Booking statuses:

- `REQUESTED`
- `CONFIRMED`
- `DECLINED`
- `CANCELLED`
- `COMPLETED`
- `NO_SHOW`

## Authenticated Admin Bookings API

All admin booking endpoints require an authenticated active admin session.
Status mutations also require CSRF protection.

- `GET /api/admin/bookings`
- `GET /api/admin/bookings?date=2026-08-29`
- `GET /api/admin/bookings?status=REQUESTED`
- `GET /api/admin/bookings/{id}`
- `PATCH /api/admin/bookings/{id}/status`

## Stop PostgreSQL

```sh
docker compose down
```

To remove the local database volume as well:

```sh
docker compose down -v
```

## Tests and Build

```sh
./mvnw test
./mvnw clean package
```

## Production Operations Notes

Provider-neutral production deployment notes live in:

- `../docs/deployment-architecture.md`
- `../docs/production-checklist.md`

Use a managed PostgreSQL service with automatic backups and point-in-time
recovery where available. Before risky migrations, take a manual backup such as
a `pg_dump` or provider snapshot.

Flyway migrations are forward-only once applied to shared or production
databases. Do not edit applied migration files; create the next migration to fix
schema or data issues.

Actuator exposure is intentionally limited to health. Do not expose `env`,
`beans`, `configprops`, `heapdump` or similar diagnostic endpoints publicly.

The backend keeps Spring Security default headers and returns concise JSON
errors. It must not log passwords, Stripe secrets, SMTP credentials or full
payment details.

## Project Architecture

- `com.basilico.backend` contains the Spring Boot application entry point.
- `config` contains shared application configuration such as local-development CORS.
- `health` contains the public API health check.
- `admin` contains authenticated admin users, bootstrap, Spring Security integration and auth DTOs/controllers.
- `menu` contains the menu database model, repositories, DTOs, services and REST controllers.
- `order` contains guest order creation, server-side price calculation, snapshot entities and authenticated admin order APIs.
- `booking` contains guest table-booking requests and authenticated admin booking APIs.
- `payment` contains Stripe Checkout Session creation, payment attempts, signed webhook handling and payment status lookup.
- `notification` contains transactional email outbox records, SMTP delivery, retry logic, templates and authenticated admin message APIs.
- `src/main/resources/db/migration` contains Flyway database migrations.
