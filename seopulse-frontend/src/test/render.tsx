import type { ReactElement, ReactNode } from 'react'
import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, useLocation } from 'react-router-dom'

import { AuthProvider } from '@/lib/auth'
import { ToastProvider } from '@/lib/toast'

export function renderWithProviders(
  ui: ReactElement,
  { route = '/', withAuth = true }: { route?: string; withAuth?: boolean } = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  const inner: ReactNode = (
    <ToastProvider>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </ToastProvider>
  )

  return render(
    <QueryClientProvider client={queryClient}>
      {withAuth ? <AuthProvider>{inner}</AuthProvider> : inner}
    </QueryClientProvider>,
  )
}

/** Renders the current location so tests can assert on navigation. */
export function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location">{`${location.pathname}${location.search}`}</div>
}
