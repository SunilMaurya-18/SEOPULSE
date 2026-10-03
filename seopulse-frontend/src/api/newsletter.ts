import api from './axios'

export const newsletterApi = {
  subscribe: (email: string) => api.post('/public/newsletter/subscribe', { email, consent: true }),
  confirm: (token: string) => api.post('/public/newsletter/confirm', { token }),
  unsubscribe: (token: string) => api.post('/public/newsletter/unsubscribe', { token }),
}
