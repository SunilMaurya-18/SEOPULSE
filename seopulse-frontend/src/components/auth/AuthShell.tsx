import { Moon, Sun } from 'lucide-react'
import { useEffect, type ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { Logo } from '@/components/brand/Logo'
import { useTheme } from '@/lib/theme'
import { AuthShowcase } from './AuthShowcase'

export function AuthShell({
  title,
  description,
  notice,
  children,
  footer,
}: {
  title: string
  description?: string
  notice?: ReactNode
  children: ReactNode
  footer?: ReactNode
}) {
  const { theme, toggleTheme } = useTheme()

  useEffect(() => {
    document.body.classList.add('app-shell')
    return () => document.body.classList.remove('app-shell')
  }, [])

  return (
    <div className="app-canvas flex min-h-dvh">
      <AuthShowcase />

      <div className="flex min-w-0 flex-1 flex-col px-5 py-5 sm:px-10">
        <header className="flex h-10 items-center justify-between">
          <Logo to="/" appearance="app" className="lg:invisible" />
          <button
            type="button"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-elevated text-muted transition-colors hover:bg-surface-high hover:text-main"
          >
            {theme === 'dark' ? <Sun className="h-[18px] w-[18px]" /> : <Moon className="h-[18px] w-[18px]" />}
          </button>
        </header>

        <main className="flex flex-1 items-center justify-center py-10">
          <div className="animate-page-enter w-full max-w-[420px]">
            <h1 className="text-large-title text-main">{title}</h1>
            {description && <p className="mt-2 text-[15px] text-muted">{description}</p>}
            {notice && <div className="mt-5">{notice}</div>}

            <div className="widget mt-7 space-y-4 p-6 sm:p-7">{children}</div>

            {footer && <p className="mt-6 text-center text-[14px] text-muted">{footer}</p>}
          </div>
        </main>

        <footer className="flex items-center justify-center gap-3 text-[12px] text-dim">
          <span>© {new Date().getFullYear()} SEOPulse</span>
          <span aria-hidden="true">·</span>
          <Link to="/terms" className="transition-colors hover:text-main">
            Terms
          </Link>
          <span aria-hidden="true">·</span>
          <Link to="/privacy" className="transition-colors hover:text-main">
            Privacy
          </Link>
          <span aria-hidden="true">·</span>
          <Link to="/pricing" className="transition-colors hover:text-main">
            Pricing
          </Link>
        </footer>
      </div>
    </div>
  )
}

export function PendingWebsiteNotice({ url, action }: { url: string; action: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl bg-accent-surface px-4 py-3">
      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-[10px] bg-gradient-to-b from-[#4aa8ff] to-[#0a84ff] text-[13px] font-bold text-white">
        {url.replace(/^https?:\/\//, '').charAt(0).toUpperCase()}
      </span>
      <p className="min-w-0 text-[13px] text-muted">
        {action} we'll connect{' '}
        <span className="font-semibold text-main [overflow-wrap:anywhere]">{url.replace(/^https?:\/\//, '')}</span>
      </p>
    </div>
  )
}
