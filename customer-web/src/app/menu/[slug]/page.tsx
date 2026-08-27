import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { MenuApiErrorState } from "@/components/menu/MenuApiErrorState";
import { ProductDetailPage } from "@/components/menu/ProductDetailPage";
import {
  findMenuCategory,
  findRelatedMenuItems,
  getMenu,
  getMenuItemBySlug,
  isMenuApiNotFoundError,
} from "@/lib/api/menu-api";
import { routes } from "@/lib/routes";
import type { MenuData, MenuItem } from "@/types/menu";

type ProductPageProps = {
  params: Promise<{ slug: string }>;
};

export const dynamic = "force-dynamic";

export async function generateMetadata({
  params,
}: ProductPageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const item = await getMenuItemBySlug(slug);

    return {
      title: `${item.name} | Basilico Dorchester`,
      description:
        item.description ??
        `View ${item.name} on the Basilico menu in Dorchester.`,
    };
  } catch (error) {
    if (isMenuApiNotFoundError(error)) {
      return {
        title: "Menu item not found | Basilico Dorchester",
      };
    }

    return {
      title: "Basilico Menu | Dorchester",
    };
  }
}

export default async function ProductPage({ params }: ProductPageProps) {
  const { slug } = await params;
  let item: MenuItem | undefined;
  let menu: MenuData | undefined;

  try {
    [item, menu] = await Promise.all([getMenuItemBySlug(slug), getMenu()]);
  } catch (error) {
    if (isMenuApiNotFoundError(error)) {
      notFound();
    }

    item = undefined;
    menu = undefined;
  }

  if (!item || !menu) {
    return (
      <>
        <HomeHeader />
        <main className="product-page">
          <Container className="product-page__container">
            <MenuApiErrorState
              actionHref={`${routes.menu}/${slug}`}
              copy="Please try again, or call Basilico and we will help."
              title="We’re having trouble loading this dish right now."
            />
          </Container>
        </main>
        <HomeFooter />
        <MobileConversionBar />
      </>
    );
  }

  const category = findMenuCategory(menu, item.category);

  if (!category) {
    notFound();
  }

  return (
    <ProductDetailPage
      category={category}
      item={item}
      relatedItems={findRelatedMenuItems(menu, item)}
    />
  );
}
