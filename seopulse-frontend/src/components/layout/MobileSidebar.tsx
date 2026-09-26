import {
  Activity,
  FileSearch,
  FileWarning,
  Gauge,
  Globe,
  Settings,
  X,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/cn'

interface MobileSidebarProps {
  open: boolean
  onClose: () => void
}

const navigation = [
  { label: 'Overview', path: '/dashboard', icon: Gauge, end: true },
  { label: 'Websites', path: '/websites', icon: Globe },
  { label: 'Audits', path: '/audits', icon: Activity },
  { label: 'Issues', path: '/issues', icon: FileWarning },
  { label: 'Pages', path: '/pages', icon: FileSearch },
  { label: 'Settings', path: '/settings', icon: Settings },
]

export function MobileSidebar({ open, onClose }: MobileSidebarProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <button
        type="button"
        aria-label="Close navigation"
        onClick={onClose}
        className="absolute inset-0 bg-black/50"
      />
      <aside className="relative flex h-full w-72 max-w-[85vw] flex-col border-r border-default bg-surface shadow-[var(--sp-overlay-shadow)]">
        <div className="flex h-14 items-center justify-between border-b border-default px-4">
          <NavLink to="/dashboard" onClick={onClose} className="flex items-center gap-2.5">
            <div className="flex h-7 w-7 items-center justify-center rounded bg-accent text-white">
              <Activity className="h-3.5 w-3.5" />
            </div>
            <span className="font-display text-sm font-semibold text-main">
              SEOPulse
            </span>
          </NavLink>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1.5 text-muted hover:bg-surface-elevated hover:text-main"
            aria-label="Close navigation"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <nav className="flex-1 space-y-0.5 px-2.5 py-4">
          {navigation.map((item) => {
            const Icon = item.icon
            return (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.end}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    'flex h-10 items-center gap-3 rounded px-3 text-sm transition-colors',
                    isActive
                      ? 'bg-accent-surface font-medium text-accent'
                      : 'text-muted hover:bg-surface-elevated hover:text-main',
                  )
                }
              >
                <Icon className="h-4 w-4" strokeWidth={1.75} />
                {item.label}
              </NavLink>
            )
          })}
        </nav>
      </aside>
    </div>
  )
}
