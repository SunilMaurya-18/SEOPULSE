import {
  auditApi,
  type Audit,
  type AuditPage,
  type AuditSummary,
  type SeoIssue,
} from '@/api/audits'

export type ReportFormat = 'html' | 'json'

export interface AuditReportPayload {
  generatedAt: string
  audit: Audit
  summary: AuditSummary | null
  pages: AuditPage[]
  issues: SeoIssue[]
}

async function fetchAllPages(
  projectId: number,
  auditId: number,
  maxItems = 500,
): Promise<AuditPage[]> {
  const size = 100
  let page = 0
  const items: AuditPage[] = []

  while (items.length < maxItems) {
    const response = await auditApi.getPages(projectId, auditId, page, size)
    const batch = response.content ?? []
    items.push(...batch)
    if (response.last || batch.length === 0) break
    page += 1
  }

  return items.slice(0, maxItems)
}

async function fetchAllIssues(
  projectId: number,
  auditId: number,
  maxItems = 1000,
): Promise<SeoIssue[]> {
  const size = 100
  let page = 0
  const items: SeoIssue[] = []

  while (items.length < maxItems) {
    const response = await auditApi.getIssues(
      projectId,
      auditId,
      page,
      size,
    )
    const batch = response.content ?? []
    items.push(...batch)
    if (response.last || batch.length === 0) break
    page += 1
  }

  return items.slice(0, maxItems)
}

export async function buildAuditReportPayload(
  projectId: number,
  auditId: number,
): Promise<AuditReportPayload> {
  const [audit, summary, pages, issues] = await Promise.all([
    auditApi.getAudit(projectId, auditId),
    auditApi.getSummary(projectId, auditId).catch(() => null),
    fetchAllPages(projectId, auditId),
    fetchAllIssues(projectId, auditId),
  ])

  return {
    generatedAt: new Date().toISOString(),
    audit,
    summary,
    pages,
    issues,
  }
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  return new Date(value).toLocaleString()
}

function scoreLabel(score: number | null) {
  if (score === null) return 'Pending'
  if (score >= 80) return 'Healthy'
  if (score >= 60) return 'Needs work'
  return 'Critical'
}

