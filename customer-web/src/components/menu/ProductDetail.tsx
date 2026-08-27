import Link from "next/link";
import { ProductInfo } from "@/components/menu/ProductInfo";
import { ProductVisual } from "@/components/menu/ProductVisual";
import { routes } from "@/lib/routes";
import type { DietaryTag, MenuCategory, MenuItem } from "@/types/menu";

type ProductDetailProps = {
  category: MenuCategory;
  dietaryLabels: Record<DietaryTag, string>;
  item: MenuItem;
};

export function ProductDetail({
  category,
  dietaryLabels,
  item,
}: ProductDetailProps) {
  return (
    <section
      className={[
        "product-detail",
        item.image ? "product-detail--image" : "product-detail--text",
      ]
        .filter(Boolean)
        .join(" ")}
      aria-label={item.name}
    >
      <Link className="product-detail__back" href={routes.menu}>
        ← Back to Menu
      </Link>

      <div className="product-detail__layout">
        <ProductVisual
          category={category}
          dietaryLabels={dietaryLabels}
          item={item}
        />
        <ProductInfo
          category={category}
          dietaryLabels={dietaryLabels}
          item={item}
        />
      </div>
    </section>
  );
}
