import Image from "next/image";
import { publicAssetExists } from "@/lib/public-assets";

type HomeImageSlotProps = {
  alt: string;
  className?: string;
  imageClassName?: string;
  priority?: boolean;
  sizes: string;
  src: string;
};

export function HomeImageSlot({
  alt,
  className,
  imageClassName,
  priority = false,
  sizes,
  src,
}: HomeImageSlotProps) {
  const hasImage = publicAssetExists(src);

  return (
    <div
      className={[
        "home-image-slot",
        hasImage ? "home-image-slot--image" : "home-image-slot--placeholder",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {hasImage ? (
        <Image
          alt={alt}
          className={["home-image-slot__image", imageClassName]
            .filter(Boolean)
            .join(" ")}
          fill
          priority={priority}
          sizes={sizes}
          src={src}
        />
      ) : (
        <span className="sr-only">{alt} will be added here.</span>
      )}
    </div>
  );
}
