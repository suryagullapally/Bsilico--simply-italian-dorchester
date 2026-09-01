"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";

export function LoginPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login, status } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/dashboard");
    }
  }, [router, status]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      await login({ email, password });
      const returnTo = safeReturnPath(searchParams.get("returnTo"));
      router.replace(returnTo ?? "/dashboard");
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Invalid email or password."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-screen">
      <section className="login-panel" aria-labelledby="login-title">
        <div>
          <p className="eyebrow">Basilico Admin</p>
          <h1 className="page-title mt-2" id="login-title">
            Welcome back.
          </h1>
          <p className="mt-3 text-muted">Basilico – Simple Italian · Dorchester</p>
        </div>

        <form className="mt-8 grid gap-4" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              autoComplete="username"
              id="email"
              required
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <div className="password-field">
              <input
                autoComplete="current-password"
                id="password"
                required
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <button
                className="button-ghost"
                type="button"
                onClick={() => setShowPassword((value) => !value)}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
          </div>

          <button className="button w-full" disabled={submitting} type="submit">
            {submitting ? "Signing in..." : "Sign in"}
          </button>

          <div aria-live="polite">
            {error ? <p className="error">{error}</p> : null}
          </div>
        </form>
      </section>
    </main>
  );
}

function safeReturnPath(value: string | null) {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return null;
  }

  if (value === "/login") {
    return null;
  }

  return value;
}
