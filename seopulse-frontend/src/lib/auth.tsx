import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import { authApi, type AuthUser } from '@/api/auth'
import { getStoredToken, setStoredToken } from '@/api/axios'

const USER_KEY = 'seopulse-user'

interface AuthContextValue {
  user: AuthUser | null
  token: string | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

function persistSession(token: string, user: AuthUser) {
  setStoredToken(token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

function clearSession() {
  setStoredToken(null)
  localStorage.removeItem(USER_KEY)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken())
  const [user, setUser] = useState<AuthUser | null>(() =>
    getStoredToken() ? readStoredUser() : null,
  )

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password)
    const nextUser: AuthUser = {
      userId: response.userId,
      name: response.name,
      email: response.email,
      role: response.role,
    }
    persistSession(response.accessToken, nextUser)
    setToken(response.accessToken)
    setUser(nextUser)
  }, [])

  const register = useCallback(
    async (name: string, email: string, password: string) => {
      const response = await authApi.register(name, email, password)
      const nextUser: AuthUser = {
        userId: response.userId,
        name: response.name,
        email: response.email,
        role: response.role,
      }
      persistSession(response.accessToken, nextUser)
      setToken(response.accessToken)
      setUser(nextUser)
    },
    [],
  )

  const logout = useCallback(() => {
    clearSession()
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token),
      login,
      register,
      logout,
    }),
    [user, token, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
