import axios from './axios'

export interface Audit {
    id: number
    websiteId: number
    websiteUrl: string
    status:
        | 'QUEUED'
        | 'CRAWLING'
        | 'ANALYZING'
        | 'COMPLETED'
        | 'FAILED'
    score: number | null
    pagesCrawled: number
    pagesAnalyzed: number
    startedAt: string | null
    completedAt: string | null
    errorMessage: string | null
    createdAt: string
}

export interface AuditSummary {
    auditId: number
    score: number | null
    pagesCrawled: number
    pagesAnalyzed: number
    totalIssues: number
    errorCount: number
    warningCount: number
    infoCount: number
}

export interface AuditPage {
    id: number
    auditId: number
    url: string
    status:
        | 'QUEUED'
        | 'CRAWLING'
        | 'ANALYZING'
        | 'COMPLETED'
        | 'FAILED'
        | string
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
}