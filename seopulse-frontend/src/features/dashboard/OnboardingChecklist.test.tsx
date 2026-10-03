import { http, HttpResponse } from 'msw'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { OnboardingStatus } from '@/api/onboarding'
import { OnboardingChecklist } from '@/features/dashboard/OnboardingChecklist'
import { renderWithProviders } from '@/test/render'
import { api, server } from '@/test/server'

const status: OnboardingStatus = {
  dismissed: false,
  steps: [
    { id: 'VERIFY_EMAIL', done: true },
    { id: 'ADD_WEBSITE', done: true },
    { id: 'RUN_AUDIT', done: false },
    { id: 'SHARE_REPORT', done: false },
    { id: 'INVITE_TEAM', done: false },
  ],
}

function renderChecklist() {
  return renderWithProviders(
    <OnboardingChecklist
      projectId={1}
      status={status}
      firstWebsiteId={7}
      activeAuditId={null}
      latestCompletedAuditId={null}
    />,
  )
}

describe('OnboardingChecklist', () => {
  it('shows progress and points to the next step', () => {
    renderChecklist()

    expect(screen.getByText('2 of 5 done')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Start audit/ })).toHaveAttribute('href', '/audits?websiteId=7')
    expect(screen.getByRole('link', { name: /Invite/ })).toHaveAttribute('href', '/settings#team')
    expect(screen.queryByRole('link', { name: /Open report/ })).not.toBeInTheDocument()
  })

  it('remembers when the checklist is hidden', async () => {
    let dismissed = false
    server.use(
      http.post(api('/onboarding/dismiss'), () => {
        dismissed = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderChecklist()

    await userEvent.click(screen.getByRole('button', { name: 'Hide getting started checklist' }))

    await waitFor(() => expect(dismissed).toBe(true))
  })
})
