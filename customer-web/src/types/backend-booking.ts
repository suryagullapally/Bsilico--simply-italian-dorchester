export type BackendBookingStatus =
  | "CANCELLED"
  | "COMPLETED"
  | "CONFIRMED"
  | "DECLINED"
  | "NO_SHOW"
  | "REQUESTED";

export type BackendCreateBookingRequest = {
  date: string;
  email: string;
  firstName: string;
  lastName: string;
  partySize: number;
  phone: string;
  specialRequests?: string;
  time: string;
};

export type BackendBookingResponse = {
  bookingReference: string;
  date: string;
  partySize: number;
  status: BackendBookingStatus;
  time: string;
};
