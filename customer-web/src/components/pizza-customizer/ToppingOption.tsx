import { formatGbpPennies } from "@/lib/format-price";
import type { CreateYourOwnToppingOption } from "@/types/menu";

type ToppingOptionProps = {
  onToggle: (topping: CreateYourOwnToppingOption) => void;
  selected: boolean;
  topping: CreateYourOwnToppingOption;
  toppingPricePennies: number;
};

export function ToppingOption({
  onToggle,
  selected,
  topping,
  toppingPricePennies,
}: ToppingOptionProps) {
  const actionLabel = selected ? "Remove" : "Add";

  return (
    <button
      aria-label={`${actionLabel} ${topping.name}`}
      aria-pressed={selected}
      className={[
        "topping-option",
        selected ? "topping-option--selected" : undefined,
      ]
        .filter(Boolean)
        .join(" ")}
      onClick={() => onToggle(topping)}
      type="button"
    >
      <span className="topping-option__name">{topping.name}</span>
      <span className="topping-option__state">
        {selected ? "Selected" : `+ ${formatGbpPennies(toppingPricePennies)}`}
      </span>
    </button>
  );
}
