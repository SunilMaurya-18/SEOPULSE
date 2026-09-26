import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuth } from '@/lib/auth'
import { WorkspaceProvider } from '@/lib/workspace'
import { OnboardingHandler } from '@/routes/OnboardingHandler'

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return (
    <WorkspaceProvider>
      <OnboardingHandler />
      <Outlet />
    </WorkspaceProvider>
  )
}
