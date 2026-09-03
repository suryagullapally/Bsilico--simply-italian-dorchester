export type BackendFulfilmentOptionsResponse = {
  baseDeliveryFeePence: number | null;
  baseDeliveryRadiusMiles: number | null;
  collectionEnabled: boolean;
  deliveryAreaMode: "POSTCODE_RULES" | "RADIUS";
  deliveryEnabled: boolean;
  deliveryFeePence: number | null;
  deliveryPricingMode: "FLAT_FEE" | "RADIUS_BANDS";
  deliveryRadiusMiles: number | null;
  extraMileFeePence: number | null;
  freeDeliveryThresholdPence: number | null;
  minimumDeliveryOrderPence: number | null;
  orderAvailability: BackendOrderAvailabilityResponse;
  preparationTimeMinutes: number | null;
  restaurantPostcode: string | null;
};

export type BackendOrderAvailabilityResponse = {
  asapAvailable: boolean;
  nextAvailableAt: string | null;
  nextAvailableDate: string | null;
  nextAvailableTime: string | null;
  restaurantOpenNow: boolean;
  restaurantTimezone: "Europe/London";
  statusMessage: string | null;
  validOrderDates: BackendOrderAvailabilityDateResponse[];
};

export type BackendOrderAvailabilityDateResponse = {
  date: string;
  label: string;
  timeSlots: string[];
};

export type BackendCheckDeliveryRequest = {
  postcode: string;
};

export type BackendDeliveryEligibilityResponse = {
  eligible: boolean;
  message: string | null;
  normalizedPostcode: string;
};

export type BackendDeliveryQuoteRequest =
  | {
      latitude?: never;
      longitude?: never;
      postcode: string;
    }
  | {
      latitude: number;
      longitude: number;
      postcode?: never;
    };

export type BackendDeliveryQuoteResponse = {
  deliveryFeePence: number | null;
  distanceMiles: number | null;
  eligible: boolean;
  estimatedDeliveryMinutes: number | null;
  message: string | null;
  normalizedPostcode: string | null;
  preparationMinutes: number | null;
  source: "POSTCODE" | "GEOLOCATION";
  travelMinutes: number | null;
};
