import { cva } from 'class-variance-authority'
import type { VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

export type BadgeVariant = 'neutral' | 'success' | 'warning' | 'danger' | 'info'

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium',
  {
    variants: {
      variant: {
        neutral: 'bg-surface-muted text-ink',
        success: 'bg-success-surface text-success',
        warning: 'bg-warning-surface text-warning',
        danger: 'bg-danger-surface text-danger',
        info: 'bg-info-surface text-info',
      },
    },
    defaultVariants: { variant: 'neutral' },
  },
)

type BadgeProps = HTMLAttributes<HTMLSpanElement> & VariantProps<typeof badgeVariants>

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />
}
