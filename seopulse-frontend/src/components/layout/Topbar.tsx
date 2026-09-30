import { Bell, ChevronLeft, LogOut, Menu, Moon, Sun } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { MobileSidebar } from '@/components/layout/MobileSidebar'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
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

function IconButton({
  label,
  onClick,
  children,
  className,
}: {
  label: string
  onClick?: () => void
  children: React.ReactNode
  className?: string
}) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={onClick}
      className={cn(
        'flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-surface-elevated hover:text-main',
        className,
      )}
    >
      {children}
    </button>
  )
}

export function Topbar() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { theme, toggleTheme } = useTheme()
  const { logout } = useAuth()
  const { project } = useWorkspace()
  const navigate = useNavigate()
  const location = useLocation()

  const pageTitle =
    Object.entries(TITLES).find(([path]) =>
      path === '/dashboard' ? location.pathname === '/dashboard' : location.pathname.startsWith(path),
    )?.[1] ?? 'Workspace'

  const showBack = location.pathname !== '/dashboard'

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
      <header className="glass sticky top-0 z-30 flex h-14 shrink-0 items-center justify-between gap-3 border-b border-default px-3 sm:px-6">
        <div className="flex min-w-0 items-center gap-1.5">
          <IconButton label="Open navigation" onClick={() => setMobileOpen(true)} className="lg:hidden">
            <Menu className="h-[18px] w-[18px]" />
          </IconButton>

          {showBack && (
            <button
              type="button"
              onClick={handleBack}
              aria-label="Go back"
              className="-ml-1 inline-flex h-8 shrink-0 items-center gap-0.5 rounded-full pr-2.5 pl-1 text-[15px] font-medium text-accent transition-opacity hover:opacity-75"
            >
              <ChevronLeft className="h-5 w-5" strokeWidth={2.25} />
              <span className="hidden sm:inline">Back</span>
            </button>
          )}

          <p className="truncate text-[15px] font-semibold tracking-[-0.01em] text-main">{pageTitle}</p>
        </div>

        <div className="flex items-center gap-1 sm:gap-1.5">
          <Link
            to="/settings"
            className="hidden max-w-[14rem] items-center gap-2 rounded-full bg-surface-elevated py-1 pr-3 pl-1 transition-colors hover:bg-surface-high sm:inline-flex"
            title={`Active workspace: ${project.name}`}
          >
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-gradient-to-b from-[#4aa8ff] to-[#0a84ff] text-[11px] font-semibold text-white">
              {project.name.charAt(0).toUpperCase()}
            </span>
            <span className="truncate text-[13px] font-medium text-main">{project.name}</span>
          </Link>

          <IconButton
            label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            onClick={toggleTheme}
          >
            {theme === 'dark' ? <Sun className="h-[18px] w-[18px]" /> : <Moon className="h-[18px] w-[18px]" />}
          </IconButton>

          <IconButton label="Notifications">
            <Bell className="h-[18px] w-[18px]" />
          </IconButton>

          <IconButton label="Sign out" onClick={() => void handleLogout()}>
            <LogOut className="h-[18px] w-[18px]" />
          </IconButton>
        </div>
      </header>

      <MobileSidebar open={mobileOpen} onClose={() => setMobileOpen(false)} />
    </>
  )
}
