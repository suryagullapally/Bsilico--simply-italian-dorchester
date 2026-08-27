import {
  CHECKOUT_STORAGE_VERSION,
  type CheckoutDraft,
  type CheckoutErrors,
  type CheckoutFieldName,
  type CheckoutState,
  type FulfilmentType,
  type ScheduledDateOption,
  type TimingType,
} from "@/lib/checkout/checkout-types";

const OPENING_DAYS = "Wednesday – Monday";
const OPENING_HOURS = "12:00 – 23:00";
const FIRST_ORDER_TIME = "12:00";
const LAST_ORDER_TIME = "22:45";

export const basilicoCollectionAddress = [
  "Basilico – Simple Italian",
  "41 Trinity Street",
  "Dorchester",
  "Dorset",
  "DT1 1TT",
];

export const checkoutGuidance = {
  delivery:
    "Delivery availability and any applicable delivery charge will be confirmed before payment.",
  hours: `${OPENING_DAYS}, ${OPENING_HOURS}. Tuesday closed.`,
  notes: "Please tell us about any allergies or dietary requirements.",
  time: "Requested times are subject to confirmation.",
};

export const initialCheckoutState: CheckoutState = {
  customer: {
    email: "",
    firstName: "",
    lastName: "",
    phone: "",
  },
  deliveryAddress: {
    city: "",
    line1: "",
    line2: "",
    postcode: "",
  },
  fulfilmentType: "",
  notes: "",
  timing: {
    requestedDate: "",
    requestedTime: "",
    type: "",
  },
};

export function validateCheckout(state: CheckoutState) {
  const errors: CheckoutErrors = {};

  if (!state.fulfilmentType) {
    errors.fulfilmentType = "Choose delivery or collection.";
  }

  if (!state.customer.firstName.trim()) {
    errors.firstName = "Enter your first name.";
  }

  if (!state.customer.lastName.trim()) {
    errors.lastName = "Enter your last name.";
  }

  if (!isValidPhoneNumber(state.customer.phone)) {
    errors.phone = "Enter a valid mobile number.";
  }

  if (!isValidEmail(state.customer.email)) {
    errors.email = "Enter a valid email address.";
  }

  if (state.fulfilmentType === "delivery") {
    if (!state.deliveryAddress.line1.trim()) {
      errors.addressLine1 = "Enter address line 1.";
    }

    if (!state.deliveryAddress.city.trim()) {
      errors.addressCity = "Enter your town or city.";
    }

    if (!state.deliveryAddress.postcode.trim()) {
      errors.addressPostcode = "Enter your postcode.";
    }
  }

  if (!state.timing.type) {
    errors.timingType = "Choose ASAP or a requested time.";
  }

  if (state.timing.type === "scheduled") {
    if (!state.timing.requestedDate) {
      errors.requestedDate = "Choose a day.";
    } else if (isTuesday(state.timing.requestedDate)) {
      errors.requestedDate = "Basilico is closed on Tuesdays.";
    }

    if (!state.timing.requestedTime) {
      errors.requestedTime = "Choose a time.";
    } else if (!isValidScheduledTime(state.timing.requestedTime)) {
      errors.requestedTime = "Choose a time between 12:00 and 22:45.";
    }
  }

  return errors;
}

export function getFirstCheckoutError(errors: CheckoutErrors) {
  const fieldOrder: CheckoutFieldName[] = [
    "fulfilmentType",
    "firstName",
    "lastName",
    "phone",
    "email",
    "addressLine1",
    "addressCity",
    "addressPostcode",
    "timingType",
    "requestedDate",
    "requestedTime",
  ];

  return fieldOrder.find((fieldName) => errors[fieldName]);
}

export function hasCheckoutErrors(errors: CheckoutErrors) {
  return Object.keys(errors).length > 0;
}

export function normalizeCheckoutState(state: CheckoutState): CheckoutState {
  return {
    customer: {
      email: state.customer.email.trim(),
      firstName: state.customer.firstName.trim(),
      lastName: state.customer.lastName.trim(),
      phone: state.customer.phone.trim(),
    },
    deliveryAddress: {
      city: state.deliveryAddress.city.trim(),
      line1: state.deliveryAddress.line1.trim(),
      line2: state.deliveryAddress.line2.trim(),
      postcode: normalizePostcode(state.deliveryAddress.postcode),
    },
    fulfilmentType: state.fulfilmentType,
    notes: state.notes.trim(),
    timing: {
      requestedDate: state.timing.requestedDate,
      requestedTime: state.timing.requestedTime,
      type: state.timing.type,
    },
  };
}

export function normalizePostcode(postcode: string) {
  return postcode.trim().replace(/\s+/g, " ").toUpperCase();
}

