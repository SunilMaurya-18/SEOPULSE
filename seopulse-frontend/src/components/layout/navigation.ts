import { Activity, FileSearch, FileWarning, Gauge, Globe, Settings, type LucideIcon } from 'lucide-react'

export interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  tint: string
  end?: boolean
}

export const primaryNav: NavItem[] = [
  { label: 'Overview', path: '/dashboard', icon: Gauge, tint: 'from-[#ff6b5f] to-[#f5504a]', end: true },
  { label: 'Websites', path: '/websites', icon: Globe, tint: 'from-[#4aa8ff] to-[#0a84ff]' },
  { label: 'Audits', path: '/audits', icon: Activity, tint: 'from-[#4ee37a] to-[#28b14c]' },
  { label: 'Issues', path: '/issues', icon: FileWarning, tint: 'from-[#ffb340] to-[#ff9500]' },
  { label: 'Pages', path: '/pages', icon: FileSearch, tint: 'from-[#c58cff] to-[#9f5cf0]' },
]

export const settingsNav: NavItem = {
  label: 'Settings',
  path: '/settings',
  icon: Settings,
  tint: 'from-[#a1a1a6] to-[#6e6e73]',
}
