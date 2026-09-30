import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'

import { authApi, toAuthUser, type AuthResponse, type AuthUser } from '@/api/auth'
import { onSessionChange, refreshSession, setAccessToken } from '@/api/axios'
import { setMonitoringUser } from '@/lib/monitoring'

const LEGACY_KEYS = ['seopulse-token', 'seopulse-user']

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

interface AuthContextValue {
  user: AuthUser | null
  status: AuthStatus
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<AuthUser>
  logout: () => Promise<void>
  logoutAll: () => Promise<void>
  markEmailVerified: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<AuthUser | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')

  const applySession = useCallback(
    (session: AuthResponse | null) => {
      if (session) {
        setAccessToken(session.accessToken)
        const nextUser = toAuthUser(session)
        setUser(nextUser)
        setMonitoringUser(nextUser.userId)
        setStatus('authenticated')
      } else {
        setAccessToken(null)
        setUser(null)
        setMonitoringUser(null)
        setStatus('anonymous')
        queryClient.clear()
      }
    },
    [queryClient],
  )

  useEffect(() => {
    LEGACY_KEYS.forEach((key) => localStorage.removeItem(key))

    let active = true
    void refreshSession().then((session) => {
      if (active) applySession(session)
    })
    const unsubscribe = onSessionChange(applySession)
    return () => {
      active = false
      unsubscribe()
    }
  }, [applySession])

  const login = useCallback(
    async (email: string, password: string) => {
      applySession(await authApi.login(email, password))
    },
    [applySession],
  )

  const register = useCallback(
    async (name: string, email: string, password: string) => {
      const session = await authApi.register(name, email, password)
      applySession(session)
      return toAuthUser(session)
    },
    [applySession],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // The local session is discarded regardless; the cookie expires on its own.
    }
    applySession(null)
  }, [applySession])

  const logoutAll = useCallback(async () => {
    await authApi.logoutAll()
    applySession(null)
  }, [applySession])

  const markEmailVerified = useCallback(() => {
    setUser((current) => (current ? { ...current, emailVerified: true } : current))
  }, [])

  const value = useMemo(
    () => ({
      user,
      status,
      isAuthenticated: status === 'authenticated',
      login,
      register,
      logout,
      logoutAll,
      markEmailVerified,
    }),
    [user, status, login, register, logout, logoutAll, markEmailVerified],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
