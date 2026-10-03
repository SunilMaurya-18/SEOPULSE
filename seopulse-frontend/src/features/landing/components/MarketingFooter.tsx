import { useState, type FormEvent, type MouseEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'

import { getErrorMessage } from '@/api/errors'
import { newsletterApi } from '@/api/newsletter'
import { Logo } from '@/components/brand/Logo'
import { smoothScrollTo } from '@/features/landing/useInView'

const footerLinks = [
  { label: 'Solutions', href: '/#solutions' },
  { label: 'Vision', href: '/#vision' },
  { label: 'Blog', href: '/#blog' },
  { label: 'Terms of Service', href: '/terms' },
  { label: 'Privacy Policy', href: '/privacy' },
  { label: 'Refund Policy', href: '/refund-policy' },
]

export function MarketingFooter() {
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [agreed, setAgreed] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [sentTo, setSentTo] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    const trimmed = email.trim()
    setSentTo('')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      setError('Enter a valid email address.')
      return
    }
    if (!agreed) {
      setError('Agree to receive marketing emails to join.')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      await newsletterApi.subscribe(trimmed)
      setSentTo(trimmed)
    } catch (err) {
      setError(getErrorMessage(err, 'Could not join the list. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  function followLink(event: MouseEvent<HTMLAnchorElement>, href: string) {
    const [path, id] = href.split('#')
    if (!id || path !== location.pathname) return
    if (smoothScrollTo(id)) event.preventDefault()
  }

  return (
    <footer id="blog" className="scroll-mt-24 bg-black px-5 py-16 text-[#f2f5ea] md:px-[60px] md:py-20">
      <div className="mx-auto grid max-w-6xl gap-14 lg:grid-cols-2">
        <div>
          <Logo />
          <ul className="mt-8 space-y-3">
            {footerLinks.map((link) => (
              <li key={link.label}>
                <Link
                  to={link.href}
                  onClick={(event) => followLink(event, link.href)}
                  className="font-mono text-sm underline underline-offset-4"
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
          <Link
            to="/register"
            className="mt-8 inline-flex rounded-md bg-accent px-4 py-2.5 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110"
          >
            Get Started
          </Link>
        </div>

        <div>
          <h2 className="font-mono text-2xl font-semibold tracking-tight">Join our mailing list</h2>
          <p className="mt-3 max-w-md font-mono text-sm leading-relaxed text-[#f2f5ea]/85">
            Stay updated with our latest news and updates delivered straight to your inbox.
          </p>
          <form onSubmit={(event) => void submit(event)} className="mt-8 max-w-md space-y-5" noValidate>
            <div>
              <label htmlFor="mailing-email" className="font-mono text-sm">
                Email address <span className="text-accent">*</span>
              </label>
              <input
                id="mailing-email"
                type="email"
                required
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value)
                  setSentTo('')
                }}
                placeholder="Enter your email"
                autoComplete="email"
                className="mt-2 w-full border-0 border-b border-white/70 bg-transparent px-0 py-2 font-mono text-sm text-[#f2f5ea] outline-none placeholder:text-[#f2f5ea]/40 focus:border-accent"
              />
            </div>
            <label className="flex items-start gap-3 font-mono text-sm">
              <input
                type="checkbox"
                checked={agreed}
                onChange={(event) => setAgreed(event.target.checked)}
                className="mt-1 accent-[#f5504a]"
              />
              <span>
                Yes, I agree to receive marketing emails. <span className="text-accent">*</span>
                <span className="mt-1 block text-xs text-[#f2f5ea]/60">
                  Unsubscribe anytime. See our{' '}
                  <Link to="/privacy" className="underline underline-offset-2">
                    Privacy Policy
                  </Link>
                  .
                </span>
              </span>
            </label>
            {error && (
              <p role="alert" className="font-mono text-sm text-accent">
                {error}
              </p>
            )}
            {sentTo && (
              <p role="status" className="font-mono text-sm text-[#f2f5ea]">
                Check your inbox to confirm. We sent a link to {sentTo}.
              </p>
            )}
            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-md bg-accent px-4 py-3 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110 disabled:pointer-events-none disabled:opacity-60 sm:w-auto sm:min-w-48"
            >
              {submitting ? 'Joining…' : 'Join Now'}
            </button>
          </form>
        </div>
      </div>
    </footer>
  )
}
