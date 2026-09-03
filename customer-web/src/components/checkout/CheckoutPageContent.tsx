"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useCart } from "@/components/cart/CartProvider";
import { Button, ButtonLink } from "@/components/ui/Button";
import {
  getDeliveryQuote,
  getFulfilmentOptions,
} from "@/lib/api/fulfilment-api";
import { createOrder } from "@/lib/api/order-api";
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
import {
  buildCreateOrderRequest,
  CheckoutOrderRequestError,
} from "@/lib/checkout/order-request";
import { formatGbpPennies } from "@/lib/format-price";
import { getApiErrorMessage } from "@/lib/api/api-error";
import { routes } from "@/lib/routes";
import type { CartLine } from "@/lib/cart/cart-types";
import type {
  BackendDeliveryQuoteResponse,
  BackendFulfilmentOptionsResponse,
} from "@/types/backend-fulfilment";
import type { BackendOrderResponse } from "@/types/backend-order";

type OrderSubmissionState =
  | { status: "idle" }
  | { status: "submitting" }
  | { message: string; status: "error" }
  | { message: string; order: BackendOrderResponse; status: "price-changed" };

type FulfilmentOptionsState =
  | { status: "loading" }
  | { message: string; status: "error" }
  | { options: BackendFulfilmentOptionsResponse; status: "ready" };

type DeliveryCheckState =
  | { status: "idle" }
  | { status: "checking" }
  | {
      message: string;
      normalizedPostcode: string | null;
      quote: BackendDeliveryQuoteResponse;
      status: "eligible";
    }
  | {
      message: string;
      normalizedPostcode: string | null;
      quote: BackendDeliveryQuoteResponse;
      status: "ineligible";
    }
  | { message: string; status: "error" };

type CompletedDeliveryCheckState = Extract<
  DeliveryCheckState,
  { status: "eligible" | "ineligible" }
>;

