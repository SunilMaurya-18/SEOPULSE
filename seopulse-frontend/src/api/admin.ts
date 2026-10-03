import api from './axios'
import type { PageResponse } from './audits'

export interface AdminStats {
  users: number
  usersLast7Days: number
  usersLast30Days: number
  verifiedUsers: number
  organizations: number
  websites: number
  audits: number
  auditsLast24Hours: number
  failedAuditsLast24Hours: number
  activeAudits: number
  newsletterSubscribers: number
  workspacesByPlan: Record<string, number>
}

export interface AdminUser {
  id: number
  name: string
  email: string
  role: string
  emailVerified: boolean
  googleLinked: boolean
  locked: boolean
  workspaces: number
  createdAt: string
}

export interface AdminOrganization {
  id: number
  name: string
  plan: string | null
  subscriptionStatus: string | null
  members: number
  websites: number
  auditsThisMonth: number
  createdAt: string
}

export interface AdminFailedAudit {
  id: number
  websiteUrl: string
  organizationId: number | null
  organizationName: string | null
  errorMessage: string | null
  retryCount: number
  createdAt: string
  completedAt: string | null
}

export const adminApi = {
  stats: () => api.get<AdminStats>('/admin/stats').then((r) => r.data),
  users: (q: string, page: number) =>
    api.get<PageResponse<AdminUser>>('/admin/users', { params: { q: q || undefined, page } }).then((r) => r.data),
  organizations: (q: string, page: number) =>
    api
      .get<PageResponse<AdminOrganization>>('/admin/organizations', { params: { q: q || undefined, page } })
      .then((r) => r.data),
  failedAudits: (page: number) =>
    api.get<PageResponse<AdminFailedAudit>>('/admin/audits/failed', { params: { page } }).then((r) => r.data),
  unlockUser: (userId: number) => api.post(`/admin/users/${userId}/unlock`).then(() => undefined),
}
