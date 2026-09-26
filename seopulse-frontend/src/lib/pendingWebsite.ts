const PENDING_URL_KEY = 'seopulse-pending-url'

export function setPendingWebsiteUrl(url: string) {
  const cleaned = url.trim()
  if (!cleaned) {
    sessionStorage.removeItem(PENDING_URL_KEY)
    return
  }
  sessionStorage.setItem(PENDING_URL_KEY, cleaned)
}

export function peekPendingWebsiteUrl(): string | null {
  return sessionStorage.getItem(PENDING_URL_KEY)
}

export function takePendingWebsiteUrl(): string | null {
  const value = sessionStorage.getItem(PENDING_URL_KEY)
  sessionStorage.removeItem(PENDING_URL_KEY)
  return value
}
