import { http, HttpResponse } from 'msw'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { websiteApi } from '@/api/websites'
import { AddWebsiteModal } from '@/features/websites/components/AddWebsiteModal'
import { api, server } from '@/test/server'

describe('AddWebsiteModal', () => {
  it('submits the URL with a name derived from the hostname and closes', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const onClose = vi.fn()
    render(<AddWebsiteModal open onClose={onClose} onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Website URL'), 'https://www.example.com/blog')
    await user.click(screen.getByRole('button', { name: 'Add website' }))

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'example.com',
      url: 'https://www.example.com/blog',
    })
    expect(onClose).toHaveBeenCalled()
  })

  it('shows the field error from a problem response and stays open', async () => {
    server.use(
      http.post(api('/projects/1/websites'), () =>
        HttpResponse.json(
          {
            status: 400,
            detail: 'Validation failed',
            errors: { url: 'URL must point to a public website' },
          },
          { status: 400 },
        ),
      ),
    )
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <AddWebsiteModal
        open
        onClose={onClose}
        onSubmit={async (data) => {
          await websiteApi.createWebsite(1, data)
        }}
      />,
    )

    await user.type(screen.getByLabelText('Website URL'), 'https://intranet.local')
    await user.type(screen.getByLabelText('Display name'), 'Intranet')
    await user.click(screen.getByRole('button', { name: 'Add website' }))

    expect(await screen.findByText('URL must point to a public website')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('renders nothing when closed', () => {
    render(<AddWebsiteModal open={false} onClose={vi.fn()} onSubmit={vi.fn()} />)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
