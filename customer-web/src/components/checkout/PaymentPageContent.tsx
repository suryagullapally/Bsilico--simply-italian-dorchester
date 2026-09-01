"use client";

import {
  CheckoutElementsProvider,
  PaymentElement,
  useCheckoutElements,
} from "@stripe/react-stripe-js/checkout";
import { loadStripe } from "@stripe/stripe-js";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Button, ButtonLink } from "@/components/ui/Button";
import { ApiRequestError, getApiErrorMessage } from "@/lib/api/api-error";
import { createCheckoutSession } from "@/lib/api/payment-api";
import { formatGbpPennies } from "@/lib/format-price";
import { routes } from "@/lib/routes";
import {
  STRIPE_PUBLISHABLE_KEY,
  hasStripePublishableKey,
} from "@/lib/stripe/config";
import type { BackendCheckoutSessionResponse } from "@/types/backend-payment";

const stripePromise = hasStripePublishableKey()
  ? loadStripe(STRIPE_PUBLISHABLE_KEY)
  : Promise.resolve(null);

type PaymentState =
  | { status: "loading" }
  | { message: string; status: "error" }
  | { message: string; status: "already-paid" }
  | { session: BackendCheckoutSessionResponse; status: "ready" };

type PaymentPageContentProps = {
  orderReference: string;
};

