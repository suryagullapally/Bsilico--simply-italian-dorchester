"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { CUSTOMER_WEB_URL } from "@/lib/api/config";
import { formatDate } from "@/lib/date-time";

const navItems = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/orders", label: "Orders" },
  { href: "/bookings", label: "Bookings" },
  { href: "/menu", label: "Menu" },
  { href: "/payments", label: "Payments" },
  { href: "/messages", label: "Messages" },
  { href: "/settings/fulfilment", label: "Fulfilment" },
];

export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { admin, logout, status } = useAuth();
  const [open, setOpen] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const pageTitle = useMemo(() => getPageTitle(pathname), [pathname]);
  const today = useMemo(
    () => formatDate(new Date().toLocaleDateString("en-CA")),
    [],
  );
  const isLogin = pathname === "/login";

  useEffect(() => {
    if (status === "unauthenticated" && !isLogin) {
      router.replace(`/login?returnTo=${encodeURIComponent(pathname)}`);
    }
  }, [isLogin, pathname, router, status]);

  async function handleLogout() {
    setSigningOut(true);

    try {
      await logout();
      router.replace("/login");
    } finally {
      setSigningOut(false);
    }
  }

  if (isLogin) {
    return <>{children}</>;
  }

  if (status === "loading" || status === "unauthenticated") {
    return (
      <main className="auth-loading" aria-live="polite">
        <div className="panel">
          <div className="panel__body">
            <p className="eyebrow">Basilico Admin</p>
            <p className="mt-2 text-muted">Checking your admin session...</p>
          </div>
        </div>
      </main>
    );
  }

  return (
    <div className="admin-shell">
      {open ? (
        <button
          aria-label="Close navigation"
          className="admin-scrim"
          type="button"
          onClick={() => setOpen(false)}
        />
      ) : null}

      <aside className={`admin-sidebar ${open ? "admin-sidebar--open" : ""}`}>
        <div className="admin-brand">
          <div className="admin-brand__title">Basilico</div>
          <div className="admin-brand__sub">Simple Italian Admin</div>
        </div>
        <nav aria-label="Admin navigation" className="admin-nav">
          {navItems.map((item) => {
            const active =
              pathname === item.href || pathname.startsWith(`${item.href}/`);

            return (
              <Link
                className={`admin-nav__link ${active ? "admin-nav__link--active" : ""}`}
                href={item.href}
                key={item.href}
                onClick={() => setOpen(false)}
              >
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto grid gap-3 p-4">
          <a
            className="button-ghost"
            href={CUSTOMER_WEB_URL}
            rel="noreferrer"
            target="_blank"
          >
            Open Customer Website
          </a>
          <button
            className="button-ghost"
            disabled={signingOut}
            type="button"
            onClick={handleLogout}
          >
            {signingOut ? "Signing out..." : "Sign out"}
          </button>
          <div className="notice text-sm">
            <strong>Development Environment</strong>
            <br />
            Admin is session protected. HTTPS and deployment hardening come later.
          </div>
        </div>
      </aside>

      <div className="min-w-0">
        <header className="admin-topbar">
          <div className="flex items-center gap-3">
            <button
              aria-expanded={open}
              aria-label="Open navigation"
              className="button-ghost lg:hidden"
              type="button"
              onClick={() => setOpen(true)}
            >
              Menu
            </button>
            <div>
              <p className="eyebrow">Basilico Admin</p>
              <p className="font-bold">{pageTitle}</p>
            </div>
          </div>
          <div className="hidden text-right text-sm text-muted sm:block">
            <p className="font-black text-foreground">{admin?.displayName}</p>
            <p>{admin?.role}</p>
            <p>{today}</p>
            <p>Europe/London</p>
          </div>
        </header>
        <main className="admin-main">{children}</main>
      </div>
    </div>
  );
}

function getPageTitle(pathname: string) {
  if (pathname.startsWith("/orders")) {
    return "Orders";
  }

  if (pathname.startsWith("/bookings")) {
    return "Bookings";
  }

  if (pathname.startsWith("/menu")) {
    return "Menu Management";
  }

  if (pathname.startsWith("/payments")) {
    return "Payments";
  }

  if (pathname.startsWith("/messages")) {
    return "Messages";
  }

  if (pathname.startsWith("/settings")) {
    return "Settings";
  }

  return "Dashboard";
}
