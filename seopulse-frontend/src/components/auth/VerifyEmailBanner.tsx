import { useState } from 'react'
import { MailWarning } from 'lucide-react'

import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/api/errors'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'
import { useToast } from '@/lib/toast'

export function VerifyEmailBanner() {
  const { user } = useAuth()
  const { pushToast } = useToast()
  const [sending, setSending] = useState(false)
  const [sent, setSent] = useState(false)

  if (!user || user.emailVerified || !user.emailVerificationRequired) return null

  async function handleResend() {
    setSending(true)
    try {
      await authApi.resendVerification()
      setSent(true)
      pushToast({
        tone: 'success',
        title: 'Verification email sent',
        description: `Check ${user?.email} for the link.`,
      })
    } catch (err) {
      pushToast({
        tone: 'error',
        title: 'Could not send email',
        description: getErrorMessage(err, 'Please try again later.'),
      })
    } finally {
      setSending(false)
    }
  }

  return (
    <div
      role="status"
      className="flex flex-wrap items-center justify-between gap-3 border-b border-warning/30 bg-warning-surface px-3 py-2 sm:px-5"
    >
      <p className="flex items-center gap-2 text-sm text-main">
        <MailWarning className="h-4 w-4 shrink-0 text-warning" aria-hidden />
        Verify your email address to start running audits.
      </p>
      <Button
        size="sm"
        variant="secondary"
        loading={sending}
        disabled={sent}
        onClick={() => void handleResend()}
      >
        {sent ? 'Email sent' : 'Resend verification email'}
      </Button>
    </div>
  )
}
