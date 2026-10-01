import axios from './axios'

export type AuditStatus =
    | 'QUEUED'
    | 'CRAWLING'
    | 'ANALYZING'
    | 'COMPLETED'
    | 'FAILED'
    | 'CANCELLED'

const ACTIVE_AUDIT_STATUSES: ReadonlySet<AuditStatus> = new Set([
    'QUEUED',
    'CRAWLING',
    'ANALYZING',
])

export function isActiveAudit(status: AuditStatus): boolean {
    return ACTIVE_AUDIT_STATUSES.has(status)
}

export interface Audit {
    id: number
    websiteId: number
    websiteUrl: string
    status: AuditStatus
    score: number | null
    pagesCrawled: number
    pagesAnalyzed: number
    startedAt: string | null
    completedAt: string | null
    errorMessage: string | null
    createdAt: string
    triggeredBy?: 'MANUAL' | 'SCHEDULED'
    issueCount?: number | null
    errorCount?: number | null
    scoreVersion?: number | null
}

export type CategoryScores = Partial<Record<string, number>>

export interface AuditSummary {
    auditId: number
    websiteId?: number
    status?: AuditStatus
    score: number | null
    pagesCrawled: number
    pagesAnalyzed: number
    totalIssues: number
    errorCount: number
    warningCount: number
    infoCount: number
    categoryScores?: CategoryScores | null
    scoreVersion?: number | null
    detailsPurged?: boolean
}

export type AuditPageStatus =
    | 'QUEUED'
    | 'CRAWLING'
    | 'CRAWLED'
    | 'REDIRECT'
    | 'SKIPPED_ROBOTS'
    | 'TOO_LARGE'
    | 'FAILED'

export interface AuditPage {
    id: number
    auditId: number
    url: string
    status: AuditPageStatus
    statusCode: number | null
    contentType: string | null
    title: string | null
    metaDescription: string | null
    canonicalUrl: string | null
    wordCount: number | null
    h1Count: number
    imageCount: number
    imagesWithoutAlt: number
    internalLinkCount: number
    externalLinkCount: number
    depth: number
    finalUrl: string | null
    redirectChain: string[] | null
    skipReason: string | null
    crawledAt: string | null
    createdAt: string
}

export interface SeoIssue {
    id: number
    auditId: number
    auditPageId: number
    url: string
    ruleCode: string
    severity: string
    message: string
    recommendation: string
    createdAt: string
    category?: string | null
    fingerprint?: string | null
    ruleTitle?: string | null
    helpUrl?: string | null
}

export interface IssueChange {
    fingerprint: string
    ruleCode: string
    ruleTitle: string
    severity: string
    category: string
    url: string
    message: string
}

export interface AuditComparison {
    auditId: number
    baselineAuditId: number | null
    baselineCompletedAt: string | null
    score: number | null
    baselineScore: number | null
    scoreDelta: number | null
    pagesCrawled: number | null
    baselinePagesCrawled: number | null
    pagesDelta: number | null
    detailsAvailable: boolean
    newCount: number
    fixedCount: number
    persistingCount: number
    newBySeverity: Record<string, number>
    fixedBySeverity: Record<string, number>
    newIssues: IssueChange[]
    fixedIssues: IssueChange[]
    newFingerprints: string[]
}

export interface TrendPoint {
    auditId: number
    completedAt: string | null
    score: number | null
    scoreVersion: number | null
    issueCount: number | null
    errorCount: number | null
    warningCount: number | null
    infoCount: number | null
    pagesCrawled: number | null
    categoryScores: CategoryScores | null
    triggeredBy: 'MANUAL' | 'SCHEDULED' | null
}

export type ReportStatus = 'PENDING' | 'GENERATING' | 'READY' | 'FAILED'

export interface Report {
    id: number
    auditId: number
    status: ReportStatus
    sizeBytes: number | null
    whiteLabel: boolean
    watermark: boolean
    errorMessage: string | null
    createdAt: string
    completedAt: string | null
}

export interface ReportShare {
    id: number
    auditId: number
    /** Only present right after the link is created. */
    url: string | null
    expiresAt: string
    revokedAt: string | null
    active: boolean
    viewCount: number
    lastViewedAt: string | null
    createdAt: string
}

export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}

