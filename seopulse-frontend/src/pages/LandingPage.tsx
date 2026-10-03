import { lazy, Suspense, useEffect } from 'react'

import { FinalCtaSection } from '@/features/landing/components/FinalCtaSection'
import { HeroSection } from '@/features/landing/components/HeroSection'
import { MarketingFooter } from '@/features/landing/components/MarketingFooter'
import { MarketingHeader } from '@/features/landing/components/MarketingHeader'
import { SolutionsSection } from '@/features/landing/components/SolutionsSection'
import { VisionSection } from '@/features/landing/components/VisionSection'
import { smoothScrollTo } from '@/features/landing/useInView'

const GlobalReachSection = lazy(() => import('@/features/landing/components/GlobalReachSection'))

export function LandingPage() {
  useEffect(() => {
    const id = window.location.hash.replace('#', '')
    if (id) smoothScrollTo(id)
  }, [])

  return (
    <div className="marketing dark min-h-screen bg-canvas text-base text-main antialiased">
      <MarketingHeader />
      <main>
        <HeroSection />
        <SolutionsSection />
        <VisionSection />
        <Suspense fallback={<div className="min-h-[790px] bg-black" aria-hidden />}>
          <GlobalReachSection />
        </Suspense>
        <FinalCtaSection />
      </main>
      <MarketingFooter />
    </div>
  )
}