export function buildHtmlReport(payload: AuditReportPayload): string {
  const { audit, summary, pages, issues, generatedAt } = payload
  const score = summary?.score ?? audit.score
  const errorCount = summary?.errorCount ?? 0
  const warningCount = summary?.warningCount ?? 0
  const infoCount = summary?.infoCount ?? 0
  const totalIssues = summary?.totalIssues ?? issues.length

  const issueRows = issues
    .map(
      (issue) => `
      <tr>
        <td><span class="badge ${escapeHtml(issue.severity.toLowerCase())}">${escapeHtml(issue.severity)}</span></td>
        <td>${escapeHtml(issue.ruleCode)}</td>
        <td>${escapeHtml(issue.url)}</td>
        <td>${escapeHtml(issue.message)}</td>
        <td>${escapeHtml(issue.recommendation || '—')}</td>
      </tr>`,
    )
    .join('')

  const pageRows = pages
    .map(
      (page) => `
      <tr>
        <td>${escapeHtml(page.url)}</td>
        <td>${page.statusCode ?? '—'}</td>
        <td>${escapeHtml(page.title || '—')}</td>
        <td>${page.h1Count}</td>
        <td>${page.imagesWithoutAlt}/${page.imageCount}</td>
        <td>${page.wordCount ?? '—'}</td>
      </tr>`,
    )
    .join('')

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>SEOPulse Audit Report #${audit.id}</title>
  <style>
    :root {
      --ink: #0b1220;
      --muted: #475569;
      --line: #e2e8f0;
      --accent: #9f1239;
      --surface: #ffffff;
      --canvas: #f4f6f8;
      --success: #059669;
      --warning: #d97706;
      --critical: #b91c1c;
      --info: #0284c7;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Segoe UI", system-ui, sans-serif;
      color: var(--ink);
      background: var(--canvas);
      line-height: 1.5;
    }
    .wrap { max-width: 1080px; margin: 0 auto; padding: 32px 20px 64px; }
    .hero {
      background: linear-gradient(125deg, rgba(159,18,57,0.08), transparent 50%), var(--surface);
      border: 1px solid var(--line);
      border-radius: 16px;
      padding: 28px;
      margin-bottom: 20px;
    }
    h1 { margin: 0; font-size: 28px; letter-spacing: -0.02em; }
    .brand { color: var(--accent); font-size: 12px; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; }
    .meta { color: var(--muted); font-size: 14px; margin-top: 8px; word-break: break-all; }
    .grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-top: 20px; }
    .card {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 14px 16px;
    }
    .label { font-size: 11px; letter-spacing: 0.1em; text-transform: uppercase; color: var(--muted); }
    .value { font-size: 24px; font-weight: 700; margin-top: 6px; font-variant-numeric: tabular-nums; }
    h2 { font-size: 18px; margin: 28px 0 12px; }
    table { width: 100%; border-collapse: collapse; background: var(--surface); border: 1px solid var(--line); border-radius: 12px; overflow: hidden; }
    th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid var(--line); font-size: 13px; vertical-align: top; }
    th { background: #f8fafc; color: var(--muted); font-size: 11px; letter-spacing: 0.08em; text-transform: uppercase; }
    tr:last-child td { border-bottom: none; }
    .badge { display: inline-block; border-radius: 999px; padding: 2px 8px; font-size: 11px; font-weight: 700; text-transform: uppercase; }
    .error, .critical { background: #fef2f2; color: var(--critical); }
    .warning { background: #fffbeb; color: var(--warning); }
    .info { background: #f0f9ff; color: var(--info); }
    .footer { margin-top: 28px; color: var(--muted); font-size: 12px; }
    @media print {
      body { background: white; }
      .wrap { padding: 0; max-width: none; }
      .hero, .card, table { break-inside: avoid; }
    }
    @media (max-width: 800px) {
      .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
  </style>
</head>
<body>
  <div class="wrap">
    <section class="hero">
      <div class="brand">SEOPulse · Audit Report</div>
      <h1>Audit #${audit.id}</h1>
      <p class="meta">${escapeHtml(audit.websiteUrl)}</p>
      <p class="meta">Status: ${escapeHtml(audit.status)} · Generated ${escapeHtml(formatDate(generatedAt))}</p>
      <div class="grid">
        <div class="card"><div class="label">Health score</div><div class="value">${score ?? '—'}</div><div class="meta">${scoreLabel(score)}</div></div>
        <div class="card"><div class="label">Pages crawled</div><div class="value">${audit.pagesCrawled}</div></div>
        <div class="card"><div class="label">Pages analyzed</div><div class="value">${audit.pagesAnalyzed}</div></div>
        <div class="card"><div class="label">Total issues</div><div class="value">${totalIssues}</div></div>
      </div>
      <div class="grid" style="margin-top:12px">
        <div class="card"><div class="label">Errors</div><div class="value" style="color:var(--critical)">${errorCount}</div></div>
        <div class="card"><div class="label">Warnings</div><div class="value" style="color:var(--warning)">${warningCount}</div></div>
        <div class="card"><div class="label">Info</div><div class="value" style="color:var(--info)">${infoCount}</div></div>
        <div class="card"><div class="label">Completed</div><div class="value" style="font-size:16px;padding-top:8px">${escapeHtml(formatDate(audit.completedAt))}</div></div>
      </div>
    </section>

    <h2>SEO issues (${issues.length})</h2>
    ${
      issues.length === 0
        ? '<div class="card">No issues recorded for this audit.</div>'
        : `<table>
            <thead><tr><th>Severity</th><th>Rule</th><th>URL</th><th>Message</th><th>Recommendation</th></tr></thead>
            <tbody>${issueRows}</tbody>
          </table>`
    }

    <h2>Crawled pages (${pages.length})</h2>
    ${
      pages.length === 0
        ? '<div class="card">No pages recorded for this audit.</div>'
        : `<table>
            <thead><tr><th>URL</th><th>Status</th><th>Title</th><th>H1</th><th>Images w/o alt</th><th>Words</th></tr></thead>
            <tbody>${pageRows}</tbody>
          </table>`
    }

    <p class="footer">Generated by SEOPulse · Audit #${audit.id} · ${escapeHtml(formatDate(generatedAt))}</p>
  </div>
</body>
</html>`
}

function downloadBlob(filename: string, blob: Blob) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

function safeFilenamePart(value: string) {
  return value
    .replace(/^https?:\/\//, '')
    .replace(/[^a-zA-Z0-9._-]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 48)
}

export async function downloadAuditReport(
  projectId: number,
  auditId: number,
  format: ReportFormat = 'html',
): Promise<void> {
  const payload = await buildAuditReportPayload(projectId, auditId)
  const host = safeFilenamePart(payload.audit.websiteUrl || 'website')
  const base = `seopulse-audit-${payload.audit.id}-${host}`

  if (format === 'json') {
    downloadBlob(
      `${base}.json`,
      new Blob([JSON.stringify(payload, null, 2)], {
        type: 'application/json',
      }),
    )
    return
  }

  downloadBlob(
    `${base}.html`,
    new Blob([buildHtmlReport(payload)], { type: 'text/html;charset=utf-8' }),
  )
}
