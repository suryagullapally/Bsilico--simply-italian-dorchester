# Basilico Customer Web

Customer-facing Next.js application for Basilico - Simple Italian.

## Production Configuration

Customer-web uses environment variables for runtime endpoints and public site
metadata:

```bash
NEXT_PUBLIC_SITE_URL=https://basilicodorchester.co.uk
NEXT_PUBLIC_API_BASE_URL=https://api.basilicodorchester.co.uk
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=<Stripe test publishable key>
```

Use local values from `.env.example` for development. Production should still
provide the real HTTPS customer URL explicitly. The compiled fallback is also
the production domain so canonical URLs, Open Graph metadata, robots and the
sitemap do not accidentally point at localhost.

`NEXT_PUBLIC_*` values are compiled into the browser bundle, so update/redeploy
after changing them. Keep Stripe in test mode for the first deployed QA pass.

## Cloudflare Workers

Customer-web is prepared for Cloudflare Workers with vinext, Cloudflare's
current recommended Next.js-on-Workers path for new Next.js 16 applications.
The normal Next.js development workflow still works:

```bash
npm run dev
npm run build
```

Cloudflare Workers commands:

```bash
npm run build:vinext
npm run preview
npm run deploy
```

The Worker name is configured as `basilico-customer` in `wrangler.jsonc`.
Generated vinext/Workers output is ignored through `.gitignore`.

For Cloudflare, create the Worker project with:

- root directory: `customer-web`
- install command: `npm install`
- build command: `npm run build:vinext`
- deploy command: `npx wrangler deploy --config dist/server/wrangler.json`
- Worker name: `basilico-customer`

Do not configure the custom domain until staging verification is complete.

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

Customer-web provides route-specific metadata for Basilico - Simple Italian in
Dorchester, including canonical URLs, Open Graph metadata, Twitter card
metadata, branded favicon/app icons, `robots.txt`, `sitemap.xml` and
server-rendered Restaurant JSON-LD. Public restaurant pages are included in the
sitemap. Basket, checkout, payment and compatibility order routes are excluded
from the public sitemap and marked `noindex`.

After production deployment:

1. Add `basilicodorchester.co.uk` to Google Search Console.
2. Verify ownership using a Cloudflare DNS TXT record.
3. Submit `https://basilicodorchester.co.uk/sitemap.xml`.
4. Request indexing for `/`, `/menu` and `/book`.
5. Test Restaurant structured data with Google's Rich Results Test.

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
npm run build:vinext
```

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
