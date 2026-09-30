import { X } from 'lucide-react'

import { Logo } from '@/components/brand/Logo'
import { AccountChip, NavRow } from './Sidebar'
import { primaryNav, settingsNav } from './navigation'

interface MobileSidebarProps {
  open: boolean
  onClose: () => void
}

export function MobileSidebar({ open, onClose }: MobileSidebarProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <button
        type="button"
        aria-label="Close navigation"
        onClick={onClose}
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
      />
      <aside className="glass relative flex h-full w-72 max-w-[85vw] flex-col border-r border-default shadow-[var(--sp-overlay-shadow)]">
        <div className="flex h-16 items-center justify-between px-5">
          <Logo to="/dashboard" onClick={onClose} appearance="app" />
          <button
            type="button"
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-full bg-surface-elevated text-muted hover:text-main"
            aria-label="Close navigation"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <nav className="flex-1 space-y-0.5 px-3">
          {primaryNav.map((item) => (
            <NavRow key={item.path} item={item} onNavigate={onClose} />
          ))}
          <div className="pt-4">
            <NavRow item={settingsNav} onNavigate={onClose} />
          </div>
        </nav>
        <div className="border-t border-default p-3">
          <AccountChip />
        </div>
      </aside>
    </div>
  )
}
