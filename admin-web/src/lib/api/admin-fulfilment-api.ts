import { adminGet, adminMutation } from "@/lib/api/api-error";
import type {
  AdminFulfilmentSettingsRequest,
  AdminFulfilmentSettingsResponse,
  CreateDeliveryPostcodeRuleRequest,
  DeliveryPostcodeRuleResponse,
} from "@/types/admin";

export function getAdminFulfilmentSettings() {
  return adminGet<AdminFulfilmentSettingsResponse>(
    "/api/admin/fulfilment/settings",
  );
}

export function updateAdminFulfilmentSettings(
  request: AdminFulfilmentSettingsRequest,
) {
  return adminMutation<AdminFulfilmentSettingsResponse>(
    "/api/admin/fulfilment/settings",
    "PUT",
    request,
  );
}

export function getAdminDeliveryPostcodeRules() {
  return adminGet<DeliveryPostcodeRuleResponse[]>(
    "/api/admin/fulfilment/postcode-rules",
  );
}

export function createAdminDeliveryPostcodeRule(
  request: CreateDeliveryPostcodeRuleRequest,
) {
  return adminMutation<DeliveryPostcodeRuleResponse>(
    "/api/admin/fulfilment/postcode-rules",
    "POST",
    request,
  );
}

export function updateAdminDeliveryPostcodeRuleActive(
  id: string | number,
  active: boolean,
) {
  return adminMutation<DeliveryPostcodeRuleResponse>(
    `/api/admin/fulfilment/postcode-rules/${id}/active`,
    "PATCH",
    { active },
  );
}
