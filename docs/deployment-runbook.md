# Basilico Vercel + Render Deployment Runbook

This runbook prepares a production-like deployment. It does not include real
secrets, DNS changes, live Stripe switching or delivery enablement.

## A. Create Render PostgreSQL

1. Create a Render PostgreSQL database named `basilico-postgres`.
2. Use database name `basilico_db`.
3. Use database user `basilico`.
4. Choose the same Render region as the backend web service.

Verify:

- database status is available
- connection details show host, port, database, user and password
- automated backups are enabled on the selected plan before live launch

## B. Create Render Backend

Preferred: create from the repository root `render.yaml`.

Manual fallback:

- service type: Web Service
- runtime: Docker
- root directory: `backend`
- Dockerfile path: `./Dockerfile`
- health check path: `/api/health`
- auto deploy: keep disabled until staging QA is repeatable

Verify:

- Render builds the Docker image with Java 21
- the Docker build uses `./mvnw -B -DskipTests clean package`
- the running service starts `java -jar /app/app.jar`

## C. Configure Backend Environment

Required at first deployment:

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=<Render PostgreSQL host>
DB_PORT=<Render PostgreSQL port>
DB_NAME=<Render PostgreSQL database>
DB_USER=<Render PostgreSQL user>
DB_PASSWORD=<Render PostgreSQL password>
BASILICO_CORS_ALLOWED_ORIGINS=https://CUSTOMER_DOMAIN,https://ADMIN_DOMAIN
CUSTOMER_WEB_BASE_URL=https://CUSTOMER_DOMAIN
ADMIN_SESSION_TIMEOUT=365d
ADMIN_SESSION_COOKIE_MAX_AGE=365d
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=<lax-or-none>
BASILICO_ADMIN_EMAIL=<owner email>
BASILICO_ADMIN_PASSWORD=<owner bootstrap password>
BASILICO_ADMIN_DISPLAY_NAME=<owner display name>
```

For Google Cloud Run with Neon PostgreSQL, set the same values as Cloud Run
environment variables or Secret Manager-backed variables. Use the Neon host,
database, user and password for `DB_HOST`, `DB_NAME`, `DB_USER` and
`DB_PASSWORD`; keep `DB_JDBC_PARAMETERS=?sslmode=require` if the selected Neon
connection string requires SSL parameters. Keep `ADMIN_SESSION_TIMEOUT=365d`
and `ADMIN_SESSION_COOKIE_MAX_AGE=365d` together so the PostgreSQL session row
and the browser's persistent `SESSION` cookie expire on the same schedule.

Use `SESSION_COOKIE_SAME_SITE=lax` when the admin and API URLs are same-site
subdomains, such as `admin.example.com` and `api.example.com`. Use
`SESSION_COOKIE_SAME_SITE=none` with `SESSION_COOKIE_SECURE=true` when testing
admin-web on a Vercel domain against the Render service domain, because that is
cross-site browser traffic.

Optional first-deploy values:

```text
DB_JDBC_PARAMETERS=?sslmode=require
MAIL_WORKER_ENABLED=false
BASILICO_ALLOW_MANUAL_PAYMENT_STATUS=false
```

Verify:

- no production secret is committed to Git
- backend logs do not print plaintext passwords, Stripe secrets or SMTP secrets
- if the owner exists already, bootstrap does not overwrite the password

## D. Run and Verify Flyway

Render runs the app and Flyway automatically at startup.

Verify from backend logs:

- migrations validate successfully
- current schema advances through all repository migrations
- `V19__create_spring_session_tables.sql` creates `SPRING_SESSION` and `SPRING_SESSION_ATTRIBUTES`
- no QA/test orders, bookings or payment attempts are seeded
- menu seed migrations create the approved Basilico menu
- fulfilment defaults are collection enabled and delivery disabled

Manual API verification:

```sh
curl https://API_DOMAIN/api/health
curl https://API_DOMAIN/actuator/health
curl https://API_DOMAIN/api/menu
```

Expected:

- `/api/health` returns `status: UP`
- actuator health reports `UP`
- public menu returns 32 active products plus Create Your Own config

## E. Create Vercel Customer Project

Create a Vercel project from the same repository.

Settings:

- framework: Next.js
- root directory: `customer-web`
- install command: `npm install`
- build command: `npm run build`

## F. Configure Customer Environment

Set:

```text
NEXT_PUBLIC_SITE_URL=https://CUSTOMER_DOMAIN
NEXT_PUBLIC_API_BASE_URL=https://API_DOMAIN
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=<Stripe test publishable key for staging>
```

Verify:

- the values are set for the intended Vercel environment
- `NEXT_PUBLIC_*` changes are followed by a redeploy
- no backend secret key is configured in customer-web

## G. Deploy Customer

Deploy the Vercel customer project.

Verify:

- homepage loads
- `/menu` loads from the Render backend
- `/robots.txt` and `/sitemap.xml` use the configured customer URL
- basket/checkout/payment routes are not included in the sitemap

## H. Create Vercel Admin Project

Create a second Vercel project from the same repository.

Settings:

- framework: Next.js
- root directory: `admin-web`
- install command: `npm install`
- build command: `npm run build`

## I. Configure Admin Environment

Set:

```text
NEXT_PUBLIC_API_BASE_URL=https://API_DOMAIN
NEXT_PUBLIC_CUSTOMER_WEB_URL=https://CUSTOMER_DOMAIN
NEXT_PUBLIC_ENABLE_DEV_PAYMENT_CONTROL=false
```

Verify:

- no admin backend secrets are configured in Vercel
- admin-web has noindex/nofollow metadata and response headers

## J. Deploy Admin

Deploy the Vercel admin project.

Verify:

- unauthenticated `/dashboard` redirects to `/login`
- owner can sign in
- refresh keeps the session
- logout invalidates the session
- admin API requests include credentials and CSRF for mutations

## K. Configure Final Domains

After Vercel/Render preview URLs work, configure final domains.

Verify:

- customer domain resolves over HTTPS
- admin domain resolves over HTTPS
- backend/API domain resolves over HTTPS
- no app is configured to call localhost in production

## L. Update CORS and App URLs

Update Render backend:

```text
BASILICO_CORS_ALLOWED_ORIGINS=https://CUSTOMER_DOMAIN,https://ADMIN_DOMAIN
CUSTOMER_WEB_BASE_URL=https://CUSTOMER_DOMAIN
```

Update Vercel customer:

```text
NEXT_PUBLIC_SITE_URL=https://CUSTOMER_DOMAIN
NEXT_PUBLIC_API_BASE_URL=https://API_DOMAIN
```

Update Vercel admin:

```text
NEXT_PUBLIC_API_BASE_URL=https://API_DOMAIN
NEXT_PUBLIC_CUSTOMER_WEB_URL=https://CUSTOMER_DOMAIN
```

Verify:

- customer pages still load the menu
- admin login still works
- admin mutations still pass CSRF

## M. Configure Stripe TEST Webhook

In the Stripe Dashboard test environment, create a webhook endpoint:

```text
https://API_DOMAIN/api/payments/stripe/webhook
```

Configure Render:

```text
STRIPE_SECRET_KEY=<Stripe test secret key>
STRIPE_WEBHOOK_SECRET=<Dashboard whsec for deployed endpoint>
```

Configure Vercel customer:

```text
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=<Stripe test publishable key>
```

Verify:

- use only test keys
- the webhook secret is for the deployed endpoint, not the local Stripe CLI
- successful test payment moves order to `PAID` + `NEW`
- failed/expired payment leaves basket retry flow intact

## N. Configure SMTP

After choosing a production SMTP provider, configure Render:

```text
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM_EMAIL=
MAIL_FROM_NAME=Basilico - Simple Italian
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_WORKER_ENABLED=true
```

Verify:

- sender address/domain is verified with the SMTP provider
- test order/booking emails arrive
- failed notifications are recorded and retryable
- Mailpit is not used in production

## O. Perform Staging QA

Run the staging/live QA checklist below before switching Stripe live.

## P. Switch Stripe LIVE Later

Only after staging QA passes:

1. Replace Stripe test keys with live keys.
2. Create a live-mode webhook endpoint for `https://API_DOMAIN/api/payments/stripe/webhook`.
3. Replace `STRIPE_WEBHOOK_SECRET` with the live endpoint `whsec_...`.
4. Redeploy/restart affected services.
5. Repeat payment success/failure/idempotency QA.

