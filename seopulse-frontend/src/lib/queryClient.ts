import axios from 'axios'
import { QueryClient } from '@tanstack/react-query'

function isClientError(error: unknown): boolean {
  if (!axios.isAxiosError(error)) return false
  const status = error.response?.status ?? 0
  return status >= 400 && status < 500
}

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 15_000,
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => !isClientError(error) && failureCount < 2,
      },
      mutations: {
        retry: false,
      },
    },
  })
}
