import { useEffect, type ReactNode } from 'react'
import { AlertCircle, AlertTriangle, ExternalLink, FileDown, Info, Link2Off } from 'lucide-react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import axios from 'axios'

import { publicReportApi, type SharedReport, type SharedRuleGroup } from '@/api/publicReports'
import { Badge } from '@/components/ui/Badge'
import { ScoreRing } from '@/components/ui/ScoreRing'
import { PageSkeleton } from '@/components/ui/Skeleton'
import { CategoryScores } from '@/features/insights/CategoryScores'
import { formatDateTime, hostOf, pathOf } from '@/lib/format'

const HEX_COLOR = /^#[0-9a-fA-F]{6}$/
const LOGO = /^data:image\/(png|jpeg);base64,[A-Za-z0-9+/=]+$/

export function SharedReportPage() {
  const { token = '' } = useParams<{ token: string }>()
  const report = useQuery({
    queryKey: ['public-report', token],
    queryFn: () => publicReportApi.get(token),
    retry: false,
    enabled: token.length > 0,
  })

  useEffect(() => {
    const meta = document.createElement('meta')
    meta.name = 'robots'
    meta.content = 'noindex, nofollow'
    document.head.appendChild(meta)
    return () => meta.remove()
  }, [])

  useEffect(() => {
    if (report.data) document.title = `SEO report · ${report.data.websiteName || hostOf(report.data.websiteUrl)}`
  }, [report.data])

  if (report.isPending && token) {
    return (
      <div className="min-h-screen bg-canvas p-6">
        <PageSkeleton />
      </div>
    )
  }

  if (!report.data) {
    const notFound = !token || (axios.isAxiosError(report.error) && report.error.response?.status === 404)
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas p-6">
        <div className="widget max-w-md p-8 text-center">
          <Link2Off className="mx-auto h-10 w-10 text-dim" />
          <h1 className="text-headline mt-4 text-main">{notFound ? 'Report not available' : 'Something went wrong'}</h1>
          <p className="mt-2 text-sm text-muted">
            {notFound
              ? 'This share link has expired or was revoked. Ask the sender for a new link.'
              : 'We could not load this report. Please try again in a moment.'}
          </p>
        </div>
      </div>
    )
  }

  return <ReportView token={token} report={report.data} />
}

