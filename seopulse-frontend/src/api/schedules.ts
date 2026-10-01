import api from './axios'

export type ScheduleFrequency = 'DAILY' | 'WEEKLY'
export type PlanSchedule = 'NONE' | 'WEEKLY' | 'DAILY'

export interface AuditSchedule {
  websiteId: number
  configured: boolean
  /** The most frequent schedule the plan allows. */
  planSchedule: PlanSchedule
  frequency: ScheduleFrequency | null
  /** ISO day of week, 1 = Monday. */
  dayOfWeek: number | null
  hourOfDay: number | null
  timezone: string | null
  enabled: boolean
  nextRunAt: string | null
  lastRunAt: string | null
  lastAuditId: number | null
  lastError: string | null
}

export interface ScheduleInput {
  frequency: ScheduleFrequency
  dayOfWeek?: number
  hourOfDay: number
  timezone: string
  enabled?: boolean
}

const path = (projectId: number, websiteId: number) =>
  `/projects/${projectId}/websites/${websiteId}/schedule`

export const scheduleApi = {
  get: (projectId: number, websiteId: number) =>
    api.get<AuditSchedule>(path(projectId, websiteId)).then((response) => response.data),
  save: (projectId: number, websiteId: number, input: ScheduleInput) =>
    api.put<AuditSchedule>(path(projectId, websiteId), input).then((response) => response.data),
  remove: (projectId: number, websiteId: number) => api.delete(path(projectId, websiteId)),
}
