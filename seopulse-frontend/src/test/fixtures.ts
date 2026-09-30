import type { AuthResponse } from '@/api/auth'
import type { Audit } from '@/api/audits'

export function authResponse(overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    accessToken: 'access-token',
    tokenType: 'Bearer',
    expiresIn: 900,
    userId: 1,
    name: 'Test User',
    email: 'test@example.com',
    role: 'USER',
    emailVerified: true,
    emailVerificationRequired: true,
    ...overrides,
  }
}

export function audit(overrides: Partial<Audit> = {}): Audit {
  return {
    id: 7,
    websiteId: 3,
    websiteUrl: 'https://example.com',
    status: 'QUEUED',
    score: null,
    pagesCrawled: 0,
    pagesAnalyzed: 0,
    startedAt: null,
    completedAt: null,
    errorMessage: null,
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

export function page<T>(content: T[], pageNumber = 0, last = true) {
  return {
    content,
    page: pageNumber,
    size: content.length,
    totalElements: content.length,
    totalPages: 1,
    first: pageNumber === 0,
    last,
  }
}
