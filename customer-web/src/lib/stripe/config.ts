export const STRIPE_PUBLISHABLE_KEY =
  process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY ?? "";

export function hasStripePublishableKey() {
  return STRIPE_PUBLISHABLE_KEY.trim().length > 0;
}
