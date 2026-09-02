import type { Metadata } from "next";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { MenuApiErrorState } from "@/components/menu/MenuApiErrorState";
import { MenuPage } from "@/components/menu/MenuPage";
import { getMenu } from "@/lib/api/menu-api";
import { buildPageMetadata } from "@/lib/seo";
import type { MenuData } from "@/types/menu";

export const metadata: Metadata = buildPageMetadata({
  title: "Basilico Menu | Italian Restaurant Dorchester",
  description:
    "Browse Basilico's Dorchester menu for sourdough pizza, Italian favourites, desserts, takeaway and delivery.",
  path: "/menu",
});

export const dynamic = "force-dynamic";

export default async function Menu() {
  let menu: MenuData | undefined;

  try {
    menu = await getMenu();
  } catch {
    menu = undefined;
  }

  if (!menu) {
    return (
      <>
        <HomeHeader />
        <main className="menu-page">
          <MenuApiErrorState />
        </main>
        <HomeFooter />
        <MobileConversionBar />
      </>
    );
  }

  return <MenuPage menu={menu} />;
}
