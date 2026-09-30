import { NavLink } from 'react-router-dom'

import { Logo } from '@/components/brand/Logo'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { primaryNav, settingsNav, type NavItem } from './navigation'

export function NavRow({ item, onNavigate }: { item: NavItem; onNavigate?: () => void }) {
  const Icon = item.icon
  return (
    <NavLink
      to={item.path}
      end={item.end}
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          'group flex h-9 items-center gap-3 rounded-[10px] px-2 text-[14px] font-medium transition-colors',
          isActive ? 'bg-surface-elevated text-main' : 'text-muted hover:bg-surface-elevated/60 hover:text-main',
        )
      }
    >
      <span
        className={cn(
          'flex h-6 w-6 shrink-0 items-center justify-center rounded-[7px] bg-gradient-to-b text-white shadow-[inset_0_1px_0_rgb(255_255_255/0.25)]',
          item.tint,
        )}
      >
        <Icon className="h-3.5 w-3.5" strokeWidth={2.25} />
      </span>
      {item.label}
    </NavLink>
  )
}

export function AccountChip() {
  const { user } = useAuth()
  const initials = (user?.name ?? 'S')
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
  return (
    <NavLink
      to="/settings"
      className="flex items-center gap-3 rounded-2xl p-2 transition-colors hover:bg-surface-elevated/70"
    >
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#ff8a65] via-[#f5504a] to-[#c2185b] text-[13px] font-semibold text-white">
        {initials}
      </span>
      <span className="min-w-0">
        <span className="block truncate text-[13px] font-semibold text-main">{user?.name}</span>
        <span className="block truncate text-[12px] text-dim">{user?.email}</span>
      </span>
    </NavLink>
  )
}

export function Sidebar() {
  return (
    <aside className="glass hidden h-dvh w-[248px] shrink-0 flex-col border-r border-default lg:flex">
      <div className="flex h-16 shrink-0 items-center px-5">
        <Logo to="/dashboard" appearance="app" />
      </div>

      <nav className="min-h-0 flex-1 overflow-y-auto px-3 pb-4">
        <p className="mb-1.5 px-2 text-[11px] font-semibold text-dim">Workspace</p>
        <div className="space-y-0.5">
          {primaryNav.map((item) => (
            <NavRow key={item.path} item={item} />
          ))}
        </div>

        <p className="mt-6 mb-1.5 px-2 text-[11px] font-semibold text-dim">Account</p>
        <NavRow item={settingsNav} />
      </nav>

      <div className="shrink-0 border-t border-default p-3">
        <AccountChip />
      </div>
    </aside>
  )
}
