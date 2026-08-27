import { ButtonLink } from "@/components/ui/Button";
import { routes } from "@/lib/routes";

type MenuApiErrorStateProps = {
  actionHref?: string;
  actionLabel?: string;
  copy?: string;
  title?: string;
};

export function MenuApiErrorState({
  actionHref = routes.menu,
  actionLabel = "Try again",
  copy = "Please try again, or call Basilico and we will help.",
  title = "We’re having trouble loading the menu right now.",
}: MenuApiErrorStateProps) {
  return (
    <section className="menu-api-error" aria-labelledby="menu-api-error-title">
      <div className="menu-api-error__content">
        <p className="type-eyebrow menu-api-error__eyebrow">Basilico menu</p>
        <h1 className="type-h2 menu-api-error__title" id="menu-api-error-title">
          {title}
        </h1>
        <p className="type-body menu-api-error__copy">{copy}</p>
        <div className="menu-api-error__actions">
          <ButtonLink href={actionHref}>{actionLabel}</ButtonLink>
          <a className="menu-api-error__phone" href="tel:07424642900">
            07424 642900
          </a>
        </div>
      </div>
    </section>
  );
}
