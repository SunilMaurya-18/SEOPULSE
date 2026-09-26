import api from './axios'
import type { PageResponse } from './websites'

export interface ProjectSummary {
  id: number
  name: string
  websiteCount: number
  auditCount: number
  completedAuditCount: number
  failedAuditCount: number
}

export interface Project {
  id: number
  name: string
  description?: string | null
  createdAt?: string
  updatedAt?: string
}

interface ProjectSummaryDto {
  projectId: number
  projectName: string
  totalWebsites: number
  totalAudits: number
  completedAudits: number
  failedAudits: number
  activeAudits?: number
}

export const projectApi = {
  getProjects: async (page = 0, size = 20) => {
    const response = await api.get<PageResponse<Project>>('/projects', {
      params: { page, size },
    })
    return response.data
  },

  getProject: async (projectId: number) => {
    const response = await api.get<Project>(`/projects/${projectId}`)
    return response.data
  },

  getProjectSummary: async (projectId: number): Promise<ProjectSummary> => {
    const response = await api.get<ProjectSummaryDto>(
      `/projects/${projectId}/summary`,
    )
    const data = response.data
    return {
      id: data.projectId,
      name: data.projectName,
      websiteCount: data.totalWebsites,
      auditCount: data.totalAudits,
      completedAuditCount: data.completedAudits,
      failedAuditCount: data.failedAudits,
    }
  },

  createProject: async (data: { name: string; description?: string }) => {
    const response = await api.post<Project>('/projects', data)
    return response.data
  },

  deleteProject: async (projectId: number) => {
    await api.delete(`/projects/${projectId}`)
  },
}
