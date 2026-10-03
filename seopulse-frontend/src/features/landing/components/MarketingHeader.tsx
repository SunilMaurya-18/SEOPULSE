import { CircleUser, Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { Logo } from '@/components/brand/Logo'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { smoothScrollTo } from '@/features/landing/useInView'

const links = [
  { label: 'Solutions', id: 'solutions' },
  { label: 'Vision', id: 'vision' },
  { label: 'Blog', id: 'blog' },
]

export function MarketingHeader() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [openPath, setOpenPath] = useState(location.pathname)
  const [scrollPast, setScrollPast] = useState(
    () => window.scrollY >= window.innerHeight * 0.72,
  )

  if (openPath !== location.pathname) {
    setOpenPath(location.pathname)
    setOpen(false)
  }

  useEffect(() => {
    function onScroll() {
      setScrollPast(window.scrollY >= window.innerHeight * 0.72)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const overHero = location.pathname === '/' && !scrollPast

  function go(id: string) {
    setOpen(false)
    if (location.pathname === '/' && smoothScrollTo(id)) return
    navigate({ pathname: '/', hash: id })
  }

  const startHref = isAuthenticated ? '/dashboard' : '/register'
  const loginHref = isAuthenticated ? '/dashboard' : '/login'
  const loginLabel = isAuthenticated ? 'Workspace' : 'Log In'

  return (
    <header
      className={cn(
        'fixed top-0 right-0 left-0 z-50',
        overHero
          ? 'bg-gradient-to-b from-black/80 via-black/35 to-transparent'
          : 'border-b border-white/10 bg-canvas/95 backdrop-blur-md',
      )}
    >
      <div className="flex items-center justify-between px-5 py-4 md:px-[60px]">
        <Logo />

        <div className="flex items-center gap-3 sm:gap-5">
          <nav
            aria-label="Primary"
            className={cn(
              'hidden items-center rounded-full border border-white/10 bg-surface-elevated/90 backdrop-blur-md py-1 pr-1 pl-5 transition-opacity duration-300 md:flex',
              overHero && 'pointer-events-none opacity-0',
            )}
          >
            {links.map((link) => (
              <button
                key={link.id}
                type="button"
                onClick={() => go(link.id)}
                className="px-3 py-2 font-mono text-sm text-main hover:text-white"
              >
                {link.label}
              </button>
            ))}
            <Link to="/pricing" className="px-3 py-2 font-mono text-sm text-main hover:text-white">
              Pricing
            </Link>
            <Link
              to={startHref}
              className="ml-2 rounded-full bg-accent px-4 py-2 font-mono text-sm text-on-accent transition hover:-translate-y-0.5 hover:brightness-110"
            >
              Get Started
            </Link>
          </nav>

          <Link
            to={loginHref}
            className="inline-flex items-center gap-2 font-mono text-sm text-accent"
          >
            <CircleUser className="h-5 w-5" aria-hidden />
            {loginLabel}
          </Link>

          <button
            type="button"
            className="inline-flex h-10 w-10 items-center justify-center rounded-md text-main md:hidden"
            aria-expanded={open}
            aria-label={open ? 'Close menu' : 'Open menu'}
            onClick={() => setOpen((value) => !value)}
          >
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {open && (
        <nav
          aria-label="Mobile"
          className="mx-5 mb-3 flex flex-col gap-1 rounded-2xl border border-white/10 bg-surface-elevated/90 backdrop-blur-md p-3 md:hidden"
        >
          {links.map((link) => (
            <button
              key={link.id}
              type="button"
              onClick={() => go(link.id)}
              className="rounded-md px-3 py-3 text-left font-mono text-sm text-main"
            >
              {link.label}
            </button>
          ))}
          <Link
            to="/pricing"
            onClick={() => setOpen(false)}
            className="rounded-md px-3 py-3 text-left font-mono text-sm text-main"
          >
            Pricing
          </Link>
          <Link
            to={startHref}
            onClick={() => setOpen(false)}
            className="mt-1 rounded-md bg-accent px-3 py-3 text-center font-mono text-sm text-on-accent"
          >
            Get Started
          </Link>
        </nav>
      )}
    </header>
  )
}
