# Basilico Deployment Architecture

This is the current deployment-preparation shape for the Basilico platform. No
domain or production credential has been selected in this repository.

Target platforms for the first production-like deployment:

- customer-web: Vercel
- admin-web: Vercel
- backend API: Render Web Service
- database: Render PostgreSQL

## Runtime Layout

```text
customer-web on Vercel
  HTTPS
    |
    v
backend API on Render
    |
    v
Render PostgreSQL

admin-web on Vercel
  HTTPS + Spring Security session auth
    |
    v
backend API on Render

Stripe
  signed webhook
    |
    v
backend API on Render

backend API on Render
    |
    v
SMTP provider
```

## Applications

Customer web:

- Public Next.js customer website.
- Reads the live menu, fulfilment settings and payment state from the backend.
- Uses the Stripe publishable key only.
- Does not store backend secrets.

Admin web:

- Private Next.js operations interface.
- Protected by Spring Security sessions on the backend.
- Must run over HTTPS in production so admin cookies can be `Secure`.
- Is marked `noindex,nofollow`, but robots settings are not a security boundary.

Backend:

- Spring Boot API.
- Built for Render with a Java 21 Docker image because Render's native runtimes
  do not include Java.
- Owns authentication, CSRF, pricing, payment state, fulfilment state and email
  delivery.
- Runs Flyway migrations forward at startup.
- Uses environment variables or a secret manager for all production secrets.
- Reads Render's `PORT` variable and falls back to local `SERVER_PORT` / `8080`.

Database:

- Render PostgreSQL is the target managed PostgreSQL service for the first
  deployment.
- Hibernate `ddl-auto=validate` remains enabled.
- Flyway is the only schema-change mechanism.
- The backend keeps the existing `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and
  `DB_PASSWORD` variables. Add `DB_JDBC_PARAMETERS` only if the selected Render
  connection mode requires extra JDBC options such as SSL mode.

Stripe:

- Checkout Sessions are created by the backend from persisted order snapshots.
- The browser never sends authoritative totals.
- Stripe webhook signing verifies payment events.
- Duplicate webhook events are stored and handled idempotently.

SMTP:

- Backend sends transactional email through generic SMTP.
- Local development uses Mailpit only.
- Production should use a verified SMTP provider/sender domain.

## HTTPS

Production must use HTTPS for:

- customer website
- admin website
- backend API
- Stripe return URLs
- Stripe webhook endpoint

Do not deploy Stripe live payment or admin-web over plain HTTP.

## CORS

Production CORS must use explicit origins through:

```text
BASILICO_CORS_ALLOWED_ORIGINS=https://CUSTOMER_DOMAIN,https://ADMIN_DOMAIN
```

Do not use `*`, especially because admin requests use credentials.

Stripe webhooks are server-to-server and do not depend on browser CORS.

## Admin Session Cookies

Backend sessions remain server-side Spring Security sessions. The browser
receives an HttpOnly `SESSION` cookie; admin-web uses `credentials: include`.
Spring Session JDBC stores the session rows in the primary PostgreSQL database
so admin sessions survive backend restarts, rolling deployments and Cloud Run
scale-to-zero cold starts.

For the preferred domain shape:

```text
customer: https://CUSTOMER_DOMAIN
admin:    https://ADMIN_DOMAIN
api:      https://API_DOMAIN
```

If `ADMIN_DOMAIN` and `API_DOMAIN` are subdomains of the same registrable
domain, keep:

```text
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=lax
ADMIN_SESSION_TIMEOUT=365d
ADMIN_SESSION_COOKIE_MAX_AGE=365d
```

If the final admin and API origins are truly cross-site, use:

```text
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=none
```

Do not disable CSRF for production cross-origin authentication.

## Database Backups

The chosen PostgreSQL provider should support:

- automatic daily backups
- point-in-time recovery where available
- manual snapshot creation before risky migrations

A manual provider-neutral backup command looks like:

```sh
pg_dump "$DATABASE_URL" > basilico-backup-$(date +%Y%m%d-%H%M%S).sql
```

Store backups outside the application server and protect them as sensitive data.

## Migration Policy

Do not edit migrations after they have been applied to shared or production
databases.

If a migration needs correction, create the next forward migration, for example:

```text
V14__fix_example_schema_issue.sql
```

Rollback should be handled by restoring a database backup or applying a
deliberate forward-fix migration. Do not rewrite production migration history.

## Production Cutover Notes

Before launch:

- set real production domains
- configure HTTPS
- configure Render PostgreSQL credentials and backups
- configure explicit CORS origins
- configure secure admin session cookies
- configure Stripe test keys and a deployed test-mode webhook first
- switch to Stripe live keys only after staging QA passes
- configure verified SMTP credentials
- create and test the production owner admin account
- keep delivery disabled until owner-approved Radius delivery rules are
  verified in the deployed environment
