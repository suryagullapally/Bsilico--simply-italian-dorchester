"use client";

import { useCallback, useEffect, useState } from "react";
import { FulfilmentSettingsPage } from "@/components/settings/FulfilmentSettingsPage";
import { ErrorPanel } from "@/components/ui/ErrorPanel";
import { LoadingPanel } from "@/components/ui/LoadingPanel";
import {
  getAdminDeliveryPostcodeRules,
  getAdminFulfilmentSettings,
} from "@/lib/api/admin-fulfilment-api";
import type {
  AdminFulfilmentSettingsResponse,
  DeliveryPostcodeRuleResponse,
} from "@/types/admin";

export type FulfilmentAdminData = {
  rules: DeliveryPostcodeRuleResponse[];
  settings: AdminFulfilmentSettingsResponse;
};

export function FulfilmentSettingsRouteClient() {
  const [data, setData] = useState<FulfilmentAdminData | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (showLoading = true) => {
    if (showLoading) {
      setLoading(true);
    }

    try {
      const [settings, rules] = await Promise.all([
        getAdminFulfilmentSettings(),
        getAdminDeliveryPostcodeRules(),
      ]);
      setData({ rules, settings });
    } catch {
      setData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [load]);

  if (loading) {
    return <LoadingPanel label="Loading fulfilment settings..." />;
  }

  if (!data) {
    return (
      <ErrorPanel title="Could not load fulfilment settings." />
    );
  }

  return <FulfilmentSettingsPage data={data} onRefresh={() => load(false)} />;
}
