import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { authResponse, page } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'
import { api, server } from '@/test/server'

function renderApp() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<p>Login screen</p>} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<p>Private dashboard</p>} />
      </Route>
    </Routes>,
    { route: '/dashboard' },
  )
}

describe('ProtectedRoute', () => {
  it('redirects to login when the refresh cookie is missing or invalid', async () => {
    server.use(http.post(api('/auth/refresh'), () => new HttpResponse(null, { status: 401 })))

    renderApp()

    expect(await screen.findByText('Login screen')).toBeInTheDocument()
    expect(screen.queryByText('Private dashboard')).not.toBeInTheDocument()
  })

  it('restores the session from the refresh cookie and renders the page', async () => {
    server.use(
      http.post(api('/auth/refresh'), () => HttpResponse.json(authResponse())),
      http.get(api('/projects'), ({ request }) =>
        request.headers.get('Authorization') === 'Bearer access-token'
          ? HttpResponse.json(page([{ id: 1, name: 'Workspace' }]))
          : new HttpResponse(null, { status: 401 }),
      ),
    )

    renderApp()

    expect(await screen.findByText('Private dashboard')).toBeInTheDocument()
  })

  it('waits for the session bootstrap instead of redirecting immediately', () => {
    server.use(
      http.post(api('/auth/refresh'), async () => {
        await new Promise(() => {})
        return HttpResponse.json(authResponse())
      }),
    )

    const { container } = renderApp()

    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull()
    expect(screen.queryByText('Login screen')).not.toBeInTheDocument()
  })
})
