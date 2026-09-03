# Basilico Environment Variables

Production secrets must be supplied by the deployment environment or secret
manager. Do not commit `.env`, `.env.local` or real credentials.

## Backend

Database:

| Variable | Required in production | Notes |
| --- | --- | --- |
| `DB_HOST` | Yes | PostgreSQL host. |
| `DB_PORT` | Yes | Usually `5432`. |
| `DB_NAME` | Yes | Production database name. |
| `DB_USER` | Yes | Production database user. |
| `DB_PASSWORD` | Yes | Production database password. |
| `DB_JDBC_PARAMETERS` | Maybe | Optional JDBC suffix such as `?sslmode=require` if the PostgreSQL provider requires TLS parameters. Leave blank for local Docker. |
| `DB_POOL_MAX_SIZE` | No | Defaults to `5` in prod profile. |
| `DB_POOL_MIN_IDLE` | No | Defaults to `0` in prod profile. |
| `DB_CONNECTION_TIMEOUT_MS` | No | Defaults to `30000`. |
| `DB_VALIDATION_TIMEOUT_MS` | No | Defaults to `5000`. |
| `DB_MAX_LIFETIME_MS` | No | Defaults to `1800000`. |

Backend URLs and CORS:

| Variable | Required in production | Notes |
| --- | --- | --- |
| `PORT` | Render sets this | Render web services provide this automatically. Spring Boot reads it before `SERVER_PORT`. |
| `SERVER_PORT` | No | Local/manual override. Defaults to `8080` when `PORT` is absent. |
| `CUSTOMER_WEB_BASE_URL` | Yes | Customer HTTPS origin used for Stripe return URLs. |
| `ADMIN_WEB_BASE_URL` | No | Admin HTTPS origin used in internal staff alert links. Defaults to the local admin URL in development and the Basilico admin domain in prod. |
| `BASILICO_CORS_ALLOWED_ORIGINS` | Yes | Comma-separated explicit customer/admin origins. Never use `*`. |

Admin authentication:

| Variable | Required in production | Notes |
| --- | --- | --- |
| `BASILICO_ADMIN_EMAIL` | Initial setup only | Used only to bootstrap an owner if the email does not already exist. |
| `BASILICO_ADMIN_PASSWORD` | Initial setup only | BCrypt-hashed at startup; never logged or stored as plaintext. |
| `BASILICO_ADMIN_DISPLAY_NAME` | Initial setup only | Display name for the bootstrapped owner. |
| `ADMIN_SESSION_TIMEOUT` | No | Defaults to `365d` in `prod` and `8h` locally; controls inactive session expiry stored in PostgreSQL through Spring Session JDBC. |
| `ADMIN_SESSION_COOKIE_MAX_AGE` | No | Defaults to `365d` in `prod` and `8h` locally; controls persistent browser lifetime for the HttpOnly `SESSION` cookie. Keep aligned with `ADMIN_SESSION_TIMEOUT`. |
| `SESSION_COOKIE_SECURE` | Yes | Use `true` behind production HTTPS. |
| `SESSION_COOKIE_SAME_SITE` | No | Defaults to `lax`. |

Stripe:

| Variable | Required in production | Notes |
| --- | --- | --- |
| `STRIPE_SECRET_KEY` | Yes | Backend-only secret key. Never expose to browser. |
| `STRIPE_WEBHOOK_SECRET` | Yes | Backend-only webhook signing secret. |
| `BASILICO_ALLOW_MANUAL_PAYMENT_STATUS` | No | Defaults `false`; development tool only. |

SMTP email:

