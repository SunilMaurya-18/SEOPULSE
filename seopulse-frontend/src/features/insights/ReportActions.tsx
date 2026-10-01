import { useEffect, useRef, useState } from 'react'
import { Check, Copy, FileDown, Link2, Trash2 } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'

import { auditApi, type ReportShare } from '@/api/audits'
import { getErrorMessage } from '@/api/errors'
import { apiUrl } from '@/api/publicReports'
import { useAuditReports, useAuditShares } from '@/api/queries/insights'
import { queryKeys } from '@/api/queries/keys'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { formatDateTime } from '@/lib/format'
import { useToast } from '@/lib/toast'

interface ReportActionsProps {
  projectId: number
  auditId: number
  disabled?: boolean
}

/** Server-rendered PDF and revocable share links. The client-side HTML/JSON export stays alongside. */
export function ReportActions({ projectId, auditId, disabled = false }: ReportActionsProps) {
  const { pushToast } = useToast()
  const queryClient = useQueryClient()
  const reports = useAuditReports(projectId, auditId, !disabled)
  const [busy, setBusy] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  const waitingFor = useRef<number | null>(null)

  const latest = reports.data?.[0] ?? null
  const generating = latest?.status === 'PENDING' || latest?.status === 'GENERATING'

  useEffect(() => {
    if (!latest || waitingFor.current !== latest.id) return
    if (latest.status === 'READY') {
      waitingFor.current = null
      pushToast({ tone: 'success', title: 'PDF ready', description: 'Your report is ready to download.' })
    } else if (latest.status === 'FAILED') {
      waitingFor.current = null
      pushToast({ tone: 'error', title: 'PDF failed', description: latest.errorMessage ?? 'The report could not be generated.' })
    }
  }, [latest, pushToast])

  async function generate() {
    setBusy(true)
    try {
      const report = await auditApi.requestReport(projectId, auditId)
      waitingFor.current = report.id
      await queryClient.invalidateQueries({ queryKey: queryKeys.auditReports(projectId, auditId) })
    } catch (err) {
      pushToast({ tone: 'error', title: 'PDF unavailable', description: getErrorMessage(err, 'Unable to start the PDF report.') })
    } finally {
      setBusy(false)
    }
  }

  async function download(reportId: number) {
    setBusy(true)
    try {
      const signed = await auditApi.reportDownloadUrl(projectId, auditId, reportId)
      window.location.assign(apiUrl(signed.url))
    } catch (err) {
      pushToast({ tone: 'error', title: 'Download failed', description: getErrorMessage(err, 'Unable to download the PDF.') })
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      {latest?.status === 'READY' ? (
        <Button size="sm" variant="secondary" loading={busy} disabled={disabled} onClick={() => void download(latest.id)}>
          <FileDown className="h-3.5 w-3.5" />
          Download PDF
        </Button>
      ) : (
        <Button
          size="sm"
          variant="secondary"
          loading={busy || generating}
          disabled={disabled}
          onClick={() => void generate()}
        >
          {!generating && <FileDown className="h-3.5 w-3.5" />}
          {generating ? 'Generating PDF…' : latest?.status === 'FAILED' ? 'Retry PDF' : 'Generate PDF'}
        </Button>
      )}
      <Button size="sm" variant="secondary" disabled={disabled} onClick={() => setShareOpen(true)}>
        <Link2 className="h-3.5 w-3.5" />
        Share
      </Button>
      {shareOpen && <ShareDialog projectId={projectId} auditId={auditId} onClose={() => setShareOpen(false)} />}
    </>
  )
}

const EXPIRY_OPTIONS = [7, 30, 90, 365]

function ShareDialog({ projectId, auditId, onClose }: { projectId: number; auditId: number; onClose: () => void }) {
  const { pushToast } = useToast()
  const queryClient = useQueryClient()
  const shares = useAuditShares(projectId, auditId)
  const [days, setDays] = useState(30)
  const [creating, setCreating] = useState(false)
  const [created, setCreated] = useState<ReportShare | null>(null)
  const [copied, setCopied] = useState(false)

  const refresh = () => queryClient.invalidateQueries({ queryKey: queryKeys.auditShares(projectId, auditId) })

  async function create() {
    setCreating(true)
    try {
      const share = await auditApi.createShare(projectId, auditId, days)
      setCreated(share)
      setCopied(false)
      await refresh()
    } catch (err) {
      pushToast({ tone: 'error', title: 'Could not create link', description: getErrorMessage(err, 'Unable to create a share link.') })
    } finally {
      setCreating(false)
    }
  }

  async function copy(url: string) {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
    } catch {
      pushToast({ tone: 'error', title: 'Copy failed', description: 'Select the link and copy it manually.' })
    }
  }

  async function revoke(share: ReportShare) {
    try {
      await auditApi.revokeShare(projectId, auditId, share.id)
      if (created?.id === share.id) setCreated(null)
      pushToast({ tone: 'success', title: 'Link revoked', description: 'Anyone opening it now gets "not found".' })
      await refresh()
    } catch (err) {
      pushToast({ tone: 'error', title: 'Revoke failed', description: getErrorMessage(err, 'Unable to revoke the link.') })
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Share report"
      description="Anyone with the link can view this audit's report and download the PDF, without signing in. Revoke it any time."
      size="lg"
    >
      <div className="flex flex-wrap items-end gap-3">
        <label className="text-[13px] text-muted">
          <span className="mb-1 block font-medium">Link expires after</span>
          <select
            value={days}
            onChange={(event) => setDays(Number(event.target.value))}
            className="h-9 rounded-[var(--sp-field-radius,0.5rem)] border border-default bg-[var(--sp-field-bg)] px-3 text-sm text-main"
          >
            {EXPIRY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option} days
              </option>
            ))}
          </select>
        </label>
        <Button loading={creating} onClick={() => void create()}>
          <Link2 className="h-4 w-4" />
          Create link
        </Button>
      </div>

      {created?.url && (
        <div className="mt-4 flex items-center gap-2 rounded-2xl bg-accent-surface p-3">
          <input
            readOnly
            value={created.url}
            aria-label="Share link"
            onFocus={(event) => event.target.select()}
            className="min-w-0 flex-1 bg-transparent text-sm text-main outline-none"
          />
          <Button size="sm" onClick={() => void copy(created.url as string)}>
            {copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
            {copied ? 'Copied' : 'Copy'}
          </Button>
        </div>
      )}
      {created?.url && (
        <p className="mt-2 text-xs text-dim">Copy it now: for security the full link is only shown once.</p>
      )}

      <h3 className="mt-6 text-[13px] font-semibold text-main">Links for this audit</h3>
      {shares.isPending ? (
        <p className="mt-2 text-sm text-dim">Loading…</p>
      ) : !shares.data?.length ? (
        <p className="mt-2 text-sm text-dim">No share links yet.</p>
      ) : (
        <ul className="mt-2 max-h-64 divide-y divide-default overflow-y-auto rounded-2xl border border-default">
          {shares.data.map((share) => (
            <li key={share.id} className="flex items-center gap-3 px-3.5 py-2.5 text-[13px]">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-main">Created {formatDateTime(share.createdAt)}</span>
                  {share.active ? (
                    <Badge variant="success">active</Badge>
                  ) : (
                    <Badge>{share.revokedAt ? 'revoked' : 'expired'}</Badge>
                  )}
                </div>
                <p className="mt-0.5 text-xs text-dim">
                  {share.viewCount} view{share.viewCount === 1 ? '' : 's'}
                  {share.active ? ` · expires ${formatDateTime(share.expiresAt)}` : ''}
                </p>
              </div>
              {share.active && (
                <Button size="sm" variant="danger" onClick={() => void revoke(share)} aria-label="Revoke link">
                  <Trash2 className="h-3.5 w-3.5" />
                  Revoke
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
    </Modal>
  )
}
