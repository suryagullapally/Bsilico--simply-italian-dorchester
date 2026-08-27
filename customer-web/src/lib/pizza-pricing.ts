import type { CreateYourOwnConfiguration } from "@/types/menu";
import type { PizzaPricing } from "@/types/pizza-customizer";

export function getCreateYourOwnPricing(
  configuration: CreateYourOwnConfiguration,
  selectedToppingCount: number,
): PizzaPricing {
  const basePricePennies = configuration.basePricePence;
  const toppingPricePennies = configuration.extraToppingPricePence;
  const extrasPricePennies = selectedToppingCount * toppingPricePennies;

  return {
    basePricePennies,
    extrasPricePennies,
    selectedToppingCount,
    toppingPricePennies,
    totalPricePennies: basePricePennies + extrasPricePennies,
  };
}
