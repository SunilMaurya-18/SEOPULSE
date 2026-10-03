import { useEffect, useLayoutEffect, useRef } from 'react'

interface TurnstileOptions {
  sitekey: string
  theme?: 'light' | 'dark' | 'auto'
  callback: (token: string) => void
  'expired-callback': () => void
  'error-callback': () => void
}

interface TurnstileApi {
  render: (element: HTMLElement, options: TurnstileOptions) => string
  remove: (widgetId: string) => void
}

declare global {
  interface Window {
    turnstile?: TurnstileApi
  }
}

const SCRIPT_URL = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

export const turnstileSiteKey = (import.meta.env.VITE_TURNSTILE_SITE_KEY as string | undefined) ?? ''
export const captchaEnabled = turnstileSiteKey.length > 0

let scriptPromise: Promise<TurnstileApi> | null = null

function loadTurnstile(): Promise<TurnstileApi> {
  if (window.turnstile) return Promise.resolve(window.turnstile)
  scriptPromise ??= new Promise<TurnstileApi>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SCRIPT_URL
    script.async = true
    script.onload = () => (window.turnstile ? resolve(window.turnstile) : reject(new Error('Turnstile missing')))
    script.onerror = () => {
      scriptPromise = null
      reject(new Error('Turnstile failed to load'))
    }
    document.head.appendChild(script)
  })
  return scriptPromise
}

interface TurnstileProps {
  onToken: (token: string | null) => void
  theme?: 'light' | 'dark' | 'auto'
  className?: string
}

/** Tokens are single use: remount with a new `key` after every submission. */
export function Turnstile({ onToken, theme = 'auto', className }: TurnstileProps) {
  const container = useRef<HTMLDivElement>(null)
  const callback = useRef(onToken)

  useLayoutEffect(() => {
    callback.current = onToken
  })

  useEffect(() => {
    if (!captchaEnabled) return
    let widgetId: string | null = null
    let cancelled = false
    loadTurnstile()
      .then((turnstile) => {
        if (cancelled || !container.current) return
        widgetId = turnstile.render(container.current, {
          sitekey: turnstileSiteKey,
          theme,
          callback: (token) => callback.current(token),
          'expired-callback': () => callback.current(null),
          'error-callback': () => callback.current(null),
        })
      })
      .catch(() => callback.current(null))
    return () => {
      cancelled = true
      callback.current(null)
      if (widgetId && window.turnstile) window.turnstile.remove(widgetId)
    }
  }, [theme])

  if (!captchaEnabled) return null
  return <div ref={container} className={className} />
}
