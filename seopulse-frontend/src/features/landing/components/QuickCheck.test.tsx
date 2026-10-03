import { http, HttpResponse } from 'msw'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { QuickCheckResult } from '@/api/quickCheck'
import { QuickCheck } from '@/features/landing/components/QuickCheck'
import { renderWithProviders } from '@/test/render'
import { api, server } from '@/test/server'

const result: QuickCheckResult = {
  url: 'https://www.example.com/',
  score: 72,
  categoryScores: { CONTENT: 60, TECHNICAL: 90 },
  pagesChecked: 3,
  partial: false,
  errorCount: 1,
  warningCount: 2,
  infoCount: 0,
  issueTypes: 4,
  issues: [
    {
      ruleCode: 'META_DESCRIPTION_MISSING',
      title: 'Missing meta description',
      severity: 'ERROR',
      category: 'CONTENT',
      recommendation: 'Add a unique meta description.',
      helpUrl: null,
      pages: 2,
    },
  ],
}

describe('QuickCheck', () => {
  it('shows the score and top issues, then hands the URL to sign-up', async () => {
    let requested: unknown
    server.use(
      http.post(api('/public/quick-check'), async ({ request }) => {
        requested = await request.json()
        return HttpResponse.json(result)
      }),
    )
    renderWithProviders(<QuickCheck />, { withAuth: false })

    await userEvent.type(screen.getByLabelText('Website address'), 'example.com')
    await userEvent.click(screen.getByRole('button', { name: 'Check my site free' }))

    expect(await screen.findByRole('heading', { name: 'example.com' })).toBeInTheDocument()
    expect(requested).toEqual({ url: 'example.com' })
    expect(screen.getByText('Missing meta description')).toBeInTheDocument()
    expect(screen.getByText(/3 more issue types found/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Get the full report/ })).toHaveAttribute(
      'href',
      `/register?url=${encodeURIComponent('https://www.example.com/')}`,
    )
  })

  it('explains why a site could not be checked', async () => {
    server.use(
      http.post(api('/public/quick-check'), () =>
        HttpResponse.json(
          { status: 400, detail: 'The homepage returned HTTP 404, so there is nothing to check yet.' },
          { status: 400 },
        ),
      ),
    )
    renderWithProviders(<QuickCheck />, { withAuth: false })

    await userEvent.type(screen.getByLabelText('Website address'), 'example.com/missing')
    await userEvent.click(screen.getByRole('button', { name: 'Check my site free' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('HTTP 404')
  })
})
