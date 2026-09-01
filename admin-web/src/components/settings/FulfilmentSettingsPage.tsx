"use client";

import { FormEvent, useState } from "react";
import type { FulfilmentAdminData } from "@/components/routes/FulfilmentSettingsRouteClient";
import { PageHeader } from "@/components/ui/PageHeader";
import { StatusBadge } from "@/components/ui/StatusBadge";
import {
  createAdminDeliveryPostcodeRule,
  updateAdminDeliveryPostcodeRuleActive,
  updateAdminFulfilmentSettings,
} from "@/lib/api/admin-fulfilment-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import {
  formatGbpPennies,
  formatPenniesForInput,
  parseGbpToPennies,
} from "@/lib/format-price";
import type {
  AdminFulfilmentSettingsResponse,
  DeliveryPostcodeRuleResponse,
} from "@/types/admin";

type FulfilmentSettingsPageProps = {
  data: FulfilmentAdminData;
  onRefresh: () => Promise<void>;
};

type SettingsFormState = {
  baseDeliveryFee: string;
  baseDeliveryRadiusMiles: string;
  collectionEnabled: boolean;
  deliveryAreaMode: "POSTCODE_RULES" | "RADIUS";
  deliveryEnabled: boolean;
  deliveryFee: string;
  deliveryPricingMode: "FLAT_FEE" | "RADIUS_BANDS";
  deliveryRadiusMiles: string;
  extraMileFee: string;
  freeDeliveryThreshold: string;
  minimumDeliveryOrder: string;
  preparationTimeMinutes: string;
  restaurantLatitude: string;
  restaurantLongitude: string;
  restaurantPostcode: string;
};

type ParsedOptionalMoney = {
  error?: string;
  value: number | null;
};

type ParsedOptionalNumber = {
  error?: string;
  value: number | null;
};

