import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'

import { setPendingWebsiteUrl, peekPendingWebsiteUrl } from '@/lib/pendingWebsite'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { audit, authResponse, page } from '@/test/fixtures'
import { LocationProbe, renderWithProviders } from '@/test/render'
import { api, server } from '@/test/server'

function renderWorkspace() {
  return renderWithProviders(
    <>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="*" element={<p>Workspace</p>} />
        </Route>
      </Routes>
      <LocationProbe />
    </>,
    { route: '/dashboard' },
  )
}

describe('OnboardingHandler', () => {
  let createdWebsite: unknown

  beforeEach(() => {
    createdWebsite = null
    server.use(
      http.post(api('/auth/refresh'), () => HttpResponse.json(authResponse())),
      http.get(api('/projects'), () => HttpResponse.json(page([{ id: 1, name: 'Workspace' }]))),
      http.post(api('/projects/1/websites'), async ({ request }) => {
        createdWebsite = await request.json()
        return HttpResponse.json({ id: 3, name: 'example.com', url: 'https://example.com' })
      }),
    )
  })

  it('connects the pending website, starts an audit and opens the report', async () => {
    server.use(
      http.post(api('/projects/1/audits'), ({ request }) => {
        expect(new URL(request.url).searchParams.get('websiteId')).toBe('3')
        return HttpResponse.json(audit({ id: 42 }), { status: 201 })
      }),
    )
    setPendingWebsiteUrl('example.com')

    renderWorkspace()

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/audits/42'))
    expect(createdWebsite).toEqual({ name: 'example.com', url: 'https://example.com' })
    expect(peekPendingWebsiteUrl()).toBeNull()
  })

  it('keeps the website but explains that audits need a verified email', async () => {
    server.use(
      http.post(api('/projects/1/audits'), () =>
        HttpResponse.json(
          { status: 403, detail: 'Verify your email first', code: 'EMAIL_NOT_VERIFIED' },
          { status: 403 },
        ),
      ),
    )
    setPendingWebsiteUrl('https://example.com')

    renderWorkspace()

    await waitFor(() =>
      expect(screen.getByTestId('location')).toHaveTextContent('/audits?websiteId=3'),
    )
    expect(await screen.findByText('Verify your email to run your first audit.')).toBeInTheDocument()
  })

  it('does nothing without a pending website', async () => {
    renderWorkspace()

    expect(await screen.findByText('Workspace')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/dashboard')
    expect(createdWebsite).toBeNull()
  })
})
