"use client";

import { useLayoutEffect } from "react";

export function MenuScrollReset() {
  useLayoutEffect(() => {
    const canControlScrollRestoration = "scrollRestoration" in window.history;
    const previousScrollRestoration = canControlScrollRestoration
      ? window.history.scrollRestoration
      : undefined;
    const resetScroll = () => {
      if (window.location.hash) {
        return;
      }

      window.scrollTo({
        behavior: "instant" as ScrollBehavior,
        left: 0,
        top: 0,
      });
    };

    if (canControlScrollRestoration) {
      window.history.scrollRestoration = "manual";
    }

    resetScroll();

    const animationFrame = window.requestAnimationFrame(resetScroll);
    const timeout = window.setTimeout(resetScroll, 120);

    return () => {
      window.cancelAnimationFrame(animationFrame);
      window.clearTimeout(timeout);

      if (
        canControlScrollRestoration &&
        previousScrollRestoration !== undefined
      ) {
        window.history.scrollRestoration = previousScrollRestoration;
      }
    };
  }, []);

  return null;
}
