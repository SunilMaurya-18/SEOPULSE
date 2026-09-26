import { useState, type FormEvent } from 'react'
import axios from 'axios'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { websiteNameFromUrl } from '@/api/websites'

interface AddWebsiteModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: { name: string; url: string }) => Promise<void>
}

function extractApiError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as
      | {
          message?: string
          validationErrors?: Record<string, string>
        }
      | undefined

    const validation = data?.validationErrors
    if (validation) {
      const first = Object.values(validation)[0]
      if (first) return first
    }

    if (data?.message && data.message !== 'Validation Failed') {
      return data.message
    }
  }

  return 'Unable to add this website. Please try again.'
}

export function AddWebsiteModal({
  open,
  onClose,
  onSubmit,
}: AddWebsiteModalProps) {
  const [url, setUrl] = useState('')
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const trimmedUrl = url.trim()
    if (!trimmedUrl) {
      setError('Please enter a website URL.')
      return
    }

    const trimmedName = name.trim() || websiteNameFromUrl(trimmedUrl)

    try {
      setLoading(true)
      setError('')
      await onSubmit({ name: trimmedName, url: trimmedUrl })
      setUrl('')
      setName('')
      onClose()
    } catch (err) {
      console.error('Failed to create website', err)
      setError(extractApiError(err))
    } finally {
      setLoading(false)
    }
  }

  function handleClose() {
    if (loading) return
    setError('')
    setUrl('')
    setName('')
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="Add website"
      description="Connect a website to start auditing it."
      size="sm"
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <Input
          id="website-url"
          label="Website URL"
          placeholder="https://example.com"
          value={url}
          onChange={(event) => setUrl(event.target.value)}
          type="url"
          required
        />

        <Input
          id="website-name"
          label="Display name"
          placeholder="Auto-filled from URL"
          value={name}
          onChange={(event) => setName(event.target.value)}
          hint="Optional. Defaults to the site hostname."
        />

        {error && <Alert variant="error">{error}</Alert>}

        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="secondary"
            onClick={handleClose}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button type="submit" loading={loading}>
            Add website
          </Button>
        </div>
      </form>
    </Modal>
  )
}
