import { Suspense } from "react";
import { LoginPageContent } from "@/components/auth/LoginPageContent";

export default function LoginPage() {
  return (
    <Suspense fallback={<main className="auth-loading">Loading sign in...</main>}>
      <LoginPageContent />
    </Suspense>
  );
}
