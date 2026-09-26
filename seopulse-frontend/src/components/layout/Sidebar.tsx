import {
  Activity,
  FileSearch,
  FileWarning,
  Gauge,
  Globe,
  Settings,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/cn'

const navigation = [
  { label: 'Overview', path: '/dashboard', icon: Gauge, end: true },
  { label: 'Websites', path: '/websites', icon: Globe },
  { label: 'Audits', path: '/audits', icon: Activity },
  { label: 'Issues', path: '/issues', icon: FileWarning },
  { label: 'Pages', path: '/pages', icon: FileSearch },
]

export function Sidebar() {
  return (
    <aside className="hidden h-dvh w-60 shrink-0 flex-col border-r border-default bg-surface/95 backdrop-blur-md lg:flex">
      <div className="flex h-14 shrink-0 items-center border-b border-default px-4">
        <NavLink to="/dashboard" className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent text-white shadow-[0_8px_20px_-10px_var(--sp-accent)]">
            <Activity className="h-3.5 w-3.5" strokeWidth={2.5} />
          </div>
          <div className="flex flex-col leading-none">
            <span className="font-display text-sm font-semibold tracking-tight text-main">
              SEOPulse
            </span>
            <span className="mt-1 font-mono text-[9px] tracking-wider text-accent uppercase">
              Intelligence
            </span>
          </div>
        </NavLink>
      </div>

      <nav className="min-h-0 flex-1 space-y-0.5 overflow-y-auto px-2.5 py-4">
        <p className="mb-2 px-2.5 font-mono text-[10px] font-medium tracking-[0.12em] text-dim uppercase">
          Workflow
        </p>
        {navigation.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  'group flex h-10 items-center gap-2.5 rounded-lg px-2.5 text-sm transition-all',
                  isActive
                    ? 'bg-accent-surface font-medium text-accent shadow-[inset_3px_0_0_0_var(--sp-accent)]'
                    : 'text-muted hover:bg-surface-elevated hover:text-main',
                )
              }
            >
              <Icon className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              {item.label}
            </NavLink>
          )
        })}
      </nav>

      <div className="shrink-0 border-t border-default p-2.5">
        <NavLink
          to="/settings"
          className={({ isActive }) =>
            cn(
              'flex h-10 items-center gap-2.5 rounded-lg px-2.5 text-sm transition-colors',
              isActive
                ? 'bg-accent-surface font-medium text-accent'
                : 'text-muted hover:bg-surface-elevated hover:text-main',
            )
          }
        >
          <Settings className="h-4 w-4" strokeWidth={1.75} />
          Settings
        </NavLink>
      </div>
    </aside>
  )
}
