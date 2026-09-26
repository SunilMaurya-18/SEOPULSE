import api from './axios'

export interface Website {
  id: number
  name?: string
  url: string
  status?: string
  projectId?: number
  createdAt?: string
  updatedAt?: string
}

export interface CreateWebsiteRequest {
  name: string
  url: string
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

function normalizeWebsiteUrl(raw: string): string {
  const trimmed = raw.trim()
  if (!trimmed) return trimmed
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  return `https://${trimmed}`
}

export function websiteNameFromUrl(rawUrl: string): string {
  try {
    const url = new URL(normalizeWebsiteUrl(rawUrl))
    return url.hostname.replace(/^www\./i, '') || 'Website'
  } catch {
    return 'Website'
  }
}

export const websiteApi = {
  getWebsites: async (
    projectId: number,
    page = 0,
    size = 20,
  ): Promise<PageResponse<Website>> => {
    const response = await api.get<PageResponse<Website>>(
      `/projects/${projectId}/websites`,
      {
        params: {
          page,
          size,
        },
      },
    )

    return response.data
  },

  createWebsite: async (
    projectId: number,
    data: CreateWebsiteRequest,
  ): Promise<Website> => {
    const response = await api.post<Website>(
      `/projects/${projectId}/websites`,
      {
        name: data.name.trim(),
        url: normalizeWebsiteUrl(data.url),
      },
    )

    return response.data
  },
}
