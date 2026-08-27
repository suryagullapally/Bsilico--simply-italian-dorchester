export type BookingState = {
  customer: {
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
  };
  date: string;
  partySize: number;
  requests: string;
  time: string;
};

export type BookingFieldName =
  | "date"
  | "time"
  | "partySize"
  | "firstName"
  | "lastName"
  | "phone"
  | "email";

export type BookingErrors = Partial<Record<BookingFieldName, string>>;
