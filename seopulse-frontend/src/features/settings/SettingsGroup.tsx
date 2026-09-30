import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

export function SettingsGroup({
  id,
  title,
  footer,
  children,
}: {
  id?: string
  title?: string
  footer?: ReactNode
  children: ReactNode
}) {
  return (
    <section id={id} className="scroll-mt-24">
      {title && <h2 className="mb-2 px-4 text-[13px] font-semibold tracking-[0.02em] text-dim uppercase">{title}</h2>}
      <div className="widget divide-y divide-default overflow-hidden">{children}</div>
      {footer && <div className="mt-2 px-4 text-xs leading-relaxed text-dim">{footer}</div>}
    </section>
  )
}

export function SettingsRow({
  icon,
  tint,
  label,
  detail,
  children,
  className,
}: {
  icon?: ReactNode
  tint?: string
  label: ReactNode
  detail?: ReactNode
  children?: ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex min-h-[52px] items-center gap-3.5 px-4 py-2.5 sm:px-5', className)}>
      {icon && (
        <span
          className={cn(
            'flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-[8px] bg-gradient-to-b text-white shadow-[inset_0_1px_0_rgb(255_255_255/0.25)]',
            tint,
          )}
        >
          {icon}
        </span>
      )}
      <div className="min-w-0 flex-1">
        <div className="truncate text-[15px] text-main">{label}</div>
        {detail && <div className="truncate text-[13px] text-dim">{detail}</div>}
      </div>
      {children && <div className="flex flex-wrap items-center justify-end gap-2">{children}</div>}
    </div>
  )
}
