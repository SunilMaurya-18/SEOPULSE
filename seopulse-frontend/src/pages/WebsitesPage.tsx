import { useEffect, useState } from 'react'
import {
  ExternalLink,
  FileSearch,
  Globe,
  Plus,
  RefreshCw,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'

import { websiteApi, type Website } from '@/api/websites'
import { AddWebsiteModal } from '@/features/websites/components/AddWebsiteModal'
import { useToast } from '@/lib/toast'
import { useWorkspace } from '@/lib/workspace'

import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { NextStepBanner } from '@/components/ui/NextStepBanner'
import { PageHeader } from '@/components/ui/PageHeader'
import { TableSkeleton } from '@/components/ui/Skeleton'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { WorkflowRail } from '@/components/ui/WorkflowRail'
import {
  Table,
  TBody,
  TD,
  TH,
  THead,
  TR,
  TableToolbar,
} from '@/components/ui/Table'

export function WebsitesPage() {
  const { projectId, revision, notifyDataChanged } = useWorkspace()
  const { pushToast } = useToast()
  const navigate = useNavigate()
  const [websites, setWebsites] = useState<Website[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showAddModal, setShowAddModal] = useState(false)

  async function loadWebsites() {
    try {
      setLoading(true)
      setError(null)

      const response = await websiteApi.getWebsites(projectId, 0, 20)
      setWebsites(response.content ?? [])
    } catch (err) {
      console.error('Failed to load websites:', err)
      setError('Unable to load websites. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateWebsite(data: { name: string; url: string }) {
    const website = await websiteApi.createWebsite(projectId, data)
    notifyDataChanged()
    pushToast({
      tone: 'success',
      title: 'Website added',
      description: `${website.url} is ready to audit.`,
    })
    await loadWebsites()
    navigate(`/audits?websiteId=${website.id}`)
  }

  useEffect(() => {
    void loadWebsites()
  }, [projectId, revision])

  return (
    <>
      <div className="space-y-6">
        <PageHeader
          eyebrow="Workspace"
          title="Websites"
          description="Connect properties, then move straight into crawl and analysis."
          action={
            <Button onClick={() => setShowAddModal(true)}>
              <Plus className="h-4 w-4" />
              Add website
            </Button>
          }
        />

        <WorkflowRail phase={websites.length === 0 ? 'connect' : 'crawl'} />

        {websites.length === 0 ? (
          <NextStepBanner
            title="Add a website to begin"
            description="Paste a public URL. We’ll attach it to this workspace and unlock audits."
            actionLabel="Add website"
            icon={<Globe className="h-4 w-4" />}
            onAction={() => setShowAddModal(true)}
          />
        ) : (
          <NextStepBanner
            title="Ready to crawl"
            description="Your sites are connected. Run an audit to start collecting SEO telemetry."
            actionLabel="Go to audits"
            to={`/audits?websiteId=${websites[0].id}`}
            icon={<FileSearch className="h-4 w-4" />}
          />
        )}

        {error && (
          <Alert
            variant="error"
            title="Failed to load websites"
            action={
              <Button size="sm" variant="secondary" onClick={loadWebsites}>
                <RefreshCw className="h-3.5 w-3.5" />
                Retry
              </Button>
            }
          >
            {error}
          </Alert>
        )}

        <Card
          title="Connected websites"
          description="Websites available for SEO auditing."
          className="overflow-hidden"
        >
          {loading ? (
            <TableSkeleton rows={5} />
          ) : websites.length === 0 ? (
            <EmptyState
              icon={<Globe className="h-5 w-5" />}
              title="No websites yet"
              description="Add your first website to start running SEO audits."
              action={
                <Button onClick={() => setShowAddModal(true)}>
                  <Plus className="h-4 w-4" />
                  Add website
                </Button>
              }
            />
          ) : (
            <>
              <TableToolbar>
                <p className="font-mono text-[11px] text-muted">
                  {websites.length} website
                  {websites.length === 1 ? '' : 's'}
                </p>
                <Link to="/audits">
                  <Button size="sm" variant="ghost">
                    <FileSearch className="h-3.5 w-3.5" />
                    View audits
                  </Button>
                </Link>
              </TableToolbar>
              <Table>
                <THead>
                  <TR className="hover:bg-transparent">
                    <TH>Website</TH>
                    <TH>Status</TH>
                    <TH>Added</TH>
                    <TH className="text-right">Actions</TH>
                  </TR>
                </THead>
                <TBody>
                  {websites.map((website) => (
                    <TR key={website.id}>
                      <TD>
                        <div className="flex items-center gap-3">
                          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded border border-default bg-surface-low">
                            <Globe className="h-4 w-4 text-muted" />
                          </div>
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-main">
                              {website.name || website.url}
                            </p>
                            <p className="mt-0.5 truncate font-mono text-[10px] text-dim">
                              {website.url}
                            </p>
                          </div>
                        </div>
                      </TD>
                      <TD>
                        {website.status ? (
                          <StatusBadge status={website.status} />
                        ) : (
                          <span className="text-sm text-dim">—</span>
                        )}
                      </TD>
                      <TD mono>
                        {website.createdAt
                          ? new Date(website.createdAt).toLocaleDateString()
                          : '—'}
                      </TD>
                      <TD className="text-right">
                        <div className="inline-flex items-center gap-2">
                          <Link to="/audits">
                            <Button size="sm" variant="secondary">
                              Audits
                            </Button>
                          </Link>
                          <a
                            href={website.url}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1.5 px-2 text-sm font-medium text-muted hover:text-accent"
                          >
                            Open
                            <ExternalLink className="h-3.5 w-3.5" />
                          </a>
                        </div>
                      </TD>
                    </TR>
                  ))}
                </TBody>
              </Table>
            </>
          )}
        </Card>
      </div>

      <AddWebsiteModal
        open={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSubmit={handleCreateWebsite}
      />
    </>
  )
}
