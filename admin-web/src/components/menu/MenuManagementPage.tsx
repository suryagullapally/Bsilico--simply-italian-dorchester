import Link from "next/link";
import { MenuQuickActions } from "@/components/menu/MenuQuickActions";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { formatGbpPennies } from "@/lib/format-price";
import type { MenuCategoryResponse, MenuItemResponse } from "@/types/admin";

export function MenuManagementPage({
  categories,
  items,
  onItemUpdated,
}: {
  categories: MenuCategoryResponse[];
  items: MenuItemResponse[];
  onItemUpdated?: (item: MenuItemResponse) => void;
}) {
  const groupedItems = groupByCategory(items, categories);

  return (
    <div className="grid gap-5">
      {groupedItems.map((group) => (
        <section className="menu-group" key={group.categoryName}>
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <p className="eyebrow">Category</p>
              <h2 className="section-title">{group.categoryName}</h2>
            </div>
            <p className="text-sm text-muted">{group.items.length} items</p>
          </div>
          <div className="grid gap-3">
            {group.items.map((item) => (
              <article className="panel menu-item-row" key={item.id}>
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-lg font-black">{item.name}</h3>
                    <StatusBadge value={item.available ? "AVAILABLE" : "SOLD_OUT"} />
                    <StatusBadge value={item.active ? "ACTIVE" : "ARCHIVED"} />
                    {item.featured ? <StatusBadge value="FEATURED" /> : null}
                  </div>
                  <p className="mt-1 text-sm font-black">
                    {formatGbpPennies(item.pricePence)}
                  </p>
                  {item.description ? (
                    <p className="mt-2 max-w-4xl text-sm text-muted">
                      {item.description}
                    </p>
                  ) : null}
                  <div className="mt-3 flex flex-wrap gap-3 text-xs font-black text-muted">
                    <span>{item.productType}</span>
                    <span>Display {item.displayOrder}</span>
                    {item.dietaryTags.length > 0 ? (
                      <span>{item.dietaryTags.join(" · ")}</span>
                    ) : null}
                  </div>
                </div>
                <div className="grid gap-3">
                  <MenuQuickActions item={item} onItemUpdated={onItemUpdated} />
                  <Link className="button-ghost" href={`/menu/${item.id}/edit`}>
                    Edit
                  </Link>
                </div>
              </article>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function groupByCategory(
  items: MenuItemResponse[],
  categories: MenuCategoryResponse[],
) {
  const itemsByCategory = new Map<string, MenuItemResponse[]>();

  for (const item of items) {
    itemsByCategory.set(item.categorySlug, [
      ...(itemsByCategory.get(item.categorySlug) ?? []),
      item,
    ]);
  }

  return categories
    .map((category) => ({
      categoryName: category.name,
      items: (itemsByCategory.get(category.slug) ?? []).sort(
        (first, second) => first.displayOrder - second.displayOrder,
      ),
    }))
    .filter((group) => group.items.length > 0);
}
