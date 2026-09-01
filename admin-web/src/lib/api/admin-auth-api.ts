import {
  adminGet,
  adminMutation,
  clearAdminCsrfToken,
  getAdminCsrfToken,
} from "@/lib/api/api-error";
import type { CurrentAdmin } from "@/types/admin";

export type LoginPayload = {
  email: string;
  password: string;
};

export async function getCsrfToken() {
  return getAdminCsrfToken();
}

export async function login(payload: LoginPayload) {
  const admin = await adminMutation<CurrentAdmin>(
    "/api/admin/auth/login",
    "POST",
    payload,
  );
  clearAdminCsrfToken();
  return admin;
}

export async function getCurrentAdmin() {
  return adminGet<CurrentAdmin>("/api/admin/auth/me");
}

export async function logout() {
  await adminMutation<void>("/api/admin/auth/logout", "POST", {});
  clearAdminCsrfToken();
}