function ReportView({ token, report }: { token: string; report: SharedReport }) {
  const branding = report.branding
  const accent = branding?.brandColor && HEX_COLOR.test(branding.brandColor) ? branding.brandColor : null
  const logo = branding?.logoDataUrl && LOGO.test(branding.logoDataUrl) ? branding.logoDataUrl : null
  const preparedBy = branding?.companyName || 'SEOPulse'
  const delta =
    typeof report.score === 'number' && typeof report.previousScore === 'number' ? report.score - report.previousScore : null

  return (
    <div className="min-h-screen bg-canvas">
      <header className="border-b border-default bg-surface" style={accent ? { borderTop: `4px solid ${accent}` } : undefined}>
        <div className="mx-auto flex max-w-4xl items-center gap-3 px-5 py-4">
          {logo ? (
            <img src={logo} alt={preparedBy} className="h-9 max-w-[180px] object-contain" />
          ) : (
            <span className="font-display text-lg font-bold text-main">{preparedBy}</span>
          )}
          <span className="ml-auto text-xs text-dim">SEO audit report</span>
          <a
            href={publicReportApi.pdfUrl(token)}
            className="inline-flex h-8 items-center gap-1.5 rounded-full bg-accent px-3.5 text-[13px] font-semibold text-on-accent hover:bg-accent-hover"
            style={accent ? { backgroundColor: accent } : undefined}
          >
            <FileDown className="h-3.5 w-3.5" />
            Download PDF
          </a>
        </div>
      </header>

      <main className="mx-auto max-w-4xl space-y-6 px-5 py-8">
        {report.watermark && (
          <p className="rounded-2xl bg-surface-elevated px-4 py-2 text-center text-xs font-semibold text-muted">
            Generated with SEOPulse Free
          </p>
        )}

        <section className="widget flex flex-col gap-6 p-6 sm:flex-row sm:items-center sm:p-8">
          <ScoreRing score={report.score} size={140} strokeWidth={12} className="mx-auto shrink-0 sm:mx-0" />
          <div className="min-w-0 flex-1">
            <h1 className="text-large-title truncate text-main">{report.websiteName || hostOf(report.websiteUrl)}</h1>
            <a
              href={report.websiteUrl}
              target="_blank"
              rel="noreferrer noopener"
              className="mt-1 inline-flex max-w-full items-center gap-1.5 text-sm text-muted hover:text-accent"
            >
              <span className="truncate">{report.websiteUrl}</span>
              <ExternalLink className="h-3.5 w-3.5 shrink-0" />
            </a>
            {branding?.coverText && <p className="mt-3 text-sm text-muted">{branding.coverText}</p>}
            <div className="mt-4 flex flex-wrap gap-2 text-xs">
              <Badge>Audited {formatDateTime(report.completedAt)}</Badge>
              <Badge>{report.pagesCrawled} pages</Badge>
              {delta !== null && (
                <Badge variant={delta > 0 ? 'success' : delta < 0 ? 'critical' : 'neutral'}>
                  {delta > 0 ? `+${delta}` : delta} since previous audit
                </Badge>
              )}
              {typeof report.newIssues === 'number' && <Badge variant="critical">{report.newIssues} new issues</Badge>}
              {typeof report.fixedIssues === 'number' && <Badge variant="success">{report.fixedIssues} fixed</Badge>}
            </div>
          </div>
        </section>

        <section className="grid grid-cols-3 gap-3">
          <Count label="Errors" value={report.errorCount} icon={<AlertCircle className="h-4 w-4 text-critical" />} />
          <Count label="Warnings" value={report.warningCount} icon={<AlertTriangle className="h-4 w-4 text-warning" />} />
          <Count label="Notices" value={report.infoCount} icon={<Info className="h-4 w-4 text-info" />} />
        </section>

        <CategoryScores scores={report.categoryScores} />

        <section className="widget p-5 sm:p-6">
          <h2 className="text-headline text-main">Top issues</h2>
          {!report.detailsAvailable ? (
            <p className="mt-2 text-sm text-muted">Issue details for this audit are no longer available.</p>
          ) : report.topIssues.length === 0 ? (
            <p className="mt-2 text-sm text-muted">No issues were found. Great work.</p>
          ) : (
            <ul className="mt-4 space-y-3">
              {report.topIssues.map((group) => (
                <IssueRow key={group.ruleCode} group={group} />
              ))}
            </ul>
          )}
        </section>

        <footer className="pb-6 text-center text-xs text-dim">
          {branding?.companyName ? `Prepared by ${branding.companyName}` : 'Generated by SEOPulse'}
        </footer>
      </main>
    </div>
  )
}

function Count({ label, value, icon }: { label: string; value: number; icon: ReactNode }) {
  return (
    <div className="widget p-4">
      <div className="flex items-center gap-1.5 text-xs text-dim">
        {icon}
        {label}
      </div>
      <p className="mt-1 text-2xl font-semibold text-main font-tabular">{value.toLocaleString()}</p>
    </div>
  )
}

function IssueRow({ group }: { group: SharedRuleGroup }) {
  const severity = group.severity.toUpperCase()
  const variant = severity === 'ERROR' ? 'critical' : severity === 'WARNING' ? 'warning' : 'info'
  return (
    <li className="rounded-2xl border border-default p-4">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={variant}>{group.severity.toLowerCase()}</Badge>
        <span className="text-[15px] font-semibold text-main">{group.title}</span>
        <span className="ml-auto text-xs text-dim font-tabular">
          {group.count} page{group.count === 1 ? '' : 's'}
        </span>
      </div>
      {group.recommendation && <p className="mt-2 text-sm text-muted">{group.recommendation}</p>}
      {group.sampleUrls.length > 0 && (
        <ul className="mt-2 space-y-0.5">
          {group.sampleUrls.map((url) => (
            <li key={url} className="truncate text-xs text-dim" title={url}>
              {pathOf(url)}
            </li>
          ))}
        </ul>
      )}
      {group.helpUrl?.startsWith('https://') && (
        <a
          href={group.helpUrl}
          target="_blank"
          rel="noreferrer noopener"
          className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-accent hover:opacity-75"
        >
          Learn more
          <ExternalLink className="h-3 w-3" />
        </a>
      )}
    </li>
  )
}
