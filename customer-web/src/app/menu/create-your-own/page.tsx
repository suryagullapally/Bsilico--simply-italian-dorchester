import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { Container } from "@/components/layout/Container";
import { MenuApiErrorState } from "@/components/menu/MenuApiErrorState";
import { PizzaCustomizer } from "@/components/pizza-customizer/PizzaCustomizer";
import {
  getCreateYourOwnCustomizer,
  isMenuApiNotFoundError,
} from "@/lib/api/menu-api";
import { routes } from "@/lib/routes";
import { buildPageMetadata } from "@/lib/seo";
import type { CreateYourOwnConfiguration } from "@/types/menu";

export const metadata: Metadata = buildPageMetadata({
  title: "Create Your Own Pizza | Basilico Dorchester",
  description:
    "Create your own Basilico sourdough pizza with extra toppings from the Basilico menu in Dorchester.",
  path: "/menu/create-your-own",
});

export const dynamic = "force-dynamic";

export default async function CreateYourOwnPizzaPage() {
  let configuration: CreateYourOwnConfiguration | undefined;

  try {
    configuration = await getCreateYourOwnCustomizer();
  } catch (error) {
    if (isMenuApiNotFoundError(error)) {
      notFound();
    }

    configuration = undefined;
  }

  if (!configuration) {
    return (
      <>
        <HomeHeader />
        <main className="pizza-customizer-page">
          <Container className="pizza-customizer-page__container">
            <Link className="pizza-customizer-page__back" href={routes.menu}>
              ← Back to Menu
            </Link>
            <MenuApiErrorState
              actionHref={routes.createYourOwn}
              copy="Please try again, or call Basilico and we will help."
              title="We’re having trouble loading Create Your Own right now."
            />
          </Container>
        </main>
        <HomeFooter />
        <MobileConversionBar />
      </>
    );
  }

  return (
    <>
      <HomeHeader />
      <main className="pizza-customizer-page">
        <Container className="pizza-customizer-page__container">
          <Link
            className="pizza-customizer-page__back"
            href={routes.menu}
          >
            ← Back to Menu
          </Link>
          <PizzaCustomizer configuration={configuration} />
        </Container>
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
