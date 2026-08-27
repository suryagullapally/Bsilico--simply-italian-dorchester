import Link from "next/link";
import { HomeImageSlot } from "@/components/home/HomeImageSlot";
import { MenuDietaryTags } from "@/components/menu/MenuDietaryTags";
import { formatGbpPennies } from "@/lib/format-price";
import type { DietaryTag, MenuItem } from "@/types/menu";

type RelatedMenuItemsProps = {
  dietaryLabels: Record<DietaryTag, string>;
  items: MenuItem[];
};

export function RelatedMenuItems({
  dietaryLabels,
  items,
}: RelatedMenuItemsProps) {
  if (items.length === 0) {
    return null;
  }

  return (
    <section className="related-menu" aria-labelledby="related-menu-title">
      <div className="related-menu__header">
        <p className="type-eyebrow related-menu__eyebrow">You may also like</p>
        <h2 className="type-h2 related-menu__title" id="related-menu-title">
          From the same section.
        </h2>
      </div>

      <div className="related-menu__grid">
        {items.map((item) => {
          const isSoldOut = !item.available;

          return (
            <article
              className={[
                "related-card",
                item.image ? "related-card--image" : "related-card--text",
                isSoldOut ? "related-card--sold-out" : undefined,
              ]
                .filter(Boolean)
                .join(" ")}
              key={item.slug}
            >
              {item.image ? (
                <HomeImageSlot
                  alt={item.image.alt}
                  className="related-card__media"
                  imageClassName="related-card__image"
                  sizes="(max-width: 767px) 92vw, (max-width: 1279px) 30vw, 18rem"
                  src={item.image.src}
                />
              ) : null}

              <div className="related-card__body">
                <div className="related-card__heading">
                  <h3 className="type-h3 related-card__title">{item.name}</h3>
                  <div className="related-card__price-group">
                    <p className="type-price related-card__price">
                      {formatGbpPennies(item.pricePence)}
                    </p>
                    {isSoldOut ? (
                      <span className="related-card__availability">
                        SOLD OUT
                      </span>
                    ) : null}
                  </div>
                </div>
                <MenuDietaryTags
                  className="related-card__tags"
                  dietaryLabels={dietaryLabels}
                  tags={item.dietaryTags}
                />
                {item.description ? (
                  <p className="type-small related-card__description">
                    {item.description}
                  </p>
                ) : null}
                <Link
                  className="related-card__link"
                  href={`/menu/${item.slug}`}
                  aria-label={`View ${item.name}`}
                >
                  View dish
                </Link>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