| Variable | Required in production | Notes |
| --- | --- | --- |
| `MAIL_HOST` | Yes | Production SMTP host. |
| `MAIL_PORT` | Yes | Usually `587` for STARTTLS. |
| `MAIL_USERNAME` | Depends on provider | SMTP username. |
| `MAIL_PASSWORD` | Depends on provider | SMTP password/API key. |
| `MAIL_FROM_EMAIL` | Yes | Verified sender address. |
| `MAIL_FROM_NAME` | No | Defaults to `Basilico - Simple Italian`. |
| `MAIL_SMTP_AUTH` | No | Defaults to `true` in prod profile. |
| `MAIL_SMTP_STARTTLS` | No | Defaults to `true` in prod profile. |
| `MAIL_MAX_ATTEMPTS` | No | Defaults to `3`. |
| `MAIL_BATCH_SIZE` | No | Defaults to `10`. |
| `MAIL_WORKER_ENABLED` | No | Defaults to `true`. |
| `MAIL_RETRY_DELAY_MS` | No | Defaults to `30000`. |
| `MAIL_INITIAL_DELAY_MS` | No | Defaults to `5000`. |
| `MAIL_HEALTH_ENABLED` | No | Defaults to `false`; enable only if SMTP health should affect readiness. |
| `BASILICO_ORDER_ALERT_EMAIL` | Recommended before taking real payments | Internal restaurant recipient for new paid-order alerts. For Basilico production, set this to `basilico2912@gmail.com`. If absent, customer payments still succeed and the missing alert configuration is logged. |

Delivery/geocoding:

| Variable | Required in production | Notes |
| --- | --- | --- |
| `POSTCODES_IO_BASE_URL` | No | Defaults to `https://api.postcodes.io`; used by the backend for UK postcode geocoding. |
| `POSTCODES_IO_TIMEOUT_MS` | No | Defaults to `2500`; prevents postcode lookup requests hanging indefinitely. |
| `POSTCODES_IO_CACHE_TTL_SECONDS` | No | Defaults to `86400`; bounded in-memory postcode coordinate cache. |
| `POSTCODES_IO_CACHE_MAX_ENTRIES` | No | Defaults to `500`; protects the local cache from unbounded growth. |
| `OPENROUTESERVICE_API_KEY` | Optional | Enables driving-time ETA estimates. If absent, eligibility and fee still work but ETA is unavailable. |
| `OPENROUTESERVICE_BASE_URL` | No | Defaults to `https://api.heigit.org/openrouteservice`. |
| `OPENROUTESERVICE_TIMEOUT_MS` | No | Defaults to `3500`; prevents routing requests hanging indefinitely. |
| `OPENROUTESERVICE_CACHE_TTL_SECONDS` | No | Defaults to `900`; short-lived in-memory route-duration cache. |
| `OPENROUTESERVICE_CACHE_MAX_ENTRIES` | No | Defaults to `500`; protects the local route cache from unbounded growth. |

Rate limiting:

| Variable | Required in production | Notes |
| --- | --- | --- |
| `RATE_LIMIT_ENABLED` | No | Defaults to `true`. |
| `RATE_LIMIT_WINDOW_SECONDS` | No | Defaults to `60`. |
| `RATE_LIMIT_ORDER_CREATE_LIMIT` | No | Defaults to `120`. |
| `RATE_LIMIT_BOOKING_CREATE_LIMIT` | No | Defaults to `60`. |
| `RATE_LIMIT_CHECKOUT_SESSION_LIMIT` | No | Defaults to `120`. |
| `RATE_LIMIT_DELIVERY_QUOTE_LIMIT` | No | Defaults to `120`. |
| `RATE_LIMIT_ADMIN_LOGIN_LIMIT` | No | Defaults to `10`. |

## Customer Web

| Variable | Required in production | Notes |
| --- | --- | --- |
| `NEXT_PUBLIC_SITE_URL` | Yes | Customer HTTPS site URL for metadata, canonical URLs, robots and sitemap. |
| `NEXT_PUBLIC_API_BASE_URL` | Yes | Public backend API URL. |
| `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` | Yes for payments | Browser-safe Stripe publishable key. Use live key only when production payments are approved. |

## Admin Web

| Variable | Required in production | Notes |
| --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | Yes | Backend API URL. |
| `NEXT_PUBLIC_CUSTOMER_WEB_URL` | Yes | Customer website URL used by the admin shell. |
| `NEXT_PUBLIC_ENABLE_DEV_PAYMENT_CONTROL` | No | Defaults false; keep false in production. |

## Render Backend Variable Checklist

