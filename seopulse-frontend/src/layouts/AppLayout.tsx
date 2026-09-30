import { Outlet, useLocation } from 'react-router-dom'
import { Suspense, useEffect } from 'react'

import { ErrorBoundary } from '@/components/ErrorBoundary'
import { VerifyEmailBanner } from '@/components/auth/VerifyEmailBanner'
import { UpgradeModal } from '@/components/billing/UpgradeModal'
import { Sidebar } from '@/components/layout/Sidebar'
import { Topbar } from '@/components/layout/Topbar'
import { PageSkeleton } from '@/components/ui/Skeleton'

export function AppLayout() {
  const location = useLocation()

  useEffect(() => {
    document.body.classList.add('app-shell')
    return () => document.body.classList.remove('app-shell')
  }, [])

  return (
    <div className="app-canvas flex h-dvh overflow-hidden">
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
          <VerifyEmailBanner />
          <div
            key={location.pathname}
            className="animate-page-enter mx-auto w-full max-w-[1320px] px-4 py-6 sm:px-6 lg:px-10 lg:py-9"
          >
            <ErrorBoundary resetKey={location.pathname}>
              <Suspense fallback={<PageSkeleton />}>
                <Outlet />
              </Suspense>
            </ErrorBoundary>
          </div>
        </main>
      </div>
      <UpgradeModal />
    </div>
  )
}
