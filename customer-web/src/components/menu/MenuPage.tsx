import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { MenuCategoryNav } from "@/components/menu/MenuCategoryNav";
import { MenuCategorySection } from "@/components/menu/MenuCategorySection";
import { MenuDeliveryStatus } from "@/components/menu/MenuDeliveryStatus";
import { MenuDietaryLegend } from "@/components/menu/MenuDietaryLegend";
import { MenuScrollReset } from "@/components/menu/MenuScrollReset";
import { dietaryLabels, dietaryLegend } from "@/data/menu";
import type { MenuData } from "@/types/menu";

type MenuPageProps = {
  menu: MenuData;
};

export function MenuPage({ menu }: MenuPageProps) {
  return (
    <>
      <HomeHeader />
      <main className="menu-page">
        <MenuScrollReset />
        <section className="menu-intro" aria-labelledby="menu-page-title">
          <Container className="menu-intro__container">
            <p className="type-eyebrow menu-intro__eyebrow">Basilico menu</p>
            <h1 className="type-h1 menu-intro__title" id="menu-page-title">
              Choose your favourite.
            </h1>
            <p className="type-body menu-intro__copy">
              From sourdough pizza and Italian favourites to something sweet,
              available for Dorchester takeaway and delivery.
            </p>
          </Container>
        </section>

        <MenuDeliveryStatus />
        <MenuCategoryNav categories={menu.categories} />
        <MenuDietaryLegend items={dietaryLegend} />

        <Container className="menu-sections">
          {menu.categories.map((category) => (
            <MenuCategorySection
              category={category}
              createYourOwnBlock={menu.createYourOwn}
              dietaryLabels={dietaryLabels}
              items={menu.itemsByCategory[category.id]}
              key={category.id}
            />
          ))}
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
