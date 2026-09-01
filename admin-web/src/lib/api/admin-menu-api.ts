import { adminGet, adminMutation } from "@/lib/api/api-error";
import type {
  ActiveUpdateRequest,
  AvailabilityUpdateRequest,
} from "@/lib/api/admin-menu-api.types";
import type {
  FeaturedUpdateRequest,
  MenuItemRequestPayload,
} from "@/lib/api/admin-menu-api.types";
import type {
  MenuItemResponse,
  MenuResponse,
} from "@/types/admin";

export async function getAdminMenuItems() {
  return adminGet<MenuItemResponse[]>("/api/admin/menu/items");
}

export async function getAdminMenuItem(id: string | number) {
  return adminGet<MenuItemResponse>(`/api/admin/menu/items/${id}`);
}

export async function getMenuCategories() {
  const menu = await adminGet<MenuResponse>("/api/menu");

  return [...menu.categories].sort(
    (first, second) => first.displayOrder - second.displayOrder,
  );
}

export async function createMenuItem(payload: MenuItemRequestPayload) {
  return adminMutation<MenuItemResponse>("/api/admin/menu/items", "POST", payload);
}

export async function updateMenuItem(
  id: string | number,
  payload: Omit<MenuItemRequestPayload, "slug">,
) {
  return adminMutation<MenuItemResponse>(
    `/api/admin/menu/items/${id}`,
    "PUT",
    payload,
  );
}

export async function updateMenuItemAvailability(
  id: string | number,
  payload: AvailabilityUpdateRequest,
) {
  return adminMutation<MenuItemResponse>(
    `/api/admin/menu/items/${id}/availability`,
    "PATCH",
    payload,
  );
}

export async function updateMenuItemActive(
  id: string | number,
  payload: ActiveUpdateRequest,
) {
  return adminMutation<MenuItemResponse>(
    `/api/admin/menu/items/${id}/active`,
    "PATCH",
    payload,
  );
}

export async function updateMenuItemFeatured(
  id: string | number,
  payload: FeaturedUpdateRequest,
) {
  return adminMutation<MenuItemResponse>(
    `/api/admin/menu/items/${id}/featured`,
    "PATCH",
    payload,
  );
}
