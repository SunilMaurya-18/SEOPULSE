import { Navigate, Route, Routes } from 'react-router-dom'

import { AppLayout } from '@/layouts/AppLayout'
import { ProtectedRoute } from '@/routes/ProtectedRoute'

import { LandingPage } from '@/pages/LandingPage'
import { TermsPage } from '@/pages/TermsPage'
import { BotPage } from '@/pages/BotPage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { WebsitesPage } from '@/pages/WebsitesPage'
import { AuditsPage } from '@/pages/AuditsPage'
import { AuditDetailPage } from '@/pages/AuditDetailPage'
import { AuditPagesPage } from '@/pages/AuditPagesPage'
import { SeoIssuesPage } from '@/pages/SeoIssuesPage'
import { IssuesPage } from '@/pages/IssuesPage'
import { PagesInventoryPage } from '@/pages/PagesInventoryPage'
import { SettingsPage } from '@/pages/SettingsPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/terms" element={<TermsPage />} />
      <Route path="/bot" element={<BotPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

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
  )
}
