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
  schedule: 'NONE' | 'WEEKLY' | 'DAILY'
  webhookAlerts: boolean
  whiteLabel: boolean
  retentionDays: number
}

export type AlertType = 'SCORE_DROP' | 'NEW_ERRORS' | 'PAGE_UNREACHABLE' | 'AUDIT_FAILED'
export type AlertChannel = 'EMAIL' | 'SLACK_WEBHOOK' | 'WEBHOOK'

export interface AlertRule {
  id: number
  websiteId: number | null
  type: AlertType
  threshold: number | null
  channel: AlertChannel
  /** Email address (blank = owners and admins), or a masked webhook URL. */
  target: string | null
  enabled: boolean
  /** Full secret only right after creation or rotation; otherwise masked. */
  signingSecret: string | null
  secretRevealed: boolean
  createdAt: string
}

export interface AlertRuleInput {
  type: AlertType
  threshold?: number | null
  channel: AlertChannel
  target?: string | null
  websiteId?: number | null
  enabled?: boolean
}

export interface AlertDelivery {
  id: number
  ruleId: number | null
  auditId: number | null
  eventType: string
  channel: AlertChannel
  subject: string
  delivered: boolean
  attempts: number
  lastError: string | null
  createdAt: string
  deliveredAt: string | null
}

export interface OrganizationBranding {
  companyName: string | null
  brandColor: string | null
  coverText: string | null
  logoDataUrl: string | null
}

export interface BrandingState {
  branding: OrganizationBranding | null
  whiteLabelAvailable: boolean
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

export const alertApi = {
  list: (orgId: number) =>
    api.get<AlertRule[]>(`/orgs/${orgId}/alerts`).then((response) => response.data),
  deliveries: (orgId: number) =>
    api.get<AlertDelivery[]>(`/orgs/${orgId}/alerts/deliveries`).then((response) => response.data),
  create: (orgId: number, input: AlertRuleInput) =>
    api.post<AlertRule>(`/orgs/${orgId}/alerts`, input).then((response) => response.data),
  update: (orgId: number, ruleId: number, input: AlertRuleInput) =>
    api.put<AlertRule>(`/orgs/${orgId}/alerts/${ruleId}`, input).then((response) => response.data),
  remove: (orgId: number, ruleId: number) => api.delete(`/orgs/${orgId}/alerts/${ruleId}`),
  test: (orgId: number, ruleId: number) =>
    api
      .post<{ delivered: boolean; error: string | null }>(`/orgs/${orgId}/alerts/${ruleId}/test`)
      .then((response) => response.data),
  rotateSecret: (orgId: number, ruleId: number) =>
    api.post<AlertRule>(`/orgs/${orgId}/alerts/${ruleId}/rotate-secret`).then((response) => response.data),
}

export const brandingApi = {
  get: (orgId: number) =>
    api.get<BrandingState>(`/orgs/${orgId}/branding`).then((response) => response.data),
  save: (orgId: number, branding: OrganizationBranding) =>
    api.put<BrandingState>(`/orgs/${orgId}/branding`, branding).then((response) => response.data),
  clear: (orgId: number) => api.delete(`/orgs/${orgId}/branding`),
}
