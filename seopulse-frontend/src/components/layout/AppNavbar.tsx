import '@designcodeio/threeui/style.css'

import { LogOut, Moon, Settings, Sun } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'

import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { useTheme } from '@/lib/theme'
import { useWorkspace } from '@/lib/workspace'
import { createTopDockController, type TopDockOptions } from '@/shaders/animated-top-dock/topDockController'

type DockItem = { label: string; path: string; end?: boolean; secondary?: boolean; icon: ReactNode }

/* glyphs are drawn on the command bar's 16-unit grid so they share its 1.25 stroke */
const NAV_ITEMS: readonly DockItem[] = [
  {
    label: 'Overview',
    path: '/dashboard',
    end: true,
    icon: <><rect x="2.25" y="2.25" width="4.5" height="4.5" rx=".8" /><rect x="9.25" y="2.25" width="4.5" height="4.5" rx=".8" /><rect x="2.25" y="9.25" width="4.5" height="4.5" rx=".8" /><rect x="9.25" y="9.25" width="4.5" height="4.5" rx=".8" /></>,
  },
  {
    label: 'Websites',
    path: '/websites',
    icon: <><circle cx="8" cy="8" r="5.9" /><path d="M2.1 8h11.8M8 2.1c1.6 1.7 2.5 3.6 2.5 5.9S9.6 12.2 8 13.9M8 2.1C6.4 3.8 5.5 5.7 5.5 8s.9 4.2 2.5 5.9" /></>,
  },
  {
    label: 'Audits',
    path: '/audits',
    icon: <path d="M1.8 8.4h2.7l1.7-4.3 3.1 8 1.7-3.7h3.2" />,
  },
  {
    label: 'Issues',
    path: '/issues',
    icon: <><path d="M7.1 2.7a1 1 0 0 1 1.8 0l5.2 9.5a1 1 0 0 1-.9 1.5H2.8a1 1 0 0 1-.9-1.5z" /><path d="M8 6.4v3M8 11.5h.01" /></>,
  },
  {
    label: 'Pages',
    path: '/pages',
    icon: <><path d="M3.4 2.4h5.4l3.8 3.8v7.4H3.4z" /><path d="M8.8 2.4v3.8h3.8M5.9 9h4.2M5.9 11.2h3" /></>,
  },
  {
    label: 'Settings',
    path: '/settings',
    secondary: true,
    icon: <><path d="M2.4 4.6h5.8M11.6 4.6h2M2.4 11.4h2M7.8 11.4h5.8" /><circle cx="9.9" cy="4.6" r="1.7" /><circle cx="6.1" cy="11.4" r="1.7" /></>,
  },
]

const DOCK_OPTIONS: TopDockOptions = {
  proximity: 122,
  spring: 0.19,
  damping: 0.7,
  widthGrowth: 17,
  heightGrowth: 16,
  drop: 3.5,
  axis: 'x',
  lockTrack: true,
}

const BRAND_MARK = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <defs>
      <linearGradient id="app-dock-mark" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#ff6b5f" />
        <stop offset="1" stopColor="#e8413b" />
      </linearGradient>
    </defs>
    <rect width="24" height="24" rx="4.5" fill="url(#app-dock-mark)" />
    <path d="M7 9h10M7 15h10" stroke="#fff" strokeWidth="1.6" strokeLinecap="round" />
    <circle cx="14" cy="9" r="2" fill="#e8413b" stroke="#fff" strokeWidth="1.6" />
    <circle cx="10" cy="15" r="2" fill="#e8413b" stroke="#fff" strokeWidth="1.6" />
  </svg>
)

function AccountMenu() {
  const { user, logout } = useAuth()
  const { project } = useWorkspace()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return undefined
    const onPointerDown = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const initials = (user?.name ?? 'S')
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  async function handleLogout() {
    setOpen(false)
    await logout()
    navigate('/login', { replace: true })
  }

  const itemClass =
    'flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-[13px] font-medium text-[#c9ccd8] transition-colors hover:bg-white/[0.07] hover:text-white'

  return (
    <div ref={menuRef} className="relative">
      <button
        type="button"
        className="app-dock__avatar"
        aria-label="Account menu"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        {initials}
      </button>

      {open && (
        <div role="menu" aria-label="Account" className="app-dock__menu">
          <div className="px-3 pt-2 pb-3">
            <p className="truncate text-[13px] font-semibold text-white">{user?.name}</p>
            <p className="truncate text-[12px] text-[#8b8fa0]">{user?.email}</p>
            <p className="mt-2 inline-flex max-w-full items-center gap-1.5 rounded-full bg-white/[0.06] py-0.5 pr-2.5 pl-0.5 text-[11px] font-medium text-[#c9ccd8]">
              <span className="flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-gradient-to-b from-[#4aa8ff] to-[#0a84ff] text-[9px] font-semibold text-white">
                {project.name.charAt(0).toUpperCase()}
              </span>
              <span className="truncate">{project.name}</span>
            </p>
          </div>
          <div className="h-px bg-white/[0.06]" />
          <div className="p-1">
            <Link role="menuitem" to="/settings" className={itemClass} onClick={() => setOpen(false)}>
              <Settings className="h-4 w-4 opacity-70" />
              Settings
            </Link>
            <button role="menuitem" type="button" className={itemClass} onClick={toggleTheme}>
              {theme === 'dark' ? <Sun className="h-4 w-4 opacity-70" /> : <Moon className="h-4 w-4 opacity-70" />}
              {theme === 'dark' ? 'Light mode' : 'Dark mode'}
            </button>
            <button role="menuitem" type="button" className={itemClass} onClick={() => void handleLogout()}>
              <LogOut className="h-4 w-4 opacity-70" />
              Sign out
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export function AppNavbar() {
  const { theme, toggleTheme } = useTheme()
  const dockRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const root = dockRef.current
    if (!root) return undefined
    return createTopDockController(root, () => DOCK_OPTIONS)
  }, [])

  return (
    <div className="animated-top-dock-component atd-modern app-dock">
      <div className="atd-modern__aurora" aria-hidden="true" />
      <header className="atd-modern__bar">
        <Link className="atd-modern__brand" to="/dashboard">
          <span className="atd-modern__mark" aria-hidden="true">{BRAND_MARK}</span>
          <span className="atd-modern__word">SEOPulse</span>
        </Link>

        <nav ref={dockRef} className="atd-modern__dock" aria-label="Primary" data-dock-state="idle" data-dock-max="0.00">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              data-dock-item
              aria-label={item.label}
              className={cn('atd-modern__item', item.secondary && 'app-dock__item--secondary')}
            >
              <span className="atd-modern__icon" aria-hidden="true"><svg viewBox="0 0 16 16">{item.icon}</svg></span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="atd-modern__actions">
          <button
            type="button"
            className="atd-modern__ghost app-dock__icon-button"
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            onClick={toggleTheme}
          >
            {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>
          <AccountMenu />
          <Link className="atd-modern__cta" to="/audits" aria-label="New audit">
            <span>New audit</span>
            <svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3.2 8h9.1M8.6 4.3 12.4 8l-3.8 3.7" /></svg>
          </Link>
        </div>
      </header>
    </div>
  )
}
