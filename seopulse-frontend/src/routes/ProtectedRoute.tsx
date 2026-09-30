import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { PageSkeleton } from '@/components/ui/Skeleton'
import { useAuth } from '@/lib/auth'
import { WorkspaceProvider } from '@/lib/workspace'
import { OnboardingHandler } from '@/routes/OnboardingHandler'

export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return (
      <div className="min-h-screen bg-canvas p-6" aria-busy="true">
        <PageSkeleton />
      </div>
    )
  }

  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return (
    <WorkspaceProvider>
      <OnboardingHandler />
      <Outlet />
    </WorkspaceProvider>
  )
}
