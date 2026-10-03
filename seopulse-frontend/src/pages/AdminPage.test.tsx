import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { AdminPage } from '@/pages/AdminPage'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { authResponse, page } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'
import { api, server } from '@/test/server'

const stats = {
  users: 42,
  usersLast7Days: 5,
  usersLast30Days: 12,
  verifiedUsers: 30,
  organizations: 40,
  websites: 77,
  audits: 310,
  auditsLast24Hours: 9,
  failedAuditsLast24Hours: 2,
  activeAudits: 1,
  newsletterSubscribers: 18,
  workspacesByPlan: { FREE: 35, PRO: 5 },
}

function renderAdmin(role: string) {
  server.use(
    http.post(api('/auth/refresh'), () => HttpResponse.json(authResponse({ role }))),
    http.get(api('/projects'), () => HttpResponse.json(page([{ id: 1, name: 'Workspace' }]))),
    http.get(api('/admin/stats'), () => HttpResponse.json(stats)),
    http.get(api('/admin/users'), () =>
      HttpResponse.json(
        page([
          {
            id: 9,
            name: 'Locked Out',
            email: 'locked@example.com',
            role: 'USER',
            emailVerified: true,
            googleLinked: false,
            locked: true,
            workspaces: 1,
            createdAt: '2026-10-01T00:00:00Z',
          },
        ]),
      ),
    ),
  )
  return renderWithProviders(
    <Routes>
      <Route element={<ProtectedRoute />}>
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/dashboard" element={<p>Dashboard</p>} />
      </Route>
    </Routes>,
    { route: '/admin' },
  )
}

describe('AdminPage', () => {
  it('shows platform stats and lets admins unlock accounts', async () => {
    let unlocked: string | null = null
    server.use(
      http.post(api('/admin/users/:id/unlock'), ({ params }) => {
        unlocked = String(params.id)
        return new HttpResponse(null, { status: 204 })
      }),
    )

    renderAdmin('ADMIN')

    expect(await screen.findByText('42')).toBeInTheDocument()
    expect(screen.getByText('35 free · 5 pro')).toBeInTheDocument()
    expect(await screen.findByText('locked@example.com')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Unlock' }))
    await screen.findByText('Account unlocked')
    expect(unlocked).toBe('9')
  })

  it('sends everyone else back to the dashboard', async () => {
    renderAdmin('USER')

    expect(await screen.findByText('Dashboard')).toBeInTheDocument()
  })
})
