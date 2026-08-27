"use client";

import { useEffect, useState } from "react";
import type { MenuCategory } from "@/types/menu";

type MenuCategoryNavProps = {
  categories: MenuCategory[];
};

export function MenuCategoryNav({ categories }: MenuCategoryNavProps) {
  const [activeCategoryId, setActiveCategoryId] = useState(
    categories[0]?.id ?? "",
  );

  useEffect(() => {
    let animationFrame = 0;

    function updateActiveCategory() {
      animationFrame = 0;

      if (categories.length === 0) {
        return;
      }

      const scrollBottom = window.scrollY + window.innerHeight;
      const documentHeight = document.documentElement.scrollHeight;

      if (scrollBottom >= documentHeight - 4) {
        setActiveCategoryId(categories[categories.length - 1].id);
        return;
      }

      const offset = getMenuScrollOffset();
      let currentCategoryId = categories[0].id;

      for (const category of categories) {
        const section = document.getElementById(category.id);

        if (!section) {
          continue;
        }

        if (section.getBoundingClientRect().top <= offset + 24) {
          currentCategoryId = category.id;
        } else {
          break;
        }
      }

      setActiveCategoryId((previousCategoryId) =>
        previousCategoryId === currentCategoryId
          ? previousCategoryId
          : currentCategoryId,
      );
    }

    function requestActiveCategoryUpdate() {
      if (animationFrame === 0) {
        animationFrame = window.requestAnimationFrame(updateActiveCategory);
      }
    }

    requestActiveCategoryUpdate();
    window.addEventListener("scroll", requestActiveCategoryUpdate, {
      passive: true,
    });
    window.addEventListener("resize", requestActiveCategoryUpdate);

    return () => {
      if (animationFrame !== 0) {
        window.cancelAnimationFrame(animationFrame);
      }

      window.removeEventListener("scroll", requestActiveCategoryUpdate);
      window.removeEventListener("resize", requestActiveCategoryUpdate);
    };
  }, [categories]);

  function handleCategoryClick(categoryId: MenuCategory["id"]) {
    const section = document.getElementById(categoryId);

    setActiveCategoryId(categoryId);

    if (!section) {
      return;
    }

    const top =
      section.getBoundingClientRect().top + window.scrollY - getMenuScrollOffset();
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;

    window.scrollTo({
      behavior: prefersReducedMotion ? "auto" : "smooth",
      top: Math.max(top, 0),
    });
  }

  return (
    <nav className="menu-category-nav" aria-label="Menu categories">
      <div className="site-container menu-category-nav__container">
        <div className="menu-category-nav__rail">
          {categories.map((category) => {
            const isActive = activeCategoryId === category.id;

            return (
              <button
                aria-current={isActive ? "true" : undefined}
                className={[
                  "menu-category-nav__link",
                  isActive ? "menu-category-nav__link--active" : undefined,
                ]
                  .filter(Boolean)
                  .join(" ")}
                key={category.id}
                onClick={() => handleCategoryClick(category.id)}
                type="button"
              >
                {category.navLabel}
              </button>
            );
          })}
        </div>
      </div>
    </nav>
  );
}

function getMenuScrollOffset() {
  const header = document.querySelector<HTMLElement>(".home-header");
  const categoryNav = document.querySelector<HTMLElement>(".menu-category-nav");

  return (header?.offsetHeight ?? 72) + (categoryNav?.offsetHeight ?? 54) + 16;
}
