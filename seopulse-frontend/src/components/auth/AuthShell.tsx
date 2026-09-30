import type { ReactNode } from 'react'

import { Logo } from '@/components/brand/Logo'

export function AuthShell({
  title,
  description,
  children,
  footer,
}: {
  title: string
  description?: string
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <Logo className="justify-center" />
          <h1 className="mt-6 font-display text-2xl font-semibold text-main">
            {title}
          </h1>
          {description && <p className="mt-1 text-sm text-muted">{description}</p>}
        </div>

        <div className="space-y-4 rounded-lg border border-default bg-surface p-5">
          {children}
        </div>

        {footer && <p className="text-center text-sm text-muted">{footer}</p>}
      </div>
    </div>
  )
}
