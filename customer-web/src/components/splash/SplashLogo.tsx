import { BrandLogo } from "@/components/brand/BrandLogo";

type SplashLogoProps = {
  className?: string;
};

export function SplashLogo({ className }: SplashLogoProps) {
  return (
    <div className={["splash-logo", className].filter(Boolean).join(" ")}>
      <BrandLogo priority />
    </div>
  );
}
