"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  getCurrentAdmin,
  login as loginRequest,
  logout as logoutRequest,
  type LoginPayload,
} from "@/lib/api/admin-auth-api";
import { AdminApiError } from "@/lib/api/api-error";
import type { CurrentAdmin } from "@/types/admin";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

type AuthContextValue = {
  admin: CurrentAdmin | null;
  status: AuthStatus;
  login: (payload: LoginPayload) => Promise<CurrentAdmin>;
  logout: () => Promise<void>;
  refreshAdmin: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [admin, setAdmin] = useState<CurrentAdmin | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  const refreshAdmin = useCallback(async () => {
    try {
      const currentAdmin = await getCurrentAdmin();
      setAdmin(currentAdmin);
      setStatus("authenticated");
    } catch (error) {
      if (error instanceof AdminApiError && error.status === 401) {
        setAdmin(null);
        setStatus("unauthenticated");
        return;
      }

      setAdmin(null);
      setStatus("unauthenticated");
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void refreshAdmin();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [refreshAdmin]);

  const login = useCallback(async (payload: LoginPayload) => {
    const currentAdmin = await loginRequest(payload);
    setAdmin(currentAdmin);
    setStatus("authenticated");
    return currentAdmin;
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } finally {
      setAdmin(null);
      setStatus("unauthenticated");
    }
  }, []);

  const value = useMemo(
    () => ({ admin, login, logout, refreshAdmin, status }),
    [admin, login, logout, refreshAdmin, status],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
