import type {
  BookingErrors,
  BookingFieldName,
  BookingState,
} from "@/lib/booking/booking-types";

const FIRST_BOOKING_TIME = "12:00";
const LAST_BOOKING_TIME = "22:45";

export const basilicoBookingDetails = {
  address: [
    "Basilico – Simple Italian",
    "41 Trinity Street",
    "Dorchester",
    "Dorset",
    "DT1 1TT",
  ],
  allergyMessage:
    "Please tell us about any allergies or dietary requirements.",
  hours: ["Wednesday – Monday", "12:00 – 23:00", "Tuesday", "Closed"],
  phone: "07424 642900",
  phoneHref: "tel:07424642900",
  timingNotice: "Requested booking times are subject to confirmation.",
};

export const initialBookingState: BookingState = {
  customer: {
    email: "",
    firstName: "",
    lastName: "",
    phone: "",
  },
  date: "",
  partySize: 2,
  requests: "",
  time: "",
};

export function validateBooking(state: BookingState) {
  const errors: BookingErrors = {};
  const dateError = getBookingDateError(state.date);

  if (dateError) {
    errors.date = dateError;
  }

  if (!state.time) {
    errors.time = "Choose a booking time.";
  } else if (!isValidBookingTime(state.time)) {
    errors.time = "Choose a time between 12:00 and 22:45.";
  }

  if (!Number.isInteger(state.partySize) || state.partySize < 1) {
    errors.partySize = "Enter at least 1 guest.";
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

  return errors;
}

export function getFirstBookingError(errors: BookingErrors) {
  const fieldOrder: BookingFieldName[] = [
    "date",
    "time",
    "partySize",
    "firstName",
    "lastName",
    "phone",
    "email",
  ];

  return fieldOrder.find((fieldName) => errors[fieldName]);
}

export function hasBookingErrors(errors: BookingErrors) {
  return Object.keys(errors).length > 0;
}

export function normalizeBookingState(state: BookingState): BookingState {
  return {
    customer: {
      email: state.customer.email.trim(),
      firstName: state.customer.firstName.trim(),
      lastName: state.customer.lastName.trim(),
      phone: state.customer.phone.trim(),
    },
    date: state.date,
    partySize: Math.max(1, Math.floor(state.partySize)),
    requests: state.requests.trim(),
    time: state.time,
  };
}

export function getBookingDateError(dateValue: string) {
  const today = getTodayDateValue();

  if (!dateValue) {
    return "Choose a booking date.";
  }

  if (dateValue < today) {
    return "Choose today or a future date.";
  }

  if (isTuesday(dateValue)) {
    return "Basilico is closed on Tuesdays. Please choose another day.";
  }

  return "";
}

export function getBookingTimeOptions() {
  const options: string[] = [];
  const start = timeToMinutes(FIRST_BOOKING_TIME);
  const end = timeToMinutes(LAST_BOOKING_TIME);

  for (let minutes = start; minutes <= end; minutes += 15) {
    options.push(minutesToTime(minutes));
  }

  return options;
}

export function getTodayDateValue(date = new Date()) {
  return formatDateValue(
    new Date(date.getFullYear(), date.getMonth(), date.getDate()),
  );
}

export function formatBookingDateLabel(dateValue: string) {
  const date = parseDateValue(dateValue);

  if (!date) {
    return "Choose a date";
  }

  return new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "long",
    weekday: "long",
  }).format(date);
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

function isTuesday(dateValue: string) {
  const date = parseDateValue(dateValue);

  return date?.getDay() === 2;
}

function isValidBookingTime(time: string) {
  if (!/^\d{2}:\d{2}$/.test(time)) {
    return false;
  }

  const minutes = timeToMinutes(time);

  return (
    minutes >= timeToMinutes(FIRST_BOOKING_TIME) &&
    minutes <= timeToMinutes(LAST_BOOKING_TIME) &&
    minutes % 15 === 0
  );
}

function parseDateValue(dateValue: string) {
  const [year, month, day] = dateValue.split("-").map(Number);

  if (!year || !month || !day) {
    return null;
  }

  return new Date(year, month - 1, day);
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
