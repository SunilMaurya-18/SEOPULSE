import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'

import { setAccessToken } from '@/api/axios'
import { server } from './server'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
  setAccessToken(null)
  sessionStorage.clear()
  localStorage.clear()
})

afterAll(() => server.close())
