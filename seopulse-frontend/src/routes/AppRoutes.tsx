import { lazy, Suspense, type ComponentType } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'

import { ErrorBoundary } from '@/components/ErrorBoundary'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { AppLayout } from '@/layouts/AppLayout'
import { ProtectedRoute } from '@/routes/ProtectedRoute'

function lazyPage<K extends string>(
  load: () => Promise<Record<K, ComponentType>>,
  name: K,
) {
  return lazy(async () => ({ default: (await load())[name] }))
}

const LandingPage = lazyPage(() => import('@/pages/LandingPage'), 'LandingPage')
const TermsPage = lazyPage(() => import('@/pages/TermsPage'), 'TermsPage')
const BotPage = lazyPage(() => import('@/pages/BotPage'), 'BotPage')
const PricingPage = lazyPage(() => import('@/pages/PricingPage'), 'PricingPage')
const LoginPage = lazyPage(() => import('@/pages/LoginPage'), 'LoginPage')
const RegisterPage = lazyPage(() => import('@/pages/RegisterPage'), 'RegisterPage')
const VerifyEmailPage = lazyPage(() => import('@/pages/VerifyEmailPage'), 'VerifyEmailPage')
const ForgotPasswordPage = lazyPage(
  () => import('@/pages/ForgotPasswordPage'),
  'ForgotPasswordPage',
)
const ResetPasswordPage = lazyPage(() => import('@/pages/ResetPasswordPage'), 'ResetPasswordPage')
const DashboardPage = lazyPage(() => import('@/pages/DashboardPage'), 'DashboardPage')
const WebsitesPage = lazyPage(() => import('@/pages/WebsitesPage'), 'WebsitesPage')
const AuditsPage = lazyPage(() => import('@/pages/AuditsPage'), 'AuditsPage')
const AuditDetailPage = lazyPage(() => import('@/pages/AuditDetailPage'), 'AuditDetailPage')
const AuditPagesPage = lazyPage(() => import('@/pages/AuditPagesPage'), 'AuditPagesPage')
const SeoIssuesPage = lazyPage(() => import('@/pages/SeoIssuesPage'), 'SeoIssuesPage')
const IssuesPage = lazyPage(() => import('@/pages/IssuesPage'), 'IssuesPage')
const PagesInventoryPage = lazyPage(
  () => import('@/pages/PagesInventoryPage'),
  'PagesInventoryPage',
)
const SettingsPage = lazyPage(() => import('@/pages/SettingsPage'), 'SettingsPage')

function FullPageFallback() {
  return (
    <div className="min-h-screen bg-canvas p-6">
      <PageSkeleton />
    </div>
  )
}

export function AppRoutes() {
  const location = useLocation()

  return (
    <ErrorBoundary resetKey={location.pathname} fullScreen>
      <Suspense fallback={<FullPageFallback />}>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/terms" element={<TermsPage />} />
          <Route path="/bot" element={<BotPage />} />
          <Route path="/pricing" element={<PricingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/websites" element={<WebsitesPage />} />
              <Route path="/audits" element={<AuditsPage />} />
              <Route path="/audits/:auditId" element={<AuditDetailPage />} />
              <Route path="/audits/:auditId/pages" element={<AuditPagesPage />} />
              <Route path="/audits/:auditId/issues" element={<SeoIssuesPage />} />
              <Route path="/issues" element={<IssuesPage />} />
              <Route path="/pages" element={<PagesInventoryPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/analyzer" element={<Navigate to="/pages" replace />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </ErrorBoundary>
  )
}
