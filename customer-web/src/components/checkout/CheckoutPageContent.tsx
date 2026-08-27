"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useCart } from "@/components/cart/CartProvider";
import { Button, ButtonLink } from "@/components/ui/Button";
import {
  CHECKOUT_STORAGE_KEY,
  type CheckoutErrors,
  type CheckoutFieldName,
  type CheckoutState,
  type DeliveryAddress,
  type FulfilmentType,
  type TimingType,
} from "@/lib/checkout/checkout-types";
import {
  basilicoCollectionAddress,
  checkoutGuidance,
  getFirstCheckoutError,
  getScheduledDateOptions,
  getScheduledTimeOptions,
  getTimingLabel,
  hasCheckoutErrors,
  initialCheckoutState,
  normalizeCheckoutState,
  normalizePostcode,
  parseCheckoutDraft,
  serializeCheckoutDraft,
  validateCheckout,
} from "@/lib/checkout/checkout-utils";
import { formatGbpPennies } from "@/lib/format-price";
import { routes } from "@/lib/routes";
import type { CartLine } from "@/lib/cart/cart-types";

export function CheckoutPageContent() {
  const router = useRouter();
  const { hydrated, itemCount, items, subtotalPennies } = useCart();
  const [checkoutState, setCheckoutState] =
    useState<CheckoutState>(initialCheckoutState);
  const [errors, setErrors] = useState<CheckoutErrors>({});
  const [draftHydrated, setDraftHydrated] = useState(false);
  const scheduledDateOptions = useMemo(() => getScheduledDateOptions(), []);
  const scheduledTimeOptions = useMemo(() => getScheduledTimeOptions(), []);

  useEffect(() => {
    const hydrationId = window.setTimeout(() => {
      setCheckoutState(readCheckoutDraft());
      setDraftHydrated(true);
    }, 0);

    return () => window.clearTimeout(hydrationId);
  }, []);

  useEffect(() => {
    if (!draftHydrated) {
      return;
    }

    writeCheckoutDraft(checkoutState);
  }, [checkoutState, draftHydrated]);

  function updateFulfilmentType(fulfilmentType: FulfilmentType) {
    setCheckoutState((currentState) => ({
      ...currentState,
      fulfilmentType,
    }));
    clearError("fulfilmentType");
  }

  function updateCustomerField(
    fieldName: keyof CheckoutState["customer"],
    value: string,
  ) {
    setCheckoutState((currentState) => ({
      ...currentState,
      customer: {
        ...currentState.customer,
        [fieldName]: value,
      },
    }));
    clearError(fieldName);
  }

  function updateAddressField(fieldName: keyof DeliveryAddress, value: string) {
    const normalizedValue =
      fieldName === "postcode" ? value.toUpperCase() : value;

    setCheckoutState((currentState) => ({
      ...currentState,
      deliveryAddress: {
        ...currentState.deliveryAddress,
        [fieldName]: normalizedValue,
      },
    }));

    if (fieldName === "line1") {
      clearError("addressLine1");
    }

    if (fieldName === "city") {
      clearError("addressCity");
    }

    if (fieldName === "postcode") {
      clearError("addressPostcode");
    }
  }

  function updateTimingType(type: TimingType) {
    setCheckoutState((currentState) => ({
      ...currentState,
      timing: {
        ...currentState.timing,
        type,
      },
    }));
    clearError("timingType");
  }

  function updateTimingField(
    fieldName: "requestedDate" | "requestedTime",
    value: string,
  ) {
    setCheckoutState((currentState) => ({
      ...currentState,
      timing: {
        ...currentState.timing,
        [fieldName]: value,
        type: "scheduled",
      },
    }));
    clearError(fieldName);
    clearError("timingType");
  }

  function updateNotes(value: string) {
    setCheckoutState((currentState) => ({
      ...currentState,
      notes: value,
    }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedState = normalizeCheckoutState(checkoutState);
    const nextErrors = validateCheckout(normalizedState);

    setCheckoutState(normalizedState);
    setErrors(nextErrors);

    if (hasCheckoutErrors(nextErrors)) {
      focusFirstInvalidField(nextErrors);
      return;
    }

    writeCheckoutDraft(normalizedState);
    router.push("/checkout/payment");
  }

  function clearError(fieldName: CheckoutFieldName) {
    setErrors((currentErrors) => {
      if (!currentErrors[fieldName]) {
        return currentErrors;
      }

      const nextErrors = { ...currentErrors };
      delete nextErrors[fieldName];
      return nextErrors;
    });
  }

  if (!hydrated || !draftHydrated) {
    return (
      <section className="checkout-loading" aria-labelledby="checkout-page-title">
        <p className="type-eyebrow checkout-page__eyebrow">Checkout</p>
        <h1 className="type-h1 checkout-page__title" id="checkout-page-title">
          Your checkout.
        </h1>
        <p className="type-body checkout-page__copy">Checking your order.</p>
      </section>
    );
  }

  if (items.length === 0) {
    return (
      <section className="checkout-empty" aria-labelledby="checkout-page-title">
        <p className="type-eyebrow checkout-page__eyebrow">Checkout</p>
        <h1 className="type-h1 checkout-page__title" id="checkout-page-title">
          Your basket is empty.
        </h1>
        <p className="type-body checkout-page__copy">
          Add something from the Basilico menu before continuing to checkout.
        </p>
        <ButtonLink href={routes.menu} variant="primary">
          VIEW MENU
        </ButtonLink>
      </section>
    );
  }

  return (
    <>
      <header className="checkout-page__header">
        <Link className="checkout-page__back" href={routes.basket}>
          ← Back to basket
        </Link>
        <p className="type-eyebrow checkout-page__eyebrow">Checkout</p>
        <h1 className="type-h1 checkout-page__title" id="checkout-page-title">
          Your checkout.
        </h1>
        <p className="type-body checkout-page__copy">
          Choose how you would like your Basilico order, then review the details
          before payment.
        </p>
      </header>

      <form className="checkout-flow" noValidate onSubmit={handleSubmit}>
        <div className="checkout-flow__form">
          <section className="checkout-section" aria-labelledby="fulfilment-title">
            <div className="checkout-section__header">
              <p className="type-eyebrow checkout-section__step">01 Fulfilment</p>
              <h2 className="type-h3 checkout-section__title" id="fulfilment-title">
                Delivery or collection.
              </h2>
            </div>

            <fieldset
              className="checkout-choice-group"
              data-checkout-field="fulfilmentType"
              aria-describedby={errors.fulfilmentType ? "fulfilment-error" : undefined}
            >
              <legend className="sr-only">Choose delivery or collection</legend>
              <button
                aria-pressed={checkoutState.fulfilmentType === "delivery"}
                className={getChoiceClassName(
                  checkoutState.fulfilmentType === "delivery",
                )}
                onClick={() => updateFulfilmentType("delivery")}
                type="button"
              >
                <span>DELIVERY</span>
                <span>
                  Delivery availability and any applicable delivery charge will
                  be confirmed before payment.
                </span>
              </button>
              <button
                aria-pressed={checkoutState.fulfilmentType === "collection"}
                className={getChoiceClassName(
                  checkoutState.fulfilmentType === "collection",
                )}
                onClick={() => updateFulfilmentType("collection")}
                type="button"
              >
                <span>COLLECTION</span>
                <span>Collection from Basilico on Trinity Street.</span>
              </button>
            </fieldset>
            <FieldError id="fulfilment-error" message={errors.fulfilmentType} />

            {checkoutState.fulfilmentType === "collection" ? (
              <div className="checkout-info-panel">
                <p className="type-eyebrow checkout-info-panel__eyebrow">
                  Collection from Basilico
                </p>
                <address className="checkout-info-panel__address">
                  {basilicoCollectionAddress.map((line) => (
                    <span key={line}>{line}</span>
                  ))}
                </address>
              </div>
            ) : null}

            {checkoutState.fulfilmentType === "delivery" ? (
              <div className="checkout-delivery">
                <p className="type-small checkout-delivery__notice">
                  {checkoutGuidance.delivery}
                </p>
                <CheckoutInput
                  autoComplete="address-line1"
                  error={errors.addressLine1}
                  fieldName="addressLine1"
                  id="delivery-line1"
                  label="Address line 1"
                  onChange={(value) => updateAddressField("line1", value)}
                  required
                  value={checkoutState.deliveryAddress.line1}
                />
                <CheckoutInput
                  autoComplete="address-line2"
                  fieldName="addressLine2"
                  id="delivery-line2"
                  label="Address line 2"
                  onChange={(value) => updateAddressField("line2", value)}
                  value={checkoutState.deliveryAddress.line2}
                />
                <div className="checkout-field-grid">
                  <CheckoutInput
                    autoComplete="address-level2"
                    error={errors.addressCity}
                    fieldName="addressCity"
                    id="delivery-city"
                    label="Town / City"
                    onChange={(value) => updateAddressField("city", value)}
                    required
                    value={checkoutState.deliveryAddress.city}
                  />
                  <CheckoutInput
                    autoComplete="postal-code"
                    error={errors.addressPostcode}
                    fieldName="addressPostcode"
                    id="delivery-postcode"
                    label="Postcode"
                    onBlur={() =>
                      updateAddressField(
                        "postcode",
                        normalizePostcode(checkoutState.deliveryAddress.postcode),
                      )
                    }
                    onChange={(value) => updateAddressField("postcode", value)}
                    required
                    value={checkoutState.deliveryAddress.postcode}
                  />
                </div>
              </div>
            ) : null}
          </section>

          <section className="checkout-section" aria-labelledby="details-title">
            <div className="checkout-section__header">
              <p className="type-eyebrow checkout-section__step">02 Your details</p>
              <h2 className="type-h3 checkout-section__title" id="details-title">
                How we contact you.
              </h2>
            </div>
            <div className="checkout-field-grid">
              <CheckoutInput
                autoComplete="given-name"
                error={errors.firstName}
                fieldName="firstName"
                id="first-name"
                label="First name"
                onChange={(value) => updateCustomerField("firstName", value)}
                required
                value={checkoutState.customer.firstName}
              />
              <CheckoutInput
                autoComplete="family-name"
                error={errors.lastName}
                fieldName="lastName"
                id="last-name"
                label="Last name"
                onChange={(value) => updateCustomerField("lastName", value)}
                required
                value={checkoutState.customer.lastName}
              />
            </div>
            <div className="checkout-field-grid">
              <CheckoutInput
                autoComplete="tel"
                error={errors.phone}
                fieldName="phone"
                id="mobile-number"
                inputMode="tel"
                label="Mobile number"
                onChange={(value) => updateCustomerField("phone", value)}
                required
                type="tel"
                value={checkoutState.customer.phone}
              />
              <CheckoutInput
                autoComplete="email"
                error={errors.email}
                fieldName="email"
                id="email-address"
                inputMode="email"
                label="Email address"
                onChange={(value) => updateCustomerField("email", value)}
                required
                type="email"
                value={checkoutState.customer.email}
              />
            </div>
          </section>

          <section className="checkout-section" aria-labelledby="time-title">
            <div className="checkout-section__header">
              <p className="type-eyebrow checkout-section__step">03 Time & notes</p>
              <h2 className="type-h3 checkout-section__title" id="time-title">
                When would you like it?
              </h2>
              <p className="type-small checkout-section__copy">
                {checkoutGuidance.hours}
              </p>
            </div>

            <fieldset
              className="checkout-choice-group checkout-choice-group--time"
              data-checkout-field="timingType"
              aria-describedby={errors.timingType ? "timing-type-error" : undefined}
            >
              <legend className="sr-only">Choose fulfilment time</legend>
              <button
                aria-pressed={checkoutState.timing.type === "asap"}
                className={getChoiceClassName(checkoutState.timing.type === "asap")}
                onClick={() => updateTimingType("asap")}
                type="button"
              >
                <span>AS SOON AS POSSIBLE</span>
                <span>We will confirm timing before payment.</span>
              </button>
              <button
                aria-pressed={checkoutState.timing.type === "scheduled"}
                className={getChoiceClassName(
                  checkoutState.timing.type === "scheduled",
                )}
                onClick={() => updateTimingType("scheduled")}
                type="button"
              >
                <span>CHOOSE A TIME</span>
                <span>{checkoutGuidance.time}</span>
              </button>
            </fieldset>
            <FieldError id="timing-type-error" message={errors.timingType} />

            {checkoutState.timing.type === "scheduled" ? (
              <div className="checkout-schedule">
                <label className="checkout-field" data-checkout-field="requestedDate">
                  <span>Requested day</span>
                  <select
                    aria-describedby={
                      errors.requestedDate ? "requested-date-error" : undefined
                    }
                    aria-invalid={Boolean(errors.requestedDate)}
                    onChange={(event) =>
                      updateTimingField("requestedDate", event.target.value)
                    }
                    required
                    value={checkoutState.timing.requestedDate}
                  >
                    <option value="">Choose a day</option>
                    {scheduledDateOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <FieldError
                    id="requested-date-error"
                    message={errors.requestedDate}
                  />
                </label>

                <label className="checkout-field" data-checkout-field="requestedTime">
                  <span>Requested time</span>
                  <select
                    aria-describedby={
                      errors.requestedTime ? "requested-time-error" : undefined
                    }
                    aria-invalid={Boolean(errors.requestedTime)}
                    onChange={(event) =>
                      updateTimingField("requestedTime", event.target.value)
                    }
                    required
                    value={checkoutState.timing.requestedTime}
                  >
                    <option value="">Choose a time</option>
                    {scheduledTimeOptions.map((time) => (
                      <option key={time} value={time}>
                        {time}
                      </option>
                    ))}
                  </select>
                  <FieldError
                    id="requested-time-error"
                    message={errors.requestedTime}
                  />
                </label>
              </div>
            ) : null}

            <label className="checkout-field checkout-field--notes">
              <span>Order notes</span>
              <textarea
                onChange={(event) => updateNotes(event.target.value)}
                placeholder="Allergies, delivery instructions or anything we should know?"
                rows={4}
                value={checkoutState.notes}
              />
            </label>
            <p className="type-small checkout-allergy">{checkoutGuidance.notes}</p>
          </section>
        </div>

        <CheckoutReview
          checkoutState={checkoutState}
          itemCount={itemCount}
          items={items}
          subtotalPennies={subtotalPennies}
        />
      </form>
    </>
  );
}

type CheckoutInputProps = {
  autoComplete?: string;
  error?: string;
  fieldName: CheckoutFieldName | "addressLine2";
  id: string;
  inputMode?: "email" | "tel" | "text";
  label: string;
  onBlur?: () => void;
  onChange: (value: string) => void;
  required?: boolean;
  type?: "email" | "tel" | "text";
  value: string;
};

function CheckoutInput({
  autoComplete,
  error,
  fieldName,
  id,
  inputMode,
  label,
  onBlur,
  onChange,
  required = false,
  type = "text",
  value,
}: CheckoutInputProps) {
  const errorId = `${id}-error`;

  return (
    <label className="checkout-field" data-checkout-field={fieldName}>
      <span>{label}</span>
      <input
        aria-describedby={error ? errorId : undefined}
        aria-invalid={Boolean(error)}
        autoComplete={autoComplete}
        id={id}
        inputMode={inputMode}
        onBlur={onBlur}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        type={type}
        value={value}
      />
      <FieldError id={errorId} message={error} />
    </label>
  );
}

type FieldErrorProps = {
  id: string;
  message?: string;
};

function FieldError({ id, message }: FieldErrorProps) {
  if (!message) {
    return null;
  }

  return (
    <p className="type-small checkout-field__error" id={id} role="alert">
      {message}
    </p>
  );
}

type CheckoutReviewProps = {
  checkoutState: CheckoutState;
  itemCount: number;
  items: CartLine[];
  subtotalPennies: number;
};

function CheckoutReview({
  checkoutState,
  itemCount,
  items,
  subtotalPennies,
}: CheckoutReviewProps) {
  const fullName = [checkoutState.customer.firstName, checkoutState.customer.lastName]
    .filter(Boolean)
    .join(" ");

  return (
    <aside className="checkout-review" aria-labelledby="checkout-review-title">
      <div>
        <p className="type-eyebrow checkout-review__eyebrow">04 Review order</p>
        <h2 className="type-h3 checkout-review__title" id="checkout-review-title">
          Check the details.
        </h2>
      </div>

      <dl className="checkout-review__details">
        <ReviewRow label="Fulfilment" value={getFulfilmentLabel(checkoutState)} />
        <ReviewRow label="Name" value={fullName || "Not entered"} />
        <ReviewRow
          label="Mobile"
          value={checkoutState.customer.phone || "Not entered"}
        />
        <ReviewRow
          label="Email"
          value={checkoutState.customer.email || "Not entered"}
        />
        {checkoutState.fulfilmentType === "delivery" ? (
          <ReviewRow
            label="Delivery address"
            value={formatDeliveryAddress(checkoutState.deliveryAddress)}
          />
        ) : null}
        <ReviewRow label="Requested time" value={getTimingLabel(checkoutState)} />
        {checkoutState.notes.trim() ? (
          <ReviewRow label="Order notes" value={checkoutState.notes.trim()} />
        ) : null}
      </dl>

      <div className="checkout-review__items">
        <p className="type-eyebrow checkout-review__items-title">
          Basket items
        </p>
        <ul className="checkout-review__list">
          {items.map((item) => (
            <li className="checkout-review__item" key={item.id}>
              <div>
                <span>
                  {item.quantity} × {item.name}
                </span>
                {item.lineType === "custom-pizza" &&
                item.selectedToppings?.length ? (
                  <small>{item.selectedToppings.join(" · ")}</small>
                ) : null}
              </div>
              <strong>
                {formatGbpPennies(item.unitPricePennies * item.quantity)}
              </strong>
            </li>
          ))}
        </ul>
      </div>

      <dl className="checkout-review__subtotal">
        <div>
          <dt>Items</dt>
          <dd>{itemCount}</dd>
        </div>
        <div>
          <dt>Subtotal</dt>
          <dd>{formatGbpPennies(subtotalPennies)}</dd>
        </div>
      </dl>

      {checkoutState.fulfilmentType === "delivery" ? (
        <p className="type-small checkout-review__notice">
          {checkoutGuidance.delivery}
        </p>
      ) : null}

      <section className="checkout-payment" aria-labelledby="payment-title">
        <p className="type-eyebrow checkout-payment__eyebrow">Payment</p>
        <h3 className="type-h3 checkout-payment__title" id="payment-title">
          Secure payment next.
        </h3>
        <p className="type-small checkout-payment__copy">
          Payment will be completed securely in the next step.
        </p>
      </section>

      <Button className="checkout-review__button" type="submit">
        CONTINUE TO PAYMENT
      </Button>
    </aside>
  );
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function getChoiceClassName(selected: boolean) {
  return ["checkout-choice", selected ? "checkout-choice--selected" : undefined]
    .filter(Boolean)
    .join(" ");
}

function getFulfilmentLabel(state: CheckoutState) {
  if (state.fulfilmentType === "delivery") {
    return "DELIVERY";
  }

  if (state.fulfilmentType === "collection") {
    return "COLLECTION";
  }

  return "Not selected";
}

function formatDeliveryAddress(address: DeliveryAddress) {
  const lines = [
    address.line1,
    address.line2,
    address.city,
    normalizePostcode(address.postcode),
  ].filter(Boolean);

  return lines.length > 0 ? lines.join(", ") : "Not entered";
}

function focusFirstInvalidField(errors: CheckoutErrors) {
  const firstError = getFirstCheckoutError(errors);

  if (!firstError) {
    return;
  }

  window.requestAnimationFrame(() => {
    const field = document.querySelector<HTMLElement>(
      `[data-checkout-field="${firstError}"]`,
    );
    const focusTarget =
      field?.matches("input, select, textarea, button") === true
        ? field
        : field?.querySelector<HTMLElement>("input, select, textarea, button");

    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;

    field?.scrollIntoView({
      behavior: prefersReducedMotion ? "auto" : "smooth",
      block: "center",
    });
    focusTarget?.focus({ preventScroll: true });
  });
}

function readCheckoutDraft() {
  try {
    return parseCheckoutDraft(window.sessionStorage.getItem(CHECKOUT_STORAGE_KEY));
  } catch {
    return initialCheckoutState;
  }
}

function writeCheckoutDraft(state: CheckoutState) {
  try {
    window.sessionStorage.setItem(
      CHECKOUT_STORAGE_KEY,
      JSON.stringify(serializeCheckoutDraft(state)),
    );
  } catch {
    // Draft persistence is a convenience; checkout still works without storage.
  }
}