export function PaymentPageContent({ orderReference }: PaymentPageContentProps) {
  const stripeReady = hasStripePublishableKey();
  const [paymentState, setPaymentState] = useState<PaymentState>(() => {
    if (!orderReference) {
      return {
        message: "We need an order reference before payment can begin.",
        status: "error",
      };
    }

    if (!stripeReady) {
      return {
        message:
          "Online payment is not configured in this local environment yet.",
        status: "error",
      };
    }

    return { status: "loading" };
  });
  const requestedRef = useRef(false);

  useEffect(() => {
    if (!orderReference || !stripeReady || requestedRef.current) {
      return;
    }

    requestedRef.current = true;

    createCheckoutSession(orderReference)
      .then((session) => {
        setPaymentState({ session, status: "ready" });
      })
      .catch((error) => {
        if (
          error instanceof ApiRequestError &&
          error.status === 409 &&
          error.message.toLowerCase().includes("already")
        ) {
          setPaymentState({
            message: error.message,
            status: "already-paid",
          });
          return;
        }

        setPaymentState({
          message: getApiErrorMessage(
            error,
            "We couldn’t start secure payment right now. Please try again.",
          ),
          status: "error",
        });
      });
  }, [orderReference, stripeReady]);

  if (paymentState.status === "loading") {
    return (
      <section className="checkout-placeholder" aria-labelledby="payment-title">
        <p className="type-eyebrow checkout-placeholder__eyebrow">
          Secure payment
        </p>
        <h1 className="type-h1 checkout-placeholder__title" id="payment-title">
          Preparing payment.
        </h1>
        <p className="type-body checkout-placeholder__copy">
          Connecting your order to Stripe.
        </p>
      </section>
    );
  }

  if (paymentState.status === "already-paid") {
    return (
      <section className="checkout-placeholder" aria-labelledby="payment-title">
        <p className="type-eyebrow checkout-placeholder__eyebrow">
          Payment
        </p>
        <h1 className="type-h1 checkout-placeholder__title" id="payment-title">
          This order has already been paid.
        </h1>
        <dl className="checkout-placeholder__details">
          <div>
            <dt>Order reference</dt>
            <dd>{orderReference}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>Paid</dd>
          </div>
        </dl>
        <p className="type-body checkout-placeholder__copy">
          No new payment session has been created.
        </p>
        <div className="checkout-placeholder__actions">
          <ButtonLink href={routes.menu} variant="primary">
            BACK TO MENU
          </ButtonLink>
          <Link className="checkout-placeholder__menu-link" href={routes.basket}>
            Back to basket
          </Link>
        </div>
      </section>
    );
  }

  if (paymentState.status === "error") {
    return (
      <section className="checkout-placeholder" aria-labelledby="payment-title">
        <p className="type-eyebrow checkout-placeholder__eyebrow">
          Payment
        </p>
        <h1 className="type-h1 checkout-placeholder__title" id="payment-title">
          Payment is not available.
        </h1>
        <p className="type-body checkout-placeholder__copy">
          {paymentState.message}
        </p>
        <div className="checkout-placeholder__actions">
          <ButtonLink href={routes.checkout} variant="primary">
            BACK TO CHECKOUT
          </ButtonLink>
          <Link className="checkout-placeholder__menu-link" href={routes.basket}>
            Back to basket
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="payment-page" aria-labelledby="payment-title">
      <div className="payment-page__intro">
        <Link className="checkout-page__back" href={routes.checkout}>
          ← Back to checkout
        </Link>
        <p className="type-eyebrow checkout-payment__eyebrow">Secure payment</p>
        <h1 className="type-h1 checkout-page__title" id="payment-title">
          Pay for your order.
        </h1>
        <p className="type-body checkout-page__copy">
          Secure payment powered by Stripe.
        </p>
      </div>

      <div className="payment-page__layout">
        <aside className="payment-page__summary" aria-label="Payment summary">
          <dl className="checkout-placeholder__details">
            <div>
              <dt>Order reference</dt>
              <dd>{paymentState.session.orderReference}</dd>
            </div>
            <div>
              <dt>Order amount</dt>
              <dd>{formatGbpPennies(paymentState.session.amountPence)}</dd>
            </div>
          </dl>
          <p className="payment-page__smallprint">
            Card details are handled directly by Stripe and never pass through
            Basilico&apos;s server.
          </p>
        </aside>

        <div className="payment-page__payment">
          <CheckoutElementsProvider
            options={{
              clientSecret: paymentState.session.checkoutSessionClientSecret,
              elementsOptions: {
                appearance: stripeAppearance,
                loader: "auto",
              },
            }}
            stripe={stripePromise}
          >
            <StripeCheckoutForm session={paymentState.session} />
          </CheckoutElementsProvider>
        </div>
      </div>
    </section>
  );
}

type StripeCheckoutFormProps = {
  session: BackendCheckoutSessionResponse;
};

function StripeCheckoutForm({ session }: StripeCheckoutFormProps) {
  const checkoutState = useCheckoutElements();
  const router = useRouter();
  const [errorMessage, setErrorMessage] = useState("");
  const [processing, setProcessing] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (processing || checkoutState.type !== "success") {
      return;
    }

    setProcessing(true);
    setErrorMessage("");

    const returnPath = `${routes.checkoutPaymentReturn}?session_id=${encodeURIComponent(
      session.stripeCheckoutSessionId,
    )}`;

    try {
      const result = await checkoutState.checkout.confirm();

      if (result.type === "error") {
        setErrorMessage(paymentErrorMessage(result.error.message));
        setProcessing(false);
        return;
      }

      router.push(returnPath);
    } catch (error) {
      setErrorMessage(paymentErrorMessage(error));
      setProcessing(false);
    }
  }

  if (checkoutState.type === "loading") {
    return (
      <div className="payment-element-shell payment-element-shell--loading">
        Loading secure payment fields.
      </div>
    );
  }

  if (checkoutState.type === "error") {
    return (
      <div
        aria-live="polite"
        className="checkout-submit-message checkout-submit-message--error"
      >
        <p>{paymentErrorMessage(checkoutState.error.message)}</p>
      </div>
    );
  }

  return (
    <form className="payment-form" onSubmit={handleSubmit}>
      <div className="payment-element-shell">
        <PaymentElement />
      </div>

      {errorMessage ? (
        <div
          aria-live="polite"
          className="checkout-submit-message checkout-submit-message--error"
        >
          <p>{errorMessage}</p>
        </div>
      ) : null}

      <Button
        className="payment-form__button"
        disabled={processing || !checkoutState.checkout.canConfirm}
        type="submit"
        variant="primary"
      >
        {processing
          ? "PROCESSING PAYMENT..."
          : `PAY ${formatGbpPennies(session.amountPence)}`}
      </Button>
    </form>
  );
}

const stripeAppearance = {
  theme: "night" as const,
  variables: {
    borderRadius: "8px",
    colorBackground: "#1b1713",
    colorDanger: "#d6634f",
    colorPrimary: "#b8c47a",
    colorText: "#fff8ea",
    colorTextSecondary: "#d7c9ac",
    fontFamily:
      "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, sans-serif",
  },
};

function paymentErrorMessage(error: unknown) {
  const message =
    error instanceof Error ? error.message : typeof error === "string" ? error : "";
  const trimmedMessage = message.trim();

  if (!trimmedMessage) {
    return "Your payment could not be completed. Please check your payment details and try again.";
  }

  return trimmedMessage;
}
