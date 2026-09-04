# Basilico Production Checklist

Do not mark an item complete until it has genuinely been completed in the
production environment.

## Domain

- [ ] final customer domain selected
- [ ] final admin domain selected
- [ ] final backend/API domain selected
- [ ] DNS configured
- [ ] HTTPS certificate active

## Hosting

- [ ] Render PostgreSQL created
- [ ] Render backend web service created from `render.yaml` or equivalent manual settings
- [ ] Render backend health check set to `/api/health`
- [ ] customer-web Vercel project created with root directory `customer-web`
- [ ] admin-web Vercel project created with root directory `admin-web`
- [ ] Vercel customer/admin environment variables configured
- [ ] Render backend environment variables configured

## Database

- [ ] managed PostgreSQL selected
- [ ] production database created
- [ ] production database credentials stored in secret manager/environment
- [ ] automatic daily backups enabled
- [ ] point-in-time recovery enabled if available
- [ ] manual backup process tested
- [ ] restore process tested

## Backend

- [ ] production profile enabled with `SPRING_PROFILES_ACTIVE=prod`
- [ ] production environment variables configured
- [ ] HTTPS enforced at the platform/proxy layer
- [ ] explicit CORS origins configured
- [ ] secure session cookies enabled
- [ ] admin session timeout and cookie Max-Age confirmed
- [ ] Spring Session JDBC tables exist in production PostgreSQL
- [ ] Flyway migrations validated against production database
- [ ] actuator exposure checked
- [ ] logs checked for secret/PII safety

## Stripe

- [ ] Stripe live account ready
- [ ] live publishable key configured in customer-web
- [ ] live secret key configured in backend
- [ ] production webhook endpoint created
- [ ] webhook signing secret configured
- [ ] live payment QA completed
- [ ] failed-payment flow QA completed
- [ ] full admin refund QA completed
- [ ] duplicate webhook/idempotency QA completed

## Email

- [ ] production SMTP provider selected
- [ ] sender domain/address verified
- [ ] SMTP credentials configured
- [ ] order received email QA completed
- [ ] internal paid-order alert email QA completed
- [ ] internal new-booking alert email QA completed
- [ ] booking request email QA completed
- [ ] admin manual email QA completed
- [ ] failed notification retry QA completed

## Admin

- [ ] production owner account bootstrapped
- [ ] production owner password changed/secured
- [ ] login QA completed
- [ ] refresh and backend restart preserve authenticated admin session
- [ ] logout/session expiry QA completed
- [ ] noindex headers verified
- [ ] admin URL not publicly linked from customer website

## Fulfilment

- [ ] collection enabled confirmed
- [ ] owner-approved 6-mile Radius delivery settings verified
- [ ] owner-approved £2/£3/£4/£5 delivery fee bands verified
- [ ] restaurant postcode and coordinate seed verified
- [ ] Postcodes.io delivery quote QA completed
- [ ] OpenRouteService ETA key configured if ETA should be shown
- [ ] delivery remains disabled until final owner launch approval

## QA

- [ ] desktop customer QA
- [ ] mobile customer QA
- [ ] admin desktop QA
- [ ] admin tablet/mobile QA
- [ ] menu sold-out QA
- [ ] order creation QA
- [ ] payment QA
- [ ] booking QA
- [ ] notification QA
- [ ] accessibility smoke QA
- [ ] production error pages checked
