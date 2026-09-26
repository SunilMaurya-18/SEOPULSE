import api from './axios'

export interface AuthUser {
  userId: number
  name: string
  email: string
  role: string
}

export interface AuthResponse extends AuthUser {
  accessToken: string
  tokenType: string
}

export const authApi = {
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/login', {
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
    const response = await api.post<AuthResponse>('/auth/register', {
      name,
      email,
      password,
    })
    return response.data
  },
}
