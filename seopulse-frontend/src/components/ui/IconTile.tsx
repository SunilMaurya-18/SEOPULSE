import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { TINTS, type Tint } from './tints'

const SIZES = {
  sm: 'h-[26px] w-[26px] rounded-[8px] [&_svg]:h-3.5 [&_svg]:w-3.5',
  md: 'h-8 w-8 rounded-[10px] [&_svg]:h-4 [&_svg]:w-4',
  lg: 'h-11 w-11 rounded-[13px] text-[17px] [&_svg]:h-5 [&_svg]:w-5',
  xl: 'h-14 w-14 rounded-[16px] text-[22px] [&_svg]:h-6 [&_svg]:w-6',
}

export function IconTile({
  tint,
  size = 'md',
  children,
  className,
}: {
  tint: Tint
  size?: keyof typeof SIZES
  children: ReactNode
  className?: string
}) {
  return (
    <span
      className={cn(
        'flex shrink-0 items-center justify-center bg-gradient-to-b font-bold text-white uppercase shadow-[inset_0_1px_0_rgb(255_255_255/0.25)]',
        TINTS[tint],
        SIZES[size],
        className,
      )}
    >
      {children}
    </span>
  )
}
