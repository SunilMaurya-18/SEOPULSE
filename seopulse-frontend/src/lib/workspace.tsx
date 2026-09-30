import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'

import { projectApi, type Project } from '@/api/projects'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'

const PROJECT_KEY = 'seopulse-project-id'

interface WorkspaceContextValue {
  project: Project
  projectId: number
  revision: number
  refresh: () => Promise<void>
  notifyDataChanged: () => void
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null)

async function resolveProject(): Promise<Project> {
  const existing = await projectApi.getProjects(0, 20)
  const list = existing.content ?? []

  const storedId = Number(localStorage.getItem(PROJECT_KEY))
  if (storedId && list.some((p) => p.id === storedId)) {
    return list.find((p) => p.id === storedId)!
  }

  if (list.length > 0) {
    localStorage.setItem(PROJECT_KEY, String(list[0].id))
    return list[0]
  }

  const created = await projectApi.createProject({
    name: 'Workspace',
    description: 'Default SEOPulse workspace',
  })
  localStorage.setItem(PROJECT_KEY, String(created.id))
  return created
}

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const queryClient = useQueryClient()
  const [project, setProject] = useState<Project | null>(null)
  const [revision, setRevision] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const next = await resolveProject()
      setProject(next)
    } catch (err) {
      console.error(err)
      setError('Unable to load workspace. Please try again.')
      setProject(null)
    } finally {
      setLoading(false)
    }
  }, [])

  const notifyDataChanged = useCallback(() => {
    setRevision((value) => value + 1)
    void queryClient.invalidateQueries({ queryKey: ['projects'] })
  }, [queryClient])

  useEffect(() => {
    if (!isAuthenticated) {
      setProject(null)
      setLoading(false)
      return
    }
    void refresh()
  }, [isAuthenticated, refresh])

  const value = useMemo(
    () =>
      project
        ? {
            project,
            projectId: project.id,
            revision,
            refresh,
            notifyDataChanged,
          }
        : null,
    [project, revision, refresh, notifyDataChanged],
  )

  if (!isAuthenticated) return <>{children}</>

  if (loading) return <PageSkeleton />

  if (error || !value) {
    return (
      <div className="mx-auto max-w-lg p-6">
        <Alert
          variant="error"
          title="Workspace unavailable"
          action={
            <Button size="sm" onClick={() => void refresh()}>
              Retry
            </Button>
          }
        >
          {error ?? 'No workspace could be loaded.'}
        </Alert>
      </div>
    )
  }

  return (
    <WorkspaceContext.Provider value={value}>
      {children}
    </WorkspaceContext.Provider>
  )
}

export function useWorkspace() {
  const ctx = useContext(WorkspaceContext)
  if (!ctx) throw new Error('useWorkspace must be used within WorkspaceProvider')
  return ctx
}
