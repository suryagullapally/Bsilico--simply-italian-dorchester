"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type MutableRefObject,
} from "react";
import {
  getDeliveryQuote,
  getFulfilmentOptions,
} from "@/lib/api/fulfilment-api";
import { getApiErrorMessage } from "@/lib/api/api-error";
import { formatGbpPennies } from "@/lib/format-price";
import type {
  BackendDeliveryQuoteResponse,
  BackendFulfilmentOptionsResponse,
} from "@/types/backend-fulfilment";

const QUOTE_STORAGE_KEY = "basilico:delivery-quote:v1";
const QUOTE_TTL_MS = 15 * 60 * 1000;

type QuoteState =
  | { status: "idle" }
  | { status: "loading" }
  | { message: string; status: "error" }
  | {
      purpose: QuotePurpose;
      quote: BackendDeliveryQuoteResponse;
      status: "quoted";
    };

type QuotePurpose = "current-location" | "postcode";

type StoredQuote = {
  createdAt: number;
  purpose: QuotePurpose;
  quote: BackendDeliveryQuoteResponse;
};

type LocationPermissionState =
  | "denied"
  | "granted"
  | "prompt"
  | "unknown"
  | "unsupported";

export function MenuDeliveryStatus() {
  const [options, setOptions] = useState<BackendFulfilmentOptionsResponse | null>(
    null,
  );
  const [postcode, setPostcode] = useState("");
  const [quoteState, setQuoteState] = useState<QuoteState>({ status: "idle" });
  const [locationPermission, setLocationPermission] =
    useState<LocationPermissionState>("unknown");
  const quoteRequestIdRef = useRef(0);
  const autoLocationRequestedRef = useRef(false);
  const deliveryCopy = useMemo(() => getDeliveryCopy(options), [options]);
  const locationAccessBlocked = locationPermission === "denied";

  const quoteLocation = useCallback(
    async (latitude: number, longitude: number, requestId: number) => {
      try {
        const quote = await getDeliveryQuote({ latitude, longitude });
        if (requestId !== quoteRequestIdRef.current) {
          return;
        }
        setStoredQuote(quote, "current-location");
        setQuoteState({ purpose: "current-location", quote, status: "quoted" });
      } catch (error) {
        if (requestId !== quoteRequestIdRef.current) {
          return;
        }
        setQuoteState({
          message: getApiErrorMessage(
            error,
            "We could not check delivery for this location.",
          ),
          status: "error",
        });
      }
    },
    [],
  );

  const requestLocationQuote = useCallback(() => {
    if (!navigator.geolocation) {
      setLocationPermission("unsupported");
      setQuoteState({
        message: "Your browser cannot share location. Enter a postcode instead.",
        status: "error",
      });
      return;
    }

    const requestId = nextQuoteRequestId(quoteRequestIdRef);
    setQuoteState({ status: "loading" });
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocationPermission("granted");
        void quoteLocation(
          position.coords.latitude,
          position.coords.longitude,
          requestId,
        );
      },
      (error) => {
        if (requestId !== quoteRequestIdRef.current) {
          return;
        }
        if (error.code === error.PERMISSION_DENIED) {
          setLocationPermission("denied");
        }
        setQuoteState({ message: getGeolocationErrorMessage(error), status: "error" });
      },
      {
        enableHighAccuracy: false,
        maximumAge: 10 * 60 * 1000,
        timeout: 7000,
      },
    );
  }, [quoteLocation]);

  useEffect(() => {
    let mounted = true;

    getFulfilmentOptions()
      .then((response) => {
        if (!mounted) {
          return;
        }

        setOptions(response);
        const storedQuote = readStoredQuote();
        if (storedQuote) {
          setQuoteState({
            purpose: storedQuote.purpose,
            quote: storedQuote.quote,
            status: "quoted",
          });
        }
      })
      .catch((error) => {
        if (mounted) {
          setQuoteState({
            message: getApiErrorMessage(
              error,
              "We could not check delivery options right now.",
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
    if (!options?.deliveryEnabled || quoteState.status !== "idle") {
      return;
    }

    if (!navigator.geolocation) {
      return;
    }

    if (!("permissions" in navigator)) {
      return;
    }

    let active = true;
    let permissionStatus: PermissionStatus | null = null;

    const handlePermissionChange = () => {
      if (!active || !permissionStatus) {
        return;
      }

      const nextPermission = toLocationPermissionState(permissionStatus.state);
      setLocationPermission(nextPermission);

      if (nextPermission === "granted" && !autoLocationRequestedRef.current) {
        autoLocationRequestedRef.current = true;
        requestLocationQuote();
      }
    };

    void navigator.permissions
      .query({ name: "geolocation" as PermissionName })
      .then((permission) => {
        if (!active) {
          return;
        }
        permissionStatus = permission;
        handlePermissionChange();
        permission.addEventListener("change", handlePermissionChange);
      })
      .catch(() => {
        if (active) {
          // Browsers without reliable Permissions API support can still call geolocation directly.
          setLocationPermission("unknown");
        }
      });

    return () => {
      active = false;
      permissionStatus?.removeEventListener("change", handlePermissionChange);
    };
  }, [options?.deliveryEnabled, quoteState.status, requestLocationQuote]);

  async function handlePostcodeSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPostcode = postcode.trim().toUpperCase();
    if (!normalizedPostcode) {
      setQuoteState({ message: "Enter a postcode to check delivery.", status: "error" });
      return;
    }

    setQuoteState({ status: "loading" });
    const requestId = nextQuoteRequestId(quoteRequestIdRef);

    try {
      const quote = await getDeliveryQuote({ postcode: normalizedPostcode });
      if (requestId !== quoteRequestIdRef.current) {
        return;
      }
      setPostcode(quote.normalizedPostcode ?? normalizedPostcode);
      setStoredQuote(quote, "postcode");
      setQuoteState({ purpose: "postcode", quote, status: "quoted" });
    } catch (error) {
      if (requestId !== quoteRequestIdRef.current) {
        return;
      }
      setQuoteState({
        message: getApiErrorMessage(
          error,
          "We could not check delivery for this postcode.",
        ),
        status: "error",
      });
    }
  }

  function handlePostcodeChange(value: string) {
    setPostcode(value.toUpperCase());
    nextQuoteRequestId(quoteRequestIdRef);
    clearStoredQuote();

    if (quoteState.status !== "idle") {
      setQuoteState({ status: "idle" });
    }
  }

  function resetQuote() {
    nextQuoteRequestId(quoteRequestIdRef);
    clearStoredQuote();
    setPostcode("");
    setQuoteState({ status: "idle" });
  }

  return (
    <section className="menu-delivery-status" aria-labelledby="menu-delivery-title">
      <div className="menu-delivery-status__content">
        <div>
          <p className="type-eyebrow menu-delivery-status__eyebrow">
            Delivery
          </p>
          <h2 className="type-h3 menu-delivery-status__title" id="menu-delivery-title">
            {deliveryCopy.title}
          </h2>
          <p className="type-small menu-delivery-status__copy">
            {deliveryCopy.copy}
          </p>
        </div>

        {options?.deliveryEnabled ? (
          <div className="menu-delivery-status__actions">
            <form className="menu-delivery-status__postcode" onSubmit={handlePostcodeSubmit}>
              <label htmlFor="menu-delivery-postcode">Enter delivery postcode</label>
              <div>
                <input
                  id="menu-delivery-postcode"
                  inputMode="text"
                  onChange={(event) => handlePostcodeChange(event.target.value)}
                  placeholder="DT1 1TT"
                  value={postcode}
                />
                <button disabled={quoteState.status === "loading"} type="submit">
                  CHECK DELIVERY POSTCODE
                </button>
              </div>
            </form>
            <button
              className="menu-delivery-status__button menu-delivery-status__button--secondary"
              disabled={quoteState.status === "loading"}
              onClick={requestLocationQuote}
              type="button"
            >
              USE MY CURRENT LOCATION
            </button>
            {locationAccessBlocked ? (
              <p className="type-small menu-delivery-status__muted">
                Location access is blocked in your browser. You can enable it in site settings or enter a delivery postcode instead.
              </p>
            ) : null}
            {quoteState.status === "quoted" ? (
              <QuoteResult
                onReset={resetQuote}
                purpose={quoteState.purpose}
                quote={quoteState.quote}
              />
            ) : null}
            <StatusMessage state={quoteState} />
          </div>
        ) : (
          <p className="type-small menu-delivery-status__muted">
            Delivery is currently unavailable. Collection is still available.
          </p>
        )}
      </div>
    </section>
  );
}

function QuoteResult({
  onReset,
  purpose,
  quote,
}: {
  onReset: () => void;
  purpose: QuotePurpose;
  quote: BackendDeliveryQuoteResponse;
}) {
  if (!quote.eligible) {
    const isCurrentLocation = purpose === "current-location";

    return (
      <div className="menu-delivery-status__result menu-delivery-status__result--unavailable">
        <strong>{isCurrentLocation ? "Current location" : "Delivery unavailable"}</strong>
        <span>
          {isCurrentLocation
            ? "We do not deliver to your current location."
            : quote.message ?? "Outside our delivery area."}
        </span>
        {isCurrentLocation ? <em>Ordering to another address?</em> : null}
        <button onClick={onReset} type="button">
          {isCurrentLocation ? "Check delivery postcode" : "Check another postcode"}
        </button>
      </div>
    );
  }

  const isPostcode = purpose === "postcode";

  return (
    <div className="menu-delivery-status__result">
      <strong>
        {isPostcode ? "✓ We deliver to this address" : "✓ Delivery available near you"}
      </strong>
      <span>{formatQuoteDetails(quote)}</span>
      {!isPostcode ? (
        <em>Your checkout delivery postcode will still be confirmed before payment.</em>
      ) : null}
      <button onClick={onReset} type="button">
        {isPostcode ? "Check another postcode" : "Enter delivery postcode"}
      </button>
    </div>
  );
}

function StatusMessage({ state }: { state: QuoteState }) {
  if (state.status === "loading") {
    return (
      <p className="type-small menu-delivery-status__muted" role="status">
        Checking delivery.
      </p>
    );
  }

  if (state.status === "error") {
    return (
      <p className="type-small menu-delivery-status__error" role="alert">
        {state.message}
      </p>
    );
  }

  return null;
}

function getGeolocationErrorMessage(error: GeolocationPositionError) {
  if (error.code === error.PERMISSION_DENIED) {
    return "Location access is blocked in your browser. You can enable it in site settings or enter a delivery postcode instead.";
  }

  if (error.code === error.POSITION_UNAVAILABLE) {
    return "We couldn't determine your current location. Try entering your delivery postcode instead.";
  }

  if (error.code === error.TIMEOUT) {
    return "Location check took too long. Please try again or enter a postcode.";
  }

  return "We couldn't check your current location. Try entering your delivery postcode instead.";
}

function formatQuoteDetails(quote: BackendDeliveryQuoteResponse) {
  const details: string[] = [];
  if (quote.distanceMiles !== null) {
    details.push(`${quote.distanceMiles.toFixed(1)} miles away`);
  }
  if (quote.deliveryFeePence !== null) {
    details.push(`${formatGbpPennies(quote.deliveryFeePence)} delivery`);
  }
  if (quote.estimatedDeliveryMinutes !== null) {
    details.push(`Approx. ${quote.estimatedDeliveryMinutes} mins`);
  }

  return details.join(" · ");
}

function getDeliveryCopy(options: BackendFulfilmentOptionsResponse | null) {
  const radius = options?.deliveryRadiusMiles ?? 6;

  if (!options) {
    return {
      copy: "Checking Basilico delivery options.",
      title: "Delivery from Basilico.",
    };
  }

  if (!options.deliveryEnabled) {
    return {
      copy: "Collection remains available from Trinity Street.",
      title: "Delivery is currently unavailable.",
    };
  }

  return {
    copy:
      "Enter the postcode you want us to deliver to. Ordering for someone else? Check their delivery postcode before checkout.",
    title: `Delivery within ${radius.toFixed(0)} miles of Basilico.`,
  };
}

function readStoredQuote(): StoredQuote | null {
  try {
    const raw = window.sessionStorage.getItem(QUOTE_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as StoredQuote;
    if (
      !parsed.quote ||
      !isQuotePurpose(parsed.purpose) ||
      Date.now() - parsed.createdAt > QUOTE_TTL_MS
    ) {
      clearStoredQuote();
      return null;
    }

    return parsed;
  } catch {
    clearStoredQuote();
    return null;
  }
}

function setStoredQuote(
  quote: BackendDeliveryQuoteResponse,
  purpose: QuotePurpose,
) {
  try {
    const value: StoredQuote = { createdAt: Date.now(), purpose, quote };
    window.sessionStorage.setItem(QUOTE_STORAGE_KEY, JSON.stringify(value));
  } catch {
    // The quote is a convenience preview; checkout will revalidate.
  }
}

function clearStoredQuote() {
  try {
    window.sessionStorage.removeItem(QUOTE_STORAGE_KEY);
  } catch {
    // Ignore storage failures; the in-memory UI state still works.
  }
}

function isQuotePurpose(value: unknown): value is QuotePurpose {
  return value === "current-location" || value === "postcode";
}

function toLocationPermissionState(
  state: PermissionState,
): Exclude<LocationPermissionState, "unsupported"> {
  if (state === "denied" || state === "granted" || state === "prompt") {
    return state;
  }

  return "unknown";
}

function nextQuoteRequestId(ref: MutableRefObject<number>) {
  ref.current += 1;
  return ref.current;
}
