# Basilico Customer Web

Customer-facing Next.js application for Basilico - Simple Italian.

## Production Configuration

Customer-web uses environment variables for runtime endpoints and public site
metadata:

```bash
NEXT_PUBLIC_SITE_URL=https://customer-domain.example
NEXT_PUBLIC_API_BASE_URL=https://api-domain.example
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=
```

Use local values from `.env.example` for development. Production must provide
the real HTTPS customer URL so canonical URLs, Open Graph metadata, robots and
the sitemap do not point at localhost.

For Vercel, create the project with:

- root directory: `customer-web`
- install command: `npm install`
- build command: `npm run build`
- output: Vercel default for Next.js

Set `NEXT_PUBLIC_API_BASE_URL` to the deployed backend HTTPS origin and
`NEXT_PUBLIC_SITE_URL` to the deployed customer-web HTTPS origin. `NEXT_PUBLIC_*`
values are compiled into the browser bundle, so update/redeploy after changing
them.

## Menu API

The live menu, product detail pages and Create Your Own pizza customiser load
runtime menu data from the Spring Boot backend configured by:

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Copy `.env.example` to `.env.local` for local development if you need to change
the backend URL.

Next.js loads `.env.local` automatically. Keep backend-only secrets such as
`STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` out of customer-web.

`src/data/menu.ts` is intentionally kept temporarily as a reference/fallback
copy during the API migration. It should not be treated as the active runtime
menu source for `/menu`, `/menu/[slug]`, or `/menu/create-your-own`.

## Payments

Checkout creates an order through the Spring Boot backend, then `/checkout/payment`
starts a Stripe Checkout Session using:

```bash
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=
```

Use a Stripe test publishable key locally. Do not commit real keys.

The customer app never sends authoritative prices or subtotals to Stripe. It
sends the backend order reference, and the backend creates the Stripe session
from persisted order snapshots.

Checkout also reads fulfilment settings from:

```bash
GET /api/fulfilment/options
POST /api/fulfilment/delivery-quote
```

The customer app shows a delivery preview on `/menu` using postcode or
permission-based browser geolocation. It stores only the short-lived quote
result in `sessionStorage`, not raw GPS coordinates. Checkout verifies the
actual delivery postcode through the backend quote API before payment.

Delivery fee and ETA are previews until `POST /api/orders` revalidates the
postcode and recalculates the backend-authoritative total. If the
backend-created order total differs from the displayed checkout total, the
customer must review the updated total before continuing to payment.

The return page `/checkout/payment/return?session_id=...` asks the backend for
the session status and clears the basket only after the backend reports
`paymentStatus=PAID`. If payment is still pending, failed or expired, the basket
is kept so the customer can retry.

## Customer Emails

Transactional order and booking emails are sent by the Spring Boot backend after
the relevant database transaction commits. Customer-web does not send SMTP email
directly and does not expose notification history publicly.

In local development, backend Docker Compose includes Mailpit so test emails can
be viewed at:

```bash
http://localhost:8025
```

## SEO and Public Indexing

Customer-web provides basic metadata for Basilico - Simple Italian in
Dorchester, plus `robots.txt` and `sitemap.xml` for public browsing routes.
Basket, checkout, payment and compatibility order routes are excluded from the
public sitemap.

Security headers include content-type protection, frame denial, a conservative
referrer policy, blocked camera/microphone permissions and same-origin
geolocation for the menu delivery preview. A Content Security Policy is
intentionally not enabled in this phase because Stripe.js and the Payment
Element require a deliberate allowlist during deployment.

## Getting Started

First, run the development server:

```bash
npm run dev -- -p 3001
# or
npm run dev
```

Open [http://localhost:3001](http://localhost:3001) with your browser to see
the result when running alongside admin-web.

The page auto-updates as you edit files in `src/`.

## Validation

```bash
npm run lint
npm run build
```

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

Use Vercel for the Basilico customer-web deployment. In this monorepo, select
`customer-web` as the Vercel project root so Vercel runs the package scripts
from the correct directory.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