export function parseCheckoutDraft(value: string | null) {
  if (!value) {
    return initialCheckoutState;
  }

  try {
    const parsedValue: unknown = JSON.parse(value);

    if (!isRecord(parsedValue) || parsedValue.version !== CHECKOUT_STORAGE_VERSION) {
      return initialCheckoutState;
    }

    if (!isRecord(parsedValue.state)) {
      return initialCheckoutState;
    }

    return normalizeParsedState(parsedValue.state);
  } catch {
    return initialCheckoutState;
  }
}

export function serializeCheckoutDraft(state: CheckoutState): CheckoutDraft {
  return {
    state: normalizeCheckoutState(state),
    version: CHECKOUT_STORAGE_VERSION,
  };
}

export function getScheduledDateOptions(startDate = new Date(), daysToShow = 14) {
  const options: ScheduledDateOption[] = [];
  const date = new Date(
    startDate.getFullYear(),
    startDate.getMonth(),
    startDate.getDate(),
  );

  while (options.length < daysToShow) {
    if (date.getDay() !== 2) {
      options.push({
        label: new Intl.DateTimeFormat("en-GB", {
          day: "numeric",
          month: "short",
          weekday: "long",
        }).format(date),
        value: formatDateValue(date),
      });
    }

    date.setDate(date.getDate() + 1);
  }

  return options;
}

export function getScheduledTimeOptions() {
  const options: string[] = [];
  const start = timeToMinutes(FIRST_ORDER_TIME);
  const end = timeToMinutes(LAST_ORDER_TIME);

  for (let minutes = start; minutes <= end; minutes += 15) {
    options.push(minutesToTime(minutes));
  }

  return options;
}

export function getTimingLabel(state: CheckoutState) {
  if (state.timing.type === "asap") {
    return "AS SOON AS POSSIBLE";
  }

  if (state.timing.type === "scheduled") {
    const dateOption = getScheduledDateOptions().find(
      (option) => option.value === state.timing.requestedDate,
    );

    if (dateOption && state.timing.requestedTime) {
      return `${dateOption.label} at ${state.timing.requestedTime}`;
    }
  }

  return "Not selected";
}

function normalizeParsedState(value: Record<string, unknown>): CheckoutState {
  const customer = isRecord(value.customer) ? value.customer : {};
  const deliveryAddress = isRecord(value.deliveryAddress)
    ? value.deliveryAddress
    : {};
  const timing = isRecord(value.timing) ? value.timing : {};

  return normalizeCheckoutState({
    customer: {
      email: stringOrEmpty(customer.email),
      firstName: stringOrEmpty(customer.firstName),
      lastName: stringOrEmpty(customer.lastName),
      phone: stringOrEmpty(customer.phone),
    },
    deliveryAddress: {
      city: stringOrEmpty(deliveryAddress.city),
      line1: stringOrEmpty(deliveryAddress.line1),
      line2: stringOrEmpty(deliveryAddress.line2),
      postcode: stringOrEmpty(deliveryAddress.postcode),
    },
    fulfilmentType: isFulfilmentType(value.fulfilmentType)
      ? value.fulfilmentType
      : "",
    notes: stringOrEmpty(value.notes),
    timing: {
      requestedDate: stringOrEmpty(timing.requestedDate),
      requestedTime: stringOrEmpty(timing.requestedTime),
      type: isTimingType(timing.type) ? timing.type : "",
    },
  });
}

function isValidPhoneNumber(phone: string) {
  const normalizedPhone = phone.trim();
  const digitCount = normalizedPhone.replace(/\D/g, "").length;

  return (
    digitCount >= 10 &&
    digitCount <= 15 &&
    /^\+?[\d\s().-]+$/.test(normalizedPhone)
  );
}

function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

function isValidScheduledTime(time: string) {
  if (!/^\d{2}:\d{2}$/.test(time)) {
    return false;
  }

  const minutes = timeToMinutes(time);

  return (
    minutes >= timeToMinutes(FIRST_ORDER_TIME) &&
    minutes <= timeToMinutes(LAST_ORDER_TIME) &&
    minutes % 15 === 0
  );
}

function isTuesday(dateValue: string) {
  const [year, month, day] = dateValue.split("-").map(Number);

  if (!year || !month || !day) {
    return false;
  }

  return new Date(year, month - 1, day).getDay() === 2;
}

function formatDateValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function timeToMinutes(time: string) {
  const [hours, minutes] = time.split(":").map(Number);

  return hours * 60 + minutes;
}

function minutesToTime(totalMinutes: number) {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function stringOrEmpty(value: unknown) {
  return typeof value === "string" ? value : "";
}

function isFulfilmentType(value: unknown): value is FulfilmentType {
  return value === "collection" || value === "delivery";
}

function isTimingType(value: unknown): value is TimingType {
  return value === "asap" || value === "scheduled";
}
