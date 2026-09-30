import api from './axios'

export interface Organization {
  id: number
  name: string
  slug: string
  role: string | null
}

export interface OrgMember {
  userId: number
  name: string
  email: string
  role: string
}

export interface OrgInvite {
  id: number
  email: string
  role: string
}

export interface PlanLimits {
  websites: number
  pagesPerAudit: number
  auditsPerMonth: number
  members: number
}

export interface BillingSnapshot {
  planCode: string
  planName: string
  status: string
  currentPeriodEnd: string | null
  trialEnd: string | null
  cancelAtPeriodEnd: boolean
  limits: PlanLimits
  auditsUsed: number
  websitesUsed: number
}

export const saasApi = {
  orgs: () => api.get<Organization[]>('/orgs').then((response) => response.data),
  members: (orgId: number) =>
    api.get<OrgMember[]>(`/orgs/${orgId}/members`).then((response) => response.data),
  invites: (orgId: number) =>
    api.get<OrgInvite[]>(`/orgs/${orgId}/invitations`).then((response) => response.data),
  invite: (orgId: number, email: string, role: string) =>
    api.post(`/orgs/${orgId}/invitations`, { email, role }),
  acceptInvite: (token: string) => api.post('/orgs/invitations/accept', { token }),
  billing: (orgId: number) =>
    api.get<BillingSnapshot>(`/orgs/${orgId}/billing`).then((response) => response.data),
  checkout: (orgId: number, plan: string, interval: string) =>
    api
      .post<{ url: string }>(`/orgs/${orgId}/billing/checkout`, { plan, interval })
      .then((response) => response.data.url),
  portal: (orgId: number) =>
    api.post<{ url: string }>(`/orgs/${orgId}/billing/portal`).then((response) => response.data.url),
}
