import { useState } from 'react'
import { Moon, Sun } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { PageHeader } from '@/components/ui/PageHeader'
import { getErrorMessage } from '@/api/errors'
import { useAuth } from '@/lib/auth'
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

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Settings"
        title="Workspace settings"
        description="Appearance and workspace preferences for SEOPulse."
      />

      <Card title="Account" description="Signed-in identity for API requests.">
        <div className="space-y-3 p-5">
          <div className="flex items-center justify-between gap-3 rounded border border-default bg-surface-low px-3 py-2.5">
            <span className="text-sm text-muted">Name</span>
            <span className="text-sm font-medium text-main">{user?.name}</span>
          </div>
          <div className="flex items-center justify-between gap-3 rounded border border-default bg-surface-low px-3 py-2.5">
            <span className="text-sm text-muted">Email</span>
            <span className="font-mono text-sm font-medium text-main">
              {user?.email}
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" size="sm" onClick={() => void handleLogout()}>
              Sign out
            </Button>
            <Button
              variant="secondary"
              size="sm"
              loading={signingOutAll}
              onClick={() => void handleLogoutAll()}
            >
              Sign out of all devices
            </Button>
          </div>
        </div>
      </Card>

      <Card title="Appearance" description="Choose light or dark telemetry theme.">
        <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-main">Theme</p>
            <p className="mt-1 text-sm text-muted">
              Light mode follows Telemetry Precision. Dark mode follows Obsidian Telemetry.
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant={theme === 'light' ? 'primary' : 'secondary'}
              onClick={() => setTheme('light')}
            >
              <Sun className="h-4 w-4" />
              Light
            </Button>
            <Button
              size="sm"
              variant={theme === 'dark' ? 'primary' : 'secondary'}
              onClick={() => setTheme('dark')}
            >
              <Moon className="h-4 w-4" />
              Dark
            </Button>
          </div>
        </div>
      </Card>

      <Card title="Active workspace" description="Current project context for all API requests.">
        <div className="space-y-3 p-5">
          <div className="flex items-center justify-between gap-3 rounded border border-default bg-surface-low px-3 py-2.5">
            <span className="text-sm text-muted">Workspace name</span>
            <span className="text-sm font-medium text-main">{project.name}</span>
          </div>
          <div className="flex items-center justify-between gap-3 rounded border border-default bg-surface-low px-3 py-2.5">
            <span className="text-sm text-muted">Project ID</span>
            <span className="font-mono text-sm font-medium text-main font-tabular">
              {project.id}
            </span>
          </div>
          <p className="text-xs text-dim">
            Project switching will appear when multi-project management is enabled.
          </p>
        </div>
      </Card>
    </div>
  )
}
