import { useMemo, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { Activity } from 'lucide-react'
import axios from 'axios'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useAuth } from '@/lib/auth'
import {
  peekPendingWebsiteUrl,
  setPendingWebsiteUrl,
} from '@/lib/pendingWebsite'
import { useToast } from '@/lib/toast'

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { pushToast } = useToast()

  const pendingFromQuery = useMemo(() => {
    const raw = searchParams.get('url') ?? searchParams.get('audit')
    return raw?.trim() || peekPendingWebsiteUrl()
  }, [searchParams])

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    if (pendingFromQuery) {
      setPendingWebsiteUrl(pendingFromQuery)
    }
    return <Navigate to="/dashboard" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      if (pendingFromQuery) {
        setPendingWebsiteUrl(pendingFromQuery)
      }
      await register(name.trim(), email.trim(), password)
      pushToast({
        tone: 'success',
        title: 'Account created',
        description: pendingFromQuery
          ? 'Connecting your website and starting an audit…'
          : 'Your workspace is ready.',
      })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError(
          err.response?.data?.message ??
            'Unable to create account. Please try again.',
        )
      } else {
        setError('Unable to create account. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <Link to="/" className="inline-flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded bg-accent text-white">
              <Activity className="h-4 w-4" strokeWidth={2.25} />
            </div>
            <span className="font-display text-lg font-semibold text-main">
              SEOPulse
            </span>
          </Link>
          <h1 className="mt-6 font-display text-2xl font-semibold text-main">
            Create account
          </h1>
          <p className="mt-1 text-sm text-muted">
            Start auditing websites in minutes.
          </p>
          {pendingFromQuery && (
            <p className="mt-3 rounded border border-default bg-surface px-3 py-2 font-mono text-xs text-muted">
              After signup we will connect{' '}
              <span className="text-main">{pendingFromQuery}</span>
            </p>
          )}
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-lg border border-default bg-surface p-5"
        >
          {error && (
            <Alert variant="error" title="Registration failed">
              {error}
            </Alert>
          )}

          <Input
            id="name"
            label="Name"
            autoComplete="name"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Your name"
          />

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

          <Input
            id="password"
            label="Password"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="At least 8 characters"
            hint="Minimum 8 characters"
          />

          <Button type="submit" className="w-full" loading={loading}>
            Create account
          </Button>
        </form>

        <p className="text-center text-sm text-muted">
          Already have an account?{' '}
          <Link
            to={
              pendingFromQuery
                ? `/login?url=${encodeURIComponent(pendingFromQuery)}`
                : '/login'
            }
            className="font-medium text-accent hover:text-accent-hover"
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