Do not enable delivery until the owner-approved 6-mile Radius settings, fee
bands and local delivery QA have been verified in the deployed environment.

## Staging / Live QA Checklist

Customer:

- [ ] homepage loads
- [ ] menu loads from backend
- [ ] dish page loads
- [ ] sold-out item appears as sold out
- [ ] sold-out item cannot be added to basket
- [ ] basket quantity and subtotal work
- [ ] collection checkout creates order
- [ ] Stripe test successful payment updates order to `PAID` + `NEW`
- [ ] failed Stripe payment shows retry-safe UX
- [ ] booking request submits and shows reference
- [ ] mobile customer QA completed
- [ ] desktop customer QA completed

Admin:

- [ ] login works
- [ ] refresh preserves authenticated session
- [ ] backend restart or Cloud Run scale-to-zero/cold start preserves authenticated session
- [ ] logout works
- [ ] dashboard loads real data
- [ ] order workflow can move `NEW` -> `ACCEPTED` -> `PREPARING` -> `READY`
- [ ] booking workflow can confirm and decline requests
- [ ] menu sold-out toggle updates customer site
- [ ] temporary menu price edit updates customer site and is restored
- [ ] Messages page shows notifications
- [ ] manual email message sends through configured SMTP
- [ ] failed notification retry works
- [ ] payment status view reflects real orders
- [ ] admin mobile/tablet QA completed
- [ ] admin desktop QA completed

Security / operations:

- [ ] `/api/admin/**` unauthenticated requests return 401
- [ ] admin mutation without CSRF returns 403
- [ ] `/api/health` returns 200
- [ ] `/actuator/health` reports `UP`
- [ ] admin noindex/nofollow headers are present
- [ ] production CORS uses explicit HTTPS origins only
- [ ] session cookie is HttpOnly, Secure and has the expected persistent Max-Age
- [ ] no production app calls localhost
- [ ] delivery remains disabled until final owner launch approval
- [ ] Radius delivery settings match the owner-approved 6-mile / £2-£5 bands
- [ ] legacy postcode rules are not used while Radius mode is active
