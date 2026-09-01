"use client";

import Link from "next/link";
import {
  type ChangeEvent,
  type FormEvent,
  useMemo,
  useState,
} from "react";
import { Button } from "@/components/ui/Button";
import { createBooking } from "@/lib/api/booking-api";
import { getApiErrorMessage } from "@/lib/api/api-error";
import type {
  BookingErrors,
  BookingState,
} from "@/lib/booking/booking-types";
import { buildCreateBookingRequest } from "@/lib/booking/booking-request";
import {
  basilicoBookingDetails,
  formatBookingDateLabel,
  getBookingDateError,
  getBookingTimeOptions,
  getFirstBookingError,
  getTodayDateValue,
  hasBookingErrors,
  initialBookingState,
  normalizeBookingState,
  validateBooking,
} from "@/lib/booking/booking-utils";
import { routes } from "@/lib/routes";
import type { BackendBookingResponse } from "@/types/backend-booking";

type BookingSubmissionState =
  | { status: "idle" }
  | { status: "submitting" }
  | { message: string; status: "error" }
  | { booking: BackendBookingResponse; status: "success" };

export function BookingPageContent() {
  const [booking, setBooking] = useState<BookingState>(initialBookingState);
  const [errors, setErrors] = useState<BookingErrors>({});
  const [submission, setSubmission] = useState<BookingSubmissionState>({
    status: "idle",
  });
  const today = useMemo(() => getTodayDateValue(), []);
  const timeOptions = useMemo(() => getBookingTimeOptions(), []);
  const liveDateError = booking.date ? getBookingDateError(booking.date) : "";
  const dateError = errors.date ?? liveDateError;
  const partySizeLabel =
    booking.partySize === 1
      ? "1 guest"
      : booking.partySize > 1
        ? `${booking.partySize} guests`
        : "Choose party size";

  function commitBookingState(nextBooking: BookingState) {
    setBooking(nextBooking);
    resetSubmission();

    if (hasBookingErrors(errors)) {
      setErrors(validateBooking(nextBooking));
    }
  }

  function updateBookingField(
    field: "date" | "requests" | "time",
    value: string,
  ) {
    commitBookingState({
      ...booking,
      [field]: value,
    });
  }

  function updatePartySize(event: ChangeEvent<HTMLInputElement>) {
    const nextPartySize = Number.parseInt(event.target.value, 10);

    commitBookingState({
      ...booking,
      partySize: Number.isNaN(nextPartySize) ? 0 : nextPartySize,
    });
  }

  function updateCustomerField(
    field: keyof BookingState["customer"],
    value: string,
  ) {
    commitBookingState({
      ...booking,
      customer: {
        ...booking.customer,
        [field]: value,
      },
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submission.status === "submitting") {
      return;
    }

    const normalizedBooking = normalizeBookingState(booking);
    const nextErrors = validateBooking(normalizedBooking);

    if (hasBookingErrors(nextErrors)) {
      setErrors(nextErrors);
      setSubmission({ status: "idle" });
      focusFirstInvalidField(nextErrors);
      return;
    }

    setBooking(normalizedBooking);
    setErrors({});
    setSubmission({ status: "submitting" });

    try {
      const response = await createBooking(
        buildCreateBookingRequest(normalizedBooking),
      );
      setSubmission({
        booking: response,
        status: "success",
      });
    } catch (error) {
      setSubmission({
        message: getApiErrorMessage(
          error,
          "We couldn’t send your booking request right now. Please try again.",
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

  return (
    <div className="booking-page__layout">
      <section className="booking-page__intro" aria-labelledby="book-page-title">
        <Link className="booking-page__back" href={routes.home}>
          ← Back to Basilico
        </Link>

        <div className="booking-page__header">
          <p className="type-eyebrow booking-page__eyebrow">Book a table</p>
          <h1 className="type-h1 booking-page__title" id="book-page-title">
            Your table at Basilico.
          </h1>
          <p className="type-body booking-page__copy">
            Choose your date, time and party size and send us your booking
            request.
          </p>
        </div>

        <article className="booking-details" aria-label="Restaurant details">
          <div>
            <p className="type-eyebrow booking-details__eyebrow">Visit us</p>
            <address className="booking-details__address">
              {basilicoBookingDetails.address.map((line) => (
                <span key={line}>{line}</span>
              ))}
            </address>
          </div>

          <dl className="booking-details__list">
            <div>
              <dt>Phone</dt>
              <dd>
                <a href={basilicoBookingDetails.phoneHref}>
                  {basilicoBookingDetails.phone}
                </a>
              </dd>
            </div>
            <div>
              <dt>Opening hours</dt>
              <dd>
                <span>{basilicoBookingDetails.hours[0]}</span>
                <span>{basilicoBookingDetails.hours[1]}</span>
                <span>
                  {basilicoBookingDetails.hours[2]} —{" "}
                  {basilicoBookingDetails.hours[3]}
                </span>
              </dd>
            </div>
          </dl>
        </article>
      </section>

      <form className="booking-form" onSubmit={handleSubmit} noValidate>
        <section className="checkout-section" aria-labelledby="booking-date-title">
          <header className="checkout-section__header">
            <p className="type-eyebrow checkout-section__step">01</p>
            <h2 className="type-h3 checkout-section__title" id="booking-date-title">
              Date, time and guests
            </h2>
            <p className="type-small checkout-section__copy">
              {basilicoBookingDetails.timingNotice}
            </p>
          </header>

          <div className="checkout-field-grid">
            <label className="checkout-field">
              <span>Date</span>
              <input
                aria-describedby={dateError ? "booking-date-error" : undefined}
                aria-invalid={Boolean(dateError)}
                data-booking-field="date"
                min={today}
                onChange={(event) => updateBookingField("date", event.target.value)}
                type="date"
                value={booking.date}
              />
              {dateError ? (
                <p className="checkout-field__error" id="booking-date-error">
                  {dateError}
                </p>
              ) : null}
            </label>

            <label className="checkout-field">
              <span>Time</span>
              <select
                aria-describedby={errors.time ? "booking-time-error" : undefined}
                aria-invalid={Boolean(errors.time)}
                data-booking-field="time"
                onChange={(event) => updateBookingField("time", event.target.value)}
                value={booking.time}
              >
                <option value="">Choose a time</option>
                {timeOptions.map((time) => (
                  <option key={time} value={time}>
                    {time}
                  </option>
                ))}
              </select>
              {errors.time ? (
                <p className="checkout-field__error" id="booking-time-error">
                  {errors.time}
                </p>
              ) : null}
            </label>

            <label className="checkout-field">
              <span>Party size</span>
              <input
                aria-describedby={
                  errors.partySize ? "booking-party-size-error" : undefined
                }
                aria-invalid={Boolean(errors.partySize)}
                data-booking-field="partySize"
                min={1}
                onChange={updatePartySize}
                type="number"
                value={booking.partySize || ""}
              />
              {errors.partySize ? (
                <p className="checkout-field__error" id="booking-party-size-error">
                  {errors.partySize}
                </p>
              ) : null}
            </label>
          </div>
        </section>

        <section className="checkout-section" aria-labelledby="booking-contact-title">
          <header className="checkout-section__header">
            <p className="type-eyebrow checkout-section__step">02</p>
            <h2 className="type-h3 checkout-section__title" id="booking-contact-title">
              Your details
            </h2>
          </header>

          <div className="checkout-field-grid">
            <label className="checkout-field">
              <span>First name</span>
              <input
                aria-describedby={
                  errors.firstName ? "booking-first-name-error" : undefined
                }
                aria-invalid={Boolean(errors.firstName)}
                autoComplete="given-name"
                data-booking-field="firstName"
                onChange={(event) =>
                  updateCustomerField("firstName", event.target.value)
                }
                type="text"
                value={booking.customer.firstName}
              />
              {errors.firstName ? (
                <p className="checkout-field__error" id="booking-first-name-error">
                  {errors.firstName}
                </p>
              ) : null}
            </label>

            <label className="checkout-field">
              <span>Last name</span>
              <input
                aria-describedby={
                  errors.lastName ? "booking-last-name-error" : undefined
                }
                aria-invalid={Boolean(errors.lastName)}
                autoComplete="family-name"
                data-booking-field="lastName"
                onChange={(event) =>
                  updateCustomerField("lastName", event.target.value)
                }
                type="text"
                value={booking.customer.lastName}
              />
              {errors.lastName ? (
                <p className="checkout-field__error" id="booking-last-name-error">
                  {errors.lastName}
                </p>
              ) : null}
            </label>

            <label className="checkout-field">
              <span>Mobile number</span>
              <input
                aria-describedby={errors.phone ? "booking-phone-error" : undefined}
                aria-invalid={Boolean(errors.phone)}
                autoComplete="tel"
                data-booking-field="phone"
                inputMode="tel"
                onChange={(event) => updateCustomerField("phone", event.target.value)}
                type="tel"
                value={booking.customer.phone}
              />
              {errors.phone ? (
                <p className="checkout-field__error" id="booking-phone-error">
                  {errors.phone}
                </p>
              ) : null}
            </label>

            <label className="checkout-field">
              <span>Email address</span>
              <input
                aria-describedby={errors.email ? "booking-email-error" : undefined}
                aria-invalid={Boolean(errors.email)}
                autoComplete="email"
                data-booking-field="email"
                onChange={(event) => updateCustomerField("email", event.target.value)}
                type="email"
                value={booking.customer.email}
              />
              {errors.email ? (
                <p className="checkout-field__error" id="booking-email-error">
                  {errors.email}
                </p>
              ) : null}
            </label>
          </div>
        </section>

        <section className="checkout-section" aria-labelledby="booking-requests-title">
          <header className="checkout-section__header">
            <p className="type-eyebrow checkout-section__step">03</p>
            <h2 className="type-h3 checkout-section__title" id="booking-requests-title">
              Special requests
            </h2>
          </header>

          <label className="checkout-field">
            <span>Special requests</span>
            <textarea
              onChange={(event) => updateBookingField("requests", event.target.value)}
              placeholder="High chair, accessibility needs, celebration or anything we should know."
              value={booking.requests}
            />
          </label>

          <p className="type-small checkout-allergy">
            {basilicoBookingDetails.allergyMessage}
          </p>
        </section>

        <section className="booking-summary" aria-labelledby="booking-summary-title">
          <div>
            <p className="type-eyebrow booking-summary__eyebrow">Booking request</p>
            <h2 className="type-h3 booking-summary__title" id="booking-summary-title">
              Review your table.
            </h2>
          </div>

          <dl className="booking-summary__details">
            <div>
              <dt>Date</dt>
              <dd>{formatBookingDateLabel(booking.date)}</dd>
            </div>
            <div>
              <dt>Time</dt>
              <dd>{booking.time || "Choose a time"}</dd>
            </div>
            <div>
              <dt>Party size</dt>
              <dd>{partySizeLabel}</dd>
            </div>
          </dl>

          <div className="booking-form__actions">
            <Button
              aria-busy={submission.status === "submitting"}
              className="booking-form__button"
              disabled={submission.status === "submitting"}
              type="submit"
            >
              {submission.status === "submitting"
                ? "SENDING REQUEST..."
                : "SEND BOOKING REQUEST"}
            </Button>

            {submission.status === "success" ? (
              <div className="booking-confirmation" aria-live="polite" role="status">
                <p className="type-eyebrow booking-confirmation__eyebrow">
                  Booking request received.
                </p>
                <dl className="booking-confirmation__details">
                  <div>
                    <dt>Reference</dt>
                    <dd>{submission.booking.bookingReference}</dd>
                  </div>
                  <div>
                    <dt>Date</dt>
                    <dd>{formatBookingDateLabel(submission.booking.date)}</dd>
                  </div>
                  <div>
                    <dt>Time</dt>
                    <dd>{formatBookingTime(submission.booking.time)}</dd>
                  </div>
                  <div>
                    <dt>Party size</dt>
                    <dd>
                      {submission.booking.partySize}{" "}
                      {submission.booking.partySize === 1 ? "guest" : "guests"}
                    </dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>Requested</dd>
                  </div>
                </dl>
                <p className="type-small">
                  We’ll confirm your table separately. For anything urgent, call{" "}
                  <a href={basilicoBookingDetails.phoneHref}>
                    {basilicoBookingDetails.phone}
                  </a>
                  .
                </p>
              </div>
            ) : null}

            {submission.status === "error" ? (
              <p className="type-small booking-form__error" role="alert">
                {submission.message}
              </p>
            ) : null}
          </div>
        </section>
      </form>
    </div>
  );
}

function formatBookingTime(time: string) {
  return time.slice(0, 5);
}

function focusFirstInvalidField(errors: BookingErrors) {
  const firstInvalidField = getFirstBookingError(errors);

  if (!firstInvalidField) {
    return;
  }

  window.requestAnimationFrame(() => {
    const field = document.querySelector<HTMLElement>(
      `[data-booking-field="${firstInvalidField}"]`,
    );

    field?.focus();
    field?.scrollIntoView({ block: "center" });
  });
}