export function FulfilmentSettingsPage({
  data,
  onRefresh,
}: FulfilmentSettingsPageProps) {
  const [settings, setSettings] = useState(data.settings);
  const [rules, setRules] = useState(data.rules);
  const [formState, setFormState] = useState(() =>
    formStateFromSettings(data.settings),
  );
  const [newRule, setNewRule] = useState("");
  const [saving, setSaving] = useState(false);
  const [rulePendingId, setRulePendingId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const activeRuleCount = rules.filter((rule) => rule.active).length;
  const radiusMode = formState.deliveryAreaMode === "RADIUS";

  async function handleSettingsSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback("");
    setError("");

    const minimumDeliveryOrderPence = parseOptionalMoney(
      formState.minimumDeliveryOrder,
      "Minimum delivery order",
    );
    const deliveryFeePence = parseOptionalMoney(
      formState.deliveryFee,
      "Delivery charge",
    );
    const freeDeliveryThresholdPence = parseOptionalMoney(
      formState.freeDeliveryThreshold,
      "Free delivery threshold",
    );
    const baseDeliveryFeePence = parseOptionalMoney(
      formState.baseDeliveryFee,
      "First-band delivery fee",
    );
    const extraMileFeePence = parseOptionalMoney(
      formState.extraMileFee,
      "Started extra-mile fee",
    );
    const restaurantLatitude = parseOptionalDecimal(
      formState.restaurantLatitude,
      "Restaurant latitude",
      { max: 90, min: -90 },
    );
    const restaurantLongitude = parseOptionalDecimal(
      formState.restaurantLongitude,
      "Restaurant longitude",
      { max: 180, min: -180 },
    );
    const deliveryRadiusMiles = parseOptionalDecimal(
      formState.deliveryRadiusMiles,
      "Delivery radius",
    );
    const baseDeliveryRadiusMiles = parseOptionalDecimal(
      formState.baseDeliveryRadiusMiles,
      "First-band radius",
    );
    const preparationTimeMinutes = parseOptionalInteger(
      formState.preparationTimeMinutes,
      "Preparation time",
    );

    const parseError =
      minimumDeliveryOrderPence.error ??
      deliveryFeePence.error ??
      freeDeliveryThresholdPence.error ??
      baseDeliveryFeePence.error ??
      extraMileFeePence.error ??
      restaurantLatitude.error ??
      restaurantLongitude.error ??
      deliveryRadiusMiles.error ??
      baseDeliveryRadiusMiles.error ??
      preparationTimeMinutes.error;

    if (parseError) {
      setError(parseError);
      return;
    }

    if (formState.deliveryEnabled) {
      if (formState.deliveryAreaMode === "POSTCODE_RULES") {
        if (deliveryFeePence.value === null) {
          setError("Set a delivery charge before enabling postcode-rule delivery.");
          return;
        }

        if (activeRuleCount === 0) {
          setError("Add at least one active postcode rule before enabling postcode-rule delivery.");
          return;
        }
      }

      if (formState.deliveryAreaMode === "RADIUS") {
        if (
          !formState.restaurantPostcode.trim() ||
          restaurantLatitude.value === null ||
          restaurantLongitude.value === null ||
          deliveryRadiusMiles.value === null ||
          preparationTimeMinutes.value === null
        ) {
          setError("Set restaurant postcode, coordinates, radius and preparation time before enabling radius delivery.");
          return;
        }

        if (
          formState.deliveryPricingMode === "RADIUS_BANDS" &&
          (baseDeliveryRadiusMiles.value === null ||
            baseDeliveryFeePence.value === null ||
            extraMileFeePence.value === null)
        ) {
          setError("Set first-band radius, first-band fee and started extra-mile fee before enabling radius-band pricing.");
          return;
        }

        if (
          formState.deliveryPricingMode === "FLAT_FEE" &&
          deliveryFeePence.value === null
        ) {
          setError("Set a delivery charge before enabling flat-fee radius delivery.");
          return;
        }
      }
    }

    setSaving(true);

    try {
      const updated = await updateAdminFulfilmentSettings({
        baseDeliveryFeePence: baseDeliveryFeePence.value,
        baseDeliveryRadiusMiles: baseDeliveryRadiusMiles.value,
        collectionEnabled: formState.collectionEnabled,
        deliveryAreaMode: formState.deliveryAreaMode,
        deliveryEnabled: formState.deliveryEnabled,
        deliveryFeePence: deliveryFeePence.value,
        deliveryPricingMode: formState.deliveryPricingMode,
        deliveryRadiusMiles: deliveryRadiusMiles.value,
        extraMileFeePence: extraMileFeePence.value,
        freeDeliveryThresholdPence: freeDeliveryThresholdPence.value,
        minimumDeliveryOrderPence: minimumDeliveryOrderPence.value,
        preparationTimeMinutes: preparationTimeMinutes.value,
        restaurantLatitude: restaurantLatitude.value,
        restaurantLongitude: restaurantLongitude.value,
        restaurantPostcode: formState.restaurantPostcode.trim() || null,
      });
      setSettings(updated);
      setFormState(formStateFromSettings(updated));
      setFeedback("Fulfilment settings saved.");
      await onRefresh();
    } catch (caught) {
      setError(
        getAdminApiErrorMessage(caught, "Could not save fulfilment settings."),
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleAddRule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const postcodePattern = newRule.trim();
    setFeedback("");
    setError("");

    if (radiusMode) {
      setError("Postcode-prefix rules are inactive while Radius mode is selected.");
      return;
    }

    if (!postcodePattern) {
      setError("Enter a postcode prefix such as DT1 or DT1 1.");
      return;
    }

    setSaving(true);

    try {
      const created = await createAdminDeliveryPostcodeRule({
        displayOrder: nextRuleDisplayOrder(rules),
        postcodePattern,
      });
      setRules(sortRules([...rules, created]));
      setNewRule("");
      setFeedback(`${created.postcodePattern} added to delivery areas.`);
      await onRefresh();
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not add postcode rule."));
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleRule(rule: DeliveryPostcodeRuleResponse) {
    setRulePendingId(rule.id);
    setFeedback("");
    setError("");

    try {
      const updated = await updateAdminDeliveryPostcodeRuleActive(
        rule.id,
        !rule.active,
      );
      setRules(sortRules(rules.map((item) => (item.id === rule.id ? updated : item))));
      setFeedback(
        `${updated.postcodePattern} marked ${updated.active ? "active" : "inactive"}.`,
      );
      await onRefresh();
    } catch (caught) {
      setError(
        getAdminApiErrorMessage(caught, "Could not update postcode rule."),
      );
    } finally {
      setRulePendingId(null);
    }
  }

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Settings" title="Fulfilment.">
        <p>
          Control collection, radius delivery and pricing. Customer checkout is
          still revalidated by the backend before payment.
        </p>
      </PageHeader>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(20rem,0.45fr)]">
        <form className="panel" onSubmit={handleSettingsSubmit}>
          <div className="panel__body grid gap-5">
            <div>
              <p className="eyebrow">Ordering modes</p>
              <div className="checkbox-grid mt-3">
                <label className="checkbox-chip">
                  <input
                    checked={formState.collectionEnabled}
                    type="checkbox"
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        collectionEnabled: event.target.checked,
                      }))
                    }
                  />
                  <span>Collection enabled</span>
                </label>
                <label className="checkbox-chip">
                  <input
                    checked={formState.deliveryEnabled}
                    type="checkbox"
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        deliveryEnabled: event.target.checked,
                      }))
                    }
                  />
                  <span>Delivery enabled</span>
                </label>
              </div>
              {formState.deliveryEnabled &&
              formState.deliveryAreaMode === "POSTCODE_RULES" &&
              activeRuleCount === 0 ? (
                <p className="error mt-3">
                  Add at least one active postcode rule before delivery can be
                  enabled.
                </p>
              ) : null}
            </div>

            <div className="form-grid form-grid--two">
              <div className="field">
                <label htmlFor="deliveryAreaMode">Service area</label>
                <select
                  id="deliveryAreaMode"
                  value={formState.deliveryAreaMode}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      deliveryAreaMode: event.target.value as SettingsFormState["deliveryAreaMode"],
                    }))
                  }
                >
                  <option value="RADIUS">Radius</option>
                  <option value="POSTCODE_RULES">Postcode rules</option>
                </select>
                <p className="text-sm text-muted">
                  Radius mode uses straight-line distance from Basilico.
                </p>
              </div>
              <div className="field">
                <label htmlFor="deliveryPricingMode">Pricing mode</label>
                <select
                  id="deliveryPricingMode"
                  value={formState.deliveryPricingMode}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      deliveryPricingMode: event.target.value as SettingsFormState["deliveryPricingMode"],
                    }))
                  }
                >
                  <option value="RADIUS_BANDS">Radius bands</option>
                  <option value="FLAT_FEE">Flat fee</option>
                </select>
                <p className="text-sm text-muted">
                  Radius bands ignore legacy free-delivery thresholds.
                </p>
              </div>
              <div className="field">
                <label htmlFor="restaurantPostcode">Restaurant postcode</label>
                <input
                  id="restaurantPostcode"
                  value={formState.restaurantPostcode}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      restaurantPostcode: event.target.value.toUpperCase(),
                    }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="deliveryRadiusMiles">Delivery radius miles</label>
                <input
                  id="deliveryRadiusMiles"
                  inputMode="decimal"
                  value={formState.deliveryRadiusMiles}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      deliveryRadiusMiles: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="restaurantLatitude">Restaurant latitude</label>
                <input
                  id="restaurantLatitude"
                  inputMode="decimal"
                  value={formState.restaurantLatitude}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      restaurantLatitude: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="restaurantLongitude">Restaurant longitude</label>
                <input
                  id="restaurantLongitude"
                  inputMode="decimal"
                  value={formState.restaurantLongitude}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      restaurantLongitude: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="preparationTimeMinutes">Preparation time minutes</label>
                <input
                  id="preparationTimeMinutes"
                  inputMode="numeric"
                  value={formState.preparationTimeMinutes}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      preparationTimeMinutes: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="baseDeliveryRadiusMiles">First-band miles</label>
                <input
                  id="baseDeliveryRadiusMiles"
                  inputMode="decimal"
                  value={formState.baseDeliveryRadiusMiles}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      baseDeliveryRadiusMiles: event.target.value,
                    }))
                  }
                />
                <p className="text-sm text-muted">0-3 miles is currently £2.</p>
              </div>
              <div className="field">
                <label htmlFor="baseDeliveryFee">First-band fee</label>
                <input
                  id="baseDeliveryFee"
                  inputMode="decimal"
                  value={formState.baseDeliveryFee}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      baseDeliveryFee: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="extraMileFee">Started extra-mile fee</label>
                <input
                  id="extraMileFee"
                  inputMode="decimal"
                  value={formState.extraMileFee}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      extraMileFee: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="minimumDeliveryOrder">
                  Minimum delivery order
                </label>
                <input
                  id="minimumDeliveryOrder"
                  inputMode="decimal"
                  placeholder="Leave blank"
                  value={formState.minimumDeliveryOrder}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      minimumDeliveryOrder: event.target.value,
                    }))
                  }
                />
                <p className="text-sm text-muted">
                  Applies to food subtotal before delivery charge if configured.
                </p>
              </div>
              <div className="field">
                <label htmlFor="deliveryFee">Legacy flat delivery charge</label>
                <input
                  id="deliveryFee"
                  inputMode="decimal"
                  placeholder="Leave blank"
                  value={formState.deliveryFee}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      deliveryFee: event.target.value,
                    }))
                  }
                />
                <p className="text-sm text-muted">
                  Used only when pricing mode is Flat fee.
                </p>
              </div>
              <div className="field sm:col-span-2">
                <label htmlFor="freeDeliveryThreshold">Legacy free delivery over</label>
                <input
                  id="freeDeliveryThreshold"
                  inputMode="decimal"
                  placeholder="Leave blank"
                  value={formState.freeDeliveryThreshold}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      freeDeliveryThreshold: event.target.value,
                    }))
                  }
                />
                <p className="text-sm text-muted">
                  Preserved for legacy pricing; inactive in Radius bands mode.
                </p>
              </div>
            </div>

            <div className="action-row">
              <button className="button" disabled={saving} type="submit">
                {saving ? "Saving..." : "Save fulfilment settings"}
              </button>
            </div>
            <div aria-live="polite">
              {feedback ? <p className="feedback">{feedback}</p> : null}
              {error ? <p className="error">{error}</p> : null}
            </div>
          </div>
        </form>

        <aside className="panel panel--cream">
          <div className="panel__body">
            <p className="eyebrow">Current state</p>
            <dl className="detail-list mt-4">
              <Detail
                label="Collection"
                value={settings.collectionEnabled ? "Enabled" : "Disabled"}
              />
              <Detail
                label="Delivery"
                value={settings.deliveryEnabled ? "Enabled" : "Disabled"}
              />
              <Detail
                label="Service area"
                value={settings.deliveryAreaMode === "RADIUS" ? "Radius" : "Postcode rules"}
              />
              <Detail
                label="Delivery radius"
                value={
                  settings.deliveryRadiusMiles === null
                    ? "Not configured"
                    : `${settings.deliveryRadiusMiles.toFixed(1)} miles`
                }
              />
              <Detail
                label="Restaurant postcode"
                value={settings.restaurantPostcode ?? "Not configured"}
              />
              <Detail
                label="Preparation"
                value={
                  settings.preparationTimeMinutes === null
                    ? "Not configured"
                    : `${settings.preparationTimeMinutes} minutes`
                }
              />
              <Detail
                label="Pricing"
                value={settings.deliveryPricingMode === "RADIUS_BANDS" ? "Radius bands" : "Flat fee"}
              />
              <Detail
                label="First band"
                value={`${formatMiles(settings.baseDeliveryRadiusMiles)} / ${formatOptionalPence(settings.baseDeliveryFeePence)}`}
              />
              <Detail
                label="Started extra mile"
                value={formatOptionalPence(settings.extraMileFeePence)}
              />
              <Detail
                label="Minimum delivery order"
                value={formatOptionalPence(settings.minimumDeliveryOrderPence)}
              />
              <Detail
                label="Legacy flat delivery"
                value={formatOptionalPence(settings.deliveryFeePence)}
              />
              <Detail
                label="Legacy free delivery over"
                value={formatOptionalPence(settings.freeDeliveryThresholdPence)}
              />
              <Detail label="Active postcode rules" value={String(activeRuleCount)} />
            </dl>
            {settings.deliveryPricingMode === "RADIUS_BANDS" ? (
              <div className="mt-4 rounded-md border border-[var(--border-subtle)] p-3 text-sm text-muted">
                <p className="font-black text-foreground">Radius-band pricing</p>
                <p className="mt-2">0-3 mi £2 · &gt;3-4 mi £3 · &gt;4-5 mi £4 · &gt;5-6 mi £5</p>
              </div>
            ) : null}
            {!settings.drivingTimeConfigured ? (
              <p className="error mt-4">
                Driving-time estimates are not configured. Delivery checks and
                fees still work from the 6-mile radius.
              </p>
            ) : null}
          </div>
        </aside>
      </div>

      <section className="panel">
        <div className="panel__body grid gap-5">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <p className="eyebrow">Delivery areas</p>
              <h2 className="section-title mt-1">Postcode prefixes.</h2>
              <p className="mt-2 text-sm text-muted">
                {radiusMode
                  ? "Radius delivery is active, so these legacy postcode prefixes are preserved but not used for customer checks."
                  : "Use simple UK outward-code prefixes such as DT1 or DT1 1. Only active rules are used for customer delivery checks."}
              </p>
            </div>
            {radiusMode ? (
              <StatusBadge value="INACTIVE IN RADIUS MODE" />
            ) : null}
          </div>

          <form className="form-grid form-grid--two" onSubmit={handleAddRule}>
            <div className="field">
              <label htmlFor="postcodePattern">Add postcode prefix</label>
              <input
                id="postcodePattern"
                placeholder="DT1"
                value={newRule}
                onChange={(event) => setNewRule(event.target.value)}
              />
            </div>
            <div className="flex items-end">
              <button
                className="button-secondary w-full"
                disabled={saving || radiusMode}
                type="submit"
              >
                Add rule
              </button>
            </div>
          </form>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Prefix</th>
                  <th>Status</th>
                  <th>Display order</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => (
                  <tr key={rule.id}>
                    <td className="font-black">{rule.postcodePattern}</td>
                    <td>
                      <StatusBadge value={rule.active ? "ACTIVE" : "INACTIVE"} />
                    </td>
                    <td>{rule.displayOrder}</td>
                    <td>
                      <button
                        className="button-ghost"
                        disabled={rulePendingId === rule.id}
                        type="button"
                        onClick={() => void handleToggleRule(rule)}
                      >
                        {rule.active ? "Deactivate" : "Reactivate"}
                      </button>
                    </td>
                  </tr>
                ))}
                {rules.length === 0 ? (
                  <tr>
                    <td colSpan={4}>
                      {radiusMode
                        ? "No legacy postcode rules configured. Radius delivery does not require them."
                        : "No delivery postcode rules configured. Postcode-rule delivery remains safely unavailable."}
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </div>
      </section>

    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function formStateFromSettings(
  settings: AdminFulfilmentSettingsResponse,
): SettingsFormState {
  return {
    baseDeliveryFee: formatOptionalInput(settings.baseDeliveryFeePence),
    baseDeliveryRadiusMiles: formatOptionalNumber(
      settings.baseDeliveryRadiusMiles,
    ),
    collectionEnabled: settings.collectionEnabled,
    deliveryAreaMode: settings.deliveryAreaMode,
    deliveryEnabled: settings.deliveryEnabled,
    deliveryFee: formatOptionalInput(settings.deliveryFeePence),
    deliveryPricingMode: settings.deliveryPricingMode,
    deliveryRadiusMiles: formatOptionalNumber(settings.deliveryRadiusMiles),
    extraMileFee: formatOptionalInput(settings.extraMileFeePence),
    freeDeliveryThreshold: formatOptionalInput(
      settings.freeDeliveryThresholdPence,
    ),
    minimumDeliveryOrder: formatOptionalInput(settings.minimumDeliveryOrderPence),
    preparationTimeMinutes: formatOptionalNumber(
      settings.preparationTimeMinutes,
    ),
    restaurantLatitude: formatOptionalNumber(settings.restaurantLatitude),
    restaurantLongitude: formatOptionalNumber(settings.restaurantLongitude),
    restaurantPostcode: settings.restaurantPostcode ?? "",
  };
}

function formatOptionalInput(value: number | null) {
  return value === null ? "" : formatPenniesForInput(value);
}

function formatOptionalPence(value: number | null) {
  return value === null ? "Not configured" : formatGbpPennies(value);
}

function formatOptionalNumber(value: number | null) {
  return value === null ? "" : String(value);
}

function formatMiles(value: number | null) {
  return value === null ? "Not configured" : `${value.toFixed(1)} miles`;
}

function parseOptionalMoney(
  value: string,
  label: string,
): ParsedOptionalMoney {
  if (!value.trim()) {
    return { value: null };
  }

  const parsed = parseGbpToPennies(value);

  if (parsed === null) {
    return {
      error: `${label} must be a valid GBP amount such as 2.50.`,
      value: null,
    };
  }

  return { value: parsed };
}

function parseOptionalDecimal(
  value: string,
  label: string,
  options: { max?: number; min?: number } = { min: 0 },
): ParsedOptionalNumber {
  if (!value.trim()) {
    return { value: null };
  }

  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return {
      error: `${label} must be a valid number.`,
      value: null,
    };
  }

  if (options.min !== undefined && parsed < options.min) {
    return {
      error: `${label} must be at least ${options.min}.`,
      value: null,
    };
  }

  if (options.max !== undefined && parsed > options.max) {
    return {
      error: `${label} must be no more than ${options.max}.`,
      value: null,
    };
  }

  return { value: parsed };
}

function parseOptionalInteger(
  value: string,
  label: string,
): ParsedOptionalNumber {
  if (!value.trim()) {
    return { value: null };
  }

  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 0) {
    return {
      error: `${label} must be a whole number.`,
      value: null,
    };
  }

  return { value: parsed };
}

function nextRuleDisplayOrder(rules: DeliveryPostcodeRuleResponse[]) {
  return rules.reduce((highest, rule) => Math.max(highest, rule.displayOrder), 0) + 1;
}

function sortRules(rules: DeliveryPostcodeRuleResponse[]) {
  return [...rules].sort(
    (firstRule, secondRule) =>
      firstRule.displayOrder - secondRule.displayOrder ||
      firstRule.postcodePattern.localeCompare(secondRule.postcodePattern),
  );
}
