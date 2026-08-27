import type { DietaryTag } from "@/types/menu";

type MenuDietaryTagsProps = {
  className?: string;
  dietaryLabels: Record<DietaryTag, string>;
  tags: DietaryTag[];
};

export function MenuDietaryTags({
  className,
  dietaryLabels,
  tags,
}: MenuDietaryTagsProps) {
  if (tags.length === 0) {
    return null;
  }

  return (
    <div
      className={["menu-item__tags", className].filter(Boolean).join(" ")}
      aria-label="Dietary markers"
    >
      {tags.map((tag) => (
        <abbr
          className="menu-item__tag"
          key={tag}
          title={dietaryLabels[tag]}
          aria-label={`${tag}: ${dietaryLabels[tag]}`}
        >
          {tag}
        </abbr>
      ))}
    </div>
  );
}
