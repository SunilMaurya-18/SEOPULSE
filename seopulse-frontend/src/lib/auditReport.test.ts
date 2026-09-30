import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'

import type { AuditPage, SeoIssue } from '@/api/audits'
import {
  buildAuditReportPayload,
  buildHtmlReport,
  downloadAuditReport,
} from '@/lib/auditReport'
import { audit, page } from '@/test/fixtures'
import { api, server } from '@/test/server'

function auditPage(id: number): AuditPage {
  return {
    id,
    auditId: 7,
    url: `https://example.com/p${id}`,
    status: 'CRAWLED',
    statusCode: 200,
    contentType: 'text/html',
    title: `Page ${id}`,
    metaDescription: null,
    canonicalUrl: null,
    wordCount: 300,
    h1Count: 1,
    imageCount: 2,
    imagesWithoutAlt: 1,
    internalLinkCount: 4,
    externalLinkCount: 0,
    depth: 1,
    finalUrl: null,
    redirectChain: null,
    skipReason: null,
    crawledAt: null,
    createdAt: '2026-01-01T00:00:00Z',
  }
}

function issue(overrides: Partial<SeoIssue> = {}): SeoIssue {
  return {
    id: 1,
    auditId: 7,
    auditPageId: 1,
    url: 'https://example.com/p1',
    ruleCode: 'MISSING_TITLE',
    severity: 'ERROR',
    message: 'Missing title',
    recommendation: 'Add a title',
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function useReportHandlers() {
  server.use(
    http.get(api('/projects/1/audits/7'), () =>
      HttpResponse.json(audit({ status: 'COMPLETED', score: 72, pagesCrawled: 3 })),
    ),
    http.get(api('/projects/1/audits/7/summary'), () =>
      HttpResponse.json({ status: 404, detail: 'Not found' }, { status: 404 }),
    ),
    http.get(api('/projects/1/audits/7/pages'), ({ request }) => {
      const pageNumber = Number(new URL(request.url).searchParams.get('page'))
      return pageNumber === 0
        ? HttpResponse.json(page([auditPage(1), auditPage(2)], 0, false))
        : HttpResponse.json(page([auditPage(3)], 1, true))
    }),
    http.get(api('/projects/1/audits/7/issues'), () => HttpResponse.json(page([issue()]))),
  )
}

describe('auditReport', () => {
  it('collects every page of results and tolerates a missing summary', async () => {
    useReportHandlers()

    const payload = await buildAuditReportPayload(1, 7)

    expect(payload.audit.score).toBe(72)
    expect(payload.summary).toBeNull()
    expect(payload.pages.map((p) => p.id)).toEqual([1, 2, 3])
    expect(payload.issues).toHaveLength(1)
  })

  it('escapes crawled content in the HTML report', () => {
    const html = buildHtmlReport({
      generatedAt: '2026-01-01T00:00:00Z',
      audit: audit({ status: 'COMPLETED', score: 90 }),
      summary: null,
      pages: [{ ...auditPage(1), title: '<script>alert(1)</script>' }],
      issues: [issue({ message: '"quoted" & <b>bold</b>' })],
    })

    expect(html).not.toContain('<script>alert(1)</script>')
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
    expect(html).toContain('&quot;quoted&quot; &amp; &lt;b&gt;bold&lt;/b&gt;')
    expect(html).toContain('Healthy')
  })

  it('downloads a JSON report with a filename derived from the site', async () => {
    useReportHandlers()
    const createObjectURL = vi.fn(() => 'blob:report')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL, revokeObjectURL }))
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    await downloadAuditReport(1, 7, 'json')

    const anchor = click.mock.contexts[0] as HTMLAnchorElement
    expect(anchor.download).toBe('seopulse-audit-7-example.com.json')
    expect(createObjectURL).toHaveBeenCalledOnce()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:report')
    vi.unstubAllGlobals()
  })
})
