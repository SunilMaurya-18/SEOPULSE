import { CapabilitiesSection } from '@/features/landing/components/CapabilitiesSection'
import { FinalCtaSection } from '@/features/landing/components/FinalCtaSection'
import { HeroSection } from '@/features/landing/components/HeroSection'
import { HowItWorksSection } from '@/features/landing/components/HowItWorksSection'
import { MarketingFooter } from '@/features/landing/components/MarketingFooter'
import { MarketingHeader } from '@/features/landing/components/MarketingHeader'

export function LandingPage() {
  return (
    <div className="min-h-screen bg-canvas font-sans text-sm text-main antialiased">
      <MarketingHeader />
      <main className="w-full pt-14">
        <HeroSection />
        <HowItWorksSection />
        <CapabilitiesSection />
        <FinalCtaSection />
      </main>
      <MarketingFooter />
    </div>
  )
}
