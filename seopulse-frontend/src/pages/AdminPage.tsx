import { useState, type ReactNode } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Activity, AlertOctagon, Building2, Globe, Mail, ShieldAlert, UserPlus, Users } from 'lucide-react'
import { Navigate } from 'react-router-dom'

import { adminApi } from '@/api/admin'
import type { PageResponse } from '@/api/audits'
import { getErrorMessage } from '@/api/errors'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { StatTile } from '@/components/ui/StatTile'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/cn'
import { formatDateTime, hostOf } from '@/lib/format'
import { useToast } from '@/lib/toast'

type Tab = 'users' | 'workspaces' | 'failed'

const tabs: { id: Tab; label: string }[] = [
  { id: 'users', label: 'Users' },
  { id: 'workspaces', label: 'Workspaces' },
  { id: 'failed', label: 'Failed audits' },
]

export function AdminPage() {
  const { user } = useAuth()
  const [tab, setTab] = useState<Tab>('users')

  if (user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />

  return (
    <div className="space-y-8">
      <header>
        <p className="text-[13px] font-semibold text-accent">Platform</p>
        <h1 className="text-large-title mt-1 text-main">Admin</h1>
        <p className="mt-1 text-[15px] text-muted">Sign-ups, workspaces and audit health across SEOPulse.</p>
      </header>

      <StatsOverview />

      <section>
        <div role="tablist" aria-label="Admin views" className="mb-4 flex gap-1 rounded-full bg-surface-low p-1 sm:w-fit dark:bg-surface-elevated/60">
          {tabs.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={tab === item.id}
              onClick={() => setTab(item.id)}
              className={cn(
                'flex-1 rounded-full px-4 py-1.5 text-[13px] font-semibold transition-colors sm:flex-none',
                tab === item.id ? 'bg-surface text-main shadow-sm' : 'text-muted hover:text-main',
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
        {tab === 'users' && <UsersTable />}
        {tab === 'workspaces' && <WorkspacesTable />}
        {tab === 'failed' && <FailedAuditsTable />}
      </section>
    </div>
  )
}

function StatsOverview() {
  const stats = useQuery({ queryKey: ['admin', 'stats'], queryFn: adminApi.stats, refetchInterval: 60_000 })

  if (stats.isPending) return <PageSkeleton />
  if (stats.isError) {
    return <Alert variant="error" title="Could not load stats">{getErrorMessage(stats.error, 'Please try again.')}</Alert>
  }
  const s = stats.data
  const plans = Object.entries(s.workspacesByPlan)

  return (
    <section className="space-y-4">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatTile tint="blue" icon={<Users />} label="Users" value={s.users.toLocaleString()} caption={`${s.verifiedUsers.toLocaleString()} verified`} />
        <StatTile tint="green" icon={<UserPlus />} label="New users" value={s.usersLast7Days.toLocaleString()} caption={`7 days · ${s.usersLast30Days.toLocaleString()} in 30 days`} />
        <StatTile tint="purple" icon={<Building2 />} label="Workspaces" value={s.organizations.toLocaleString()} caption={`${s.websites.toLocaleString()} websites`} />
        <StatTile tint="teal" icon={<Mail />} label="Newsletter" value={s.newsletterSubscribers.toLocaleString()} caption="Confirmed subscribers" />
        <StatTile tint="indigo" icon={<Globe />} label="Audits" value={s.audits.toLocaleString()} caption={`${s.auditsLast24Hours.toLocaleString()} in 24 hours`} />
        <StatTile tint="orange" icon={<Activity />} label="Running now" value={s.activeAudits.toLocaleString()} caption="Queued or crawling" />
        <StatTile
          tint="red"
          icon={<AlertOctagon />}
          label="Failed"
          value={s.failedAuditsLast24Hours.toLocaleString()}
          caption="Audits in 24 hours"
          valueClassName={s.failedAuditsLast24Hours > 0 ? 'text-critical' : undefined}
        />
        <StatTile
          tint="coral"
          icon={<ShieldAlert />}
          label="Plans"
          value={plans.length === 0 ? '—' : plans.map(([code, n]) => `${n} ${code.toLowerCase()}`).join(' · ')}
          valueClassName="text-[15px]"
        />
      </div>
    </section>
  )
}

function SearchBox({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder: string }) {
  return (
    <div className="mb-3 max-w-sm">
      <Input id="admin-search" label="Search" value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
    </div>
  )
}

function UsersTable() {
  const { pushToast } = useToast()
  const queryClient = useQueryClient()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const users = useQuery({
    queryKey: ['admin', 'users', q, page],
    queryFn: () => adminApi.users(q, page),
    placeholderData: keepPreviousData,
  })
  const unlock = useMutation({
    mutationFn: adminApi.unlockUser,
    onSuccess: () => {
      pushToast({ tone: 'success', title: 'Account unlocked' })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (err) => pushToast({ tone: 'error', title: 'Unlock failed', description: getErrorMessage(err, 'Please try again.') }),
  })

  return (
    <>
      <SearchBox value={q} onChange={(value) => { setQ(value); setPage(0) }} placeholder="Name or email" />
      <DataTable
        query={users}
        page={page}
        onPage={setPage}
        empty="No users match."
        head={['User', 'Status', 'Workspaces', 'Joined', '']}
        row={(u) => [
          <div key="u" className="min-w-0">
            <p className="truncate font-medium text-main">{u.name}</p>
            <p className="truncate text-xs text-dim">{u.email}</p>
          </div>,
          <div key="s" className="flex flex-wrap gap-1">
            {u.role === 'ADMIN' && <Badge variant="accent">Admin</Badge>}
            <Badge variant={u.emailVerified ? 'success' : 'warning'}>{u.emailVerified ? 'Verified' : 'Unverified'}</Badge>
            {u.googleLinked && <Badge variant="info">Google</Badge>}
            {u.locked && <Badge variant="critical">Locked</Badge>}
          </div>,
          <span key="w" className="font-tabular">{u.workspaces}</span>,
          <span key="j" className="text-xs text-muted">{formatDateTime(u.createdAt)}</span>,
          u.locked ? (
            <Button key="a" size="sm" variant="secondary" loading={unlock.isPending && unlock.variables === u.id} onClick={() => unlock.mutate(u.id)}>
              Unlock
            </Button>
          ) : null,
        ]}
        rowKey={(u) => u.id}
      />
    </>
  )
}

function WorkspacesTable() {
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const orgs = useQuery({
    queryKey: ['admin', 'organizations', q, page],
    queryFn: () => adminApi.organizations(q, page),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <SearchBox value={q} onChange={(value) => { setQ(value); setPage(0) }} placeholder="Workspace name" />
      <DataTable
        query={orgs}
        page={page}
        onPage={setPage}
        empty="No workspaces match."
        head={['Workspace', 'Plan', 'Members', 'Websites', 'Audits this month', 'Created']}
        row={(o) => [
          <span key="n" className="font-medium text-main">{o.name}</span>,
          <div key="p" className="flex flex-wrap gap-1">
            <Badge variant={o.plan && o.plan !== 'FREE' ? 'accent' : 'neutral'}>{o.plan ?? 'None'}</Badge>
            {o.subscriptionStatus && o.subscriptionStatus !== 'ACTIVE' && (
              <Badge variant="warning">{o.subscriptionStatus.toLowerCase().replace('_', ' ')}</Badge>
            )}
          </div>,
          <span key="m" className="font-tabular">{o.members}</span>,
          <span key="w" className="font-tabular">{o.websites}</span>,
          <span key="a" className="font-tabular">{o.auditsThisMonth}</span>,
          <span key="c" className="text-xs text-muted">{formatDateTime(o.createdAt)}</span>,
        ]}
        rowKey={(o) => o.id}
      />
    </>
  )
}

function FailedAuditsTable() {
  const [page, setPage] = useState(0)
  const audits = useQuery({
    queryKey: ['admin', 'failed-audits', page],
    queryFn: () => adminApi.failedAudits(page),
    placeholderData: keepPreviousData,
  })

  return (
    <DataTable
      query={audits}
      page={page}
      onPage={setPage}
      empty="No failed audits. Nice."
      head={['Audit', 'Workspace', 'Error', 'Attempts', 'Failed']}
      row={(a) => [
        <div key="a" className="min-w-0">
          <p className="truncate font-medium text-main">{hostOf(a.websiteUrl)}</p>
          <p className="text-xs text-dim">#{a.id}</p>
        </div>,
        <span key="o" className="text-muted">{a.organizationName ?? '—'}</span>,
        <p key="e" className="line-clamp-2 max-w-md text-xs text-muted" title={a.errorMessage ?? undefined}>
          {a.errorMessage ?? 'No message'}
        </p>,
        <span key="r" className="font-tabular">{a.retryCount + 1}</span>,
        <span key="f" className="text-xs text-muted">{formatDateTime(a.completedAt ?? a.createdAt)}</span>,
      ]}
      rowKey={(a) => a.id}
    />
  )
}

function DataTable<T>({
  query,
  page,
  onPage,
  empty,
  head,
  row,
  rowKey,
}: {
  query: { data?: PageResponse<T>; isPending: boolean; isError: boolean; error: unknown; isFetching: boolean }
  page: number
  onPage: (page: number) => void
  empty: string
  head: string[]
  row: (item: T) => ReactNode[]
  rowKey: (item: T) => number
}) {
  if (query.isPending) return <PageSkeleton />
  if (query.isError || !query.data) {
    return <Alert variant="error" title="Could not load">{getErrorMessage(query.error, 'Please try again.')}</Alert>
  }
  const data = query.data

  return (
    <div className={cn('widget overflow-hidden transition-opacity', query.isFetching && 'opacity-70')}>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-[13px]">
          <thead className="border-b border-default text-xs text-dim">
            <tr>
              {head.map((label, i) => (
                <th key={i} scope="col" className="px-4 py-3 font-semibold whitespace-nowrap">
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-default">
            {data.content.length === 0 ? (
              <tr>
                <td colSpan={head.length} className="px-4 py-8 text-center text-muted">
                  {empty}
                </td>
              </tr>
            ) : (
              data.content.map((item) => (
                <tr key={rowKey(item)} className="align-middle">
                  {row(item).map((cell, i) => (
                    <td key={i} className="px-4 py-3">
                      {cell}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      {data.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-default px-4 py-2.5 text-xs text-dim">
          <span>
            Page {data.page + 1} of {data.totalPages} · {data.totalElements.toLocaleString()} total
          </span>
          <div className="flex gap-2">
            <Button size="sm" variant="ghost" disabled={data.first} onClick={() => onPage(page - 1)}>
              Previous
            </Button>
            <Button size="sm" variant="ghost" disabled={data.last} onClick={() => onPage(page + 1)}>
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
