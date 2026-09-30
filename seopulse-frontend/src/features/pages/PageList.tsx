import { useState } from 'react'
import { ChevronDown, ExternalLink } from 'lucide-react'

import type { AuditPage } from '@/api/audits'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { cn } from '@/lib/cn'
import { formatDateTime, pathOf } from '@/lib/format'

export function PageList({ pages }: { pages: AuditPage[] }) {
  const [expandedId, setExpandedId] = useState<number | null>(null)
  return (
    <ul className="px-3 pb-3">
      {pages.map((page) => (
        <PageRow
          key={page.id}
          page={page}
          expanded={expandedId === page.id}
          onToggle={() => setExpandedId(expandedId === page.id ? null : page.id)}
        />
      ))}
    </ul>
  )
}

function httpTone(code: number | null) {
  if (code === null) return 'bg-surface-elevated text-dim'
  if (code >= 200 && code < 300) return 'bg-success-surface text-success'
  if (code >= 400) return 'bg-critical-surface text-critical'
  return 'bg-warning-surface text-warning'
}

function PageRow({ page, expanded, onToggle }: { page: AuditPage; expanded: boolean; onToggle: () => void }) {
  const crawled = page.status === 'CRAWLED'
  return (
    <li className={cn('rounded-2xl transition-colors', expanded ? 'bg-surface-low dark:bg-surface-elevated/40' : 'hover:bg-surface-elevated/40')}>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={expanded}
        aria-label={`Toggle details for ${page.url}`}
        className="flex w-full items-center gap-4 px-3 py-3 text-left"
      >
        <span
          className={cn(
            'num flex h-11 w-11 shrink-0 items-center justify-center rounded-[13px] text-[13px] font-bold',
            httpTone(page.statusCode),
          )}
        >
          {page.statusCode ?? '—'}
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-main" title={page.url}>
            {pathOf(page.url)}
          </p>
          <p className="mt-0.5 truncate text-xs text-dim">
            {page.finalUrl
              ? `→ ${page.finalUrl}`
              : page.skipReason
                ? page.skipReason
                : crawled
                  ? page.title || 'No title detected'
                  : page.url}
          </p>
        </div>
        <div className="hidden items-center gap-1.5 md:flex">
          {crawled ? (
            <>
              <Signal label={`H1 ${page.h1Count}`} problem={page.h1Count !== 1} />
              <Signal label={`${page.wordCount ?? 0} words`} problem={(page.wordCount ?? 0) < 200} />
              <Signal label={`${page.internalLinkCount} links`} problem={page.internalLinkCount === 0} />
              {page.imagesWithoutAlt > 0 && <Signal label={`${page.imagesWithoutAlt} missing alt`} problem />}
            </>
          ) : (
            <StatusBadge status={page.status} />
          )}
        </div>
        <ChevronDown className={cn('h-4 w-4 shrink-0 text-dim transition-transform', expanded && 'rotate-180')} />
      </button>
      {expanded && (
        <div className="px-3 pb-4">
          <div className="rounded-2xl bg-surface p-5 card-shadow dark:bg-surface-elevated/60">
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <StatusBadge status={page.status} />
              <a
                href={page.url}
                target="_blank"
                rel="noreferrer"
                className="inline-flex min-w-0 items-center gap-1.5 text-xs font-medium text-accent hover:opacity-75"
              >
                <span className="truncate">{page.url}</span>
                <ExternalLink className="h-3 w-3 shrink-0" />
              </a>
            </div>
            {crawled ? <PageDetails page={page} /> : <CrawlOutcomeDetails page={page} />}
          </div>
        </div>
      )}
    </li>
  )
}

function Signal({ label, problem }: { label: string; problem: boolean }) {
  return (
    <span
      className={cn(
        'rounded-full px-2.5 py-1 text-[11px] font-semibold whitespace-nowrap',
        problem ? 'bg-warning-surface text-warning' : 'bg-surface-elevated text-muted',
      )}
    >
      {label}
    </span>
  )
}

function PageDetails({ page }: { page: AuditPage }) {
  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
      <DetailGroup
        title="Metadata"
        items={[
          ['Title', page.title || 'Not detected'],
          ['Meta description', page.metaDescription || 'Not detected'],
          ['Canonical', page.canonicalUrl || 'Not detected'],
          ['Content type', page.contentType || 'Unknown'],
        ]}
      />
      <DetailGroup
        title="Content"
        items={[
          ['Word count', page.wordCount === null ? 'Unknown' : page.wordCount.toLocaleString()],
          ['H1 count', String(page.h1Count)],
          ['Images', String(page.imageCount)],
          ['Images without alt', String(page.imagesWithoutAlt)],
        ]}
      />
      <DetailGroup
        title="Links & crawl"
        items={[
          ['Internal links', String(page.internalLinkCount)],
          ['External links', String(page.externalLinkCount)],
          ['Depth', String(page.depth)],
          ['HTTP status', page.statusCode === null ? 'Unknown' : String(page.statusCode)],
        ]}
      />
      <DetailGroup
        title="Timestamps"
        items={[
          ['Crawled', formatDateTime(page.crawledAt)],
          ['Created', formatDateTime(page.createdAt)],
        ]}
      />
    </div>
  )
}

const OUTCOME_EXPLANATIONS: Partial<Record<AuditPage['status'], string>> = {
  REDIRECT:
    'This URL redirects. The destination is crawled as its own page when it belongs to the same site.',
  SKIPPED_ROBOTS:
    "The site's robots.txt disallows SEOPulseBot from this URL, so it was not requested.",
  TOO_LARGE:
    'The response exceeded the crawler size limit and was not analyzed.',
  FAILED:
    'The page could not be fetched (network error, timeout, or a blocked address).',
}

function CrawlOutcomeDetails({ page }: { page: AuditPage }) {
  const chain = page.redirectChain ?? []

  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
      <div>
        <h3 className="text-xs font-semibold tracking-[0.04em] text-dim uppercase">Crawl outcome</h3>
        <p className="mt-2 text-sm text-main">
          {OUTCOME_EXPLANATIONS[page.status] ?? 'No details available.'}
        </p>
        {page.skipReason && <p className="mt-2 text-sm text-warning">{page.skipReason}</p>}
      </div>
      {chain.length > 0 && (
        <div>
          <h3 className="text-xs font-semibold tracking-[0.04em] text-dim uppercase">Redirect chain</h3>
          <ol className="mt-2 space-y-1.5">
            {chain.map((hop, index) => (
              <li key={`${index}-${hop}`} className="break-all text-xs text-main">
                <span className="text-dim">{index + 1}.</span> {hop}
              </li>
            ))}
          </ol>
        </div>
      )}
    </div>
  )
}

function DetailGroup({ title, items }: { title: string; items: Array<[string, string]> }) {
  return (
    <div>
      <h3 className="text-xs font-semibold tracking-[0.04em] text-dim uppercase">{title}</h3>
      <dl className="mt-3 space-y-3">
        {items.map(([label, value]) => (
          <div key={label}>
            <dt className="text-xs text-muted">{label}</dt>
            <dd className="mt-0.5 break-words text-sm font-medium text-main">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
