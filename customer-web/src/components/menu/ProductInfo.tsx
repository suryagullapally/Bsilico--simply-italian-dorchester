import { MenuDietaryTags } from "@/components/menu/MenuDietaryTags";
import { ProductActionArea } from "@/components/menu/ProductActionArea";
import { formatGbpPennies } from "@/lib/format-price";
import type { DietaryTag, MenuCategory, MenuItem } from "@/types/menu";

type ProductInfoProps = {
  category: MenuCategory;
  dietaryLabels: Record<DietaryTag, string>;
  item: MenuItem;
};

export function ProductInfo({
  category,
  dietaryLabels,
  item,
}: ProductInfoProps) {
  const isSoldOut = !item.available;

  return (
    <div className="product-info">
      <p className="type-eyebrow product-info__category">{category.title}</p>
      <h1 className="type-h1 product-info__title">{item.name}</h1>

      <div className="product-info__meta">
        <MenuDietaryTags
          className="product-info__tags"
          dietaryLabels={dietaryLabels}
          tags={item.dietaryTags}
        />
        {isSoldOut ? (
          <span className="product-info__availability">SOLD OUT</span>
        ) : null}
      </div>

      {item.description ? (
        <p className="type-body product-info__description">{item.description}</p>
      ) : null}

      <p className="type-price product-info__price">
        {formatGbpPennies(item.pricePence)}
      </p>

      <ProductActionArea item={item} />

      <p className="type-small product-info__allergy">
        Please tell us about any allergies or dietary requirements before
        ordering.
      </p>
    </div>
  );
}
