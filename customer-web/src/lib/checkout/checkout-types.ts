export const CHECKOUT_STORAGE_KEY = "basilico:checkout:v1";
export const CHECKOUT_STORAGE_VERSION = 1;

export type FulfilmentType = "collection" | "delivery";
export type TimingType = "asap" | "scheduled";

export type CheckoutCustomer = {
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
};

export type DeliveryAddress = {
  city: string;
  line1: string;
  line2: string;
  postcode: string;
};

export type CheckoutTiming = {
  requestedDate: string;
  requestedTime: string;
  type: TimingType | "";
};

export type CheckoutState = {
  customer: CheckoutCustomer;
  deliveryAddress: DeliveryAddress;
  fulfilmentType: FulfilmentType | "";
  notes: string;
  timing: CheckoutTiming;
};

export type CheckoutDraft = {
  state: CheckoutState;
  version: typeof CHECKOUT_STORAGE_VERSION;
};

export type CheckoutFieldName =
  | "fulfilmentType"
  | "firstName"
  | "lastName"
  | "phone"
  | "email"
  | "addressLine1"
  | "addressCity"
  | "addressPostcode"
  | "timingType"
  | "requestedDate"
  | "requestedTime";

export type CheckoutErrors = Partial<Record<CheckoutFieldName, string>>;

export type ScheduledDateOption = {
  label: string;
  value: string;
};
