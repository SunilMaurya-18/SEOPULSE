import api, { authClient } from './axios'

export interface AuthUser {
  userId: number
  name: string
  email: string
  role: string
  emailVerified: boolean
  /** When false, unverified accounts can still run audits. */
  emailVerificationRequired: boolean
}

export interface AuthResponse extends AuthUser {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export const authApi = {
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const response = await authClient.post<AuthResponse>('/auth/login', {
      email,
      password,
    })
    return response.data
  },

  register: async (
    name: string,
    email: string,
    password: string,
  ): Promise<AuthResponse> => {
    const response = await authClient.post<AuthResponse>('/auth/register', {
      name,
      email,
      password,
    })
    return response.data
  },

  logout: async (): Promise<void> => {
    await authClient.post('/auth/logout')
  },

  logoutAll: async (): Promise<void> => {
    await api.post('/auth/logout-all', null, { withCredentials: true })
  },

  verifyEmail: async (token: string): Promise<void> => {
    await authClient.post('/auth/verify-email', { token })
  },

  resendVerification: async (): Promise<void> => {
    await api.post('/auth/resend-verification')
  },

  forgotPassword: async (email: string): Promise<void> => {
    await authClient.post('/auth/forgot-password', { email })
  },

  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await authClient.post('/auth/reset-password', { token, newPassword })
  },
}

export function toAuthUser(response: AuthResponse): AuthUser {
  return {
    userId: response.userId,
    name: response.name,
    email: response.email,
    role: response.role,
    emailVerified: response.emailVerified,
    emailVerificationRequired: response.emailVerificationRequired,
  }
}
