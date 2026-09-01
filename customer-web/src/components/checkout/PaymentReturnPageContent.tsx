"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { useCart } from "@/components/cart/CartProvider";
import { Button, ButtonLink } from "@/components/ui/Button";
import { getApiErrorMessage } from "@/lib/api/api-error";
import { getCheckoutSessionStatus } from "@/lib/api/payment-api";
import { routes } from "@/lib/routes";
import type { BackendCheckoutSessionStatusResponse } from "@/types/backend-payment";

const MAX_POLL_ATTEMPTS = 15;
const POLL_INTERVAL_MS = 1000;

type PaymentReturnView = "expired" | "failed" | "not-completed" | "processing" | "success";

type ReturnState =
  | { status: "loading" }
  | { message: string; status: "error" }
  | {
      attempts: number;
      paymentStatus: BackendCheckoutSessionStatusResponse;
      status: "ready";
      timedOut: boolean;
    };

type PaymentReturnPageContentProps = {
  sessionId: string;
};

export function PaymentReturnPageContent({
  sessionId,
}: PaymentReturnPageContentProps) {
  const { clearCart } = useCart();
  const [returnState, setReturnState] = useState<ReturnState>({
    status: "loading",
  });
  const [refreshKey, setRefreshKey] = useState(0);
  const clearedCartRef = useRef(false);
  const timeoutRef = useRef<number | null>(null);

  const requestRefresh = useCallback(() => {
    setRefreshKey((currentValue) => currentValue + 1);
  }, []);

  useEffect(() => {
    let mounted = true;

    async function checkStatus(attempt = 0): Promise<void> {
      if (!sessionId) {
        setReturnState({
          message: "We need a payment session before checking payment status.",
          status: "error",
        });
        return;
      }

      try {
        const paymentStatus = await getCheckoutSessionStatus(sessionId);

        if (!mounted) {
          return;
        }

        if (paymentStatus.paymentStatus === "PAID" && !clearedCartRef.current) {
          clearCart();
          clearedCartRef.current = true;
        }

        const shouldContinuePolling =
          isPaymentAwaitingConfirmation(paymentStatus) &&
          attempt < MAX_POLL_ATTEMPTS;

        setReturnState({
          attempts: attempt,
          paymentStatus,
          status: "ready",
          timedOut:
            !shouldContinuePolling &&
            isPaymentAwaitingConfirmation(paymentStatus),
        });

        if (shouldContinuePolling) {
          timeoutRef.current = window.setTimeout(() => {
            void checkStatus(attempt + 1);
          }, POLL_INTERVAL_MS);
        }
      } catch (error) {
        if (!mounted) {
          return;
        }

        setReturnState({
          message: getApiErrorMessage(
            error,
            "We couldn't confirm your payment status right now.",
          ),
          status: "error",
        });
      }
    }

    void checkStatus();

    return () => {
      mounted = false;
      if (timeoutRef.current !== null) {
        window.clearTimeout(timeoutRef.current);
      }
    };
  }, [clearCart, refreshKey, sessionId]);

  if (returnState.status === "loading") {
    return (
      <section
        aria-labelledby="payment-return-title"
        aria-live="polite"
        className="checkout-placeholder"
      >
        <p className="type-eyebrow checkout-placeholder__eyebrow">
          Payment
        </p>
        <h1
          className="type-h1 checkout-placeholder__title"
          id="payment-return-title"
        >
          We&apos;re confirming your payment.
        </h1>
        <p className="type-body checkout-placeholder__copy">
          This usually takes just a moment.
        </p>
      </section>
    );
  }

  if (returnState.status === "error") {
    return (
      <section
        aria-labelledby="payment-return-title"
        aria-live="polite"
        className="checkout-placeholder"
      >
        <p className="type-eyebrow checkout-placeholder__eyebrow">
          Payment
        </p>
        <h1
          className="type-h1 checkout-placeholder__title"
          id="payment-return-title"
        >
          We couldn’t check payment status.
        </h1>
        <p className="type-body checkout-placeholder__copy">
          {returnState.message}
        </p>
        <div className="checkout-placeholder__actions">
          <Button onClick={requestRefresh} variant="primary">
            CHECK AGAIN
          </Button>
          <Link className="checkout-placeholder__menu-link" href={routes.basket}>
            Back to basket
          </Link>
        </div>
      </section>
    );
  }

  const paymentStatus = returnState.paymentStatus;
  const view = getPaymentReturnView(paymentStatus, returnState.timedOut);
  const retryPaymentHref = `${routes.checkoutPayment}?order=${encodeURIComponent(
    paymentStatus.orderReference,
  )}`;

  return (
    <section
      aria-labelledby="payment-return-title"
      aria-live="polite"
      className="checkout-placeholder"
    >
      <p className="type-eyebrow checkout-placeholder__eyebrow">Payment</p>
      <h1
        className="type-h1 checkout-placeholder__title"
        id="payment-return-title"
      >
        {headingForView(view)}
      </h1>

      <dl className="checkout-placeholder__details">
        <div>
          <dt>Order reference</dt>
          <dd>{paymentStatus.orderReference}</dd>
        </div>
        <div>
          <dt>Payment status</dt>
          <dd>{displayStatus(paymentStatus.paymentStatus)}</dd>
        </div>
        <div>
          <dt>Order status</dt>
          <dd>{displayStatus(paymentStatus.orderStatus)}</dd>
        </div>
      </dl>

      <p className="type-body checkout-placeholder__copy">
        {copyForView(view, returnState.timedOut)}
      </p>

      <div className="checkout-placeholder__actions">
        {view === "success" ? (
          <ButtonLink href={routes.menu} variant="primary">
            BACK TO MENU
          </ButtonLink>
        ) : view === "processing" ? (
          <Button onClick={requestRefresh} variant="primary">
            CHECK STATUS AGAIN
          </Button>
        ) : (
          <ButtonLink href={retryPaymentHref} variant="primary">
            TRY PAYMENT AGAIN
          </ButtonLink>
        )}
        <Link className="checkout-placeholder__menu-link" href={routes.basket}>
          {view === "success" ? "View basket" : "Back to basket"}
        </Link>
      </div>
    </section>
  );
}