export const auditApi = {
    getAudits: async (
        projectId: number,
        websiteId: number,
        page = 0,
        size = 10,
    ) => {
        const response = await axios.get<PageResponse<Audit>>(
            `/projects/${projectId}/audits`,
            {
                params: {
                    websiteId,
                    page,
                    size,
                },
            },
        )

        return response.data
    },

    createAudit: async (
        projectId: number,
        websiteId: number,
    ) => {
        const response = await axios.post<Audit>(
            `/projects/${projectId}/audits`,
            null,
            {
                params: {
                    websiteId,
                },
            },
        )

        return response.data
    },

    getAudit: async (
        projectId: number,
        auditId: number,
    ) => {
        const response = await axios.get<Audit>(
            `/projects/${projectId}/audits/${auditId}`,
        )

        return response.data
    },

    cancelAudit: async (
        projectId: number,
        auditId: number,
    ) => {
        const response = await axios.post<Audit>(
            `/projects/${projectId}/audits/${auditId}/cancel`,
        )

        return response.data
    },

    getSummary: async (
        projectId: number,
        auditId: number,
    ) => {
        const response = await axios.get<AuditSummary>(
            `/projects/${projectId}/audits/${auditId}/summary`,
        )

        return response.data
    },

    getPages: async (
        projectId: number,
        auditId: number,
        page = 0,
        size = 20,
    ) => {
        const response = await axios.get<PageResponse<AuditPage>>(
            `/projects/${projectId}/audits/${auditId}/pages`,
            {
                params: {
                    page,
                    size,
                },
            },
        )

        return response.data
    },

    getIssues: async (
        projectId: number,
        auditId: number,
        page = 0,
        size = 10,
        severity?: string,
        ruleCode?: string,
    ) => {
        const response = await axios.get<PageResponse<SeoIssue>>(
            `/projects/${projectId}/audits/${auditId}/issues`,
            {
                params: {
                    page,
                    size,
                    ...(severity ? { severity } : {}),
                    ...(ruleCode ? { ruleCode } : {}),
                },
            },
        )

        return response.data
    },

    emailReport: async (
        projectId: number,
        auditId: number,
        payload: { recipients?: string[]; note?: string } = {},
    ) => {
        const response = await axios.post<{ recipients: number }>(
            `/projects/${projectId}/audits/${auditId}/email`,
            payload,
        )
        return response.data.recipients
    },

    /** Defaults to the previous completed audit of the same website. */
    compare: async (projectId: number, auditId: number, baselineId?: number) => {
        const response = await axios.get<AuditComparison>(
            `/projects/${projectId}/audits/${auditId}/compare`,
            { params: baselineId ? { baseline: baselineId } : {} },
        )
        return response.data
    },

    trend: async (projectId: number, websiteId: number, limit = 30) => {
        const response = await axios.get<TrendPoint[]>(
            `/projects/${projectId}/websites/${websiteId}/trend`,
            { params: { limit } },
        )
        return response.data
    },

    requestReport: async (projectId: number, auditId: number) => {
        const response = await axios.post<Report>(`/projects/${projectId}/audits/${auditId}/reports`)
        return response.data
    },

    listReports: async (projectId: number, auditId: number) => {
        const response = await axios.get<Report[]>(`/projects/${projectId}/audits/${auditId}/reports`)
        return response.data
    },

    reportDownloadUrl: async (projectId: number, auditId: number, reportId: number) => {
        const response = await axios.post<{ url: string; expiresAt: string }>(
            `/projects/${projectId}/audits/${auditId}/reports/${reportId}/download-url`,
        )
        return response.data
    },

    createShare: async (projectId: number, auditId: number, expiresInDays?: number) => {
        const response = await axios.post<ReportShare>(
            `/projects/${projectId}/audits/${auditId}/shares`,
            expiresInDays ? { expiresInDays } : {},
        )
        return response.data
    },

    listShares: async (projectId: number, auditId: number) => {
        const response = await axios.get<ReportShare[]>(`/projects/${projectId}/audits/${auditId}/shares`)
        return response.data
    },

    revokeShare: async (projectId: number, auditId: number, shareId: number) => {
        await axios.delete(`/projects/${projectId}/audits/${auditId}/shares/${shareId}`)
    },
}