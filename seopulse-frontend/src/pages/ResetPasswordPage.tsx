import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/api/errors'
import { AuthShell } from '@/components/auth/AuthShell'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

export function ResetPasswordPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [token] = useState(() => searchParams.get('token') ?? '')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (searchParams.has('token')) setSearchParams({}, { replace: true })
  }, [searchParams, setSearchParams])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      await authApi.resetPassword(token, password)
      setDone(true)
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to reset your password. Please try again.'))
    } finally {
      setLoading(false)
    }
  }

  const footer = (
    <Link to="/login" className="font-medium text-accent hover:text-accent-hover">
      Back to sign in
    </Link>
  )

  if (!token) {
    return (
      <AuthShell title="Choose a new password" footer={footer}>
        <Alert variant="error" title="Invalid link">
          This reset link is missing its token. Request a new one from{' '}
          <Link to="/forgot-password" className="underline">
            the forgot password page
          </Link>
          .
        </Alert>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="Choose a new password" footer={footer}>
      {done ? (
        <>
          <Alert variant="success" title="Password updated">
            Your password has been changed and all other sessions were signed out.
          </Alert>
          <Link to="/login">
            <Button className="w-full">Sign in</Button>
          </Link>
        </>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <Alert variant="error" title="Reset failed">
              {error}
            </Alert>
          )}
          <Input
            id="new-password"
            label="New password"
            type="password"
            autoComplete="new-password"
            required
            minLength={10}
            maxLength={72}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            hint="Minimum 10 characters. Passwords found in known data breaches are rejected."
          />
          <Input
            id="confirm-password"
            label="Confirm new password"
            type="password"
            autoComplete="new-password"
            required
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
          <Button type="submit" className="w-full" loading={loading}>
            Update password
          </Button>
        </form>
      )}
    </AuthShell>
  )
}
