# Basilico Admin Web

Authenticated development admin interface for Basilico - Simple Italian.

## Requirements

- Node.js compatible with Next.js 16
- npm
- Basilico Spring Boot backend running on `http://localhost:8080`

## Run Locally

```bash
npm install
npm run dev
```

The admin app runs on:

```text
http://localhost:3002
```

## Environment

Copy `.env.example` to `.env.local` if you need to change local URLs.

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_CUSTOMER_WEB_URL=http://localhost:3001
NEXT_PUBLIC_ENABLE_DEV_PAYMENT_CONTROL=false
```

Production must set explicit HTTPS URLs. Do not point production admin-web at
localhost services.

For Vercel, create the project with:

- root directory: `admin-web`
- install command: `npm install`
- build command: `npm run build`
- output: Vercel default for Next.js

Set `NEXT_PUBLIC_API_BASE_URL` to the deployed backend HTTPS origin and
`NEXT_PUBLIC_CUSTOMER_WEB_URL` to the deployed customer-web HTTPS origin.
`NEXT_PUBLIC_*` values are compiled into the browser bundle, so update/redeploy
after changing them.

## Authentication

Admin-web signs in against the Spring Boot backend using server-side Spring
Security sessions. The backend owns authentication and protects `/api/admin/**`.

The browser stores only the backend's HttpOnly session cookie. Admin-web does
not store auth tokens in `localStorage` or `sessionStorage`.

CSRF is enabled on the backend. Admin-web requests the Spring CSRF token and
sends it on protected state-changing admin requests.

Local login URL:

```text
http://localhost:3002/login
```

The initial local owner account is bootstrapped by backend environment
variables. Do not put real credentials in this repository.

This app still should not be publicly deployed until HTTPS, production secrets
and operational hosting are complete. The backend remains the real security
boundary; admin-web route redirects are user experience only.

When admin-web and backend are deployed on HTTPS subdomains of the same
registrable domain, the current `SameSite=lax` session cookie setting is the
preferred first choice. If the final architecture uses truly different sites,
set `SESSION_COOKIE_SAME_SITE=none` on the backend and keep
`SESSION_COOKIE_SECURE=true`; do not disable CSRF.

Admin-web is marked `noindex,nofollow` with metadata, `robots.txt` and an
`X-Robots-Tag` response header. These are indexing hints, not authentication.

## Payments

The Payments page uses real order and payment-attempt data from the backend.
Stripe Checkout Sessions and webhooks are the authoritative payment flow.

The old manual payment-status control is hidden by default. It appears only when
admin-web is started with:

```text
NEXT_PUBLIC_ENABLE_DEV_PAYMENT_CONTROL=true
```

The backend also rejects that mutation unless:

```text
BASILICO_ALLOW_MANUAL_PAYMENT_STATUS=true
```

Keep both disabled outside deliberate local development testing.

## Current Features

- Dashboard using real order, booking and menu data
- Orders list and order detail
- Order status controls
- Stripe payment-attempt visibility on order detail
- Development payment status control when explicitly enabled
- Bookings list and booking detail
- Booking status controls
- Messages page for customer email notification history
- Order and booking communication panels for manual customer email
- Menu item list grouped by real category order
- Sold out, featured and archive controls
- Menu item create and edit forms
- Payment status view derived from orders
- Fulfilment settings for collection, Radius delivery, delivery pricing and
  legacy postcode rules

## Customer Email Messaging

The Messages route reads real `customer_notifications` rows from the backend.
It shows automatic transactional emails and manual admin messages with status:

- `PENDING`
- `SENDING`
- `SENT`
- `FAILED`

Order detail and booking detail include a Communication panel. Admin staff can
send a manual email to the stored customer email for that order or booking
without retyping the recipient. The backend sends the email through SMTP and
records the result. Admin-web only says an email was sent when the backend
returns `SENT`.

Failed emails can be retried from Messages or from the scoped communication
history. For local testing, run backend Docker Compose and open Mailpit:

```text
http://localhost:8025
```

Mailpit SMTP runs on `localhost:1025` and catches local test emails safely.

## Production Headers

Admin-web sends conservative security headers:

- `X-Robots-Tag: noindex, nofollow`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `X-Frame-Options: DENY`
- restrictive camera, microphone and geolocation permissions policy

A Content Security Policy is intentionally not added in this phase so future
Stripe/admin integrations can be allowlisted deliberately instead of broken by
an incomplete policy.

## Fulfilment Settings

The Fulfilment route manages the backend-authoritative settings for online
ordering:

- collection enabled / disabled
- delivery enabled / disabled
- service area mode, including Radius delivery
- restaurant postcode and coordinate seed
- 6-mile delivery radius
- 20-minute preparation time
- radius-band pricing
- minimum delivery order in GBP
- legacy flat delivery charge in GBP
- legacy optional free-delivery threshold
- legacy delivery postcode-prefix rules

Delivery is seeded as disabled, but the owner-approved Radius configuration is
stored in the backend: 6 miles from Basilico, 20 minutes preparation, £2 for
0-3 miles and £1 per started additional mile up to 6 miles. Admin-web converts
GBP inputs such as `2.00` into integer pennies before sending them to Spring
Boot.

When Radius mode is active, postcode-prefix rules are preserved but marked as
inactive/not used. Driving-time estimates require
`OPENROUTESERVICE_API_KEY`; without it, delivery eligibility and fees still work
but ETA is unavailable.

## Not Included Yet

- Password reset
- Staff invitation / role-management UI
- Stripe refunds, disputes or production reconciliation tools
- SMS, WhatsApp or customer conversation history
- Image upload
- Menu category CRUD
