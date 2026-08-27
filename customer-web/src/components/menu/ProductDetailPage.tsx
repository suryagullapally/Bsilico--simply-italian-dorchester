import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { ProductDetail } from "@/components/menu/ProductDetail";
import { RelatedMenuItems } from "@/components/menu/RelatedMenuItems";
import { dietaryLabels } from "@/data/menu";
import type { MenuCategory, MenuItem } from "@/types/menu";

type ProductDetailPageProps = {
  category: MenuCategory;
  item: MenuItem;
  relatedItems: MenuItem[];
};

export function ProductDetailPage({
  category,
  item,
  relatedItems,
}: ProductDetailPageProps) {
  return (
    <>
      <HomeHeader />
      <main className="product-page">
        <Container className="product-page__container">
          <ProductDetail
            category={category}
            dietaryLabels={dietaryLabels}
            item={item}
          />
          <RelatedMenuItems
            dietaryLabels={dietaryLabels}
            items={relatedItems}
          />
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
