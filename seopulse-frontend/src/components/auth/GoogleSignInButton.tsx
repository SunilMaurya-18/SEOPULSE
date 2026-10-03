import { useEffect, useLayoutEffect, useRef } from 'react'

import { useTheme } from '@/lib/theme'

interface GoogleIdApi {
  initialize: (options: { client_id: string; callback: (response: { credential?: string }) => void }) => void
  renderButton: (
    element: HTMLElement,
    options: { theme: string; size: string; text: string; shape: string; width: number; logo_alignment: string },
  ) => void
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdApi } }
  }
}

const SCRIPT_URL = 'https://accounts.google.com/gsi/client'

export const googleClientId = (import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined) ?? ''
export const googleSignInEnabled = googleClientId.length > 0

let scriptPromise: Promise<GoogleIdApi> | null = null

function loadGoogleIdentity(): Promise<GoogleIdApi> {
  if (window.google?.accounts?.id) return Promise.resolve(window.google.accounts.id)
  scriptPromise ??= new Promise<GoogleIdApi>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SCRIPT_URL
    script.async = true
    script.onload = () =>
      window.google?.accounts?.id ? resolve(window.google.accounts.id) : reject(new Error('Google sign-in missing'))
    script.onerror = () => {
      scriptPromise = null
      reject(new Error('Google sign-in failed to load'))
    }
    document.head.appendChild(script)
  })
  return scriptPromise
}

interface GoogleSignInButtonProps {
  text?: 'signin_with' | 'signup_with' | 'continue_with'
  onCredential: (credential: string) => void
  onUnavailable?: () => void
}

/** Renders nothing unless VITE_GOOGLE_CLIENT_ID is set. */
export function GoogleSignInButton({ text = 'continue_with', onCredential, onUnavailable }: GoogleSignInButtonProps) {
  const { theme } = useTheme()
  const container = useRef<HTMLDivElement>(null)
  const handlers = useRef({ onCredential, onUnavailable })

  useLayoutEffect(() => {
    handlers.current = { onCredential, onUnavailable }
  })

  useEffect(() => {
    if (!googleSignInEnabled) return
    let cancelled = false
    loadGoogleIdentity()
      .then((google) => {
        const element = container.current
        if (cancelled || !element) return
        google.initialize({
          client_id: googleClientId,
          callback: (response) => {
            if (response.credential) handlers.current.onCredential(response.credential)
          },
        })
        element.replaceChildren()
        google.renderButton(element, {
          theme: theme === 'dark' ? 'filled_black' : 'outline',
          size: 'large',
          text,
          shape: 'rectangular',
          logo_alignment: 'center',
          width: Math.min(400, Math.max(200, Math.round(element.getBoundingClientRect().width))),
        })
      })
      .catch(() => handlers.current.onUnavailable?.())
    return () => {
      cancelled = true
    }
  }, [theme, text])

  if (!googleSignInEnabled) return null

  return (
    <div className="space-y-4">
      <div ref={container} className="flex h-10 w-full justify-center" />
      <div className="flex items-center gap-3 text-[12px] font-medium text-dim" aria-hidden="true">
        <span className="h-px flex-1 bg-surface-high" />
        or
        <span className="h-px flex-1 bg-surface-high" />
      </div>
    </div>
  )
}