Render supplies `PORT` automatically for web services. Render PostgreSQL values
map cleanly into the existing Basilico database variables:

| Basilico variable | Render PostgreSQL source |
| --- | --- |
| `DB_HOST` | database host |
| `DB_PORT` | database port |
| `DB_NAME` | database name |
| `DB_USER` | database user |
| `DB_PASSWORD` | database password |

Required at first deployment:

| Variable | Notes |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | Enables production overrides. |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | From Render PostgreSQL. |
| `BASILICO_CORS_ALLOWED_ORIGINS` | Exact Vercel customer and admin HTTPS origins. |
| `CUSTOMER_WEB_BASE_URL` | Exact customer HTTPS origin for payment return URLs. |
| `ADMIN_SESSION_TIMEOUT=365d` | Long-lived inactive admin session expiry in the database. |
| `ADMIN_SESSION_COOKIE_MAX_AGE=365d` | Persistent browser cookie lifetime for admin sessions. |
| `SESSION_COOKIE_SECURE=true` | Required behind production HTTPS. |
| `SESSION_COOKIE_SAME_SITE` | Use `lax` when admin and API are same-site subdomains. Use `none` only for truly cross-site HTTPS setups, such as Vercel preview domains talking to a Render service domain, and still keep `Secure=true`. |
| `BASILICO_ADMIN_EMAIL` / `BASILICO_ADMIN_PASSWORD` / `BASILICO_ADMIN_DISPLAY_NAME` | Needed for first owner bootstrap. Remove or leave inert after the owner exists; the password is not overwritten automatically. |

Required before Stripe live:

| Variable | Notes |
| --- | --- |
| `STRIPE_SECRET_KEY` | Use test key for staging. Switch to live only after QA. |
| `STRIPE_WEBHOOK_SECRET` | Use the webhook signing secret from the deployed Stripe endpoint. |
| `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` | Set in Vercel customer-web. Use test publishable key for staging. |

Required before email live:

| Variable | Notes |
| --- | --- |
| `MAIL_HOST` / `MAIL_PORT` | Production SMTP server. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP credentials or API key, depending on provider. |
| `MAIL_FROM_EMAIL` | Verified sender address. |
| `MAIL_FROM_NAME` | Sender display name. |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | Usually `true` / `true` for production SMTP. |
| `MAIL_WORKER_ENABLED` | Keep `false` until SMTP is configured; set `true` when ready to send. |

Required before delivery live:

| Variable | Notes |
| --- | --- |
| `OPENROUTESERVICE_API_KEY` | Optional but recommended before launch if Basilico wants checkout/menu ETA. Delivery radius and fee work without it. |

Optional / development only:

| Variable | Notes |
| --- | --- |
| `SERVER_PORT` | Local manual port override; Render uses `PORT`. |
| `DB_JDBC_PARAMETERS` | Set only if the database connection requires extra JDBC parameters such as SSL mode. |
| `BASILICO_ALLOW_MANUAL_PAYMENT_STATUS` | Development-only payment control; keep `false` outside deliberate local testing. |
| `RATE_LIMIT_*` | Defaults are suitable for first deployment; tune after real traffic observations. |
| `MAIL_HEALTH_ENABLED` | Keep `false` unless SMTP health should influence service health. |
| `NEXT_PUBLIC_ENABLE_DEV_PAYMENT_CONTROL` | Admin-web development-only switch; keep unset/false in production. |

## Google Cloud Run + Neon Notes

Cloud Run should use the same backend variables listed above. Set
`SPRING_PROFILES_ACTIVE=prod`, map the Neon PostgreSQL credentials into the
existing `DB_*` variables, and keep `DB_JDBC_PARAMETERS=?sslmode=require` if
the chosen Neon connection string requires it. Admin sessions are stored in
Neon by Spring Session JDBC, so use `ADMIN_SESSION_TIMEOUT=365d`,
`ADMIN_SESSION_COOKIE_MAX_AGE=365d`, `SESSION_COOKIE_SECURE=true` and
`SESSION_COOKIE_SAME_SITE=lax` for the production admin/API same-site domain
shape.
