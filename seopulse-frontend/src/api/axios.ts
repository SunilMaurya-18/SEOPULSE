import axios from 'axios'

const TOKEN_KEY = 'seopulse-token'

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = String(error.config?.url ?? '')
      const isAuthCall = url.includes('/auth/login') || url.includes('/auth/register')
      if (!isAuthCall && getStoredToken()) {
        setStoredToken(null)
        if (!window.location.pathname.startsWith('/login')) {
          window.location.assign('/login')
        }
      }
    }
    return Promise.reject(error)
  },
)

export default api
