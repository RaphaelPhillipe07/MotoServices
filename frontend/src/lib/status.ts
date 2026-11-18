import { Ban, CheckCircle2, Circle, Clock, Lock, Wrench } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { StatusOS } from '../api/types'
import type { BadgeVariant } from '../components/ui/Badge'

type StatusMeta = {
  label: string
  variant: BadgeVariant
  icon: LucideIcon
}

export const statusMeta: Record<StatusOS, StatusMeta> = {
  ABERTA: { label: 'Aberta', variant: 'info', icon: Circle },
  EM_ANDAMENTO: { label: 'Em andamento', variant: 'warning', icon: Wrench },
  AGUARDANDO_CLIENTE: { label: 'Aguardando cliente', variant: 'warning', icon: Clock },
  CONCLUIDA: { label: 'Concluída', variant: 'success', icon: CheckCircle2 },
  FECHADA: { label: 'Fechada', variant: 'neutral', icon: Lock },
  CANCELADA: { label: 'Cancelada', variant: 'danger', icon: Ban },
}

export const statusOrder: StatusOS[] = [
  'ABERTA',
  'EM_ANDAMENTO',
  'AGUARDANDO_CLIENTE',
  'CONCLUIDA',
  'FECHADA',
  'CANCELADA',
]
