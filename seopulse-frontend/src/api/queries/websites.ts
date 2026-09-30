import { useQuery } from '@tanstack/react-query'

import { websiteApi } from '@/api/websites'
import { queryKeys } from './keys'

export function useWebsites(projectId: number) {
  return useQuery({
    queryKey: queryKeys.websites(projectId),
    queryFn: async () => (await websiteApi.getWebsites(projectId, 0, 100)).content ?? [],
  })
}
