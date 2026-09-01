import { mkdirSync, readdirSync, statSync } from "node:fs";
import { dirname, extname, join, relative } from "node:path";
import sharp from "sharp";

const publicDir = join(process.cwd(), "public");
const sourceExtensions = new Set([".jpg", ".jpeg", ".png"]);

const rules = [
  {
    test: (path) => path.startsWith("images/menu/"),
    maxLongEdge: 1200,
    quality: 82,
  },
  {
    test: (path) => path === "images/home/basilico-hero.png",
    maxLongEdge: 1600,
    quality: 82,
  },
  {
    test: (path) =>
      path.startsWith("images/home/") ||
      path.startsWith("images/restaurant/"),
    maxLongEdge: 1600,
    quality: 82,
  },
  {
    test: (path) => path.startsWith("images/location/"),
    maxLongEdge: 1200,
    quality: 86,
  },
  {
    test: (path) => path.startsWith("brand/"),
    maxLongEdge: 1024,
    quality: 90,
  },
];

function walk(dir) {
  const files = [];

  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const filePath = join(dir, entry.name);

    if (entry.isDirectory()) {
      files.push(...walk(filePath));
      continue;
    }

    if (sourceExtensions.has(extname(entry.name).toLowerCase())) {
      files.push(filePath);
    }
  }

  return files;
}

function ruleFor(relativePath) {
  return rules.find((rule) => rule.test(relativePath));
}

function resizeOptions(metadata, maxLongEdge) {
  const width = metadata.width ?? 0;
  const height = metadata.height ?? 0;

  if (width >= height) {
    return { width: maxLongEdge, withoutEnlargement: true };
  }

  return { height: maxLongEdge, withoutEnlargement: true };
}

const results = [];

for (const sourcePath of walk(publicDir)) {
  const relativePath = relative(publicDir, sourcePath);
  const rule = ruleFor(relativePath);

  if (!rule) {
    continue;
  }

  const parsedExtension = extname(sourcePath);
  const outputPath = sourcePath.slice(0, -parsedExtension.length) + ".webp";
  const before = statSync(sourcePath).size;
  const metadata = await sharp(sourcePath).metadata();

  mkdirSync(dirname(outputPath), { recursive: true });

  await sharp(sourcePath)
    .rotate()
    .resize(resizeOptions(metadata, rule.maxLongEdge))
    .webp({
      alphaQuality: 100,
      effort: 6,
      quality: rule.quality,
      smartSubsample: true,
    })
    .toFile(outputPath);

  const outputMetadata = await sharp(outputPath).metadata();
  const after = statSync(outputPath).size;

  results.push({
    source: relativePath,
    output: relative(publicDir, outputPath),
    before,
    after,
    beforeDimensions: `${metadata.width}x${metadata.height}`,
    afterDimensions: `${outputMetadata.width}x${outputMetadata.height}`,
  });
}

const beforeTotal = results.reduce((total, result) => total + result.before, 0);
const afterTotal = results.reduce((total, result) => total + result.after, 0);
const savedPercent = beforeTotal
  ? ((1 - afterTotal / beforeTotal) * 100).toFixed(1)
  : "0.0";

for (const result of results) {
  const beforeKb = (result.before / 1024).toFixed(1);
  const afterKb = (result.after / 1024).toFixed(1);

  console.log(
    `${result.source} -> ${result.output} ${result.beforeDimensions} -> ${result.afterDimensions} ${beforeKb} KB -> ${afterKb} KB`,
  );
}

console.log(
  `Optimized ${results.length} images: ${(beforeTotal / 1024 / 1024).toFixed(2)} MB -> ${(afterTotal / 1024 / 1024).toFixed(2)} MB (${savedPercent}% smaller)`,
);
