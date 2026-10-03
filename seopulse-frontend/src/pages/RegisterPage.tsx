import { useMemo, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'

import { getErrorMessage } from '@/api/errors'
import { AuthShell, PendingWebsiteNotice } from '@/components/auth/AuthShell'
import { GoogleSignInButton } from '@/components/auth/GoogleSignInButton'
import { captchaEnabled, Turnstile } from '@/components/security/Turnstile'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import {
  peekPendingWebsiteUrl,
  setPendingWebsiteUrl,
} from '@/lib/pendingWebsite'
import { useToast } from '@/lib/toast'

export function RegisterPage() {
  const { register, loginWithGoogle, isAuthenticated } = useAuth()
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
  const [captchaToken, setCaptchaToken] = useState<string | null>(null)
  const [captchaKey, setCaptchaKey] = useState(0)

  if (isAuthenticated) {
    if (pendingFromQuery) {
      setPendingWebsiteUrl(pendingFromQuery)
    }
    return <Navigate to="/dashboard" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (captchaEnabled && !captchaToken) {
      setError('Complete the security check and try again.')
      return
    }
    setLoading(true)
    try {
      if (pendingFromQuery) {
        setPendingWebsiteUrl(pendingFromQuery)
      }
      const created = await register(name.trim(), email.trim(), password, captchaToken ?? undefined)
      pushToast({
        tone: 'success',
        title: 'Account created',
        description: pendingFromQuery
          ? 'Connecting your website…'
          : created.emailVerificationRequired
            ? 'Check your inbox for a link to verify your email.'
            : 'Welcome to SEOPulse. Connect a website to run your first audit.',
      })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to create account. Please try again.'))
      setCaptchaKey((key) => key + 1)
    } finally {
      setLoading(false)
    }
  }

  async function handleGoogle(credential: string) {
    setError(null)
    setLoading(true)
    try {
      if (pendingFromQuery) {
        setPendingWebsiteUrl(pendingFromQuery)
      }
      await loginWithGoogle(credential)
      pushToast({
        tone: 'success',
        title: 'Signed in with Google',
        description: pendingFromQuery
          ? 'Connecting your website…'
          : 'Welcome to SEOPulse. Connect a website to run your first audit.',
      })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, 'Google sign-in failed. Please try again.'))
    } finally {
      setLoading(false)
    }
  }

  const strength = passwordStrength(password)

  return (
    <AuthShell
      title="Create your account"
      description="Start auditing websites in minutes."
      notice={
        pendingFromQuery && <PendingWebsiteNotice url={pendingFromQuery} action="After you sign up," />
      }
      footer={
        <>
          Already have an account?{' '}
          <Link
            to={
              pendingFromQuery
                ? `/login?url=${encodeURIComponent(pendingFromQuery)}`
                : '/login'
            }
            className="font-semibold text-accent hover:text-accent-hover"
          >
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <Alert variant="error" title="Registration failed">
            {error}
          </Alert>
        )}

        <GoogleSignInButton text="signup_with" onCredential={(credential) => void handleGoogle(credential)} />

        <Input
          id="name"
          label="Name"
          autoComplete="name"
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Your name"
          className="h-11"
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
          className="h-11"
        />

        <div className="space-y-2">
          <Input
            id="password"
            label="Password"
            type="password"
            autoComplete="new-password"
            required
            minLength={10}
            maxLength={72}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="At least 10 characters"
            aria-describedby="password-strength"
            className="h-11"
          />
          <div className="flex items-center gap-3">
            <div className="flex flex-1 gap-1" aria-hidden="true">
              {[1, 2, 3, 4].map((step) => (
                <span
                  key={step}
                  className={cn(
                    'h-1 flex-1 rounded-full transition-colors duration-300',
                    strength && step <= strength.score ? strength.tone : 'bg-surface-high',
                  )}
                />
              ))}
            </div>
            <span id="password-strength" aria-live="polite" className="text-[12px] font-medium text-muted">
              {strength?.label ?? '10+ characters'}
            </span>
          </div>
          <p className="text-[12px] text-dim">Passwords found in known data breaches are rejected.</p>
        </div>

        <Turnstile key={captchaKey} onToken={setCaptchaToken} className="flex justify-center" />

        <Button type="submit" size="lg" className="w-full" loading={loading}>
          Create account
        </Button>

        <p className="text-center text-[12px] text-dim">
          By creating an account you agree to the{' '}
          <Link to="/terms" className="font-medium text-muted underline-offset-2 hover:text-main hover:underline">
            Terms
          </Link>{' '}
          and acknowledge the{' '}
          <Link to="/privacy" className="font-medium text-muted underline-offset-2 hover:text-main hover:underline">
            Privacy Policy
          </Link>
          .
        </p>
      </form>
    </AuthShell>
  )
}

function passwordStrength(password: string) {
  if (!password) return null
  if (password.length < 10) {
    const missing = 10 - password.length
    return { score: 1, tone: 'bg-critical', label: `${missing} more character${missing === 1 ? '' : 's'}` }
  }
  const variety = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((rule) => rule.test(password)).length
  if (password.length >= 14 && variety >= 3) return { score: 4, tone: 'bg-success', label: 'Strong' }
  if (password.length >= 12 || variety >= 3) return { score: 3, tone: 'bg-info', label: 'Good' }
  return { score: 2, tone: 'bg-warning', label: 'Fair' }
}
