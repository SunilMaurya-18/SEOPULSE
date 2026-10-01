import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import type { AuditComparison, SeoIssue, TrendPoint } from '@/api/audits'
import { IssueGroups } from '@/features/issues/IssueGroups'
import { renderWithProviders } from '@/test/render'
import { ComparisonPanel } from './ComparisonPanel'
import { TrendChart } from './TrendChart'

function point(auditId: number, score: number): TrendPoint {
  return {
    auditId,
    completedAt: `2026-09-${String(auditId).padStart(2, '0')}T10:00:00Z`,
    score,
    scoreVersion: 2,
    issueCount: 10,
    errorCount: 1,
    warningCount: 4,
    infoCount: 5,
    pagesCrawled: 20,
    categoryScores: null,
    triggeredBy: 'SCHEDULED',
  }
}

function comparison(overrides: Partial<AuditComparison> = {}): AuditComparison {
  return {
    auditId: 9,
    baselineAuditId: 8,
    baselineCompletedAt: '2026-09-01T10:00:00Z',
    score: 72,
    baselineScore: 80,
    scoreDelta: -8,
    pagesCrawled: 20,
    baselinePagesCrawled: 20,
    pagesDelta: 0,
    detailsAvailable: true,
    newCount: 1,
    fixedCount: 2,
    persistingCount: 5,
    newBySeverity: { ERROR: 1 },
    fixedBySeverity: { WARNING: 2 },
    newIssues: [
      {
        fingerprint: 'fp-new',
        ruleCode: 'MISSING_TITLE',
        ruleTitle: 'Missing title',
        severity: 'ERROR',
        category: 'CONTENT',
        url: 'https://example.com/about',
        message: 'Page has no title',
      },
    ],
    fixedIssues: [],
    newFingerprints: ['fp-new'],
    ...overrides,
  }
}

function issue(id: number, fingerprint: string, url: string): SeoIssue {
  return {
    id,
    auditId: 9,
    auditPageId: id,
    url,
    ruleCode: 'MISSING_TITLE',
    severity: 'ERROR',
    message: 'Page has no title',
    recommendation: 'Add a title',
    createdAt: '2026-09-02T10:00:00Z',
    fingerprint,
    ruleTitle: 'Missing title',
    helpUrl: 'https://developers.google.com/search/docs',
  } as SeoIssue
}

describe('TrendChart', () => {
  it('explains that a trend needs two audits', () => {
    renderWithProviders(<TrendChart points={[point(1, 70)]} />, { withAuth: false })
    expect(screen.getByText(/after two completed audits/i)).toBeInTheDocument()
  })

  it('links every audit on the chart', () => {
    renderWithProviders(<TrendChart points={[point(1, 70), point(2, 64), point(3, 81)]} currentAuditId={3} />, {
      withAuth: false,
    })
    expect(screen.getByRole('img', { name: /from 70 to 81/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Audit #2, score 64' })).toHaveAttribute('href', '/audits/2')
  })
})

describe('ComparisonPanel', () => {
  it('shows the score change and new/fixed counts', () => {
    renderWithProviders(<ComparisonPanel comparison={comparison()} />, { withAuth: false })
    expect(screen.getByText('Issues changed since last audit')).toBeInTheDocument()
    expect(screen.getByText('-8 score')).toBeInTheDocument()
    expect(screen.getByText('Missing title')).toBeInTheDocument()
    expect(screen.getByText('Nothing was fixed since the last audit.')).toBeInTheDocument()
  })

  it('explains the first audit has nothing to compare with', () => {
    renderWithProviders(<ComparisonPanel comparison={comparison({ baselineAuditId: null })} />, { withAuth: false })
    expect(screen.getByText(/first completed audit/i)).toBeInTheDocument()
  })

  it('falls back to scores only when details were purged', () => {
    renderWithProviders(<ComparisonPanel comparison={comparison({ detailsAvailable: false })} />, { withAuth: false })
    expect(screen.getByText(/removed by data retention/i)).toBeInTheDocument()
  })
})

describe('IssueGroups', () => {
  it('badges groups that contain new issues', () => {
    renderWithProviders(
      <IssueGroups
        issues={[issue(1, 'fp-new', 'https://example.com/a'), issue(2, 'fp-old', 'https://example.com/b')]}
        newFingerprints={new Set(['fp-new'])}
      />,
      { withAuth: false },
    )
    expect(screen.getByText('1 new')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Learn more/ })).toHaveAttribute(
      'href',
      'https://developers.google.com/search/docs',
    )
  })
})
