import { HomeImageSlot } from "@/components/home/HomeImageSlot";
import { MenuDietaryTags } from "@/components/menu/MenuDietaryTags";
import type { DietaryTag, MenuCategory, MenuItem } from "@/types/menu";

type ProductVisualProps = {
  category: MenuCategory;
  dietaryLabels: Record<DietaryTag, string>;
  item: MenuItem;
};

export function ProductVisual({
  category,
  dietaryLabels,
  item,
}: ProductVisualProps) {
  if (item.image) {
    return (
      <HomeImageSlot
        alt={item.image.alt}
        className="product-visual product-visual--image"
        imageClassName="product-visual__image"
        priority
        sizes="(max-width: 767px) 100vw, (max-width: 1279px) 48vw, 38rem"
        src={item.image.src}
      />
    );
  }

  return (
    <div className="product-visual product-visual--text" aria-hidden="true">
      <div className="product-visual__text-content">
        <p className="type-eyebrow product-visual__category">{category.title}</p>
        <p className="type-h2 product-visual__name">{item.name}</p>
        <MenuDietaryTags
          className="product-visual__tags"
          dietaryLabels={dietaryLabels}
          tags={item.dietaryTags}
        />
      </div>
    </div>
  );
}
