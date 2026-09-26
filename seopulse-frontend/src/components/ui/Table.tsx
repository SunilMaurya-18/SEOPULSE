import type { HTMLAttributes, ReactNode, TdHTMLAttributes, ThHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

export function Table({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="w-full overflow-x-auto">
      <table
        className={cn('w-full min-w-[640px] border-collapse text-left text-sm', className)}
        {...props}
      >
        {children}
      </table>
    </div>
  )
}

export function THead({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <thead
      className={cn('border-b border-default bg-surface-low', className)}
      {...props}
    >
      {children}
    </thead>
  )
}

export function TBody({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <tbody className={cn('divide-y divide-default/60', className)} {...props}>
      {children}
    </tbody>
  )
}

export function TR({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={cn('transition-colors hover:bg-surface-low/80', className)}
      {...props}
    >
      {children}
    </tr>
  )
}

export function TH({
  className,
  children,
  ...props
}: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn(
        'px-4 py-3 font-mono text-[10px] font-medium tracking-wider text-dim uppercase sm:px-5',
        className,
      )}
      {...props}
    >
      {children}
    </th>
  )
}

export function TD({
  className,
  children,
  mono,
  ...props
}: TdHTMLAttributes<HTMLTableCellElement> & { mono?: boolean }) {
  return (
    <td
      className={cn(
        'px-4 py-3.5 text-sm text-main sm:px-5',
        mono && 'font-mono text-xs text-muted font-tabular',
        className,
      )}
      {...props}
    >
      {children}
    </td>
  )
}

export function TableToolbar({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-col gap-3 border-b border-default px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-5">
      {children}
    </div>
  )
}
