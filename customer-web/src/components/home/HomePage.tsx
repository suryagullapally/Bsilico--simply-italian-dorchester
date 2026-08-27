import { AtmosphereSection } from "@/components/home/AtmosphereSection";
import { BasilicoStory } from "@/components/home/BasilicoStory";
import { ExperienceSection } from "@/components/home/ExperienceSection";
import { FinalCTA } from "@/components/home/FinalCTA";
import { HeroSection } from "@/components/home/HeroSection";
import { HomeFooter } from "@/components/home/HomeFooter";
import { HomeHeader } from "@/components/home/HomeHeader";
import { MobileConversionBar } from "@/components/home/MobileConversionBar";
import { SignatureDishes } from "@/components/home/SignatureDishes";
import { VisitSection } from "@/components/home/VisitSection";

export function HomePage() {
  return (
    <>
      <HomeHeader />
      <main className="home-page">
        <HeroSection />
        <SignatureDishes />
        <BasilicoStory />
        <ExperienceSection />
        <AtmosphereSection />
        <VisitSection />
        <FinalCTA />
      </main>
      <HomeFooter />
      <MobileConversionBar />
    </>
  );
}