function getPaymentReturnView(
  paymentStatus: BackendCheckoutSessionStatusResponse,
  timedOut: boolean,
): PaymentReturnView {
  if (paymentStatus.paymentStatus === "PAID") {
    return "success";
  }

  if (
    paymentStatus.paymentAttemptStatus === "EXPIRED" ||
    normalizedStatus(paymentStatus.checkoutSessionStatus) === "expired"
  ) {
    return "expired";
  }

  if (
    paymentStatus.paymentStatus === "FAILED" ||
    paymentStatus.paymentAttemptStatus === "FAILED"
  ) {
    return "failed";
  }

  if (isOpenUnpaidCheckoutSession(paymentStatus)) {
    return "not-completed";
  }

  if (timedOut) {
    return "processing";
  }

  return "processing";
}

function isPaymentAwaitingConfirmation(
  paymentStatus: BackendCheckoutSessionStatusResponse,
) {
  if (
    paymentStatus.paymentStatus === "PAID" ||
    paymentStatus.paymentStatus === "FAILED" ||
    paymentStatus.paymentAttemptStatus === "PAID" ||
    paymentStatus.paymentAttemptStatus === "FAILED" ||
    paymentStatus.paymentAttemptStatus === "EXPIRED"
  ) {
    return false;
  }

  const checkoutSessionStatus = normalizedStatus(
    paymentStatus.checkoutSessionStatus,
  );
  const checkoutSessionPaymentStatus = normalizedStatus(
    paymentStatus.checkoutSessionPaymentStatus,
  );

  if (checkoutSessionStatus === "expired") {
    return false;
  }

  if (
    checkoutSessionStatus === "open" &&
    checkoutSessionPaymentStatus === "unpaid"
  ) {
    return false;
  }

  return (
    checkoutSessionStatus === "complete" ||
    checkoutSessionPaymentStatus === "paid" ||
    !checkoutSessionStatus
  );
}

function isOpenUnpaidCheckoutSession(
  paymentStatus: BackendCheckoutSessionStatusResponse,
) {
  return (
    normalizedStatus(paymentStatus.checkoutSessionStatus) === "open" &&
    normalizedStatus(paymentStatus.checkoutSessionPaymentStatus) === "unpaid"
  );
}

function headingForView(view: PaymentReturnView) {
  switch (view) {
    case "success":
      return "Payment successful.";
    case "expired":
      return "PAYMENT SESSION EXPIRED";
    case "failed":
    case "not-completed":
      return "PAYMENT NOT COMPLETED";
    case "processing":
      return "Your payment is still being confirmed.";
  }
}

function copyForView(view: PaymentReturnView, timedOut: boolean) {
  switch (view) {
    case "success":
      return "Order received. Your order has been sent to Basilico.";
    case "expired":
      return "This payment session has expired. Your order has not been sent to the kitchen yet. Try payment again when you're ready.";
    case "failed":
      return "Your payment was not completed. Your order has not been sent to the kitchen yet. No successful payment has been recorded for this order.";
    case "not-completed":
      return "Your payment was not completed. Your order has not been sent to the kitchen yet. No successful payment has been recorded for this order.";
    case "processing":
      return timedOut
        ? "Your payment is still being confirmed. Please check again in a moment."
        : "We're waiting for Stripe to confirm the payment. Your basket will be kept until payment is complete.";
  }
}

function displayStatus(status: string) {
  return status.replaceAll("_", " ").toLowerCase();
}

function normalizedStatus(status: string | null | undefined) {
  return status?.trim().toLowerCase() ?? "";
}
