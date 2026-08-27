"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { SplashLogo } from "@/components/splash/SplashLogo";

type SplashMode = "first" | "returning" | "reduced";

const FIRE_VIDEO_SRC = "/video/basilico-fire.mp4";
const SPLASH_SEEN_KEY = "basilico:splash-seen";

const SPLASH_TIMINGS: Record<
  SplashMode,
  { exitDurationMs: number; skipDelayMs: number; visibleDurationMs: number }
> = {
  first: {
    visibleDurationMs: 2200,
    exitDurationMs: 380,
    skipDelayMs: 620,
  },
  returning: {
    visibleDurationMs: 520,
    exitDurationMs: 210,
    skipDelayMs: 140,
  },
  reduced: {
    visibleDurationMs: 260,
    exitDurationMs: 180,
    skipDelayMs: 120,
  },
};

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function readSplashSeen() {
  try {
    return window.sessionStorage.getItem(SPLASH_SEEN_KEY) === "true";
  } catch {
    return false;
  }
}

function markSplashSeen() {
  try {
    window.sessionStorage.setItem(SPLASH_SEEN_KEY, "true");
  } catch {
    // Storage can be unavailable in hardened browser modes; the splash still works.
  }
}

export function SplashScreen() {
  const [isFireVideoUnavailable, setIsFireVideoUnavailable] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const [isVisible, setIsVisible] = useState(true);
  const [mode, setMode] = useState<SplashMode | "pending">("pending");
  const [showSkip, setShowSkip] = useState(false);
  const isFinishingRef = useRef(false);
  const timerRefs = useRef<number[]>([]);

  const clearTimers = useCallback(() => {
    for (const timer of timerRefs.current) {
      window.clearTimeout(timer);
    }

    timerRefs.current = [];
  }, []);

  const finishSplash = useCallback(
    (exitDurationMs: number) => {
      if (isFinishingRef.current) {
        return;
      }

      isFinishingRef.current = true;
      clearTimers();
      markSplashSeen();
      setShowSkip(false);
      setIsExiting(true);

      timerRefs.current.push(
        window.setTimeout(() => {
          setIsVisible(false);
        }, exitDurationMs),
      );
    },
    [clearTimers],
  );

  useEffect(() => {
    timerRefs.current.push(
      window.setTimeout(() => {
        const nextMode: SplashMode = prefersReducedMotion()
          ? "reduced"
          : readSplashSeen()
            ? "returning"
            : "first";
        const timing = SPLASH_TIMINGS[nextMode];

        setMode(nextMode);
        markSplashSeen();

        timerRefs.current.push(
          window.setTimeout(() => {
            setShowSkip(true);
          }, timing.skipDelayMs),
        );

        timerRefs.current.push(
          window.setTimeout(() => {
            finishSplash(timing.exitDurationMs);
          }, timing.visibleDurationMs),
        );
      }, 0),
    );

    return () => {
      clearTimers();
    };
  }, [clearTimers, finishSplash]);

  useEffect(() => {
    if (!isVisible) {
      return;
    }

    const { body, documentElement } = document;
    const siteContent = document.getElementById("site-content");
    const previousBodyOverflow = body.style.overflow;
    const previousBodyPaddingRight = body.style.paddingRight;
    const previousHtmlOverflow = documentElement.style.overflow;
    const hadInert = siteContent?.hasAttribute("inert") ?? false;
    const scrollbarWidth = window.innerWidth - documentElement.clientWidth;

    body.style.overflow = "hidden";
    documentElement.style.overflow = "hidden";

    if (scrollbarWidth > 0) {
      body.style.paddingRight = `${scrollbarWidth}px`;
    }

    siteContent?.setAttribute("inert", "");

    return () => {
      body.style.overflow = previousBodyOverflow;
      body.style.paddingRight = previousBodyPaddingRight;
      documentElement.style.overflow = previousHtmlOverflow;

      if (!hadInert) {
        siteContent?.removeAttribute("inert");
      }
    };
  }, [isVisible]);

  if (!isVisible) {
    return null;
  }

  return (
    <div
      aria-label="Basilico introduction"
      className="splash-screen"
      data-mode={mode}
      data-state={isExiting ? "exiting" : "entering"}
    >
      <div className="splash-screen__base" aria-hidden="true" />
      <div className="splash-screen__ember" aria-hidden="true" />

      {mode !== "pending" && mode !== "reduced" && !isFireVideoUnavailable ? (
        <video
          aria-hidden="true"
          autoPlay
          className="splash-screen__fire"
          loop
          muted
          playsInline
          preload="metadata"
          src={FIRE_VIDEO_SRC}
          tabIndex={-1}
          onError={() => setIsFireVideoUnavailable(true)}
        />
      ) : null}

      <div className="splash-screen__haze" aria-hidden="true" />
      <div className="splash-screen__illumination" aria-hidden="true" />
      <div className="splash-screen__vignette" aria-hidden="true" />

      <SplashLogo />

      <button
        aria-label="Skip Basilico introduction"
        className={[
          "splash-screen__skip",
          "type-navigation",
          showSkip && !isExiting ? "is-visible" : "",
        ]
          .filter(Boolean)
          .join(" ")}
        disabled={!showSkip || isExiting}
        onClick={() => finishSplash(220)}
        type="button"
      >
        Skip
      </button>
    </div>
  );
}
