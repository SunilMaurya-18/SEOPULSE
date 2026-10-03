import api from './axios'

export type OnboardingStepId = 'VERIFY_EMAIL' | 'ADD_WEBSITE' | 'RUN_AUDIT' | 'SHARE_REPORT' | 'INVITE_TEAM'

export interface OnboardingStatus {
  dismissed: boolean
  steps: { id: OnboardingStepId; done: boolean }[]
}

export const onboardingApi = {
  status: (projectId: number) =>
    api.get<OnboardingStatus>(`/projects/${projectId}/onboarding`).then((response) => response.data),
  dismiss: () => api.post('/onboarding/dismiss'),
}