export function CheckoutPageContent() {
  const router = useRouter();
  const { hydrated, itemCount, items, subtotalPennies } = useCart();
  const [checkoutState, setCheckoutState] =
    useState<CheckoutState>(initialCheckoutState);
  const [errors, setErrors] = useState<CheckoutErrors>({});
  const [draftHydrated, setDraftHydrated] = useState(false);
  const [submission, setSubmission] = useState<OrderSubmissionState>({
    status: "idle",
  });
  const [fulfilmentState, setFulfilmentState] =
    useState<FulfilmentOptionsState>({ status: "loading" });
  const [deliveryCheck, setDeliveryCheck] = useState<DeliveryCheckState>({
    status: "idle",
  });
  const fulfilmentOptions =
    fulfilmentState.status === "ready" ? fulfilmentState.options : null;
  const orderAvailability = fulfilmentOptions?.orderAvailability ?? null;
  const scheduledDateOptions = useMemo(
    () => getScheduledDateOptions(orderAvailability),
    [orderAvailability],
  );
  const scheduledTimeOptions = useMemo(
    () =>
      getScheduledTimeOptions(
        orderAvailability,
        checkoutState.timing.requestedDate,
      ),
    [checkoutState.timing.requestedDate, orderAvailability],
  );
  const onlineOrderingAvailable = Boolean(
    fulfilmentOptions?.collectionEnabled || fulfilmentOptions?.deliveryEnabled,
  );
  const estimatedDeliveryFeePence = useMemo(
    () =>
      getEstimatedDeliveryFeePence(
        checkoutState.fulfilmentType,
        deliveryCheck,
      ),
    [checkoutState.fulfilmentType, deliveryCheck],
  );
  const estimatedTotalPence =
    estimatedDeliveryFeePence === null
      ? null
      : subtotalPennies + estimatedDeliveryFeePence;

  useEffect(() => {
    const hydrationId = window.setTimeout(() => {
      setCheckoutState(readCheckoutDraft());
      setDraftHydrated(true);
    }, 0);

    return () => window.clearTimeout(hydrationId);
  }, []);

  useEffect(() => {
    let mounted = true;

    getFulfilmentOptions()
      .then((options) => {
        if (mounted) {
          setFulfilmentState({ options, status: "ready" });
        }
      })
      .catch((error) => {
        if (mounted) {
          setFulfilmentState({
            message: getApiErrorMessage(
              error,
              "We’re having trouble loading ordering options right now. Please call Basilico on 07424 642900.",
            ),
            status: "error",
          });
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!draftHydrated) {
      return;
    }

    writeCheckoutDraft(checkoutState);
  }, [checkoutState, draftHydrated]);

  useEffect(() => {
    if (!draftHydrated || !fulfilmentOptions) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setCheckoutState((currentState) => {
        if (isFulfilmentEnabled(currentState.fulfilmentType, fulfilmentOptions)) {
          return currentState;
        }

        const availableFulfilmentType =
          getSingleAvailableFulfilmentType(fulfilmentOptions);

        if (currentState.fulfilmentType === availableFulfilmentType) {
          return currentState;
        }

        return {
          ...currentState,
          fulfilmentType: availableFulfilmentType,
        };
      });
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [draftHydrated, fulfilmentOptions]);

  useEffect(() => {
    if (!draftHydrated || !orderAvailability) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setCheckoutState((currentState) => {
        if (
          currentState.timing.type === "asap" &&
          !orderAvailability.asapAvailable
        ) {
          return {
            ...currentState,
            timing: {
              requestedDate: "",
              requestedTime: "",
              type: "",
            },
          };
        }

        if (currentState.timing.type !== "scheduled") {
          return currentState;
        }

        const dateAvailability = orderAvailability.validOrderDates.find(
          (date) => date.date === currentState.timing.requestedDate,
        );
        if (!dateAvailability) {
          return {
            ...currentState,
            timing: {
              requestedDate: "",
              requestedTime: "",
              type: "scheduled",
            },
          };
        }

        if (!dateAvailability.timeSlots.includes(currentState.timing.requestedTime)) {
          return {
            ...currentState,
            timing: {
              ...currentState.timing,
              requestedTime: "",
            },
          };
        }

        return currentState;
      });
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [draftHydrated, orderAvailability]);

  function updateFulfilmentType(fulfilmentType: FulfilmentType) {
    if (!fulfilmentOptions) {
      setErrors((currentErrors) => ({
        ...currentErrors,
        fulfilmentType: "Ordering options are still loading.",
      }));
      return;
    }

    if (!isFulfilmentEnabled(fulfilmentType, fulfilmentOptions)) {
      setErrors((currentErrors) => ({
        ...currentErrors,
        fulfilmentType:
          fulfilmentType === "delivery"
            ? "Delivery is currently unavailable."
            : "Collection is currently unavailable.",
      }));
      return;
    }

    resetSubmission();
    setDeliveryCheck({ status: "idle" });
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
    resetSubmission();
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
    resetSubmission();
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
      setDeliveryCheck({ status: "idle" });
      clearError("addressPostcode");
    }
  }

  function updateTimingType(type: TimingType) {
    if (type === "asap" && orderAvailability?.asapAvailable === false) {
      setErrors((currentErrors) => ({
        ...currentErrors,
        timingType:
          "Basilico is currently closed. Please choose an available order time.",
      }));
      return;
    }

    resetSubmission();
    setCheckoutState((currentState) => ({
      ...currentState,
      timing: {
        requestedDate: type === "asap" ? "" : currentState.timing.requestedDate,
        requestedTime: type === "asap" ? "" : currentState.timing.requestedTime,
        type,
      },
    }));
    clearError("timingType");
  }

  function updateTimingField(
    fieldName: "requestedDate" | "requestedTime",
    value: string,
  ) {
    resetSubmission();
    setCheckoutState((currentState) => ({
      ...currentState,
      timing: {
        requestedDate:
          fieldName === "requestedDate" ? value : currentState.timing.requestedDate,
        requestedTime:
          fieldName === "requestedDate" ? "" : value,
        type: "scheduled",
      },
    }));
    clearError(fieldName);
    if (fieldName === "requestedDate") {
      clearError("requestedTime");
    }
    clearError("timingType");
  }

  function updateNotes(value: string) {
    resetSubmission();
    setCheckoutState((currentState) => ({
      ...currentState,
      notes: value,
    }));
  }

  async function verifyDeliveryPostcode(
    postcode: string,
  ): Promise<DeliveryCheckState> {
    const normalizedPostcode = normalizePostcode(postcode);

    if (!normalizedPostcode) {
      const nextState: DeliveryCheckState = {
        message: "Enter your postcode.",
        status: "error",
      };
      setDeliveryCheck(nextState);
      return nextState;
    }

    setDeliveryCheck({ status: "checking" });

    try {
      const result = await getDeliveryQuote({
        postcode: normalizedPostcode,
      });
      const nextState = toDeliveryCheckState(result);
      const checkedPostcode = result.normalizedPostcode ?? normalizedPostcode;

      setDeliveryCheck(nextState);
      setCheckoutState((currentState) => ({
        ...currentState,
        deliveryAddress: {
          ...currentState.deliveryAddress,
          postcode: checkedPostcode,
        },
      }));

      if (nextState.status === "eligible") {
        clearError("addressPostcode");
      } else {
        setErrors((currentErrors) => ({
          ...currentErrors,
          addressPostcode: nextState.message,
        }));
      }

      return nextState;
    } catch (error) {
      const nextState: DeliveryCheckState = {
        message: getApiErrorMessage(
          error,
          "We could not check delivery for this postcode right now.",
        ),
        status: "error",
      };
      setDeliveryCheck(nextState);
      setErrors((currentErrors) => ({
        ...currentErrors,
        addressPostcode: nextState.message,
      }));
      return nextState;
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submission.status === "submitting") {
      return;
    }

    if (submission.status === "price-changed") {
      router.push(paymentRoute(submission.order.orderReference));
      return;
    }

    const normalizedState = normalizeCheckoutState(checkoutState);
    const nextErrors = validateCheckout(normalizedState, orderAvailability);

    if (fulfilmentState.status === "loading") {
      nextErrors.fulfilmentType = "Ordering options are still loading.";
    } else if (fulfilmentState.status === "error") {
      nextErrors.fulfilmentType = fulfilmentState.message;
    } else if (!onlineOrderingAvailable) {
      nextErrors.fulfilmentType = "Online ordering is temporarily unavailable.";
    } else if (
      !isFulfilmentEnabled(normalizedState.fulfilmentType, fulfilmentState.options)
    ) {
      nextErrors.fulfilmentType =
        normalizedState.fulfilmentType === "delivery"
          ? "Delivery is currently unavailable."
          : "Collection is currently unavailable.";
    }

    setCheckoutState(normalizedState);
    setErrors(nextErrors);

    if (hasCheckoutErrors(nextErrors)) {
      focusFirstInvalidField(nextErrors);
      return;
    }

    if (normalizedState.fulfilmentType === "delivery") {
      const deliveryCheckResult = await verifyDeliveryPostcode(
        normalizedState.deliveryAddress.postcode,
      );

      if (deliveryCheckResult.status !== "eligible") {
        focusFirstInvalidField({
          addressPostcode: getDeliveryCheckMessage(deliveryCheckResult),
        });
        return;
      }
    }

    writeCheckoutDraft(normalizedState);
    setSubmission({ status: "submitting" });

    try {
      const order = await createOrder(
        buildCreateOrderRequest(normalizedState, items),
      );

      if (
        order.subtotalPence !== subtotalPennies ||
        estimatedTotalPence === null ||
        order.totalPence !== estimatedTotalPence
      ) {
        setSubmission({
          message:
            "The order total has changed since this was reviewed. Please check the updated total before continuing to payment.",
          order,
          status: "price-changed",
        });
        return;
      }

      router.push(paymentRoute(order.orderReference));
    } catch (error) {
      setSubmission({
        message:
          error instanceof CheckoutOrderRequestError
            ? error.message
            : getApiErrorMessage(
                error,
                "We couldn’t create your order right now. Please try again.",
              ),
        status: "error",
      });
    }
  }

  function resetSubmission() {
    setSubmission((currentSubmission) =>
      currentSubmission.status === "idle" ? currentSubmission : { status: "idle" },
    );
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
                disabled={!fulfilmentOptions?.deliveryEnabled}
                className={getChoiceClassName(
                  checkoutState.fulfilmentType === "delivery",
                  Boolean(!fulfilmentOptions || !fulfilmentOptions.deliveryEnabled),
                )}
                onClick={() => updateFulfilmentType("delivery")}
                type="button"
              >
                <span>DELIVERY</span>
                <span>
                  {fulfilmentOptions?.deliveryEnabled
                    ? "Delivery is checked from the address postcode you enter below."
                    : "Delivery is currently unavailable."}
                </span>
              </button>
              <button
                aria-pressed={checkoutState.fulfilmentType === "collection"}
                disabled={!fulfilmentOptions?.collectionEnabled}
                className={getChoiceClassName(
                  checkoutState.fulfilmentType === "collection",
                  Boolean(!fulfilmentOptions || !fulfilmentOptions.collectionEnabled),
                )}
                onClick={() => updateFulfilmentType("collection")}
                type="button"
              >
                <span>COLLECTION</span>
                <span>
                  {fulfilmentOptions?.collectionEnabled
                    ? "Collection from Basilico on Trinity Street."
                    : "Collection is currently unavailable."}
                </span>
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
                    onBlur={() => {
                      const normalizedPostcode = normalizePostcode(
                        checkoutState.deliveryAddress.postcode,
                      );
                      updateAddressField("postcode", normalizedPostcode);
                      void verifyDeliveryPostcode(normalizedPostcode);
                    }}
                    onChange={(value) => updateAddressField("postcode", value)}
                    required
                    value={checkoutState.deliveryAddress.postcode}
                  />
                </div>
                <DeliveryCheckMessage deliveryCheck={deliveryCheck} />
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

            <OrderAvailabilityMessage orderAvailability={orderAvailability} />

            <fieldset
              className="checkout-choice-group checkout-choice-group--time"
              data-checkout-field="timingType"
              aria-describedby={errors.timingType ? "timing-type-error" : undefined}
            >
              <legend className="sr-only">Choose fulfilment time</legend>
              <button
                aria-pressed={checkoutState.timing.type === "asap"}
                className={getChoiceClassName(
                  checkoutState.timing.type === "asap",
                  orderAvailability?.asapAvailable === false,
                )}
                disabled={orderAvailability?.asapAvailable === false}
                onClick={() => updateTimingType("asap")}
                type="button"
              >
                <span>AS SOON AS POSSIBLE</span>
                <span>
                  {orderAvailability?.asapAvailable === false
                    ? "Unavailable while Basilico is closed."
                    : "We will confirm timing before payment."}
                </span>
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
          deliveryCheck={deliveryCheck}
          estimatedDeliveryFeePence={estimatedDeliveryFeePence}
          estimatedTotalPence={estimatedTotalPence}
          fulfilmentState={fulfilmentState}
          itemCount={itemCount}
          items={items}
          orderAvailability={orderAvailability}
          onlineOrderingAvailable={onlineOrderingAvailable}
          submission={submission}
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

function OrderAvailabilityMessage({
  orderAvailability,
}: {
  orderAvailability: BackendFulfilmentOptionsResponse["orderAvailability"] | null;
}) {
  if (!orderAvailability) {
    return (
      <p className="type-small checkout-order-availability" role="status">
        Checking Basilico ordering times.
      </p>
    );
  }

  if (orderAvailability.asapAvailable) {
    return null;
  }

  return (
    <div className="checkout-order-availability checkout-order-availability--closed">
      <strong>Basilico is currently closed.</strong>
      <span>{orderAvailability.statusMessage ?? "You can still order for later."}</span>
    </div>
  );
}

type CheckoutReviewProps = {
  checkoutState: CheckoutState;
  deliveryCheck: DeliveryCheckState;
  estimatedDeliveryFeePence: number | null;
  estimatedTotalPence: number | null;
  fulfilmentState: FulfilmentOptionsState;
  itemCount: number;
  items: CartLine[];
  orderAvailability: BackendFulfilmentOptionsResponse["orderAvailability"] | null;
  onlineOrderingAvailable: boolean;
  submission: OrderSubmissionState;
  subtotalPennies: number;
};

function CheckoutReview({
  checkoutState,
  deliveryCheck,
  estimatedDeliveryFeePence,
  estimatedTotalPence,
  fulfilmentState,
  itemCount,
  items,
  orderAvailability,
  onlineOrderingAvailable,
  submission,
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
        <ReviewRow
          label="Requested time"
          value={getTimingLabel(checkoutState, orderAvailability)}
        />
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
          <dt>Food subtotal</dt>
          <dd>{formatGbpPennies(subtotalPennies)}</dd>
        </div>
        <div>
          <dt>{checkoutState.fulfilmentType === "delivery" ? "Delivery" : "Collection"}</dt>
          <dd>
            {getFulfilmentChargeLabel(
              checkoutState.fulfilmentType,
              estimatedDeliveryFeePence,
            )}
          </dd>
        </div>
        <div>
          <dt>Total</dt>
          <dd>
            {estimatedTotalPence === null
              ? "To be confirmed"
              : formatGbpPennies(estimatedTotalPence)}
          </dd>
        </div>
        {checkoutState.fulfilmentType === "delivery" &&
        deliveryCheck.status === "eligible" &&
        deliveryCheck.quote.estimatedDeliveryMinutes !== null ? (
          <div>
            <dt>Estimated delivery</dt>
            <dd>Approx. {deliveryCheck.quote.estimatedDeliveryMinutes} mins</dd>
          </div>
        ) : null}
      </dl>

      {fulfilmentState.status === "loading" ? (
        <p className="type-small checkout-review__notice">
          Checking delivery and collection options.
        </p>
      ) : null}

      {fulfilmentState.status === "error" ? (
        <p
          className="type-small checkout-submit-message checkout-submit-message--error"
          role="alert"
        >
          {fulfilmentState.message}
        </p>
      ) : null}

      {fulfilmentState.status === "ready" && !onlineOrderingAvailable ? (
        <p
          className="type-small checkout-submit-message checkout-submit-message--error"
          role="alert"
        >
          Online ordering is temporarily unavailable.
        </p>
      ) : null}

      {checkoutState.fulfilmentType === "delivery" ? (
        <>
          <p className="type-small checkout-review__notice">
            {checkoutGuidance.delivery}
          </p>
          <DeliveryCheckMessage deliveryCheck={deliveryCheck} />
        </>
      ) : null}

      {submission.status === "price-changed" ? (
        <div className="checkout-submit-message checkout-submit-message--notice" role="status">
          <p className="type-small">{submission.message}</p>
          <dl>
            <div>
              <dt>Food subtotal</dt>
              <dd>{formatGbpPennies(submission.order.subtotalPence)}</dd>
            </div>
            <div>
              <dt>Delivery</dt>
              <dd>
                {submission.order.deliveryFeePence === 0
                  ? "Free"
                  : formatGbpPennies(submission.order.deliveryFeePence)}
              </dd>
            </div>
            <div>
              <dt>Updated total</dt>
              <dd>{formatGbpPennies(submission.order.totalPence)}</dd>
            </div>
            <div>
              <dt>Order reference</dt>
              <dd>{submission.order.orderReference}</dd>
            </div>
          </dl>
        </div>
      ) : null}

      {submission.status === "error" ? (
        <p
          className="type-small checkout-submit-message checkout-submit-message--error"
          role="alert"
        >
          {submission.message}
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

      <Button
        aria-busy={submission.status === "submitting"}
        className="checkout-review__button"
        disabled={isSubmitDisabled(
          submission,
          fulfilmentState,
          onlineOrderingAvailable,
        )}
        type="submit"
      >
        {getSubmitButtonLabel(submission, fulfilmentState, onlineOrderingAvailable)}
      </Button>
    </aside>
  );
}

function DeliveryCheckMessage({
  deliveryCheck,
}: {
  deliveryCheck: DeliveryCheckState;
}) {
  if (deliveryCheck.status === "idle") {
    return null;
  }

  if (deliveryCheck.status === "checking") {
    return (
      <p className="type-small checkout-delivery-status" role="status">
        Checking delivery for this postcode.
      </p>
    );
  }

  const quoteDetails =
    deliveryCheck.status === "eligible"
      ? formatDeliveryQuoteDetails(deliveryCheck.quote)
      : null;

  return (
    <p
      className={`type-small checkout-delivery-status checkout-delivery-status--${deliveryCheck.status}`}
      role={deliveryCheck.status === "eligible" ? "status" : "alert"}
    >
      {deliveryCheck.message}
      {quoteDetails ? <span>{quoteDetails}</span> : null}
    </p>
  );
}

function getSubmitButtonLabel(
  submission: OrderSubmissionState,
  fulfilmentState: FulfilmentOptionsState,
  onlineOrderingAvailable: boolean,
) {
  if (submission.status === "submitting") {
    return "CREATING ORDER...";
  }

  if (submission.status === "price-changed") {
    return "CONTINUE WITH UPDATED TOTAL";
  }

  if (fulfilmentState.status === "loading") {
    return "CHECKING OPTIONS...";
  }

  if (fulfilmentState.status === "error" || !onlineOrderingAvailable) {
    return "ORDERING UNAVAILABLE";
  }

  return "CONTINUE TO PAYMENT";
}

function isSubmitDisabled(
  submission: OrderSubmissionState,
  fulfilmentState: FulfilmentOptionsState,
  onlineOrderingAvailable: boolean,
) {
  if (submission.status === "price-changed") {
    return false;
  }

  return (
    submission.status === "submitting" ||
    fulfilmentState.status !== "ready" ||
    !onlineOrderingAvailable
  );
}

function paymentRoute(orderReference: string) {
  return `${routes.checkoutPayment}?order=${encodeURIComponent(orderReference)}`;
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function getChoiceClassName(selected: boolean, disabled = false) {
  return [
    "checkout-choice",
    selected ? "checkout-choice--selected" : undefined,
    disabled ? "checkout-choice--disabled" : undefined,
  ]
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

function toDeliveryCheckState(
  response: BackendDeliveryQuoteResponse,
): CompletedDeliveryCheckState {
  const message =
    response.message ??
    (response.eligible
      ? "Delivery available for this address."
      : "Sorry, we currently deliver within 6 miles of Basilico.");

  if (response.eligible) {
    return {
      message,
      normalizedPostcode: response.normalizedPostcode,
      quote: response,
      status: "eligible",
    };
  }

  return {
    message,
    normalizedPostcode: response.normalizedPostcode,
    quote: response,
    status: "ineligible",
  };
}

function isFulfilmentEnabled(
  fulfilmentType: CheckoutState["fulfilmentType"],
  options: BackendFulfilmentOptionsResponse,
) {
  if (fulfilmentType === "collection") {
    return options.collectionEnabled;
  }

  if (fulfilmentType === "delivery") {
    return options.deliveryEnabled;
  }

  return false;
}

function getSingleAvailableFulfilmentType(
  options: BackendFulfilmentOptionsResponse,
): CheckoutState["fulfilmentType"] {
  if (options.collectionEnabled && !options.deliveryEnabled) {
    return "collection";
  }

  if (options.deliveryEnabled && !options.collectionEnabled) {
    return "delivery";
  }

  return "";
}

function getEstimatedDeliveryFeePence(
  fulfilmentType: CheckoutState["fulfilmentType"],
  deliveryCheck: DeliveryCheckState,
) {
  if (!fulfilmentType) {
    return null;
  }

  if (fulfilmentType === "collection") {
    return 0;
  }

  return deliveryCheck.status === "eligible"
    ? deliveryCheck.quote.deliveryFeePence
    : null;
}

function getFulfilmentChargeLabel(
  fulfilmentType: CheckoutState["fulfilmentType"],
  deliveryFeePence: number | null,
) {
  if (fulfilmentType === "collection") {
    return "Free";
  }

  if (fulfilmentType === "delivery") {
    if (deliveryFeePence === null) {
      return "To be confirmed";
    }

    return deliveryFeePence === 0 ? "FREE" : formatGbpPennies(deliveryFeePence);
  }

  return "Choose option";
}

function getDeliveryCheckMessage(deliveryCheck: DeliveryCheckState) {
  if (deliveryCheck.status === "idle" || deliveryCheck.status === "checking") {
    return "Check your delivery postcode.";
  }

  return deliveryCheck.message;
}

function formatDeliveryQuoteDetails(quote: BackendDeliveryQuoteResponse) {
  const details: string[] = [];

  if (quote.distanceMiles !== null) {
    details.push(`${quote.distanceMiles.toFixed(1)} miles from Basilico`);
  }

  if (quote.deliveryFeePence !== null) {
    details.push(`${formatGbpPennies(quote.deliveryFeePence)} delivery`);
  }

  if (quote.estimatedDeliveryMinutes !== null) {
    details.push(`Approx. ${quote.estimatedDeliveryMinutes} mins`);
  }

  return details.length > 0 ? details.join(" · ") : null;
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
