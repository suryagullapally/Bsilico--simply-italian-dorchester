import type { DietaryLegendItem } from "@/types/menu";

type MenuDietaryLegendProps = {
  items: DietaryLegendItem[];
};

export function MenuDietaryLegend({ items }: MenuDietaryLegendProps) {
  return (
    <section className="menu-dietary" aria-labelledby="menu-dietary-title">
      <div className="site-container menu-dietary__container">
        <div className="menu-dietary__intro">
          <h2 className="type-eyebrow menu-dietary__title" id="menu-dietary-title">
            Dietary guide
          </h2>
          <p className="type-small menu-dietary__allergy">
            Please tell us about any allergies or dietary requirements before
            ordering.
          </p>
        </div>

        <dl className="menu-dietary__legend">
          {items.map((item) => (
            <div className="menu-dietary__legend-item" key={item.tag}>
              <dt>{item.tag}</dt>
              <dd>{item.description}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
