"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useMemo, useState, useTransition } from "react";
import { createMenuItem, updateMenuItem } from "@/lib/api/admin-menu-api";
import { getAdminApiErrorMessage } from "@/lib/api/api-error";
import { CUSTOMER_WEB_URL } from "@/lib/api/config";
import {
  formatGbpPennies,
  formatPenniesForInput,
  parseGbpToPennies,
} from "@/lib/format-price";
import type {
  DietaryTag,
  MenuCategoryResponse,
  MenuItemResponse,
  ProductType,
} from "@/types/admin";

const dietaryTags: DietaryTag[] = ["V", "GF", "VE"];
const productTypes: ProductType[] = ["STANDARD", "PIZZA"];

type MenuItemFormProps = {
  categories: MenuCategoryResponse[];
  item?: MenuItemResponse;
};

export function MenuItemForm({ categories, item }: MenuItemFormProps) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [name, setName] = useState(item?.name ?? "");
  const [slug, setSlug] = useState(item?.slug ?? "");
  const [description, setDescription] = useState(item?.description ?? "");
  const [price, setPrice] = useState(
    item ? formatPenniesForInput(item.pricePence) : "",
  );
  const [categoryId, setCategoryId] = useState(
    categoryIdFromItem(item, categories)?.toString() ?? "",
  );
  const [imagePath, setImagePath] = useState(item?.imagePath ?? "");
  const [productType, setProductType] = useState<ProductType>(
    item?.productType ?? "STANDARD",
  );
  const [available, setAvailable] = useState(item?.available ?? true);
  const [active, setActive] = useState(item?.active ?? true);
  const [featured, setFeatured] = useState(item?.featured ?? false);
  const [customizable, setCustomizable] = useState(item?.customizable ?? false);
  const [displayOrder, setDisplayOrder] = useState(
    item?.displayOrder.toString() ?? "1",
  );
  const [selectedTags, setSelectedTags] = useState<DietaryTag[]>(
    item?.dietaryTags ?? [],
  );
  const previewUrl = useMemo(
    () =>
      imagePath.trim().startsWith("/")
        ? `${CUSTOMER_WEB_URL}${imagePath.trim()}`
        : "",
    [imagePath],
  );

  function toggleTag(tag: DietaryTag) {
    setSelectedTags((tags) =>
      tags.includes(tag)
        ? tags.filter((current) => current !== tag)
        : [...tags, tag],
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setFeedback("");

    const pricePence = parseGbpToPennies(price);
    const order = Number(displayOrder);
    const parsedCategoryId = Number(categoryId);

    if (!name.trim()) {
      setError("Name is required.");
      return;
    }

    if (!item && !slug.trim()) {
      setError("Slug is required for new menu items.");
      return;
    }

    if (pricePence === null) {
      setError("Price must be a valid GBP amount such as 8.99.");
      return;
    }

    if (!Number.isInteger(order)) {
      setError("Display order must be a whole number.");
      return;
    }

    if (!parsedCategoryId) {
      setError("Choose a category.");
      return;
    }

    const payload = {
      active,
      available,
      categoryId: parsedCategoryId,
      customizable,
      description: description.trim() || null,
      dietaryTags: selectedTags,
      displayOrder: order,
      featured,
      imagePath: imagePath.trim() || null,
      name: name.trim(),
      pricePence,
      productType,
    };

    try {
      if (item) {
        const updated = await updateMenuItem(item.id, payload);
        setFeedback(`${updated.name} saved at ${formatGbpPennies(updated.pricePence)}.`);
        startTransition(() => router.refresh());
      } else {
        const created = await createMenuItem({
          ...payload,
          slug: slug.trim(),
        });
        router.push(`/menu/${created.id}/edit`);
      }
    } catch (caught) {
      setError(getAdminApiErrorMessage(caught, "Could not save this menu item."));
    }
  }

  return (
    <form className="grid gap-4" onSubmit={handleSubmit}>
      <section className="panel">
        <div className="panel__body form-grid form-grid--two">
          <div className="field">
            <label htmlFor="name">Name</label>
            <input
              id="name"
              required
              value={name}
              onBlur={() => {
                if (!item && !slug.trim()) {
                  setSlug(slugify(name));
                }
              }}
              onChange={(event) => setName(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="slug">Slug</label>
            <input
              disabled={Boolean(item)}
              id="slug"
              required={!item}
              value={slug}
              onChange={(event) => setSlug(event.target.value)}
            />
            {item ? (
              <p className="text-sm text-muted">
                Existing slugs are not edited in this first admin version.
              </p>
            ) : null}
          </div>
          <div className="field">
            <label htmlFor="price">Price</label>
            <input
              id="price"
              inputMode="decimal"
              placeholder="8.99"
              required
              value={price}
              onChange={(event) => setPrice(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="category">Category</label>
            <select
              id="category"
              required
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
            >
              <option value="">Choose category</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="productType">Product type</label>
            <select
              id="productType"
              value={productType}
              onChange={(event) => setProductType(event.target.value as ProductType)}
            >
              {productTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="displayOrder">Display order</label>
            <input
              id="displayOrder"
              required
              type="number"
              value={displayOrder}
              onChange={(event) => setDisplayOrder(event.target.value)}
            />
          </div>
          <div className="field sm:col-span-2">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </div>
          <div className="field sm:col-span-2">
            <label htmlFor="imagePath">Image path</label>
            <input
              id="imagePath"
              placeholder="/images/menu/example.png"
              value={imagePath}
              onChange={(event) => setImagePath(event.target.value)}
            />
            {previewUrl ? (
              <div
                aria-label="Image path preview"
                className="image-preview mt-2"
                style={{ backgroundImage: `url(${previewUrl})` }}
              />
            ) : null}
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel__body form-grid">
          <div>
            <p className="field-label">Dietary markers</p>
            <div className="checkbox-grid mt-2">
              {dietaryTags.map((tag) => (
                <label className="checkbox-chip" key={tag}>
                  <input
                    checked={selectedTags.includes(tag)}
                    type="checkbox"
                    onChange={() => toggleTag(tag)}
                  />
                  <span>{labelForDietaryTag(tag)}</span>
                </label>
              ))}
            </div>
          </div>
          <div>
            <p className="field-label">Operational state</p>
            <div className="checkbox-grid mt-2">
              <label className="checkbox-chip">
                <input
                  checked={available}
                  type="checkbox"
                  onChange={(event) => setAvailable(event.target.checked)}
                />
                <span>Available</span>
              </label>
              <label className="checkbox-chip">
                <input
                  checked={active}
                  type="checkbox"
                  onChange={(event) => setActive(event.target.checked)}
                />
                <span>Active</span>
              </label>
              <label className="checkbox-chip">
                <input
                  checked={featured}
                  type="checkbox"
                  onChange={(event) => setFeatured(event.target.checked)}
                />
                <span>Featured</span>
              </label>
              <label className="checkbox-chip">
                <input
                  checked={customizable}
                  type="checkbox"
                  onChange={(event) => setCustomizable(event.target.checked)}
                />
                <span>Customizable</span>
              </label>
            </div>
          </div>
        </div>
      </section>

      <div className="action-row">
        <button className="button" disabled={pending} type="submit">
          {pending ? "Saving..." : item ? "Save menu item" : "Create menu item"}
        </button>
        <button className="button-ghost" type="button" onClick={() => router.push("/menu")}>
          Back to menu
        </button>
      </div>
      <div aria-live="polite">
        {feedback ? <p className="feedback">{feedback}</p> : null}
        {error ? <p className="error">{error}</p> : null}
      </div>
    </form>
  );
}

function categoryIdFromItem(
  item: MenuItemResponse | undefined,
  categories: MenuCategoryResponse[],
) {
  if (!item) {
    return categories[0]?.id;
  }

  return categories.find((category) => category.slug === item.categorySlug)?.id;
}

function labelForDietaryTag(tag: DietaryTag) {
  switch (tag) {
    case "V":
      return "V — Vegetarian";
    case "GF":
      return "GF — Gluten Free";
    case "VE":
      return "VE — Vegan";
  }
}

function slugify(value: string) {
  return value
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/['’]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}
