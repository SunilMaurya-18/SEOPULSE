import { useState, type FormEvent } from 'react'
import { Download, Trash2 } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'

import { accountApi } from '@/api/account'
import { getErrorMessage } from '@/api/errors'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { SettingsGroup, SettingsRow } from '@/features/settings/SettingsGroup'
import { useAuth } from '@/lib/auth'
import { useToast } from '@/lib/toast'

export function AccountDataSection() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const { pushToast } = useToast()
  const [exporting, setExporting] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')

  async function handleExport() {
    setExporting(true)
    try {
      await accountApi.downloadExport()
    } catch (err) {
      pushToast({
        tone: 'error',
        title: 'Export failed',
        description: getErrorMessage(err, 'Unable to prepare your data. Please try again.'),
      })
    } finally {
      setExporting(false)
    }
  }

  function closeConfirm() {
    if (deleting) return
    setConfirmOpen(false)
    setPassword('')
    setError('')
  }

  async function handleDelete(event: FormEvent) {
    event.preventDefault()
    if (!password) {
      setError('Enter your password to continue.')
      return
    }
    setDeleting(true)
    setError('')
    try {
      await accountApi.delete(password)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to delete your account.'))
      setDeleting(false)
      return
    }
    await logout()
    pushToast({ tone: 'success', title: 'Account deleted', description: 'Your account and data have been removed.' })
    navigate('/', { replace: true })
  }

  return (
    <>
      <SettingsGroup
        title="Privacy & data"
        footer="Your export is a JSON file with your profile, workspaces, websites and audit results."
      >
        <SettingsRow
          icon={<Download className="h-4 w-4" />}
          tint="from-[#4aa8ff] to-[#0a84ff]"
          label="Download my data"
          detail="A copy of everything stored for your account."
        >
          <Button variant="secondary" size="sm" loading={exporting} onClick={() => void handleExport()}>
            Download
          </Button>
        </SettingsRow>
        <SettingsRow
          icon={<Trash2 className="h-4 w-4" />}
          tint="from-[#ff6b5f] to-[#ff3b30]"
          label={<span className="text-critical">Delete account</span>}
          detail="Permanently removes your account. This cannot be undone."
        >
          <Button variant="danger" size="sm" onClick={() => setConfirmOpen(true)}>
            Delete
          </Button>
        </SettingsRow>
      </SettingsGroup>

      <Modal
        open={confirmOpen}
        onClose={closeConfirm}
        title="Delete your account?"
        description="This permanently deletes your account and cannot be undone."
        size="sm"
      >
        <form onSubmit={handleDelete} className="space-y-4" noValidate>
          <ul className="list-disc space-y-1.5 pl-5 text-[13px] leading-relaxed text-muted">
            <li>Workspaces where you are the only member are deleted with their websites, audits and reports.</li>
            <li>Paid subscriptions on those workspaces are cancelled immediately.</li>
            <li>In shared workspaces, your projects are handed to a teammate.</li>
          </ul>
          <Input
            id="delete-account-password"
            type="password"
            label="Current password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={error || undefined}
            autoFocus
          />
          <p className="-mt-2 text-[12px] text-dim">
            Signed up with Google? Set a password first with{' '}
            <Link to="/forgot-password" className="font-medium text-accent hover:text-accent-hover">
              Forgot password
            </Link>
            .
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={closeConfirm} disabled={deleting}>
              Cancel
            </Button>
            <Button type="submit" variant="danger" loading={deleting}>
              Delete account
            </Button>
          </div>
        </form>
      </Modal>
    </>
  )
}
