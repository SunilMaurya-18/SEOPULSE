import { useEffect, useRef, useState } from 'react'
import { BadgeCheck, Briefcase, Hash, LogOut, MonitorSmartphone, Moon, Sun } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { PageHeader } from '@/components/ui/PageHeader'
import { getErrorMessage } from '@/api/errors'
import { saasApi } from '@/api/saas'
import { SettingsGroup, SettingsRow } from '@/features/settings/SettingsGroup'
import { WorkspacePlan } from '@/features/settings/WorkspacePlan'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { useTheme } from '@/lib/theme'
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'

export function SettingsPage() {
  const { theme, setTheme } = useTheme()
  const { user, logout, logoutAll } = useAuth()
  const { project } = useWorkspace()
  const navigate = useNavigate()
  const { pushToast } = useToast()
  const [signingOutAll, setSigningOutAll] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()
  const [planKey, setPlanKey] = useState(0)
  const acceptedToken = useRef<string | null>(null)
  const inviteToken = searchParams.get('invite')

  useEffect(() => {
    if (!inviteToken || acceptedToken.current === inviteToken) return
    acceptedToken.current = inviteToken
    saasApi
      .acceptInvite(inviteToken)
      .then(() => {
        pushToast({ tone: 'success', title: 'Invitation accepted', description: 'You joined the team workspace.' })
        setPlanKey((value) => value + 1)
      })
      .catch((err) => {
        pushToast({
          tone: 'error',
          title: 'Invitation not accepted',
          description: getErrorMessage(err, 'The invitation link is invalid or has expired.'),
        })
      })
      .finally(() => {
        setSearchParams(
          (params) => {
            params.delete('invite')
            return params
          },
          { replace: true },
        )
      })
  }, [inviteToken, pushToast, setSearchParams])

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  async function handleLogoutAll() {
    setSigningOutAll(true)
    try {
      await logoutAll()
      navigate('/login', { replace: true })
    } catch (err) {
      pushToast({
        tone: 'error',
        title: 'Sign out failed',
        description: getErrorMessage(err, 'Unable to sign out other sessions.'),
      })
      setSigningOutAll(false)
    }
  }

  const initials = (user?.name ?? 'S')
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <PageHeader title="Settings" description="Your account, team, billing and appearance." />

      <div className="widget flex items-center gap-4 p-5 sm:p-6">
        <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#ff8a65] via-[#f5504a] to-[#c2185b] text-xl font-semibold text-white shadow-[0_10px_24px_-10px_rgb(245_80_74/0.8)]">
          {initials}
        </span>
        <div className="min-w-0">
          <p className="font-display truncate text-[22px] font-bold tracking-[-0.02em] text-main">{user?.name}</p>
          <p className="truncate text-[15px] text-muted">{user?.email}</p>
          {user?.emailVerified ? (
            <span className="mt-1.5 inline-flex items-center gap-1 text-xs font-semibold text-success">
              <BadgeCheck className="h-3.5 w-3.5" /> Verified
            </span>
          ) : user?.emailVerificationRequired ? (
            <span className="mt-1.5 inline-flex items-center gap-1 text-xs font-semibold text-warning">
              Email not verified
            </span>
          ) : null}
        </div>
      </div>

      <WorkspacePlan key={planKey} />

      <SettingsGroup title="Appearance">
        <SettingsRow
          icon={theme === 'dark' ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
          tint="from-[#6e7dff] to-[#4a5bff]"
          label="Theme"
          detail="Dark is the default."
        >
          <div role="radiogroup" aria-label="Theme" className="inline-flex rounded-[10px] bg-surface-elevated p-[3px]">
            {(['light', 'dark'] as const).map((mode) => (
              <button
                key={mode}
                type="button"
                role="radio"
                aria-checked={theme === mode}
                onClick={() => setTheme(mode)}
                className={cn(
                  'inline-flex h-7 items-center gap-1.5 rounded-[8px] px-3 text-[13px] font-medium capitalize transition-all',
                  theme === mode
                    ? 'bg-surface text-main shadow-[0_1px_3px_rgb(0_0_0/0.12)] dark:bg-surface-high'
                    : 'text-muted hover:text-main',
                )}
              >
                {mode === 'light' ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
                {mode}
              </button>
            ))}
          </div>
        </SettingsRow>
      </SettingsGroup>

      <SettingsGroup
        title="Workspace"
        footer="Project switching will appear when multi-project management is enabled."
      >
        <SettingsRow icon={<Briefcase className="h-4 w-4" />} tint="from-[#4aa8ff] to-[#0a84ff]" label="Name">
          <span className="text-[15px] text-dim">{project.name}</span>
        </SettingsRow>
        <SettingsRow icon={<Hash className="h-4 w-4" />} tint="from-[#a1a1a6] to-[#6e6e73]" label="Project ID">
          <span className="text-[15px] text-dim font-tabular">{project.id}</span>
        </SettingsRow>
      </SettingsGroup>

      <SettingsGroup title="Sessions">
        <button type="button" onClick={() => void handleLogout()} className="block w-full text-left hover:bg-surface-elevated/50">
          <SettingsRow icon={<LogOut className="h-4 w-4" />} tint="from-[#ff6b5f] to-[#f5504a]" label={<span className="text-accent">Sign out</span>} />
        </button>
        <button
          type="button"
          disabled={signingOutAll}
          onClick={() => void handleLogoutAll()}
          className="block w-full text-left hover:bg-surface-elevated/50 disabled:opacity-50"
        >
          <SettingsRow
            icon={<MonitorSmartphone className="h-4 w-4" />}
            tint="from-[#ff6b5f] to-[#ff3b30]"
            label={<span className="text-critical">Sign out of all devices</span>}
            detail="Ends every session, including this one."
          />
        </button>
      </SettingsGroup>
    </div>
  )
}
