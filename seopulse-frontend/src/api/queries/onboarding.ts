import { useQuery } from '@tanstack/react-query'

import { onboardingApi, type OnboardingStatus } from '@/api/onboarding'
import { queryKeys } from './keys'

export function useOnboarding(projectId: number) {
  return useQuery({
    queryKey: queryKeys.onboarding(projectId),
    queryFn: () => onboardingApi.status(projectId),
  })
}

export function isOnboardingOpen(status: OnboardingStatus | undefined): status is OnboardingStatus {
  return Boolean(status && !status.dismissed && status.steps.some((step) => !step.done))
}
