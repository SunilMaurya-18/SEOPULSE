import { BadgeCheck } from 'lucide-react'

type CitationToastProps = {
  message: string | null
}

export function CitationToast({ message }: CitationToastProps) {
  const visible = Boolean(message)

  return (
    <div
      className={`pointer-events-none fixed right-6 bottom-6 z-50 flex items-center gap-2 rounded border border-[#27272a] bg-[#18181b] px-4 py-3 font-mono text-sm text-white shadow-xl transition-all duration-300 ${
        visible
          ? 'translate-y-0 opacity-100'
          : 'translate-y-16 opacity-0'
      }`}
      role="status"
      aria-live="polite"
    >
      <BadgeCheck className="h-[18px] w-[18px] text-[#dc2626]" />
      <span>{message ?? 'Clause reference copied to clipboard'}</span>
    </div>
  )
}
