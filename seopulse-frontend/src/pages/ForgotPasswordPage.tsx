import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/api/errors'
import { AuthShell } from '@/components/auth/AuthShell'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await authApi.forgotPassword(email.trim())
      setSent(true)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to send a reset link. Please try again.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Reset your password"
      description="We'll email you a link to choose a new password."
      footer={
        <Link to="/login" className="font-medium text-accent hover:text-accent-hover">
          Back to sign in
        </Link>
      }
    >
      {sent ? (
        <Alert variant="success" title="Check your inbox">
          If an account exists for {email.trim()}, a reset link is on its way. The link expires in
          one hour.
        </Alert>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <Alert variant="error" title="Request failed">
              {error}
            </Alert>
          )}
          <Input
            id="email"
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@company.com"
          />
          <Button type="submit" className="w-full" loading={loading}>
            Send reset link
          </Button>
        </form>
      )}
    </AuthShell>
  )
}
