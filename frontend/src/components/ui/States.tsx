import { AlertTriangle, Loader2 } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Button } from './Button'

type EmptyStateProps = {
  title: string
  description?: string
  icon?: LucideIcon
  action?: ReactNode
}

export function EmptyState({ title, description, icon: Icon, action }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-dashed border-border-strong bg-surface px-6 py-10 text-center">
      {Icon && <Icon aria-hidden="true" className="mx-auto size-8 text-ink-muted" />}
      <p className="mt-3 font-medium">{title}</p>
      {description && <p className="mx-auto mt-1 max-w-md text-sm text-ink-muted">{description}</p>}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  )
}

export function LoadingState({ label = 'Carregando…' }: { label?: string }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center justify-center gap-2 rounded-xl border border-border bg-surface px-6 py-10 text-sm text-ink-muted"
    >
      <Loader2 aria-hidden="true" className="size-4 animate-spin" />
      {label}
    </div>
  )
}

type ErrorStateProps = {
  message: string
  title?: string
  onRetry?: () => void
}

export function ErrorState({
  message,
  title = 'Não foi possível carregar',
  onRetry,
}: ErrorStateProps) {
  return (
    <div
      role="alert"
      className="flex flex-col gap-4 rounded-xl border border-danger/30 bg-danger-surface/50 p-5 sm:flex-row sm:items-start"
    >
      <AlertTriangle aria-hidden="true" className="size-5 shrink-0 text-danger" />
      <div className="flex-1">
        <p className="font-medium text-danger">{title}</p>
        <p className="mt-1 text-sm text-ink-muted">{message}</p>
      </div>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  )
}

type FeedbackMessageProps = {
  tone?: 'success' | 'error' | 'info'
  children: ReactNode
  className?: string
}

export function FeedbackMessage({ tone = 'success', children, className }: FeedbackMessageProps) {
  const estilos = {
    success: 'border-success/30 bg-success-surface text-success',
    error: 'border-danger/30 bg-danger-surface text-danger',
    info: 'border-info/30 bg-info-surface text-info',
  }[tone]

  return (
    <p
      role={tone === 'error' ? 'alert' : 'status'}
      className={cn('rounded-md border px-3 py-2 text-sm font-medium', estilos, className)}
    >
      {children}
    </p>
  )
}
