import { setupServer } from 'msw/node'

import { API_BASE_URL } from '@/api/axios'

export const server = setupServer()

export const api = (path: string) => `${API_BASE_URL}${path}`
