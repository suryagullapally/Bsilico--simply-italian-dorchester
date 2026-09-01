# Basilico Localhost Audit

This audit covers hardcoded `localhost:3000`, `localhost:3001`,
`localhost:3002` and `localhost:8080` occurrences found during Phase 8B-1.

## Runtime Source

| File | Classification | Notes |
| --- | --- | --- |
| `backend/src/main/resources/application.yml` | Safe development fallback | Local CORS and customer return URL defaults. Production uses `SPRING_PROFILES_ACTIVE=prod` plus explicit Render env vars. |
| `customer-web/src/lib/api/config.ts` | Safe development fallback | Defaults API calls to local backend only when `NEXT_PUBLIC_API_BASE_URL` is absent. Vercel production must set the env var. |
| `customer-web/src/lib/site-config.ts` | Safe development fallback | Defaults metadata base URL to local customer-web only when `NEXT_PUBLIC_SITE_URL` is absent. Vercel production must set the env var. |
| `admin-web/src/lib/api/config.ts` | Safe development fallback | Defaults API/customer links to local services only when Vercel env vars are absent. Vercel production must set both env vars. |

## Environment Examples

| File | Classification | Notes |
| --- | --- | --- |
| `backend/.env.example` | Safe local example | Shows local Docker/Postgres/Mailpit/customer/admin origins only. |
| `customer-web/.env.example` | Safe local example | Shows local customer/API values for development. |
| `admin-web/.env.example` | Safe local example | Shows local backend/customer values for development. |

## Documentation

README and `docs/` localhost references are local-development instructions or
deployment warnings. They are not application runtime dependencies.

## Result

No genuine production runtime problem was found. The production deployment must
still set explicit HTTPS Render/Vercel variables so build-time `NEXT_PUBLIC_*`
values and backend CORS/return URLs never fall back to localhost.
