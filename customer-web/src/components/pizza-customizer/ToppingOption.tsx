import { formatGbpPennies } from "@/lib/format-price";

type ToppingOptionProps = {
  name: string;
  onToggle: (name: string) => void;
  selected: boolean;
  toppingPricePennies: number;
};

export function ToppingOption({
  name,
  onToggle,
  selected,
  toppingPricePennies,
}: ToppingOptionProps) {
  const actionLabel = selected ? "Remove" : "Add";

  return (
    <button
      aria-label={`${actionLabel} ${name}`}
      aria-pressed={selected}
      className={[
        "topping-option",
        selected ? "topping-option--selected" : undefined,
      ]
        .filter(Boolean)
        .join(" ")}
      onClick={() => onToggle(name)}
      type="button"
    >
      <span className="topping-option__name">{name}</span>
      <span className="topping-option__state">
        {selected ? "Selected" : `+ ${formatGbpPennies(toppingPricePennies)}`}
      </span>
    </button>
  );
}
