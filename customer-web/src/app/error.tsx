"use client";

export default function CustomerError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <main className="menu-page">
      <section className="menu-api-error" aria-labelledby="customer-error-title">
        <div className="menu-api-error__content">
          <p className="type-eyebrow menu-api-error__eyebrow">Basilico</p>
          <h1 className="type-h2 menu-api-error__title" id="customer-error-title">
            Something went wrong.
          </h1>
          <p className="type-body menu-api-error__copy">
            Please try again, or call Basilico and we will help.
          </p>
          <div className="menu-api-error__actions">
            <button className="button button--primary" onClick={reset} type="button">
              Try again
            </button>
            <a className="menu-api-error__phone" href="tel:07424642900">
              07424 642900
            </a>
          </div>
        </div>
      </section>
    </main>
  );
}
