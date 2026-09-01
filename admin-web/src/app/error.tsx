"use client";

export default function AdminError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="content-shell">
      <section className="panel">
        <div className="panel__body">
          <p className="eyebrow">Admin</p>
          <h1 className="section-title mt-2">Something went wrong.</h1>
          <p className="mt-3 text-muted">
            The admin interface could not complete that action. Please try again.
          </p>
          <div className="mt-5">
            <button className="button" onClick={reset} type="button">
              Try again
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}
