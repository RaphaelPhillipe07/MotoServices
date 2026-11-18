import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Button } from './Button'

export const Dialog = DialogPrimitive.Root

type DialogContentProps = {
  title: string
  description?: string
  children: ReactNode
  className?: string
}

export function DialogContent({ title, description, children, className }: DialogContentProps) {
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-ink/40" />
      <DialogPrimitive.Content
        className={cn(
          'fixed top-1/2 left-1/2 z-50 max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-xl bg-surface p-6 shadow-xl',
          className,
        )}
      >
        <DialogPrimitive.Title className="text-lg font-semibold">{title}</DialogPrimitive.Title>
        {description && (
          <DialogPrimitive.Description className="mt-1 text-sm text-ink-muted">
            {description}
          </DialogPrimitive.Description>
        )}
        <DialogPrimitive.Close asChild>
          <Button
            variant="ghost"
            size="icon"
            aria-label="Fechar"
            className="absolute top-3 right-3"
          >
            <X aria-hidden="true" className="size-4" />
          </Button>
        </DialogPrimitive.Close>
        <div className="mt-4">{children}</div>
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  )
}
