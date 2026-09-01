import Link from "next/link";
import { MenuItemCard } from "@/components/menu/MenuItemCard";
import { routes } from "@/lib/routes";
import type {
  CreateYourOwnConfiguration,
  DietaryTag,
  MenuCategory,
  MenuItem,
} from "@/types/menu";

type MenuCategorySectionProps = {
  category: MenuCategory;
  createYourOwnBlock?: CreateYourOwnConfiguration;
  dietaryLabels: Record<DietaryTag, string>;
  items: MenuItem[];
};

export function MenuCategorySection({
  category,
  createYourOwnBlock,
  dietaryLabels,
  items,
}: MenuCategorySectionProps) {
  const customizer =
    category.id === "sourdough-pizza-calzone" ? createYourOwnBlock : undefined;

  return (
    <section
      className="menu-section"
      id={category.id}
      aria-labelledby={`${category.id}-title`}
    >
      <header className="menu-section__header">
        <p className="type-eyebrow menu-section__eyebrow">Menu</p>
        <h2 className="type-h2 menu-section__title" id={`${category.id}-title`}>
          {category.title}
        </h2>
      </header>

      <div className="menu-section__content">
        {items.length > 0 ? (
          <div className="menu-section__items">
            {items.map((item) => (
              <MenuItemCard
                dietaryLabels={dietaryLabels}
                item={item}
                key={item.id}
              />
            ))}
          </div>
        ) : (
          <p className="type-small menu-section__empty">
            Confirmed Basilico dishes for this section are being added.
          </p>
        )}

        {customizer ? (
          <aside
            className="menu-create-own"
            aria-labelledby="create-your-own-title"
          >
            <Link
              className="menu-create-own__link"
              href={routes.createYourOwn}
              aria-labelledby="create-your-own-title"
            >
              <p className="type-eyebrow menu-create-own__eyebrow">
                Sourdough pizza
              </p>
              <h3
                className="type-h3 menu-create-own__title"
                id="create-your-own-title"
              >
                {customizer.title}
              </h3>
              <div className="menu-create-own__pricing">
                <p className="type-price menu-create-own__price-line">
                  {customizer.pricingCopy}
                </p>
                <p className="type-small menu-create-own__copy">
                  {customizer.extraToppingCopy}
                </p>
              </div>
              <ul
                className="menu-create-own__toppings"
                aria-label="Available toppings"
              >
                {customizer.toppings.map((topping) => (
                  <li key={topping.id}>{topping.name}</li>
                ))}
              </ul>
              <span className="menu-create-own__cta">Create yours</span>
            </Link>
          </aside>
        ) : null}
      </div>
    </section>
  );
}
