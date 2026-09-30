import { Outlet, useLocation } from 'react-router-dom'
import { Suspense } from 'react'

import { ErrorBoundary } from '@/components/ErrorBoundary'
import { VerifyEmailBanner } from '@/components/auth/VerifyEmailBanner'
import { Sidebar } from '@/components/layout/Sidebar'
import { Topbar } from '@/components/layout/Topbar'
import { PageSkeleton } from '@/components/ui/Skeleton'

export function AppLayout() {
  const location = useLocation()

  return (
    <div className="app-canvas flex h-dvh overflow-hidden">
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <Topbar />
        <VerifyEmailBanner />
        <main className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
          <div
            key={location.pathname}
            className="animate-page-enter mx-auto w-full max-w-[1440px] px-3 py-5 sm:px-5 lg:px-6 lg:py-6"
          >
            <ErrorBoundary resetKey={location.pathname}>
              <Suspense fallback={<PageSkeleton />}>
                <Outlet />
              </Suspense>
            </ErrorBoundary>
          </div>
        </main>
      </div>
    </div>
  )
}
