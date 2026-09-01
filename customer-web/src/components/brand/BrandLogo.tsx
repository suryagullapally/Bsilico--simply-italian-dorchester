import Image from "next/image";

type BrandLogoProps = {
  alt?: string;
  className?: string;
  priority?: boolean;
  sizes?: string;
};

export function BrandLogo({
  alt = "Basilico Simple Italian restaurant logo",
  className,
  priority = false,
  sizes = "(max-width: 640px) 84vw, (max-width: 1024px) 34rem, 40rem",
}: BrandLogoProps) {
  return (
    <Image
      src="/brand/basilico-logo.webp"
      alt={alt}
      width={1024}
      height={683}
      priority={priority}
      sizes={sizes}
      className={["brand-logo", className].filter(Boolean).join(" ")}
    />
  );
}
