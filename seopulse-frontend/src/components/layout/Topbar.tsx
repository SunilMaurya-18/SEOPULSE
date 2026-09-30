import {
  ArrowLeft,
  Bell,
  Briefcase,
  LogOut,
  Menu,
  Moon,
  Plus,
  Sun,
} from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/Button'
import { MobileSidebar } from '@/components/layout/MobileSidebar'
import { useAuth } from '@/lib/auth'
import { useTheme } from '@/lib/theme'
import { useWorkspace } from '@/lib/workspace'

const TITLES: Record<string, string> = {
  '/dashboard': 'Overview',
  '/websites': 'Websites',
  '/audits': 'Audits',
  '/issues': 'Issues',
  '/pages': 'Pages',
  '/settings': 'Settings',
}

function parentPath(pathname: string): string {
  if (/^\/audits\/\d+\/(pages|issues)/.test(pathname)) {
    return pathname.replace(/\/(pages|issues)$/, '')
  }
  if (/^\/audits\/\d+/.test(pathname)) return '/audits'
  if (pathname === '/dashboard') return '/'
  return '/dashboard'
}

export function Topbar() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { theme, toggleTheme } = useTheme()
  const { user, logout } = useAuth()
  const { project } = useWorkspace()
  const navigate = useNavigate()
  const location = useLocation()

  const pageTitle =
    Object.entries(TITLES).find(([path]) =>
      path === '/dashboard'
        ? location.pathname === '/dashboard'
        : location.pathname.startsWith(path),
    )?.[1] ?? 'Workspace'

  const showBack = location.pathname !== '/dashboard'

  const initials = (user?.name ?? 'S')
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  function handleBack() {
    if (window.history.length > 1) {
      navigate(-1)
      return
    }
    navigate(parentPath(location.pathname))
  }

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <>
      <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center justify-between border-b border-default bg-surface/90 px-3 backdrop-blur-md sm:px-5">
        <div className="flex min-w-0 items-center gap-2.5">
          <button
            type="button"
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-default text-muted hover:bg-surface-elevated hover:text-main lg:hidden"
            onClick={() => setMobileOpen(true)}
            aria-label="Open navigation"
          >
            <Menu className="h-4 w-4" />
          </button>

          {showBack && (
            <Button
              variant="secondary"
              size="sm"
              onClick={handleBack}
              aria-label="Go back"
              className="shrink-0"
            >
              <ArrowLeft className="h-4 w-4" />
              <span className="hidden sm:inline">Back</span>
            </Button>
          )}

          <div className="min-w-0">
            <p className="truncate font-display text-sm font-semibold text-main">
              {pageTitle}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 sm:gap-2">
          <Link
            to="/settings"
            className="inline-flex max-w-[11rem] items-center gap-2 rounded-lg border border-default bg-surface-low px-2 py-1.5 transition-colors hover:border-accent/30 hover:bg-accent-surface sm:max-w-[14rem] sm:px-2.5"
            title={`Active workspace: ${project.name}`}
          >
            <Briefcase className="h-3.5 w-3.5 shrink-0 text-accent" />
            <span className="min-w-0">
              <span className="hidden font-mono text-[9px] tracking-wider text-dim uppercase sm:block">
                Active workspace
              </span>
              <span className="block truncate text-xs font-medium text-main">
                {project.name}
              </span>
            </span>
          </Link>

          <Button
            variant="ghost"
            size="sm"
            onClick={toggleTheme}
            aria-label={
              theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'
            }
          >
            {theme === 'dark' ? (
              <Sun className="h-4 w-4" />
            ) : (
              <Moon className="h-4 w-4" />
            )}
          </Button>

          <Button variant="ghost" size="sm" aria-label="Notifications">
            <Bell className="h-4 w-4" />
          </Button>

          <Link to="/websites">
            <Button size="sm">
              <Plus className="h-4 w-4" />
              <span className="hidden sm:inline">Add website</span>
            </Button>
          </Link>

          <div
            className="ml-1 flex h-8 w-8 items-center justify-center rounded-lg border border-default bg-surface-elevated text-xs font-semibold text-main"
            title={user?.email}
            aria-label={user?.name ?? 'Account'}
          >
            {initials}
          </div>

          <Button
            variant="ghost"
            size="sm"
            onClick={handleLogout}
            aria-label="Sign out"
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </header>

      <MobileSidebar open={mobileOpen} onClose={() => setMobileOpen(false)} />
    </>
  )
}
