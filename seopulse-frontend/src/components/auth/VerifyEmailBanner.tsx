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
    <div className="mx-auto w-full max-w-[1320px] px-4 pt-5 sm:px-6 lg:px-10">
      <div
        role="status"
        className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-warning-surface px-4 py-3"
      >
        <p className="flex items-center gap-3 text-sm font-medium text-main">
          <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-warning text-white">
            <MailWarning className="h-3.5 w-3.5" aria-hidden />
          </span>
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
    </div>
  )
}
