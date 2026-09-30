import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { Logo } from '@/components/brand/Logo'

const footerLinks = [
  { label: 'Solutions', href: '/#solutions' },
  { label: 'Vision', href: '/#vision' },
  { label: 'Blog', href: '/#blog' },
  { label: 'Privacy Policy', href: '/terms' },
  { label: 'Accessibility Statement', href: '/terms' },
]

function SocialIcon({ name }: { name: 'facebook' | 'instagram' | 'tiktok' }) {
  const paths = {
    facebook: 'M14 8h-2V6c0-.6.4-1 1-1h1V3h-2c-1.7 0-3 1.3-3 3v2H7v2h2v7h2v-7h2l1-2z',
    instagram:
      'M8 3h8a5 5 0 0 1 5 5v8a5 5 0 0 1-5 5H8a5 5 0 0 1-5-5V8a5 5 0 0 1 5-5zm8 2H8a3 3 0 0 0-3 3v8a3 3 0 0 0 3 3h8a3 3 0 0 0 3-3V8a3 3 0 0 0-3-3zm-4 3.2A3.8 3.8 0 1 1 8.2 12 3.8 3.8 0 0 1 12 8.2zm0 2A1.8 1.8 0 1 0 13.8 12 1.8 1.8 0 0 0 12 10.2zM17.2 6.6a1 1 0 1 1-1 1 1 1 0 0 1 1-1z',
    tiktok:
      'M14 4c.6 2.2 2 3.6 4 4v2.2c-1.4 0-2.6-.4-4-1.2v5.4A5.4 5.4 0 1 1 8.6 9.1v2.3a3.1 3.1 0 1 0 2.2 3V4z',
  }
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5 fill-current" aria-hidden>
      <path d={paths[name]} />
    </svg>
  )
}

export function MarketingFooter() {
  const [email, setEmail] = useState('')
  const [agreed, setAgreed] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  function submit(event: FormEvent) {
    event.preventDefault()
    const trimmed = email.trim()
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      setError('Enter a valid email address.')
      setDone(false)
      return
    }
    if (!agreed) {
      setError('Agree to receive marketing emails to join.')
      setDone(false)
      return
    }
    setError('')
    setDone(true)
  }

  return (
    <footer id="blog" className="scroll-mt-24 bg-black px-5 py-16 text-[#f2f5ea] md:px-[60px] md:py-20">
      <div className="mx-auto grid max-w-6xl gap-14 lg:grid-cols-2">
        <div>
          <Logo />
          <ul className="mt-8 space-y-3">
            {footerLinks.map((link) => (
              <li key={link.label}>
                <Link to={link.href} className="font-mono text-sm underline underline-offset-4">
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
          <div className="mt-8 space-y-1 font-mono text-sm text-[#f2f5ea]/90">
            <p>
              <a href="tel:1234567890">123-456-7890</a>
            </p>
            <p>
              <a href="mailto:info@mysite.com">info@mysite.com</a>
            </p>
            <p>500 Terry Francine St, San Francisco, CA 94158</p>
          </div>
        </div>

        <div>
          <h2 className="font-mono text-2xl font-normal tracking-wide">Join our mailing list</h2>
          <p className="mt-3 max-w-md font-mono text-sm leading-relaxed text-[#f2f5ea]/85">
            Stay updated with our latest news and updates delivered straight to your inbox.
          </p>
          <form onSubmit={submit} className="mt-8 max-w-md space-y-5" noValidate>
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
                  setDone(false)
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
              <span>Yes, I agree to receive marketing emails. <span className="text-accent">*</span></span>
            </label>
            {error && (
              <p role="alert" className="font-mono text-sm text-accent">
                {error}
              </p>
            )}
            {done && (
              <p role="status" className="font-mono text-sm text-[#f2f5ea]">
                You are on the list. We will write to {email.trim()}.
              </p>
            )}
            <button
              type="submit"
              className="w-full rounded-md bg-accent px-4 py-3 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110 sm:w-auto sm:min-w-48"
            >
              Join Now
            </button>
          </form>

          <div className="mt-10 flex items-center gap-4">
            <a href="https://facebook.com" aria-label="Facebook" className="text-[#f2f5ea] hover:text-accent">
              <SocialIcon name="facebook" />
            </a>
            <a href="https://instagram.com" aria-label="Instagram" className="text-[#f2f5ea] hover:text-accent">
              <SocialIcon name="instagram" />
            </a>
            <a href="https://www.tiktok.com" aria-label="TikTok" className="text-[#f2f5ea] hover:text-accent">
              <SocialIcon name="tiktok" />
            </a>
          </div>
          <p className="mt-6 font-mono text-sm">© 2035 by SEOPulse.</p>
        </div>
      </div>
    </footer>
  )
}
