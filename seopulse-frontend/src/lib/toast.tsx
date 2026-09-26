import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import { cn } from '@/lib/cn'

export type ToastTone = 'success' | 'error' | 'info'

export interface ToastInput {
  title: string
  description?: string
  tone?: ToastTone
  durationMs?: number
}

interface ToastItem extends ToastInput {
  id: number
  tone: ToastTone
}

interface ToastContextValue {
  pushToast: (toast: ToastInput) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

const toneStyles: Record<ToastTone, string> = {
  success: 'border-success/25 bg-surface/95',
  error: 'border-critical/25 bg-surface/95',
  info: 'border-info/25 bg-surface/95',
}

const toneIcons = {
  success: CheckCircle2,
  error: AlertCircle,
  info: Info,
}

const toneIconColor = {
  success: 'text-success',
  error: 'text-critical',
  info: 'text-info',
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: number) => {
    setItems((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const pushToast = useCallback(
    (toast: ToastInput) => {
      const id = Date.now() + Math.floor(Math.random() * 1000)
      const item: ToastItem = {
        id,
        title: toast.title,
        description: toast.description,
        tone: toast.tone ?? 'info',
        durationMs: toast.durationMs,
      }
      setItems((prev) => [...prev.slice(-3), item])
      window.setTimeout(() => dismiss(id), toast.durationMs ?? 4200)
    },
    [dismiss],
  )

  const value = useMemo(() => ({ pushToast }), [pushToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        className="pointer-events-none fixed right-4 bottom-4 z-[100] flex w-[min(100vw-2rem,24rem)] flex-col gap-2"
        aria-live="polite"
      >
        {items.map((toast) => {
          const Icon = toneIcons[toast.tone]
          return (
            <div
              key={toast.id}
              className={cn(
                'pointer-events-auto animate-[toast-in_280ms_ease-out] rounded-xl border px-3.5 py-3 shadow-overlay backdrop-blur-md',
                toneStyles[toast.tone],
              )}
            >
              <div className="flex items-start gap-3">
                <Icon
                  className={cn('mt-0.5 h-4 w-4 shrink-0', toneIconColor[toast.tone])}
                />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-main">{toast.title}</p>
                  {toast.description && (
                    <p className="mt-0.5 text-xs leading-5 text-muted">
                      {toast.description}
                    </p>
                  )}
                </div>
                <button
                  type="button"
                  className="rounded p-0.5 text-muted hover:text-main"
                  onClick={() => dismiss(toast.id)}
                  aria-label="Dismiss"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
