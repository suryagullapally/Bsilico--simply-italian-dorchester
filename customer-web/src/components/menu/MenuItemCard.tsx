import Link from "next/link";
import { HomeImageSlot } from "@/components/home/HomeImageSlot";
import { MenuDietaryTags } from "@/components/menu/MenuDietaryTags";
import { MenuItemCartControls } from "@/components/menu/MenuItemCartControls";
import { formatGbpPennies } from "@/lib/format-price";
import type { DietaryTag, MenuItem } from "@/types/menu";

type MenuItemCardProps = {
  dietaryLabels: Record<DietaryTag, string>;
  item: MenuItem;
};

export function MenuItemCard({ dietaryLabels, item }: MenuItemCardProps) {
  const isSoldOut = !item.available;

  return (
    <article
      className={[
        "menu-item",
        item.image ? "menu-item--with-image" : "menu-item--text-only",
        isSoldOut ? "menu-item--sold-out" : undefined,
      ]
        .filter(Boolean)
        .join(" ")}
      id={item.id}
    >
      <Link
        aria-label={`View ${item.name}`}
        className="menu-item__product-link"
        href={`/menu/${item.slug}`}
      >
        {item.image ? (
          <HomeImageSlot
            alt={item.image.alt}
            className="menu-item__media"
            imageClassName="menu-item__image"
            sizes="(max-width: 767px) 92vw, (max-width: 1279px) 34vw, 18rem"
            src={item.image.src}
          />
        ) : null}

        <div className="menu-item__body">
          <div className="menu-item__heading">
            <div className="menu-item__title-group">
              <h3 className="type-h3 menu-item__title">{item.name}</h3>
              <MenuDietaryTags
                dietaryLabels={dietaryLabels}
                tags={item.dietaryTags}
              />
            </div>

            <div className="menu-item__price-group">
              <p className="type-price menu-item__price">
                {formatGbpPennies(item.pricePence)}
              </p>
              {isSoldOut ? (
                <span className="menu-item__availability">SOLD OUT</span>
              ) : null}
            </div>
          </div>

          {item.description ? (
            <p className="type-small menu-item__description">
              {item.description}
            </p>
          ) : null}

          <span className="menu-item__link" aria-hidden="true">
            {item.customizable ? "Customise" : "View dish"}
          </span>
        </div>
      </Link>

      <div className="menu-item__footer">
        <MenuItemCartControls item={item} />
      </div>
    </article>
  );
}
