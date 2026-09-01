"use client";

import Image from "next/image";
import { useState } from "react";

type HeroVisualProps = {
  src: string;
};

export function HeroVisual({ src }: HeroVisualProps) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const hasImage = src.trim().length > 0 && failedSrc !== src;

  return (
    <div className="home-hero__visual">
      {hasImage ? (
        <Image
          alt="Basilico Italian food and wood-fired cooking"
          className="home-hero__image"
          fill
          priority
          sizes="(max-width: 1023px) 92vw, 48vw"
          src={src}
          onError={() => setFailedSrc(src)}
        />
      ) : (
        <div className="home-hero__oven-fallback" aria-hidden="true" />
      )}
    </div>
  );
}
