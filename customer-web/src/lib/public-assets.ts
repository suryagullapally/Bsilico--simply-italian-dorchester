import { existsSync } from "node:fs";
import { join } from "node:path";

export function publicAssetExists(src: string) {
  const normalizedSrc = src.startsWith("/") ? src.slice(1) : src;

  return existsSync(join(process.cwd(), "public", normalizedSrc));
}
