import { useEffect } from 'react'

import { FinalCtaSection } from '@/features/landing/components/FinalCtaSection'
import { HeroSection } from '@/features/landing/components/HeroSection'
import { MarketingFooter } from '@/features/landing/components/MarketingFooter'
import { MarketingHeader } from '@/features/landing/components/MarketingHeader'
import { SolutionsSection } from '@/features/landing/components/SolutionsSection'
import { StatsSection } from '@/features/landing/components/StatsSection'
import { VisionSection } from '@/features/landing/components/VisionSection'
import { smoothScrollTo } from '@/features/landing/useInView'

export function LandingPage() {
  useEffect(() => {
    const id = window.location.hash.replace('#', '')
    if (id) smoothScrollTo(id)
  }, [])

  return (
    <div className="marketing dark min-h-screen bg-[#05070a] text-base text-[#f2f5ea] antialiased">
      <MarketingHeader />
      <main>
        <HeroSection />
        <SolutionsSection />
        <VisionSection />
        <StatsSection />
        <FinalCtaSection />
      </main>
      <MarketingFooter />
    </div>
  )
}
